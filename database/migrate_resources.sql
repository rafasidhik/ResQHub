-- Resource & Inventory Management migration - apply only these tables to
-- an existing ResQHub database (the full DDL also appears in
-- resqhub_schema.sql for fresh installs). Safe to re-run.
USE resqhub;

CREATE TABLE IF NOT EXISTS resources (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name              VARCHAR(150)    NOT NULL,
    code              VARCHAR(30)     NOT NULL,
    category          ENUM('FOOD','WATER','MEDICINE','CLOTHING',
                           'SHELTER_SUPPLIES','MEDICAL_SUPPLIES',
                           'RESCUE_EQUIPMENT','OTHER') NOT NULL,
    available_quantity INT UNSIGNED   NOT NULL DEFAULT 0,
    minimum_level     INT UNSIGNED    NOT NULL DEFAULT 0,
    unit              VARCHAR(40)     NULL,
    description       VARCHAR(300)    NULL,
    status            ENUM('AVAILABLE','LOW_STOCK','OUT_OF_STOCK')
                      NOT NULL DEFAULT 'AVAILABLE',
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
    source            VARCHAR(100)    NULL,
    destination       VARCHAR(150)    NULL,
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
    distributed_to    VARCHAR(150)    NOT NULL,
    destination       ENUM('SHELTER','VICTIM','DISASTER_AREA','RESCUE_TEAM',
                           'HOSPITAL','FOOD_DISTRIBUTION','OTHER') NOT NULL,
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

-- Seed resources only if the resources table is empty (idempotent).
INSERT INTO resources (name, code, category, available_quantity,
                        minimum_level, unit, description, status, created_by)
SELECT 'Food Packets', 'RES-FOOD-001', 'FOOD', 1500, 300, 'packets',
       'Meal packets for distribution.', 'AVAILABLE', 1
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE code = 'RES-FOOD-001');

INSERT INTO resources (name, code, category, available_quantity,
                        minimum_level, unit, description, status, created_by)
SELECT 'Water Bottles', 'RES-WTR-002', 'WATER', 2000, 500, 'bottles',
       '500 ml sealed water bottles.', 'AVAILABLE', 1
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE code = 'RES-WTR-002');

INSERT INTO resources (name, code, category, available_quantity,
                        minimum_level, unit, description, status, created_by)
SELECT 'Medicines', 'RES-MED-003', 'MEDICINE', 120, 200, 'boxes',
       'Assorted essential medicines.', 'LOW_STOCK', 1
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE code = 'RES-MED-003');

INSERT INTO resources (name, code, category, available_quantity,
                        minimum_level, unit, description, status, created_by)
SELECT 'Blankets', 'RES-BLK-004', 'SHELTER_SUPPLIES', 700, 200, 'blankets',
       'Warm blankets for shelters and victims.', 'AVAILABLE', 2
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE code = 'RES-BLK-004');

INSERT INTO resources (name, code, category, available_quantity,
                        minimum_level, unit, description, status, created_by)
SELECT 'First-Aid Kits', 'RES-FAK-005', 'MEDICAL_SUPPLIES', 20, 50, 'kits',
       'Basic first-aid kits.', 'LOW_STOCK', 2
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE code = 'RES-FAK-005');

INSERT INTO resources (name, code, category, available_quantity,
                        minimum_level, unit, description, status, created_by)
SELECT 'Rescue Ropes', 'RES-RPE-006', 'RESCUE_EQUIPMENT', 0, 40, 'coils',
       'Ropes for rescue operations.', 'OUT_OF_STOCK', 1
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE code = 'RES-RPE-006');

INSERT INTO resources (name, code, category, available_quantity,
                        minimum_level, unit, description, status, created_by)
SELECT 'Clothing', 'RES-CLO-007', 'CLOTHING', 300, 100, 'sets',
       'Clothing sets for affected families.', 'AVAILABLE', 1
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE code = 'RES-CLO-007');

-- Seed stock movements referencing only seeded resources (idempotent).
INSERT INTO stock_movements (resource_id, movement_type, quantity,
    previous_quantity, new_quantity, source, destination, reason,
    disaster_id, recorded_by)
SELECT r.id, 'STOCK_IN', 500, 1000, 1500, 'Donation', NULL,
       'Initial relief supply received.', 1, 1
FROM resources r WHERE r.code = 'RES-FOOD-001'
AND NOT EXISTS (SELECT 1 FROM stock_movements x
                WHERE x.resource_id = r.id
                  AND x.previous_quantity = 1000 AND x.movement_type = 'STOCK_IN');

INSERT INTO stock_movements (resource_id, movement_type, quantity,
    previous_quantity, new_quantity, source, destination, reason,
    disaster_id, recorded_by)
SELECT r.id, 'STOCK_IN', 800, 1200, 2000, 'Government Supplies', NULL,
       'State allocation for relief camps.', 1, 1
FROM resources r WHERE r.code = 'RES-WTR-002'
AND NOT EXISTS (SELECT 1 FROM stock_movements x
                WHERE x.resource_id = r.id
                  AND x.previous_quantity = 1200 AND x.movement_type = 'STOCK_IN');

INSERT INTO stock_movements (resource_id, movement_type, quantity,
    previous_quantity, new_quantity, source, destination, reason,
    disaster_id, recorded_by)
SELECT r.id, 'STOCK_OUT', 100, 2000, 1900, NULL, 'Relief Camp A', 'Shelter support.', 1, 1
FROM resources r WHERE r.code = 'RES-WTR-002'
AND NOT EXISTS (SELECT 1 FROM stock_movements x
                WHERE x.resource_id = r.id
                  AND x.previous_quantity = 2000 AND x.movement_type = 'STOCK_OUT');

-- Seed a distribution record (idempotent by unique-ish combination).
INSERT INTO resource_distributions (resource_id, quantity, distributed_to,
    destination, disaster_id, shelter_id, victim_id, reason, distributed_by)
SELECT r.id, 100, 'Relief Camp A', 'SHELTER', 1, 1, NULL,
       'Drinking water for residents.', 1
FROM resources r WHERE r.code = 'RES-WTR-002'
AND NOT EXISTS (SELECT 1 FROM resource_distributions x
                WHERE x.resource_id = r.id AND x.distributed_to = 'Relief Camp A');
