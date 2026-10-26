# ResQHub — Integrated Disaster Response Coordination System

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021-orange.svg?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Architecture](https://img.shields.io/badge/Architecture-Zero--ORM%20Direct%20JDBC-blue.svg)](#architectural-highlights)
[![Database](https://img.shields.io/badge/Database-MySQL%208.0-blue.svg?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Frontend](https://img.shields.io/badge/Frontend-Thymeleaf%20%2B%20Bootstrap%205.3-teal.svg?logo=bootstrap&logoColor=white)](https://getbootstrap.com/)
[![Security](https://img.shields.io/badge/Security-Spring%20Security%206%20%28RBAC%29-red.svg?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)

**Help. Hope. Humanity.**

*An enterprise-grade, browser-based emergency coordination platform unifying disaster relief agencies, frontline rescue teams, shelter administrators, community volunteers, and citizens during humanitarian crises.*

[Key Features](#key-features) • [Architecture](#architectural-highlights) • [Roles & Permissions](#role-based-access-matrix) • [Quick Start](#quick-start-guide) • [Test Personas](#sample-data--login-credentials) • [Project Layout](#project-layout)

---

</div>

## Overview

During major natural catastrophes—such as catastrophic landslides, monsoonal river deluges, and flash flooding—fragmented communications cause fatal delays. **ResQHub** brings frontline operations into a unified command and coordination system:

- Real-time emergency incident logging and severity classification.
- Priority-weighted victim rescue dispatching with automated urgency scores.
- Evacuee tracking, medical triage, and smart shelter capacity allocation.
- End-to-end community food distribution planning and inventory buffers.
- Logistics warehouse inventory tracking and humanitarian donation management.
- Dynamic task dispatch for accredited and local community volunteers.

---

## Key Features

### 1. Incident & Disaster Management
- Unified disaster logs categorized by type (`FLOOD`, `LANDSLIDE`, `CYCLONE`, `EARTHQUAKE`, `FIRE`, `INDUSTRIAL`).
- Live tracking of affected populations, operational status (`ACTIVE`, `CONTAINED`, `RESOLVED`), and severity ratings.
- Automated linkage between disasters, active evacuation shelters, victim rosters, and rescue missions.

### 2. Rescue Operations & Intelligent Triage
- Urgent citizen and field SOS request registry with GPS/location tagging.
- **Smart Priority Engine**: Evaluates life-threatening situations, stranded children/seniors, rising water levels, and medical needs to assign weighted priorities (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
- Team assignment and dispatch lifecycle tracking: `PENDING` &rarr; `ASSIGNED` &rarr; `EN_ROUTE` &rarr; `ON_SITE` &rarr; `COMPLETED`.

### 3. Evacuee Registry & Smart Shelter Allocation
- Comprehensive victim registry tracking displacement history, medical conditions, and family groupings.
- **Smart Allocation Engine**: Analyzes real-time shelter capacity, proximity, and infrastructure to recommend the optimal relief camp for displaced groups, mitigating overcrowding.
- Check-in, bed assignment, and transfer management across certified relief centers.

### 4. Food Distribution & Nutrition Management
- Multi-tier meal requirement calculations based on beneficiary headcount and per-person dietary allocations.
- Automated food shortage alert triggers based on warehouse stock buffer levels.
- Full tracking of distribution events, delivery batches, transit boats/vehicles, and receiving camp coordinators.

### 5. Inventory Logistics & Stock Movements
- Multi-category inventory management (`FOOD`, `WATER`, `MEDICAL_SUPPLIES`, `RESCUE_EQUIPMENT`, `SHELTER_SUPPLIES`, `CLOTHING`).
- Safety reorder thresholds with instant low-stock notifications.
- Complete audit trail of stock-ins, warehouse transfers, and emergency dispatches.

### 6. Humanitarian Aid & Relief Coordination
- Cash and in-kind donation pledge management with transparent status tracking (`PLEDGED`, `RECEIVED`, `DISTRIBUTED`).
- Public landing page coordination hub connecting donors directly to designated Central Administrators, Rescue Managers, and Camp Managers.

### 7. Volunteer Mobilization & "My Tasks"
- Volunteer registration with emergency skill sets, certifications, contact info, and maximum workload thresholds.
- Dedicated **My Tasks** portal enabling volunteers to view assigned relief missions, update assignment progress, and coordinate on-site.

### 8. Situational Analytics & Reporting
- Role-based operational dashboards with live KPI counters and status charts.
- Exportable CSV reports and printer-friendly summaries across all operational domains.

---

## Architectural Highlights

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ResQHub Web Client                              │
│         (HTML5, Vanilla CSS Design System, Bootstrap 5.3, JS)          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTP / HTTPS
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     Spring Boot 3.2.4 Application                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │     Spring Security 6 (RBAC, Form Auth, Session Management)      │  │
│  └────────────────────────────────┬─────────────────────────────────┘  │
│                                   ▼                                    │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │           Spring Web MVC Controllers & Thymeleaf 3 Engine         │  │
│  └────────────────────────────────┬─────────────────────────────────┘  │
│                                   ▼                                    │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │         Business Services & Intelligent Priority Algorithms      │  │
│  └────────────────────────────────┬─────────────────────────────────┘  │
│                                   ▼                                    │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │          Zero-ORM Pure JDBC Data Access Objects (DAOs)           │  │
│  │           (PreparedStatement, GeneratedKeys, Direct SQL)         │  │
│  └────────────────────────────────┬─────────────────────────────────┘  │
│                                   ▼                                    │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                HikariCP High-Performance Pool                    │  │
│  └────────────────────────────────┬─────────────────────────────────┘  │
└───────────────────────────────────┼────────────────────────────────────┘
                                    │ JDBC Connection
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     MySQL 8.0 Relational Engine                        │
│               (27 Tables, Strict Constraints, UTF-8 MB4)               │
└────────────────────────────────────────────────────────────────────────┘
```

- **Zero-ORM Direct JDBC Architecture**: ResQHub deliberately avoids the memory overhead, lazy-loading pitfalls, and unexpected query cascades of heavy ORMs (like Hibernate/JPA). All database interactions are executed via explicit, parameterized JDBC queries with deterministic execution times.
- **HikariCP Connection Pooling**: High-throughput connection reuse configured in `application.properties` ensures stability under high-traffic crisis surges.
- **100% Offline-Ready Asset Bundling**: All CSS frameworks (Bootstrap 5.3) and icon fonts (Bootstrap Icons) are packaged locally in the application bundle—allowing operation even when wide-area internet connectivity is severed.
- **Harmonious Color Palette**:
  - Forest Green: `#285943` (Primary Navigation & Authority)
  - Dark Green: `#173D30` (Header Gradients & Depth)
  - Earth Brown: `#795548` (Secondary Action Tokens)
  - Warm Humanitarian Saffron: `#E65100` (Relief Aid & Donation CTAs)
  - Light Sage: `#EAF3E9` (Soft Card Backgrounds)
  - Off-White: `#FCFBF8` (Body Background)

---

## Role-Based Access Matrix

| Module / Operation | Administrator | Rescue Officer | Camp Manager | Volunteer | Citizen |
|---|:---:|:---:|:---:|:---:|:---:|
| **Dashboard** | Full Overview | Operational | Camp Focus | Task Focus | Public / SOS |
| **Disasters** | Create / Edit / View | Create / Edit / View | View Only | View Only | View Only |
| **Rescue Requests** | Full Access | Triage & Dispatch | View Only | Assigned Tasks | Submit SOS |
| **Rescue Teams** | Manage Teams | Manage & Assign | View Only | View Only | &mdash; |
| **Victims Registry** | Full Access | Full Access | Manage (Camp) | View Only | &mdash; |
| **Shelters** | Full Access | View Only | Manage Occupancy | View Only | View Only |
| **Smart Allocation** | Full Access | View Only | Run Allocation | &mdash; | &mdash; |
| **Food Distribution** | Full Access | Full Access | Create & Distribute | View / Deliver | &mdash; |
| **Resources / Stock** | Full Access | Requisition | Manage Camp Buffer | View Only | &mdash; |
| **Donations** | Full Access | Full Access | View Materials | &mdash; | Contact Officials |
| **Volunteer Roster** | Manage Roster | Assign Tasks | View Available | **My Tasks** | Volunteer Signup |
| **User Management** | Full Control | &mdash; | &mdash; | &mdash; | &mdash; |
| **Reports & CSV Export** | All Reports | Operational | Camp Reports | &mdash; | &mdash; |

---

## Technical Specifications

| Component | Specification |
|---|---|
| **Runtime Environment** | Java Standard Edition 17 or 21 LTS |
| **Core Framework** | Spring Boot 3.2.4 (Embedded Apache Tomcat) |
| **Security Framework** | Spring Security 6 with PBKDF2 Password Hashing |
| **Persistence Layer** | Zero-ORM direct JDBC via `com.mysql:mysql-connector-j` |
| **Connection Pool** | HikariCP (Connection timeout: 20s, Max pool: 20) |
| **View Template Engine** | Thymeleaf 3 with Spring Security 6 dialect |
| **Frontend UI Toolkit** | Bootstrap 5.3 (bundled locally) + Bootstrap Icons 1.11 |
| **Database Server** | MySQL Community Server 8.0+ |
| **Build System** | Apache Maven 3.8+ |

---

## Quick Start Guide

### 1. Prerequisites
Ensure the following tools are installed on your machine:
- **JDK 17 or JDK 21** (`java -version`)
- **Apache Maven 3.8+** (`mvn -v`)
- **MySQL Server 8.0+** (`mysql --version`)

---

### 2. Database Initialization

Log into MySQL as an administrative user and execute the schema and sample data scripts:

```bash
# 1. Create the database and schema
mysql -u root -p < database/schema.sql

# 2. Populate verified sample data (disasters, shelters, teams, food distributions, etc.)
mysql -u root -p < database/sample_data.sql
```

Create the dedicated application user (as configured by default):

```sql
CREATE USER 'resqhub_app'@'localhost' IDENTIFIED BY 'ResQHub@123';
GRANT ALL PRIVILEGES ON resqhub_web.* TO 'resqhub_app'@'localhost';
FLUSH PRIVILEGES;
```

---

### 3. Application Configuration

Verify database credentials in [`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/resqhub_web?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
spring.datasource.username=resqhub_app
spring.datasource.password=ResQHub@123
```

*(You can override these properties using standard environment variables or JVM flags if deploying to staging/production).*

---

### 4. Build and Run

Run the application using the Spring Boot Maven plugin:

```bash
mvn spring-boot:run
```

Or build an executable, standalone JAR:

```bash
mvn clean package -DskipTests
java -jar target/resqhub_web.jar
```

The application will start on **`http://localhost:8080/`**.

---

## Sample Data & Login Credentials

All sample accounts are pre-configured in `sample_data.sql` with the standard development password: **`ResQHub@123`**

| Role | Username | Password | Purpose & Capabilities |
|---|---|---|---|
| **Administrator** | `admin` | `ResQHub@123` | Full administrative control, system settings, user management |
| **Rescue Officer** | `rescue_officer` | `ResQHub@123` | Incident triage, field rescue team dispatch, victim tracking |
| **Camp Manager** | `camp_manager` | `ResQHub@123` | Shelter capacity, resident registration, food distribution |
| **Volunteer** | `volunteer_user` | `ResQHub@123` | Access to personal task board, delivery coordination |
| **Citizen** | `citizen_user` | `ResQHub@123` | Submit SOS distress reports, track emergency requests |

---

## Project Layout

```
ResQHub_Web/
├── database/
│   ├── schema.sql             # 27 relational tables with strict FK constraints
│   ├── sample_data.sql        # Realistic Kerala disaster relief mock dataset
│   └── migrations/            # Versioned SQL patch scripts
├── docs/                      # Architectural and technical documentation
├── src/
│   └── main/
│       ├── java/com/resqhub/
│       │   ├── ResQHubApplication.java      # Spring Boot application entry point
│       │   ├── config/                      # Database connection manager & settings
│       │   ├── controller/                  # Legacy UI controllers & calculators
│       │   ├── dao/                         # Pure JDBC Data Access Objects (Zero-ORM)
│       │   ├── exception/                   # Domain-specific exception hierarchy
│       │   ├── model/                       # Data entities, status enums & POJOs
│       │   ├── security/                    # Spring Security configuration & Auth provider
│       │   ├── service/                     # Core business logic & allocation engines
│       │   ├── spring/controller/           # Spring MVC Web Controllers
│       │   ├── util/                        # Validation, string parsers & helpers
│       │   └── web/                         # Navigation modules & web metadata
│       └── resources/
│           ├── application.properties       # Server port, HikariCP pool, database configs
│           ├── static/assets/
│           │   ├── css/resqhub.css          # Design system, themes & custom styling
│           │   ├── images/logo.svg          # High-resolution SVG brand vectors
│           │   ├── js/rq.js                 # Modal controls & client-side interactions
│           │   └── vendor/                  # Bundled Bootstrap 5.3 & Bootstrap Icons
│           └── templates/
│               ├── auth/                    # Login, signup, password reset & setup pages
│               ├── fragments/               # Reusable headers, navigation & sidebar fragments
│               ├── dashboard.html           # Main operational metrics dashboard
│               ├── index.html               # Public landing page & donation modal
│               └── *.html                   # Disaster, victim, shelter, food & task templates
├── pom.xml                                  # Maven project configuration & dependencies
└── README.md                                # System documentation
```

---

## Verification & Testing

To execute automated build checks and tests:

```bash
mvn test
```

### Manual Testing Scenarios:
1. **Public Emergency Dispatch**: Navigate to `http://localhost:8080/`, click **Report Emergency (SOS)**, fill in location and urgency indicators, and verify immediate priority calculation.
2. **Relief Aid Coordination**: Click **Donate Relief Aid** to open the official coordination modal with verified contacts for the Central Administrator, Rescue Manager, and Camp Manager.
3. **Smart Shelter Allocation**: Log in as `camp_manager`, navigate to **Smart Allocation**, select a disaster, and preview ranking recommendations based on real-time camp capacities.
4. **Food Distribution Cycle**: Navigate to **Food Distribution**, check requirement totals, allocate meal rations from warehouse stock, assign a volunteer, and log delivery batches.
5. **Volunteer Execution**: Log in as `volunteer_user` and open **My Tasks** to view and accept active field assignments.

---

## License

This project is licensed under the Apache License, Version 2.0. See the `LICENSE` file for details.
