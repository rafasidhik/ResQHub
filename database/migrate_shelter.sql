-- Shelter Management migration - apply only these tables to an existing
-- ResQHub database (the full DDL also appears in resqhub_schema.sql for
-- fresh installs).
USE resqhub;

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
    INDEX idx_shelter_created_by (created_by)
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
    UNIQUE KEY uq_shelter_facility (shelter_id, facility_name)
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
    status        ENUM('ACTIVE','RELEASED') NOT NULL DEFAULT 'ACTIVE',
    allocated_by  BIGINT UNSIGNED NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_alloc_shelter (shelter_id, status),
    KEY idx_alloc_victim (victim_id)
) ENGINE = InnoDB;

-- Seed only if the shelters table is empty (idempotent migration).
INSERT INTO shelters (name, code, district, city, address, location_description,
                      max_capacity, current_occupancy, contact_number, manager_name,
                      disaster_id, wheelchair_accessible, elderly_friendly,
                      medical_accessible, special_assistance, operational_status,
                      created_by)
SELECT 'Relief Camp A', 'SHL-001', 'Malappuram', 'Malappuram',
 'Govt Higher Secondary School, Kottakkal', 'Main relief camp near the river bridge.',
 500, 480, '9846000001', 'Ravi Nair', 1, 1, 1, 1, 1, 'NEAR_CAPACITY', 1
WHERE NOT EXISTS (SELECT 1 FROM shelters WHERE code = 'SHL-001');

INSERT INTO shelters (name, code, district, city, address, location_description,
                      max_capacity, current_occupancy, contact_number, manager_name,
                      disaster_id, wheelchair_accessible, elderly_friendly,
                      medical_accessible, special_assistance, operational_status,
                      created_by)
SELECT 'Relief Camp B', 'SHL-002', 'Malappuram', 'Tirur',
 'Community Hall, Tirur', 'Second camp with kitchen facilities.',
 300, 120, '9846000002', 'Suma Menon', 1, 1, 1, 0, 0, 'AVAILABLE', 1
WHERE NOT EXISTS (SELECT 1 FROM shelters WHERE code = 'SHL-002');

INSERT INTO shelters (name, code, district, city, address, location_description,
                      max_capacity, current_occupancy, contact_number, manager_name,
                      disaster_id, wheelchair_accessible, elderly_friendly,
                      medical_accessible, special_assistance, operational_status,
                      created_by)
SELECT 'Wayanad Base Camp', 'SHL-003', 'Wayanad', 'Kalpetta',
 'Collectorate Annex, Kalpetta', 'District headquarter shelter.',
 800, 640, '9846000003', 'George Varghese', 1, 0, 1, 1, 1, 'ACTIVE', 2
WHERE NOT EXISTS (SELECT 1 FROM shelters WHERE code = 'SHL-003');

INSERT INTO shelters (name, code, district, city, address, location_description,
                      max_capacity, current_occupancy, contact_number, manager_name,
                      disaster_id, wheelchair_accessible, elderly_friendly,
                      medical_accessible, special_assistance, operational_status,
                      created_by)
SELECT 'Kovalam Relief Hall', 'SHL-004', 'Thiruvananthapuram', 'Kovalam',
 'Tourist Village Hall', 'Near the landslide site.',
 150, 150, '9846000004', 'Anitha S', 2, 1, 1, 1, 0, 'FULL', 2
WHERE NOT EXISTS (SELECT 1 FROM shelters WHERE code = 'SHL-004');

INSERT INTO shelter_facilities (shelter_id, facility_name, available)
SELECT s.id, f.facility_name, f.available FROM (
    SELECT 1 AS sid, 'Drinking Water' AS facility_name, 1 AS available
    UNION ALL SELECT 1, 'Food Facilities', 1
    UNION ALL SELECT 1, 'Medical Support', 1
    UNION ALL SELECT 1, 'Toilets', 1
    UNION ALL SELECT 1, 'Electricity', 1
    UNION ALL SELECT 2, 'Drinking Water', 1
    UNION ALL SELECT 2, 'Food Facilities', 1
    UNION ALL SELECT 2, 'Toilets', 1
    UNION ALL SELECT 2, 'Electricity', 0
    UNION ALL SELECT 3, 'Drinking Water', 1
    UNION ALL SELECT 3, 'Medical Support', 1
    UNION ALL SELECT 3, 'First Aid', 1
    UNION ALL SELECT 3, 'Sleeping Arrangements', 1
    UNION ALL SELECT 4, 'Drinking Water', 1
    UNION ALL SELECT 4, 'Food Facilities', 0
) f JOIN shelters s ON s.code = CASE f.sid
            WHEN 1 THEN 'SHL-001'
            WHEN 2 THEN 'SHL-002'
            WHEN 3 THEN 'SHL-003'
            WHEN 4 THEN 'SHL-004' END
WHERE NOT EXISTS (SELECT 1 FROM shelter_facilities x
                  WHERE x.shelter_id = s.id
                    AND x.facility_name = f.facility_name);

INSERT INTO shelter_allocations (shelter_id, victim_id, family_name, people_count,
                                 notes, status, allocated_by)
SELECT a.shelter_id, a.victim_id, a.family_name, a.people_count, a.notes,
       a.status, a.allocated_by
FROM (
    SELECT 1 AS shelter_id, 1 AS victim_id, 'Anand Menon & family' AS family_name,
           4 AS people_count, 'Allocated after rescue from Chundale.' AS notes,
           'ACTIVE' AS status, 1 AS allocated_by
    UNION ALL SELECT 1, 2, 'Lakshmi Pillai', 1,
           'Elderly resident, diabetic - needs medical attention.', 'ACTIVE', 1
    UNION ALL SELECT 3, NULL, 'Kollam family of 5', 5,
           'Family from Wayanad, awaiting individual victim records.', 'ACTIVE', 2
) a
JOIN shelters s ON s.code = CASE a.shelter_id
            WHEN 1 THEN 'SHL-001'
            WHEN 3 THEN 'SHL-003' END
WHERE NOT EXISTS (SELECT 1 FROM shelter_allocations x
                  WHERE x.shelter_id = s.id AND x.family_name = a.family_name);
