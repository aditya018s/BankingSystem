package com.program.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "loans")
public class Loan {

    public enum Status { PENDING, APPROVED, REJECTED, ACTIVE, CLOSED }
    public enum Purpose { HOME, CAR, EDUCATION, PERSONAL, BUSINESS, MEDICAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(nullable = false)
    private double interestRate = 10.5; // annual %

    @Column(nullable = false)
    private double emiAmount;

    @Column(nullable = false)
    private double totalPayable;

    @Column(nullable = false)
    private double remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private String adminRemark;

    private String loanNumber;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    private List<LoanPayment> payments;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime approvedAt;
    private LocalDateTime closedAt;

    // ── Getters & Setters ───────────────────────────────────
    public Long getId()                           { return id; }
    public void setId(Long id)                    { this.id = id; }
    public User getUser()                         { return user; }
    public void setUser(User u)                   { this.user = u; }
    public double getAmount()                     { return amount; }
    public void setAmount(double a)               { this.amount = a; }
    public int getTenureMonths()                  { return tenureMonths; }
    public void setTenureMonths(int t)            { this.tenureMonths = t; }
    public double getInterestRate()               { return interestRate; }
    public void setInterestRate(double r)         { this.interestRate = r; }
    public double getEmiAmount()                  { return emiAmount; }
    public void setEmiAmount(double e)            { this.emiAmount = e; }
    public double getTotalPayable()               { return totalPayable; }
    public void setTotalPayable(double t)         { this.totalPayable = t; }
    public double getRemainingAmount()            { return remainingAmount; }
    public void setRemainingAmount(double r)      { this.remainingAmount = r; }
    public Purpose getPurpose()                   { return purpose; }
    public void setPurpose(Purpose p)             { this.purpose = p; }
    public Status getStatus()                     { return status; }
    public void setStatus(Status s)               { this.status = s; }
    public String getAdminRemark()                { return adminRemark; }
    public void setAdminRemark(String r)          { this.adminRemark = r; }
    public String getLoanNumber()                 { return loanNumber; }
    public void setLoanNumber(String n)           { this.loanNumber = n; }
    public List<LoanPayment> getPayments()        { return payments; }
    public void setPayments(List<LoanPayment> p)  { this.payments = p; }
    public LocalDateTime getAppliedAt()           { return appliedAt; }
    public LocalDateTime getUpdatedAt()           { return updatedAt; }
    public LocalDateTime getApprovedAt()          { return approvedAt; }
    public void setApprovedAt(LocalDateTime a)    { this.approvedAt = a; }
    public LocalDateTime getClosedAt()            { return closedAt; }
    public void setClosedAt(LocalDateTime c)      { this.closedAt = c; }
}