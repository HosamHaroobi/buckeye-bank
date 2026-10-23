package org.example;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank("buckeye-bank.db");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("""

                ================================
                       BUCKEYE BANK
                ================================
                1. Create Account
                2. Deposit Money
                3. Withdraw Money
                4. Transfer Money
                5. Check Balance
                6. Transaction History
                7. View All Accounts
                8. Exit
                ================================
                """);
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> {
                        String number = read(scanner, "Account number: ");
                        String holder = read(scanner, "Account holder: ");
                        bank.createAccount(number, holder);
                        System.out.println("Account created!");
                    }
                    case "2" -> {
                        String number = read(scanner, "Account number: ");
                        bank.deposit(number, amount(scanner));
                        System.out.println("Deposit successful!");
                    }
                    case "3" -> {
                        String number = read(scanner, "Account number: ");
                        bank.withdraw(number, amount(scanner));
                        System.out.println("Withdrawal successful!");
                    }
                    case "4" -> {
                        String from = read(scanner, "Sender account number: ");
                        String to = read(scanner, "Receiver account number: ");
                        bank.transfer(from, to, amount(scanner));
                        System.out.println("Transfer successful!");
                    }
                    case "5" -> {
                        BankAccount account = bank.findAccount(read(scanner, "Account number: "));
                        if (account == null) System.out.println("Account not found.");
                        else display(account);
                    }
                    case "6" -> {
                        String number = read(scanner, "Account number: ");
                        var transactions = bank.history(number);
                        if (transactions.isEmpty()) System.out.println("No transactions yet.");
                        for (Transaction t : transactions) {
                            System.out.printf("%s | %s | $%s | %s%n",
                                    t.date(), t.type(), t.amount(), t.description());
                        }
                    }
                    case "7" -> {
                        var accounts = bank.allAccounts();
                        if (accounts.isEmpty()) System.out.println("No accounts yet.");
                        for (BankAccount account : accounts) display(account);
                    }
                    case "8" -> {
                        System.out.println("Thank you for using Buckeye Bank!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Cannot complete operation: " + e.getMessage());
            } catch (ArithmeticException e) {
                System.out.println("Amount is too large.");
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static String read(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static BigDecimal amount(Scanner scanner) {
        String input = read(scanner, "Amount ($): ");
        try {
            return new BigDecimal(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid number.");
        }
    }

    private static void display(BankAccount account) {
        System.out.printf("Account: %s | Holder: %s | Balance: $%s%n",
                account.accountNumber(), account.accountHolder(), account.balance());
    }
}
