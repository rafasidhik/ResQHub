-- Blood Donor Management migration - apply only these tables to an existing
-- ResQHub database. Creates the blood donor, request, match and donation
-- tables plus idempotent seed data. Adds the BLOOD notification type to the
-- notifications enum (the module raises supply-shortage alerts).
USE resqhub;

-- The Blood module adds a BLOOD notification type to the notifications enum.
ALTER TABLE notifications
    MODIFY type ENUM('CRITICAL_RESCUE','LOW_STOCK','ASSIGNMENT','FOOD',
                     'BLOOD','SYSTEM')
    NOT NULL;

-- -------------------------------------------------------------------------
-- Registered blood donors
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_donors (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name          VARCHAR(150)    NOT NULL,
    blood_group        ENUM('A_POSITIVE','A_NEGATIVE','B_POSITIVE',
                            'B_NEGATIVE','AB_POSITIVE','AB_NEGATIVE',
                            'O_POSITIVE','O_NEGATIVE') NOT NULL,
    location           VARCHAR(150)    NOT NULL,
    phone              VARCHAR(20)     NULL,
    email              VARCHAR(120)    NULL,
    availability       ENUM('AVAILABLE','UNAVAILABLE','DEFERRED','PENDING') NOT NULL DEFAULT 'AVAILABLE',
    last_donation_date DATE            NULL,
    eligibility        ENUM('ELIGIBLE','TEMPORARY_DEFERRED','PERMANENTLY_INELIGIBLE','MEDICAL_HOLD')
                           NOT NULL DEFAULT 'ELIGIBLE',
    notes              VARCHAR(500)    NULL,
    registered_by      BIGINT UNSIGNED NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_donor_phone (phone),
    KEY idx_donor_blood_group (blood_group, availability, eligibility),
    KEY idx_donor_location (location),
    KEY idx_donor_registered_by (registered_by),
    CONSTRAINT fk_donor_registered_by FOREIGN KEY (registered_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- -------------------------------------------------------------------------
-- Blood requests (a hospital/victim need for blood)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_requests (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_code          VARCHAR(30)     NOT NULL,
    blood_group           ENUM('A_POSITIVE','A_NEGATIVE','B_POSITIVE',
                               'B_NEGATIVE','AB_POSITIVE','AB_NEGATIVE',
                               'O_POSITIVE','O_NEGATIVE') NOT NULL,
    units_required        INT UNSIGNED    NOT NULL DEFAULT 1,
    location              VARCHAR(150)    NOT NULL,
    priority              ENUM('CRITICAL','HIGH','MEDIUM','LOW')
                              NOT NULL DEFAULT 'MEDIUM',
    status                ENUM('PENDING','MATCHING_DONORS','DONOR_FOUND',
                               'BLOOD_COLLECTED','FULFILLED','CANCELLED')
                              NOT NULL DEFAULT 'PENDING',
    emergency_details     VARCHAR(500)    NULL,
    hospital_id           BIGINT UNSIGNED NULL,
    victim_id             BIGINT UNSIGNED NULL,
    disaster_id           BIGINT UNSIGNED NULL,
    created_by            BIGINT UNSIGNED NULL,
    request_date          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    required_date         DATETIME        NULL,
    fulfilled_by_match_id BIGINT UNSIGNED NULL,
    created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_request_code (request_code),
    KEY idx_request_status (status, priority),
    KEY idx_request_blood_group (blood_group),
    KEY idx_request_hospital (hospital_id),
    KEY idx_request_victim (victim_id),
    KEY idx_request_disaster (disaster_id),
    KEY idx_request_created_by (created_by),
    CONSTRAINT fk_request_hospital FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE SET NULL,
    CONSTRAINT fk_request_victim FOREIGN KEY (victim_id)
        REFERENCES victims (id)  ON DELETE SET NULL,
    CONSTRAINT fk_request_disaster FOREIGN KEY (disaster_id)
        REFERENCES disasters (id) ON DELETE SET NULL,
    CONSTRAINT fk_request_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- -------------------------------------------------------------------------
-- Donor-request matches (proposed / contacted / confirmed / collected)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_matches (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id          BIGINT UNSIGNED NOT NULL,
    donor_id            BIGINT UNSIGNED NOT NULL,
    status              ENUM('SUGGESTED','CONTACTED','CONFIRMED','COLLECTED',
                             'DECLINED','CANCELLED') NOT NULL DEFAULT 'SUGGESTED',
    units_matched       INT UNSIGNED    NOT NULL DEFAULT 1,
    location_matched    TINYINT(1)      NOT NULL DEFAULT 0,
    donor_distance_rank INT UNSIGNED    NOT NULL DEFAULT 0,
    notes               VARCHAR(500)    NULL,
    matched_by          BIGINT UNSIGNED NULL,
    matched_at          DATETIME        NULL,
    confirmed_at        DATETIME        NULL,
    collected_at        DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_match_request_donor (request_id, donor_id),
    KEY idx_match_donor (donor_id),
    KEY idx_match_status (status),
    KEY idx_match_matched_by (matched_by),
    CONSTRAINT fk_match_request FOREIGN KEY (request_id)
        REFERENCES blood_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_match_donor FOREIGN KEY (donor_id)
        REFERENCES blood_donors  (id) ON DELETE CASCADE,
    CONSTRAINT fk_match_matched_by FOREIGN KEY (matched_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- -------------------------------------------------------------------------
-- Completed / recorded donations
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_donations (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    donor_id         BIGINT UNSIGNED NULL,
    blood_group      ENUM('A_POSITIVE','A_NEGATIVE','B_POSITIVE','B_NEGATIVE',
                          'AB_POSITIVE','AB_NEGATIVE','O_POSITIVE',
                          'O_NEGATIVE') NOT NULL,
    donation_date    DATE            NOT NULL,
    request_id       BIGINT UNSIGNED NULL,
    units_donated    INT UNSIGNED    NOT NULL DEFAULT 1,
    donation_status  VARCHAR(40)     NOT NULL DEFAULT 'COLLECTED',
    notes            VARCHAR(500)    NULL,
    recorded_by      BIGINT UNSIGNED NULL,
    recorded_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_donation_donor (donor_id, donation_date),
    KEY idx_donation_request (request_id),
    KEY idx_donation_date (donation_date),
    KEY idx_donation_recorded_by (recorded_by),
    CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id)
        REFERENCES blood_donors (id) ON DELETE SET NULL,
    CONSTRAINT fk_donation_request FOREIGN KEY (request_id)
        REFERENCES blood_requests (id) ON DELETE SET NULL,
    CONSTRAINT fk_donation_recorded_by FOREIGN KEY (recorded_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- The self-referential request -> match link used to record which match
-- ultimately fulfilled a request.
ALTER TABLE blood_requests
    ADD CONSTRAINT fk_request_fulfilled_match FOREIGN KEY (fulfilled_by_match_id)
        REFERENCES blood_matches (id) ON DELETE SET NULL;

-- -------------------------------------------------------------------------
-- Blood request lifecycle history (spec: Request History)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_request_history (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id   BIGINT UNSIGNED NULL,
    event        VARCHAR(120)    NOT NULL,
    details      VARCHAR(500)    NULL,
    performed_by BIGINT UNSIGNED NULL,
    performed_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_history_request (request_id, performed_at),
    KEY idx_history_performed_by (performed_by),
    CONSTRAINT fk_history_request FOREIGN KEY (request_id)
        REFERENCES blood_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_performed_by FOREIGN KEY (performed_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- -------------------------------------------------------------------------
-- Idempotent seed data (registered_by admin=1 / officer1=2; disaster 1;
-- victims 1-3)
-- -------------------------------------------------------------------------
INSERT IGNORE INTO blood_donors
    (full_name, blood_group, location, phone, email, availability,
     last_donation_date, eligibility, notes, registered_by)
VALUES
    ('Raju Varma',      'O_NEGATIVE', 'Wayanad district, Kerala',
     '9847001001', 'raju@resqhub.org', 'AVAILABLE', NULL,
     'ELIGIBLE', 'Universal donor; rapid responder', 1),
    ('Kavya Nair',      'O_POSITIVE', 'Chundale relief point',
     '9847001002', 'kavya@resqhub.org', 'AVAILABLE',
     DATE_SUB(CURDATE(), INTERVAL 95 DAY), 'ELIGIBLE',
     'Regular donor', 2),
    ('Manoj Pillai',    'A_POSITIVE', 'Kalpetta, Wayanad',
     '9847001003', 'manoj@resqhub.org', 'AVAILABLE',
     DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'ELIGIBLE', NULL, 1),
    ('Aiswarya Das',    'B_POSITIVE', 'Wayandad district, Kerala',
     '9847001004', 'aiswarya@resqhub.org', 'UNAVAILABLE',
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'ELIGIBLE',
     'Recently donated; 90-day window', 2),
    ('Thomas Kurian',   'AB_POSITIVE', 'Chundale relief point',
     '9847001005', 'thomas@resqhub.org', 'AVAILABLE', NULL,
     'TEMPORARY_DEFERRED', 'Medical review pending', 1);

INSERT IGNORE INTO blood_requests
    (request_code, blood_group, units_required, location, priority, status,
     emergency_details, hospital_id, victim_id, disaster_id, created_by)
VALUES
    ('BR-RQ-0001', 'O_NEGATIVE', 2, 'Chundale relief point', 'CRITICAL',
     'MATCHING_DONORS', 'Trauma victim with severe bleeding; universal donor needed',
     1, 1, 1, 2),
    ('BR-RQ-0002', 'A_POSITIVE', 1, 'Kalpetta, Wayanad', 'HIGH',
     'PENDING', 'Surgery requirement', 1, 2, 1, 1),
    ('BR-RQ-0003', 'B_POSITIVE', 1, 'Chundale relief point', 'MEDIUM',
     'PENDING', 'Stock replenishment for clinic', NULL, 3, 1, 1);

-- Seed one confirmed match for the critical request so the matching/report
-- screens have immediate data to show.
INSERT IGNORE INTO blood_matches
    (request_id, donor_id, status, units_matched, location_matched,
     donor_distance_rank, notes, matched_by, matched_at)
SELECT r.id, d.id, 'CONFIRMED', 1, 1, 1, 'Nearest universal donor on site',
       2, NOW()
FROM blood_requests r
JOIN blood_donors d ON d.blood_group = 'O_NEGATIVE'
WHERE r.request_code = 'BR-RQ-0001'
  AND d.full_name = 'Raju Varma'
LIMIT 1;

-- Update the critical request so it reflects that a donor was found.
UPDATE blood_requests r
JOIN blood_matches m ON m.request_id = r.id AND m.status = 'CONFIRMED'
SET r.status = 'DONOR_FOUND'
WHERE r.request_code = 'BR-RQ-0001';
