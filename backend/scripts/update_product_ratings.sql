-- Set random star ratings (0.0 to 5.0, one decimal) for all products.
-- Run in PostgreSQL (e.g. psql or pgAdmin) connected to your ecommerce database.

UPDATE products
SET rating = (ROUND((random() * 5)::numeric, 1))::real;

-- Optional: show how many were updated and a sample
-- SELECT COUNT(*) AS updated_count FROM products;
-- SELECT id, product_name, rating FROM products ORDER BY random() LIMIT 10;
