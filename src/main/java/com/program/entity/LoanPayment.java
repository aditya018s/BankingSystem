package com.program.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_payments")
public class LoanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private int emiNumber;

    private String referenceNumber;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime paidAt;

    // ── Getters & Setters ───────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }
    public Loan getLoan()                      { return loan; }
    public void setLoan(Loan loan)             { this.loan = loan; }
    public User getUser()                      { return user; }
    public void setUser(User user)             { this.user = user; }
    public double getAmount()                  { return amount; }
    public void setAmount(double amount)       { this.amount = amount; }
    public int getEmiNumber()                  { return emiNumber; }
    public void setEmiNumber(int n)            { this.emiNumber = n; }
    public String getReferenceNumber()         { return referenceNumber; }
    public void setReferenceNumber(String r)   { this.referenceNumber = r; }
    public LocalDateTime getPaidAt()           { return paidAt; }
}