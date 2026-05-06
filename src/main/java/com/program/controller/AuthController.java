package com.program.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.program.entity.Transactions;
import com.program.entity.User;
import com.program.repository.UserRepository;
import com.program.service.TransactionService;
import com.program.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private TransactionService transactionService;
    @Autowired private UserRepository userRepository;

    @ModelAttribute
    public void setCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    @ModelAttribute
    public void syncSecuritySession(HttpSession session, Principal principal) {
        if (principal != null && session.getAttribute("loggedInUser") == null) {
            User user = userService.getUser(principal.getName());
            if (user != null) {
                session.setAttribute("loggedInUser", user.getUsername());
                session.setAttribute("userId", user.getId());
                if (session.getAttribute("lastLoginTime") == null) {
                    session.setAttribute("lastLoginTime", LocalDateTime.now());
                }
            }
        }
    }

    // ── Home ────────────────────────────────────────────────
    @GetMapping("/")
    public String home(HttpSession session, Model model,
                       @RequestParam(required = false) String logout) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username != null) {
            User user = userService.getUser(username);
            addUserTrustAttributes(model, user, session);
        }
        if (logout != null) {
            model.addAttribute("logoutSuccess", true);
        }
        return "home";
    }

    // ── Dashboard ───────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";

        User user = userService.getUser(username);
        addUserTrustAttributes(model, user, session);
        model.addAttribute("transactions", transactionService.getTransactions(username));
        return "dashboard";
    }

    // ── Login ───────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage(HttpSession session,
                            @RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (session.getAttribute("loggedInUser") != null) return "redirect:/dashboard";
        if (error != null) model.addAttribute("error", "Invalid username or password. You will be locked out after 5 failed attempts.");
        if (logout != null) model.addAttribute("success", "You have been logged out successfully");
        return "login";
    }

    // ── Signup ──────────────────────────────────────────────
    @GetMapping("/signup")
    public String signupPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) return "redirect:/dashboard";
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(User user, @RequestParam String confirmPassword, Model model) {
        String result = userService.registerUser(user, confirmPassword);
        if (!"success".equals(result)) { model.addAttribute("error", result); return "signup"; }
        return "redirect:/login";
    }

    // ── Forgot / Reset Password ─────────────────────────────
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String username,
                                 @RequestParam String email, Model model) {
        String token = userService.createResetToken(username, email);
        if (token == null) { model.addAttribute("error", "Username and email do not match"); return "forgot-password"; }
        model.addAttribute("success", "Identity verified. Use the reset form below.");
        model.addAttribute("resetToken", token);
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword, Model model) {
        String result = userService.resetPassword(token, password, confirmPassword);
        if (!"success".equals(result)) { model.addAttribute("error", result); model.addAttribute("token", token); return "reset-password"; }
        return "redirect:/login?resetSuccess";
    }

    // ── Account ─────────────────────────────────────────────
    @GetMapping("/account")
    public String account(HttpSession session,
                          @ModelAttribute("success") String success, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";
        User user = userRepository.findByUsername(username).orElse(null);
        addUserTrustAttributes(model, user, session);
        if (success != null && !success.isBlank()) model.addAttribute("success", success);
        return "account";
    }

    // ── Update Profile ──────────────────────────────────────
    @GetMapping("/update")
    public String updatePage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";
        User user = userService.getUser(username);
        addUserTrustAttributes(model, user, session);
        return "update";
    }

    @PostMapping("/update")
    public String updateProfile(User user, HttpSession session, RedirectAttributes ra) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";
        String result = userService.updateProfile(username, user);
        if (!"success".equals(result)) { ra.addFlashAttribute("error", result); return "redirect:/update"; }
        ra.addFlashAttribute("success", "Profile updated successfully");
        return "redirect:/account";
    }

    // ── Change Password ─────────────────────────────────────
    @GetMapping("/change-password")
    public String changePasswordPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";
        String result = userService.changePassword(username, currentPassword, newPassword, confirmPassword);
        if (!"success".equals(result)) { model.addAttribute("error", result); return "change-password"; }
        model.addAttribute("success", "Password changed successfully");
        return "change-password";
    }

    // ── Balance ─────────────────────────────────────────────
    @GetMapping("/balance")
    public String balance(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";
        User user = userRepository.findByUsername(username).orElse(null);
        model.addAttribute("balance", user != null ? user.getBalance() : 0.0);
        return "balance";
    }

    // ── Credit ──────────────────────────────────────────────
    @GetMapping("/credit")
    public String creditPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";
        return "credit";
    }

    @PostMapping("/credit")
    public String credit(@RequestParam double amount, HttpSession session, RedirectAttributes ra) {
        String username = (String) session.getAttribute("loggedInUser");
        String result = transactionService.credit(username, amount);
        ra.addFlashAttribute(result.equals("success") ? "success" : "error",
                result.equals("success") ? "Money added successfully" : result);
        return "redirect:/credit";
    }

    // ── Debit ───────────────────────────────────────────────
    @GetMapping("/debit")
    public String debitPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";
        return "debit";
    }

    @PostMapping("/debit")
    public String debit(@RequestParam double amount, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        String result = transactionService.debit(username, amount);
        if (!"success".equals(result)) { model.addAttribute("error", result); return "debit"; }
        model.addAttribute("success", "Money debited");
        return "debit";
    }

    // ── Transactions (PAGINATED) ────────────────────────────
    @GetMapping("/transactions")
    public String transactions(HttpSession session,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String query,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";

        User user = userRepository.findByUsername(username).orElse(null);

        Page<Transactions> txPage = transactionService.getTransactionsPaged(
                username, type, query, page);

        model.addAttribute("transactions",  txPage.getContent());
        model.addAttribute("currentPage",   txPage.getNumber());
        model.addAttribute("totalPages",    txPage.getTotalPages());
        model.addAttribute("totalItems",    txPage.getTotalElements());
        model.addAttribute("balance",       user != null ? user.getBalance() : 0.0);
        model.addAttribute("selectedType",  type  == null ? "" : type);
        model.addAttribute("searchQuery",   query == null ? "" : query);
        addUserTrustAttributes(model, user, session);

        return "transactions";
    }

    // ── Transfer ────────────────────────────────────────────
    @GetMapping("/transfer")
    public String transferPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";
        return "Transfer";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam String toUser, @RequestParam double amount,
                           HttpSession session, RedirectAttributes ra) {
        String fromUser = (String) session.getAttribute("loggedInUser");
        if (fromUser == null) return "redirect:/login";
        String result = transactionService.transfer(fromUser, toUser, amount);
        ra.addFlashAttribute(result.equals("success") ? "success" : "error",
                result.equals("success") ? "Transfer successful" : result);
        return "redirect:/transfer";
    }


    // ── PDF Export ──────────────────────────────────────────
    @Autowired
    private com.program.service.PdfExportService pdfExportService;

    @GetMapping("/transactions/export-pdf")
    public void exportPdf(HttpSession session,
                          HttpServletResponse response) throws Exception {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) { response.sendRedirect("/login"); return; }

        com.program.entity.User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) { response.sendRedirect("/login"); return; }

        java.util.List<com.program.entity.Transactions> transactions =
                transactionService.getTransactions(username);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"statement-" + username + "-" +
                        java.time.LocalDate.now() + ".pdf\"");

        pdfExportService.exportTransactions(user, transactions, response.getOutputStream());
    }

    // ── Helpers ─────────────────────────────────────────────
    private void addUserTrustAttributes(Model model, User user, HttpSession session) {
        if (user == null) return;
        model.addAttribute("user", user);
        model.addAttribute("maskedAccountNumber", buildMaskedAccountNumber(user));
        model.addAttribute("lastLoginDisplay", formatLastLogin(session));
    }

    private String buildMaskedAccountNumber(User user) {
        String n = String.format("%010d", user.getId() == null ? 0 : user.getId());
        return "SB-" + n.substring(0, 2) + "XX-XX" + n.substring(6);
    }

    private String formatLastLogin(HttpSession session) {
        Object value = session.getAttribute("lastLoginTime");
        if (value instanceof LocalDateTime t)
            return t.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        return "Active in current session";
    }
}