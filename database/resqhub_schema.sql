-- =====================================================================
-- ResQHub - Integrated Disaster Response Coordination System
-- Owner : Rafa (rafa/core-rescue branch)
-- Scope : Core tables -> roles, users, disasters, victims,
--         rescue_requests, rescue_teams, rescue_assignments
--
-- Conventions (team-wide):
--   * Table names      : lowercase plural
--   * Primary keys     : id BIGINT UNSIGNED AUTO_INCREMENT
--   * Foreign keys     : <entity>_id
--   * Columns          : snake_case
--   * Every table      : created_at / updated_at audit columns
--   * Engine           : InnoDB (transactions + FK enforcement)
--   * Charset          : utf8mb4
-- =====================================================================

DROP DATABASE IF EXISTS resqhub;
CREATE DATABASE resqhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE resqhub;

-- ---------------------------------------------------------------------
-- 1. roles  (lookup table for role-based access control)
--    users(1) --< roles(1) : many users belong to one role (N:1)
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50)      NOT NULL,
    description VARCHAR(200)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_role_name (role_name)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. users  (login accounts; password stored as SHA-256 hex digest)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                     BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    username               VARCHAR(50)      NOT NULL,
    password_hash          CHAR(64)         NOT NULL COMMENT 'SHA-256 hex of password',
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

-- ---------------------------------------------------------------------
-- 3. disasters  (one disaster --< many victims/rescue requests)
-- ---------------------------------------------------------------------
CREATE TABLE disasters (
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

-- ---------------------------------------------------------------------
-- 4. victims  (each victim is associated with exactly one disaster)
--    NOTE: shelter linkage is owned by Ameya's shelter_allocations
--    table; victims only track a denormalised shelter_status flag.
-- ---------------------------------------------------------------------
CREATE TABLE victims (
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

-- ---------------------------------------------------------------------
-- 5. rescue_requests  (help requests from victims/citizens)
--    priority column is COMPUTED by the Rescue Priority Algorithm in
--    the service layer and persisted for reporting/sorting.
-- ---------------------------------------------------------------------
CREATE TABLE rescue_requests (
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

-- ---------------------------------------------------------------------
-- 6. rescue_teams
-- ---------------------------------------------------------------------
CREATE TABLE rescue_teams (
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

-- ---------------------------------------------------------------------
-- 7. rescue_assignments  (junction table resolving M:N between
--    rescue_requests and rescue_teams, with relationship attributes)
-- ---------------------------------------------------------------------
CREATE TABLE rescue_assignments (
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

-- ---------------------------------------------------------------------
-- 8. team_members  (individual members of a rescue team)
--    rescue_teams(1) --< team_members(N)
-- ---------------------------------------------------------------------
CREATE TABLE team_members (
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

-- ---------------------------------------------------------------------
-- 9. team_skills  (skills associated with a rescue team)
--    rescue_teams(1) --< team_skills(N)
-- ---------------------------------------------------------------------
CREATE TABLE team_skills (
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

-- ---------------------------------------------------------------------
-- 10. team_equipment  (equipment tracked per rescue team)
--     rescue_teams(1) --< team_equipment(N)
-- ---------------------------------------------------------------------
CREATE TABLE team_equipment (
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

-- ---------------------------------------------------------------------
-- 11. volunteers  (people volunteering for disaster-response tasks)
-- ---------------------------------------------------------------------
CREATE TABLE volunteers (
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

-- ---------------------------------------------------------------------
-- 12. volunteer_skills  (normalised skill records per volunteer)
--     volunteers(1) --< volunteer_skills(N)
-- ---------------------------------------------------------------------
CREATE TABLE volunteer_skills (
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

-- ---------------------------------------------------------------------
-- 13. volunteer_assignments  (emergency tasks assigned to volunteers)
--     volunteers(1) --< volunteer_assignments(N)
-- ---------------------------------------------------------------------
CREATE TABLE volunteer_assignments (
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

-- ---------------------------------------------------------------------
-- 14. volunteer_activity  (history of volunteer participation)
--     volunteers(1) --< volunteer_activity(N)
-- ---------------------------------------------------------------------
CREATE TABLE volunteer_activity (
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

-- =====================================================================
-- SEED DATA
-- Passwords below are SHA-256 hex digests.
--   admin    / Admin@123
--   officer1 / Rescue@123
-- =====================================================================
INSERT INTO roles (role_name, description) VALUES
('ADMIN',            'Full system control'),
('RESCUE_OFFICER',   'Manages disasters, victims, rescue requests, teams and assignments'),
('CAMP_MANAGER',     'Manages shelters, occupancy, victims and camp resources'),
('MEDICAL_OFFICER',  'Manages medical information and hospital coordination'),
('BLOOD_COORDINATOR','Manages blood donors, inventory and emergency matching'),
('VOLUNTEER',        'Views assigned tasks and updates availability'),
('CITIZEN',          'Submits emergency requests and views information');

INSERT INTO users (username, password_hash, full_name, email, phone, role_id) VALUES
('admin',    'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7',
 'System Administrator', 'admin@resqhub.org',    '9876500001', 1),
('officer1', 'f52af13d84a7dfe7b00bb272dae45c9e25dcb54f3cf200bef8f129f753b1776c',
 'Rafa Nair',            'rafa@resqhub.org',     '9876500002', 2);

INSERT INTO disasters (title, disaster_type, severity, status, location,
                       affected_population, start_date, description, reported_by) VALUES
('Wayanad Monsoon Floods', 'FLOOD', 'SEVERE', 'ACTIVE',
 'Wayanad district, Kerala', 12500, NOW() - INTERVAL 2 DAY,
 'Heavy rainfall caused river overflow across low-lying areas.', 1),
('Kovalam Cliff Landslide', 'LANDSLIDE', 'MODERATE', 'REPORTED',
 'Kovalam, Thiruvananthapuram', 300, NOW() - INTERVAL 6 HOUR,
 'Landslide triggered by continuous rain; houses buried.', 2);

INSERT INTO victims (full_name, age, gender, phone, emergency_status, medical_condition,
                     family_info, current_location, disaster_id, registered_by) VALUES
('Anand Menon',   34, 'MALE',   '9847000001', 'CRITICAL', 'Leg fracture, bleeding',
 'Wife and 2 children with him', 'Chundale relief point', 1, 2),
('Lakshmi Pillai',67, 'FEMALE', NULL,         'INJURED',  'Diabetic, dehydration',
 'Lives alone',                  'Meppadi camp ground',   1, 2),
('Abdul Rasheed', 28, 'MALE',   '9847000003', 'MISSING',  NULL,
 'Brother searching',            'Kovalam cliff area',    2, 1);

INSERT INTO rescue_teams (team_name, team_type, leader_name, contact_number,
                          member_count, skills, equipment, base_location) VALUES
('Coast Guard Alpha', 'NDRF',      'Cmdr. Suresh',  '9848000001', 12,
 'water rescue, swimming, rope rescue', 'inflatable boats, life jackets, ropes',
 'Kozhikode NDRF Base'),
('City Rapid Response', 'FIRE_RESCUE', 'Officer Vinod', '9848000002', 8,
 'debris cutting, first aid, confined space rescue', 'cutters, stretchers, oxygen kits',
 'Thiruvananthapuram Fire Station');

INSERT INTO team_members (team_id, member_name, role, contact_number, special_skills) VALUES
(1, 'Cmdr. Suresh', 'Team Leader', '9848000001', 'water rescue, navigation'),
(1, 'Ajith Kumar',  'Medical Responder', '9848000011', 'first aid, emergency medicine'),
(1, 'Ravi Menon',   'Rescue Specialist', '9848000012', 'swimming, rope rescue'),
(1, 'Sreelakshmi',  'Driver',        '9848000013', 'boat handling'),
(2, 'Officer Vinod','Team Leader',   '9848000002', 'fire and rescue'),
(2, 'Deepak',       'Rescue Specialist', '9848000021', 'confined space rescue'),
(2, 'Maya',         'Medical Responder', '9848000022', 'first aid');

INSERT INTO team_skills (team_id, skill_name, description) VALUES
(1, 'Water rescue', 'Rescue from flooded areas and water bodies'),
(1, 'Swimming', 'Certified swimmers for deep water operations'),
(1, 'Rope rescue', 'Rope systems for extraction'),
(2, 'Debris cutting', 'Cutting through rubble and collapsed structures'),
(2, 'First aid', 'On-site medical attention'),
(2, 'Confined space rescue', 'Rescue in tight spaces');

INSERT INTO team_equipment (team_id, equipment_name, quantity, description) VALUES
(1, 'Inflatable boats', 4, 'For flood and water operations'),
(1, 'Life jackets', 20, 'Personal flotation devices'),
(1, 'Ropes', 12, 'Heavy-duty rescue ropes'),
(2, 'Cutters', 3, 'Hydraulic cutting tools'),
(2, 'Stretchers', 4, 'Patient transport'),
(2, 'Oxygen kits', 2, 'Emergency oxygen supply');

INSERT INTO volunteers (full_name, contact_number, email, location, skills,
                        availability, emergency_role, max_workload) VALUES
('Nithin George',  '9847000101', 'nithin@resqhub.org', 'Wayanad',
 'first aid, swimming, logistics', 'AVAILABLE', 'MEDICAL', 3),
('Priya Rajan',    '9847000102', 'priya@resqhub.org',   'Kozhikode',
 'shelter, counselling, first aid', 'AVAILABLE', 'SHELTER', 2),
('Faizal Khan',    '9847000103', 'faizal@resqhub.org',  'Thiruvananthapuram',
 'food distribution, driving', 'AVAILABLE', 'FOOD', 2);

INSERT INTO volunteer_skills (volunteer_id, skill_name) VALUES
(1, 'First aid'),
(1, 'Swimming'),
(1, 'Logistics'),
(2, 'Shelter management'),
(2, 'Counselling'),
(2, 'First aid'),
(3, 'Food distribution'),
(3, 'Driving');

-- Demo VOLUNTEER auth account linked to volunteer 1 for self-service testing
--   volunteer1 / Volunteer@123
INSERT INTO users (username, password_hash, full_name, email, phone, role_id) VALUES
('volunteer1', '3e789d2f398b50e9999d157196a68254dc44c67a04b9adefcefe8c7a195e2c6d',
 'Nithin George', 'nithin@resqhub.org', '9847000101', 6);

UPDATE volunteers SET user_id = (SELECT id FROM users WHERE username = 'volunteer1')
WHERE contact_number = '9847000101';


INSERT INTO rescue_requests (disaster_id, victim_id, requester_name, contact_number, location,
                             people_count, children_count, elderly_count, life_threatening,
                             medical_emergency, trapped_under_debris, required_assistance) VALUES
(1, 1, 'Anand Menon', '9847000001', 'Chundale, Wayanad',
 4, 2, 0, TRUE, TRUE, FALSE, 'Immediate medical evacuation and boat rescue'),
(2, NULL, 'Neighbour of Abdul', '9847000009', 'Kovalam cliff road',
 1, 0, 0, TRUE, FALSE, TRUE, 'Debris cutting team to extract trapped person');

-- ---------------------------------------------------------------------
-- 8. account_deletion_requests  (user-initiated deletion workflow)
--    users(1) --< account_deletion_requests(N) : many requests per user
-- ---------------------------------------------------------------------
CREATE TABLE account_deletion_requests (
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
