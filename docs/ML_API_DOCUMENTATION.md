# ML Algorithm API Documentation

This document explains the ML algorithm endpoints and the stages of execution for updating the database with ML categorization results.

---

## What to run to “learn” on products and users

Only **Phase 1** actually trains/categorizes and writes results into the database. Phase 2 and Phase 3 are not implemented yet (they return “pending”).

| Goal | What to run | When |
|------|-------------|------|
| **Learn on users and products** (fill `ml_category`, product categories) | **POST `/api/ml/phase1`** | When you want to (re)run user and product categorization and update the DB. |
| **In-app recommendations** (For You, Popular, Similar) | Nothing extra | They use the **Python ML service** (port 5000) and DB data automatically when users browse. No Phase 1 required. |

**To run Phase 1:**

1. **Backend** running (e.g. `http://localhost:8080`).
2. **Python** and project data in place: `datasets/raw/` (e.g. `users_5000.csv`, `products_10000.csv` or similar) and `src/phase1/` scripts in the **project root** (the parent of `backend/` and `datasets/`). Phase 1 runs from that root.
3. Call **POST** `http://localhost:8080/api/ml/phase1` (Swagger, Postman, or `curl -X POST http://localhost:8080/api/ml/phase1`).

Phase 1 will:

1. Run Python scripts (user + product categorization) that read from `datasets/raw/` and write to `datasets/results/phase1/`.
2. Read those result CSVs and update **users** (`ml_category`) and **products** (`category`, `sub_category`, `ml_category`) in PostgreSQL.

---

## How to know the results are correct

### 1. Check the Phase 1 API response

- **`"success": true`** and no `"error"` means the pipeline ran.
- In **`stages.stage3_update_users`**: `updated` should be high, `errors` should be **0**. Some `not_found` is OK (CSV can have IDs not in the DB).
- In **`stages.stage4_update_products`**: same idea — high `updated`, **0** `errors`.

If any stage has `success: false` or non‑zero `errors`, check backend logs and the “Error Handling” section below.

### 2. Check the database (pgAdmin or any SQL client)

Run these in your ecommerce database:

```sql
-- Users: are they categorized?
SELECT ml_category, COUNT(*) AS count
FROM users
WHERE ml_category IS NOT NULL
GROUP BY ml_category
ORDER BY count DESC;

-- Users: how many classified vs total
SELECT
  COUNT(*) AS total_users,
  COUNT(ml_category) AS classified_users
FROM users;

-- Products: do they have categories?
SELECT category, sub_category, COUNT(*) AS count
FROM products
GROUP BY category, sub_category
ORDER BY count DESC;
```

**You know it worked when:**

- **Users:** `classified_users` is large (and matches what you expect from your CSV). `ml_category` has values like `high_value`, `active_browser`, `category_loyal`, etc.
- **Products:** Rows have non‑null `category` / `sub_category` and counts look reasonable (e.g. several categories, not everything in one bucket).

### 3. Check the result files (optional)

- **Users:** `datasets/results/phase1/users_with_clusters.csv` — should exist and have a column with the category labels.
- **Products:** `datasets/results/phase1/products_with_categories.csv` — should have `predicted_main_category` and `predicted_sub_category`.

If Phase 1 failed early, these files might be missing or unchanged; if Stage 2+ failed, the CSVs may be correct but the DB not updated (check logs).

### 4. Check the app

- **Recommendations:** Open the app, go to the homepage and a product page. “For You”, “Popular Purchases”, and “Similar Items” should load (they use the ML service and DB; they do **not** require Phase 1 to have run).
- **Phase 1 impact:** After a successful Phase 1, user segments and product categories in the DB are updated; any feature that reads `ml_category` or product `category`/`sub_category` will then use the new values.

---

## Why is `ml_category` empty?

Both the **users** and **products** tables have an `ml_category` column. Neither is filled automatically (no trigger on login, no background job).

**Users:** `ml_category` is populated when you run the **Phase 1 ML pipeline**:

1. Phase 1 runs Python scripts (user categorization) that produce `datasets/results/phase1/users_with_clusters.csv`
2. The backend reads that CSV and updates each user’s `ml_category` (e.g. `"high_value"`, `"active_browser"`, `"category_loyal"`)

**Products:** `ml_category` is populated by the same Phase 1 run (or single-product categorize):

1. Phase 1 runs product categorization and produces `datasets/results/phase1/products_with_categories.csv` (with `predicted_main_category`, `predicted_sub_category`)
2. The backend updates each product’s `category`, `sub_category`, and **`ml_category`** (set to the main category from ML)

**How to fill them:**

- **All users and products:** Call **POST `/api/ml/phase1`**. This runs the full Phase 1 pipeline and updates both users and products (including both `ml_category` columns). Requires Python/ML scripts and datasets to be in place.
- **Single user:** **POST `/api/ml/user/{userId}/categorize`** — updates that user’s `ml_category`.
- **Single product:** **POST `/api/ml/product/{productId}/categorize`** — updates that product’s `category`, `sub_category`, and `ml_category`.

If you have never run Phase 1 (or the single-user/single-product endpoints), `ml_category` will stay `NULL` for all users and products.

## Endpoints

### POST `/api/ml/phase1`
Runs Phase 1: User and Product Categorization

**What it does:**
1. Executes Python ML scripts for user and product categorization
2. Reads results from CSV files in `datasets/results/phase1/`
3. Updates users in PostgreSQL with ML categories
4. Updates products in PostgreSQL with ML categories

**Response Structure:**
```json
{
  "success": true,
  "stages": {
    "stage1_execute_scripts": {
      "success": true,
      "user_categorization": { "exit_code": 0, "success": true, "output": "..." },
      "product_categorization": { "exit_code": 0, "success": true, "output": "..." }
    },
    "stage2_read_results": {
      "success": true,
      "user_categories": { "3": "explorer", "4": "explorer", ... },
      "user_count": 5000,
      "product_categories": { "278": { "main_category": "Accessories", "sub_category": "Bags" }, ... },
      "product_count": 10000
    },
    "stage3_update_users": {
      "success": true,
      "updated": 4850,
      "not_found": 150,
      "errors": 0,
      "total_processed": 5000
    },
    "stage4_update_products": {
      "success": true,
      "updated": 9800,
      "not_found": 200,
      "errors": 0,
      "total_processed": 10000
    }
  },
  "summary": {
    "users_updated": 4850,
    "users_not_found": 150,
    "users_errors": 0,
    "products_updated": 9800,
    "products_not_found": 200,
    "products_errors": 0
  }
}
```

### POST `/api/ml/phase2`
Runs Phase 2: Recommendation System (pending implementation)

### POST `/api/ml/phase3`
Runs Phase 3: Single Item Categorization (pending implementation)

### GET `/api/ml/status`
Returns the status of ML service and available phases

## Execution Stages Explained

### Stage 1: Execute Python Scripts
**Purpose:** Run the ML algorithms to generate categorization results

**What happens:**
- Executes `src/phase1/user_categorization.py` to categorize users
- Executes `src/phase1/product_categorization.py` to categorize products
- Scripts read data from `datasets/raw/` and save results to `datasets/results/phase1/`

**Output files created:**
- `datasets/results/phase1/users_with_clusters.csv` - User categorization results
- `datasets/results/phase1/products_with_categories.csv` - Product categorization results
- `datasets/results/phase1/categorization_summary.csv` - Summary metrics
- `datasets/results/phase1/feature_importance.csv` - Feature importance scores

**Verification:**
- Check that both scripts completed with exit code 0
- Check the output logs for any errors
- Verify that CSV files were created in `datasets/results/phase1/`

### Stage 2: Read Results from CSV
**Purpose:** Parse the CSV files and extract categorization data

**What happens:**
- Reads `users_with_clusters.csv` and extracts `user_id` → `category` mappings
- Reads `products_with_categories.csv` and extracts `id` → `predicted_main_category`, `predicted_sub_category` mappings

**Data extracted:**
- User categories: e.g., "high_value", "active_browser", "occasional_buyer", "price_sensitive", "category_loyal", "explorer", "light_user", "inactive"
- Product categories: Main category (e.g., "Accessories") and Sub category (e.g., "Bags")

**Verification:**
- Check that `user_count` matches expected number of users
- Check that `product_count` matches expected number of products
- Verify that categories are non-empty strings

### Stage 3: Update Users in PostgreSQL
**Purpose:** Update the `users` table with ML categorization results

**What happens:**
- For each user in the CSV results:
  1. Finds the user by ID in the database
  2. Updates `ml_category` field with the ML category (e.g., "high_value")
  3. Sets `segment` to `CLASSIFIED`
  4. Sets `last_classified_at` to current timestamp
  5. Saves the user

**Database fields updated:**
- `users.ml_category` - ML category string (e.g., "high_value")
- `users.segment` - Set to `CLASSIFIED`
- `users.last_classified_at` - Timestamp of classification

**Verification:**
- Check `updated` count - should match most users from CSV
- Check `not_found` count - users in CSV but not in database (expected if CSV has more users)
- Check `errors` count - should be 0
- Query database: `SELECT ml_category, COUNT(*) FROM users WHERE ml_category IS NOT NULL GROUP BY ml_category;`

### Stage 4: Update Products in PostgreSQL
**Purpose:** Update the `products` table with ML categorization results

**What happens:**
- For each product in the CSV results:
  1. Finds the product by ID in the database
  2. Updates `category` field with `predicted_main_category`
  3. Updates `sub_category` field with `predicted_sub_category`
  4. Sets `updated_at` to current timestamp
  5. Saves the product

**Database fields updated:**
- `products.category` - Main category from ML (e.g., "Accessories")
- `products.sub_category` - Sub category from ML (e.g., "Bags")
- `products.updated_at` - Timestamp of update

**Verification:**
- Check `updated` count - should match most products from CSV
- Check `not_found` count - products in CSV but not in database (expected if CSV has more products)
- Check `errors` count - should be 0
- Query database: `SELECT category, sub_category, COUNT(*) FROM products GROUP BY category, sub_category;`

## Database Schema Changes

### Users Table
Added column:
- `ml_category VARCHAR(255)` - Stores ML category string (e.g., "high_value", "active_browser")

The migration runs automatically on application startup via `MLCategoryMigration.java`.

## Verification Queries

After running Phase 1, you can verify the updates with these SQL queries:

```sql
-- Check user categorization distribution
SELECT ml_category, COUNT(*) as count 
FROM users 
WHERE ml_category IS NOT NULL 
GROUP BY ml_category 
ORDER BY count DESC;

-- Check how many users were classified
SELECT 
  COUNT(*) as total_users,
  COUNT(ml_category) as classified_users,
  COUNT(*) - COUNT(ml_category) as unclassified_users
FROM users;

-- Check product categorization distribution
SELECT category, sub_category, COUNT(*) as count 
FROM products 
GROUP BY category, sub_category 
ORDER BY count DESC;

-- Check when users were last classified
SELECT 
  DATE(last_classified_at) as classification_date,
  COUNT(*) as users_classified
FROM users 
WHERE last_classified_at IS NOT NULL
GROUP BY DATE(last_classified_at)
ORDER BY classification_date DESC;
```

## Error Handling

If any stage fails:
- The response will include `"success": false` and an `"error"` message
- Completed stages will still be included in the response
- Check the logs for detailed error messages
- Common issues:
  - Python scripts not found: Check that `src/phase1/` exists
  - CSV files not found: Run the Python scripts first or check `datasets/results/phase1/`
  - Database connection issues: Check PostgreSQL is running
  - Path resolution issues: Check project structure matches expected layout

## Example Usage

```bash
# Run Phase 1
curl -X POST http://localhost:8080/api/ml/phase1

# Check status
curl http://localhost:8080/api/ml/status
```

## Notes

- The ML scripts must be run from the project root directory
- CSV files are expected in `datasets/results/phase1/`
- The service automatically handles path resolution
- Database migrations run automatically on startup
- All updates are logged with detailed stage information
