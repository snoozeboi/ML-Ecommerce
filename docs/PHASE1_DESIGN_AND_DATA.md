# Phase 1: Design, Learning Data, and How the 10,000 Products Are Updated

## 1. How Phase 1 is designed

Phase 1 has **two independent pipelines** that run in sequence when you call **POST `/api/ml/phase1`** (or run `run_all_phases.py --phase1`):

| Part | What it does | Python module | Output |
|------|----------------|----------------|--------|
| **Product categorization** | Trains XGBoost on product text + price, predicts main + sub category for every product | `src/phase1/product_categorization.py` | `datasets/results/phase1/products_with_categories.csv` |
| **User categorization** | Trains Random Forest on user behavior features, assigns each user a segment (e.g. high_value, explorer) | `src/phase1/user_categorization.py` | `datasets/results/phase1/users_with_clusters.csv` |

After the Python scripts finish, the **Java backend** (`MLService`) reads those two CSVs and **updates PostgreSQL**:

- **Products:** `category`, `sub_category`, `ml_category`, `updated_at` in the `products` table (matched by product `id`, or by product name if ID not found).
- **Users:** `ml_category`, `segment`, `last_classified_at` in the `users` table (matched by user `id`).

So Phase 1 is: **learn from CSV files → write result CSVs → backend reads result CSVs and updates the DB.**

---

## 2. From where Phase 1 learns (input files)

All paths are under the **project root** (parent of `backend/`, `datasets/`, `src/`).

### Product categorization (10,000 products)

| Purpose | File | Used for |
|--------|------|----------|
| **Primary input (preferred)** | `backend/src/main/resources/seed/products.csv` | Same file the backend seeds from. Source of truth for DB with correct categories and row order. |
| **Fallback input** | `datasets/raw/products_10000.csv` | Loads all products. Uses columns: `id`, `product_name`, `description`, `price`, `main_category`, `sub_category`. |

**Note:** `product_interaction_metadata.csv` is **never** used for Phase 1 product categorization. Its `cat1`/`cat2` can be wrong (e.g. pid 16 = Food/smoothies for Cotton Tote Bag Set).

- **Learning:** The model is trained on the chosen CSV: features = `product_name` + `description` (TF-IDF) + `price` (scaled); target = `main_category || sub_category`. So it **learns from the categories in seed/products.csv (preferred) or products_10000.csv**.
- **Output:** Predictions for all rows are written to `datasets/results/phase1/products_with_categories.csv` (with `predicted_main_category`, `predicted_sub_category`). That result file is what the backend uses to update the DB (see below).

### User categorization

| Purpose | File | Used for |
|--------|------|----------|
| **Products (for user features)** | `datasets/raw/products_10000.csv` | Product categories and metadata used to build user features (e.g. favorite category, avg price). |
| **Users** | `datasets/raw/users_5000.csv` | User list (id, etc.). |
| **Clicks** | `datasets/raw/user_clicks_interactions.csv` | Click interactions (wide or long: uid, product_id / pid1..pidN). |
| **Purchases** | `datasets/raw/user_purchase_interactions.csv` | Purchase interactions (wide or long). |
| **Visit time** | `datasets/raw/user_visits_time_interactions.csv` | Visit time per product (wide or long). |
| **Optional** | `datasets/raw/product_interaction_metadata.csv` | Product metadata. |

User categorization **learns** from these interactions + product categories to build features (clicks, purchases, visit time, category diversity, favorite category, etc.), then trains a Random Forest to assign each user a **segment** (e.g. `high_value`, `explorer`, `active_browser`, `category_loyal`). Results are written to `users_with_clusters.csv`; the backend reads that file to update `users.ml_category` (and related fields) in the DB.

---

## 3. For the first 10,000 products: which file updates the info?

Flow for the **10,000 products**:

```
1. INPUT (read-only)
   datasets/raw/products_10000.csv
   - Columns used: id, product_name, description, price, main_category, sub_category, ...
   - This file is NOT modified by Phase 1.
   - The model learns from the existing main_category and sub_category in this file.

2. PYTHON RUN (product categorization)
   - ProductCategorization loads products_10000.csv.
   - Trains XGBoost on (text + price) → (main_category || sub_category).
   - Predicts for all rows and writes:

3. RESULT CSV (written by Python, read by backend)
   datasets/results/phase1/products_with_categories.csv
   - Columns include: id, product_name, ..., predicted_main_category, predicted_sub_category.
   - This is the file that carries the “updated” category info for all 10,000 products.

4. BACKEND UPDATES POSTGRESQL (MLService)
   - MLService.readPhase1Results() reads products_with_categories.csv.
   - For each row: product_id = id, main_category = predicted_main_category, sub_category = predicted_sub_category.
   - MLService.updateProductsInDatabase() updates the products table:
     - products.category  = predicted_main_category
     - products.ml_category = predicted_main_category
     - products.sub_category = predicted_sub_category
     - products.updated_at = now
   - Matching is by product id; if not found, by product name.
```

So:

- **The file that holds the “updated” category info for the 10,000 products** is **`datasets/results/phase1/products_with_categories.csv`** (written by Phase 1 Python).
- **The file that Phase 1 learns from** for products is **`datasets/raw/products_10000.csv`** (never updated by Phase 1).
- **The database** is updated from **`products_with_categories.csv`** by the Java backend after Phase 1 runs.

---

## 4. Summary table

| Question | Answer |
|----------|--------|
| **Phase 1 design** | Two pipelines: (1) Product categorization (XGBoost), (2) User categorization (Random Forest). Python writes result CSVs; Java reads them and updates PostgreSQL. |
| **Where does Phase 1 learn (products)?** | From **`datasets/raw/products_10000.csv`**: uses existing `main_category` and `sub_category` as labels; features = product_name + description (TF-IDF) + price. |
| **Where does Phase 1 learn (users)?** | From **`datasets/raw/users_5000.csv`** and interaction files (**user_clicks_interactions.csv**, **user_purchase_interactions.csv**, **user_visits_time_interactions.csv**) plus product categories from **products_10000.csv**. |
| **For the first 10,000 products, from what file does it update the info?** | **Result file:** `datasets/results/phase1/products_with_categories.csv`. The backend reads this file and updates the `products` table (category, sub_category, ml_category, updated_at). The **source of the categories in that file** is the model trained on **`datasets/raw/products_10000.csv`**; the raw CSV itself is not updated. |

---

## 5. Where the backend looks for result files

- **Default:** `datasets/results/phase1/` under the project root (the same root that contains `src/phase1`).
- **Nested repo:** If the backend runs from a parent folder and the project is in a subfolder (e.g. `ML-eCommers-GitHub-15.2.26`), `MLService` also checks that subfolder’s `datasets/results/phase1/` for `users_with_clusters.csv` and `products_with_categories.csv`.

So after running Phase 1, the “single source of truth” for the 10,000 products’ updated categories is **`datasets/results/phase1/products_with_categories.csv`**; the DB is a copy of that info, updated by the backend.
