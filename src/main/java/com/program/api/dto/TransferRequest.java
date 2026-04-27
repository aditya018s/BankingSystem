package com.program.api.dto;

public class TransferRequest {
    private String toUsername;
    private double amount;
    public String getToUsername()          { return toUsername; }
    public void setToUsername(String u)    { this.toUsername = u; }
    public double getAmount()              { return amount; }
    public void setAmount(double a)        { this.amount = a; }
}