-- This file runs on startup to keep the local PostgreSQL schema aligned.
-- It is intentionally idempotent and uses `continue-on-error` to tolerate already-applied changes.

-- Rename legacy column (no IF EXISTS for RENAME; safe due to continue-on-error).
ALTER TABLE public.users RENAME COLUMN enabled TO is_verified;

-- Add verification columns.
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_verified boolean NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS email_verified_at timestamptz NULL;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS email_verification_last_sent_at timestamptz NULL;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS email_verification_send_count_24h integer NOT NULL DEFAULT 0;

-- Add token lifecycle columns.
ALTER TABLE public.verification_tokens ADD COLUMN IF NOT EXISTS created_at timestamptz NULL;
ALTER TABLE public.verification_tokens ADD COLUMN IF NOT EXISTS consumed_at timestamptz NULL;
ALTER TABLE public.verification_tokens ADD COLUMN IF NOT EXISTS revoked_at timestamptz NULL;

