-- ============================================
-- EcommerceFlow Database Initialization (MySQL)
-- ============================================

-- Customers table (used by JDBC Source Connector)
CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100) DEFAULT 'US',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Inventory table (used by Inventory Consumer)
CREATE TABLE IF NOT EXISTS inventory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50) UNIQUE NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    quantity_available INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    price DECIMAL(10,2) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notifications log table (for sink connector)
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    message TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- Sample Data
-- ============================================

INSERT IGNORE INTO customers (customer_id, first_name, last_name, email, phone, address, city, country) VALUES
('C001', 'John', 'Doe', 'john.doe@example.com', '+1-555-0101', '123 Main St', 'New York', 'US'),
('C002', 'Jane', 'Smith', 'jane.smith@example.com', '+1-555-0102', '456 Oak Ave', 'Los Angeles', 'US'),
('C003', 'Bob', 'Johnson', 'bob.johnson@example.com', '+1-555-0103', '789 Pine Rd', 'Chicago', 'US'),
('C004', 'Alice', 'Williams', 'alice.williams@example.com', '+1-555-0104', '321 Elm St', 'Houston', 'US'),
('C005', 'Charlie', 'Brown', 'charlie.brown@example.com', '+1-555-0105', '654 Maple Dr', 'Phoenix', 'US'),
('C006', 'Diana', 'Davis', 'diana.davis@example.com', '+1-555-0106', '987 Cedar Ln', 'San Francisco', 'US'),
('C007', 'Eve', 'Miller', 'eve.miller@example.com', '+1-555-0107', '147 Birch Ct', 'Seattle', 'US'),
('C008', 'Frank', 'Wilson', 'frank.wilson@example.com', '+1-555-0108', '258 Walnut Way', 'Denver', 'US'),
('C009', 'Grace', 'Moore', 'grace.moore@example.com', '+1-555-0109', '369 Spruce Pl', 'Boston', 'US'),
('C010', 'Hank', 'Taylor', 'hank.taylor@example.com', '+1-555-0110', '741 Ash Blvd', 'Miami', 'US');

INSERT IGNORE INTO inventory (product_id, product_name, category, quantity_available, price) VALUES
('P001', 'Wireless Bluetooth Headphones', 'Electronics', 500, 79.99),
('P002', 'Organic Cotton T-Shirt', 'Clothing', 1000, 29.99),
('P003', 'Stainless Steel Water Bottle', 'Home & Kitchen', 750, 24.99),
('P004', 'USB-C Fast Charger', 'Electronics', 300, 19.99),
('P005', 'Running Shoes Pro', 'Sports', 200, 129.99),
('P006', 'Laptop Backpack', 'Accessories', 400, 49.99),
('P007', 'Ceramic Coffee Mug Set', 'Home & Kitchen', 600, 34.99),
('P008', 'Fitness Tracker Band', 'Electronics', 350, 59.99),
('P009', 'Yoga Mat Premium', 'Sports', 250, 39.99),
('P010', 'Portable Bluetooth Speaker', 'Electronics', 450, 44.99);
