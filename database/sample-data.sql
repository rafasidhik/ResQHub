-- =======================================================================
-- ResQHub Sample Demonstration Data (Kerala)
-- Password for all seed users is: Password@123
-- Verified Spring Security BCrypt hash for 'Password@123':
-- $2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC
-- =======================================================================

USE resqhub;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE activity_logs;
TRUNCATE TABLE notifications;
TRUNCATE TABLE donations;
TRUNCATE TABLE food_distributions;
TRUNCATE TABLE resource_allocations;
TRUNCATE TABLE inventory_transactions;
TRUNCATE TABLE inventory_items;
TRUNCATE TABLE volunteer_assignments;
TRUNCATE TABLE volunteers;
TRUNCATE TABLE shelter_allocations;
TRUNCATE TABLE shelter_residents;
TRUNCATE TABLE shelters;
TRUNCATE TABLE rescue_assignments;
TRUNCATE TABLE rescue_team_members;
TRUNCATE TABLE rescue_teams;
TRUNCATE TABLE rescue_requests;
TRUNCATE TABLE victims;
TRUNCATE TABLE disasters;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- Seed Users (Password@123 for all: $2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC)
INSERT INTO users (id, username, email, password_hash, full_name, phone, role, status) VALUES
(1, 'admin', 'admin@resqhub.org', '$2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC', 'Administrator System', '+91 98765 43210', 'ADMIN', 'ACTIVE'),
(2, 'rescue_officer', 'rescue.officer@resqhub.org', '$2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC', 'Kavita Menon', '+91 98765 43211', 'RESCUE_OFFICER', 'ACTIVE'),
(3, 'rescue_officer2', 'rescue.officer2@resqhub.org', '$2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC', 'Rajesh Nair', '+91 98765 43212', 'RESCUE_OFFICER', 'ACTIVE'),
(4, 'camp_manager', 'camp.manager@resqhub.org', '$2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC', 'Anjali Menon', '+91 98765 43213', 'CAMP_MANAGER', 'ACTIVE'),
(5, 'camp_manager2', 'camp.manager2@resqhub.org', '$2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC', 'Arun Kurup', '+91 98765 43214', 'CAMP_MANAGER', 'ACTIVE'),
(6, 'volunteer_user', 'volunteer.user@resqhub.org', '$2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC', 'Deepak Menon', '+91 98765 43215', 'VOLUNTEER', 'ACTIVE'),
(7, 'volunteer_user2', 'volunteer.user2@resqhub.org', '$2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC', 'Priya Nambiar', '+91 98765 43216', 'VOLUNTEER', 'ACTIVE'),
(8, 'citizen_user', 'citizen.user@resqhub.org', '$2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC', 'Rahul Nair', '+91 98765 43217', 'CITIZEN', 'ACTIVE'),
(9, 'demo', 'demo@resqhub.org', '$2a$10$Dr1m0dEIFXy3EduetGX19.9.SXL37Uk1VynSYMIbQHnirGS8A1HQC', 'Demo', '+91 98765 43218', 'CITIZEN', 'ACTIVE');

-- Seed Disasters (Kerala)
INSERT INTO disasters (id, name, disaster_type, description, location, severity, status, start_date) VALUES
(1, 'Meppadi & Mundakkai Landslide Disaster', 'LANDSLIDE', 'Torrential monsoon downpour triggered devastating landslides and debris flows across the Mundakkai, Chooralmala and Meppadi highlands of Wayanad.', 'Meppadi, Mundakkai & Chooralmala, Wayanad District', 'CRITICAL', 'ACTIVE', '2026-10-10'),
(2, 'Kuttanad Backwater Flood', 'FLOOD', 'Rising Vembanad lake levels and heavy upstream monsoon discharge inundated the paddy-belt villages and roads of the Kuttanad region.', 'Kuttanad & Champakulam, Alappuzha District', 'HIGH', 'ACTIVE', '2026-10-12'),
(3, 'Periyar Basin Flash Flood', 'FLOOD', 'Surplus water released from the Idukki reservoir caused sudden flash flooding along the Periyar river through Aluva and Kalady.', 'Periyar Basin - Aluva & Kalady, Ernakulam District', 'HIGH', 'CONTAINED', '2026-10-08'),
(4, 'Ockhi Coastal Cyclone Surge', 'CYCLONE', 'Cyclonic storm surge and gale-force winds battered the southern coastal belt, damaging fishing hamlets and uprooting trees.', 'Poonthura & Vizhinjam Coastal Belt, Thiruvananthapuram District', 'MODERATE', 'CLOSED', '2026-10-01');

-- Seed Victims
INSERT INTO victims (id, reference_number, disaster_id, full_name, age, contact_number, location, affected_family_members, accessibility_needs, current_situation, rescue_status, priority) VALUES
(1, 'VIC-2026-0001', 1, 'Thomas Kuruvilla', 48, '+91 98111 22334', 'Mundakkai Town, Near Chooralmala Bridge, Wayanad', 4, 'Elderly parent with wheelchair requirement', 'Trapped under debris on upper slope, water level rising rapidly', 'AWAITING_RESCUE', 'CRITICAL'),
(2, 'VIC-2026-0002', 1, 'Fathima Beevi', 34, '+91 98111 22335', 'Attamala Estate, Meppadi, Wayanad', 3, 'Infant child requiring emergency infant nutrition', 'Cut off by fast-flowing mud currents, boat assistance requested', 'ASSIGNED', 'HIGH'),
(3, 'VIC-2026-0003', 2, 'Rajeevan P. M.', 52, '+91 98111 22336', 'Champakulam Village, Kuttanad, Alappuzha', 5, 'None', 'Evacuated from submerged paddy-field home; sheltered in Community Hall', 'SHELTERED', 'MEDIUM'),
(4, 'VIC-2026-0004', 3, 'Sarojini Amma', 67, '+91 98111 22337', 'Kalady Bridge Road, Aluva, Ernakulam', 2, 'Walking assistance / cardiac patient', 'Rescued safely by Kerala Fire & Rescue swift-water unit', 'RESCUED', 'HIGH');

-- Seed Rescue Requests
INSERT INTO rescue_requests (id, request_number, victim_id, disaster_id, reported_by_user_id, location, description, contact_number, people_count, urgency, calculated_priority, status) VALUES
(1, 'REQ-2026-0001', 1, 1, 8, 'Mundakkai Town, Near Chooralmala Bridge, Wayanad', '4 family members including grandmother unable to escape the rising debris and floodwater.', '+91 98111 22334', 4, 'CRITICAL', 'CRITICAL', 'PENDING'),
(2, 'REQ-2026-0002', 2, 1, 8, 'Attamala Estate, Meppadi, Wayanad', 'Young mother with 6-month-old infant cut off by fast-moving mud currents.', '+91 98111 22335', 3, 'HIGH', 'HIGH', 'ASSIGNED'),
(3, 'REQ-2026-0003', NULL, 2, 9, 'Champakulam Higher Secondary School, Kuttanad', '12 local residents stranded on higher floor with dwindling drinking supplies.', '+91 98111 55667', 12, 'HIGH', 'HIGH', 'VERIFIED'),
(4, 'REQ-2026-0004', 4, 3, 2, 'Kalady Bridge Road, Aluva, Ernakulam', 'Elderly couple evacuated from the flooded Periyar riverbank.', '+91 98111 22337', 2, 'MEDIUM', 'HIGH', 'COMPLETED');

-- Seed Rescue Teams (Kerala)
INSERT INTO rescue_teams (id, team_identifier, name, team_leader, contact_number, skills, operating_area, capacity, availability_status, status) VALUES
(1, 'TEAM-001', 'NDRF 4th Battalion Rapid Response Unit', 'Dy. Commandant Sreejith Kumar', '+91 98700 11001', 'Water Rescue, Flood Evacuation, Medical First Response', 'Wayanad Highlands - Meppadi Sector', 15, 'ASSIGNED', 'ACTIVE'),
(2, 'TEAM-002', 'Kerala Fire & Rescue - Mountain Unit', 'Station Officer Anil Varghese', '+91 98700 11002', 'High Altitude Ropes, Landslide Debris Clearance, Heavy Rigging', 'Wayanad & Idukki Hill Districts', 12, 'AVAILABLE', 'ACTIVE'),
(3, 'TEAM-003', 'Kerala Civil Defence Swift Water Squad', 'Squad Leader Reshma Thomas', '+91 98700 11003', 'Inflatable Boat Navigation, Night Diving, CPR Certified', 'Kuttanad & Vembanad Backwaters', 10, 'AVAILABLE', 'ACTIVE');

-- Seed Rescue Team Members
INSERT INTO rescue_team_members (id, team_id, member_name, role_title, contact_number, specialty) VALUES
(1, 1, 'Sreejith Kumar', 'Team Commander', '+91 98700 11001', 'Incident Command'),
(2, 1, 'Suresh Pillai', 'Boat Operator', '+91 98700 11011', 'Motorized Zodiac Boat Handling'),
(3, 1, 'Dr. Geeta Nair', 'Field Paramedic', '+91 98700 11012', 'Trauma and Emergency First Aid'),
(4, 2, 'Anil Varghese', 'Team Leader', '+91 98700 11002', 'Mountain Operations'),
(5, 2, 'Manu Sreenivasan', 'Rigging Specialist', '+91 98700 11021', 'Rope Rescue');

-- Seed Rescue Assignments
INSERT INTO rescue_assignments (id, request_id, team_id, assigned_by_user_id, start_time, expected_completion, actual_completion, status, progress_notes) VALUES
(1, 2, 1, 2, '2026-10-14 10:00:00', '2026-10-14 14:00:00', NULL, 'IN_PROGRESS', 'Zodiac Unit 2 deployed toward Attamala Estate with infant medical kit.'),
(2, 4, 2, 2, '2026-10-09 08:30:00', '2026-10-09 12:00:00', '2026-10-09 11:45:00', 'COMPLETED', 'Both residents evacuated safely to the Aluva Relief Camp.');

-- Seed Shelters (Kerala)
INSERT INTO shelters (id, shelter_identifier, name, address, location_description, contact_person, contact_number, total_capacity, current_occupancy, status) VALUES
(1, 'SHL-001', 'Government Higher Secondary School Relief Camp, Aluva', 'Kalady Road, Aluva, Ernakulam District', 'Spacious school auditorium with kitchen, sanitation block, and generator backup', 'Anjali Menon', '+91 98765 43213', 250, 85, 'AVAILABLE'),
(2, 'SHL-002', 'Champakulam Community Hall', 'Champakulam, Kuttanad, Alappuzha District', 'Elevated civic center equipped with clean drinking water purification unit', 'Arun Kurup', '+91 98765 43214', 120, 110, 'NEARLY_FULL'),
(3, 'SHL-003', 'Meppadi Municipal Gymnasium', 'Meppadi Town, Wayanad District', 'Multi-purpose indoor sports complex with bedding', 'Dinesh Nair', '+91 98765 43230', 80, 20, 'AVAILABLE');

-- Seed Shelter Residents
INSERT INTO shelter_residents (id, shelter_id, victim_id, disaster_id, resident_name, family_size, check_in_time, check_out_time, status, registered_by_user_id) VALUES
(1, 2, 3, 2, 'Rajeevan P. M.', 5, '2026-10-13 14:00:00', NULL, 'ACTIVE', 5),
(2, 3, 4, 3, 'Sarojini Amma', 2, '2026-10-09 12:30:00', '2026-10-12 09:00:00', 'CHECKED_OUT', 4);

-- Seed Shelter Allocations
INSERT INTO shelter_allocations (id, shelter_id, disaster_id, requested_people_count, status, recommendation_reason, decided_by_user_id) VALUES
(1, 1, 1, 40, 'CONFIRMED', 'Ample remaining capacity (165 spaces) and direct road accessibility from the Wayanad relief corridor.', 4),
(2, 2, 2, 10, 'CONFIRMED', 'Immediate proximity to the Kuttanad backwater evacuation route.', 5);

-- Seed Volunteers (Kerala)
INSERT INTO volunteers (id, volunteer_identifier, user_id, full_name, email, phone, operating_area, skills, experience_years, availability_status, approval_status, emergency_contact) VALUES
(1, 'VOL-001', 6, 'Deepak Menon', 'vol.deepak@resqhub.org', '+91 98765 43215', 'Kochi & Aluva, Ernakulam District', 'First Aid, Logistics, Food Distribution', 3, 'ASSIGNED', 'APPROVED', 'Sita Menon (+91 98765 43299)'),
(2, 'VOL-002', 7, 'Priya Nambiar', 'vol.priya@resqhub.org', '+91 98765 43216', 'Kuttanad & Alappuzha District', 'Psychological Support, Child Care, Shelter Support', 2, 'AVAILABLE', 'APPROVED', 'Manoj Nambiar (+91 98765 43298)'),
(3, 'VOL-003', NULL, 'Kurian Joseph', 'kurian.joseph@example.com', '+91 98765 43290', 'Wayanad Highlands', 'Ham Radio, Driver (Heavy Vehicle)', 4, 'AVAILABLE', 'PENDING', 'Ashok Joseph (+91 98765 43291)');

-- Seed Volunteer Assignments
INSERT INTO volunteer_assignments (id, volunteer_id, disaster_id, task_title, description, location, priority, start_time, expected_completion, status, progress_notes, assigned_by_user_id) VALUES
(1, 1, 1, 'Coordinate Food Distribution at Aluva Relief Camp', 'Assist camp manager in receiving and handing out 250 meal packets to sheltered families.', 'Government HSS Relief Camp, Aluva (SHL-001)', 'HIGH', '2026-10-14 09:00:00', '2026-10-14 17:00:00', 'IN_PROGRESS', 'Morning batch distribution finished; afternoon intake ongoing.', 1),
(2, 2, 2, 'Help Register Incoming Residents', 'Maintain register of newly arrived flood-affected families and allocate bed numbers.', 'Champakulam Community Hall (SHL-002)', 'MEDIUM', '2026-10-14 13:00:00', '2026-10-14 19:00:00', 'ACCEPTED', NULL, 1);

-- Seed Inventory Items
INSERT INTO inventory_items (id, item_name, category, unit, description, quantity_available, minimum_stock_level, storage_location) VALUES
(1, 'Ready-to-Eat Emergency Meal Kits', 'FOOD', 'BOXES', 'Nutritious sealed meal packets ready for instant consumption', 650, 150, 'Warehouse Bay 1 - North'),
(2, 'Purified Bottled Drinking Water (20L Cans)', 'DRINKING_WATER', 'LITERS', 'Potable certified spring water cans', 1200, 300, 'Warehouse Bay 2 - Water Depot'),
(3, 'Woolen Thermal Blankets', 'BLANKETS', 'PIECES', 'High warmth sanitized relief blankets', 420, 100, 'Warehouse Bay 3 - Textiles'),
(4, 'Comprehensive Hygiene & Sanitation Kits', 'HYGIENE_KITS', 'SETS', 'Soap, detergent, sanitary pads, disinfectant, and toothbrushes', 280, 80, 'Warehouse Bay 4 - Medical & Hygiene'),
(5, 'Life Jackets with Whistle & Reflectors', 'RESCUE_EQUIPMENT', 'PIECES', 'Adult and child certified marine rescue buoyancy vests', 95, 30, 'Warehouse Bay 5 - Gear Locker');

-- Seed Inventory Transactions
INSERT INTO inventory_transactions (id, item_id, transaction_type, quantity, reason, performed_by_user_id) VALUES
(1, 1, 'RECEIVED', 800, 'Initial supplier batch intake from Kerala State Disaster Management Authority (KSDMA)', 1),
(2, 1, 'ISSUED', 150, 'Dispatched to Aluva Relief Camp', 1),
(3, 2, 'RECEIVED', 1500, 'Bulk water donation received and stored', 1),
(4, 2, 'ISSUED', 300, 'Emergency convoy delivery to Champakulam Community Hall', 1);

-- Seed Resource Allocations
INSERT INTO resource_allocations (id, item_id, quantity, disaster_id, shelter_id, destination_location, purpose, responsible_person, status, allocated_by_user_id, remarks) VALUES
(1, 1, 150, 1, 1, 'Government HSS Relief Camp, Aluva (SHL-001)', 'Daily sustenance for sheltered evacuees', 'Anjali Menon', 'DELIVERED', 1, 'Successfully received and verified at site kitchen.'),
(2, 2, 300, 2, 2, 'Champakulam Community Hall (SHL-002)', 'Drinking water replenishment', 'Arun Kurup', 'DELIVERED', 1, 'Stored safely in water station.'),
(3, 3, 100, 1, 1, 'Government HSS Relief Camp, Aluva (SHL-001)', 'Cold weather protection for highland evacuees', 'Anjali Menon', 'APPROVED', 1, 'Awaiting dispatch vehicle arrival.');

-- Seed Food Distributions
INSERT INTO food_distributions (id, shelter_id, location, item_id, quantity, recipient_count, distribution_date, status) VALUES
(1, 1, 'Government HSS Relief Camp Dining Wing, Aluva', 1, 120, 115, '2026-10-13', 'COMPLETED'),
(2, 2, 'Champakulam Community Hall Courtyard', 1, 80, 78, '2026-10-13', 'COMPLETED'),
(3, 1, 'Government HSS Relief Camp Dining Wing, Aluva', 1, 130, 125, '2026-10-14', 'PLANNED');

-- Seed Donations
INSERT INTO donations (id, donation_reference, donor_name, donor_email, donor_phone, donation_type, amount, item_id, item_quantity, donation_date, status, notes, verified_by_user_id, inventory_added) VALUES
(1, 'DON-2026-0001', 'Kerala Samudra Charitable Trust', 'contact@keralasamudra.org', '+91 98200 44556', 'MONETARY', 250000.00, NULL, NULL, '2026-10-11', 'VERIFIED', 'Wire transfer received for Wayanad landslide relief supply procurement.', 1, FALSE),
(2, 'DON-2026-0002', 'Cochin Shipyard Relief Fund', 'relief@cochinshipyard.com', '+91 98200 77889', 'MATERIAL', NULL, 3, 200, '2026-10-12', 'RECEIVED', 'Donation of 200 thermal blankets received at warehouse.', 1, TRUE),
(3, 'DON-2026-0003', 'Dr. Sanjay Menon', 'sanjay.menon@clinic.in', '+91 98200 99112', 'MATERIAL', NULL, 4, 100, '2026-10-14', 'SUBMITTED', 'Pledged 100 hygiene kits to be dropped at the Ernakulam logistics hub.', NULL, FALSE);

-- Seed Notifications
INSERT INTO notifications (id, user_id, target_role, title, message, link_url, notification_type, is_read) VALUES
(1, 1, 'ADMIN', 'New Critical Rescue Request Reported', 'Request REQ-2026-0001 from Mundakkai, Wayanad requires immediate team assignment.', '/rescue-requests/1', 'RESCUE', FALSE),
(2, 2, 'RESCUE_OFFICER', 'Rescue Team Deployed', 'NDRF 4th Battalion is currently en route for Operation Attamala Estate.', '/rescue-requests/2', 'RESCUE', FALSE),
(3, 4, 'CAMP_MANAGER', 'Shelter Occupancy Warning', 'Champakulam Community Hall has reached 91% capacity threshold.', '/shelters/2', 'SHELTER', FALSE),
(4, 6, 'VOLUNTEER', 'Task Assigned: Food Distribution', 'You have been assigned to coordinate meal packet delivery at the Aluva Relief Camp.', '/volunteers/assignments', 'VOLUNTEER', TRUE);

-- Seed Activity Logs
INSERT INTO activity_logs (id, user_id, action, module, affected_record_id, description) VALUES
(1, 1, 'CREATE_DISASTER', 'DISASTER', '1', 'Created disaster record: Meppadi & Mundakkai Landslide Disaster'),
(2, 1, 'CREATE_DISASTER', 'DISASTER', '2', 'Created disaster record: Kuttanad Backwater Flood'),
(3, 8, 'SUBMIT_REQUEST', 'RESCUE', 'REQ-2026-0001', 'Citizen Rahul Nair submitted rescue request for 4 trapped individuals'),
(4, 2, 'ASSIGN_TEAM', 'RESCUE', 'REQ-2026-0002', 'Rescue Officer Kavita Menon assigned NDRF 4th Battalion to request REQ-2026-0002'),
(5, 1, 'RECEIVE_INVENTORY', 'INVENTORY', 'ITEM-1', 'Added 800 boxes of Ready-to-Eat Emergency Meal Kits to warehouse'),
(6, 1, 'VERIFY_DONATION', 'DONATION', 'DON-2026-0001', 'Verified monetary donation of INR 250,000 from Kerala Samudra Charitable Trust');
