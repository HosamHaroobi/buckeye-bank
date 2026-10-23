
package org.example;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BankController {

    private final Bank bank = new Bank("buckeye-bank.db");

    // Welcome
    @GetMapping("/hello")
    public String hello() {
        return "Welcome to Buckeye Bank API!";
    }

    // Get all accounts
    @GetMapping("/accounts")
    public List<BankAccount> getAllAccounts()
            throws SQLException {

        return bank.allAccounts();
    }

    // Get one account
    @GetMapping("/accounts/{accountNumber}")
    public BankAccount getAccount(
            @PathVariable String accountNumber
    ) throws SQLException {

        BankAccount account = bank.findAccount(accountNumber);

        if (account == null) {
            throw new AccountNotFoundException("Account not found.");
        }

        return account;
    }

    // Create an account
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccount createAccount(
            @RequestBody CreateAccountRequest request
    ) throws SQLException {

        bank.createAccount(
                request.accountNumber(),
                request.accountHolder()
        );

        return bank.findAccount(request.accountNumber().trim());
    }

    // Deposit
    @PostMapping("/accounts/{accountNumber}/deposits")
    public BankAccount deposit(
            @PathVariable String accountNumber,
            @RequestBody AmountRequest request
    ) throws SQLException {

        bank.deposit(accountNumber, request.amount());

        return bank.findAccount(accountNumber);
    }

    // Withdraw
    @PostMapping("/accounts/{accountNumber}/withdrawals")
    public BankAccount withdraw(
            @PathVariable String accountNumber,
            @RequestBody AmountRequest request
    ) throws SQLException {

        bank.withdraw(accountNumber, request.amount());

        return bank.findAccount(accountNumber);
    }

    // Transfer money between accounts
    @PostMapping("/transfers")
    public String transfer(
            @RequestBody TransferRequest request
    ) throws SQLException {

        bank.transfer(
                request.fromAccount(),
                request.toAccount(),
                request.amount()
        );

        return "Transfer completed successfully!";
    }

    // Get account transaction history
    @GetMapping("/accounts/{accountNumber}/transactions")
    public List<Transaction> getTransactions(
            @PathVariable String accountNumber
    ) throws SQLException {

        return bank.history(accountNumber);
    }

    // Request data types
    public record CreateAccountRequest(
            String accountNumber,
            String accountHolder
    ) {}

    public record AmountRequest(
            BigDecimal amount
    ) {}

    public record TransferRequest(
            String fromAccount,
            String toAccount,
            BigDecimal amount
    ) {}
}
