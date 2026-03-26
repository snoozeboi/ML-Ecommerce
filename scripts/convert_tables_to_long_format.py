"""
Script to convert interaction tables from Wide format to Long format
This allows the system to handle all 10000 products instead of just 10
"""

import pandas as pd
import numpy as np
from pathlib import Path

def convert_wide_to_long(wide_df, value_name):
    """
    Converts wide format interaction table to long format
    
    Parameters:
    - wide_df: DataFrame in wide format (uid, pid1, pid2, ..., pid10)
    - value_name: Name for the value column (e.g., 'clicks', 'purchases', 'visit_time')
    
    Returns:
    - DataFrame in long format (uid, product_id, value_name)
    """
    print(f"  Converting {value_name} table...")
    
    # Find all pid columns
    pid_columns = [col for col in wide_df.columns if col.startswith('pid')]
    if len(pid_columns) == 0:
        raise ValueError(f"No columns starting with 'pid' found in DataFrame")
    
    # Melt the dataframe: uid stays as identifier, pid columns become rows
    long_df = wide_df.melt(
        id_vars=['uid'],
        value_vars=pid_columns,
        var_name='product_col',
        value_name=value_name
    )
    
    # Extract product_id from pid1, pid2, etc. (pid1 -> 1, pid2 -> 2, etc.)
    long_df['product_id'] = long_df['product_col'].str.replace('pid', '').astype(int)
    
    # Remove rows with zero or negative values (no interaction)
    long_df = long_df[long_df[value_name] > 0]
    
    # Select and rename columns
    long_df = long_df[['uid', 'product_id', value_name]].copy()
    
    print(f"    Converted {len(wide_df)} users to {len(long_df)} interactions")
    print(f"    Unique products: {long_df['product_id'].nunique()}")
    print(f"    Product ID range: {long_df['product_id'].min()} - {long_df['product_id'].max()}")
    
    return long_df

def expand_interactions_to_all_products(long_df, all_product_ids, value_name):
    """
    Expands interactions to include all 10000 products
    For products not in original data, adds interactions with value 0 (or random small values)
    
    Parameters:
    - long_df: DataFrame in long format (uid, product_id, value_name)
    - all_product_ids: List of all product IDs (1-10000)
    - value_name: Name for the value column
    
    Returns:
    - Expanded DataFrame with all products
    """
    print(f"  Expanding {value_name} to all products...")
    
    # Get all unique users
    all_users = long_df['uid'].unique()
    
    # Create all combinations of users and products
    expanded_data = []
    
    for user_id in all_users:
        user_interactions = long_df[long_df['uid'] == user_id]
        user_product_ids = set(user_interactions['product_id'].unique())
        
        for product_id in all_product_ids:
            if product_id in user_product_ids:
                # Keep existing interaction
                value = user_interactions[user_interactions['product_id'] == product_id][value_name].iloc[0]
                expanded_data.append({
                    'uid': user_id,
                    'product_id': product_id,
                    value_name: value
                })
            else:
                # Add zero interaction (or skip if we only want non-zero)
                # For now, we'll skip zeros to keep the file size manageable
                pass
    
    expanded_df = pd.DataFrame(expanded_data)
    
    print(f"    Expanded from {len(long_df)} to {len(expanded_df)} interactions")
    print(f"    Unique products: {expanded_df['product_id'].nunique()}")
    
    return expanded_df

# Removed generate_realistic_interactions - we'll just convert what exists

def main():
    data_path = Path("datasets/raw")
    results_path = Path("datasets/raw")
    
    print("=" * 80)
    print("Converting interaction tables from Wide to Long format")
    print("=" * 80)
    
    # Load products to get all product IDs
    print("\nLoading products data...")
    products_df = pd.read_csv(data_path / "products_10000.csv")
    all_product_ids = sorted(products_df['id'].astype(int).unique().tolist())
    print(f"  Found {len(all_product_ids)} products (ID range: {min(all_product_ids)} - {max(all_product_ids)})")
    
    # Convert each table
    tables_to_convert = [
        ('user_clicks_interactions.csv', 'clicks'),
        ('user_purchase_interactions.csv', 'purchases'),
        ('user_visits_time_interactions.csv', 'visit_time')
    ]
    
    for filename, value_name in tables_to_convert:
        print(f"\n{'=' * 80}")
        print(f"Processing {filename}")
        print(f"{'=' * 80}")
        
        # Load wide format table
        wide_df = pd.read_csv(data_path / filename)
        print(f"  Loaded {len(wide_df)} users")
        
        # Convert to long format
        long_df = convert_wide_to_long(wide_df, value_name)
        
        # Save as backup (original Wide format)
        backup_path = data_path / f"{filename.replace('.csv', '_wide_backup.csv')}"
        wide_df.to_csv(backup_path, index=False)
        print(f"  Saved original Wide format to: {backup_path.name}")
        
        # Save new Long format (replaces original file)
        output_path = data_path / filename
        long_df.to_csv(output_path, index=False)
        print(f"  Saved Long format to: {filename}")
        print(f"  Final: {len(long_df)} interactions, {long_df['product_id'].nunique()} unique products")
    
    # Update product_interaction_metadata.csv
    print(f"\n{'=' * 80}")
    print("Updating product_interaction_metadata.csv")
    print(f"{'=' * 80}")
    
    metadata_path = data_path / "product_interaction_metadata.csv"
    if metadata_path.exists():
        # Load existing metadata
        existing_metadata = pd.read_csv(metadata_path)
        print(f"  Loaded {len(existing_metadata)} products from existing metadata")
        
        # Create metadata for all products
        all_metadata = []
        for product_id in all_product_ids:
            product_row = products_df[products_df['id'] == product_id]
            if len(product_row) > 0:
                # Get existing metadata if available
                existing_row = existing_metadata[existing_metadata['pid'] == product_id]
                
                if len(existing_row) > 0:
                    # Use existing metadata
                    row = existing_row.iloc[0].to_dict()
                else:
                    # Generate new metadata based on product data
                    row = {
                        'pid': product_id,
                        'clicks': int(product_row.iloc[0]['views'] * 0.1) if pd.notna(product_row.iloc[0]['views']) else 0,
                        'visit_time': int(product_row.iloc[0]['views'] * 0.5) if pd.notna(product_row.iloc[0]['views']) else 0,
                        'purchases': int(product_row.iloc[0]['views'] * 0.01) if pd.notna(product_row.iloc[0]['views']) else 0,
                    }
                    
                    # Get categories
                    if 'main_category' in product_row.columns and pd.notna(product_row.iloc[0]['main_category']):
                        row['cat1'] = product_row.iloc[0]['main_category']
                    elif 'predicted_main_category' in product_row.columns and pd.notna(product_row.iloc[0]['predicted_main_category']):
                        row['cat1'] = product_row.iloc[0]['predicted_main_category']
                    else:
                        row['cat1'] = 'Unknown'
                    
                    if 'sub_category' in product_row.columns and pd.notna(product_row.iloc[0]['sub_category']):
                        row['cat2'] = product_row.iloc[0]['sub_category']
                    elif 'predicted_sub_category' in product_row.columns and pd.notna(product_row.iloc[0]['predicted_sub_category']):
                        row['cat2'] = product_row.iloc[0]['predicted_sub_category']
                    else:
                        row['cat2'] = 'Unknown'
                
                all_metadata.append(row)
        
        new_metadata_df = pd.DataFrame(all_metadata)
        
        # Save backup
        backup_path = data_path / "product_interaction_metadata_backup.csv"
        existing_metadata.to_csv(backup_path, index=False)
        print(f"  Saved original metadata to: {backup_path.name}")
        
        # Save new metadata
        new_metadata_df.to_csv(metadata_path, index=False)
        print(f"  Saved updated metadata: {len(new_metadata_df)} products")
    else:
        print("  product_interaction_metadata.csv not found, skipping...")
    
    print(f"\n{'=' * 80}")
    print("Conversion completed successfully!")
    print(f"{'=' * 80}")

if __name__ == "__main__":
    main()

