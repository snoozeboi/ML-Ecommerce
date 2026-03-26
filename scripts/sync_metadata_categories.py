"""
Sync product_interaction_metadata.csv categories (cat1, cat2) from the demo product source.
Uses datasets/raw/products_10000.csv (or seed/products.csv) so metadata matches the catalog.
Preserves existing clicks, visit_time, purchases per row; only updates categories.
Run once after seeding, or whenever you want metadata categories to match the product list.
"""

import pandas as pd
from pathlib import Path

def main():
    root = Path(__file__).resolve().parent.parent
    raw = root / "datasets" / "raw"
    metadata_path = raw / "product_interaction_metadata.csv"
    products_10000_path = raw / "products_10000.csv"
    seed_path = root / "backend" / "src" / "main" / "resources" / "seed" / "products.csv"

    if not metadata_path.exists():
        print(f"Metadata not found: {metadata_path}")
        return

    # Prefer products_10000 (same order as seed), fallback to seed
    if products_10000_path.exists():
        products_df = pd.read_csv(products_10000_path, encoding='utf-8-sig')
        products_df.columns = products_df.columns.str.strip().str.replace('"', '').str.replace('\ufeff', '')
        print(f"Using {products_10000_path.name} ({len(products_df)} products)")
    elif seed_path.exists():
        products_df = pd.read_csv(seed_path, encoding='utf-8-sig')
        products_df.columns = products_df.columns.str.strip().str.replace('"', '').str.replace('\ufeff', '')
        if 'id' not in products_df.columns and products_df.columns[0].lower().replace('"', '').strip() == 'id':
            products_df = products_df.rename(columns={products_df.columns[0]: 'id'})
        print(f"Using seed/products.csv ({len(products_df)} products)")
    else:
        print("No product source found (products_10000.csv or seed/products.csv)")
        return

    existing = pd.read_csv(metadata_path)
    print(f"Loaded metadata: {len(existing)} rows")

    main_cat = 'main_category' if 'main_category' in products_df.columns else 'category'
    sub_cat = 'sub_category' if 'sub_category' in products_df.columns else 'sub_category'
    if main_cat not in products_df.columns:
        main_cat = [c for c in products_df.columns if 'category' in c.lower() or 'main' in c.lower()]
        main_cat = main_cat[0] if main_cat else None
    if not main_cat:
        print("Product source has no category column")
        return

    # Build new metadata: one row per product by order (pid = 1-based index)
    rows = []
    for i in range(len(products_df)):
        pid = i + 1
        row = products_df.iloc[i]
        cat1 = str(row.get(main_cat, 'Unknown')).strip() if pd.notna(row.get(main_cat)) else 'Unknown'
        cat2 = str(row.get(sub_cat, 'Unknown')).strip() if pd.notna(row.get(sub_cat)) else 'Unknown'

        old = existing[existing['pid'] == pid]
        if len(old) > 0:
            clicks = int(old.iloc[0].get('clicks', 0))
            visit_time = int(old.iloc[0].get('visit_time', 0))
            purchases = int(old.iloc[0].get('purchases', 0))
        else:
            clicks = visit_time = purchases = 0

        rows.append({'pid': pid, 'clicks': clicks, 'visit_time': visit_time, 'purchases': purchases, 'cat1': cat1, 'cat2': cat2})

    out = pd.DataFrame(rows)

    # If original had more rows, append them with Unknown categories (preserve clicks/visit_time/purchases)
    if len(existing) > len(out):
        extra = existing[existing['pid'] > len(out)]
        extra_rows = [{
            'pid': int(r['pid']),
            'clicks': int(r.get('clicks', 0)),
            'visit_time': int(r.get('visit_time', 0)),
            'purchases': int(r.get('purchases', 0)),
            'cat1': 'Unknown',
            'cat2': 'Unknown'
        } for _, r in extra.iterrows()]
        out = pd.concat([out, pd.DataFrame(extra_rows)], ignore_index=True)

    backup_path = raw / "product_interaction_metadata_backup.csv"
    existing.to_csv(backup_path, index=False)
    print(f"Backup saved: {backup_path.name}")

    out.to_csv(metadata_path, index=False)
    print(f"Updated {metadata_path.name} ({len(out)} rows); cat1/cat2 from product source")
    print("Sample (row 16):", out.iloc[15].to_dict())

if __name__ == "__main__":
    main()
