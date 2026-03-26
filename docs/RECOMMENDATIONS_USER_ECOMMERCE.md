# Recommended categories for user@ecommerce.com (from database)

## How the app decides what to recommend

Personalized recommendations for a user are driven by:

1. **ML service** (Python, `localhost:5000`) – when running, it uses:
   - `users` (including `ml_category` if set)
   - `user_purchase_history` (product IDs per user)
   - `user_view_history` (views)
   - `cart_items` (current cart)
   - Product categories from those products

2. **Backend fallback** (when ML is down) – `RecommendationController.fallbackPersonalizedRecommendations()`:
   - Collects **product categories** from:
     - Products in `user_purchase_history` for that user
     - Products in `cart_items` for that user
   - Recommends more products from those same categories (then fills with other products if needed)

So the **categories we are supposed to recommend** for `user@ecommerce.com` are the **product categories** (e.g. Electronics, Fashion, Home) that come from that user’s **purchases**, **cart**, and **view history** in the database. The `users.ml_category` column is a **user segment** (e.g. high_value, active_browser), not a product category.

---

## What the code creates for user@ecommerce.com

From `DataInitializer.java`, when `user@ecommerce.com` is created:

- No purchase history
- No view history
- No cart items
- `ml_category` is not set

So **by default** (fresh DB, no extra seed), there is **no user-specific data** for that user. In that case:

- **ML service**: will typically fall back to global/popular or generic logic.
- **Backend fallback**: `fallbackPersonalizedRecommendations` finds no preferred categories (empty purchase + cart), so it falls back to **all products** (by views) and does not filter by category.

So with the default DB state, there are **no specific categories “we are supposed to recommend”** for `user@ecommerce.com`; the app will show popular/global recommendations until that user has some history.

---

## How to see the actual recommended categories in your DB (pgAdmin)

Run these in pgAdmin against your PostgreSQL database to see what **your** DB says for `user@ecommerce.com`.

### 1. User id

```sql
SELECT id, email, user_name, ml_category
FROM users
WHERE email = 'user@ecommerce.com';
```

### 2. Categories from **purchase history**

(Products this user has bought → their categories.)

```sql
SELECT DISTINCT p.category
FROM products p
JOIN user_purchase_history uph ON uph.product_id = p.id
WHERE uph.user_id = (SELECT id FROM users WHERE email = 'user@ecommerce.com')
  AND p.category IS NOT NULL AND p.category != '';
```

### 3. Categories from **cart**

(Products currently in cart.)

```sql
SELECT DISTINCT p.category
FROM products p
JOIN cart_items ci ON ci.product_id = p.id
JOIN users u ON u.id = ci.user_id
WHERE u.email = 'user@ecommerce.com'
  AND p.category IS NOT NULL AND p.category != '';
```

### 4. Categories from **view history**

(Products this user has viewed.)

```sql
SELECT DISTINCT p.category
FROM products p
JOIN user_view_history uvh ON uvh.product_id = p.id
JOIN users u ON u.id = uvh.user_id
WHERE u.email = 'user@ecommerce.com'
  AND p.category IS NOT NULL AND p.category != '';
```

### 5. All categories that should drive recommendations (union)

```sql
WITH uid AS (SELECT id FROM users WHERE email = 'user@ecommerce.com')
SELECT DISTINCT p.category
FROM products p
WHERE (
    p.id IN (SELECT product_id FROM user_purchase_history WHERE user_id = (SELECT id FROM uid))
    OR p.id IN (SELECT product_id FROM cart_items WHERE user_id = (SELECT id FROM uid))
    OR p.id IN (SELECT product_id FROM user_view_history WHERE user_id = (SELECT id FROM uid))
  )
  AND p.category IS NOT NULL AND p.category != ''
ORDER BY 1;
```

- If this returns **no rows**, then with the current DB there are **no specific categories** for that user; the app will use global/popular behavior.
- If it returns rows (e.g. `Electronics`, `Fashion`), those are the **product categories** the app is supposed to use to recommend to `user@ecommerce.com` (purchase/cart/view-based logic).

---

## Summary

| Source              | Table / behavior                    | Drives recommended categories? |
|---------------------|-------------------------------------|---------------------------------|
| Purchases           | `user_purchase_history` → products | Yes (product `category`)        |
| Cart                | `cart_items` → products             | Yes (product `category`)        |
| Views               | `user_view_history` → products      | Yes (product `category`)        |
| User segment        | `users.ml_category`                 | No (segment, not product cat.) |

**For user@ecommerce.com:** run the SQL above in pgAdmin. The result of query **5** is the set of **categories we are supposed to recommend** to that user based on your current database. If the result is empty, the app has no user-specific signal and will recommend globally/popular until the user has purchases, cart items, or views.
