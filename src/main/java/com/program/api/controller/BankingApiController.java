package com.program.api.controller;

import com.program.api.dto.*;
import com.program.entity.Transactions;
import com.program.entity.User;
import com.program.repository.UserRepository;
import com.program.service.TransactionService;
import com.program.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API layer for Secure Bank.
 * All endpoints return JSON. Base path: /api
 *
 * Authentication: session-based (same login as the web app).
 * Call POST /login first, then use the session cookie.
 */
@RestController
@RequestMapping("/api")
public class BankingApiController {

    @Autowired private UserService userService;
    @Autowired private TransactionService transactionService;
    @Autowired private UserRepository userRepository;

    // ── Helper: get logged-in username from session ─────────
    private String getUsername(HttpSession session) {
        return (String) session.getAttribute("loggedInUser");
    }

    private ResponseEntity<ApiResponse<Void>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Not authenticated. Please login first."));
    }

    // ════════════════════════════════════════════════════════
    // GET /api/account
    // Returns the logged-in user's account info
    // ════════════════════════════════════════════════════════
    @GetMapping("/account")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccount(HttpSession session) {
        String username = getUsername(session);
        if (username == null) return ResponseEntity.status(401)
                .body(ApiResponse.error("Not authenticated"));

        User user = userService.getUser(username);
        if (user == null) return ResponseEntity.status(404)
                .body(ApiResponse.error("User not found"));

        return ResponseEntity.ok(ApiResponse.ok("Account fetched successfully", AccountDTO.from(user)));
    }

    // ════════════════════════════════════════════════════════
    // GET /api/balance
    // Returns just the balance
    // ════════════════════════════════════════════════════════
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Double>> getBalance(HttpSession session) {
        String username = getUsername(session);
        if (username == null) return ResponseEntity.status(401)
                .body(ApiResponse.error("Not authenticated"));

        User user = userService.getUser(username);
        if (user == null) return ResponseEntity.status(404)
                .body(ApiResponse.error("User not found"));

        return ResponseEntity.ok(ApiResponse.ok("Balance fetched", user.getBalance()));
    }

    // ════════════════════════════════════════════════════════
    // GET /api/transactions
    // Returns transaction history. Optional ?type=CREDIT&page=0
    // ════════════════════════════════════════════════════════
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactions(
            HttpSession session,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page) {

        String username = getUsername(session);
        if (username == null) return ResponseEntity.status(401)
                .body(ApiResponse.error("Not authenticated"));

        List<Transactions> txList = transactionService
                .getTransactionsPaged(username, type, query, page)
                .getContent();

        List<TransactionDTO> dtos = txList.stream()
                .map(TransactionDTO::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Transactions fetched", dtos));
    }

    // ════════════════════════════════════════════════════════
    // POST /api/credit
    // Body: { "amount": 500.0 }
    // ════════════════════════════════════════════════════════
    @PostMapping("/credit")
    public ResponseEntity<ApiResponse<AccountDTO>> credit(
            @RequestBody CreditRequest req,
            HttpSession session) {

        String username = getUsername(session);
        if (username == null) return ResponseEntity.status(401)
                .body(ApiResponse.error("Not authenticated"));

        if (req.getAmount() <= 0)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Amount must be greater than 0"));

        String result = transactionService.credit(username, req.getAmount());
        if (!"success".equals(result))
            return ResponseEntity.badRequest().body(ApiResponse.error(result));

        User updated = userService.getUser(username);
        return ResponseEntity.ok(ApiResponse.ok(
                "Rs " + req.getAmount() + " credited successfully", AccountDTO.from(updated)));
    }

    // ════════════════════════════════════════════════════════
    // POST /api/debit
    // Body: { "amount": 200.0 }
    // ════════════════════════════════════════════════════════
    @PostMapping("/debit")
    public ResponseEntity<ApiResponse<AccountDTO>> debit(
            @RequestBody DebitRequest req,
            HttpSession session) {

        String username = getUsername(session);
        if (username == null) return ResponseEntity.status(401)
                .body(ApiResponse.error("Not authenticated"));

        if (req.getAmount() <= 0)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Amount must be greater than 0"));

        String result = transactionService.debit(username, req.getAmount());
        if (!"success".equals(result))
            return ResponseEntity.badRequest().body(ApiResponse.error(result));

        User updated = userService.getUser(username);
        return ResponseEntity.ok(ApiResponse.ok(
                "Rs " + req.getAmount() + " debited successfully", AccountDTO.from(updated)));
    }

    // ════════════════════════════════════════════════════════
    // POST /api/transfer
    // Body: { "toUsername": "alex_payee", "amount": 100.0 }
    // ════════════════════════════════════════════════════════
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<AccountDTO>> transfer(
            @RequestBody TransferRequest req,
            HttpSession session) {

        String username = getUsername(session);
        if (username == null) return ResponseEntity.status(401)
                .body(ApiResponse.error("Not authenticated"));

        if (req.getAmount() <= 0)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Amount must be greater than 0"));

        if (req.getToUsername() == null || req.getToUsername().isBlank())
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Recipient username is required"));

        String result = transactionService.transfer(username, req.getToUsername(), req.getAmount());
        if (!"success".equals(result))
            return ResponseEntity.badRequest().body(ApiResponse.error(result));

        User updated = userService.getUser(username);
        return ResponseEntity.ok(ApiResponse.ok(
                "Rs " + req.getAmount() + " transferred to " + req.getToUsername(),
                AccountDTO.from(updated)));
    }

    // ════════════════════════════════════════════════════════
    // GET /api/users/check?username=abc
    // Check if a username exists (useful for transfer validation)
    // ════════════════════════════════════════════════════════
    @GetMapping("/users/check")
    public ResponseEntity<ApiResponse<Boolean>> checkUser(
            @RequestParam String username,
            HttpSession session) {

        if (getUsername(session) == null) return ResponseEntity.status(401)
                .body(ApiResponse.error("Not authenticated"));

        boolean exists = userRepository.findByUsername(username).isPresent();
        return ResponseEntity.ok(ApiResponse.ok(
                exists ? "User found" : "User not found", exists));
    }
}