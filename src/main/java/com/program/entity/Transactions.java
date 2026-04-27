package com.program.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transactions {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String type;
	private double amount;
	private String referenceNumber;
	private String details;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	// Keep existing date field for compatibility
	private LocalDateTime date;

	// Audit timestamp (auto-set on insert)
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	public Long getId()                          { return id; }
	public void setId(Long id)                   { this.id = id; }
	public String getType()                      { return type; }
	public void setType(String type)             { this.type = type; }
	public double getAmount()                    { return amount; }
	public void setAmount(double amount)         { this.amount = amount; }
	public String getReferenceNumber()           { return referenceNumber; }
	public void setReferenceNumber(String r)     { this.referenceNumber = r; }
	public String getDetails()                   { return details; }
	public void setDetails(String details)       { this.details = details; }
	public User getUser()                        { return user; }
	public void setUser(User user)               { this.user = user; }
	public LocalDateTime getDate()               { return date; }
	public void setDate(LocalDateTime date)      { this.date = date; }
	public LocalDateTime getCreatedAt()          { return createdAt; }
}