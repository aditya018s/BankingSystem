package com.program.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.program.entity.User;
import com.program.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String registerUser(User user, String confirmPassword) {
        String name = normalize(user.getName());
        String username = normalize(user.getUsername());
        String email = normalize(user.getEmail());
        String mobile = normalize(user.getMobile());
        String address = normalize(user.getAddress());
        String pincode = normalize(user.getPincode());
        String password = user.getPassword() == null ? "" : user.getPassword().trim();
        String confirmedPassword = confirmPassword == null ? "" : confirmPassword.trim();

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || mobile.isEmpty()
                || address.isEmpty() || pincode.isEmpty() || password.isEmpty()) {
            return "All fields are required";
        }

        if (username.length() < 4 || !username.matches("[A-Za-z0-9_]+")) {
            return "Username must be at least 4 characters and use only letters, numbers, or underscore";
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return "Enter a valid email address";
        }

        if (!mobile.matches("\\d{10}")) {
            return "Mobile number must be exactly 10 digits";
        }

        if (!pincode.matches("\\d{6}")) {
            return "Pin code must be exactly 6 digits";
        }

        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }

        if (!password.equals(confirmedPassword)) {
            return "Passwords do not match";
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return "Username already exists";
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return "Email already exists";
        }

        user.setName(name);
        user.setUsername(username);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setAddress(address);
        user.setPincode(pincode);
        user.setPassword(passwordEncoder.encode(password));
        user.setBalance(0.0);

        userRepository.save(user);

        return "success";
    }

    public User getUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public String updateProfile(String currentUsername, User updatedUser) {
        User existingUser = userRepository.findByUsername(currentUsername).orElse(null);
        if (existingUser == null) {
            return "User not found";
        }

        String name = normalize(updatedUser.getName());
        String email = normalize(updatedUser.getEmail());
        String mobile = normalize(updatedUser.getMobile());
        String address = normalize(updatedUser.getAddress());
        String pincode = normalize(updatedUser.getPincode());

        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() || address.isEmpty() || pincode.isEmpty()) {
            return "All fields are required";
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return "Enter a valid email address";
        }

        if (!mobile.matches("\\d{10}")) {
            return "Mobile number must be exactly 10 digits";
        }

        if (!pincode.matches("\\d{6}")) {
            return "Pin code must be exactly 6 digits";
        }

        if (userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(existingUser.getId()))
                .isPresent()) {
            return "Email already exists";
        }

        existingUser.setName(name);
        existingUser.setEmail(email);
        existingUser.setMobile(mobile);
        existingUser.setAddress(address);
        existingUser.setPincode(pincode);
        userRepository.save(existingUser);

        return "success";
    }

    public String createResetToken(String username, String email) {
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);

        User user = userRepository.findByUsername(normalizedUsername).orElse(null);
        if (user == null || !user.getEmail().equalsIgnoreCase(normalizedEmail)) {
            return null;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        return token;
    }

    public String resetPassword(String token, String newPassword, String confirmPassword) {
        User user = userRepository.findByResetToken(token).orElse(null);
        if (user == null || user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return "Reset link is invalid or expired";
        }

        String password = newPassword == null ? "" : newPassword.trim();
        String confirmed = confirmPassword == null ? "" : confirmPassword.trim();

        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }

        if (!password.equals(confirmed)) {
            return "Passwords do not match";
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return "success";
    }

    public String changePassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "User not found";
        }

        String current = currentPassword == null ? "" : currentPassword.trim();
        String next = newPassword == null ? "" : newPassword.trim();
        String confirmed = confirmPassword == null ? "" : confirmPassword.trim();

        if (!passwordEncoder.matches(current, user.getPassword())) {
            return "Current password is incorrect";
        }

        if (next.length() < 8) {
            return "New password must be at least 8 characters long";
        }

        if (!next.equals(confirmed)) {
            return "Passwords do not match";
        }

        if (passwordEncoder.matches(next, user.getPassword())) {
            return "New password must be different from current password";
        }

        user.setPassword(passwordEncoder.encode(next));
        userRepository.save(user);
        return "success";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
