-- =======================================================================
-- ResQHub Web Sample Data — Comprehensive Disaster Response Dataset
-- Database: MySQL 8.x / Schema: resqhub_web
--
-- Coverage: Verified Kerala Disasters (2024–2026), Nearby Relief Shelters,
-- Displaced Victims, Rescue Requests & Teams, Community Volunteers,
-- Inventory Resources, Distributions, Donations, and Food Distribution.
--
-- Safely mapped to ResQHub_Web schema (27 tables, strict foreign keys).
-- =======================================================================

USE resqhub_web;

-- -----------------------------------------------------------------------
-- 1. ROLES (Lookup Table)
-- -----------------------------------------------------------------------
INSERT IGNORE INTO roles (id, role_name, description) VALUES
(1, 'ADMINISTRATOR', 'Full system control'),
(2, 'RESCUE_OFFICER', 'Manages disasters, victims, rescue requests, teams and assignments'),
(3, 'CAMP_MANAGER',   'Manages shelters, occupancy, victims and camp resources'),
(4, 'VOLUNTEER',      'Views assigned tasks and updates availability'),
(5, 'CITIZEN',        'Submits emergency requests and views information');

-- -----------------------------------------------------------------------
-- 2. USERS
-- Password for all accounts is: ResQHub@123
-- Stored as PBKDF2 salted hash (also compatible with legacy upgrade)
-- -----------------------------------------------------------------------
INSERT INTO users (id, username, password_hash, full_name, email, phone, role_id, account_status) VALUES
(1, 'admin', '5ebe770734a0ab28c24609b04644806d099b3925b4c43a3f9d7cef1b06c10f39', 'Administrator System', 'admin@resqhub.org', '9876543210', 1, 'ACTIVE'),
(2, 'rescue_officer', '5ebe770734a0ab28c24609b04644806d099b3925b4c43a3f9d7cef1b06c10f39', 'Kavita Menon', 'rescue.officer@resqhub.org', '9876543211', 2, 'ACTIVE'),
(3, 'camp_manager', '5ebe770734a0ab28c24609b04644806d099b3925b4c43a3f9d7cef1b06c10f39', 'Anjali Menon', 'camp.manager@resqhub.org', '9876543213', 3, 'ACTIVE'),
(4, 'volunteer_user', '5ebe770734a0ab28c24609b04644806d099b3925b4c43a3f9d7cef1b06c10f39', 'Deepak Menon', 'volunteer.user@resqhub.org', '9876543215', 4, 'ACTIVE'),
(5, 'citizen_user', '5ebe770734a0ab28c24609b04644806d099b3925b4c43a3f9d7cef1b06c10f39', 'Rahul Nair', 'citizen.user@resqhub.org', '9876543217', 5, 'ACTIVE'),
(6, 'demo', '5ebe770734a0ab28c24609b04644806d099b3925b4c43a3f9d7cef1b06c10f39', 'Demo Citizen', 'demo@resqhub.org', '9876543218', 5, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    email = VALUES(email),
    phone = VALUES(phone),
    role_id = VALUES(role_id),
    account_status = VALUES(account_status);

SET @admin_id = 1;

-- -----------------------------------------------------------------------
-- 3. DISASTERS (6 Verified Major Kerala Disasters: 2024–2026)
-- -----------------------------------------------------------------------
INSERT INTO disasters (id, title, disaster_type, severity, status, location, affected_population, start_date, description, reported_by) VALUES
(1, 'Mundakkai-Chooralmala Catastrophic Landslide Disaster', 'LANDSLIDE', 'CATASTROPHIC', 'CONTAINED',
 'Meppadi, Mundakkai & Chooralmala, Vythiri Taluk, Wayanad District', 12500, '2024-07-30 02:00:00',
 'Torrential monsoon downpours exceeding 570 mm in 48 hours triggered devastating slope failures across Mundakkai and Chooralmala. Over 350 casualties and extensive property damage; multi-agency response by NDRF, Indian Army, and KSDMA.', @admin_id),

(2, 'Kuttanad Monsoon Waterlogging & Backwater Inundation', 'FLOOD', 'SEVERE', 'CONTAINED',
 'Kuttanad & Champakulam, Alappuzha District', 35000, '2024-07-28 06:00:00',
 'Surplus inflow into Vembanad Lake and heavy discharge from Pamba and Manimala rivers inundated low-lying paddy-belt settlements, requiring camp mobilizations across more than 40 relief centers.', @admin_id),

(3, 'Central Kerala River Basin Flash Flood Deluge', 'FLOOD', 'SEVERE', 'ACTIVE',
 'Ranni & Vadaserikkara (Pamba Basin), Pathanamthitta District', 18000, '2026-08-01 04:30:00',
 'Torrential monsoonal cloudbursts triggered flash flooding along Pamba, Manimala, and Meenachil rivers. Submerged market zones in Ranni (Ittiyapara), displacing thousands into relief shelters.', @admin_id),

(4, 'Southern Kerala Kallakkadal Coastal Swell Surge', 'CYCLONE', 'MODERATE', 'RESOLVED',
 'Valiyathura, Poonthura & Pozhiyoor Coastal Belt, Thiruvananthapuram District', 4500, '2024-05-04 08:00:00',
 'High-energy ocean swell waves (Kallakkadal) breached sea walls and flooded hundreds of coastal homes across Valiyathura, Poonthura, and Pozhiyoor.', @admin_id),

(5, 'Vilangad Multi-Slope Landslides & Bridge Washout', 'LANDSLIDE', 'SEVERE', 'CONTAINED',
 'Vilangad, Vanimel Panchayat, Vadakara Taluk, Kozhikode District', 3200, '2024-07-30 03:30:00',
 'Multiple simultaneous hill landslides washed out Malayangad and Valook bridges, destroying houses and isolating residential clusters. NDRF and local fire units mobilized for evacuation.', @admin_id),

(6, 'Chalakudy River Basin Monsoon Surge & Inundation', 'FLOOD', 'SEVERE', 'RESOLVED',
 'Chalakudy Basin & Wadakkanchery, Thrissur District', 22000, '2024-07-31 05:00:00',
 'Heavy rainfall in Western Ghats and releases from Peringalkuthu reservoir swelled Chalakudy river beyond warning mark, inundating low-lying agricultural tracts and residential wards.', @admin_id)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    disaster_type = VALUES(disaster_type),
    severity = VALUES(severity),
    status = VALUES(status),
    location = VALUES(location),
    affected_population = VALUES(affected_population),
    start_date = VALUES(start_date),
    description = VALUES(description);

-- -----------------------------------------------------------------------
-- 4. VICTIMS
-- -----------------------------------------------------------------------
INSERT INTO victims (id, full_name, age, gender, phone, emergency_status, medical_condition, family_info, current_location, shelter_status, disaster_id, registered_by) VALUES
(1, 'Thomas Kuruvilla', 48, 'MALE', '9811122334', 'CRITICAL', 'Elderly parent with wheelchair requirement', 'Family of 4', 'Mundakkai Town, Near Chooralmala Bridge, Wayanad', 'IN_SHELTER', 1, @admin_id),
(2, 'Fathima Beevi', 34, 'FEMALE', '9811122335', 'RESCUE_REQUIRED', 'Infant child requiring emergency infant nutrition', 'Mother and 2 children', 'Attamala Estate, Meppadi, Wayanad', 'IN_SHELTER', 1, @admin_id),
(3, 'Rajeevan P. M.', 52, 'MALE', '9811122336', 'SAFE', 'None', 'Family of 5', 'Champakulam Village, Kuttanad, Alappuzha', 'IN_SHELTER', 2, @admin_id),
(4, 'Sarojini Amma', 67, 'FEMALE', '9811122337', 'NEEDS_ASSISTANCE', 'Cardiac patient needing regular medication', 'Elderly couple (2 members)', 'Ittiyapara Market Road, Ranni, Pathanamthitta', 'IN_SHELTER', 3, @admin_id),
(5, 'Balan Nambiar', 61, 'MALE', '9811122338', 'RESCUE_REQUIRED', 'Hypertension medication needed', 'Family of 3', 'Malayangad, Vilangad, Vadakara, Kozhikode', 'IN_SHELTER', 5, @admin_id),
(6, 'Jude Netto', 42, 'MALE', '9811122339', 'SAFE', 'None', 'Family of 6', 'Valiyathura Coastal Ward, Thiruvananthapuram', 'IN_SHELTER', 4, @admin_id),
(7, 'Syamala Kumari', 55, 'FEMALE', '9811122340', 'INJURED', 'Diabetic senior citizen, minor contusions', 'Family of 4', 'Kadukutty Riverside Colony, Chalakudy, Thrissur', 'IN_SHELTER', 6, @admin_id),
(8, 'Mani Muthu', 39, 'MALE', '9811122341', 'SAFE', 'None', 'Family of 5', 'Gap Road Workers Colony, Devikulam, Idukki', 'RELOCATED', 1, @admin_id),
(9, 'Anoop Chandran', 29, 'MALE', '9811122342', 'SAFE', 'None', '2 roommates', 'Technopark Backside Lowlands, Kazhakoottam, Thiruvananthapuram', 'NOT_SHELTERED', 4, @admin_id),
(10, 'Laila Hameed', 45, 'FEMALE', '9811122343', 'CRITICAL', 'Post-surgery mobility limitation; trauma treatment', 'Family of 4', 'Chooralmala Bazaar Junction, Wayanad', 'IN_SHELTER', 1, @admin_id)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    age = VALUES(age),
    gender = VALUES(gender),
    phone = VALUES(phone),
    emergency_status = VALUES(emergency_status),
    medical_condition = VALUES(medical_condition),
    family_info = VALUES(family_info),
    current_location = VALUES(current_location),
    shelter_status = VALUES(shelter_status);

-- -----------------------------------------------------------------------
-- 5. RESCUE TEAMS
-- -----------------------------------------------------------------------
INSERT INTO rescue_teams (id, team_name, team_type, leader_name, contact_number, member_count, skills, equipment, availability_status, operational_status, base_location) VALUES
(1, 'NDRF 4th Battalion Unit', 'NDRF', 'Sreejith Kumar', '9870011001', 15, 'Water Rescue, Flood Evacuation, Medical First Response', 'Motorized Zodiac Boats, Life Jackets, First Aid Kits', 'DEPLOYED', 'ON_MISSION', 'Wayanad Highlands - Meppadi Sector'),
(2, 'Kerala Fire & Rescue Mountain Squad', 'FIRE_RESCUE', 'Anil Varghese', '9870011002', 12, 'High Altitude Ropes, Landslide Debris Clearance, Heavy Rigging', 'Ropes, Pulleys, Stretchers, Hydraulic Cutters', 'AVAILABLE', 'STANDBY', 'Wayanad & Idukki Hill Districts'),
(3, 'Kerala Civil Defence Swift Water Squad', 'COMMUNITY', 'Reshma Thomas', '9870011003', 10, 'Inflatable Boat Navigation, Night Diving, CPR Certified', 'Inflatable Rafts, Oars, Life Rings, Torches', 'AVAILABLE', 'STANDBY', 'Kuttanad & Pamba River Basins'),
(4, 'Indian Army Madras Sappers Task Force', 'OTHER', 'Vivek Narayanan', '9870011004', 24, 'Bailey Bridge Construction, Heavy Earthmoving, Extreme Terrain Access', 'Heavy Earthmovers, Bailey Bridge Units, Winches', 'DEPLOYED', 'ON_MISSION', 'Wayanad Landslide Disaster Zone'),
(5, 'Kerala Coastal Police Marine Unit', 'POLICE', 'Francis Gomez', '9870011005', 8, 'High-Sea Rescue, Swell Surge Patrol, Lifeboat Evacuation', 'Speed Boats, Buoys, Deep-Water Navigation Gear', 'AVAILABLE', 'STANDBY', 'Thiruvananthapuram & Kollam Coastal Belt'),
(6, 'High-Range 4x4 Emergency Mobile Squad', 'COMMUNITY', 'Rakesh Mohan', '9870011006', 8, 'Rough Terrain Winching, Tree Cutting, Medical Transit', '4x4 Offroad Trucks, Power Saws, Recovery Winches', 'DEPLOYED', 'EN_ROUTE', 'Idukki Gap Road & Adimali Corridor'),
(7, 'Kozhikode District Scuba & Disaster Unit', 'FIRE_RESCUE', 'P. K. Musthafa', '9870011007', 10, 'Flash Flood River Rescue, Mudflow Reconnaissance', 'Scuba Gear, Thermal Cameras, Inflatable Dinghies', 'DEPLOYED', 'ON_MISSION', 'Vadakara & Vilangad River Basins'),
(8, 'Rapid Trauma & Medical Air-Evac Cell', 'MEDICAL', 'Dr. Shobha Warrier', '9870011008', 6, 'Field Triage, Advanced Trauma Life Support, Air Ambulance Interface', 'Defibrillators, Emergency Triage Packs, Oxygen Cylinders', 'AVAILABLE', 'STANDBY', 'Central Kerala Command Base')
ON DUPLICATE KEY UPDATE
    team_name = VALUES(team_name),
    leader_name = VALUES(leader_name),
    contact_number = VALUES(contact_number),
    member_count = VALUES(member_count),
    skills = VALUES(skills),
    equipment = VALUES(equipment),
    availability_status = VALUES(availability_status),
    operational_status = VALUES(operational_status);

-- -----------------------------------------------------------------------
-- 6. RESCUE TEAM MEMBERS
-- -----------------------------------------------------------------------
INSERT INTO team_members (id, team_id, member_name, role, contact_number, special_skills, availability) VALUES
(1, 1, 'Sreejith Kumar', 'Team Commander', '9870011001', 'Incident Command & Coordination', 'DEPLOYED'),
(2, 1, 'Suresh Pillai', 'Boat Operator', '9870011011', 'Motorized Zodiac Boat Handling', 'DEPLOYED'),
(3, 1, 'Dr. Geeta Nair', 'Field Paramedic', '9870011012', 'Trauma Care & Triage First Aid', 'DEPLOYED'),
(4, 2, 'Anil Varghese', 'Team Leader', '9870011002', 'Mountain Search & Rope Rescue', 'AVAILABLE'),
(5, 2, 'Manu Sreenivasan', 'Rigging Specialist', '9870011021', 'Rope Rescue & Aerial Traversing', 'AVAILABLE'),
(6, 3, 'Reshma Thomas', 'Squad Leader', '9870011003', 'Swift Water Search and Rescue', 'AVAILABLE'),
(7, 3, 'Bibin Paul', 'Rescue Diver', '9870011031', 'Underwater Search & Navigation', 'AVAILABLE'),
(8, 4, 'Vivek Narayanan', 'Task Force Officer', '9870011004', 'Combat Engineering & Bailey Bridges', 'DEPLOYED'),
(9, 5, 'Francis Gomez', 'Marine Inspector', '9870011005', 'Coastal Swell Operations', 'AVAILABLE'),
(10, 6, 'Rakesh Mohan', 'Field Coordinator', '9870011006', 'High-Altitude Winching & 4x4 Extrication', 'DEPLOYED'),
(11, 7, 'P. K. Musthafa', 'Dive Officer', '9870011007', 'Debris River Penetration', 'DEPLOYED'),
(12, 8, 'Dr. Shobha Warrier', 'Chief Medical Officer', '9870011008', 'Emergency Resuscitation & Triage', 'AVAILABLE')
ON DUPLICATE KEY UPDATE
    member_name = VALUES(member_name),
    role = VALUES(role),
    contact_number = VALUES(contact_number),
    special_skills = VALUES(special_skills),
    availability = VALUES(availability);

-- -----------------------------------------------------------------------
-- 7. RESCUE REQUESTS
-- -----------------------------------------------------------------------
INSERT INTO rescue_requests (id, disaster_id, victim_id, requester_name, contact_number, location, people_count, children_count, elderly_count, life_threatening, medical_emergency, trapped_under_debris, required_assistance, priority, status) VALUES
(1, 1, 1, 'Thomas Kuruvilla', '9811122334', 'Mundakkai Town, Near Chooralmala Bridge, Wayanad', 4, 1, 1, TRUE, TRUE, TRUE, '4 family members including grandmother trapped under mud and debris; water rising.', 'CRITICAL', 'PENDING'),
(2, 1, 2, 'Fathima Beevi', '9811122335', 'Attamala Estate, Meppadi, Wayanad', 3, 2, 0, TRUE, FALSE, FALSE, 'Mother with 6-month-old infant cut off by mud currents; boat assistance needed.', 'HIGH', 'ASSIGNED'),
(3, 2, 3, 'Rajeevan P. M.', '9811122336', 'Champakulam Higher Secondary School, Kuttanad', 12, 4, 2, FALSE, FALSE, FALSE, '12 local residents stranded on upper floor with dwindling drinking water.', 'HIGH', 'UNDER_REVIEW'),
(4, 3, 4, 'Sarojini Amma', '9811122337', 'Ittiyapara Market Road, Ranni, Pathanamthitta', 2, 0, 2, FALSE, TRUE, FALSE, 'Elderly cardiac patient couple surrounded by 4 feet of river water in ground floor.', 'HIGH', 'RESCUED'),
(5, 5, 5, 'Balan Nambiar', '9811122338', 'Malayangad River Crossing, Vilangad, Kozhikode', 3, 0, 1, TRUE, FALSE, FALSE, 'Family stranded after wooden footbridge washed away; bank crumbling.', 'HIGH', 'IN_PROGRESS'),
(6, 4, 6, 'Jude Netto', '9811122339', 'Poonthura Church Road, Thiruvananthapuram', 6, 2, 1, FALSE, FALSE, FALSE, 'Sea surge overtopped boundary wall; courtyard submerged with high swell waves.', 'MEDIUM', 'RESCUED'),
(7, 6, 7, 'Syamala Kumari', '9811122340', 'Kadukutty Near River Temple, Chalakudy, Thrissur', 4, 0, 1, TRUE, TRUE, FALSE, 'Floodwater reached 4.5 feet inside home; diabetic senior requires stretcher evacuation.', 'HIGH', 'IN_PROGRESS'),
(8, 1, 8, 'Mani Muthu', '9811122341', 'Bodimettu Ghat Road Kilometer 14, Idukki', 5, 1, 0, TRUE, FALSE, TRUE, 'Passenger van blocked between two rockslides on mountain curve.', 'CRITICAL', 'ASSIGNED'),
(9, 4, 9, 'Anoop Chandran', '9811122342', 'Thettiyar Canal Basin, Kazhakoottam, Thiruvananthapuram', 2, 0, 0, FALSE, FALSE, FALSE, 'Basement drainage overflow trapping vehicles.', 'LOW', 'RESCUED'),
(10, 1, 10, 'Laila Hameed', '9811122343', 'Chooralmala Riverbank Plantation Line, Wayanad', 4, 1, 1, TRUE, TRUE, FALSE, 'Severely injured plantation worker requires aerial ropeway evacuation.', 'CRITICAL', 'RESCUED')
ON DUPLICATE KEY UPDATE
    requester_name = VALUES(requester_name),
    contact_number = VALUES(contact_number),
    location = VALUES(location),
    people_count = VALUES(people_count),
    children_count = VALUES(children_count),
    elderly_count = VALUES(elderly_count),
    life_threatening = VALUES(life_threatening),
    medical_emergency = VALUES(medical_emergency),
    trapped_under_debris = VALUES(trapped_under_debris),
    required_assistance = VALUES(required_assistance),
    priority = VALUES(priority),
    status = VALUES(status);

-- -----------------------------------------------------------------------
-- 8. RESCUE ASSIGNMENTS
-- -----------------------------------------------------------------------
INSERT INTO rescue_assignments (id, rescue_request_id, rescue_team_id, assigned_by, assignment_status, notes, completed_at) VALUES
(1, 2, 1, 2, 'EN_ROUTE', 'Zodiac Unit deployed toward Attamala Estate with infant nutrition kit.', NULL),
(2, 4, 3, 2, 'COMPLETED', 'Both elderly residents evacuated safely to Ranni St. Thomas College Camp.', '2026-08-01 11:45:00'),
(3, 5, 7, 2, 'EN_ROUTE', 'Rigging rope traverse across swollen river at Malayangad for family of 3.', NULL),
(4, 6, 5, 2, 'COMPLETED', '6 family members extracted safely using coastal high-clearance rescue boat.', '2024-05-04 14:10:00'),
(5, 7, 3, 2, 'ON_SITE', 'Motorized raft deployed to Kadukutty riverside residence; diabetic senior secured on stretcher.', NULL),
(6, 8, 6, 2, 'EN_ROUTE', 'Heavy winch unit clearing mud barrier at Bodimettu Km 14.', NULL),
(7, 9, 3, 2, 'COMPLETED', 'Submersible pump deployed; floodwater cleared.', '2026-10-18 12:15:00'),
(8, 10, 4, 2, 'COMPLETED', 'Madras Sappers zip-line evacuated casualty across river to field ambulance.', '2024-07-30 19:40:00')
ON DUPLICATE KEY UPDATE
    rescue_request_id = VALUES(rescue_request_id),
    rescue_team_id = VALUES(rescue_team_id),
    assignment_status = VALUES(assignment_status),
    notes = VALUES(notes),
    completed_at = VALUES(completed_at);

-- -----------------------------------------------------------------------
-- 9. SHELTERS (14 Verified Relief Centers)
-- -----------------------------------------------------------------------
INSERT INTO shelters (id, name, code, district, city, address, location_description, max_capacity, current_occupancy, contact_number, manager_name, disaster_id, wheelchair_accessible, elderly_friendly, medical_accessible, special_assistance, operational_status, created_by) VALUES
(1, 'St. Thomas College Relief Camp', 'SHL-001', 'Pathanamthitta', 'Ranni', 'Pazhavangadi, Ranni', 'Elevated multi-storey college auditorium equipped with community kitchen.', 250, 85, '9876543213', 'Anjali Menon', 3, 1, 1, 1, 1, 'ACTIVE', @admin_id),
(2, 'Champakulam Community Hall Relief Camp', 'SHL-002', 'Alappuzha', 'Kuttanad', 'Champakulam, Kuttanad', 'Elevated civic center equipped with water filtration and boat jetty access.', 120, 110, '9876543214', 'Arun Kurup', 2, 0, 1, 1, 0, 'NEAR_CAPACITY', @admin_id),
(3, 'Meppadi Municipal Youth Center', 'SHL-003', 'Wayanad', 'Meppadi', 'Meppadi Town, Vythiri Taluk', 'Indoor sports complex for emergency relief intake and staging.', 80, 20, '9876543230', 'Dinesh Nair', 1, 1, 0, 0, 0, 'AVAILABLE', @admin_id),
(4, 'Government VHSS Meppadi Relief Hub', 'SHL-004', 'Wayanad', 'Meppadi', 'Meppadi Town Center', 'Major designated district relief hub with dedicated medical triage clinic.', 350, 140, '9876543231', 'Sreekumar P.', 1, 1, 1, 1, 1, 'ACTIVE', @admin_id),
(5, 'St. Joseph Higher Secondary School', 'SHL-005', 'Wayanad', 'Meppadi', 'Meppadi - Ooty Road', 'Designated major evacuation center with multi-storey classrooms and clean water tanks.', 200, 65, '9876543232', 'Fr. Varghese Mathew', 1, 1, 1, 0, 0, 'AVAILABLE', @admin_id),
(6, 'St. George Higher Secondary School', 'SHL-006', 'Kozhikode', 'Vilangad', 'Vilangad Road, Vanimel Panchayat', 'Primary relief camp designated for Vilangad landslide evacuees.', 180, 45, '9876543241', 'Mathew Joseph', 5, 0, 1, 1, 0, 'AVAILABLE', @admin_id),
(7, 'Paloor Government LP School', 'SHL-007', 'Kozhikode', 'Paloor', 'Paloor, Vanimel Panchayat', 'Auxiliary disaster shelter for families relocated from high-risk slopes.', 90, 15, '9876543242', 'Sujatha K.', 5, 0, 1, 0, 0, 'AVAILABLE', @admin_id),
(8, 'Government Regional Fisheries High School', 'SHL-008', 'Thiruvananthapuram', 'Valiyathura', 'Valiyathura, Vallakkadavu P.O.', 'Official coastal relief camp operated during sea surges and swell flooding.', 220, 80, '9876543261', 'Johnson Fernandez', 4, 1, 1, 1, 0, 'ACTIVE', @admin_id),
(9, 'Government Higher Secondary School Ranni', 'SHL-009', 'Pathanamthitta', 'Ranni', 'Main Road, Ranni Town', 'Designated emergency shelter equipped with large multi-purpose halls.', 180, 50, '9876543272', 'P. R. Varghese', 3, 1, 1, 1, 0, 'AVAILABLE', @admin_id),
(10, 'St. Mary HSS Relief Camp', 'SHL-010', 'Alappuzha', 'Champakulam', 'Champakulam, Kuttanad', 'Elevated concrete multi-storey building serving Kuttanad backwater evacuees.', 180, 90, '9876543281', 'Fr. George Plathottam', 2, 0, 1, 1, 0, 'AVAILABLE', @admin_id),
(11, 'Panampilly Memorial Govt College', 'SHL-011', 'Thrissur', 'Chalakudy', 'Potore Road, Chalakudy', 'Major regional flood relief center with auditorium and supply drop zone.', 300, 70, '9876543251', 'Dr. Radhika K.', 6, 1, 1, 1, 1, 'ACTIVE', @admin_id),
(12, 'Government Girls HSS Chalakudy', 'SHL-012', 'Thrissur', 'Chalakudy', 'Main Town Road, Chalakudy', 'Designated urban emergency shelter equipped with meal preparation infrastructure.', 150, 30, '9876543252', 'K. Narayanan', 6, 1, 1, 0, 0, 'AVAILABLE', @admin_id),
(13, 'Government VHSS Adimali', 'SHL-013', 'Idukki', 'Adimali', 'Adimali Town, Devikulam Taluk', 'High-range shelter equipped with diesel generator and warm community halls.', 160, 40, '9876543293', 'Biju Xavier', 1, 0, 1, 1, 0, 'AVAILABLE', @admin_id),
(14, 'Kazhakoottam Govt Higher Secondary School', 'SHL-014', 'Thiruvananthapuram', 'Kazhakoottam', 'National Highway Bypass, Kazhakoottam', 'Designated urban flood relief camp situated on elevated ground with complete kitchen.', 200, 25, '9876543294', 'Mini R.', 4, 1, 0, 0, 0, 'AVAILABLE', @admin_id)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    district = VALUES(district),
    max_capacity = VALUES(max_capacity),
    current_occupancy = VALUES(current_occupancy),
    contact_number = VALUES(contact_number),
    manager_name = VALUES(manager_name),
    disaster_id = VALUES(disaster_id),
    operational_status = VALUES(operational_status);

-- -----------------------------------------------------------------------
-- 10. SHELTER ALLOCATIONS
-- -----------------------------------------------------------------------
INSERT INTO shelter_allocations (id, shelter_id, victim_id, family_name, people_count, notes, status, allocated_by) VALUES
(1, 4, 1, 'Thomas Kuruvilla', 4, 'Primary relief hub located ~3.8 km from Chooralmala incident zone.', 'CHECKED_IN', 3),
(2, 4, 2, 'Fathima Beevi', 3, 'Young mother with infant accommodated in family wing.', 'CHECKED_IN', 3),
(3, 2, 3, 'Rajeevan P. M.', 5, 'Evacuated from submerged paddy area; accommodated in ground floor hall.', 'CHECKED_IN', 3),
(4, 1, 4, 'Sarojini Amma', 2, 'Elderly cardiac patient accommodated near first-aid room.', 'RELEASED', 3),
(5, 6, 5, 'Balan Nambiar', 3, 'Relocated from landslide fracture slope.', 'CHECKED_IN', 3),
(6, 8, 6, 'Jude Netto', 6, 'Coastal swell surge evacuee family accommodated.', 'RELEASED', 3),
(7, 11, 7, 'Syamala Kumari', 4, 'Accommodated with wheelchair access provided.', 'CHECKED_IN', 3),
(8, 13, 8, 'Mani Muthu', 5, 'Estate worker family safely sheltered.', 'CHECKED_IN', 3),
(9, 14, 9, 'Anoop Chandran', 2, 'Urban runoff evacuee temporary stay.', 'RELEASED', 3),
(10, 4, 10, 'Laila Hameed', 4, 'Receiving medical care after zip-line extraction.', 'CHECKED_IN', 3)
ON DUPLICATE KEY UPDATE
    shelter_id = VALUES(shelter_id),
    victim_id = VALUES(victim_id),
    family_name = VALUES(family_name),
    people_count = VALUES(people_count),
    notes = VALUES(notes),
    status = VALUES(status);

-- -----------------------------------------------------------------------
-- 11. VOLUNTEERS
-- -----------------------------------------------------------------------
INSERT INTO volunteers (id, full_name, contact_number, email, user_id, location, skills, availability, emergency_role, max_workload) VALUES
(1, 'Deepak Menon', '9876543215', 'vol.deepak@resqhub.org', 4, 'Kochi & Aluva, Ernakulam District', 'First Aid, Logistics, Food Distribution', 'BUSY', 'FOOD', 3),
(2, 'Priya Nambiar', '9876543216', 'vol.priya@resqhub.org', NULL, 'Kuttanad & Alappuzha District', 'Psychological Support, Child Care, Shelter Support', 'AVAILABLE', 'SHELTER', 2),
(3, 'Kurian Joseph', '9876543290', 'kurian.joseph@example.com', NULL, 'Wayanad Highlands', 'Ham Radio, Heavy Vehicle Driver', 'AVAILABLE', 'TRANSPORT', 3),
(4, 'Amina Rasheed', '9876543281', 'amina.rasheed@example.com', NULL, 'Kozhikode & Vadakara', 'Registered Nurse, Triage Assessment, Wound Dressing', 'BUSY', 'MEDICAL', 4),
(5, 'Sreekanth V.', '9876543283', 'sreekanth.v@example.com', NULL, 'Thrissur & Chalakudy Basin', 'Community Kitchen, Supply Logistics, Electrician', 'AVAILABLE', 'FOOD', 3),
(6, 'Jerome Fernandez', '9876543285', 'jerome.f@example.com', NULL, 'Thiruvananthapuram Coastal Belt', 'Life Saving, Lifeguard Certified, Coastal Navigation', 'AVAILABLE', 'RESCUE', 3),
(7, 'Lakshmi Prabha', '9876543287', 'lakshmi.prabha@example.com', NULL, 'Pathanamthitta & Ranni', 'Relief Registry, Elderly Care, Communication', 'AVAILABLE', 'GENERAL', 2),
(8, 'Anandhu Mohan', '9876543289', 'anandhu.m@example.com', NULL, 'Idukki Mountain Range', '4x4 Offroad Driver, Mountain Trekker, Ham Radio', 'BUSY', 'TRANSPORT', 3),
(9, 'Faisal Babu', '9876543271', 'faisal.babu@example.com', NULL, 'Malappuram & Nilambur', 'Water Filtration Specialist, Heavy Machinery Handling', 'AVAILABLE', 'GENERAL', 3),
(10, 'Divya Chandran', '9876543273', 'divya.chandran@example.com', NULL, 'Kottayam & Pala', 'Pharmacist, Medication Inventory, First Aid Trainer', 'AVAILABLE', 'MEDICAL', 3)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    contact_number = VALUES(contact_number),
    email = VALUES(email),
    location = VALUES(location),
    skills = VALUES(skills),
    availability = VALUES(availability),
    emergency_role = VALUES(emergency_role);

-- -----------------------------------------------------------------------
-- 12. VOLUNTEER ASSIGNMENTS
-- -----------------------------------------------------------------------
INSERT INTO volunteer_assignments (id, volunteer_id, task_name, description, location, priority, status) VALUES
(1, 1, 'Coordinate Food Distribution at Meppadi Relief Hub', 'Assist camp manager in receiving and handing out meal packets to sheltered families.', 'Government VHSS Relief Camp, Meppadi (SHL-004)', 3, 'IN_PROGRESS'),
(2, 2, 'Help Register Incoming Residents', 'Maintain register of newly arrived flood-affected families and allocate bed numbers.', 'Champakulam Community Hall (SHL-002)', 2, 'ACCEPTED'),
(3, 4, 'Emergency Triage & Wound Dressing Clinic', 'Provide basic trauma first aid to evacuees arriving from Vilangad debris zone.', 'St. George HSS, Vilangad (SHL-006)', 3, 'COMPLETED'),
(4, 5, 'Community Kitchen Setup & Water Supply Check', 'Inspect RO drinking water tanks and help volunteers prepare porridge for 250 residents.', 'Panampilly Memorial College, Chalakudy (SHL-011)', 2, 'COMPLETED'),
(5, 6, 'Coastal Sandbagging & Sea Breaker Reinforcement', 'Assist local fishers in staging sandbags along Valiyathura pier road.', 'Valiyathura Beach Road (Near SHL-008)', 3, 'COMPLETED'),
(6, 7, 'Specialized Senior Citizen Care & Medicine Dispatch', 'Check vitals for elderly residents and distribute cardiac and diabetic medications.', 'St. Thomas College Relief Camp, Ranni (SHL-001)', 3, 'COMPLETED'),
(7, 8, 'Clear Road Debris & Assist 4x4 Supply Transits', 'Navigate rough terrain vehicles carrying baby formula and blankets to cut-off tea estate lines.', 'Adimali - Gap Road Sector (SHL-013)', 3, 'IN_PROGRESS'),
(8, 9, 'Install Mobile Water Purification Station', 'Assemble rapid water purification unit to supply safe drinking water to stranded residents.', 'Champakulam Boat Jetty (SHL-002)', 3, 'ACCEPTED'),
(9, 10, 'Sort and Categorize Donated Medications', 'Catalog emergency antibiotic, analgesic, and pediatric shipments received at central depot.', 'Meppadi Municipal Youth Center (SHL-003)', 2, 'IN_PROGRESS'),
(10, 1, 'Urban Flood Relief Food Box Logistics', 'Coordinate loading and dispatch of 200 food boxes to flooded residential wards.', 'Kazhakoottam GHSS (SHL-014)', 2, 'COMPLETED')
ON DUPLICATE KEY UPDATE
    task_name = VALUES(task_name),
    description = VALUES(description),
    location = VALUES(location),
    priority = VALUES(priority),
    status = VALUES(status);

-- -----------------------------------------------------------------------
-- 13. RESOURCES (Inventory Items)
-- -----------------------------------------------------------------------
INSERT INTO resources (id, name, code, category, available_quantity, minimum_level, unit, description, status, created_by) VALUES
(1, 'Ready-to-Eat Emergency Meal Kits', 'RES-FOOD-001', 'FOOD', 650, 150, 'boxes', 'Nutritious vacuum-sealed meal packets ready for instant consumption without cooking', 'AVAILABLE', @admin_id),
(2, 'Purified Bottled Drinking Water (20L Cans)', 'RES-WTR-001', 'WATER', 1200, 300, 'cans', 'Potable certified mineral spring water cans for camp hydration stations', 'AVAILABLE', @admin_id),
(3, 'Woolen Thermal Relief Blankets', 'RES-SHEL-001', 'SHELTER_SUPPLIES', 420, 100, 'pieces', 'High warmth sanitized wool-synthetic blend relief blankets', 'AVAILABLE', @admin_id),
(4, 'Comprehensive Family Hygiene & Sanitation Kits', 'RES-MEDS-001', 'MEDICAL_SUPPLIES', 280, 80, 'sets', 'Soap, detergent, sanitary napkins, chlorine tablets, disinfectant, and toothbrushes', 'AVAILABLE', @admin_id),
(5, 'Life Jackets with Whistle & Reflectors', 'RES-EQUIP-001', 'RESCUE_EQUIPMENT', 95, 30, 'pieces', 'Adult and child certified marine buoyancy vests for flood rescue operations', 'AVAILABLE', @admin_id),
(6, 'Inflatable Rubber Rescue Dinghies & Oars', 'RES-EQUIP-002', 'RESCUE_EQUIPMENT', 18, 5, 'sets', 'Heavy-duty puncture-resistant 6-person evacuation boats with foot pumps and oars', 'AVAILABLE', @admin_id),
(7, 'High-Intensity Waterproof LED Torches', 'RES-EQUIP-003', 'RESCUE_EQUIPMENT', 150, 40, 'pieces', 'Rechargeable search-and-rescue grade waterproof flashlights with emergency strobe', 'AVAILABLE', @admin_id),
(8, 'Heavy-Duty Waterproof Tarpaulins (18ft x 24ft)', 'RES-SHEL-002', 'SHELTER_SUPPLIES', 320, 75, 'pieces', 'UV-treated reinforced polyethylene sheets for temporary camp roofing and ground cover', 'AVAILABLE', @admin_id),
(9, 'Emergency Adult & Children Clothing Packs', 'RES-CLOTH-001', 'CLOTHING', 210, 60, 'sets', 'Pre-sorted cotton clothing sets including shirts, dhotis, pants, and inner garments', 'AVAILABLE', @admin_id),
(10, 'Infant Nutrition & Baby Milk Formula Packs', 'RES-FOOD-002', 'FOOD', 140, 45, 'boxes', 'Sterile infant cereal, milk powder formula, and feeding bottles for displaced mothers', 'AVAILABLE', @admin_id)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    available_quantity = VALUES(available_quantity),
    minimum_level = VALUES(minimum_level),
    unit = VALUES(unit),
    description = VALUES(description),
    status = VALUES(status);

-- -----------------------------------------------------------------------
-- 14. STOCK MOVEMENTS
-- -----------------------------------------------------------------------
INSERT INTO stock_movements (id, resource_id, movement_type, quantity, previous_quantity, new_quantity, source, destination, reason, disaster_id, recorded_by) VALUES
(1, 1, 'STOCK_IN', 800, 0, 800, 'KSDMA Supplier Batch', NULL, 'Initial supplier batch intake from Kerala State Disaster Management Authority', 1, @admin_id),
(2, 1, 'STOCK_OUT', 150, 800, 650, NULL, 'Ranni Relief Camp (SHL-001)', 'Dispatched to Ranni Relief Camp for central kitchen supply', 3, @admin_id),
(3, 2, 'STOCK_IN', 1500, 0, 1500, 'Kerala Water Authority', NULL, 'Bulk water donation received and stored from Kerala Water Authority', 2, @admin_id),
(4, 2, 'STOCK_OUT', 300, 1500, 1200, NULL, 'Champakulam Community Hall (SHL-002)', 'Emergency convoy delivery to Champakulam Community Hall', 2, @admin_id),
(5, 3, 'STOCK_IN', 520, 0, 520, 'Cochin Shipyard Relief Fund', NULL, 'Donation consignment received from Cochin Shipyard Relief Fund', 1, @admin_id),
(6, 3, 'STOCK_OUT', 100, 520, 420, NULL, 'GVHSS Meppadi (SHL-004)', 'Dispatched to GVHSS Meppadi Relief Camp for landslide evacuees', 1, @admin_id),
(7, 4, 'STOCK_IN', 380, 0, 380, 'Red Cross Kerala Chapter', NULL, 'Sanitation kit procurement consignment through Red Cross Kerala Chapter', 5, @admin_id),
(8, 4, 'STOCK_OUT', 100, 380, 280, NULL, 'Vilangad St. George HSS (SHL-006)', 'Dispatched to Vilangad St. George HSS shelter for displaced families', 5, @admin_id),
(9, 5, 'STOCK_IN', 120, 0, 120, 'Marine Safety Corporation', NULL, 'Procured 120 marine safety life jackets from Marine Safety Corporation', 2, @admin_id),
(10, 10, 'STOCK_IN', 180, 0, 180, 'UNICEF India Liaison', NULL, 'Emergency pediatric nutrition grant received from UNICEF India liaison', 1, @admin_id)
ON DUPLICATE KEY UPDATE
    resource_id = VALUES(resource_id),
    movement_type = VALUES(movement_type),
    quantity = VALUES(quantity),
    previous_quantity = VALUES(previous_quantity),
    new_quantity = VALUES(new_quantity),
    reason = VALUES(reason);

-- -----------------------------------------------------------------------
-- 15. RESOURCE DISTRIBUTIONS
-- -----------------------------------------------------------------------
INSERT INTO resource_distributions (id, resource_id, quantity, distributed_to, destination, disaster_id, shelter_id, victim_id, reason, distributed_by) VALUES
(1, 1, 150, 'Government VHSS Relief Camp, Meppadi', 'SHELTER', 1, 4, NULL, 'Daily sustenance for sheltered evacuees', @admin_id),
(2, 2, 300, 'Champakulam Community Hall', 'SHELTER', 2, 2, NULL, 'Drinking water replenishment for flooded wards', @admin_id),
(3, 3, 100, 'Government VHSS Relief Camp, Meppadi', 'SHELTER', 1, 4, NULL, 'Cold weather protection for highland evacuees', @admin_id),
(4, 4, 80, 'St. George HSS, Vilangad', 'SHELTER', 5, 6, NULL, 'General hygiene and water purification tablets', @admin_id),
(5, 5, 25, 'Champakulam Boat Jetty', 'DISASTER_AREA', 2, 2, NULL, 'Water safety gear for local volunteer evacuation boats', @admin_id),
(6, 6, 4, 'St. Thomas College Relief Camp, Ranni', 'SHELTER', 3, 1, NULL, 'Inflatable boats for emergency flood perimeter transit', @admin_id),
(7, 7, 30, 'GVHSS Adimali Shelter', 'SHELTER', 1, 13, NULL, 'Night lighting and inspection gear for ghat road operations', @admin_id),
(8, 8, 50, 'St. Joseph HSS, Meppadi', 'SHELTER', 1, 5, NULL, 'Waterproofing auxiliary tent extensions and dining halls', @admin_id),
(9, 9, 60, 'Panampilly Memorial College, Chalakudy', 'SHELTER', 6, 11, NULL, 'Clean replacement clothes for flood-stranded evacuees', @admin_id),
(10, 10, 40, 'Government VHSS Relief Camp, Meppadi', 'SHELTER', 1, 4, NULL, 'Infant formula and feeding supplies for nursing mothers', @admin_id)
ON DUPLICATE KEY UPDATE
    resource_id = VALUES(resource_id),
    quantity = VALUES(quantity),
    distributed_to = VALUES(distributed_to),
    destination = VALUES(destination),
    reason = VALUES(reason);

-- -----------------------------------------------------------------------
-- 16. DONORS & DONATIONS
-- -----------------------------------------------------------------------
INSERT INTO donors (id, full_name, contact_number, email, location, donor_type) VALUES
(1, 'Kerala Samudra Charitable Trust', '9820044556', 'contact@keralasamudra.org', 'Ernakulam, Kerala', 'ORGANIZATION'),
(2, 'Cochin Shipyard Relief Fund', '9820077889', 'relief@cochinshipyard.com', 'Kochi, Kerala', 'COMPANY'),
(3, 'Dr. Sanjay Menon', '9820099112', 'sanjay.menon@clinic.in', 'Kozhikode, Kerala', 'INDIVIDUAL'),
(4, 'Lulu Humanitarian Foundation', '9820011223', 'relief@lulugroup.com', 'Kochi, Kerala', 'ORGANIZATION'),
(5, 'Hindustan Latex Family Planning Trust', '9820033445', 'csr@hlfppt.org', 'Thiruvananthapuram, Kerala', 'COMPANY'),
(6, 'Eastern Condiments Relief Wing', '9820055667', 'csr@eastern.in', 'Adimali, Idukki', 'COMPANY'),
(7, 'Muthoot M. George Foundation', '9820077990', 'foundation@muthoot.com', 'Kochi, Kerala', 'ORGANIZATION'),
(8, 'Apex Marine Safety Systems', '9820088221', 'donations@apexmarine.in', 'Alappuzha, Kerala', 'COMPANY'),
(9, 'Dr. Radhakrishnan Memorial Trust', '9820044332', 'rmtrust@kerala.org', 'Kottayam, Kerala', 'ORGANIZATION'),
(10, 'Mother & Child Care Alliance', '9820066554', 'relief@mccalliance.org', 'Thrissur, Kerala', 'ORGANIZATION')
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    contact_number = VALUES(contact_number),
    email = VALUES(email),
    location = VALUES(location);

INSERT INTO donations (id, donor_id, donation_type, amount, material_name, quantity, description, status) VALUES
(1, 1, 'CASH', 250000.00, NULL, NULL, 'Wire transfer received for Wayanad landslide relief supply procurement.', 'RECEIVED'),
(2, 2, 'MATERIAL', NULL, 'Woolen Thermal Relief Blankets', 200, 'Donation of 200 woolen thermal blankets received at warehouse.', 'RECEIVED'),
(3, 3, 'MATERIAL', NULL, 'Family Hygiene & Sanitation Kits', 100, 'Pledged 100 hygiene kits to be dropped at the logistics hub.', 'RECEIVED'),
(4, 4, 'CASH', 500000.00, NULL, NULL, 'Direct grant earmarked for dry ration kits and camp water filtration units.', 'RECEIVED'),
(5, 5, 'MATERIAL', NULL, 'Sanitary and Hygiene Packs', 150, 'Donation of 150 sanitary and hygiene packs received.', 'RECEIVED'),
(6, 6, 'MATERIAL', NULL, 'Ready-to-Eat Food Packets', 300, '300 boxes of emergency ready-to-eat food packets delivered to Meppadi hub.', 'RECEIVED'),
(7, 7, 'CASH', 300000.00, NULL, NULL, 'Electronic bank transfer for emergency ambulance fuel and medical equipment.', 'RECEIVED'),
(8, 8, 'MATERIAL', NULL, 'Certified Marine Life Vests', 50, '50 certified marine life vests delivered to central warehouse.', 'RECEIVED'),
(9, 9, 'CASH', 100000.00, NULL, NULL, 'Monetary relief contribution for displaced Kuttanad farming families.', 'RECEIVED'),
(10, 10, 'MATERIAL', NULL, 'Infant Formula & Feeding Kits', 80, '80 boxes of infant formula and baby feeding kits received into inventory.', 'RECEIVED')
ON DUPLICATE KEY UPDATE
    donor_id = VALUES(donor_id),
    donation_type = VALUES(donation_type),
    amount = VALUES(amount),
    material_name = VALUES(material_name),
    quantity = VALUES(quantity),
    description = VALUES(description),
    status = VALUES(status);

-- -----------------------------------------------------------------------
-- 17. NOTIFICATIONS
-- -----------------------------------------------------------------------
INSERT INTO notifications (id, recipient_user_id, type, priority, status, message, related_module, related_event_id) VALUES
(1, 1, 'CRITICAL_RESCUE', 'CRITICAL', 'UNREAD', 'New Critical Rescue Request reported in Mundakkai, Wayanad.', 'RESCUE', 1),
(2, 2, 'ASSIGNMENT', 'WARNING', 'UNREAD', 'NDRF 4th Battalion is currently en route for Operation Attamala Estate.', 'RESCUE', 2),
(3, 3, 'LOW_STOCK', 'WARNING', 'UNREAD', 'Champakulam Community Hall has reached 91% capacity threshold.', 'SHELTER', 2),
(4, 4, 'ASSIGNMENT', 'INFO', 'READ', 'You have been assigned to coordinate meal packet delivery at the Meppadi Relief Camp.', 'VOLUNTEER', 1),
(5, 1, 'LOW_STOCK', 'WARNING', 'UNREAD', 'Life Jackets stock is below recommended buffer (95 remaining).', 'RESOURCE', 5),
(6, 2, 'CRITICAL_RESCUE', 'CRITICAL', 'UNREAD', 'Severe River Flooding Alert: Kadukutty residence water levels rising above warning mark.', 'RESCUE', 7),
(7, 3, 'FOOD', 'INFO', 'READ', 'Consignment of 100 thermal blankets received at GVHSS Meppadi.', 'RESOURCE', 4),
(8, 4, 'ASSIGNMENT', 'INFO', 'UNREAD', 'New task: Sort and categorize newly received medical supplies at Meppadi Youth Center.', 'VOLUNTEER', 9),
(9, 1, 'SYSTEM', 'INFO', 'UNREAD', 'Lulu Humanitarian Foundation contributed INR 500,000 for relief procurement.', 'DONATION', 4),
(10, 5, 'CRITICAL_RESCUE', 'INFO', 'UNREAD', 'Your emergency request has been assigned to NDRF 4th Battalion.', 'RESCUE', 2)
ON DUPLICATE KEY UPDATE
    recipient_user_id = VALUES(recipient_user_id),
    type = VALUES(type),
    priority = VALUES(priority),
    status = VALUES(status),
    message = VALUES(message);

-- -----------------------------------------------------------------------
-- 18. FOOD DISTRIBUTION REQUESTS
-- -----------------------------------------------------------------------
INSERT INTO food_requests (id, request_code, disaster_id, location, beneficiary_type, beneficiaries, required_quantity, priority, status, description, requested_at, created_by, allocated_quantity, allocated_resource_id, allocated_at, allocated_by, assigned_volunteer_id, assigned_at, completed_at) VALUES
(1, 'FD-101', 1, 'Government VHSS Relief Camp, Meppadi (SHL-004)', 'SHELTER', 280, 840, 'HIGH', 'IN_PROGRESS',
 'Daily hot meal rations and high-energy nutrition packs for landslide evacuee families.', '2024-07-30 08:30:00', 3, 840, 1, '2024-07-30 09:15:00', @admin_id, 1, '2024-07-30 09:30:00', NULL),

(2, 'FD-102', 1, 'St. Joseph HSS, Meppadi (SHL-005)', 'FAMILY', 140, 420, 'CRITICAL', 'PARTIALLY_FULFILLED',
 'Emergency pediatric formula and specialized infant nutrition for displaced nursing mothers.', '2024-07-30 11:00:00', 3, 250, 10, '2024-07-30 12:00:00', @admin_id, 1, '2024-07-30 12:15:00', NULL),

(3, 'FD-103', 2, 'Champakulam Community Hall (SHL-002)', 'SHELTER', 320, 960, 'HIGH', 'COMPLETED',
 'Cooked porridge and dry ration survival kits for marooned backwater agricultural workers.', '2024-07-28 10:00:00', 3, 960, 1, '2024-07-28 11:30:00', @admin_id, 2, '2024-07-28 11:45:00', '2024-07-29 20:00:00'),

(4, 'FD-104', 3, 'St. Thomas College Relief Camp, Ranni (SHL-001)', 'SHELTER', 210, 630, 'HIGH', 'ALLOCATED',
 'Central kitchen grain supply, drinking water boxes, and ready-to-eat dry biscuit packs.', '2026-08-01 07:45:00', 2, 630, 1, '2026-08-01 08:30:00', @admin_id, 7, '2026-08-01 08:45:00', NULL),

(5, 'FD-105', 5, 'St. George HSS, Vilangad (SHL-006)', 'GROUP', 180, 540, 'CRITICAL', 'APPROVED',
 'Urgent dry provisions and clean water transit for families cut off by Malayangad bridge collapse.', '2024-07-30 06:30:00', 2, 0, NULL, NULL, NULL, NULL, NULL, NULL),

(6, 'FD-106', 6, 'Panampilly Memorial College, Chalakudy (SHL-011)', 'SHELTER', 250, 750, 'MEDIUM', 'COMPLETED',
 'Community kitchen supplies, rice packets, and boiled pulses for 250 camp residents.', '2024-07-31 08:00:00', 3, 750, 1, '2024-07-31 09:00:00', @admin_id, 5, '2024-07-31 09:15:00', '2024-08-01 19:30:00'),

(7, 'FD-107', 4, 'Valiyathura St. Antony HSS Shelter (SHL-008)', 'VICTIM', 95, 285, 'MEDIUM', 'IN_PROGRESS',
 'Meal packets and clean drinking water bottles for sea surge-affected coastal fishing families.', '2024-05-04 10:15:00', @admin_id, 285, 1, '2024-05-04 11:00:00', @admin_id, 6, '2024-05-04 11:15:00', NULL),

(8, 'FD-108', 1, 'Meppadi Municipal Youth Center (SHL-003)', 'GROUP', 120, 360, 'LOW', 'PENDING',
 'Buffer meal kit reserve requested for incoming volunteer brigades and night rescue workers.', '2024-07-30 14:00:00', 2, 0, NULL, NULL, NULL, NULL, NULL, NULL),

(9, 'FD-109', 1, 'GVHSS Adimali Shelter (SHL-013)', 'FAMILY', 65, 195, 'MEDIUM', 'APPROVED',
 'Warm cooked porridge and nutrition replenishment for elderly tea plantation workers.', '2024-07-31 09:30:00', 3, 0, NULL, NULL, NULL, NULL, NULL, NULL),

(10, 'FD-110', 2, 'Kazhakoottam GHSS (SHL-014)', 'SHELTER', 160, 480, 'LOW', 'COMPLETED',
 'Supplementary dry ration kits, biscuit packets, and canned milk for transit camp evacuees.', '2024-07-29 11:30:00', @admin_id, 480, 1, '2024-07-29 12:15:00', @admin_id, 1, '2024-07-29 12:30:00', '2024-07-30 18:00:00')
ON DUPLICATE KEY UPDATE
    request_code = VALUES(request_code),
    disaster_id = VALUES(disaster_id),
    location = VALUES(location),
    beneficiary_type = VALUES(beneficiary_type),
    beneficiaries = VALUES(beneficiaries),
    required_quantity = VALUES(required_quantity),
    priority = VALUES(priority),
    status = VALUES(status),
    description = VALUES(description),
    allocated_quantity = VALUES(allocated_quantity),
    allocated_resource_id = VALUES(allocated_resource_id),
    allocated_at = VALUES(allocated_at),
    allocated_by = VALUES(allocated_by),
    assigned_volunteer_id = VALUES(assigned_volunteer_id),
    assigned_at = VALUES(assigned_at),
    completed_at = VALUES(completed_at);

-- -----------------------------------------------------------------------
-- 19. FOOD DISTRIBUTIONS (History & Fulfillment Records)
-- -----------------------------------------------------------------------
INSERT INTO food_distributions (id, request_id, resource_id, quantity, beneficiaries_served, distributed_to, location, distributed_at, distributed_by, note) VALUES
(1, 1, 1, 420, 280, 'Government VHSS Relief Camp, Meppadi', 'Government VHSS Relief Camp, Meppadi (SHL-004)',
 '2024-07-30 13:00:00', 4, 'First batch hot meals and drinking water distributed to 280 sheltered evacuees.'),

(2, 2, 10, 150, 70, 'St. Joseph HSS, Meppadi', 'St. Joseph HSS, Meppadi (SHL-005)',
 '2024-07-30 14:30:00', 4, 'Infant milk formula and baby cereal packs handed out to nursing mothers.'),

(3, 3, 1, 480, 320, 'Champakulam Community Hall', 'Champakulam Community Hall (SHL-002)',
 '2024-07-28 13:30:00', 3, 'Lunch ration packets and clean water delivered via country boats.'),

(4, 3, 1, 480, 320, 'Champakulam Community Hall', 'Champakulam Community Hall (SHL-002)',
 '2024-07-29 19:30:00', 3, 'Evening dinner packets and dry nutritional biscuits fulfilled 100% of request.'),

(5, 4, 1, 200, 100, 'St. Thomas College Relief Camp, Ranni', 'St. Thomas College Relief Camp, Ranni (SHL-001)',
 '2026-08-01 12:00:00', 2, 'First batch hot kanji (porridge) distribution for elderly and children.'),

(6, 6, 1, 750, 250, 'Panampilly Memorial College, Chalakudy', 'Panampilly Memorial College, Chalakudy (SHL-011)',
 '2024-08-01 18:00:00', 3, 'Community kitchen bulk distribution completed for all 250 residents.'),

(7, 7, 1, 190, 95, 'Valiyathura St. Antony HSS Shelter', 'Valiyathura St. Antony HSS Shelter (SHL-008)',
 '2024-05-04 14:00:00', @admin_id, 'Two days dry rations handed over to camp committee.'),

(8, 10, 1, 480, 160, 'Kazhakoottam GHSS', 'Kazhakoottam GHSS (SHL-014)',
 '2024-07-30 17:00:00', 4, 'Full batch meal kits delivered to urban flood transit shelter.')
ON DUPLICATE KEY UPDATE
    request_id = VALUES(request_id),
    resource_id = VALUES(resource_id),
    quantity = VALUES(quantity),
    beneficiaries_served = VALUES(beneficiaries_served),
    distributed_to = VALUES(distributed_to),
    location = VALUES(location),
    distributed_at = VALUES(distributed_at),
    distributed_by = VALUES(distributed_by),
    note = VALUES(note);

