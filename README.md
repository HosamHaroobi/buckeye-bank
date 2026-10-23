# Buckeye Bank

A Java 17+ console banking application with durable SQLite storage, designed as a learning portfolio project.

## Features

- Create and list multiple bank accounts
- Deposit and withdraw amounts in dollars
- Transfer between accounts (database transaction)
- View stored balances and timestamped transaction history
- Reject invalid amounts and insufficient funds
- Run automated JUnit tests

## Requirements

- JDK 17 or later (Java 25 works)
- Maven, or IntelliJ IDEA's Maven support
- Internet access for the first Maven dependency download

## Start

1. Open this folder (`buckeye-bank`) as a project in IntelliJ.
2. Allow Maven to load dependencies from `pom.xml`.
3. Run `org.example.Main`.
4. Follow the menu to create accounts and transact.

From the terminal you can also use `mvn test` and `mvn exec:java`.

## Persistent data

The first launch creates `buckeye-bank.db` in the process working directory.
Restarting the program from the same working directory keeps previous accounts and transactions.
Do not commit this database file to a public repository. Back it up before deleting or moving it.

## Learning notes

Stored money values are integer cents to avoid floating-point rounding. All operations use SQL prepared statements. Transfers perform both balance changes and history writes in one transaction: failures roll it back.

This is an **educational console application**, not a production banking service. There are no logins, access controls, encryption-at-rest configuration, multi-user coordination guarantees, or audited deployment processes. Spring Boot REST APIs, PostgreSQL, and Docker are future milestones.
