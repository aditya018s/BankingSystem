package com.program.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import com.program.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.program.entity.Transactions;
import com.program.entity.User;
import com.program.repository.TransactionRepository;
import com.program.repository.UserRepository;

@Service
public class TransactionService {

    private static final int PAGE_SIZE = 10;

    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private EmailService emailService;

    // ── Credit ──────────────────────────────────────────────
    @Transactional
    public String credit(String username, double amount) {
        if (amount <= 0)    return "Amount must be greater than 0";
        if (amount < 500)   return "Minimum deposit amount is ₹500";
        if (amount > 100000) return "Maximum deposit per transaction is ₹1,00,000";
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";

        user.setBalance(user.getBalance() + amount);
        userRepository.save(user);

        Transactions tx = new Transactions();
        tx.setUser(user); tx.setType("CREDIT"); tx.setAmount(amount);
        tx.setReferenceNumber(generateReference("CR"));
        tx.setDetails("Money added to account wallet");
        tx.setDate(LocalDateTime.now());
        transactionRepository.save(tx);
        emailService.sendTransactionNotification(user, tx);
        return "success";
    }

    // ── Debit ───────────────────────────────────────────────
    @Transactional
    public String debit(String username, double amount) {
        if (amount <= 0)    return "Amount must be greater than 0";
        if (amount < 500)   return "Minimum withdrawal amount is ₹500";
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";
        if (user.getBalance() < amount) return "Insufficient balance";

        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);

        Transactions tx = new Transactions();
        tx.setUser(user); tx.setType("DEBIT"); tx.setAmount(amount);
        tx.setReferenceNumber(generateReference("DB"));
        tx.setDetails("Withdrawal from available balance");
        tx.setDate(LocalDateTime.now());
        transactionRepository.save(tx);
        emailService.sendTransactionNotification(user, tx);
        return "success";
    }

    // ── Transfer ────────────────────────────────────────────
    @Transactional
    public String transfer(String fromUsername, String toUsername, double amount) {
        if (fromUsername == null || toUsername == null) return "User not found";

        String sender   = fromUsername.trim();
        String receiver = toUsername.trim();

        if (amount <= 0)    return "Amount must be greater than 0";
        if (amount < 500)   return "Minimum transfer amount is ₹500";
        if (sender.isEmpty() || receiver.isEmpty())   return "User not found";
        if (sender.equalsIgnoreCase(receiver))        return "Cannot transfer to yourself";

        User senderUser   = userRepository.findByUsername(sender).orElse(null);
        User receiverUser = userRepository.findByUsername(receiver).orElse(null);

        if (senderUser == null || receiverUser == null) return "User not found";
        if (senderUser.getBalance() < amount)           return "Insufficient balance";

        String ref = generateReference("TRX");

        senderUser.setBalance(senderUser.getBalance() - amount);
        receiverUser.setBalance(receiverUser.getBalance() + amount);
        userRepository.save(senderUser);
        userRepository.save(receiverUser);

        Transactions debit = new Transactions();
        debit.setUser(senderUser); debit.setType("TRANSFER_OUT"); debit.setAmount(amount);
        debit.setReferenceNumber(ref);
        debit.setDetails("Transfer sent to " + receiverUser.getUsername());
        debit.setDate(LocalDateTime.now());

        Transactions credit = new Transactions();
        credit.setUser(receiverUser); credit.setType("TRANSFER_IN"); credit.setAmount(amount);
        credit.setReferenceNumber(ref);
        credit.setDetails("Transfer received from " + senderUser.getUsername());
        credit.setDate(LocalDateTime.now());

        transactionRepository.save(debit);
        transactionRepository.save(credit);
        emailService.sendTransactionNotification(senderUser, debit);
        emailService.sendTransactionNotification(receiverUser, credit);
        return "success";
    }

    // ── Fetch (non-paginated, used by dashboard) ───────────
    public List<Transactions> getTransactions(String username) {
        return transactionRepository.findByUserUsernameOrderByDateDesc(username);
    }

    // ── Fetch with type+query filter (non-paginated) ───────
    public List<Transactions> getTransactions(String username, String type, String query) {
        return getTransactions(username).stream()
                .filter(t -> matchesType(t, type))
                .filter(t -> matchesQuery(t, query))
                .collect(Collectors.toList());
    }

    // ── Paginated fetch with filter ────────────────────────
    public Page<Transactions> getTransactionsPaged(
            String username, String type, String query, int page) {

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        boolean hasType  = type  != null && !type.isBlank();
        boolean hasQuery = query != null && !query.isBlank();

        if (hasType && hasQuery) {
            return transactionRepository.searchByUsernameAndType(
                    username, type.trim(), query.trim(), pageable);
        } else if (hasType) {
            return transactionRepository.findByUserUsernameAndTypeOrderByDateDesc(
                    username, type.trim(), pageable);
        } else if (hasQuery) {
            return transactionRepository.searchByUsername(
                    username, query.trim(), pageable);
        } else {
            return transactionRepository.findByUserUsernameOrderByDateDesc(
                    username, pageable);
        }
    }

    private boolean matchesType(Transactions t, String type) {
        if (type == null || type.isBlank()) return true;
        return t.getType() != null && t.getType().equalsIgnoreCase(type.trim());
    }

    private boolean matchesQuery(Transactions t, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase();
        return (t.getReferenceNumber() != null && t.getReferenceNumber().toLowerCase().contains(q))
                || (t.getDetails()         != null && t.getDetails().toLowerCase().contains(q))
                || (t.getType()            != null && t.getType().toLowerCase().contains(q));
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}