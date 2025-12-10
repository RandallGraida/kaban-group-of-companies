# Kaban Core Banking System Frontend Developer Guide

**Angular | TypeScript | RxJS | AWS Free Tier**

## 1. Endpoints by Role

### Shared / Common Endpoints
**Accessible by all authenticated roles.**

| Feature | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Authentication** | `POST` | `/api/auth/login` | Secure JWT login. Returns token + user role. |
| **Profile** | `GET` | `/api/auth/profile` | View own user profile (Name, Avatar, Branch). |
| **Branches** | `GET` | `/api/branches` | List all bank branches (e.g., MAIN, CORP). |
| **Announcements** | `GET` | `/api/announcements` | View global bank announcements. |

### 🏦 Admin Specific Endpoints (`ROLE_ADMIN`)
**Used for managing customer accounts and reviewing transactions within a specific branch.**

| Feature | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Transaction Review** | `GET` | `/api/admin/transactions/pending` | List withdrawals/large deposits needing approval in their branch. |
| | `PUT` | `/api/admin/transactions/{id}/approve` | **Approve** a pending transaction. |
| | `PUT` | `/api/admin/transactions/{id}/reject` | **Reject** a pending transaction. |
| **User Management** | `GET` | `/api/admin/users` | List all users within their branch. |
| | `GET` | `/api/admin/users/{id}` | View detailed customer profile & history. |
| | `PUT` | `/api/admin/users/{id}/status` | Freeze or Activate a customer account. |
| **Branch Ledger** | `GET` | `/api/admin/ledger/summary` | View total liquidity for their branch. |

### 🚀 Super Admin Specific Endpoints (`ROLE_SUPER_ADMIN`)
**Used for global system oversight and administration.**

| Feature | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Global Ledger** | `GET` | `/api/super-admin/ledger/global` | View total bank reserves across all branches. |
| **Admin Management** | `POST` | `/api/super-admin/admins` | Create a new Admin user. |
| | `DELETE` | `/api/super-admin/admins/{id}` | Remove an Admin user. |
| **System Audit** | `GET` | `/api/super-admin/audit-logs` | Access system-wide action logs. |

### 👤 Customer Specific Endpoints (`ROLE_USER`)
**Used for managing personal finances.**

| Feature | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **My Accounts** | `GET` | `/api/accounts/me` | List all **your own** savings/checking accounts. |
| | `GET` | `/api/accounts/{id}` | View details (Balance, Status) of a specific account. |
| **Transactions** | `POST` | `/api/transactions/deposit` | Initiate a deposit. |
| | `POST` | `/api/transactions/withdraw` | Request a withdrawal. |
| | `GET` | `/api/transactions/history` | View chronological history of **your own** transactions. |
| **Transfer** | `POST` | `/api/transactions/transfer` | Transfer funds to another internal Kaban account. |

## 2. How Data is Digested by Banking Branch

The system uses `branch_code` to segregate data so Admins only manage users within their jurisdiction, while Super Admins can see all.

### For Customers (`ROLE_USER`)
The scope is automatically locked to the **User's ID** by the Backend Security Context.
* **Accounts:** `GET /api/accounts/me` returns only accounts owned by the logged-in user.
* **Transactions:** Users only see their own history.

### For Admins (`ROLE_ADMIN`)
The admin is scoped to their assigned **Branch**.
* **Reviewing Transactions:** `GET /api/admin/transactions/pending` automatically filters results where `user.branch_code == admin.branch_code`.
* **User Search:** `GET /api/admin/users?query=Maria` returns "Maria" only if she belongs to the Admin's branch.

### For Super Admins (`ROLE_SUPER_ADMIN`)
High-level auditors can pass `?branch_code=ALL` to view global data or access dedicated super-admin endpoints.

### Data Structure Example:

#### Account Object (Frontend Model)
```typescript
export interface Account {
  id: string; // UUID
  accountNumber: string; // e.g., "KABAN-2025-8821"
  accountType: 'SAVINGS' | 'CHECKING';
  branchCode: 'MAIN' | 'CORP' | 'RET'; // Links account to a specific branch
  balance: number; // BigDecimal from backend
  status: 'ACTIVE' | 'FROZEN';
}
```