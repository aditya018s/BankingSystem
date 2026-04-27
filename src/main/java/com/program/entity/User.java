package com.program.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private double balance = 0.0;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String mobile;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String pincode;

    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Column(nullable = false)
    private boolean enabled = true;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    // ── Rate limiting ───────────────────────────────────────
    private int failedLoginAttempts = 0;
    private LocalDateTime lockoutUntil;

    // ── Audit timestamps ────────────────────────────────────
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Constructors ────────────────────────────────────────
    public User() {}

    public User(String name, String username, String password, String pincode,
                double balance, String email, String mobile, String address) {
        this.name = name; this.username = username; this.password = password;
        this.balance = balance; this.email = email; this.mobile = mobile;
        this.address = address; this.pincode = pincode;
    }

    // ── Getters & Setters ───────────────────────────────────
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }
    public String getName()                          { return name; }
    public void setName(String name)                 { this.name = name; }
    public String getUsername()                      { return username; }
    public void setUsername(String u)                { this.username = u; }
    public String getPassword()                      { return password; }
    public void setPassword(String p)                { this.password = p; }
    public double getBalance()                       { return balance; }
    public void setBalance(double b)                 { this.balance = b; }
    public String getEmail()                         { return email; }
    public void setEmail(String e)                   { this.email = e; }
    public String getMobile()                        { return mobile; }
    public void setMobile(String m)                  { this.mobile = m; }
    public String getAddress()                       { return address; }
    public void setAddress(String a)                 { this.address = a; }
    public String getPincode()                       { return pincode; }
    public void setPincode(String p)                 { this.pincode = p; }
    public String getRole()                          { return role; }
    public void setRole(String r)                    { this.role = r; }
    public boolean isEnabled()                       { return enabled; }
    public void setEnabled(boolean e)                { this.enabled = e; }
    public String getResetToken()                    { return resetToken; }
    public void setResetToken(String t)              { this.resetToken = t; }
    public LocalDateTime getResetTokenExpiry()       { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime t) { this.resetTokenExpiry = t; }
    public int getFailedLoginAttempts()              { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int f)        { this.failedLoginAttempts = f; }
    public LocalDateTime getLockoutUntil()           { return lockoutUntil; }
    public void setLockoutUntil(LocalDateTime l)     { this.lockoutUntil = l; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public LocalDateTime getUpdatedAt()              { return updatedAt; }

    // ── Helpers ─────────────────────────────────────────────
    public boolean isLockedOut() {
        return lockoutUntil != null && LocalDateTime.now().isBefore(lockoutUntil);
    }
}