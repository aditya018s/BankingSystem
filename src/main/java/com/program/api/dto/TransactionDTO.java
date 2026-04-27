package com.program.api.dto;

import com.program.entity.Transactions;
import java.time.LocalDateTime;

public class TransactionDTO {

    private Long id;
    private String type;
    private double amount;
    private String referenceNumber;
    private String details;
    private String username;
    private LocalDateTime date;
    private LocalDateTime createdAt;

    public static TransactionDTO from(Transactions tx) {
        TransactionDTO dto = new TransactionDTO();
        dto.id              = tx.getId();
        dto.type            = tx.getType();
        dto.amount          = tx.getAmount();
        dto.referenceNumber = tx.getReferenceNumber();
        dto.details         = tx.getDetails();
        dto.username        = tx.getUser() != null ? tx.getUser().getUsername() : null;
        dto.date            = tx.getDate();
        dto.createdAt       = tx.getCreatedAt();
        return dto;
    }

    public Long getId()                  { return id; }
    public String getType()              { return type; }
    public double getAmount()            { return amount; }
    public String getReferenceNumber()   { return referenceNumber; }
    public String getDetails()           { return details; }
    public String getUsername()          { return username; }
    public LocalDateTime getDate()       { return date; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}