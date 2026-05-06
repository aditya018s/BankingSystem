package com.program.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
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

    @Autowired
    private UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                userRepository.save(user);
                request.getSession().setAttribute("loggedInUser", user.getUsername());
                request.getSession().setAttribute("userId", user.getId());
                request.getSession().setAttribute("lastLoginTime", LocalDateTime.now());
            });
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            response.sendRedirect(isAdmin ? "/admin/dashboard" : "/dashboard");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            if (username != null && !username.isBlank()) {
                userRepository.findByUsername(username.trim()).ifPresent(user -> {
                    int attempts = user.getFailedLoginAttempts() + 1;
                    user.setFailedLoginAttempts(attempts);
                    if (attempts >= MAX_ATTEMPTS) {
                        user.setLockoutUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                        user.setFailedLoginAttempts(0);
                    }
                    userRepository.save(user);
                });
            }
            response.sendRedirect("/login?error");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(username ->
                        userRepository.findByUsername(username)
                                .map(user -> {
                                    if (user.isLockedOut())
                                        throw new UsernameNotFoundException("Account temporarily locked");
                                    return User.withUsername(user.getUsername())
                                            .password(user.getPassword())
                                            .roles(user.getRole().replace("ROLE_", ""))
                                            .disabled(!user.isEnabled())
                                            .build();
                                })
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                )
                .authorizeHttpRequests(auth -> auth
                        // Public pages
                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/forgot-password",
                                "/reset-password",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        // Admin web pages
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Admin API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // All other API — authenticated
                        .requestMatchers("/api/**").authenticated()
                        // Everything else
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler(authenticationFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .rememberMe(Customizer.withDefaults())
                // No CSRF for API
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/api/**"))
                )
                // Return JSON 401 for API instead of redirect
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> {
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
}