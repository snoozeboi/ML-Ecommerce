-- =============================================================================
-- What user@ecommerce.com should see in RECOMMENDATIONS (based on current DB)
-- Run in pgAdmin: open Query Tool, paste one block at a time (or all), execute.
--
-- KEY RESULTS:
--   Query 1: user id (e.g. 2)
--   Queries 2-4: categories from purchases / cart / views (may be empty)
--   Query 5: ALL categories that drive recommendations. EMPTY = app shows
--            global/popular; NON-EMPTY = app recommends from these categories.
--   Query 6: The first 30 products they should see in "Recommended" when
--            using the backend fallback (same logic as when ML is down).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) User row
-- -----------------------------------------------------------------------------
SELECT id AS user_id, email, user_name, ml_category
FROM users
WHERE email = 'user@ecommerce.com';


-- -----------------------------------------------------------------------------
-- 2) Categories from PURCHASES (products this user bought)
-- -----------------------------------------------------------------------------
SELECT 'PURCHASES' AS source, p.category, p.sub_category, COUNT(*) AS product_count
FROM products p
JOIN user_purchase_history uph ON uph.product_id = p.id
WHERE uph.user_id = (SELECT id FROM users WHERE email = 'user@ecommerce.com')
  AND p.category IS NOT NULL AND p.category != ''
GROUP BY p.category, p.sub_category
ORDER BY 1, 2;


-- -----------------------------------------------------------------------------
-- 3) Categories from CART (products currently in cart)
-- -----------------------------------------------------------------------------
SELECT 'CART' AS source, p.category, p.sub_category, COUNT(*) AS product_count
FROM products p
JOIN cart_items ci ON ci.product_id = p.id
JOIN users u ON u.id = ci.user_id
WHERE u.email = 'user@ecommerce.com'
  AND p.category IS NOT NULL AND p.category != ''
GROUP BY p.category, p.sub_category
ORDER BY 1, 2;


-- -----------------------------------------------------------------------------
-- 4) Categories from VIEW HISTORY (products this user viewed)
-- -----------------------------------------------------------------------------
SELECT 'VIEWS' AS source, p.category, p.sub_category, COUNT(*) AS product_count
FROM products p
JOIN user_view_history uvh ON uvh.product_id = p.id
JOIN users u ON u.id = uvh.user_id
WHERE u.email = 'user@ecommerce.com'
  AND p.category IS NOT NULL AND p.category != ''
GROUP BY p.category, p.sub_category
ORDER BY 1, 2;


-- -----------------------------------------------------------------------------
-- 5) ALL categories that drive recommendations (union: purchase + cart + view)
--    If this returns no rows → app shows global/popular, not category-specific.
-- -----------------------------------------------------------------------------
WITH uid AS (SELECT id FROM users WHERE email = 'user@ecommerce.com'),
     interacted_products AS (
       SELECT product_id FROM user_purchase_history WHERE user_id = (SELECT id FROM uid)
       UNION
       SELECT product_id FROM cart_items WHERE user_id = (SELECT id FROM uid)
       UNION
       SELECT product_id FROM user_view_history WHERE user_id = (SELECT id FROM uid)
     )
SELECT DISTINCT p.category, p.sub_category
FROM products p
WHERE p.id IN (SELECT product_id FROM interacted_products)
  AND p.category IS NOT NULL AND p.category != ''
ORDER BY 1, 2;


-- -----------------------------------------------------------------------------
-- 6) PRODUCTS user@ecommerce.com should see in Recommendations (first 30)
--    Mirrors backend fallback: preferred categories from purchases + cart;
--    then products in those categories by views (or all by views if no pref).
-- -----------------------------------------------------------------------------
WITH uid AS (SELECT id FROM users WHERE email = 'user@ecommerce.com'),
     exclude_ids AS (
       SELECT product_id AS id FROM user_purchase_history WHERE user_id = (SELECT id FROM uid)
       UNION
       SELECT product_id FROM cart_items WHERE user_id = (SELECT id FROM uid)
     ),
     preferred_cats AS (
       SELECT DISTINCT p.category FROM products p
       WHERE p.id IN (SELECT id FROM exclude_ids)
         AND p.category IS NOT NULL AND p.category != ''
     ),
     has_pref AS (SELECT EXISTS(SELECT 1 FROM preferred_cats) AS has)
SELECT p.id, p.product_name, p.category, p.sub_category, p.views, p.rating
FROM products p
CROSS JOIN has_pref
WHERE p.id NOT IN (SELECT id FROM exclude_ids)
  AND (NOT has_pref.has OR p.category IN (SELECT category FROM preferred_cats))
ORDER BY p.views DESC NULLS LAST
LIMIT 30;
