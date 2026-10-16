# ResQHub — Integrated Disaster Response Coordination System
> **Slogan:** *Help. Hope. Humanity.*

ResQHub is an enterprise-grade, integrated disaster management and humanitarian response coordination web application built with **Java 17**, **Spring Boot 3**, **Spring JDBC (`JdbcTemplate`)**, **MySQL 8**, **Spring Security (BCrypt)**, **Thymeleaf**, **Bootstrap 5**, and **Chart.js**.

The platform streamlines multi-agency disaster operations, citizen emergency rescue requests, victim triage and family reunification, shelter capacity allocation, relief inventory logistics, volunteer task dispatching, and humanitarian donation verification.

---

## 1. Architectural Highlights & Strict Design Philosophy

- **Data Persistence:** Built entirely on **Spring JDBC (`JdbcTemplate`)** with direct, parameterized SQL queries, explicit `RowMapper` implementations, and transactional consistency (`@Transactional`). **Zero JPA/Hibernate ORM** is used.
- **Frontend Architecture:** Clean server-rendered HTML5 views using **Thymeleaf 3** with modular layout fragments (`navbar.html`, `sidebar.html`, `footer.html`), styled with custom **Bootstrap 5** design tokens (Deep Green `#1E5631`, Earthy Brown `#7F5539`, and Clean Sand `#F4F6F4`), and interactive client-side **Chart.js** charts.
- **Deterministic Rule-Based Intelligence:** ResQHub avoids opaque, non-deterministic "AI" claims. All recommendations (squad matching, shelter allocation, priority triage) are powered by **transparent, auditable mathematical and heuristic algorithms**.
- **Self-Contained & Zero External Paid APIs:** Operates fully standalone on local infrastructure without paid external cloud services, third-party analytics trackers, or paid map APIs.

---

## 2. Role-Based Access Control (RBAC) & Default Credentials

ResQHub enforces strict role-based authorization across 5 distinct user roles using Spring Security method security (`@PreAuthorize`) and URL pattern guards:

| Role | Username | Password | Dedicated Dashboard & Permissions |
| :--- | :--- | :--- | :--- |
| **System Administrator** (`ROLE_ADMIN`) | `admin` | `Password@123` | **Unified Command Center** (`/dashboard/admin`): Complete system administration, user provisioning, disaster declaration, audit logs, and global analytics. |
| **Field Rescue Officer** (`ROLE_RESCUE_OFFICER`) | `rescue_officer` | `Password@123` | **Field Rescue Command** (`/dashboard/rescue-officer`): Emergency rescue queue, victim triage, squad dispatching, and smart squad allocation. |
| **Camp Manager** (`ROLE_CAMP_MANAGER`) | `camp_manager` | `Password@123` | **Shelter & Logistics Operations** (`/dashboard/camp-manager`): Camp bed occupancy, resident check-in/out, food rations distribution, and inventory restock. |
| **Field Volunteer** (`ROLE_VOLUNTEER`) | `volunteer_user` | `Password@123` | **Volunteer Mission Board** (`/dashboard/volunteer`): Availability status toggle (`On-Duty` / `Off-Duty`), assigned task lifecycle (`Accept` $\to$ `Start` $\to$ `Complete`). |
| **Citizen** (`ROLE_CITIZEN`) | `citizen_user` | `Password@123` | **Citizen Emergency Portal** (`/dashboard/citizen`): Emergency SOS rescue alerts, missing family member tracking, shelter lookup, and donation portal. |

> All demonstration accounts share the password `Password@123`. Additional seeded accounts: `rescue_officer2`, `camp_manager2`, `volunteer_user2`, `demo`.

---

## 3. System Modules Directory

ResQHub features 17 fully developed functional modules:

1. **Authentication & User Management** (`/login`, `/register`, `/admin/users`): BCrypt hashed credentials, status toggles, and role assignments.
2. **Disaster Management** (`/disasters`): Real-time disaster registry, severity indicators (`CRITICAL`, `HIGH`, `MODERATE`, `LOW`), operational integrity guards preventing deletion of active disaster records.
3. **Victim Registration & Triage** (`/victims`): Auto-generated unique tracking numbers (`VIC-YYYY-XXXX`), family size tracking, priority triage (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), rescue lifecycle updates.
4. **Rescue Request Coordination** (`/rescue-requests`): Citizen emergency SOS intake, urgency weighting, location mapping, and status tracking (`PENDING` $\to$ `ASSIGNED` $\to$ `IN_PROGRESS` $\to$ `COMPLETED`).
5. **Rescue Teams & Squad Readiness** (`/rescue-teams`): Squad registry, specialized equipment tags (Water, Medical, Heavy Urban, Aerial), team readiness status (`AVAILABLE`, `DEPLOYED`, `RESTING`).
6. **Smart Team Allocation Engine** (`/rescue-requests/smart-allocation`): Deterministic scoring algorithm matching teams based on specialty, proximity, group size capacity, and past missions.
7. **Shelter & Camp Directory** (`/shelters`): Camp management, address, emergency contact, total bed capacity, and live occupancy meters.
8. **Resident Evacuation Tracking** (`/shelters/{id}`): Refugee check-in/check-out workflow with idempotent bed count synchronization.
9. **Smart Shelter Allocation Engine** (`/shelters/smart-allocate`): Deterministic shelter recommendation scoring based on spare capacity percentage and family accommodation.
10. **Relief Inventory Management** (`/inventory`): Stock tracking across food, medical, clothing, shelter, and water items with minimum threshold low-stock warnings.
11. **Stock Audit & Transactions** (`/inventory/adjust`): Append-only transaction log (`INCOMING_DONATION`, `DISPATCH_SHELTER`, `CORRECTION`, `DAMAGED_EXPIRED`).
12. **Shelter Resource Allocations** (`/resources/allocations`): Formal supply requests from camps with approval, dispatch, and delivery confirmation steps.
13. **Food & Ration Distribution** (`/resources/food-distributions`): Meal batch distribution logging, tracking rations served, meal types, and target refugee camps.
14. **Volunteer Onboarding & Registry** (`/volunteers`, `/volunteers/register`): Public volunteer sign-up, administrative vetting and background verification workflow (`PENDING` $\to$ `APPROVED` / `REJECTED`).
15. **Volunteer Task Dispatching** (`/volunteers/assignments`): Assignment of operational tasks to vetted volunteers with real-time status progression.
16. **Donation Intake & Verification** (`/donations`, `/donations/new`): Public donor intake for monetary relief or physical material goods, administrative verification, and automatic inventory deposit.
17. **Reporting & Operational Analytics** (`/reports`): Official Situation Report (SITREP) generator, printable triage rosters, and RFC-4180 compliant CSV exports for victims, inventory, and financial audits.

---

## 4. Rule-Based Allocation Algorithms

### A. Smart Rescue Team Matching Algorithm
The team matching algorithm deterministically ranks available rescue squads for an incident using a multi-factor weighted scoring formula:

$$\text{Score} = (\text{Specialty Match} \times 40) + (\text{Availability Bonus} \times 25) + (\text{Capacity Fit} \times 20) + (\text{Experience Score} \times 15)$$

- **Specialty Match (40 pts):** Matches disaster environment (e.g. `WATER_RESCUE` squad for Floods/Cyclones, `STRUCTURAL_COLLAPSE` squad for Earthquakes/Landslides).
- **Availability Bonus (25 pts):** Teams currently `AVAILABLE` receive full marks; teams resting or in standby receive lower scores.
- **Capacity Fit (20 pts):** Evaluates whether squad member size is adequately suited for the group of trapped victims.
- **Experience Score (15 pts):** Historical successful extractions completed by the squad.

### B. Smart Shelter Routing Algorithm
Recommends the optimal evacuation camps for displaced populations:

$$\text{Score} = (\text{Spare Capacity \%} \times 50) + (\text{Active Operations Status} \times 30) + (\text{Proximity Score} \times 20)$$

- **Spare Capacity (50 pts):** Prioritizes shelters that have sufficient beds to house entire family units together without splitting them.
- **Active Operations (30 pts):** Ensures only verified, fully staffed shelters receive evacuee batches.
- **Proximity Score (20 pts):** Favor shelters within the disaster's administrative zone.

---

## 5. Database Setup & Configuration

ResQHub runs on local **MySQL 8.0+** on port `3306`.

### Database Credentials (`src/main/resources/application.properties`):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/resqhub?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=ResQHub#2026
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### Database Initialization:
The system database contains 19 relational tables. The schema and sample records can be initialized directly using MySQL CLI or MySQL Workbench:
```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS resqhub;"
mysql -u root -p resqhub < c:\Users\rafasidhik\Projects\Website\ResQHub\database\schema.sql
mysql -u root -p resqhub < c:\Users\rafasidhik\Projects\Website\ResQHub\database\sample-data.sql
```

---

## 6. Building and Running

### Prerequisites:
- Java JDK 17+
- Apache Maven 3.8+
- MySQL Server 8.0+ running on port 3306

### Run Full Test Suite:
```powershell
cd c:\Users\rafasidhik\Projects\Website\ResQHub
mvn test
```
All **71 unit and integration tests** execute against the database with 100% pass rate.

### Run Application:
```powershell
mvn spring-boot:run
```
The application will start on **http://localhost:8085**.

---

## 7. License & Attribution
Developed for humanitarian coordination and emergency disaster relief operations.  
*Help. Hope. Humanity.*
