package com.program.service;

import com.program.entity.Loan;
import com.program.entity.LoanPayment;
import com.program.entity.Transactions;
import com.program.entity.User;
import com.program.repository.LoanPaymentRepository;
import com.program.repository.LoanRepository;
import com.program.repository.TransactionRepository;
import com.program.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LoanService {

    @Autowired private LoanRepository loanRepository;
    @Autowired private LoanPaymentRepository loanPaymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private EmailService emailService;

    private static final double ANNUAL_INTEREST_RATE = 10.5;

    // ── EMI Calculation ─────────────────────────────────────
    public double calculateEmi(double principal, int tenureMonths) {
        double r     = ANNUAL_INTEREST_RATE / 12 / 100;
        double power = Math.pow(1 + r, tenureMonths);
        return Math.round((principal * r * power / (power - 1)) * 100.0) / 100.0;
    }

    public double calculateTotalPayable(double emi, int tenure) {
        return Math.round(emi * tenure * 100.0) / 100.0;
    }

    // ── Apply for loan ──────────────────────────────────────
    @Transactional
    public String applyLoan(String username, double amount,
                            int tenureMonths, Loan.Purpose purpose) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null)       return "User not found";
        if (amount < 1000)      return "Minimum loan amount is ₹1,000";
        if (amount > 1000000)   return "Maximum loan amount is ₹10,00,000";
        if (tenureMonths < 3)   return "Minimum tenure is 3 months";
        if (tenureMonths > 360) return "Maximum tenure is 360 months";

        long active = loanRepository.countActiveLoans(user);
        if (active >= 2) return "You already have 2 active loans";

        double emi   = calculateEmi(amount, tenureMonths);
        double total = calculateTotalPayable(emi, tenureMonths);

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setAmount(amount);
        loan.setTenureMonths(tenureMonths);
        loan.setInterestRate(ANNUAL_INTEREST_RATE);
        loan.setEmiAmount(emi);
        loan.setTotalPayable(total);
        loan.setRemainingAmount(total);
        loan.setPurpose(purpose);
        loan.setStatus(Loan.Status.PENDING);
        loan.setLoanNumber("LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        loanRepository.save(loan);
        return "success";
    }

    // ── Get loans ───────────────────────────────────────────
    public List<Loan> getUserLoans(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return List.of();
        return loanRepository.findByUserOrderByAppliedAtDesc(user);
    }

    public Loan getLoan(Long id) {
        return loanRepository.findById(id).orElse(null);
    }

    public List<LoanPayment> getLoanPayments(Long loanId) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null) return List.of();
        return loanPaymentRepository.findByLoanOrderByEmiNumberAsc(loan);
    }

    // ── Feature 2: Admin view payments ──────────────────────
    public List<LoanPayment> getAdminLoanPayments(Long loanId) {
        return getLoanPayments(loanId);
    }

    // ── Feature 1 + 3 + 4: Pay ONE EMI ─────────────────────
    // Records in BOTH loan_payments AND transactions tables
    // No monthly restriction — can pay multiple times
    @Transactional
    public String payEmi(String username, Long loanId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";

        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null)                                    return "Loan not found";
        if (!loan.getUser().getUsername().equals(username))  return "Unauthorized";
        if (loan.getStatus() != Loan.Status.ACTIVE)          return "Loan is not active";
        if (loan.getRemainingAmount() <= 0.01)               return "Loan is already fully paid";

        double emiToPay = Math.min(loan.getEmiAmount(), loan.getRemainingAmount());

        if (user.getBalance() < emiToPay)
            return "Insufficient balance. Available: ₹" +
                    String.format("%.2f", user.getBalance()) +
                    " · EMI: ₹" + String.format("%.2f", emiToPay);

        String ref = "EMI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Deduct from balance
        user.setBalance(Math.round((user.getBalance() - emiToPay) * 100.0) / 100.0);
        userRepository.save(user);

        // Update loan
        loan.setRemainingAmount(Math.round((loan.getRemainingAmount() - emiToPay) * 100.0) / 100.0);
        int emiNumber = loanPaymentRepository.countByLoan(loan) + 1;

        // Feature 1: Save as Transaction (visible in transaction history)
        Transactions tx = new Transactions();
        tx.setUser(user);
        tx.setType("EMI_PAYMENT");
        tx.setAmount(emiToPay);
        tx.setReferenceNumber(ref);
        tx.setDetails("EMI #" + emiNumber + " — " + loan.getLoanNumber()
                + " (" + loan.getPurpose() + ")");
        tx.setDate(LocalDateTime.now());
        transactionRepository.save(tx);

        // Save LoanPayment record
        LoanPayment payment = new LoanPayment();
        payment.setLoan(loan);
        payment.setUser(user);
        payment.setAmount(emiToPay);
        payment.setEmiNumber(emiNumber);
        payment.setReferenceNumber(ref);
        loanPaymentRepository.save(payment);

        // Auto-close if fully paid
        if (loan.getRemainingAmount() <= 0.01) {
            loan.setStatus(Loan.Status.CLOSED);
            loan.setClosedAt(LocalDateTime.now());
        }
        loanRepository.save(loan);

        // Email notification
        emailService.sendTransactionNotification(user, tx);

        return "success";
    }

    // ── Feature 5: Pay ALL remaining EMIs at once ───────────
    @Transactional
    public String payAllEmi(String username, Long loanId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";

        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null)                                    return "Loan not found";
        if (!loan.getUser().getUsername().equals(username))  return "Unauthorized";
        if (loan.getStatus() != Loan.Status.ACTIVE)          return "Loan is not active";
        if (loan.getRemainingAmount() <= 0.01)               return "Loan is already fully paid";

        double totalDue = loan.getRemainingAmount();

        if (user.getBalance() < totalDue)
            return "Insufficient balance. Available: ₹" +
                    String.format("%.2f", user.getBalance()) +
                    " · Total due: ₹" + String.format("%.2f", totalDue);

        String ref = "FULL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Deduct full amount
        user.setBalance(Math.round((user.getBalance() - totalDue) * 100.0) / 100.0);
        userRepository.save(user);

        int emiNumber = loanPaymentRepository.countByLoan(loan) + 1;

        // Save as Transaction
        Transactions tx = new Transactions();
        tx.setUser(user);
        tx.setType("EMI_PAYMENT");
        tx.setAmount(totalDue);
        tx.setReferenceNumber(ref);
        tx.setDetails("Full loan closure — all remaining EMIs paid. Loan: "
                + loan.getLoanNumber() + " (" + loan.getPurpose() + "). "
                + "Outstanding amount ₹" + String.format("%.2f", totalDue)
                + " deducted in one transaction. Loan account closed immediately.");
        tx.setDate(LocalDateTime.now());
        transactionRepository.save(tx);

        // Save LoanPayment
        LoanPayment payment = new LoanPayment();
        payment.setLoan(loan);
        payment.setUser(user);
        payment.setAmount(totalDue);
        payment.setEmiNumber(emiNumber);
        payment.setReferenceNumber(ref);
        loanPaymentRepository.save(payment);

        // Close loan
        loan.setRemainingAmount(0);
        loan.setStatus(Loan.Status.CLOSED);
        loan.setClosedAt(LocalDateTime.now());
        loanRepository.save(loan);

        emailService.sendTransactionNotification(user, tx);

        return "success";
    }

    // ── All loans (admin) ───────────────────────────────────
    public List<Loan> getAllLoans() {
        return loanRepository.findAllByOrderByAppliedAtDesc();
    }

    // ── Admin: approve ──────────────────────────────────────
    @Transactional
    public String approveLoan(Long loanId, String remark) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null) return "Loan not found";
        if (loan.getStatus() != Loan.Status.PENDING) return "Only pending loans can be approved";

        User user = loan.getUser();
        user.setBalance(user.getBalance() + loan.getAmount());
        userRepository.save(user);

        // Credit transaction for loan disbursement
        Transactions tx = new Transactions();
        tx.setUser(user);
        tx.setType("CREDIT");
        tx.setAmount(loan.getAmount());
        tx.setReferenceNumber("LOAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setDetails("Loan disbursed — " + loan.getLoanNumber() + " (" + loan.getPurpose() + ")");
        tx.setDate(LocalDateTime.now());
        transactionRepository.save(tx);

        loan.setStatus(Loan.Status.ACTIVE);
        loan.setAdminRemark(remark != null && !remark.isBlank() ? remark : "Approved");
        loan.setApprovedAt(LocalDateTime.now());
        loanRepository.save(loan);

        emailService.sendTransactionNotification(user, tx);
        return "success";
    }

    // ── Admin: reject ───────────────────────────────────────
    @Transactional
    public String rejectLoan(Long loanId, String remark) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null) return "Loan not found";
        if (loan.getStatus() != Loan.Status.PENDING) return "Only pending loans can be rejected";

        loan.setStatus(Loan.Status.REJECTED);
        loan.setAdminRemark(remark != null && !remark.isBlank() ? remark : "Rejected by admin");
        loanRepository.save(loan);
        return "success";
    }
}