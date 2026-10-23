package org.example;

import java.math.BigDecimal;

/** A read-only snapshot of one account as loaded from the database. */
public record BankAccount(String accountNumber, String accountHolder, BigDecimal balance) {
}
