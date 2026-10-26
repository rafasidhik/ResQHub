-- =====================================================================
-- ResQHub Web - FRESH INSTALLATION schema (MySQL 8)
--
-- * Derived from the original desktop schema (resqhub_schema.sql).
--   Table names, columns, constraints and relationships are UNCHANGED,
--   except users.password_hash is widened CHAR(64) -> VARCHAR(255) so it
--   can hold salted PBKDF2 hashes (see migrations/001).
-- * NON-DESTRUCTIVE: the original file began with DROP DATABASE. This one
--   never drops anything; every CREATE uses IF NOT EXISTS, so running it
--   twice (or against an existing ResQHub database) is safe.
-- * NO user accounts are created and no passwords are shipped. Create the
--   first administrator in the browser at /resqhub/setup (see README).
-- * Includes the five lookup roles, which the application requires.
--
-- Run:  mysql -u root -p < database/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS resqhub_web CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE resqhub_web;

CREATE TABLE IF NOT EXISTS roles (
    id          INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50)      NOT NULL,
    description VARCHAR(200)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_role_name (role_name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id                     BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    username               VARCHAR(50)      NOT NULL,
    password_hash          VARCHAR(255)     NOT NULL COMMENT 'PBKDF2 salted hash (legacy: SHA-256 hex)',
    full_name              VARCHAR(100)     NOT NULL,
    email                  VARCHAR(120)     NOT NULL,
    phone                  VARCHAR(15)      NULL,
    role_id                INT UNSIGNED     NOT NULL,
    account_status         ENUM('ACTIVE','INACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts  INT              NOT NULL DEFAULT 0,
    last_login             DATETIME         NULL,
    created_at             DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_users_phone CHECK (phone IS NULL OR phone REGEXP '^[0-9]{10}$')
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS disasters (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title               VARCHAR(150)    NOT NULL,
    disaster_type       ENUM('FLOOD','EARTHQUAKE','CYCLONE','LANDSLIDE','FIRE',
                             'BUILDING_COLLAPSE','INDUSTRIAL_ACCIDENT','ACCIDENT',
                             'EPIDEMIC','OTHER') NOT NULL,
    severity            ENUM('LOW','MODERATE','SEVERE','CATASTROPHIC') NOT NULL,
    status              ENUM('REPORTED','ACTIVE','CONTAINED','RESOLVED') NOT NULL DEFAULT 'REPORTED',
    location            VARCHAR(200)    NOT NULL,
    affected_population INT UNSIGNED    NOT NULL DEFAULT 0,
    start_date          DATETIME        NOT NULL,
    end_date            DATETIME        NULL COMMENT 'NULL while ongoing',
    description         TEXT            NULL,
    reported_by         BIGINT UNSIGNED NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_disasters_status (status),
    INDEX idx_disasters_severity (severity),
    CONSTRAINT fk_disasters_reported_by FOREIGN KEY (reported_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_disasters_dates CHECK (end_date IS NULL OR end_date >= start_date)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS victims (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name        VARCHAR(100)    NOT NULL,
    age              INT             NOT NULL,
    gender           ENUM('MALE','FEMALE','OTHER') NOT NULL,
    phone            VARCHAR(15)     NULL,
    emergency_status ENUM('SAFE','NEEDS_ASSISTANCE','RESCUE_REQUIRED','INJURED','CRITICAL','MISSING') NOT NULL DEFAULT 'SAFE',
    medical_condition TEXT           NULL COMMENT 'allergies, chronic illness, injuries',
    family_info      TEXT            NULL COMMENT 'family members, dependants',
    current_location VARCHAR(200)    NOT NULL,
    shelter_status   ENUM('NOT_SHELTERED','IN_SHELTER','RELOCATED') NOT NULL DEFAULT 'NOT_SHELTERED',
    disaster_id      BIGINT UNSIGNED NOT NULL,
    registered_by    BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_victims_disaster (disaster_id),
    INDEX idx_victims_emergency_status (emergency_status),
    CONSTRAINT fk_victims_disaster FOREIGN KEY (disaster_id) REFERENCES disasters (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_victims_registered_by FOREIGN KEY (registered_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_victims_age CHECK (age BETWEEN 0 AND 130),
    CONSTRAINT chk_victims_phone CHECK (phone IS NULL OR phone REGEXP '^[0-9]{10}$')
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS rescue_requests (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    disaster_id         BIGINT UNSIGNED NOT NULL,
    victim_id           BIGINT UNSIGNED NULL COMMENT 'NULL if caller not registered as victim',
    requester_name      VARCHAR(100)    NOT NULL,
    contact_number      VARCHAR(15)     NOT NULL,
    location            VARCHAR(200)    NOT NULL,
    people_count        INT UNSIGNED    NOT NULL DEFAULT 1,
    children_count      INT UNSIGNED    NOT NULL DEFAULT 0,
    elderly_count       INT UNSIGNED    NOT NULL DEFAULT 0,
    life_threatening    BOOLEAN         NOT NULL DEFAULT FALSE,
    medical_emergency   BOOLEAN         NOT NULL DEFAULT FALSE,
    trapped_under_debris BOOLEAN        NOT NULL DEFAULT FALSE,
    required_assistance TEXT            NULL,
    priority            ENUM('CRITICAL','HIGH','MEDIUM','LOW') NULL
                        COMMENT 'computed by Rescue Priority Algorithm',
    status              ENUM('PENDING','UNDER_REVIEW','ASSIGNED','IN_PROGRESS','RESCUED','CANCELLED')
                        NOT NULL DEFAULT 'PENDING',
    requested_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_requests_status (status),
    INDEX idx_requests_priority (priority),
    CONSTRAINT fk_requests_disaster FOREIGN KEY (disaster_id) REFERENCES disasters (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_requests_victim FOREIGN KEY (victim_id) REFERENCES victims (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS rescue_teams (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    team_name           VARCHAR(100)    NOT NULL,
    team_type           ENUM('FIRE_RESCUE','MEDICAL','NDRF','POLICE','COMMUNITY','OTHER') NOT NULL,
    leader_name         VARCHAR(100)    NOT NULL,
    contact_number      VARCHAR(15)     NOT NULL,
    member_count        INT UNSIGNED    NOT NULL DEFAULT 1,
    skills              TEXT            NULL COMMENT 'swimming, first-aid, rope rescue...',
    equipment           TEXT            NULL COMMENT 'boats, stretchers, cutters...',
    availability_status ENUM('AVAILABLE','UNAVAILABLE','DEPLOYED','OFF_DUTY') NOT NULL DEFAULT 'AVAILABLE',
    operational_status  ENUM('STANDBY','ASSIGNED','EN_ROUTE','ON_MISSION','RETURNING','OPERATION_COMPLETED','INACTIVE') NOT NULL DEFAULT 'STANDBY',
    base_location       VARCHAR(200)    NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_teams_team_name (team_name),
    INDEX idx_teams_availability (availability_status),
    INDEX idx_teams_operational (operational_status)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS rescue_assignments (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    rescue_request_id BIGINT UNSIGNED NOT NULL,
    rescue_team_id    BIGINT UNSIGNED NOT NULL,
    assigned_by       BIGINT UNSIGNED NULL,
    assignment_status ENUM('ASSIGNED','EN_ROUTE','ON_SITE','COMPLETED','ABORTED')
                      NOT NULL DEFAULT 'ASSIGNED',
    notes             TEXT            NULL,
    completed_at      DATETIME        NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_assignments_request (rescue_request_id),
    INDEX idx_assignments_team (rescue_team_id),
    INDEX idx_assignments_status (assignment_status),
    CONSTRAINT fk_assignments_request FOREIGN KEY (rescue_request_id) REFERENCES rescue_requests (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_assignments_team FOREIGN KEY (rescue_team_id) REFERENCES rescue_teams (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_assignments_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS team_members (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    team_id         BIGINT UNSIGNED NOT NULL,
    member_name     VARCHAR(100)    NOT NULL,
    role            VARCHAR(50)     NOT NULL COMMENT 'Team Leader, Medical Responder, etc.',
    contact_number  VARCHAR(15)     NULL,
    special_skills  TEXT            NULL,
    availability    ENUM('AVAILABLE','UNAVAILABLE','DEPLOYED','OFF_DUTY') NOT NULL DEFAULT 'AVAILABLE',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_tm_team (team_id),
    CONSTRAINT fk_tm_team FOREIGN KEY (team_id) REFERENCES rescue_teams (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS team_skills (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    team_id     BIGINT UNSIGNED NOT NULL,
    skill_name  VARCHAR(100)    NOT NULL,
    description VARCHAR(300)    NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_ts_team (team_id),
    CONSTRAINT fk_ts_team FOREIGN KEY (team_id) REFERENCES rescue_teams (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS team_equipment (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    team_id         BIGINT UNSIGNED NOT NULL,
    equipment_name  VARCHAR(100)    NOT NULL,
    quantity        INT UNSIGNED    NOT NULL DEFAULT 1,
    description     VARCHAR(300)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_te_team (team_id),
    CONSTRAINT fk_te_team FOREIGN KEY (team_id) REFERENCES rescue_teams (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS volunteers (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(100)    NOT NULL,
    contact_number  VARCHAR(15)     NOT NULL,
    email           VARCHAR(120)    NULL,
    user_id         BIGINT UNSIGNED NULL COMMENT 'link to auth user for self-service',
    location        VARCHAR(200)    NOT NULL,
    skills          TEXT            NULL COMMENT 'comma-separated summary',
    availability    ENUM('AVAILABLE','BUSY','UNAVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
    emergency_role  ENUM('MEDICAL','SHELTER','FOOD','RESCUE','COMMUNICATION','TRANSPORT','GENERAL') NULL,
    max_workload    INT UNSIGNED    NOT NULL DEFAULT 2,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_volunteers_contact (contact_number),
    INDEX idx_volunteers_availability (availability),
    INDEX idx_volunteers_location (location),
    CONSTRAINT fk_volunteers_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS volunteer_skills (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    volunteer_id  BIGINT UNSIGNED NOT NULL,
    skill_name    VARCHAR(100)    NOT NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_vs_volunteer (volunteer_id),
    CONSTRAINT fk_vs_volunteer FOREIGN KEY (volunteer_id) REFERENCES volunteers (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS volunteer_assignments (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    volunteer_id  BIGINT UNSIGNED NOT NULL,
    task_name     VARCHAR(150)    NOT NULL,
    description   TEXT            NULL,
    location      VARCHAR(200)    NULL,
    priority      INT             NOT NULL DEFAULT 1,
    status        ENUM('ASSIGNED','ACCEPTED','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'ASSIGNED',
    assigned_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at  DATETIME        NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_va_volunteer (volunteer_id),
    INDEX idx_va_status (status),
    CONSTRAINT fk_va_volunteer FOREIGN KEY (volunteer_id) REFERENCES volunteers (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS volunteer_activity (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    volunteer_id  BIGINT UNSIGNED NOT NULL,
    activity_type VARCHAR(50)     NOT NULL COMMENT 'TASK_ASSIGNED, TASK_ACCEPTED, TASK_STARTED, TASK_COMPLETED, AVAILABILITY_CHANGED',
    description   VARCHAR(300)    NULL,
    activity_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_vact_volunteer (volunteer_id),
    CONSTRAINT fk_vact_volunteer FOREIGN KEY (volunteer_id) REFERENCES volunteers (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS donors (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(100)    NOT NULL,
    contact_number  VARCHAR(15)     NULL,
    email           VARCHAR(120)    NULL,
    location        VARCHAR(200)    NULL,
    donor_type      ENUM('INDIVIDUAL','ORGANIZATION','COMPANY') NOT NULL DEFAULT 'INDIVIDUAL',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_donors_contact (contact_number),
    INDEX idx_donors_donor_type (donor_type),
    INDEX idx_donors_location (location)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS donations (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    donor_id        BIGINT UNSIGNED NOT NULL,
    donation_type   ENUM('CASH','MATERIAL') NOT NULL,
    amount          DECIMAL(12,2)   NULL COMMENT 'cash amount in rupees',
    material_name   VARCHAR(120)    NULL COMMENT 'for material donations',
    quantity        INT UNSIGNED    NULL COMMENT 'units for material donations',
    description     VARCHAR(300)    NULL,
    status          ENUM('RECEIVED','ALLOCATED','PARTIALLY_DISTRIBUTED','DISTRIBUTED') NOT NULL DEFAULT 'RECEIVED',
    donated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_donations_donor (donor_id),
    INDEX idx_donations_type (donation_type),
    INDEX idx_donations_status (status),
    CONSTRAINT fk_donations_donor FOREIGN KEY (donor_id) REFERENCES donors (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS donation_distributions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    donation_id     BIGINT UNSIGNED NOT NULL,
    distributed_to  VARCHAR(150)    NOT NULL COMMENT 'beneficiary / camp / relief operation',
    quantity        INT UNSIGNED    NOT NULL COMMENT 'units or amount distributed',
    distributed_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description     VARCHAR(300)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_dd_donation (donation_id),
    CONSTRAINT fk_dd_donation FOREIGN KEY (donation_id) REFERENCES donations (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

INSERT IGNORE INTO roles (role_name, description) VALUES
('ADMIN',            'Full system control'),
('RESCUE_OFFICER',   'Manages disasters, victims, rescue requests, teams and assignments'),
('CAMP_MANAGER',     'Manages shelters, occupancy, victims and camp resources'),
('VOLUNTEER',        'Views assigned tasks and updates availability'),
('CITIZEN',          'Submits emergency requests and views information');

CREATE TABLE IF NOT EXISTS account_deletion_requests (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED  NOT NULL,
    status      ENUM('PENDING','APPROVED','DENIED') NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by BIGINT UNSIGNED  NULL,
    reviewed_at TIMESTAMP        NULL,
    admin_notes VARCHAR(500)     NULL,
    PRIMARY KEY (id),
    KEY idx_adr_user (user_id),
    CONSTRAINT fk_adr_user   FOREIGN KEY (user_id)     REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_adr_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
    id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    type             ENUM('CRITICAL_RESCUE','LOW_STOCK','ASSIGNMENT','FOOD','SYSTEM')
                     NOT NULL,
    priority         ENUM('CRITICAL','WARNING','INFO') NOT NULL DEFAULT 'INFO',
    status           ENUM('UNREAD','READ','ARCHIVED')  NOT NULL DEFAULT 'UNREAD',
    message          VARCHAR(500)     NOT NULL,
    related_module   VARCHAR(60)      NULL,
    related_event_id BIGINT UNSIGNED  NULL,
    auto_generated   TINYINT(1)       NOT NULL DEFAULT 0,
    dedup_key        VARCHAR(120)     NULL,
    read_at          DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notifications_recipient (recipient_user_id, status),
    KEY idx_notifications_dedup (dedup_key),
    CONSTRAINT fk_notifications_user FOREIGN KEY (recipient_user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS shelters (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name                VARCHAR(150)    NOT NULL,
    code                VARCHAR(30)     NOT NULL,
    district            VARCHAR(80)     NOT NULL,
    city                VARCHAR(80)     NULL,
    address             VARCHAR(200)    NULL,
    location_description VARCHAR(250)   NULL,
    max_capacity        INT UNSIGNED    NOT NULL,
    current_occupancy   INT UNSIGNED    NOT NULL DEFAULT 0,
    available_capacity  INT GENERATED ALWAYS AS (max_capacity - current_occupancy)
                        STORED,
    contact_number      VARCHAR(15)     NULL,
    manager_name        VARCHAR(100)    NULL,
    disaster_id         BIGINT UNSIGNED NULL,
    wheelchair_accessible TINYINT(1)    NOT NULL DEFAULT 0,
    elderly_friendly    TINYINT(1)      NOT NULL DEFAULT 0,
    medical_accessible  TINYINT(1)      NOT NULL DEFAULT 0,
    special_assistance  TINYINT(1)      NOT NULL DEFAULT 0,
    operational_status  ENUM('ACTIVE','AVAILABLE','NEAR_CAPACITY','FULL',
                             'INACTIVE','CLOSED') NOT NULL DEFAULT 'AVAILABLE',
    created_by          BIGINT UNSIGNED NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_shelter_code (code),
    KEY idx_shelter_status (operational_status),
    KEY idx_shelter_district (district),
    KEY idx_shelter_avail (available_capacity),
    INDEX idx_shelter_victims_link (disaster_id),
    INDEX idx_shelter_created_by (created_by),
    CONSTRAINT fk_shelter_disaster FOREIGN KEY (disaster_id)
        REFERENCES disasters (id) ON DELETE SET NULL,
    CONSTRAINT fk_shelter_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS shelter_facilities (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    shelter_id   BIGINT UNSIGNED NOT NULL,
    facility_name VARCHAR(120)   NOT NULL,
    available    TINYINT(1)      NOT NULL DEFAULT 1,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_shelter_facility (shelter_id, facility_name),
    CONSTRAINT fk_facility_shelter FOREIGN KEY (shelter_id)
        REFERENCES shelters (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS shelter_allocations (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    shelter_id    BIGINT UNSIGNED NOT NULL,
    victim_id     BIGINT UNSIGNED NULL,
    family_name   VARCHAR(150)    NULL,
    people_count  INT UNSIGNED    NOT NULL DEFAULT 1,
    notes         VARCHAR(300)    NULL,
    allocated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at   DATETIME        NULL,
    status        ENUM('PENDING','ACTIVE','CHECKED_IN','COMPLETED',
                       'CANCELLED','RELEASED') NOT NULL DEFAULT 'ACTIVE',
    allocated_by  BIGINT UNSIGNED NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_alloc_shelter (shelter_id, status),
    KEY idx_alloc_victim (victim_id),
    CONSTRAINT fk_alloc_shelter FOREIGN KEY (shelter_id)
        REFERENCES shelters (id) ON DELETE CASCADE,
    CONSTRAINT fk_alloc_victim FOREIGN KEY (victim_id)
        REFERENCES victims (id) ON DELETE SET NULL,
    CONSTRAINT fk_alloc_created_by FOREIGN KEY (allocated_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS resources (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name              VARCHAR(150)    NOT NULL,
    code              VARCHAR(30)     NOT NULL,
    category          ENUM('FOOD','WATER','MEDICINE','CLOTHING',
                           'SHELTER_SUPPLIES','MEDICAL_SUPPLIES',
                           'RESCUE_EQUIPMENT','OTHER') NOT NULL,
    available_quantity INT UNSIGNED   NOT NULL DEFAULT 0,
    minimum_level     INT UNSIGNED    NOT NULL DEFAULT 0,
    unit              VARCHAR(40)     NULL COMMENT 'e.g. packets, bottles, kits',
    description       VARCHAR(300)    NULL,
    status            ENUM('AVAILABLE','LOW_STOCK','OUT_OF_STOCK')
                      NOT NULL DEFAULT 'AVAILABLE'
                      COMMENT 'derived from available_quantity vs minimum_level',
    created_by        BIGINT UNSIGNED NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_resource_code (code),
    KEY idx_resource_category (category),
    KEY idx_resource_status (status),
    CONSTRAINT fk_resource_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS stock_movements (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resource_id       BIGINT UNSIGNED NOT NULL,
    movement_type     ENUM('STOCK_IN','STOCK_OUT') NOT NULL,
    quantity          INT UNSIGNED    NOT NULL,
    previous_quantity INT UNSIGNED    NOT NULL,
    new_quantity      INT UNSIGNED    NOT NULL,
    source            VARCHAR(100)    NULL COMMENT 'for stock-in: donation, govt, ...',
    destination       VARCHAR(150)    NULL COMMENT 'for stock-out: where it went',
    reason            VARCHAR(300)    NULL,
    disaster_id       BIGINT UNSIGNED NULL,
    moved_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by       BIGINT UNSIGNED NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sm_resource (resource_id, moved_at),
    KEY idx_sm_type (movement_type),
    KEY idx_sm_disaster (disaster_id),
    CONSTRAINT fk_sm_resource FOREIGN KEY (resource_id)
        REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT fk_sm_disaster FOREIGN KEY (disaster_id)
        REFERENCES disasters (id) ON DELETE SET NULL,
    CONSTRAINT fk_sm_recorded_by FOREIGN KEY (recorded_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS resource_distributions (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resource_id       BIGINT UNSIGNED NOT NULL,
    quantity          INT UNSIGNED    NOT NULL,
    distributed_to    VARCHAR(150)    NOT NULL COMMENT 'recipient / camp / operation',
    destination       ENUM('SHELTER','VICTIM','DISASTER_AREA','RESCUE_TEAM',
                           'FOOD_DISTRIBUTION','OTHER') NOT NULL,
    disaster_id       BIGINT UNSIGNED NULL,
    shelter_id        BIGINT UNSIGNED NULL,
    victim_id         BIGINT UNSIGNED NULL,
    reason            VARCHAR(300)    NULL,
    distributed_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    distributed_by    BIGINT UNSIGNED NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_rd_resource (resource_id, distributed_at),
    KEY idx_rd_destination (destination),
    KEY idx_rd_disaster (disaster_id),
    KEY idx_rd_shelter (shelter_id),
    KEY idx_rd_victim (victim_id),
    CONSTRAINT fk_rd_resource FOREIGN KEY (resource_id)
        REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT fk_rd_disaster FOREIGN KEY (disaster_id)
        REFERENCES disasters (id) ON DELETE SET NULL,
    CONSTRAINT fk_rd_shelter FOREIGN KEY (shelter_id)
        REFERENCES shelters (id) ON DELETE SET NULL,
    CONSTRAINT fk_rd_victim FOREIGN KEY (victim_id)
        REFERENCES victims (id) ON DELETE SET NULL,
    CONSTRAINT fk_rd_distributed_by FOREIGN KEY (distributed_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS food_requests (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_code          VARCHAR(30)     NOT NULL,
    disaster_id           BIGINT UNSIGNED NULL,
    location              VARCHAR(150)    NOT NULL,
    beneficiary_type      ENUM('VICTIM','FAMILY','SHELTER','GROUP')
                          NOT NULL DEFAULT 'GROUP',
    beneficiaries         INT UNSIGNED    NOT NULL DEFAULT 0,
    required_quantity     INT UNSIGNED    NOT NULL DEFAULT 0,
    priority              ENUM('CRITICAL','HIGH','MEDIUM','LOW')
                          NOT NULL DEFAULT 'MEDIUM',
    status                ENUM('PENDING','APPROVED','ALLOCATED',
                               'PARTIALLY_FULFILLED','IN_PROGRESS',
                               'COMPLETED','CANCELLED')
                          NOT NULL DEFAULT 'PENDING',
    description           VARCHAR(300)    NULL,
    requested_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT UNSIGNED NULL,
    allocated_quantity    INT UNSIGNED    NOT NULL DEFAULT 0,
    allocated_resource_id BIGINT UNSIGNED NULL,
    allocated_at          DATETIME        NULL,
    allocated_by          BIGINT UNSIGNED NULL,
    assigned_volunteer_id BIGINT UNSIGNED NULL,
    assigned_at           DATETIME        NULL,
    completed_at          DATETIME        NULL,
    created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_food_request_code (request_code),
    KEY idx_food_request_status (status),
    KEY idx_food_request_disaster (disaster_id),
    KEY idx_food_request_priority (priority),
    CONSTRAINT fk_food_request_disaster FOREIGN KEY (disaster_id)
        REFERENCES disasters (id) ON DELETE SET NULL,
    CONSTRAINT fk_food_request_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_food_request_resource FOREIGN KEY (allocated_resource_id)
        REFERENCES resources (id) ON DELETE SET NULL,
    CONSTRAINT fk_food_request_volunteer FOREIGN KEY (assigned_volunteer_id)
        REFERENCES volunteers (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS food_distributions (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id           BIGINT UNSIGNED NOT NULL,
    resource_id          BIGINT UNSIGNED NULL,
    quantity             INT UNSIGNED    NOT NULL,
    beneficiaries_served INT UNSIGNED    NOT NULL DEFAULT 0,
    distributed_to       VARCHAR(150)    NULL,
    location             VARCHAR(150)    NULL,
    distributed_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    distributed_by       BIGINT UNSIGNED NULL,
    note                 VARCHAR(300)    NULL,
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_food_dist_request (request_id, distributed_at),
    KEY idx_food_dist_location (location),
    CONSTRAINT fk_food_dist_request FOREIGN KEY (request_id)
        REFERENCES food_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_food_dist_resource FOREIGN KEY (resource_id)
        REFERENCES resources (id) ON DELETE SET NULL,
    CONSTRAINT fk_food_dist_by FOREIGN KEY (distributed_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;
