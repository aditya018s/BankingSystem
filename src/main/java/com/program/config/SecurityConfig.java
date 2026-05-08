package com.program.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.program.repository.UserRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final int MAX_ATTEMPTS    = 5;
    private static final int LOCKOUT_MINUTES = 15;

    @Autowired private UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .map(user -> {
                    if (user.isLockedOut())
                        throw new UsernameNotFoundException(
                                "Account locked. Try again after " + LOCKOUT_MINUTES + " minutes.");
                    return org.springframework.security.core.userdetails.User
                            .withUsername(user.getUsername())
                            .password(user.getPassword())
                            .roles(user.getRole().replace("ROLE_", ""))
                            .disabled(!user.isEnabled())
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService uds, PasswordEncoder pe) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(pe);
        return p;
    }

    // ── Success: straight to dashboard, no 2FA ──────────────
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();

            userRepository.findByUsername(username).ifPresent(user -> {
                // Reset failed attempts
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                // Track login IP for new device alerts
                String ip = getClientIp(request);
                String lastIp = user.getLastLoginIp();
                user.setLastLoginIp(ip);
                user.setLastLoginTime(LocalDateTime.now());
                userRepository.save(user);

                // Set session
                request.getSession().setAttribute("loggedInUser", user.getUsername());
                request.getSession().setAttribute("userId",        user.getId());
                request.getSession().setAttribute("lastLoginTime", LocalDateTime.now());
                request.getSession().setAttribute("loginIp",       ip);
                request.getSession().setAttribute("isNewDevice",
                        lastIp != null && !lastIp.equals(ip));
            });

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            response.sendRedirect(isAdmin ? "/admin/dashboard" : "/dashboard");
        };
    }

    // ── Failure: track attempts ─────────────────────────────
    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            if (username != null && !username.isBlank()) {
                userRepository.findByUsername(username.trim()).ifPresent(user -> {
                    int attempts = user.getFailedLoginAttempts() + 1;
                    user.setFailedLoginAttempts(attempts);
                    if (attempts >= MAX_ATTEMPTS) {
                        user.setLockoutUntil(
                                LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                        user.setFailedLoginAttempts(0);
                    }
                    userRepository.save(user);
                });
            }
            response.sendRedirect("/login?error");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authProvider,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler) throws Exception {

        http
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login", "/signup",
                                "/verify-signup-otp", "/resend-signup-otp",
                                "/forgot-password",   "/reset-password",
                                "/css/**", "/js/**", "/images/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .rememberMe(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/api/**")))
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (request, response, e) -> {
                                    response.setStatus(401);
                                    response.setContentType("application/json");
                                    response.getWriter().write(
                                            "{\"success\":false,\"message\":\"Not authenticated\",\"data\":null}"
                                    );
                                },
                                new AntPathRequestMatcher("/api/**")
                        )
                );

        return http.build();
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}