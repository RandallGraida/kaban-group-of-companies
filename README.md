# Kaban Core Banking System

## Complete Platform Overview

### The Problem We're Solving

**What Was Happening Before?**
Kaban operations were struggling with manual, paper-based inefficiencies:

* **Manual Ledger Nightmares:** Tellers had to manually record deposits and withdrawals in physical logbooks or disconnected spreadsheets.
* **Data Inconsistency:** Customer balances often mismatched between branches due to lack of real-time synchronization.
* **Security Risks:** Without a centralized identity system, verifying customer signatures and account ownership was prone to fraud.
* **Audit Failures:** Tracing funds or generating financial reports took days of manual compilation.

### The Solution: Enterprise Core Banking Platform

A centralized, **ACID-compliant** financial platform that ensures data integrity, real-time balance updates, and secure role-based access control (RBAC). It transforms banking from a manual struggle into a streamlined, automated, and secure digital experience.

---

## How It Works

### The Three User Roles

#### 1. Bank Customers (Users)

* *Formerly "Club Members"*
* **Role:** Individual account holders managing their finances.
* **Capabilities:**
  * View real-time **Account Balance** and status.
  * Initiate **Deposits** and **Withdrawals** (Transaction Requests).
  * View detailed **Transaction History**.
  * Update personal KYC (Know Your Customer) profile.

#### 2. Bank Tellers / Managers (Admins)

* *Formerly "Department Leads"*
* **Role:** Branch employees managing customer requests and liquidity.
* **Capabilities:**
  * **Approve/Reject** pending transaction requests.
  * **Freeze/Unfreeze** suspicious accounts.
  * View customer profiles and account details.
  * Generate daily branch transaction reports.

#### 3. System Auditors (Super Admins)

* *Formerly "Technology Directors"*
* **Role:** Executive oversight and system configuration.
* **Capabilities:**
  * **Global Ledger Access:** View total bank reserves across all branches.
  * **User Management:** Create or remove Admin access.
  * **Audit Logs:** Track every system action for compliance.

---

### The "Branches" (Divisions)

*Replaces "Departments". Each user is assigned to a specific branch for localized management.*

* **MAIN** - Main Branch (Headquarters)
* **CORP** - Corporate Banking Division
* **RET** - Retail Banking Division
* **LOAN** - Lending & Credit Division
* **INTL** - International Remittance
* **DIGI** - Digital Banking Unit
* **SEC** - Security & Fraud Prevention

---

## Platform Features

### 1. Dashboard - Your Financial Hub

**What you see when you log in:**

* **Account Summary:** Your current Available Balance and Total Balance.
* **Recent Activity:** The last 5 transactions (Deposits/Withdrawals).
* **Quick Actions:** Buttons to "Deposit Funds" or "Withdraw Funds".
* **Account Status:** Indicators for Active, Frozen, or Pending Verification.

### 2. Transaction Management (The Core Engine)

*Replaces "Assignments & Submissions"*

**Types of Transactions:**

1. **Deposit:** Adding funds to the account.
   * *Input:* Amount, Source of Funds.
   * *Result:* Balance increases immediately (if auto-approved) or upon Admin review.
2. **Withdrawal:** Removing funds.
   * *Validation:* System checks `if (Balance >= Request Amount)`.
   * *Result:* Balance decreases.
3. **Transfer:** Moving funds between accounts (Internal).

**The Workflow:**

1. **Initiation:** User requests a withdrawal of ₱5,000.
2. **Validation:** System checks funds and user status.
3. **Processing:**
   * *Small Amounts:* Auto-processed.
   * *Large Amounts:* Flagged for Admin Review.
4. **Completion:** Database updates with ACID compliance (all or nothing).

### 3. The Ledger System (Financial Integrity)

*Replaces "Gamification & Points"*

Instead of "Points," we track **Currency (PHP)**.

* **Precision:** Stored as `BigDecimal` (Java) or `DECIMAL(19,4)` (PostgreSQL) to prevent rounding errors.
* **Immutable Logs:** Once a transaction is "COMPLETED," it cannot be deleted, only reversed via a separate "CORRECTION" transaction.

### 4. Admin Portal (Oversight)

* **User Lookup:** Search users by Account Number or Email.
* **Transaction Review Queue:** See list of pending withdrawals requiring manual approval.
* **Liquidity View:** See total cash flow (In vs. Out) for the day.

---

## User Flows

### For Bank Customers (Users)

**Daily Usage Flow:**

1. Log in securely (JWT Auth).
2. Check **Current Balance** on Dashboard.
3. Click **"Deposit"**.
4. Enter Amount (e.g., ₱10,000) and Description ("Salary").
5. Confirm Transaction.
6. See **Balance Update** immediately.
7. Check **Transaction History** to verify the log exists.

### For Bank Admins (Tellers)

**Review Flow:**

1. Log in to Admin Dashboard.
2. Navigate to **"Pending Transactions"**.
3. See a request: "User Juan dela Cruz requested withdrawal of ₱500,000."
4. **Action:**
   * *Review:* Check user's history for suspicious activity.
   * *Approve:* Funds are released, status updates to "COMPLETED".
   * *Reject:* Funds return to balance, status "REJECTED".

---

## Example Scenarios

### Scenario 1: New Account Opening

**Maria** wants to open a savings account:

1. Registers with email and sets a secure password.
2. System assigns her a unique **Account Number** (e.g., `KABAN-2025-001`).
3. Initial Balance is **₱0.00**.
4. She performs her first **Deposit** of ₱5,000.
5. System records the transaction and updates her balance to ₱5,000.

### Scenario 2: High-Value Withdrawal (Security Check)

**John** tries to withdraw ₱1,000,000:

1. John submits the withdrawal request.
2. System detects amount > ₱50,000 (Threshold).
3. Status set to **"PENDING REVIEW"**.
4. **Admin (Teller)** sees the alert on their dashboard.
5. Admin calls John to verify identity.
6. Admin clicks **"Approve"**.
7. John's balance decreases, and the money is released.

### Scenario 3: Insufficient Funds

**Sarah** has ₱1,000 but tries to withdraw ₱5,000:

1. Sarah enters ₱5,000.
2. Backend `TransactionService` checks `account.getBalance()`.
3. Validation fails: `InsufficientFundsException`.
4. Transaction is blocked immediately.
5. Sarah receives an error message: "Transaction Failed: Insufficient Balance."

---

## Real-World Impact

| Metric | Manual System (Before) | Core Banking System (After) |
| :--- | :--- | :--- |
| **Transaction Time** | 10-15 minutes per user | Instant (< 1 second) |
| **Data Accuracy** | 60% (Prone to human error) | 100% (ACID Compliant) |
| **Audit Ability** | Impossible / Paper-based | Full Digital Audit Trail |
| **User Access** | Physical Branch Only | 24/7 Web Access |

---

## Technical Architecture Overview

* **Backend:** Java 17, Spring Boot 3 (Spring Security, JPA)
* **Frontend:** Angular, TypeScript
* **Database:** PostgreSQL (Relational Data Persistence)
* **Cloud:** AWS (EC2, RDS, S3)

**Key Security Features:**

* **BCrypt** Password Hashing.
* **JWT** (JSON Web Token) Stateless Authentication.
* **Role-Based Access Control (RBAC)** at the Controller level.
* **HTTPS/SSL** Encryption for all data in transit.
