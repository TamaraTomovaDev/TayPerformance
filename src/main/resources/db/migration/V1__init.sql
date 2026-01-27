-- V1__init.sql
-- PostgreSQL schema voor Tay Performance (basis)

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- USERS
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     version BIGINT,
                                     username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- CUSTOMERS
CREATE TABLE IF NOT EXISTS customers (
                                         id BIGSERIAL PRIMARY KEY,
                                         version BIGINT,
                                         phone VARCHAR(30) NOT NULL UNIQUE,
    first_name VARCHAR(80),
    last_name VARCHAR(80),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);

-- SERVICES
CREATE TABLE IF NOT EXISTS services (
                                        id BIGSERIAL PRIMARY KEY,
                                        version BIGINT,
                                        name VARCHAR(120) NOT NULL,
    description TEXT,
    min_minutes INT NOT NULL,
    max_minutes INT NOT NULL,
    default_minutes INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_service_minutes
    CHECK (min_minutes > 0 AND max_minutes >= min_minutes AND default_minutes BETWEEN min_minutes AND max_minutes)
    );

CREATE INDEX IF NOT EXISTS idx_services_active ON services(active);
CREATE INDEX IF NOT EXISTS idx_services_name ON services(name);

-- APPOINTMENTS
CREATE TABLE IF NOT EXISTS appointments (
                                            id BIGSERIAL PRIMARY KEY,
                                            version BIGINT,

                                            customer_id BIGINT NOT NULL REFERENCES customers(id),
    service_id BIGINT REFERENCES services(id),

    assigned_staff_id BIGINT REFERENCES users(id),
    created_by_user_id BIGINT REFERENCES users(id),
    updated_by_user_id BIGINT REFERENCES users(id),

    car_brand VARCHAR(80) NOT NULL,
    car_model VARCHAR(80),
    description TEXT,

    start_time TIMESTAMPTZ NOT NULL,
    end_time   TIMESTAMPTZ NOT NULL,
    duration_minutes INT NOT NULL,

    price NUMERIC(10,2),

    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_duration_positive CHECK (duration_minutes > 0)
    );

CREATE INDEX IF NOT EXISTS idx_appt_start_time ON appointments(start_time);
CREATE INDEX IF NOT EXISTS idx_appt_status ON appointments(status);
CREATE INDEX IF NOT EXISTS idx_appt_customer_start ON appointments(customer_id, start_time);
CREATE INDEX IF NOT EXISTS idx_appt_assigned_start ON appointments(assigned_staff_id, start_time);

-- RANGE kolom (generated) voor overlap-detectie
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS time_range tstzrange
    GENERATED ALWAYS AS (tstzrange(start_time, end_time, '[)')) STORED;

-- Geen overlap per medewerker (CONFIRMED/RESCHEDULED blokkeren)
-- Opmerking: als assigned_staff_id NULL is, laat Postgres overlaps toe.
-- Dat is oké voor REQUESTED, en pas bij CONFIRMED geef je assigned_staff_id.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'no_overlapping_per_staff'
  ) THEN
ALTER TABLE appointments
    ADD CONSTRAINT no_overlapping_per_staff
    EXCLUDE USING gist (
        assigned_staff_id WITH =,
        time_range WITH &&
      )
      WHERE (status IN ('CONFIRMED', 'RESCHEDULED'));
END IF;
END $$;

-- SMS LOGS
CREATE TABLE IF NOT EXISTS sms_logs (
                                        id BIGSERIAL PRIMARY KEY,
                                        version BIGINT,
                                        appointment_id BIGINT NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    to_phone VARCHAR(30) NOT NULL,
    provider_message_id VARCHAR(120),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_sms_appt ON sms_logs(appointment_id);
CREATE INDEX IF NOT EXISTS idx_sms_status ON sms_logs(status);
CREATE INDEX IF NOT EXISTS idx_sms_created ON sms_logs(created_at);

-- (Optioneel) audit logs
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGSERIAL PRIMARY KEY,
                                          entity_type VARCHAR(60) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(60) NOT NULL,
    details_json TEXT,
    actor_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(actor_user_id);
