-- =======================================================================
-- ResQHub Database Schema
-- Integrated Disaster Response Coordination System
-- Database: MySQL 8.x
-- =======================================================================

DROP DATABASE IF EXISTS resqhub;
CREATE DATABASE resqhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE resqhub;


-- 1. USERS
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role ENUM('ADMIN', 'RESCUE_OFFICER', 'CAMP_MANAGER', 'VOLUNTEER', 'CITIZEN') NOT NULL DEFAULT 'CITIZEN',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_role (role),
    INDEX idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. DISASTERS
CREATE TABLE disasters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    disaster_type ENUM('FLOOD', 'EARTHQUAKE', 'LANDSLIDE', 'CYCLONE', 'FIRE') NOT NULL,
    description TEXT,
    location VARCHAR(200) NOT NULL,
    severity ENUM('LOW', 'MODERATE', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MODERATE',
    status ENUM('ACTIVE', 'CONTAINED', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    start_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_disaster_status (status),
    INDEX idx_disaster_severity (severity),
    INDEX idx_disaster_type (disaster_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. VICTIMS
CREATE TABLE victims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_number VARCHAR(30) NOT NULL UNIQUE,
    disaster_id BIGINT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    age INT,
    contact_number VARCHAR(30),
    location VARCHAR(200) NOT NULL,
    affected_family_members INT NOT NULL DEFAULT 1,
    accessibility_needs VARCHAR(255),
    current_situation TEXT,
    rescue_status ENUM('AWAITING_RESCUE', 'ASSIGNED', 'RESCUED', 'SHELTERED', 'ACCOUNTED_FOR') NOT NULL DEFAULT 'AWAITING_RESCUE',
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (disaster_id) REFERENCES disasters(id) ON DELETE RESTRICT,
    INDEX idx_victim_disaster (disaster_id),
    INDEX idx_victim_status (rescue_status),
    INDEX idx_victim_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. RESCUE REQUESTS
CREATE TABLE rescue_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_number VARCHAR(30) NOT NULL UNIQUE,
    victim_id BIGINT NULL,
    disaster_id BIGINT NOT NULL,
    reported_by_user_id BIGINT NULL,
    location VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    people_count INT NOT NULL DEFAULT 1,
    urgency ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    calculated_priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    status ENUM('PENDING', 'VERIFIED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (victim_id) REFERENCES victims(id) ON DELETE SET NULL,
    FOREIGN KEY (disaster_id) REFERENCES disasters(id) ON DELETE RESTRICT,
    FOREIGN KEY (reported_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_req_status (status),
    INDEX idx_req_priority (calculated_priority),
    INDEX idx_req_disaster (disaster_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. RESCUE TEAMS
CREATE TABLE rescue_teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_identifier VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    team_leader VARCHAR(100) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    skills VARCHAR(255) NOT NULL,
    operating_area VARCHAR(150) NOT NULL,
    capacity INT NOT NULL DEFAULT 10,
    availability_status ENUM('AVAILABLE', 'ASSIGNED', 'ON_OPERATION', 'UNAVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_team_avail (availability_status),
    INDEX idx_team_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. RESCUE TEAM MEMBERS
CREATE TABLE rescue_team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    role_title VARCHAR(50) NOT NULL,
    contact_number VARCHAR(30),
    specialty VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES rescue_teams(id) ON DELETE CASCADE,
    INDEX idx_member_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. RESCUE ASSIGNMENTS
CREATE TABLE rescue_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NOT NULL,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expected_completion TIMESTAMP NULL,
    actual_completion TIMESTAMP NULL,
    status ENUM('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'ASSIGNED',
    progress_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES rescue_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES rescue_teams(id) ON DELETE RESTRICT,
    FOREIGN KEY (assigned_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_assign_req (request_id),
    INDEX idx_assign_team (team_id),
    INDEX idx_assign_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. SHELTERS
CREATE TABLE shelters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shelter_identifier VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    location_description TEXT,
    contact_person VARCHAR(100) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    total_capacity INT NOT NULL DEFAULT 50,
    current_occupancy INT NOT NULL DEFAULT 0,
    status ENUM('AVAILABLE', 'NEARLY_FULL', 'FULL', 'TEMPORARILY_CLOSED') NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shelter_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. SHELTER RESIDENTS
CREATE TABLE shelter_residents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shelter_id BIGINT NOT NULL,
    victim_id BIGINT NULL,
    disaster_id BIGINT NOT NULL,
    resident_name VARCHAR(100) NOT NULL,
    family_size INT NOT NULL DEFAULT 1,
    check_in_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    check_out_time TIMESTAMP NULL,
    status ENUM('ACTIVE', 'CHECKED_OUT') NOT NULL DEFAULT 'ACTIVE',
    registered_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shelter_id) REFERENCES shelters(id) ON DELETE RESTRICT,
    FOREIGN KEY (victim_id) REFERENCES victims(id) ON DELETE SET NULL,
    FOREIGN KEY (disaster_id) REFERENCES disasters(id) ON DELETE RESTRICT,
    FOREIGN KEY (registered_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_res_shelter (shelter_id),
    INDEX idx_res_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. SHELTER ALLOCATIONS
CREATE TABLE shelter_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shelter_id BIGINT NOT NULL,
    disaster_id BIGINT NOT NULL,
    requested_people_count INT NOT NULL DEFAULT 1,
    status ENUM('RECOMMENDED', 'CONFIRMED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'RECOMMENDED',
    recommendation_reason VARCHAR(255),
    decided_by_user_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shelter_id) REFERENCES shelters(id) ON DELETE RESTRICT,
    FOREIGN KEY (disaster_id) REFERENCES disasters(id) ON DELETE RESTRICT,
    FOREIGN KEY (decided_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_alloc_shelter (shelter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. VOLUNTEERS
CREATE TABLE volunteers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    volunteer_identifier VARCHAR(30) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    operating_area VARCHAR(150) NOT NULL,
    skills VARCHAR(255) NOT NULL,
    experience_years INT DEFAULT 0,
    availability_status ENUM('AVAILABLE', 'ASSIGNED', 'UNAVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
    approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    emergency_contact VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_vol_avail (availability_status),
    INDEX idx_vol_approval (approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. VOLUNTEER ASSIGNMENTS
CREATE TABLE volunteer_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    volunteer_id BIGINT NOT NULL,
    disaster_id BIGINT NOT NULL,
    task_title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(200) NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'MEDIUM',
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expected_completion TIMESTAMP NULL,
    status ENUM('PENDING', 'OFFERED', 'ACCEPTED', 'DECLINED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    progress_notes TEXT,
    assigned_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (volunteer_id) REFERENCES volunteers(id) ON DELETE RESTRICT,
    FOREIGN KEY (disaster_id) REFERENCES disasters(id) ON DELETE RESTRICT,
    FOREIGN KEY (assigned_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_vol_assign (volunteer_id),
    INDEX idx_vol_assign_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. INVENTORY ITEMS
CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL,
    category ENUM('FOOD', 'DRINKING_WATER', 'CLOTHING', 'BLANKETS', 'HYGIENE_KITS', 'RESCUE_EQUIPMENT') NOT NULL,
    unit ENUM('KG', 'LITERS', 'BOXES', 'PIECES', 'SETS') NOT NULL DEFAULT 'PIECES',
    description VARCHAR(255),
    quantity_available INT NOT NULL DEFAULT 0,
    minimum_stock_level INT NOT NULL DEFAULT 10,
    storage_location VARCHAR(150) NOT NULL,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_item_category (category),
    INDEX idx_item_quantity (quantity_available)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. INVENTORY TRANSACTIONS
CREATE TABLE inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    transaction_type ENUM('RECEIVED', 'ISSUED', 'ALLOCATED', 'ADJUSTED') NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    performed_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES inventory_items(id) ON DELETE RESTRICT,
    FOREIGN KEY (performed_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_trans_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. RESOURCE ALLOCATIONS
CREATE TABLE resource_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    disaster_id BIGINT NOT NULL,
    shelter_id BIGINT NULL,
    destination_location VARCHAR(200) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    responsible_person VARCHAR(100) NOT NULL,
    status ENUM('PLANNED', 'APPROVED', 'DISPATCHED', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PLANNED',
    allocated_by_user_id BIGINT NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES inventory_items(id) ON DELETE RESTRICT,
    FOREIGN KEY (disaster_id) REFERENCES disasters(id) ON DELETE RESTRICT,
    FOREIGN KEY (shelter_id) REFERENCES shelters(id) ON DELETE SET NULL,
    FOREIGN KEY (allocated_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_res_alloc_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. FOOD DISTRIBUTIONS
CREATE TABLE food_distributions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shelter_id BIGINT NULL,
    location VARCHAR(200) NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    recipient_count INT NOT NULL DEFAULT 1,
    distribution_date DATE NOT NULL,
    status ENUM('PLANNED', 'COMPLETED') NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shelter_id) REFERENCES shelters(id) ON DELETE SET NULL,
    FOREIGN KEY (item_id) REFERENCES inventory_items(id) ON DELETE RESTRICT,
    INDEX idx_food_date (distribution_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. DONATIONS
CREATE TABLE donations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donation_reference VARCHAR(30) NOT NULL UNIQUE,
    donor_name VARCHAR(100) NOT NULL,
    donor_email VARCHAR(100) NOT NULL,
    donor_phone VARCHAR(30),
    donation_type ENUM('MONETARY', 'MATERIAL') NOT NULL,
    amount DECIMAL(12,2) NULL,
    item_id BIGINT NULL,
    item_quantity INT NULL,
    donation_date DATE NOT NULL,
    status ENUM('SUBMITTED', 'VERIFIED', 'RECEIVED', 'ALLOCATED', 'CANCELLED') NOT NULL DEFAULT 'SUBMITTED',
    notes TEXT,
    verified_by_user_id BIGINT NULL,
    inventory_added BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES inventory_items(id) ON DELETE SET NULL,
    FOREIGN KEY (verified_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_don_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. NOTIFICATIONS
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    target_role ENUM('ADMIN', 'RESCUE_OFFICER', 'CAMP_MANAGER', 'VOLUNTEER', 'CITIZEN') NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    link_url VARCHAR(255),
    notification_type ENUM('RESCUE', 'SHELTER', 'VOLUNTEER', 'INVENTORY', 'DISASTER', 'DONATION', 'SYSTEM') NOT NULL DEFAULT 'SYSTEM',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id, is_read),
    INDEX idx_notif_role (target_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. ACTIVITY LOGS
CREATE TABLE activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    affected_record_id VARCHAR(50),
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_log_user (user_id),
    INDEX idx_log_module (module),
    INDEX idx_log_date (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
