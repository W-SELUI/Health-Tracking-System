# Health Tracking System

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![Database](https://img.shields.io/badge/Database-MySQL%20%7C%20H2-blue)

## About

The **Health Tracking System** is a web application developed for **CS241 Assignment 2** by Group 15. It provides patients with a central place to manage health information and gives authorised healthcare providers controlled access to patient records.

The application is built with Java 25, Spring Boot, Spring Security, Spring Data JPA, Thymeleaf, and MySQL. H2 is also available for runtime and test environments.

## Features

- Patient registration, login, profiles, and role-based dashboards
- Health record creation, editing, viewing, and validation
- Blood-pressure tracking and health analytics
- Daily activity logging, history, date filtering, and summaries
- Medication, dosage, schedule, adherence, and appointment tracking
- Patient-provider consent and access requests
- Provider access to authorised patient health records
- Patient-to-provider messaging and replies
- Provider verification and administrative user management
- CSRF protection, input validation, ownership checks, and role restrictions

## User roles

| Role | Main capabilities |
| --- | --- |
| Patient | Manage personal records, activities, medication, appointments, consent, and messages |
| Provider | Request consent and view health information for authorised patients |
| Administrator | Manage users and verify providers |

## Technology stack

- Java 25
- Spring Boot
- Spring MVC and Thymeleaf
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- H2
- Maven

## Prerequisites

- JDK 25
- MySQL 8 or another compatible MySQL installation
- Git

The repository includes Maven Wrapper scripts, so Maven does not need to be installed separately.

## Getting started

### 1. Clone the repository

```bash
git clone https://github.com/W-SELUI/Health-Tracking-System.git
cd Health-Tracking-System
```

### 2. Create the database

Create a MySQL database named `health_tracking_system`:

```sql
CREATE DATABASE health_tracking_system;
```

### 3. Configure the database

The application reads configuration from environment variables. The defaults are suitable for a local MySQL server using the `root` account, but setting the values explicitly is recommended:

```bash
export DB_URL="jdbc:mysql://localhost:3306/health_tracking_system?useSSL=false&serverTimezone=UTC"
export DB_USERNAME="root"
export DB_PASSWORD="your-password"
```

On Windows PowerShell:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/health_tracking_system?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
```

### 4. Run the application

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

On Windows:

```bat
mvnw.cmd spring-boot:run
```

Open [http://localhost:8081](http://localhost:8081). The port can be changed with the `PORT` environment variable.

## Running tests

Run the automated test suite with:

```bash
./mvnw test
```

On Windows:

```bat
mvnw.cmd test
```

The tests cover application behaviour, validation, authentication, role boundaries, consent workflows, provider access, messaging, activities, medication features, and analytics.

## Docker

Build and run the application with Docker:

```bash
docker build -t health-tracking-system .
docker run --rm -p 8080:8080 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/health_tracking_system?useSSL=false&serverTimezone=UTC" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="your-password" \
  health-tracking-system
```

For hosted deployments, provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and optionally `PORT`. The Docker image is configured for Java 25 and uses the platform-provided port.

## Project structure

```text
src/
├── main/java/com/group15/health_tracking_system/
│   ├── activity/       Activity logging
│   ├── analytics/      Patient and provider analytics
│   ├── consent/        Consent and provider access
│   ├── health/         Health records and blood pressure
│   ├── medication/     Medication, dosage, schedules, and appointments
│   ├── message/        Patient-provider messaging
│   └── user/           Authentication, profiles, and administration
├── main/resources/
│   ├── templates/      Thymeleaf views
│   └── application.properties
└── test/               Integration and application tests
```

## Team

This project was completed by:

- **William Selui** — [@W-SELUI](https://github.com/W-SELUI)
- **Jiya Reddy** — [@Jiya-Yashaswi-Reddy](https://github.com/Jiya-Yashaswi-Reddy)
- **Shaniza Shania Ali** — [@ali-shaniza](https://github.com/ali-shaniza)
- **Tasmiyah Hussein** — [@Tasmiyah-Hussein](https://github.com/Tasmiyah-Hussein)
- **Yash Dass** — [@Yash-Dass](https://github.com/Yash-Dass)

## Academic context

**Course:** CS241  
**Assessment:** Assignment 2  
**Group:** Group 15

This repository contains the team's implementation and supporting tests for the assignment.

## Notes

- Do not commit database passwords or other secrets.
- `spring.jpa.hibernate.ddl-auto=update` is intended for development and assignment deployment environments. Use a migration strategy before using the application in production.
- The application supports a bootstrap administrator through `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_FIRST_NAME`, and `ADMIN_LAST_NAME`.
