# ResQHub – Complete Technical Work Distribution

## 0. Shared Foundation — All 4 Members

Before splitting modules, all members should agree on these:

- Common package structure
- GitHub repository and branching workflow
- MySQL database naming conventions
- Primary key / foreign key conventions
- Common enums and statuses
- MVC + Service + DAO architecture
- Common exception-handling approach
- Common UI navigation style
- Integration testing

### Suggested Structure

```text
ResQHub/
│
├── src/
│   └── com/resqhub/
│       ├── model/
│       ├── dao/
│       ├── service/
│       ├── controller/
│       ├── view/
│       ├── exception/
│       ├── util/
│       ├── config/
│       └── main/
│
├── resources/
└── database/
```

The project architecture specifically defines MVC, a service layer, DAO layer, JDBC/database layer, and utility layer as the main separation of responsibilities.

---

## 1. Rafa — Core System & Emergency Operations 🚨

Rafa handles the central foundation of disaster response.

### Modules

**Authentication & User Management**
- Login
- Logout
- Password validation
- Role-based access
- Account status
- User profile basics

**Disaster Management**
- Create disaster
- Update disaster
- Search disasters
- Disaster type
- Severity
- Location
- Status
- Affected population

**Victim Management**
- Victim registration
- Personal details
- Emergency status
- Medical information
- Family information
- Current location
- Disaster association

**Rescue Request Management**
- Create rescue request
- Number of people
- Emergency severity
- Required assistance
- Request status
- Assignment tracking

**Rescue Team Management**
- Team registration
- Team members
- Skills
- Equipment
- Availability
- Assignment
- Status tracking

### Main Business Logic

**Rescue Priority Algorithm**

- **Input:**
  - Life-threatening condition
  - Number of people
  - Children/elderly
  - Medical emergency
  - Disaster severity
  - Location urgency
- **Output:**
  - `CRITICAL`
  - `HIGH`
  - `MEDIUM`
  - `LOW`

### Database Ownership

**Main Tables:**
- `users`
- `roles`
- `disasters`
- `victims`
- `rescue_requests`
- `rescue_teams`
- `rescue_assignments`

### Technical Ownership
- Model
- DAO
- Service
- Controller
- Swing Screens
- JDBC Queries
- Validation
- Rescue Priority Logic

**GitHub Branch:** `rafa/core-rescue`

---

## 2. Ameya — Shelter, Resources & Food 🏠📦

Ameya handles where victims go and how emergency resources are managed.

### Modules

**Shelter Management**
- Shelter registration
- Location
- Capacity
- Current occupancy
- Available capacity
- Facilities
- Accessibility
- Status

**Smart Shelter Allocation**
The system finds the most suitable shelter based on:
- Available capacity
- Distance
- Required facilities
- Accessibility
- Family size
- Priority

**Resource Inventory**
- **Manage:**
  - Food
  - Water
  - Medicine
  - Blankets
  - Clothing
  - First-aid supplies
  - Rescue equipment
- **Operations:**
  - Stock-in
  - Stock-out
  - Transfer
  - Distribution
  - Low-stock detection
  - Inventory history

**Food Distribution**
- Distribution requests
- Beneficiary count
- Food allocation
- Distribution history

### Main Business Logic

**Shelter Allocation Algorithm**

```text
Victim / Family
       ↓
Get Requirements
       ↓
Find Available Shelters
       ↓
Check Capacity
       ↓
Check Facilities
       ↓
Calculate Suitability Score
       ↓
Select Best Shelter
```

**Resource Allocation Logic**
- Available stock
- Requested quantity
- Priority
- Location
- Expiry, where applicable

### Database Ownership

**Main Tables:**
- `shelters`
- `shelter_facilities`
- `shelter_allocations`
- `resources`
- `inventory_transactions`
- `resource_distributions`
- `food_distribution_requests`
- `food_distributions`

### Technical Ownership
- Model
- DAO
- Service
- Controller
- Swing Screens
- JDBC
- Shelter Allocation Algorithm
- Resource/Stock Logic
- Validation
- Custom Exceptions

**GitHub Branch:** `ameya/shelter-resources`

---

## 3. Malavika — Hospital & Blood Management 🏥🩸

Malavika handles the medical emergency coordination system.

### Modules

**Hospital Management**
- Hospital registration
- Location
- Available beds
- Emergency facilities
- Medical capacity
- Blood availability
- Hospital status

**Blood Donor Management**
- Donor registration
- Blood group
- Availability
- Eligibility
- Donation history
- Location

**Blood Requests**
- Blood group required
- Required units
- Urgency
- Location
- Request status

**Emergency Blood Matching**
Find suitable donors using:
- ABO compatibility
- Rh compatibility
- Availability
- Required units
- Urgency
- Location

### Main Business Logic

**Blood Matching Algorithm**

```text
Blood Request
      ↓
Identify Required Blood Group
      ↓
Find Compatible Donors
      ↓
Check Rh Compatibility
      ↓
Check Availability
      ↓
Filter by Location
      ↓
Rank by Urgency / Suitability
      ↓
Return Best Matches
```

### Database Ownership

**Main Tables:**
- `hospitals`
- `hospital_facilities`
- `blood_donors`
- `blood_donation_history`
- `blood_requests`
- `blood_matches`

### Technical Ownership
- Model
- DAO
- Service
- Controller
- Swing Screens
- JDBC
- Blood Compatibility Logic
- Blood Matching Algorithm
- Hospital Capacity Processing
- Validation
- Custom Exceptions

**GitHub Branch:** `malavika/medical-blood`

---

## 4. Stina — Volunteers, Donations, Alerts & Reports 🙋📊

Stina handles human resource coordination and system-level output.

### Modules

**Volunteer Management**
- Volunteer registration
- Skills
- Availability
- Location
- Current workload
- Assigned tasks
- Activity tracking

**Volunteer Assignment**
Assign volunteers based on:
- Required skills
- Availability
- Location
- Current workload
- Task priority

**Donation Management**
- Donor registration
- Cash donations
- Material donations
- Donation tracking
- Distribution tracking
- Donation history

**Notifications & Alerts**
Examples:
- Low stock
- Critical rescue request
- Shelter nearing capacity
- Blood shortage
- Assignment notifications

**Reports & Analytics**
Generate reports for:
- Active disasters
- Victim statistics
- Rescue request statistics
- Shelter occupancy
- Blood availability
- Volunteer availability
- Resource inventory
- Donation statistics
- Food distribution
- Hospital capacity

### Main Business Logic

**Volunteer Assignment Algorithm**

```text
Task
  ↓
Required Skills
  ↓
Find Available Volunteers
  ↓
Match Skills
  ↓
Check Location
  ↓
Check Current Workload
  ↓
Rank Candidates
  ↓
Assign Best Volunteer
```

**Reporting Operations**
- `COUNT`
- `SUM`
- `AVG`
- `MIN`
- `MAX`
- `GROUP BY`
- `HAVING`
- `JOIN`
- Filtering
- Sorting

These SQL and reporting requirements are explicitly part of the project architecture.

### Database Ownership

**Main Tables:**
- `volunteers`
- `volunteer_skills`
- `volunteer_assignments`
- `volunteer_activity`
- `donors`
- `donations`
- `donation_distributions`
- `notifications`

*Note: Reports mainly read from all members' tables, so Stina will coordinate with the others for report queries.*

**GitHub Branch:** `stina/volunteer-reports`

---

## Complete Responsibility Matrix

| Technical Part | Rafa | Ameya | Malavika | Stina |
| :--- | :---: | :---: | :---: | :---: |
| Authentication | ✅ | | | |
| User/Role Management | ✅ | | | |
| Disaster Management | ✅ | | | |
| Victim Management | ✅ | | | |
| Rescue Requests | ✅ | | | |
| Rescue Teams | ✅ | | | |
| Rescue Priority Logic | ✅ | | | |
| Shelter Management | | ✅ | | |
| Smart Shelter Allocation | | ✅ | | |
| Resource Inventory | | ✅ | | |
| Resource Allocation | | ✅ | | |
| Food Distribution | | ✅ | | |
| Hospital Management | | | ✅ | |
| Blood Donors | | | ✅ | |
| Blood Requests | | | ✅ | |
| Blood Matching | | | ✅ | |
| Volunteer Management | | | | ✅ |
| Volunteer Assignment | | | | ✅ |
| Donation Management | | | | ✅ |
| Notifications | | | | ✅ |
| Reports & Analytics | | | | ✅ |

---

## Important Integration Responsibilities

Some things must be coordinated between members.

```text
Rafa → Ameya
Victim ──> Needs Shelter ──> Shelter Allocation

Rafa → Malavika
Victim ──> Medical Emergency ──> Hospital / Blood Request

Rafa → Stina
Rescue Operation ──> Task Created ──> Volunteer Assignment

Ameya ↔ Stina
Resource Shortage ──> Notification / Report

Malavika ↔ Stina
Blood Shortage ──> Alert / Report

All Members → Reports
All Module Data ──> SQL Queries ──> Statistics ──> Reports / Dashboard
```

---

## Equal Contribution Rule

To keep the work genuinely equal, each member should complete these layers for their assigned modules:

```text
Database Tables
      ↓
Model Classes
      ↓
DAO Classes
      ↓
Service / Business Logic
      ↓
Validation + Exceptions
      ↓
Controller
      ↓
Swing GUI
      ↓
JDBC Testing
```

This is important because ResQHub is intended to demonstrate meaningful business processing—not simply forms connected to database tables.

---

## Recommended Final Ownership

```text
RAFA
├── Authentication & Users
├── Disaster
├── Victims
├── Rescue Requests
├── Rescue Teams
└── Rescue Priority

AMEYA
├── Shelters
├── Shelter Allocation
├── Resources
├── Inventory
└── Food Distribution

MALAVIKA
├── Hospitals
├── Blood Donors
├── Blood Requests
├── Blood Matching
└── Medical Capacity

STINA
├── Volunteers
├── Volunteer Assignment
├── Donations
├── Notifications
└── Reports & Analytics
```