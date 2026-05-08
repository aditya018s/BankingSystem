package com.program.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.program.entity.Transactions;
import com.program.entity.User;
import com.program.repository.UserRepository;
import com.program.service.EmailService;
import com.program.service.PdfExportService;
import com.program.service.TransactionService;
import com.program.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private TransactionService transactionService;
    @Autowired private EmailService emailService;
    @Autowired private UserRepository userRepository;
    @Autowired private PdfExportService pdfExportService;

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
                if (session.getAttribute("lastLoginTime") == null)
                    session.setAttribute("lastLoginTime", LocalDateTime.now());
            }
        }
    }

    // ── Home ────────────────────────────────────────────────
    @GetMapping("/")
    public String home(HttpSession session, Model model,
                       @RequestParam(required = false) String logout) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username != null) addUserTrustAttributes(model, userService.getUser(username), session);
        if (logout != null) model.addAttribute("logoutSuccess", true);
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

        // Show new device banner if flagged
        Boolean isNew = (Boolean) session.getAttribute("isNewDevice");
        if (Boolean.TRUE.equals(isNew)) {
            model.addAttribute("newDeviceAlert", true);
            session.removeAttribute("isNewDevice");
        }
        return "dashboard";
    }

    // ── Login ───────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage(HttpSession session,
                            @RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (session.getAttribute("loggedInUser") != null) return "redirect:/dashboard";
        if (error   != null) model.addAttribute("error",
                "Invalid username or password. Account locks after 5 failed attempts.");
        if (logout  != null) model.addAttribute("success", "Logged out successfully.");
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
        if (result.startsWith("otp_sent:")) {
            return "redirect:/verify-signup-otp?username=" + result.split(":")[1];
        }
        model.addAttribute("error", result);
        return "signup";
    }

    // ── Verify Signup OTP ───────────────────────────────────
    @GetMapping("/verify-signup-otp")
    public String verifySignupOtpPage(@RequestParam String username, Model model) {
        model.addAttribute("username", username);
        return "verify-otp";
    }

    @PostMapping("/verify-signup-otp")
    public String verifySignupOtp(@RequestParam String username,
                                  @RequestParam String otp, Model model) {
        String result = userService.verifySignupOtp(username, otp.trim());
        if ("success".equals(result)) return "redirect:/login?verified";
        model.addAttribute("username", username);
        model.addAttribute("error", result);
        return "verify-otp";
    }

    @PostMapping("/resend-signup-otp")
    public String resendSignupOtp(@RequestParam String username, RedirectAttributes ra) {
        String result = userService.resendSignupOtp(username);
        ra.addFlashAttribute("success".equals(result) ? "success" : "error",
                "success".equals(result) ? "OTP resent to your email!" : result);
        return "redirect:/verify-signup-otp?username=" + username;
    }

    // ── Forgot Password → sends reset link via email ────────
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String username,
                                 @RequestParam String email, Model model) {
        String token = userService.createResetToken(username, email);
        if (token == null) {
            model.addAttribute("error", "No account found with that username and email.");
            return "forgot-password";
        }
        // Send reset link via email
        User user = userService.getUser(username);
        if (user != null) emailService.sendPasswordResetEmail(user, token);
        model.addAttribute("success",
                "✅ Password reset link sent to your email! Check your inbox.");
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
        if (!"success".equals(result)) {
            model.addAttribute("error", result);
            model.addAttribute("token", token);
            return "reset-password";
        }
        return "redirect:/login?resetSuccess";
    }

    // ── Account ─────────────────────────────────────────────
    @GetMapping("/account")
    public String account(HttpSession session,
                          @ModelAttribute("success") String success, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";
        addUserTrustAttributes(model, userRepository.findByUsername(username).orElse(null), session);
        if (success != null && !success.isBlank()) model.addAttribute("success", success);
        return "account";
    }

    // ── Update Profile ──────────────────────────────────────
    @GetMapping("/update")
    public String updatePage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";
        addUserTrustAttributes(model, userService.getUser(username), session);
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
        ra.addFlashAttribute("success".equals(result) ? "success" : "error",
                "success".equals(result) ? "Money added successfully!" : result);
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
        model.addAttribute("success", "Withdrawal successful!");
        return "debit";
    }

    // ── Transactions (paginated) ────────────────────────────
    @GetMapping("/transactions")
    public String transactions(HttpSession session,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String query,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) return "redirect:/login";

        User user = userRepository.findByUsername(username).orElse(null);
        Page<Transactions> txPage = transactionService.getTransactionsPaged(username, type, query, page);

        model.addAttribute("transactions", txPage.getContent());
        model.addAttribute("currentPage",  txPage.getNumber());
        model.addAttribute("totalPages",   txPage.getTotalPages());
        model.addAttribute("totalItems",   txPage.getTotalElements());
        model.addAttribute("balance",      user != null ? user.getBalance() : 0.0);
        model.addAttribute("selectedType", type  == null ? "" : type);
        model.addAttribute("searchQuery",  query == null ? "" : query);
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
        ra.addFlashAttribute("success".equals(result) ? "success" : "error",
                "success".equals(result) ? "Transfer successful!" : result);
        return "redirect:/transfer";
    }

    // ── PDF Export ──────────────────────────────────────────
    @GetMapping("/transactions/export-pdf")
    public void exportPdf(HttpSession session, HttpServletResponse response) throws Exception {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) { response.sendRedirect("/login"); return; }
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) { response.sendRedirect("/login"); return; }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"statement-" + username + "-" +
                        java.time.LocalDate.now() + ".pdf\"");
        pdfExportService.exportTransactions(user, transactionService.getTransactions(username),
                response.getOutputStream());
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