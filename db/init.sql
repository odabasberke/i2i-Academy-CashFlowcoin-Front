-- =====================================================================
-- CryptoPal - Initial Database Schema (DDL)
-- Target: PostgreSQL 18.x
-- =====================================================================
-- ACID notes:
--   Atomicity / Isolation -> enforced at the application layer via
--     Spring's @Transactional. Wallet debit/credit and trade execution
--     must happen inside a single DB transaction; isolation level can
--     be raised to SERIALIZABLE for balance-critical operations.
--   Consistency -> enforced here via FOREIGN KEY, CHECK, NOT NULL and
--     UNIQUE constraints, so the database rejects invalid states rather
--     than relying on application code alone.
--   Durability -> provided natively by PostgreSQL's WAL; no schema
--     action needed, but it's why fsync must never be disabled in prod.
--
-- Financial amounts use NUMERIC (fixed-point), never FLOAT/DOUBLE, to
-- avoid binary floating-point rounding errors on money.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Enum types
-- ---------------------------------------------------------------------
CREATE TYPE user_role AS ENUM ('USER', 'PREMIUM', 'ADMIN');
CREATE TYPE tx_type   AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'BUY', 'SELL', 'FEE');
CREATE TYPE tx_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED');

-- ---------------------------------------------------------------------
-- Shared trigger: keep updated_at current on every UPDATE
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- 1. users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                BIGSERIAL PRIMARY KEY,
    public_id         UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE, -- safe to expose via API
    username          VARCHAR(50)  NOT NULL UNIQUE,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    role              user_role NOT NULL DEFAULT 'USER',
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_username_length CHECK (char_length(username) >= 3),
    CONSTRAINT chk_email_format CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);

CREATE INDEX idx_users_email ON users (email);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 2. wallets  (one row per user per currency)
-- ---------------------------------------------------------------------
CREATE TABLE wallets (
    id              BIGSERIAL PRIMARY KEY,
    public_id       UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_id         BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    currency_code   VARCHAR(10) NOT NULL,                 -- BTC, ETH, USDT...
    balance         NUMERIC(30, 10) NOT NULL DEFAULT 0,
    locked_balance  NUMERIC(30, 10) NOT NULL DEFAULT 0,    -- reserved by open orders
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_wallet_user_currency UNIQUE (user_id, currency_code),
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_locked_non_negative CHECK (locked_balance >= 0)
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);

CREATE TRIGGER trg_wallets_updated_at
    BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 3. transaction_logs  (append-only ledger of every balance change)
-- ---------------------------------------------------------------------
CREATE TABLE transaction_logs (
    id             BIGSERIAL PRIMARY KEY,
    public_id      UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_id        BIGINT NOT NULL REFERENCES users (id)   ON DELETE RESTRICT,
    wallet_id      BIGINT NOT NULL REFERENCES wallets (id) ON DELETE RESTRICT,
    type           tx_type NOT NULL,
    status         tx_status NOT NULL DEFAULT 'PENDING',
    currency_pair  VARCHAR(20),                 -- e.g. BTC/USDT; null for deposit/withdrawal
    amount         NUMERIC(30, 10) NOT NULL,
    price          NUMERIC(30, 10),             -- execution price for BUY/SELL
    fee            NUMERIC(30, 10) NOT NULL DEFAULT 0,
    balance_after  NUMERIC(30, 10) NOT NULL,     -- wallet balance snapshot post-transaction
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMPTZ,

    CONSTRAINT chk_amount_nonzero CHECK (amount <> 0),
    CONSTRAINT chk_fee_non_negative CHECK (fee >= 0)
);

CREATE INDEX idx_txlogs_user_id ON transaction_logs (user_id);
CREATE INDEX idx_txlogs_wallet_id ON transaction_logs (wallet_id);
CREATE INDEX idx_txlogs_created_at ON transaction_logs (created_at DESC);
CREATE INDEX idx_txlogs_pending ON transaction_logs (status) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------
-- 4. price_trends  (OHLCV candles for historical charting)
-- ---------------------------------------------------------------------
CREATE TABLE price_trends (
    id             BIGSERIAL PRIMARY KEY,
    symbol         VARCHAR(20) NOT NULL,        -- e.g. BTCUSDT
    interval_type  VARCHAR(10) NOT NULL,        -- 1m, 5m, 1h, 1d...
    open_time      TIMESTAMPTZ NOT NULL,
    close_time     TIMESTAMPTZ NOT NULL,
    open_price     NUMERIC(30, 10) NOT NULL,
    high_price     NUMERIC(30, 10) NOT NULL,
    low_price      NUMERIC(30, 10) NOT NULL,
    close_price    NUMERIC(30, 10) NOT NULL,
    volume         NUMERIC(30, 10) NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_trend_symbol_interval_time UNIQUE (symbol, interval_type, open_time),
    CONSTRAINT chk_high_low CHECK (high_price >= low_price)
);

CREATE INDEX idx_trends_symbol_time ON price_trends (symbol, interval_type, open_time DESC);
