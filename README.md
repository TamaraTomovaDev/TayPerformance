# TayPerformance (Spring Boot) — MVP Backend

Backend API voor **Tay Performance** (garage/detailing) met:
- Publieke endpoints voor klanten (zonder login)
- Interne endpoints voor medewerkers (JWT beveiligd)
- Afsprakenbeheer + klantenbeheer
- SMS logging (audit trail) met “fake send” (Twilio later)

> Status: **MVP-ready backend** ✅  
> Focus: clean architecture, security basics, DTO’s, logging/audit.

---

## Tech stack

- Java 17
- Spring Boot 3.2.x
- Spring Web
- Spring Data JPA (Hibernate)
- Spring Security (JWT)
- Validation (Jakarta Validation)
- H2 (dev) / PostgreSQL (prod)
- Lombok
- JJWT (JWT library)
- libphonenumber (phone normalization)

---

## Projectstructuur (high level)

com.tayperformance
├─ config
│ ├─ SecurityConfig
│ ├─ JwtAuthenticationFilter
│ ├─ JwtProvider
│ └─ MethodSecurityConfig
├─ controller
│ ├─ publicapi
│ └─ internal
├─ dto
│ ├─ auth, appointment, customer, service, sms, user
├─ entity
│ ├─ Appointment, Customer, DetailService, SmsLog, User
│ └─ enums: AppointmentStatus, Role, SmsStatus, SmsType
├─ exception
│ ├─ GlobalExceptionHandler + custom exceptions
├─ mapper
│ ├─ AppointmentMapper, CustomerMapper, DetailServiceMapper, SmsLogMapper, UserMapper
├─ repository
│ ├─ AppointmentRepository, CustomerRepository, DetailServiceRepository, SmsLogRepository, UserRepository
├─ service
│ ├─ auth (AuthService + CustomUserDetailsService)
│ ├─ appointment (AppointmentService + helpers)
│ ├─ customer (CustomerService)
│ ├─ sms (SmsService + TwilioSmsService)
│ └─ user (UserService)
└─ util
└─ PhoneNumberHelper


---

## Profiles & configuratie

Je gebruikt 3 property files:

### `application.properties`
Algemene defaults (app name, timezone, json instellingen, port).

### `application-dev.properties` (DEV)
- H2 in-memory DB
- H2 console aan
- `ddl-auto=create-drop`
- SQL logging aan
- JWT secret hardcoded (alleen dev)

### `application-prod.properties` (PROD)
- PostgreSQL
- `ddl-auto=validate`
- SQL logging uit
- JWT secret via environment variables

---

## MVP checklist (wat AAN / wat UIT)

### ✅ AAN (MVP)
- `spring.profiles.active=dev` lokaal
- H2 console (dev only)
- JWT auth voor **/api/internal/**
- Public endpoints zonder login (**/api/public/**)
- DTO mapping (geen entities “lekken” in responses)
- SmsLog opslag (ook als SMS nog “fake” is)

### ❌ UIT (later)
- Twilio delivery webhooks (DELIVERED updates)
- Retry jobs (FAILED sms retry scheduler)
- Analytics dashboards
- Retention/archiving batch jobs

### ❗ Productie checklist (moet vóór launch)
- `spring.jpa.hibernate.ddl-auto=validate`
- Flyway migrations (schema versiebeheer)
- `spring.h2.console.enabled=false`
- JWT secret verplicht via ENV (`JWT_SECRET`)
- CORS restricties op jouw domein

---

## Security model

### Public
- `/api/public/**` is open (geen auth)

### Internal
- `/api/internal/**` vereist JWT
- Rollen:
    - `ADMIN`
    - `STAFF`

### JWT flow (high level)
1. `POST /api/public/auth/login` (voorbeeld)
2. Server valideert username/password
3. Server geeft JWT terug
4. Frontend stuurt `Authorization: Bearer <token>`
5. `JwtAuthenticationFilter` zet Authentication in SecurityContext

---

## Belangrijkste modules

### Afspraken (Appointment)
- Conflict detectie (overlappende tijden)
- Status workflow: REQUESTED → CONFIRMED → IN_PROGRESS → COMPLETED / CANCELED / NOSHOW
- Filtering per dag / periode / staff
- Customer history support

### Klanten (Customer)
- Minimalisatie persoonsgegevens (phone is primary)
- Soft delete via `active`
- Phone normalisatie via `PhoneNumberHelper` (E.164)

### Services (DetailService)
- Lijst van beschikbare services (naam, duur, prijs)
- Public list + internal CRUD

### SMS logging (SmsLog)
- Elke SMS poging wordt gelogd met:
    - type (CONFIRM/UPDATE/CANCEL/REMINDER)
    - status (QUEUED/SENT/DELIVERED/FAILED)
- MVP: “fake send” → enkel logging + status SENT
- Later: Twilio integratie + delivery callbacks

### Users (intern)
- Alleen medewerkers hebben accounts
- Klanten hebben **geen** accounts
- User management via internal admin endpoints

---

## API endpoints (globaal)

> Exacte endpoints kunnen verschillen per controller, maar structuur is:

### Public (`/api/public/**`)
- Auth (login)
- Public appointment booking / viewing (optioneel afhankelijk van implementatie)
- Services list
- Customer “self” acties indien voorzien

### Internal (`/api/internal/**`)
- Appointments CRUD + status updates
- Customers CRUD + search
- Detail services CRUD
- Sms logs listing / troubleshooting
- Users admin (ADMIN-only voor role/password/active)

---

## Run lokaal (DEV)

### 1) Start met dev profile
In IntelliJ:
- VM options: `-Dspring.profiles.active=dev`

Of via terminal:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
