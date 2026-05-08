package com.program.service;

import com.program.entity.Transactions;
import com.program.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── Send OTP for Signup ─────────────────────────────────
    @Async
    public void sendSignupOtp(String toEmail, String name, String otp) {
        String subject = "🔐 Verify Your Secure Bank Account — OTP: " + otp;
        String body = buildOtpEmail(name, otp, "signup verification",
                "Your account will be activated once you verify your email address.",
                10);
        sendHtml(toEmail, subject, body);
    }

    // ── Send OTP for 2FA Login ──────────────────────────────
    @Async
    public void sendLoginOtp(String toEmail, String name, String otp, String ip) {
        String subject = "🔐 Secure Bank Login OTP: " + otp;
        String body = buildOtpEmail(name, otp, "login verification",
                "Someone (IP: " + ip + ") is trying to log in to your account. " +
                        "If this wasn't you, please change your password immediately.",
                5);
        sendHtml(toEmail, subject, body);
    }

    // ── Send Transaction Notification ───────────────────────
    @Async
    public void sendTransactionNotification(User user, Transactions tx) {
        boolean isCredit = "CREDIT".equals(tx.getType()) || "TRANSFER_IN".equals(tx.getType());
        String emoji    = isCredit ? "💚" : "🔴";
        String typeText = switch (tx.getType()) {
            case "CREDIT"       -> "Money Added";
            case "DEBIT"        -> "Money Withdrawn";
            case "TRANSFER_IN"  -> "Money Received";
            case "TRANSFER_OUT" -> "Money Sent";
            default             -> tx.getType();
        };

        String subject = emoji + " " + typeText + " — ₹" +
                String.format("%.2f", tx.getAmount()) + " | Secure Bank";

        String amountColor = isCredit ? "#10b981" : "#ef4444";
        String amountSign  = isCredit ? "+" : "-";

        String body = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,sans-serif">
                <div style="max-width:560px;margin:40px auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)">
                  
                  <!-- Header -->
                  <div style="background:linear-gradient(135deg,#16313c,#2b6777);padding:32px;text-align:center">
                    <h1 style="color:white;margin:0;font-size:22px">🏦 Secure Bank</h1>
                    <p style="color:rgba(255,255,255,0.75);margin:6px 0 0;font-size:14px">Transaction Alert</p>
                  </div>

                  <!-- Body -->
                  <div style="padding:32px">
                    <p style="color:#334e68;font-size:16px;margin:0 0 24px">Hi <strong>%s</strong>,</p>

                    <!-- Amount box -->
                    <div style="background:#f8fafc;border-radius:12px;padding:24px;text-align:center;margin-bottom:24px;border-left:4px solid %s">
                      <p style="color:#486581;font-size:13px;margin:0 0 8px;text-transform:uppercase;letter-spacing:.05em">%s</p>
                      <p style="font-size:42px;font-weight:900;color:%s;margin:0">%s₹%s</p>
                    </div>

                    <!-- Details -->
                    <table style="width:100%;border-collapse:collapse">
                      <tr>
                        <td style="padding:10px 0;color:#486581;font-size:14px;border-bottom:1px solid #e5e7eb">Reference</td>
                        <td style="padding:10px 0;color:#102a43;font-size:14px;font-weight:600;text-align:right;border-bottom:1px solid #e5e7eb">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:10px 0;color:#486581;font-size:14px;border-bottom:1px solid #e5e7eb">Details</td>
                        <td style="padding:10px 0;color:#102a43;font-size:14px;font-weight:600;text-align:right;border-bottom:1px solid #e5e7eb">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:10px 0;color:#486581;font-size:14px;border-bottom:1px solid #e5e7eb">Date & Time</td>
                        <td style="padding:10px 0;color:#102a43;font-size:14px;font-weight:600;text-align:right;border-bottom:1px solid #e5e7eb">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:10px 0;color:#486581;font-size:14px">Available Balance</td>
                        <td style="padding:10px 0;color:#10b981;font-size:16px;font-weight:800;text-align:right">₹%s</td>
                      </tr>
                    </table>

                    <p style="color:#6b7280;font-size:13px;margin:24px 0 0;line-height:1.6">
                      If you did not perform this transaction, please 
                      <a href="#" style="color:#2b6777">contact us immediately</a> 
                      or change your password.
                    </p>
                  </div>

                  <!-- Footer -->
                  <div style="background:#f8fafc;padding:20px;text-align:center;border-top:1px solid #e5e7eb">
                    <p style="color:#9ca3af;font-size:12px;margin:0">© 2026 Secure Bank · This is an automated notification</p>
                  </div>
                </div>
                </body>
                </html>
                """.formatted(
                user.getName(),
                amountColor,
                typeText,
                amountColor,
                amountSign,
                String.format("%.2f", tx.getAmount()),
                tx.getReferenceNumber(),
                tx.getDetails(),
                tx.getDate() != null ? tx.getDate().format(FMT) : "—",
                String.format("%.2f", user.getBalance())
        );

        sendHtml(user.getEmail(), subject, body);
    }

    // ── Send New Device Login Alert ─────────────────────────
    @Async
    public void sendNewDeviceAlert(User user, String ip, String time) {
        String subject = "⚠️ New Login Detected — Secure Bank";
        String body = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,sans-serif">
                <div style="max-width:560px;margin:40px auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)">
                  <div style="background:linear-gradient(135deg,#b91c1c,#7f1d1d);padding:32px;text-align:center">
                    <h1 style="color:white;margin:0;font-size:22px">⚠️ Security Alert</h1>
                    <p style="color:rgba(255,255,255,0.8);margin:6px 0 0;font-size:14px">Secure Bank</p>
                  </div>
                  <div style="padding:32px">
                    <p style="color:#334e68;font-size:16px;margin:0 0 16px">Hi <strong>%s</strong>,</p>
                    <p style="color:#334e68;font-size:15px;line-height:1.6;margin:0 0 24px">
                      We detected a new login to your account from a new IP address.
                    </p>
                    <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:12px;padding:20px;margin-bottom:24px">
                      <table style="width:100%%">
                        <tr>
                          <td style="color:#6b7280;font-size:14px;padding:6px 0">IP Address</td>
                          <td style="color:#991b1b;font-weight:700;font-size:14px;text-align:right">%s</td>
                        </tr>
                        <tr>
                          <td style="color:#6b7280;font-size:14px;padding:6px 0">Time</td>
                          <td style="color:#991b1b;font-weight:700;font-size:14px;text-align:right">%s</td>
                        </tr>
                      </table>
                    </div>
                    <p style="color:#334e68;font-size:15px;line-height:1.6">
                      <strong>Was this you?</strong> If yes, you can ignore this email.<br>
                      If this wasn't you, please change your password immediately.
                    </p>
                  </div>
                  <div style="background:#f8fafc;padding:20px;text-align:center;border-top:1px solid #e5e7eb">
                    <p style="color:#9ca3af;font-size:12px;margin:0">© 2026 Secure Bank · Security Notification</p>
                  </div>
                </div>
                </body>
                </html>
                """.formatted(user.getName(), ip, time);

        sendHtml(user.getEmail(), subject, body);
    }


    // ── Send Password Reset Email ───────────────────────────
    @Async
    public void sendPasswordResetEmail(User user, String token) {
        String resetLink = "http://localhost:9090/reset-password?token=" + token;
        String subject = "🔑 Reset Your Secure Bank Password";
        String body = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,sans-serif">
                <div style="max-width:560px;margin:40px auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)">
                  <div style="background:linear-gradient(135deg,#16313c,#2b6777);padding:32px;text-align:center">
                    <h1 style="color:white;margin:0;font-size:22px">🏦 Secure Bank</h1>
                    <p style="color:rgba(255,255,255,0.75);margin:6px 0 0;font-size:14px">Password Reset Request</p>
                  </div>
                  <div style="padding:32px;text-align:center">
                    <div style="font-size:48px;margin-bottom:16px">🔑</div>
                    <p style="color:#334e68;font-size:16px;margin:0 0 8px">Hi <strong>%s</strong>,</p>
                    <p style="color:#486581;font-size:14px;margin:0 0 32px;line-height:1.6">
                      We received a request to reset your password.<br>
                      Click the button below to set a new password.
                    </p>
                    <a href="%s"
                       style="display:inline-block;background:linear-gradient(135deg,#16313c,#2b6777);color:white;padding:14px 32px;border-radius:999px;text-decoration:none;font-weight:700;font-size:16px;margin-bottom:24px">
                      Reset My Password
                    </a>
                    <p style="color:#ef4444;font-size:13px;font-weight:600;margin:0 0 24px">
                      ⏱ This link expires in 15 minutes
                    </p>
                    <div style="background:#f8fafc;border-radius:10px;padding:14px;font-size:13px;color:#6b7280">
                      If you didn't request this, ignore this email.<br>
                      Your password won't change unless you click the link above.
                    </div>
                  </div>
                  <div style="background:#f8fafc;padding:20px;text-align:center;border-top:1px solid #e5e7eb">
                    <p style="color:#9ca3af;font-size:12px;margin:0">© 2026 Secure Bank · Do not reply to this email</p>
                  </div>
                </div>
                </body>
                </html>
                """.formatted(user.getName(), resetLink);
        sendHtml(user.getEmail(), subject, body);
    }

    // ── Helper: build OTP email ─────────────────────────────
    private String buildOtpEmail(String name, String otp, String purpose,
                                 String context, int expiryMins) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,sans-serif">
                <div style="max-width:560px;margin:40px auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)">
                  <div style="background:linear-gradient(135deg,#16313c,#2b6777);padding:32px;text-align:center">
                    <h1 style="color:white;margin:0;font-size:22px">🏦 Secure Bank</h1>
                    <p style="color:rgba(255,255,255,0.75);margin:6px 0 0;font-size:14px">One-Time Password</p>
                  </div>
                  <div style="padding:32px;text-align:center">
                    <p style="color:#334e68;font-size:16px;margin:0 0 8px">Hi <strong>%s</strong>,</p>
                    <p style="color:#486581;font-size:14px;margin:0 0 32px;line-height:1.6">%s</p>
                    
                    <p style="color:#486581;font-size:13px;margin:0 0 12px;text-transform:uppercase;letter-spacing:.08em">Your OTP for %s</p>
                    
                    <div style="background:linear-gradient(135deg,#16313c,#2b6777);border-radius:16px;padding:24px;display:inline-block;min-width:220px;margin-bottom:24px">
                      <p style="font-size:48px;font-weight:900;color:white;margin:0;letter-spacing:12px">%s</p>
                    </div>
                    
                    <p style="color:#ef4444;font-size:14px;font-weight:600;margin:0 0 24px">
                      ⏱ Expires in %d minutes
                    </p>
                    
                    <p style="color:#9ca3af;font-size:13px;margin:0;line-height:1.6">
                      Never share this OTP with anyone.<br>
                      Secure Bank will never ask for your OTP.
                    </p>
                  </div>
                  <div style="background:#f8fafc;padding:20px;text-align:center;border-top:1px solid #e5e7eb">
                    <p style="color:#9ca3af;font-size:12px;margin:0">© 2026 Secure Bank · Do not reply to this email</p>
                  </div>
                </div>
                </body>
                </html>
                """.formatted(name, context, purpose, otp, expiryMins);
    }

    // ── Core send method ────────────────────────────────────
    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, "Secure Bank");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Email send failed to " + to + ": " + e.getMessage());
        }
    }
}