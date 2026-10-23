package org.example;

import java.math.BigDecimal;

/** A read-only transaction history entry. */
public record Transaction(String type, BigDecimal amount, String description, String date) {
}
