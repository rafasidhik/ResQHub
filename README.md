# ResQHub — Integrated Disaster Response Coordination System

A desktop Java application for coordinating disaster response across rescue,
shelter, medical, and relief modules. It connects to a MySQL database through
JDBC and presents a role-based Swing GUI for administrators, rescue officers,
camp managers, medical officers, blood coordinators, volunteers, and citizens.

## Features / Modules

| Module | Purpose |
| --- | --- |
| Overview Dashboard | Live landing page (staff): stat cards, attention alerts, role-based quick actions |
| Disasters | Register and track disasters, their severity and status |
| Victims | Victim registration, triage, emergency status |
| Rescue Requests | Report/submit and process rescue requests |
| Rescue Teams | Team management and deployment |
| Smart Allocation | Auto-assignment of victims/requests to teams and shelters |
| Shelters | Shelter capacity and facilities, near-capacity warnings |
| Volunteers | Volunteer directory, availability, assignments |
| Donations | Record donations and their distribution |
| Resources & Inventory | Stock levels, low-stock/out-of-stock alerts |
| Food Distribution | Food requests and shortage tracking |
| Hospitals | Hospital capacity, accepting status, patient referrals |
| Blood Donors | Donor registry, blood requests, matching, donations |
| Notifications | Per-user alert center |
| Reports & Analytics | Summary reports across modules |
| Users | Account administration, deletion requests |

## Tech Stack

- **Language / UI:** Java (JDK 26), Java Swing
- **Database:** MySQL 8 (JDBC, InnoDB, utf8mb4)
- **Driver:** `lib/mysql-connector-j-9.3.0.jar`
- **Build / run:** `compile.bat` and `run.bat` (Windows batch scripts)

## Project Structure

```
ResQHub\
├── src\com\resqhub\
│   ├── main\          ResQHubApplication.java  (entry point)
│   ├── controller\    business logic + ActionResult
│   ├── service\       use-case layer, session/role checks
│   ├── dao\           JDBC data access
│   ├── model\         entities + enums
│   ├── view\          Swing panels (dashboard, per-module UIs)
│   ├── exception\     typed exceptions
│   ├── util\          utilities (PasswordUtil, DBConnection, etc.)
│   └── test\          AllTests.java  (single consolidated test suite)
├── database\
│   ├── resqhub_schema.sql               base schema + seed data (all tables)
│   └── migrate_*.sql                    incremental migrations (list below)
├── resources\config\
│   ├── db.properties.template           database config template
│   └── db.properties                    YOUR LOCAL copy (gitignored)
├── lib\                                 MySQL Connector/J jar
├── compile.bat                          build script
├── run.bat                              launch script
└── README.md
```

## Prerequisites

1. **Java JDK** — the batch files point at `C:\Program Files\Java\jdk-26.0.2.1`.
   If your JDK is installed elsewhere, edit the `JAVA_HOME` line in
   `compile.bat` and `run.bat`.
2. **MySQL Server 8** — running locally on port `3306`. The application uses
   the same password as your MySQL `root` user.
3. **git, git-lfs** (optional, only if you want to clone).

## How to Run Locally

Workflow: **Fork → Clone → Load database → Configure → Compile → Run**

Recommended location (used in the examples below):

```
C:\Users\<you>\Projects\ResQHub
```

### 1. Fork the repository

Open the project on GitHub and click **Fork** to create a copy under your own
GitHub account:

```
https://github.com/rafasidhik/ResQHub
```

### 2. Clone your fork

Replace `<your-username>` with your GitHub username and clone:

```
git clone https://github.com/<your-username>/ResQHub.git
cd ResQHub
```

For read-only local use, you can clone the original directly instead of your
fork:

```
git clone https://github.com/rafasidhik/ResQHub.git
cd ResQHub
```

### 3. Configure the database

The schema file creates the `resqhub` database, all tables **and** seeds
demo data. For a fresh install run only this one file:

```
mysql --host=127.0.0.1 --user=root -p < database\resqhub_schema.sql
```

> **Incremental upgrades:** skip unless you already have an older schema.
> Run the migrations **in this order** from `database\`:
> `migrate_shelter.sql`, `migrate_smart_allocation.sql`,
> `migrate_resources.sql`, `migrate_food_distribution.sql`,
> `migrate_hospital.sql`, `migrate_blood.sql`

### 4. Set the database password

Copy the template to your local config file and edit the password:

```
copy resources\config\db.properties.template resources\config\db.properties
notepad resources\config\db.properties
```

In `db.properties`, set values for your machine:

```
db.url=jdbc:mysql://localhost:3306/resqhub
db.username=root
db.password=YOUR_MYSQL_ROOT_PASSWORD
```

> `db.properties` is gitignored — never commit real passwords.

### 5. Compile

From the project root:

```
compile.bat
```

This creates `out\` with the compiled classes. A successful build prints
`Compilation successful. Run with: run.bat`.

### 6. Run the application

```
run.bat
```

This launches the Swing app (class `com.resqhub.main.ResQHubApplication`).

### Seeded demo logins

`database\resqhub_schema.sql` seeds users (`admin`, `officer1`, `volunteer1`,
plus more). Their passwords are stored as **SHA-256 hashes** of the plaintext
in the seed file. To log in reliably, either:

- change the password after logging in via *Account → Change Password*, or
- insert your own user with a known hash into the `users` table, or
- have an administrator reset the password for you.

## Running the Test Suite

The single consolidated suite `src\com\resqhub\test\AllTests.java` runs all
module regression tests. From the project root, **after compiling**:

```
java -cp "out;lib\*;resources" com.resqhub.test.AllTests
```

A green run ends with:

```
ALL TESTS DONE - total failures: 0
```

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `java`/`javac` not recognized | Edit `JAVA_HOME` in `compile.bat` / `run.bat` to your JDK path |
| `Access denied for user 'root'` | Fix `db.password` in `resources\config\db.properties` |
| `Unknown database 'resqhub'` | Run `database\resqhub_schema.sql` (step 1) |
| Dashboard shows `0`s for a module | Expected if that module has no data yet; counts update on Refresh |
| Tests fail with `... is not available` | Re-run `resqhub_schema.sql` against a fresh `resqhub` database |