-- Food Distribution Management migration - apply only to an existing
-- ResQHub database (the full DDL also appears in resqhub_schema.sql for
-- fresh installs). Safe to re-run.
USE resqhub;

-- The Food module adds a FOOD notification type to the notifications enum.
ALTER TABLE notifications
    MODIFY type ENUM('CRITICAL_RESCUE','LOW_STOCK','ASSIGNMENT','FOOD','SYSTEM')
    NOT NULL;

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

-- Seed only if the requests table is empty (idempotent).
INSERT INTO food_requests (request_code, disaster_id, location,
    beneficiary_type, beneficiaries, required_quantity, priority, status,
    description, requested_at, created_by, allocated_quantity,
    allocated_resource_id, allocated_at, allocated_by)
SELECT 'FD-101', 1, 'Relief Camp A', 'SHELTER', 250, 750, 'HIGH', 'APPROVED',
       'Hot meals for shelter residents.', NOW(), 1, 0, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM food_requests WHERE request_code = 'FD-101');

INSERT INTO food_requests (request_code, disaster_id, location,
    beneficiary_type, beneficiaries, required_quantity, priority, status,
    description, requested_at, created_by, allocated_quantity,
    allocated_resource_id, allocated_at, allocated_by)
SELECT 'FD-102', 1, 'Relief Camp B', 'SHELTER', 180, 540, 'CRITICAL',
       'PARTIALLY_FULFILLED', 'High-priority meals; only 300 reserved so far.',
       NOW(), 1, 300, 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM food_requests WHERE request_code = 'FD-102');

INSERT INTO food_requests (request_code, disaster_id, location,
    beneficiary_type, beneficiaries, required_quantity, priority, status,
    description, requested_at, created_by, allocated_quantity,
    allocated_resource_id, allocated_at, allocated_by)
SELECT 'FD-103', 1, 'Chundale community', 'GROUP', 400, 800, 'MEDIUM',
       'COMPLETED', 'Community meal drive completed.', NOW(), 1, 800, 1,
       NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM food_requests WHERE request_code = 'FD-103');

INSERT INTO food_distributions (request_id, resource_id, quantity,
    beneficiaries_served, distributed_to, location, distributed_at,
    distributed_by, note)
SELECT r.id, 1, 200, 100, 'Relief Camp B', 'Relief Camp B', NOW(), 1,
       'First batch handed out.'
FROM food_requests r WHERE r.request_code = 'FD-102'
AND NOT EXISTS (SELECT 1 FROM food_distributions x WHERE x.request_id = r.id
                AND x.quantity = 200 AND x.beneficiaries_served = 100);

INSERT INTO food_distributions (request_id, resource_id, quantity,
    beneficiaries_served, distributed_to, location, distributed_at,
    distributed_by, note)
SELECT r.id, 1, 800, 400, 'Chundale community', 'Chundale community', NOW(),
       1, 'Community meal drive complete.'
FROM food_requests r WHERE r.request_code = 'FD-103'
AND NOT EXISTS (SELECT 1 FROM food_distributions x WHERE x.request_id = r.id
                AND x.quantity = 800 AND x.beneficiaries_served = 400);
