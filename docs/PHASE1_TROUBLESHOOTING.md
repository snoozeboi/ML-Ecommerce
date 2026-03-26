# Phase 1: No changes in PostgreSQL / pgAdmin

If you ran **POST /api/ml/phase1** from Swagger but don’t see any changes in the database, use this checklist.

---

## 1. Check the Swagger response

After calling **POST /api/ml/phase1**, look at the response body:

- **`"success": true`** – The pipeline ran without throwing.
- **`stages.stage2_read_results`** – `user_count` and `product_count` should be &gt; 0. If both are 0, the backend didn’t find or couldn’t read the Phase 1 result CSVs.
- **`stages.stage3_update_users`** – `updated` = how many users were updated; `not_found` = user IDs from the CSV that don’t exist in the DB; `errors` should be 0.
- **`stages.stage4_update_products`** – Same idea: `updated`, `not_found`, `errors`.

If **`updated` is 0** for both users and products, the usual cause is **ID mismatch** (see below).

---

## 2. Check backend logs (project root and CSV paths)

When the backend starts and when Phase 1 runs, it logs:

- **Project root** – e.g. `ML Service initialized - Project root: ...\ML-eCommers-GitHub-9.2.26`
- **Datasets path** – should be `...\ML-eCommers-GitHub-9.2.26\datasets`
- **Stage 2** – “Reading user categorization results from: ...\datasets\results\phase1\users_with_clusters.csv” (and same for products).

If the project root is wrong (e.g. inside `backend` or another folder), the backend won’t find `datasets/results/phase1/` and `user_count` / `product_count` will be 0.

**Fix:** Run the backend with working directory = **project root** (the folder that contains `backend/`, `datasets/`, `scripts/`). In your IDE, set the run configuration’s “Working directory” to that folder.

---

## 3. ID mismatch (CSV vs database)

Phase 1 result CSVs use IDs from the **datasets** (e.g. `users_with_clusters.csv` has `user_id` 3, 4, 5, …; `products_with_categories.csv` has `id` 278, 3118, 3775, …).  
Your PostgreSQL **users** and **products** may have been created with different IDs (e.g. 1, 2, 3, …).

- **Users:** The backend only matches by **user_id**. If the CSV has 3, 4, 5 and your DB has 1, 2, 3, then user_id 3 exists in both and can be updated; users 1 and 2 in the DB have no row in the CSV so they stay unchanged. So you may see **some** user updates.
- **Products:** The backend first matches by **product id**; if that fails, it falls back to **product name** (from the CSV). So even if your DB has product IDs 1, 2, 3, … and the CSV has 278, 3118, …, products can still be updated by matching **product_name**. After a recent change, Phase 1 should update products by name when ID doesn’t match.

If you still see **0 product updates**, ensure:

- The Phase 1 result file **`datasets/results/phase1/products_with_categories.csv`** exists and contains a **`product_name`** column and **`predicted_main_category`** / **`predicted_sub_category`**.
- Product names in that CSV match (or are contained in) the names in your **products** table (e.g. “Premium Backpack”, “Canvas Tote Bag”).

---

## 4. Verify in pgAdmin

After a successful run (and non‑zero `updated` in the response), run in your ecommerce DB:

```sql
-- Users: how many have ml_category set?
SELECT ml_category, COUNT(*) FROM users WHERE ml_category IS NOT NULL GROUP BY ml_category;

-- Products: how many have category set?
SELECT category, sub_category, COUNT(*) FROM products WHERE category IS NOT NULL GROUP BY category, sub_category;
```

You should see rows with counts. If these are empty, the updates didn’t apply (check the response and logs as above).

---

## 5. Run Phase 1 again after fixes

After fixing the working directory or ensuring the CSVs exist and have the right columns:

1. Run **POST /api/ml/phase1** again from Swagger.
2. Check the response: `stage3_update_users.updated` and `stage4_update_products.updated` &gt; 0.
3. Re-run the pgAdmin queries above to confirm data in the DB.
