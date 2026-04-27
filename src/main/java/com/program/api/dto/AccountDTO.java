package com.program.api.dto;

import com.program.entity.User;
import java.time.LocalDateTime;

public class AccountDTO {

    private Long id;
    private String name;
    private String username;
    private String email;
    private String mobile;
    private String address;
    private String pincode;
    private double balance;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;

    // Factory method — convert entity to DTO safely
    public static AccountDTO from(User user) {
        AccountDTO dto = new AccountDTO();
        dto.id        = user.getId();
        dto.name      = user.getName();
        dto.username  = user.getUsername();
        dto.email     = user.getEmail();
        dto.mobile    = user.getMobile();
        dto.address   = user.getAddress();
        dto.pincode   = user.getPincode();
        dto.balance   = user.getBalance();
        dto.role      = user.getRole();
        dto.enabled   = user.isEnabled();
        dto.createdAt = user.getCreatedAt();
        return dto;
    }

    public Long getId()              { return id; }
    public String getName()          { return name; }
    public String getUsername()      { return username; }
    public String getEmail()         { return email; }
    public String getMobile()        { return mobile; }
    public String getAddress()       { return address; }
    public String getPincode()       { return pincode; }
    public double getBalance()       { return balance; }
    public String getRole()          { return role; }
    public boolean isEnabled()       { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}