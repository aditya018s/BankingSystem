package com.program.service;

import com.program.entity.User;
import com.program.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;

    // Use real encoder — we need actual BCrypt matching
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        // Inject real passwordEncoder since @InjectMocks won't pick it up automatically
        try {
            var field = UserService.class.getDeclaredField("passwordEncoder");
            field.setAccessible(true);
            field.set(userService, passwordEncoder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ════════════════════════════════════════════════════════
    // REGISTER TESTS
    // ════════════════════════════════════════════════════════

    @Test
    @DisplayName("Register: valid user registers successfully")
    void register_validUser_succeeds() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User user = buildUser("New User", "newuser", "new@email.com",
                "9876543210", "SecurePass1", "123 Street", "400001");

        String result = userService.registerUser(user, "SecurePass1");

        assertThat(result).isEqualTo("success");
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Register: duplicate username returns error")
    void register_duplicateUsername_returnsError() {
        when(userRepository.findByUsername("existing"))
                .thenReturn(Optional.of(new User()));

        User user = buildUser("Test", "existing", "test@email.com",
                "9876543210", "SecurePass1", "Addr", "400001");

        String result = userService.registerUser(user, "SecurePass1");

        assertThat(result).isEqualTo("Username already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register: duplicate email returns error")
    void register_duplicateEmail_returnsError() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("used@email.com"))
                .thenReturn(Optional.of(new User()));

        User user = buildUser("Test", "newuser", "used@email.com",
                "9876543210", "SecurePass1", "Addr", "400001");

        String result = userService.registerUser(user, "SecurePass1");

        assertThat(result).isEqualTo("Email already exists");
    }

    @Test
    @DisplayName("Register: password too short returns error")
    void register_shortPassword_returnsError() {
        User user = buildUser("Test", "newuser", "test@email.com",
                "9876543210", "abc", "Addr", "400001");

        String result = userService.registerUser(user, "abc");

        assertThat(result).isEqualTo("Password must be at least 8 characters long");
    }

    @Test
    @DisplayName("Register: passwords don't match returns error")
    void register_passwordMismatch_returnsError() {
        User user = buildUser("Test", "newuser", "test@email.com",
                "9876543210", "Password1", "Addr", "400001");

        String result = userService.registerUser(user, "Different1");

        assertThat(result).isEqualTo("Passwords do not match");
    }

    @Test
    @DisplayName("Register: invalid email format returns error")
    void register_invalidEmail_returnsError() {
        User user = buildUser("Test", "newuser", "notanemail",
                "9876543210", "Password1", "Addr", "400001");

        String result = userService.registerUser(user, "Password1");

        assertThat(result).isEqualTo("Enter a valid email address");
    }

    @Test
    @DisplayName("Register: invalid mobile number returns error")
    void register_invalidMobile_returnsError() {
        User user = buildUser("Test", "newuser", "test@email.com",
                "123", "Password1", "Addr", "400001");

        String result = userService.registerUser(user, "Password1");

        assertThat(result).isEqualTo("Mobile number must be exactly 10 digits");
    }

    @Test
    @DisplayName("Register: password gets encoded before saving")
    void register_passwordGetsEncoded() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User user = buildUser("Test", "newuser", "test@email.com",
                "9876543210", "RawPassword1", "Addr", "400001");

        userService.registerUser(user, "RawPassword1");

        // Password must not be stored as plain text
        assertThat(user.getPassword()).isNotEqualTo("RawPassword1");
        assertThat(passwordEncoder.matches("RawPassword1", user.getPassword())).isTrue();
    }

    // ════════════════════════════════════════════════════════
    // CHANGE PASSWORD TESTS
    // ════════════════════════════════════════════════════════

    @Test
    @DisplayName("Change password: valid change succeeds")
    void changePassword_valid_succeeds() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("OldPass123"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        String result = userService.changePassword(
                "testuser", "OldPass123", "NewPass456", "NewPass456");

        assertThat(result).isEqualTo("success");
        assertThat(passwordEncoder.matches("NewPass456", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Change password: wrong current password returns error")
    void changePassword_wrongCurrent_returnsError() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("CorrectPass1"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        String result = userService.changePassword(
                "testuser", "WrongPass1", "NewPass456", "NewPass456");

        assertThat(result).isEqualTo("Current password is incorrect");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Change password: same as old returns error")
    void changePassword_sameAsOld_returnsError() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("SamePass123"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        String result = userService.changePassword(
                "testuser", "SamePass123", "SamePass123", "SamePass123");

        assertThat(result).isEqualTo("New password must be different from current password");
    }

    // ── Helper ──────────────────────────────────────────────
    private User buildUser(String name, String username, String email,
                           String mobile, String password,
                           String address, String pincode) {
        User u = new User();
        u.setName(name);
        u.setUsername(username);
        u.setEmail(email);
        u.setMobile(mobile);
        u.setPassword(password);
        u.setAddress(address);
        u.setPincode(pincode);
        return u;
    }
}