package com.program.controller;

import com.program.entity.Loan;
import com.program.entity.LoanPayment;
import com.program.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/loans")
public class AdminLoanController {

    @Autowired private LoanService loanService;

    // ── All Loans ───────────────────────────────────────────
    @GetMapping
    public String allLoans(Model model) {
        List<Loan> loans = loanService.getAllLoans();

        long pending  = loans.stream().filter(l -> l.getStatus() == Loan.Status.PENDING).count();
        long active   = loans.stream().filter(l -> l.getStatus() == Loan.Status.ACTIVE).count();
        long closed   = loans.stream().filter(l -> l.getStatus() == Loan.Status.CLOSED).count();
        long rejected = loans.stream().filter(l -> l.getStatus() == Loan.Status.REJECTED).count();

        model.addAttribute("loans",    loans);
        model.addAttribute("pending",  pending);
        model.addAttribute("active",   active);
        model.addAttribute("closed",   closed);
        model.addAttribute("rejected", rejected);
        return "admin/loans";
    }

    // ── Feature 2: View EMI payments for a specific loan ────
    @GetMapping("/{id}/payments")
    public String loanPayments(@PathVariable Long id, Model model) {
        Loan loan = loanService.getLoan(id);
        if (loan == null) return "redirect:/admin/loans";

        List<LoanPayment> payments = loanService.getAdminLoanPayments(id);

        int paidEmis      = payments.size();
        int remainingEmis = Math.max(0, loan.getTenureMonths() - paidEmis);
        double amountPaid = payments.stream().mapToDouble(LoanPayment::getAmount).sum();

        model.addAttribute("loan",          loan);
        model.addAttribute("payments",      payments);
        model.addAttribute("paidEmis",      paidEmis);
        model.addAttribute("remainingEmis", remainingEmis);
        model.addAttribute("amountPaid",    amountPaid);
        return "admin/loan-payments";
    }

    // ── Approve ─────────────────────────────────────────────
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String remark,
                          RedirectAttributes ra) {
        String result = loanService.approveLoan(id, remark);
        if ("success".equals(result)) {
            ra.addFlashAttribute("success",
                    "✅ Loan approved! Amount credited to user's account.");
        } else {
            ra.addFlashAttribute("error", result);
        }
        return "redirect:/admin/loans";
    }

    // ── Reject ──────────────────────────────────────────────
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String remark,
                         RedirectAttributes ra) {
        String result = loanService.rejectLoan(id, remark);
        if ("success".equals(result)) {
            ra.addFlashAttribute("success", "Loan application rejected.");
        } else {
            ra.addFlashAttribute("error", result);
        }
        return "redirect:/admin/loans";
    }
}