# CSVs vs database, and when Phase 2 runs

## 1. Is there a CSV that gets updated with recent changes?

**Short answer:** Only **one** CSV is written from the app with “recent” data, and only when you **run Phase 1**.

| File | Who writes it | When | Purpose |
|------|----------------|------|--------|
| **`datasets/raw/current_products_for_phase1.csv`** | Backend (Java) | **Right before Phase 1 runs** (POST `/api/ml/phase1`) | Exports **current products from the DB** so Phase 1 can train on the live catalog. Overwritten each Phase 1 run. |
| **`datasets/results/phase1/products_with_categories.csv`** | Phase 1 (Python) | When Phase 1 runs | **Output** of product categorization; backend then reads it to update the DB. Not updated from DB on every change. |
| **`datasets/results/phase1/users_with_clusters.csv`** | Phase 1 (Python) | When Phase 1 runs | **Output** of user categorization; backend reads it to update `users.ml_category`. |
| **`datasets/raw/product_interaction_metadata.csv`** | **Not** updated by the app | — | You can sync it **manually** with `scripts/sync_metadata_categories.py` so `cat1`/`cat2` match your catalog (e.g. from seed or products_10000). |

So:

- **Products and users** in the app live in **PostgreSQL**. The app does **not** continuously write product/user changes to any CSV.
- The only CSV that reflects “recent” DB state is **`current_products_for_phase1.csv`**, and only at **Phase 1 run time** (backend exports current products, then Phase 1 runs).
- There is **no** CSV that is automatically updated on every product edit or new user; that’s all in the DB.

---

## 2. Do I need to run Phase 2 for ML to work? Does it learn from user interactions dynamically?

**Short answer:** You **do not** need to run the Phase 2 script for the live site. The ML service already gets data from the backend and updates from user interactions in real time.

### Two different “Phase 2” usages

| Usage | What it is | When / how |
|-------|------------|------------|
| **Phase 2 script (batch)** | `run_all_phases.py --phase2` or POST `/api/ml/phase2` | Reads **CSV files** (products_10000, users_5000, interaction CSVs, Phase 1 results), trains the full hybrid model, writes evaluation to `datasets/results/phase2/`. Used for **offline training and evaluation**. |
| **Phase 2 inside the ML service (live)** | Flask app (`backend/ml_service/app.py`) | Receives data via **POST `/data/load`** (full payload: products, users, interactions from the **DB**) and **POST `/interactions/record`** (single new view/cart/purchase). Uses the same Phase 2 code with **in-memory** data and **dynamic updates**. |

### How the live ML service gets its data

1. **Initial / periodic load:** When you request recommendations (or trending, similar, etc.), the backend calls **`/data/load`** (with a short TTL so it’s not every request). The payload is built from the **database** (products, users, cart, purchases, view history). So the ML service’s “source of truth” at runtime is the **DB**, not CSVs.
2. **Real-time interactions:** When a user views a product or adds to cart or purchases, the backend calls **`/interactions/record`**. The ML service appends that to its in-memory interactions and, if Phase 2 is available, calls **`update_interaction_dynamic(...)`**. So recommendations **do** update dynamically from user interactions.
3. **After a purchase,** the backend also calls **`forceReloadMLData()`**, which sends the full payload to `/data/load` again so the ML service sees the updated purchase history.

So:

- **You do not need to run Phase 2** (the script or `/api/ml/phase2`) for the site’s recommendations to work.
- The ML service **does** work and learn from interactions **dynamically**, using data from the DB and from `/interactions/record`. Running the Phase 2 script is optional (for evaluation / batch training); the live API uses the same logic with payload data and real-time updates.
