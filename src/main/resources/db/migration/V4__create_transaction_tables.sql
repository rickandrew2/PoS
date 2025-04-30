-- Create transactions table
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_number VARCHAR(10) NOT NULL UNIQUE,
    transaction_date DATETIME NOT NULL,
    cashier_id BIGINT NOT NULL,
    customer_id BIGINT,
    subtotal DECIMAL(10,2) NOT NULL,
    vat_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    is_vat_included BOOLEAN NOT NULL DEFAULT TRUE,
    is_pwd_discount BOOLEAN NOT NULL DEFAULT FALSE,
    is_senior_discount BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (cashier_id) REFERENCES users(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- Create transaction_items table
CREATE TABLE transaction_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
); 