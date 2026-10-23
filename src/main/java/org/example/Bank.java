package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Database-backed banking operations. All monetary values are stored as integer cents. */
public class Bank {
    private final String databaseUrl;

    public Bank(String databaseFile) {
        this.databaseUrl = "jdbc:sqlite:" + databaseFile;
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS accounts (
                  account_number TEXT PRIMARY KEY,
                  account_holder TEXT NOT NULL,
                  balance_cents INTEGER NOT NULL DEFAULT 0 CHECK(balance_cents >= 0)
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  account_number TEXT NOT NULL REFERENCES accounts(account_number),
                  type TEXT NOT NULL,
                  amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
                  description TEXT NOT NULL,
                  occurred_at TEXT NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot initialize database: " + e.getMessage(), e);
        }
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(databaseUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private static long cents(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Enter a valid amount with at most two decimal places.");
        }
    }

    private static BigDecimal dollars(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    public void createAccount(String number, String holder) throws SQLException {
        if (number == null || number.isBlank() || holder == null || holder.isBlank()) {
            throw new IllegalArgumentException("Account number and name are required.");
        }
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO accounts (account_number, account_holder) VALUES (?, ?)")) {
            statement.setString(1, number.trim());
            statement.setString(2, holder.trim());
            statement.executeUpdate();
        }
    }

    public BankAccount findAccount(String number) throws SQLException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT account_number, account_holder, balance_cents FROM accounts WHERE account_number = ?")) {
            statement.setString(1, number);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new BankAccount(result.getString(1), result.getString(2), dollars(result.getLong(3)));
            }
        }
    }

    public List<BankAccount> allAccounts() throws SQLException {
        List<BankAccount> accounts = new ArrayList<>();
        try (Connection connection = open(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT account_number, account_holder, balance_cents FROM accounts ORDER BY account_number")) {
            while (result.next()) {
                accounts.add(new BankAccount(result.getString(1), result.getString(2), dollars(result.getLong(3))));
            }
        }
        return accounts;
    }

    private static Long getBalanceCents(Connection connection, String number) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance_cents FROM accounts WHERE account_number = ?")) {
            statement.setString(1, number);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : null;
            }
        }
    }

    private static void setBalanceCents(Connection connection, String number, long balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE accounts SET balance_cents = ? WHERE account_number = ?")) {
            statement.setLong(1, balance);
            statement.setString(2, number);
            statement.executeUpdate();
        }
    }

    private static void record(Connection connection, String number, String type,
                               long amount, String description) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO transactions (account_number, type, amount_cents, description, occurred_at)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, number);
            statement.setString(2, type);
            statement.setLong(3, amount);
            statement.setString(4, description);
            statement.setString(5, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    public void deposit(String number, BigDecimal amount) throws SQLException {
        long value = cents(amount);
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                Long old = getBalanceCents(connection, number);
                if (old == null) throw new IllegalArgumentException("Account not found.");
                setBalanceCents(connection, number, Math.addExact(old, value));
                record(connection, number, "DEPOSIT", value, "Money deposited");
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void withdraw(String number, BigDecimal amount) throws SQLException {
        long value = cents(amount);
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                Long old = getBalanceCents(connection, number);
                if (old == null) throw new IllegalArgumentException("Account not found.");
                if (old < value) throw new IllegalArgumentException("Insufficient funds.");
                setBalanceCents(connection, number, old - value);
                record(connection, number, "WITHDRAWAL", value, "Money withdrawn");
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void transfer(String from, String to, BigDecimal amount) throws SQLException {
        long value = cents(amount);
        if (from == null || from.equals(to)) {
            throw new IllegalArgumentException("Choose two different accounts.");
        }
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                Long sender = getBalanceCents(connection, from);
                Long receiver = getBalanceCents(connection, to);
                if (sender == null || receiver == null) throw new IllegalArgumentException("Account not found.");
                if (sender < value) throw new IllegalArgumentException("Insufficient funds.");
                long newReceiver = Math.addExact(receiver, value);
                setBalanceCents(connection, from, sender - value);
                setBalanceCents(connection, to, newReceiver);
                record(connection, from, "TRANSFER OUT", value, "Sent to " + to);
                record(connection, to, "TRANSFER IN", value, "Received from " + from);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public List<Transaction> history(String number) throws SQLException {
        if (findAccount(number) == null) throw new IllegalArgumentException("Account not found.");
        List<Transaction> history = new ArrayList<>();
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement("""
                SELECT type, amount_cents, description, occurred_at
                FROM transactions WHERE account_number = ? ORDER BY id DESC
                """)) {
            statement.setString(1, number);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    history.add(new Transaction(result.getString(1), dollars(result.getLong(2)),
                            result.getString(3), result.getString(4)));
                }
            }
        }
        return history;
    }
}
