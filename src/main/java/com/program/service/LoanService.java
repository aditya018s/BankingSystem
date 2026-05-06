package com.program.service;

import com.program.entity.Loan;
import com.program.entity.LoanPayment;
import com.program.entity.User;
import com.program.repository.LoanPaymentRepository;
import com.program.repository.LoanRepository;
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

    private static final double ANNUAL_INTEREST_RATE = 10.5;

    // ── Calculate EMI ───────────────────────────────────────
    // Formula: EMI = P * r * (1+r)^n / ((1+r)^n - 1)
    public double calculateEmi(double principal, int tenureMonths) {
        double monthlyRate = ANNUAL_INTEREST_RATE / 12 / 100;
        double power = Math.pow(1 + monthlyRate, tenureMonths);
        return Math.round((principal * monthlyRate * power / (power - 1)) * 100.0) / 100.0;
    }

    public double calculateTotalPayable(double emi, int tenure) {
        return Math.round(emi * tenure * 100.0) / 100.0;
    }

    // ── Apply for loan ──────────────────────────────────────
    @Transactional
    public String applyLoan(String username, double amount,
                            int tenureMonths, Loan.Purpose purpose) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";
        if (amount < 1000)  return "Minimum loan amount is Rs 1,000";
        if (amount > 1000000) return "Maximum loan amount is Rs 10,00,000";
        if (tenureMonths < 3)  return "Minimum tenure is 3 months";
        if (tenureMonths > 360) return "Maximum tenure is 360 months (30 years)";

        // Max 2 active loans at a time
        long activeLoans = loanRepository.countActiveLoans(user);
        if (activeLoans >= 2) return "You already have 2 active loans. Please close existing loans first";

        double emi          = calculateEmi(amount, tenureMonths);
        double totalPayable = calculateTotalPayable(emi, tenureMonths);

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setAmount(amount);
        loan.setTenureMonths(tenureMonths);
        loan.setInterestRate(ANNUAL_INTEREST_RATE);
        loan.setEmiAmount(emi);
        loan.setTotalPayable(totalPayable);
        loan.setRemainingAmount(totalPayable);
        loan.setPurpose(purpose);
        loan.setStatus(Loan.Status.PENDING);
        loan.setLoanNumber(generateLoanNumber());

        loanRepository.save(loan);
        return "success";
    }

    // ── Get user loans ──────────────────────────────────────
    public List<Loan> getUserLoans(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return List.of();
        return loanRepository.findByUserOrderByAppliedAtDesc(user);
    }

    // ── Get single loan ─────────────────────────────────────
    public Loan getLoan(Long id) {
        return loanRepository.findById(id).orElse(null);
    }

    // ── Get payments for a loan ─────────────────────────────
    public List<LoanPayment> getLoanPayments(Long loanId) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null) return List.of();
        return loanPaymentRepository.findByLoanOrderByEmiNumberAsc(loan);
    }

    // ── Pay EMI ─────────────────────────────────────────────
    @Transactional
    public String payEmi(String username, Long loanId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "User not found";

        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null) return "Loan not found";
        if (!loan.getUser().getUsername().equals(username))
            return "Unauthorized";
        if (loan.getStatus() != Loan.Status.ACTIVE)
            return "Loan is not active";
        if (loan.getRemainingAmount() <= 0)
            return "Loan already fully paid";

        double emiToPay = Math.min(loan.getEmiAmount(), loan.getRemainingAmount());

        if (user.getBalance() < emiToPay)
            return "Insufficient balance. Your balance is Rs " +
                    String.format("%.2f", user.getBalance()) +
                    " but EMI is Rs " + String.format("%.2f", emiToPay);

        // Deduct from balance
        user.setBalance(user.getBalance() - emiToPay);
        userRepository.save(user);

        // Update loan
        loan.setRemainingAmount(
                Math.round((loan.getRemainingAmount() - emiToPay) * 100.0) / 100.0);

        int emiNumber = loanPaymentRepository.countByLoan(loan) + 1;

        // Save payment record
        LoanPayment payment = new LoanPayment();
        payment.setLoan(loan);
        payment.setUser(user);
        payment.setAmount(emiToPay);
        payment.setEmiNumber(emiNumber);
        payment.setReferenceNumber("EMI-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase());
        loanPaymentRepository.save(payment);

        // Close loan if fully paid
        if (loan.getRemainingAmount() <= 0) {
            loan.setStatus(Loan.Status.CLOSED);
            loan.setClosedAt(LocalDateTime.now());
        }

        loanRepository.save(loan);
        return "success";
    }

    // ── Admin: all loans ────────────────────────────────────
    public List<Loan> getAllLoans() {
        return loanRepository.findAllByOrderByAppliedAtDesc();
    }

    // ── Admin: approve loan ─────────────────────────────────
    @Transactional
    public String approveLoan(Long loanId, String remark) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null) return "Loan not found";
        if (loan.getStatus() != Loan.Status.PENDING)
            return "Only pending loans can be approved";

        // Credit loan amount to user's account
        User user = loan.getUser();
        user.setBalance(user.getBalance() + loan.getAmount());
        userRepository.save(user);

        loan.setStatus(Loan.Status.ACTIVE);
        loan.setAdminRemark(remark != null ? remark : "Approved");
        loan.setApprovedAt(LocalDateTime.now());
        loanRepository.save(loan);
        return "success";
    }

    // ── Admin: reject loan ──────────────────────────────────
    @Transactional
    public String rejectLoan(Long loanId, String remark) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null) return "Loan not found";
        if (loan.getStatus() != Loan.Status.PENDING)
            return "Only pending loans can be rejected";

        loan.setStatus(Loan.Status.REJECTED);
        loan.setAdminRemark(remark != null ? remark : "Rejected by admin");
        loanRepository.save(loan);
        return "success";
    }

    // ── Helpers ─────────────────────────────────────────────
    private String generateLoanNumber() {
        return "LN-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();
    }
}