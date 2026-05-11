package com.program.controller;

import com.program.entity.Loan;
import com.program.entity.LoanPayment;
import com.program.entity.User;
import com.program.repository.UserRepository;
import com.program.service.LoanService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/loans")
public class LoanController {

    @Autowired private LoanService loanService;
    @Autowired private UserRepository userRepository;

    // ── My Loans ────────────────────────────────────────────
    @GetMapping
    public String myLoans(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";

        User user = userRepository.findByUsername(username).orElse(null);
        List<Loan> loans = loanService.getUserLoans(username);

        model.addAttribute("user", user);
        model.addAttribute("loans", loans);
        model.addAttribute("purposes", Loan.Purpose.values());
        return "loan/my-loans";
    }

    // ── Apply ───────────────────────────────────────────────
    @GetMapping("/apply")
    public String applyPage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";
        User user = userRepository.findByUsername(username).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("purposes", Loan.Purpose.values());
        return "loan/apply";
    }

    @PostMapping("/apply")
    public String applyLoan(@RequestParam double amount,
                            @RequestParam int tenureMonths,
                            @RequestParam Loan.Purpose purpose,
                            HttpSession session, RedirectAttributes ra) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";

        String result = loanService.applyLoan(username, amount, tenureMonths, purpose);
        if ("success".equals(result)) {
            ra.addFlashAttribute("success",
                    "Loan application submitted! We'll review it shortly.");
            return "redirect:/loans";
        }
        ra.addFlashAttribute("error", result);
        return "redirect:/loans/apply";
    }

    // ── Loan Detail ─────────────────────────────────────────
    @GetMapping("/{id}")
    public String loanDetail(@PathVariable Long id,
                             HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";

        Loan loan = loanService.getLoan(id);
        if (loan == null || !loan.getUser().getUsername().equals(username))
            return "redirect:/loans";

        List<LoanPayment> payments = loanService.getLoanPayments(id);
        User user = userRepository.findByUsername(username).orElse(null);

        int paidEmis      = payments.size();
        int remainingEmis = Math.max(0, loan.getTenureMonths() - paidEmis);

        model.addAttribute("user",          user);
        model.addAttribute("loan",          loan);
        model.addAttribute("payments",      payments);
        model.addAttribute("paidEmis",      paidEmis);
        model.addAttribute("remainingEmis", remainingEmis);
        return "loan/detail";
    }

    // ── Pay ONE EMI ─────────────────────────────────────────
    @PostMapping("/{id}/pay-emi")
    public String payEmi(@PathVariable Long id,
                         HttpSession session, RedirectAttributes ra) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";

        String result = loanService.payEmi(username, id);
        if ("success".equals(result)) {
            ra.addFlashAttribute("success", "✅ EMI paid successfully!");
        } else {
            ra.addFlashAttribute("error", result);
        }
        return "redirect:/loans/" + id;
    }

    // ── Pay ALL EMIs at once (Feature 5) ────────────────────
    @PostMapping("/{id}/pay-all")
    public String payAll(@PathVariable Long id,
                         HttpSession session, RedirectAttributes ra) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";

        String result = loanService.payAllEmi(username, id);
        if ("success".equals(result)) {
            ra.addFlashAttribute("success",
                    "🎉 Congratulations! All EMIs paid. Your loan is now closed!");
        } else {
            ra.addFlashAttribute("error", result);
        }
        return "redirect:/loans/" + id;
    }

    // ── EMI Calculator ──────────────────────────────────────
    @GetMapping("/calculator")
    public String calculator(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        User user = username != null ?
                userRepository.findByUsername(username).orElse(null) : null;
        model.addAttribute("user", user);
        return "loan/calculator";
    }

    // ── AJAX EMI Calculation ─────────────────────────────────
    @GetMapping("/calculate-emi")
    @ResponseBody
    public java.util.Map<String, Object> calculateEmi(
            @RequestParam double amount,
            @RequestParam int tenure) {
        double emi      = loanService.calculateEmi(amount, tenure);
        double total    = loanService.calculateTotalPayable(emi, tenure);
        double interest = Math.round((total - amount) * 100.0) / 100.0;

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("emi",           emi);
        result.put("totalPayable",  total);
        result.put("totalInterest", interest);
        result.put("principal",     amount);
        return result;
    }
}