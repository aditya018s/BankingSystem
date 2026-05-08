package com.program.service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.program.entity.User;
import com.program.repository.UserRepository;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    // ── Generate 6-digit OTP ────────────────────────────────
    public String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    // ── Register: validate then save as unverified ──────────
    public String registerUser(User user, String confirmPassword) {
        String name      = normalize(user.getName());
        String username  = normalize(user.getUsername());
        String email     = normalize(user.getEmail());
        String mobile    = normalize(user.getMobile());
        String address   = normalize(user.getAddress());
        String pincode   = normalize(user.getPincode());
        String password  = user.getPassword() == null ? "" : user.getPassword().trim();
        String confirmed = confirmPassword == null ? "" : confirmPassword.trim();

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() ||
                mobile.isEmpty() || address.isEmpty() || pincode.isEmpty() || password.isEmpty())
            return "All fields are required";

        if (username.length() < 4 || !username.matches("[A-Za-z0-9_]+"))
            return "Username must be at least 4 characters (letters, numbers, underscore)";

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
            return "Enter a valid email address";

        if (!mobile.matches("\\d{10}"))
            return "Mobile number must be exactly 10 digits";

        if (!pincode.matches("\\d{6}"))
            return "Pin code must be exactly 6 digits";

        if (password.length() < 8)
            return "Password must be at least 8 characters long";

        if (!password.equals(confirmed))
            return "Passwords do not match";

        if (userRepository.findByUsername(username).isPresent())
            return "Username already exists";

        if (userRepository.findByEmail(email).isPresent())
            return "Email already exists";

        // Save user as disabled until email verified
        user.setName(name);
        user.setUsername(username);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setAddress(address);
        user.setPincode(pincode);
        user.setPassword(passwordEncoder.encode(password));
        user.setBalance(0.0);
        user.setEnabled(false);        // disabled until OTP verified
        user.setEmailVerified(false);

        // Generate OTP
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setOtpPurpose("SIGNUP");

        userRepository.save(user);

        // Send OTP email
        emailService.sendSignupOtp(email, name, otp);

        return "otp_sent:" + username;
    }

    // ── Verify signup OTP ───────────────────────────────────
    public String verifySignupOtp(String username, String inputOtp) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";

        if (!"SIGNUP".equals(user.getOtpPurpose()))
            return "Invalid OTP request";

        if (!user.isOtpValid(inputOtp))
            return "Invalid or expired OTP. Please try again";

        // Activate account
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setOtpPurpose(null);
        userRepository.save(user);

        return "success";
    }

    // ── Resend signup OTP ───────────────────────────────────
    public String resendSignupOtp(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";
        if (user.isEmailVerified()) return "Email already verified";

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setOtpPurpose("SIGNUP");
        userRepository.save(user);

        emailService.sendSignupOtp(user.getEmail(), user.getName(), otp);
        return "success";
    }

    // ── Generate and send 2FA login OTP ────────────────────
    public String sendLoginOtp(String username, String ip) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setOtpPurpose("LOGIN");
        userRepository.save(user);

        emailService.sendLoginOtp(user.getEmail(), user.getName(), otp, ip);
        return "success";
    }

    // ── Verify login OTP ────────────────────────────────────
    public String verifyLoginOtp(String username, String inputOtp) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";

        if (!"LOGIN".equals(user.getOtpPurpose()))
            return "Invalid OTP request";

        if (!user.isOtpValid(inputOtp))
            return "Invalid or expired OTP";

        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setOtpPurpose(null);
        userRepository.save(user);

        return "success";
    }

    // ── Get user ────────────────────────────────────────────
    public User getUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    // ── Update profile ──────────────────────────────────────
    public String updateProfile(String currentUsername, User updatedUser) {
        User existing = userRepository.findByUsername(currentUsername).orElse(null);
        if (existing == null) return "User not found";

        String name    = normalize(updatedUser.getName());
        String email   = normalize(updatedUser.getEmail());
        String mobile  = normalize(updatedUser.getMobile());
        String address = normalize(updatedUser.getAddress());
        String pincode = normalize(updatedUser.getPincode());

        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() ||
                address.isEmpty() || pincode.isEmpty())
            return "All fields are required";

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
            return "Enter a valid email address";

        if (!mobile.matches("\\d{10}"))
            return "Mobile number must be exactly 10 digits";

        if (!pincode.matches("\\d{6}"))
            return "Pin code must be exactly 6 digits";

        if (userRepository.findByEmail(email)
                .filter(u -> !u.getId().equals(existing.getId())).isPresent())
            return "Email already exists";

        existing.setName(name);
        existing.setEmail(email);
        existing.setMobile(mobile);
        existing.setAddress(address);
        existing.setPincode(pincode);
        userRepository.save(existing);
        return "success";
    }

    // ── Password reset token ────────────────────────────────
    public String createResetToken(String username, String email) {
        User user = userRepository.findByUsername(normalize(username)).orElse(null);
        if (user == null || !user.getEmail().equalsIgnoreCase(normalize(email)))
            return null;

        String token = UUID.randomUUID().toString().replace("-", "");
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        return token;
    }

    public String resetPassword(String token, String newPassword, String confirmPassword) {
        User user = userRepository.findByResetToken(token).orElse(null);
        if (user == null || user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now()))
            return "Reset link is invalid or expired";

        String pass = newPassword == null ? "" : newPassword.trim();
        String conf = confirmPassword == null ? "" : confirmPassword.trim();

        if (pass.length() < 8)  return "Password must be at least 8 characters";
        if (!pass.equals(conf)) return "Passwords do not match";

        user.setPassword(passwordEncoder.encode(pass));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return "success";
    }

    // ── Change password ─────────────────────────────────────
    public String changePassword(String username, String currentPassword,
                                 String newPassword, String confirmPassword) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";

        String curr = currentPassword == null ? "" : currentPassword.trim();
        String next  = newPassword == null ? "" : newPassword.trim();
        String conf  = confirmPassword == null ? "" : confirmPassword.trim();

        if (!passwordEncoder.matches(curr, user.getPassword()))
            return "Current password is incorrect";
        if (next.length() < 8)
            return "New password must be at least 8 characters";
        if (!next.equals(conf))
            return "Passwords do not match";
        if (passwordEncoder.matches(next, user.getPassword()))
            return "New password must be different from current password";

        user.setPassword(passwordEncoder.encode(next));
        userRepository.save(user);
        return "success";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}