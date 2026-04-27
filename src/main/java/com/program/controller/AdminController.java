package com.program.controller;

import com.program.entity.Transactions;
import com.program.entity.User;
import com.program.repository.TransactionRepository;
import com.program.repository.UserRepository;
import com.program.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private TransactionService transactionService;
    @Autowired private PasswordEncoder passwordEncoder;

    // ── Dashboard ──────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> users = userRepository.findAll();
        List<Transactions> transactions = transactionRepository.findAll();

        long regularUsers = users.stream()
                .filter(u -> !"ROLE_ADMIN".equals(u.getRole()))
                .count();

        double totalBalance = users.stream()
                .filter(u -> !"ROLE_ADMIN".equals(u.getRole()))
                .mapToDouble(User::getBalance)
                .sum();

        model.addAttribute("totalUsers", regularUsers);
        model.addAttribute("totalTransactions", transactions.size());
        model.addAttribute("totalBalance", totalBalance);
        return "admin/dashboard";
    }

    // ── All Users ──────────────────────────────────────────────────────────
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    // ── All Transactions ───────────────────────────────────────────────────
    @GetMapping("/transactions")
    public String transactions(Model model) {
        // Most recent first
        List<Transactions> all = transactionRepository.findAll();
        all.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        model.addAttribute("transactions", all);
        return "admin/transactions";
    }

    // ── Block / Unblock User ───────────────────────────────────────────────
    @PostMapping("/users/{id}/toggle-block")
    public String toggleBlock(@PathVariable Long id, RedirectAttributes ra) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null || "ROLE_ADMIN".equals(user.getRole())) {
            ra.addFlashAttribute("error", "Cannot modify this user");
            return "redirect:/admin/users";
        }

        boolean nowEnabled = !user.isEnabled(); // toggled state
        user.setEnabled(nowEnabled);
        userRepository.save(user);

        // Message reflects what just happened
        ra.addFlashAttribute("success",
                nowEnabled
                        ? "User '" + user.getUsername() + "' has been unblocked."
                        : "User '" + user.getUsername() + "' has been blocked.");

        return "redirect:/admin/users";
    }

    // ── Delete User ────────────────────────────────────────────────────────
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null || "ROLE_ADMIN".equals(user.getRole())) {
            ra.addFlashAttribute("error", "Cannot delete this user");
            return "redirect:/admin/users";
        }

        // Delete transactions first (foreign key)
        List<Transactions> userTxs = transactionRepository
                .findByUserUsernameOrderByDateDesc(user.getUsername());
        transactionRepository.deleteAll(userTxs);
        userRepository.delete(user);

        ra.addFlashAttribute("success", "User '" + user.getUsername() + "' deleted successfully.");
        return "redirect:/admin/users";
    }

    // ── Change User Password ───────────────────────────────────────────────
    @PostMapping("/users/{id}/change-password")
    public String changePassword(@PathVariable Long id,
                                 @RequestParam String newPassword,
                                 RedirectAttributes ra) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        if (newPassword == null || newPassword.trim().length() < 8) {
            ra.addFlashAttribute("error", "Password must be at least 8 characters");
            return "redirect:/admin/users";
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
        ra.addFlashAttribute("success", "Password updated for '" + user.getUsername() + "'.");
        return "redirect:/admin/users";
    }

    // ── Admin Credit User ──────────────────────────────────────────────────
    @PostMapping("/users/{id}/credit")
    public String creditUser(@PathVariable Long id,
                             @RequestParam double amount,
                             RedirectAttributes ra) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        String result = transactionService.credit(user.getUsername(), amount);
        if ("success".equals(result)) {
            ra.addFlashAttribute("success",
                    "₹" + amount + " credited to '" + user.getUsername() + "'.");
        } else {
            ra.addFlashAttribute("error", result);
        }
        return "redirect:/admin/users";
    }

    // ── Admin Debit User ───────────────────────────────────────────────────
    @PostMapping("/users/{id}/debit")
    public String debitUser(@PathVariable Long id,
                            @RequestParam double amount,
                            RedirectAttributes ra) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        String result = transactionService.debit(user.getUsername(), amount);
        if ("success".equals(result)) {
            ra.addFlashAttribute("success",
                    "₹" + amount + " debited from '" + user.getUsername() + "'.");
        } else {
            ra.addFlashAttribute("error", result);
        }
        return "redirect:/admin/users";
    }
}