# TayPerformance (Spring Boot) — MVP Backend

Backend API voor **Tay Performance** (garage / detailing) met:
- **Public API** voor klantgerichte acties (zonder login)
- **Internal API** voor medewerkers (JWT beveiligd, roles)
- **Afsprakenbeheer + klantenbeheer + garage settings**
- **SMS logging (audit trail)** + optioneel echte verzending via **Twilio** (kan “fake send” zijn in MVP)

> Status: **MVP-ready backend** ✅  
> Doel: stabiele basis (security, DTO’s, logging, migrations) waarop de Angular admin app kan bouwen.

---

## Inhoud
- [Tech stack](#tech-stack)
- [Architectuur & packages](#architectuur--packages)
- [Profiles & configuratie](#profiles--configuratie)
- [Database & migrations](#database--migrations)
- [Security model (JWT)](#security-model-jwt)
- [Domeinen](#domeinen)
- [API Endpoints](#api-endpoints)
- [SMS / Twilio](#sms--twilio)
- [Run lokaal (DEV)](#run-lokaal-dev)
- [Build & run (PROD)](#build--run-prod)
- [Troubleshooting](#troubleshooting)

---

## Tech stack

- Java 17
- Spring Boot 3.2.x
- Spring Web
- Spring Data JPA (Hibernate 6)
- Spring Security 6 (JWT, stateless)
- Jakarta Validation (DTO validation)
- PostgreSQL (prod: Neon) / H2 of lokale PostgreSQL (dev)
- Flyway (schema versiebeheer)
- Lombok
- JJWT (JWT library)
- libphonenumber (phone normalisatie)

---

## Architectuur & packages

**High level structuur** (kan licht verschillen per commit, maar dit is de intentie):

com.tayperformance
├─ config / security
│ ├─ SecurityConfig
│ ├─ JwtAuthenticationFilter
│ ├─ JwtProvider
│ └─ MethodSecurityConfig
├─ controller
│ ├─ publicapi // /api/public/**
│ └─ internalapi // /api/internal/**
├─ dto
│ ├─ auth
│ ├─ appointment
│ ├─ customer
│ ├─ settings
│ ├─ sms
│ └─ user
├─ entity
│ ├─ Appointment
│ ├─ Customer
│ ├─ GarageSettings
│ ├─ SmsLog
│ ├─ User
│ └─ enums: AppointmentStatus, Role, SmsStatus, SmsType
├─ exception
│ ├─ GlobalExceptionHandler
│ └─ custom exceptions (NotFound, Conflict, Validation, ...)
├─ mapper
│ ├─ AppointmentMapper
│ ├─ CustomerMapper
│ ├─ SettingsMapper
│ └─ SmsLogMapper
├─ repository
│ ├─ AppointmentRepository
│ ├─ CustomerRepository
│ ├─ GarageSettingsRepository
│ ├─ SmsLogRepository
│ └─ UserRepository
├─ service
│ ├─ auth (AuthService + CustomUserDetailsService)
│ ├─ appointment (AppointmentService)
│ ├─ customer (CustomerService)
│ ├─ settings (SettingsService)
│ ├─ sms (SmsService + TwilioSmsService/Fake)
│ └─ user (UserService)
└─ util
└─ PhoneNumberHelper


**Principes**
- Controllers spreken in **DTO’s**, niet in entities.
- Business logica zit in services (controllers dun).
- Database schema via Flyway, `ddl-auto=validate` in prod.

---

## Profiles & configuratie

### `application.properties`
Algemene defaults (app name, server port, timezone/JSON instellingen, logging basis).

### `application-dev.properties` (DEV)
- H2 (in-memory) of lokale DB
- H2 console aan (dev only)
- `spring.jpa.hibernate.ddl-auto=create-drop` (alleen dev)
- SQL logging kan aan
- JWT secret kan hardcoded (dev only)

### `application-prod.properties` (PROD)
- PostgreSQL (Neon)
- `spring.jpa.hibernate.ddl-auto=validate`
- SQL logging uit
- JWT secret via environment variable

> ✅ Productie: **nooit** `create-drop`. Altijd Flyway + validate.

---

## Database & migrations

### Flyway
Migrations staan in:
src/main/resources/db/migration/


Minstens:
- `V1__init.sql`
- `V2__create_garage_settings.sql`

> Bij prod deployments: Flyway voert nieuwe migrations uit, Hibernate valideert (`validate`) dat entities overeenkomen.

### Datamodellen (conceptueel)
- `appointments`
- `customers`
- `garage_settings`
- `sms_logs`
- `users`

> In MVP is “delete” meestal **soft** via status (`CANCELED`) of `active=false`.

---

## Security model (JWT)

### Public
- `/api/public/**` open (geen auth)

### Internal
- `/api/internal/**` vereist JWT
- Roles:
    - `ADMIN`
    - `STAFF`

### JWT flow
1. `POST /api/public/auth/login`
2. Server valideert credentials
3. Server geeft `{ token }` terug
4. Client stuurt: `Authorization: Bearer <token>`
5. `JwtAuthenticationFilter` zet `Authentication` in `SecurityContext`

---

## Domeinen

### Appointments (Afspraak)
- Status workflow:
  `REQUESTED → CONFIRMED → IN_PROGRESS → COMPLETED`
  en uitzonderingen: `CANCELED`, `NOSHOW`
- Admin kan:
    - create confirmed
    - update (PATCH)
    - cancel (status = CANCELED)
    - start/complete/noshow

### Customers (Klant)
- Primary identifier: **phone** (E.164)
- `active` flag voor soft delete
- phone normalisatie via `PhoneNumberHelper` / libphonenumber

### Settings (Garage Settings)
- Garage profiel (naam, adres, telefoon, etc.)
- SMS templates (confirmation/ready/cancel/...)
- Opslag in DB zodat admin het kan aanpassen zonder deploy

### SMS Logs
- Elke “send attempt” wordt gelogd:
    - toNumber
    - message body (of template id + resolved text)
    - status: `SENT/FAILED/...`
    - error message (bij failed)
    - link naar appointment (optioneel)
- MVP kan “fake send” doen (logging) of echte Twilio

### Users (Medewerkers)
- Alleen medewerkers hebben accounts
- Klanten hebben **geen** accounts
- ADMIN kan accounts beheren (optioneel in MVP)

---

## API Endpoints

> Base path: `http://localhost:8080/api`

### Public — `/api/public/**`

#### Auth
**POST** `/api/public/auth/login`

Request:
```json
{ "username": "…", "password": "…" }
Response:

{ "token": "JWT_TOKEN" }
Public appointment booking kan bestaan in latere fase (website), maar admin app gebruikt vooral internal.

Internal — /api/internal/** (JWT required)
Appointments
Base: /api/internal/appointments

GET /api/internal/appointments?q=&page=&size=
→ Spring Page response { content, totalElements, ... }

GET /api/internal/appointments/{id}
→ AppointmentResponse

POST /api/internal/appointments
→ create confirmed appointment (CONFIRMED)
Body: CreateAppointmentRequest

PATCH /api/internal/appointments/{id}
→ update (partial)
Body: UpdateAppointmentRequest

POST /api/internal/appointments/{id}/confirm
→ REQUESTED → CONFIRMED
Body: UpdateAppointmentRequest

POST /api/internal/appointments/{id}/cancel?reason=...
→ cancel (soft delete)
Let op: reason is query param (optioneel)

POST /api/internal/appointments/{id}/start

POST /api/internal/appointments/{id}/complete

POST /api/internal/appointments/{id}/noshow

Customers
Base: /api/internal/customers

GET /api/internal/customers

POST /api/internal/customers

Settings
Base: /api/internal/settings

GET /api/internal/settings

PUT /api/internal/settings

SMS Logs
Base: /api/internal/sms-logs

GET /api/internal/sms-logs

GET /api/internal/sms-logs/failed?sinceDays=7

GET /api/internal/sms-logs/appointments/{appointmentId}

SMS / Twilio
MVP gedrag
SMS kan in MVP op “fake send” staan:

backend logt in sms_logs

status SENT (of QUEUED) zonder echte provider call

Twilio integratie (later / optioneel)
TwilioSmsService gebruikt Twilio credentials uit env vars:

TWILIO_ACCOUNT_SID

TWILIO_AUTH_TOKEN

TWILIO_FROM_NUMBER

In productie: failures worden gelogd als FAILED met error message

Later uitbreiden met:

delivery webhooks (DELIVERED updates)

retry jobs / scheduler

Frontend doet geen Twilio calls. Alles gebeurt server-side.

Run lokaal (DEV)
1) Start met dev profile
IntelliJ VM options

-Dspring.profiles.active=dev
Terminal

mvn spring-boot:run -Dspring-boot.run.profiles=dev
2) API testen
Login: POST /api/public/auth/login

Gebruik token voor internal endpoints

Build & run (PROD)
Zet env vars:

JWT_SECRET

SPRING_DATASOURCE_URL

SPRING_DATASOURCE_USERNAME

SPRING_DATASOURCE_PASSWORD

(optioneel Twilio vars)

Run:

mvn clean package
java -jar target/tayperformance-*.jar --spring.profiles.active=prod
Troubleshooting
401 / Unauthorized op internal endpoints
Token ontbreekt of is verlopen

Header moet exact:
Authorization: Bearer <token>

Flyway errors / schema mismatch
Check db/migration scripts

In prod: ddl-auto=validate → schema moet overeenkomen

Cancel endpoint werkt niet
Backend verwacht:
POST /api/internal/appointments/{id}/cancel?reason=...

Niet { reason } in JSON body

Price / BigDecimal issues
price kan string of number terugkomen (frontend moet beide kunnen tonen)

MVP status
✅ Auth (JWT)
✅ Internal appointments lifecycle
✅ Customers basics
✅ Settings (DB)
✅ SMS logging
✅ Flyway migrations basis

🚧 Later:

Public website booking flow

REQUESTED inbox in admin

Twilio delivery webhooks + retries

Analytics dashboards

Rate limiting & CORS stricterTayPerformance — Enterprise-Ready Garage Management Platform

TayPerformance is a production-oriented garage & automotive service management platform designed with clean architecture, security best practices, and scalability in mind.

It provides a secure internal operations system combined with a mobile-first administrative interface optimized for real-world usage in automotive service environments.

🏗 System Architecture

The platform follows a layered, domain-driven structure with strict separation of concerns:

Controller layer (Public / Internal API separation)

Service layer (business logic & workflow control)

Repository layer (data access abstraction)

DTO mapping layer (no entity leakage)

Security layer (JWT-based authentication)

Infrastructure services (SMS, phone normalization, logging)

The backend is fully stateless and built for horizontal scalability.

🔐 Security & Access Control

Stateless JWT authentication

Role-based authorization (ADMIN, STAFF)

Public and internal API separation

Method-level security

Secure password hashing (BCrypt)

Environment-based secret configuration

Production-ready CORS configuration

All internal endpoints are protected and require valid Bearer tokens.

📅 Appointment Management Engine

The appointment system is designed as a structured workflow engine:

Status lifecycle:

REQUESTED

CONFIRMED

IN_PROGRESS

COMPLETED

CANCELED

NOSHOW

Conflict detection (overlapping time validation)

Staff assignment logic

Soft-delete via status transitions

Efficient daily/monthly filtering

Optimized frontend caching for performance

The system ensures operational consistency while remaining flexible for business growth.

📩 SMS Notification Infrastructure

A modular SMS infrastructure layer supports:

Confirmation notifications

Cancellation messages

Reminder messages

Status tracking (QUEUED, SENT, DELIVERED, FAILED)

Persistent SMS audit logs

Twilio integration is prepared via a service abstraction, enabling:

Fake send mode (development)

Real production SMS delivery

Delivery webhook support (future-ready)

🧠 Database Design

PostgreSQL (Neon Cloud production)

H2 in-memory (development)

Flyway migration-ready

Strict schema validation in production

Normalized relational structure

Entities include:

Appointment

Customer

DetailService

SmsLog

User

Phone numbers are normalized to E.164 format using libphonenumber.

📱 Mobile-First Admin Interface

The Angular admin application is designed as a mobile-first operational tool:

Smartphone-optimized layout

Drawer-based editing

Real-time appointment updates

Local cache store for reduced API load

OnPush change detection strategy

Dirty-form detection with confirmation modal

Toast notification feedback system

The interface minimizes friction for field usage.

⚙️ DevOps & Environment Strategy

Profile-based configuration (dev / prod)

Environment variable secrets

SQL logging only in development

No in-memory database in production

Production-safe Hibernate configuration (ddl-auto=validate)

The system is built to be deployment-ready without structural changes.

🛠 Technology Stack

Backend:

Java 17

Spring Boot 3.x

Spring Security 6

Spring Data JPA

PostgreSQL

Flyway

JJWT

Lombok

libphonenumber

Twilio SDK

Frontend:

Angular 17+

Standalone components

RxJS

TypeScript

Modern mobile UI architecture

📈 Scalability Vision

Designed to support:

Multi-staff expansion

Public booking portal

Analytics dashboards

Invoice generation

Multi-location support

Advanced reporting

Notification automation

🎯 Strategic Goal

To provide a robust, secure, and scalable management platform capable of supporting the digital transformation of small-to-medium automotive service businesses.

🔥 Ultra-Short Enterprise GitHub Description

Enterprise-ready garage management platform built with Spring Boot & Angular. JWT-secured internal API, PostgreSQL cloud database, SMS notification engine (Twilio-ready), and mobile-first admin interface.