package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BankTest {
    @TempDir Path temporaryDirectory;

    private Bank newBank() {
        return new Bank(temporaryDirectory.resolve("test.db").toString());
    }

    @Test
    void depositsAndWithdrawalsPersistAcrossRestart() throws Exception {
        Bank bank = newBank();
        bank.createAccount("1001", "Hosam");
        bank.deposit("1001", new BigDecimal("500.00"));
        bank.withdraw("1001", new BigDecimal("150.00"));

        Bank reopened = newBank();
        assertEquals(new BigDecimal("350.00"), reopened.findAccount("1001").balance());
        assertEquals(2, reopened.history("1001").size());
    }

    @Test
    void transferUpdatesBothAccountsAndHistory() throws Exception {
        Bank bank = newBank();
        bank.createAccount("1001", "Hosam");
        bank.createAccount("1002", "Doug");
        bank.deposit("1001", new BigDecimal("500.00"));
        bank.transfer("1001", "1002", new BigDecimal("125.00"));

        assertEquals(new BigDecimal("375.00"), bank.findAccount("1001").balance());
        assertEquals(new BigDecimal("125.00"), bank.findAccount("1002").balance());
        assertEquals("TRANSFER IN", bank.history("1002").get(0).type());
    }

    @Test
    void rejectedOperationsDoNotChangeBalances() throws Exception {
        Bank bank = newBank();
        bank.createAccount("1001", "Hosam");
        bank.createAccount("1002", "Doug");
        bank.deposit("1001", new BigDecimal("50.00"));

        assertThrows(IllegalArgumentException.class,
                () -> bank.withdraw("1001", new BigDecimal("100.00")));
        assertThrows(IllegalArgumentException.class,
                () -> bank.transfer("1001", "1002", new BigDecimal("100.00")));
        assertThrows(IllegalArgumentException.class,
                () -> bank.deposit("1001", new BigDecimal("0.001")));
        assertEquals(new BigDecimal("50.00"), bank.findAccount("1001").balance());
        assertEquals(new BigDecimal("0.00"), bank.findAccount("1002").balance());
    }
}
