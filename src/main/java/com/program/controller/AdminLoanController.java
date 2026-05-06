package com.program.controller;

import com.program.entity.Loan;
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

    // ── All loans ───────────────────────────────────────────
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

    // ── Approve ─────────────────────────────────────────────
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String remark,
                          RedirectAttributes ra) {
        String result = loanService.approveLoan(id, remark);
        if ("success".equals(result)) {
            ra.addFlashAttribute("success",
                    "Loan approved! Amount credited to user's account.");
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