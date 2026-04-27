package com.program.api.controller;

import com.program.api.dto.AccountDTO;
import com.program.api.dto.ApiResponse;
import com.program.api.dto.TransactionDTO;
import com.program.entity.Transactions;
import com.program.entity.User;
import com.program.repository.TransactionRepository;
import com.program.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-only REST API.
 * All endpoints require ROLE_ADMIN.
 * Base path: /api/admin
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;

    // ════════════════════════════════════════════════════════
    // GET /api/admin/users
    // Returns all users (excluding passwords)
    // ════════════════════════════════════════════════════════
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAllUsers() {
        List<AccountDTO> users = userRepository.findAll().stream()
                .map(AccountDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Users fetched", users));
    }

    // ════════════════════════════════════════════════════════
    // GET /api/admin/users/{id}
    // Returns a single user by ID
    // ════════════════════════════════════════════════════════
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(ApiResponse.ok("User found", AccountDTO.from(u))))
                .orElse(ResponseEntity.status(404).body(ApiResponse.error("User not found")));
    }

    // ════════════════════════════════════════════════════════
    // GET /api/admin/transactions
    // Returns all transactions across all users
    // ════════════════════════════════════════════════════════
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getAllTransactions() {
        List<TransactionDTO> txList = transactionRepository.findAll().stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .map(TransactionDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Transactions fetched", txList));
    }

    // ════════════════════════════════════════════════════════
    // GET /api/admin/stats
    // Returns system-wide stats
    // ════════════════════════════════════════════════════════
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getStats() {
        List<User> users = userRepository.findAll();
        List<Transactions> txList = transactionRepository.findAll();

        long regularUsers = users.stream()
                .filter(u -> !"ROLE_ADMIN".equals(u.getRole())).count();
        long activeUsers = users.stream()
                .filter(u -> u.isEnabled() && !"ROLE_ADMIN".equals(u.getRole())).count();
        double totalBalance = users.stream()
                .filter(u -> !"ROLE_ADMIN".equals(u.getRole()))
                .mapToDouble(User::getBalance).sum();

        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalUsers",        regularUsers);
        stats.put("activeUsers",       activeUsers);
        stats.put("totalTransactions", txList.size());
        stats.put("totalBalance",      totalBalance);

        return ResponseEntity.ok(ApiResponse.ok("Stats fetched", stats));
    }
}