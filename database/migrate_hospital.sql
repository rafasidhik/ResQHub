-- Hospital Management migration - apply only these tables to an existing
-- ResQHub database. Creates the hospital, referral and capacity-log tables
-- plus idempotent seed data for hospitals / referrals / capacity history.
USE resqhub;

CREATE TABLE IF NOT EXISTS hospitals (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name             VARCHAR(150)    NOT NULL,
    hospital_id      VARCHAR(30)     NOT NULL,
    district         VARCHAR(80)     NOT NULL,
    city             VARCHAR(80)     NULL,
    area             VARCHAR(80)     NULL,
    address          VARCHAR(200)    NULL,
    phone            VARCHAR(20)     NULL,
    emergency_contact VARCHAR(20)    NULL,
    email            VARCHAR(120)    NULL,
    total_beds       INT UNSIGNED    NOT NULL,
    occupied_beds    INT UNSIGNED    NOT NULL DEFAULT 0,
    facilities       VARCHAR(500)    NULL,
    status           ENUM('AVAILABLE','LIMITED_CAPACITY','FULL','INACTIVE',
                          'EMERGENCY_ONLY') NOT NULL DEFAULT 'AVAILABLE',
    disaster_id      BIGINT UNSIGNED NULL,
    created_by       BIGINT UNSIGNED NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hospital_id (hospital_id),
    KEY idx_hospital_status (status),
    KEY idx_hospital_district (district),
    KEY idx_hospital_disaster (disaster_id),
    KEY idx_hospital_created_by (created_by),
    CONSTRAINT fk_hospital_disaster FOREIGN KEY (disaster_id)
        REFERENCES disasters (id) ON DELETE SET NULL,
    CONSTRAINT fk_hospital_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS hospital_referrals (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    hospital_id         BIGINT UNSIGNED NOT NULL,
    victim_id           BIGINT UNSIGNED NULL,
    victim_name         VARCHAR(150)    NULL,
    reason              VARCHAR(300)    NOT NULL,
    beds_required       INT UNSIGNED    NOT NULL DEFAULT 1,
    required_facilities VARCHAR(500)    NULL,
    status              ENUM('PENDING','ACCEPTED','ADMITTED','DISCHARGED',
                             'REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    beds_applied        TINYINT(1)      NOT NULL DEFAULT 0,
    referred_by         BIGINT UNSIGNED NULL,
    referred_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at           DATETIME        NULL,
    notes               VARCHAR(500)    NULL,
    disaster_id         BIGINT UNSIGNED NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_referral_hospital (hospital_id, status),
    KEY idx_referral_victim (victim_id),
    KEY idx_referral_status (status),
    KEY idx_referral_disaster (disaster_id),
    KEY idx_referral_referred_by (referred_by),
    CONSTRAINT fk_referral_hospital FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE CASCADE,
    CONSTRAINT fk_referral_victim FOREIGN KEY (victim_id)
        REFERENCES victims (id) ON DELETE SET NULL,
    CONSTRAINT fk_referral_disaster FOREIGN KEY (disaster_id)
        REFERENCES disasters (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS hospital_capacity_logs (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    hospital_id       BIGINT UNSIGNED NOT NULL,
    previous_occupied INT UNSIGNED    NOT NULL DEFAULT 0,
    updated_occupied  INT UNSIGNED    NOT NULL DEFAULT 0,
    available_beds    INT UNSIGNED    NOT NULL DEFAULT 0,
    reason            VARCHAR(400)    NULL,
    changed_by        BIGINT UNSIGNED NULL,
    changed_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_caplog_hospital (hospital_id, changed_at),
    CONSTRAINT fk_caplog_hospital FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Seed only if the hospitals table is empty (idempotent migration).
INSERT INTO hospitals (name, hospital_id, district, city, area, address,
                       phone, emergency_contact, email, total_beds,
                       occupied_beds, facilities, status, disaster_id,
                       created_by)
SELECT 'District Hospital Malappuram', 'HSP-001', 'Malappuram', 'Malappuram',
 'Kottakkal', 'Govt Medical College Rd, Kottakkal', '0483-2730001',
 '9846001001', 'dh@malappuram.gov.in', 300, 285,
 'EMERGENCY_DEPARTMENT,TRAUMA_CARE,ICU,AMBULANCE_SUPPORT,SURGERY_FACILITIES,BLOOD_BANK,VENTILATOR_SUPPORT',
 'LIMITED_CAPACITY', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE hospital_id = 'HSP-001');

INSERT INTO hospitals (name, hospital_id, district, city, area, address,
                       phone, emergency_contact, email, total_beds,
                       occupied_beds, facilities, status, disaster_id,
                       created_by)
SELECT 'Taluk Hospital Tirur', 'HSP-002', 'Malappuram', 'Tirur', 'Tirur',
 'Thazhe Angadi, Tirur', '0483-2720002', '9846001002',
 'taluk@tirur.gov.in', 120, 78,
 'EMERGENCY_DEPARTMENT,TRAUMA_CARE,ICU,AMBULANCE_SUPPORT',
 'AVAILABLE', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE hospital_id = 'HSP-002');

INSERT INTO hospitals (name, hospital_id, district, city, area, address,
                       phone, emergency_contact, email, total_beds,
                       occupied_beds, facilities, status, disaster_id,
                       created_by)
SELECT 'Wayanad Medical College', 'HSP-003', 'Wayanad', 'Kalpetta',
 'Kalpetta', 'Medical College Campus, Kalpetta', '0493-2730003',
 '9846001003', 'admin@wdmc.edu.in', 500, 500,
 'EMERGENCY_DEPARTMENT,TRAUMA_CARE,ICU,AMBULANCE_SUPPORT,SURGERY_FACILITIES,BLOOD_BANK,VENTILATOR_SUPPORT',
 'FULL', 1, 2
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE hospital_id = 'HSP-003');

INSERT INTO hospitals (name, hospital_id, district, city, area, address,
                       phone, emergency_contact, email, total_beds,
                       occupied_beds, facilities, status, disaster_id,
                       created_by)
SELECT 'Taluk Hospital Thiruvananthapuram', 'HSP-004',
 'Thiruvananthapuram', 'Thiruvananthapuram', 'Kovalam', 'Kovalam Junction',
 '0471-2730004', '9846001004', 'th@tvm.gov.in', 80, 60,
 'EMERGENCY_DEPARTMENT,ICU,AMBULANCE_SUPPORT',
 'LIMITED_CAPACITY', 2, 2
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE hospital_id = 'HSP-004');

INSERT INTO hospitals (name, hospital_id, district, city, area, address,
                       phone, emergency_contact, email, total_beds,
                       occupied_beds, facilities, status, disaster_id,
                       created_by)
SELECT 'Kozhikode Trauma Centre', 'HSP-005', 'Kozhikode', 'Kozhikode',
 'Medical College', 'Gokulam Road, Medical College, Kozhikode',
 '0495-2730005', '9846001005', 'trauma@kzhd.in', 150, 45,
 'EMERGENCY_DEPARTMENT,TRAUMA_CARE,ICU,AMBULANCE_SUPPORT,SURGERY_FACILITIES,VENTILATOR_SUPPORT',
 'AVAILABLE', NULL, 2
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE hospital_id = 'HSP-005');

INSERT INTO hospital_referrals (hospital_id, victim_id, victim_name, reason,
                                beds_required, required_facilities, status,
                                beds_applied, referred_by, referred_at, notes,
                                disaster_id)
SELECT h.id, 1, 'Anand Menon', 'Leg fracture requiring surgery and ICU care',
       2, 'TRAUMA_CARE,SURGERY_FACILITIES,ICU', 'ADMITTED', 1, 2,
       NOW() - INTERVAL 1 DAY, 'Brought by rescue team Alpha from Chundale.', 1
FROM hospitals h WHERE h.hospital_id = 'HSP-001'
  AND NOT EXISTS (SELECT 1 FROM hospital_referrals x
                  WHERE x.victim_id = 1 AND x.hospital_id = h.id);

INSERT INTO hospital_referrals (hospital_id, victim_id, victim_name, reason,
                                beds_required, required_facilities, status,
                                beds_applied, referred_by, referred_at, notes,
                                disaster_id)
SELECT h.id, 2, 'Lakshmi Pillai', 'Diabetic dehydration, needs observation',
       1, 'ICU', 'PENDING', 0, 2, NOW() - INTERVAL 5 HOUR,
       'Elderly resident requiring close monitoring.', 1
FROM hospitals h WHERE h.hospital_id = 'HSP-002'
  AND NOT EXISTS (SELECT 1 FROM hospital_referrals x
                  WHERE x.victim_id = 2 AND x.hospital_id = h.id);
