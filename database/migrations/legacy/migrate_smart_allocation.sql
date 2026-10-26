-- Smart Shelter Allocation migration.
-- Widens shelter_allocations.status so the allocation lifecycle can
-- track PENDING -> ACTIVE -> CHECKED_IN -> COMPLETED / CANCELLED
-- (RELEASED is retained for the legacy release action).
-- Safe to re-run: MODIFY COLUMN converges to the same definition.

ALTER TABLE shelter_allocations
    MODIFY COLUMN status ENUM('PENDING','ACTIVE','CHECKED_IN',
                              'COMPLETED','CANCELLED','RELEASED')
                 NOT NULL DEFAULT 'ACTIVE';

-- Idempotent seed of a few non-ACTIVE allocations so the Smart
-- Allocation management view and the Allocation Overview report have
-- data on a fresh build.
INSERT INTO shelter_allocations (shelter_id, victim_id, family_name,
                                 people_count, notes, status, allocated_by)
SELECT s.id, NULL, 'PENDING family - Malappuram evacuees', 6,
       'Reserved at Relief Camp B while transport is arranged.',
       'PENDING', 1
FROM shelters s WHERE s.code = 'SHL-002'
AND NOT EXISTS (SELECT 1 FROM shelter_allocations x
                WHERE x.shelter_id = s.id AND x.family_name =
                    'PENDING family - Malappuram evacuees');

INSERT INTO shelter_allocations (shelter_id, victim_id, family_name,
                                 people_count, notes, status, allocated_by)
SELECT s.id, NULL, 'Completed camp stay', 2,
       'Family completed their stay. ', 'COMPLETED', 1
FROM shelters s WHERE s.code = 'SHL-001'
AND NOT EXISTS (SELECT 1 FROM shelter_allocations x
                WHERE x.shelter_id = s.id AND x.family_name =
                    'Completed camp stay');

INSERT INTO shelter_allocations (shelter_id, victim_id, family_name,
                                 people_count, notes, status, allocated_by)
SELECT s.id, NULL, 'Cancelled reservation', 3,
       'Family relocated to the Wayanad Base Camp instead.',
       'CANCELLED', 1
FROM shelters s WHERE s.code = 'SHL-002'
AND NOT EXISTS (SELECT 1 FROM shelter_allocations x
                WHERE x.shelter_id = s.id AND x.family_name =
                    'Cancelled reservation');
