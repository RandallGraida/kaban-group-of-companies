-- ============================================================================
--  Kaban - CORE BANKING SCHEMA
-- ============================================================================
--  WARNING: This script drops existing tables and data.
--  It sets up the schema for Secure Banking, Transaction Ledgers, and ACID compliance.

-- Enable pgcrypto for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. DROP EXISTING TABLES (Order matters for FK constraints)
DROP TRIGGER IF EXISTS update_balance_on_transaction ON public.transactions;
DROP FUNCTION IF EXISTS public.handle_transaction_balance;
DROP TABLE IF EXISTS public.transactions CASCADE;
DROP TABLE IF EXISTS public.accounts CASCADE;
DROP TABLE IF EXISTS public.user_profiles CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;

-- 2. CREATE TABLES

-- A. Users (Authentication & Identity)
-- Maps to Spring Security UserDetails
CREATE TABLE public.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL, -- BCrypt hash from Spring Security
    role TEXT DEFAULT 'ROLE_USER', -- 'ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN'
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- B. User Profiles (KYC Data)
-- Separates auth credentials from personal banking details
CREATE TABLE public.user_profiles (
    id UUID PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    phone_number TEXT,
    address TEXT,
    date_of_birth DATE,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- C. Accounts (Financial Containers)
-- The core entity holding the balance
CREATE TABLE public.accounts (
    id BIGSERIAL PRIMARY KEY, -- Long ID for easy internal indexing
    account_number TEXT UNIQUE NOT NULL, -- Public facing ID (e.g., 'KABAN-1001')
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    account_type TEXT DEFAULT 'SAVINGS', -- 'SAVINGS', 'CHECKING'
    balance NUMERIC(19, 4) DEFAULT 0.0000 CHECK (balance >= 0), -- Prevent negative balance at DB level
    currency TEXT DEFAULT 'PHP',
    status TEXT DEFAULT 'ACTIVE', -- 'ACTIVE', 'FROZEN', 'CLOSED'
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- D. Transactions (The Ledger)
-- Immutable record of every money movement.
CREATE TABLE public.transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id BIGINT REFERENCES public.accounts(id) ON DELETE NO ACTION, -- Never cascade delete a ledger!
    type TEXT NOT NULL, -- 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT'
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0), -- Always positive, logic determines sign
    description TEXT,
    reference_number TEXT UNIQUE, -- External reference
    status TEXT DEFAULT 'COMPLETED', -- 'PENDING', 'COMPLETED', 'FAILED'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. FUNCTIONS & TRIGGERS

-- A. Auto-create Profile on Signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_profiles (id, first_name, last_name)
    VALUES (new.id, 'New', 'Customer'); -- Defaults, updated later via API
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_user_created
AFTER INSERT ON public.users
FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- B. Automatic Balance Calculation Trigger
-- Updates the Account balance whenever a Transaction is inserted.
CREATE OR REPLACE FUNCTION public.handle_transaction_balance()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' THEN
        IF NEW.type IN ('DEPOSIT', 'TRANSFER_IN') THEN
            UPDATE public.accounts
            SET balance = balance + NEW.amount,
                updated_at = NOW()
            WHERE id = NEW.account_id;
        ELSIF NEW.type IN ('WITHDRAWAL', 'TRANSFER_OUT') THEN
            UPDATE public.accounts
            SET balance = balance - NEW.amount,
                updated_at = NOW()
            WHERE id = NEW.account_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER update_balance_on_transaction
AFTER INSERT ON public.transactions
FOR EACH ROW EXECUTE FUNCTION public.handle_transaction_balance();

-- 4. ROW LEVEL SECURITY (RLS)
-- Defense in Depth: Even if API fails, DB prevents unauthorized access.

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

-- Users
CREATE POLICY "Users can view own data" ON public.users
FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Admins can view user data" ON public.users
FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN'))
);

-- Profiles
CREATE POLICY "Users can view/edit own profile" ON public.user_profiles
FOR ALL USING (auth.uid() = id);

CREATE POLICY "Admins can view profiles" ON public.user_profiles
FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN'))
);

-- Accounts
CREATE POLICY "Users can view own accounts" ON public.accounts
FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "Admins can view all accounts" ON public.accounts
FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN'))
);

-- Transactions
CREATE POLICY "Users can view own transactions" ON public.transactions
FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.accounts WHERE id = public.transactions.account_id AND user_id = auth.uid())
);

CREATE POLICY "Admins can view all transactions" ON public.transactions
FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN'))
);