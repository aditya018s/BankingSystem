package com.program.api.dto;

// ── Credit / Debit request ──────────────────────────────────
class AmountRequest {
    private double amount;
    public double getAmount()        { return amount; }
    public void setAmount(double a)  { this.amount = a; }
}

public class CreditRequest extends AmountRequest {}