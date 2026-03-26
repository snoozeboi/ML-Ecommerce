"""
E-Commerce Recommendation System - Phase 1: Product Categorization
מערכת קטגוריזציה של מוצרים באמצעות XGBoost (Optimized for Speed)
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.metrics import classification_report, accuracy_score
from pathlib import Path
import warnings
warnings.filterwarnings('ignore')

# XGBoost import
try:
    from xgboost import XGBClassifier
    XGBOOST_AVAILABLE = True
except ImportError:
    XGBOOST_AVAILABLE = False
    print("Warning: XGBoost not available. Please install with: pip install xgboost")

class ProductCategorization:
    def __init__(self, data_path):
        """
        Initializes the ProductCategorization class
        
        Parameters:
        - data_path: Path to the data directory containing datasets
        """
        self.data_path = Path(data_path)
        self.products_df = None
        self.model = None
        self.tfidf_vectorizer = None
        self.price_scaler = None
        self.label_encoder = None  # LabelEncoder להמרת קטגוריות למספרים
        self.products_with_categories = None
        
    def load_data(self, prefer_demo_catalog=False):
        """
        Loads product data from CSV file.
        
        When prefer_demo_catalog=False (e.g. Phase 1 batch):
          Priority: current_products_for_phase1 → seed → products_10000
          Use live DB export when available for batch training.
        When prefer_demo_catalog=True (e.g. ml_service single-product categorization):
          Priority: products_10000 → seed → current_products_for_phase1
          Use demo/curated categories as reference so new products get consistent categorization
          and we don't overwrite good categories with Phase 1 export.
        
        product_interaction_metadata.csv is NEVER used - its cat1/cat2 can be wrong.
        """
        print("Loading product data...")
        
        current_path = self.data_path / "datasets" / "raw" / "current_products_for_phase1.csv"
        seed_path = self.data_path / "backend" / "src" / "main" / "resources" / "seed" / "products.csv"
        products_10000_path = self.data_path / "datasets" / "raw" / "products_10000.csv"
        
        if prefer_demo_catalog:
            # Single-product ML: use demo catalog so we keep good categories and only categorize new products
            if products_10000_path.exists():
                products_path = products_10000_path
                print("  Using products_10000.csv (demo reference for single-product ML)")
            elif seed_path.exists():
                products_path = seed_path
                print("  Using seed/products.csv (demo reference)")
            elif current_path.exists():
                products_path = current_path
                print("  Using current_products_for_phase1.csv (fallback)")
            else:
                products_path = products_10000_path  # will raise FileNotFoundError with full message
        else:
            # Phase 1 batch: use live catalog when available
            if current_path.exists():
                products_path = current_path
                print("  Using current_products_for_phase1.csv (live catalog from DB)")
            elif seed_path.exists():
                products_path = seed_path
                print("  Using seed/products.csv (initial demo)")
            else:
                products_path = products_10000_path
                print("  Using products_10000.csv (static fallback)")
        
        if not products_path.exists():
            raise FileNotFoundError(
                f"Product data file not found.\n"
                f"Tried: {current_path}\n"
                f"Tried: {seed_path}\n"
                f"Tried: {products_10000_path}\n"
                f"Please run Phase 1 after seeding DB, or ensure seed/products.csv or products_10000.csv exists."
            )
        
        try:
            # Read CSV and handle BOM/encoding issues
            self.products_df = pd.read_csv(products_path, encoding='utf-8-sig')
            if self.products_df.empty:
                raise ValueError("products_10000.csv is empty")
            
            # Clean column names (remove BOM, quotes, whitespace)
            self.products_df.columns = self.products_df.columns.str.strip().str.replace('"', '').str.replace('\ufeff', '')
            
            # Ensure 'id' column exists (check for variations)
            if 'id' not in self.products_df.columns:
                # Try to find id column with different names
                for col in self.products_df.columns:
                    if col.lower().strip().replace('"', '') == 'id':
                        self.products_df.rename(columns={col: 'id'}, inplace=True)
                        break
                else:
                    raise ValueError(f"Could not find 'id' column in products CSV. Available columns: {list(self.products_df.columns)}")
            
            print(f"  Loaded {len(self.products_df)} products")
        except Exception as e:
            raise ValueError(f"Error loading products file: {e}")
    
    def clean_data(self):
        """
        Cleans the product data
        
        What it does:
        - Fills missing text fields with empty strings
        - Fills missing categories with 'Unknown'
        - Removes products with no name or description
        """
        print("\nCleaning product data...")
        
        # Fill missing text fields with empty strings
        self.products_df['product_name'] = self.products_df['product_name'].fillna('')
        self.products_df['description'] = self.products_df['description'].fillna('')
        
        # Fill missing categories with 'Unknown'
        self.products_df['main_category'] = self.products_df['main_category'].fillna('Unknown')
        self.products_df['sub_category'] = self.products_df['sub_category'].fillna('Unknown')
        
        # Remove completely empty products
        initial_size = len(self.products_df)
        self.products_df = self.products_df[~((self.products_df['product_name'].str.strip() == '') & 
                                               (self.products_df['description'].str.strip() == ''))]
        removed = initial_size - len(self.products_df)
        if removed > 0:
            print(f"  Removed {removed} empty products")
        print(f"  Final dataset size: {len(self.products_df)}")
    
    def prepare_features(self):
        """
        Prepares features for machine learning
        
        What it does:
        - Combines product_name and description into combined_text
        - Creates combined_category (main_category || sub_category)
        - Separates features (X) and target (y)
        
        Returns:
        - X_text: Text features (product names and descriptions)
        - X_price: Price features
        - y: Target categories
        """
        print("\nPreparing features for ML...")
        
        # Combine text features
        self.products_df['combined_text'] = (self.products_df['product_name'] + ' ' + 
                                            self.products_df['description'] + ' ' + 
                                            self.products_df['description'])
        
        # Create combined target feature
        self.products_df['combined_category'] = (self.products_df['main_category'] + ' || ' + 
                                                 self.products_df['sub_category'])
        
        # Separate features and target
        X_text = self.products_df['combined_text']
        X_price = self.products_df['price']
        y = self.products_df['combined_category']
        
        print(f"  Text features: {X_text.shape}")
        print(f"  Price features: {X_price.shape}")
        print(f"  Target categories: {y.nunique()} unique categories")
        
        return X_text, X_price, y
    
    def train_model(self):
        """
        Trains XGBoost model for product categorization (optimized for speed)
        
        What it does:
        1. Prepares features
        2. Splits data into train/test
        3. Converts text to numbers using TF-IDF
        4. Trains XGBoost model (optimized for fast training)
        5. Evaluates the model
        
        Returns:
        - Dictionary with accuracy metrics
        """
        if not XGBOOST_AVAILABLE:
            raise ImportError(
                "XGBoost is not installed. Please install it with: pip install xgboost"
            )
        
        print("\n" + "="*60)
        print("Product Categorization - XGBoost (Optimized for Speed)")
        print("="*60)
        
        # Prepare features
        X_text, X_price, y = self.prepare_features()
        
        # Remove rare categories for stratification
        category_counts = y.value_counts()
        rare_categories = category_counts[category_counts < 2]
        if len(rare_categories) > 0:
            print(f"\nRemoving {len(rare_categories)} rare category combinations...")
            mask = ~y.isin(rare_categories.index)
            X_text = X_text[mask]
            X_price = X_price[mask]
            y = y[mask]
            self.products_df = self.products_df[mask].reset_index(drop=True)
        
        # Combine for splitting
        X_combined = pd.DataFrame({'text': X_text, 'price': X_price})
        
        # Split data
        print("\nSplitting data into train/test sets...")
        try:
            X_train, X_test, y_train, y_test = train_test_split(
                X_combined, y,
                test_size=0.20,
                random_state=42,
                stratify=y
            )
            print("  Successfully stratified by combined category")
        except ValueError:
            print("  Could not stratify by combined category, using main_category...")
            main_cat = self.products_df['main_category']
            X_train, X_test, y_train, y_test = train_test_split(
                X_combined, y,
                test_size=0.20,
                random_state=42,
                stratify=main_cat
            )
        
        print(f"  Training set: {len(X_train)} products")
        print(f"  Test set: {len(X_test)} products")
        
        # Feature engineering - TF-IDF (optimized for speed + accuracy)
        print("\nConverting text to numbers using TF-IDF...")
        self.tfidf_vectorizer = TfidfVectorizer(
            max_features=1200,         # איזון טוב: 1200 תכונות (יותר מ-1000 לדיוק, פחות מ-1500 למהירות)
            ngram_range=(1, 2),
            min_df=2,                  # הקטנו מ-3 ל-2 (יותר תכונות = דיוק טוב יותר)
            max_df=0.90,
            stop_words='english',
            sublinear_tf=True          # שימוש ב-log scale (שיפור דיוק קטן)
        )
        
        # שמירה על sparse matrices (מהיר יותר!) - XGBoost תומך ב-sparse
        X_train_text_tfidf = self.tfidf_vectorizer.fit_transform(X_train['text'])
        X_test_text_tfidf = self.tfidf_vectorizer.transform(X_test['text'])
        
        # Scale price
        print("Scaling price feature...")
        self.price_scaler = StandardScaler()
        X_train_price_scaled = self.price_scaler.fit_transform(X_train['price'].values.reshape(-1, 1))
        X_test_price_scaled = self.price_scaler.transform(X_test['price'].values.reshape(-1, 1))
        
        # Combine features (using sparse matrices for speed)
        from scipy.sparse import hstack as sparse_hstack
        X_train_combined = sparse_hstack([X_train_text_tfidf, X_train_price_scaled])
        X_test_combined = sparse_hstack([X_test_text_tfidf, X_test_price_scaled])
        
        # Convert string labels to numeric labels for XGBoost
        print("\nEncoding category labels to numbers...")
        self.label_encoder = LabelEncoder()
        y_train_encoded = self.label_encoder.fit_transform(y_train)
        y_test_encoded = self.label_encoder.transform(y_test)
        print(f"  Encoded {len(self.label_encoder.classes_)} unique categories")
        
        # Train model (XGBoost optimized for speed + accuracy - improved)
        print("\nTraining XGBoost model (optimized for speed + accuracy)...")
        self.model = XGBClassifier(
            n_estimators=60,           # איזון: 60 עצים (יותר מ-50 לדיוק, פחות מ-100 למהירות)
            max_depth=5,               # עומק בינוני (5 במקום 4) = דיוק טוב יותר, עדיין מהיר
            learning_rate=0.15,        # learning rate בינוני (0.15) = איזון טוב
            subsample=0.85,            # 85% מהנתונים (איזון)
            colsample_bytree=0.85,    # 85% מהתכונות (איזון)
            reg_alpha=0.1,             # L1 regularization
            reg_lambda=1.0,            # L2 regularization
            tree_method='hist',        # שיטת בניית עץ מהירה ביותר
            max_bin=256,               # הקטנת bins (מהיר יותר, עדיין מדויק)
            grow_policy='lossguide',   # בניית עץ לפי loss (מהיר יותר מ-depthwise)
            random_state=42,
            n_jobs=-1,                 # שימוש בכל הליבות
            eval_metric='mlogloss',    # מטריקה למולטי-קלאס
            verbosity=0                # ללא פלט (מהיר יותר)
        )
        self.model.fit(X_train_combined, y_train_encoded)
        
        # Evaluate
        print("\nEvaluating model...")
        y_pred_encoded = self.model.predict(X_test_combined)
        # Convert predictions back to category strings
        y_pred = self.label_encoder.inverse_transform(y_pred_encoded)
        accuracy_combined = accuracy_score(y_test, y_pred)
        
        # Split predictions for separate evaluation
        y_test_main = y_test.str.split(' || ').str[0]
        y_test_sub = y_test.str.split(' || ').str[1]
        y_pred_main = pd.Series(y_pred).str.split(' || ').str[0]
        y_pred_sub = pd.Series(y_pred).str.split(' || ').str[1]
        
        accuracy_main = accuracy_score(y_test_main, y_pred_main)
        accuracy_sub = accuracy_score(y_test_sub, y_pred_sub)
        
        print(f"\nFinal Metrics:")
        print(f"  Combined Category Accuracy: {accuracy_combined:.4f} ({accuracy_combined*100:.2f}%)")
        print(f"  Main Category Accuracy: {accuracy_main:.4f} ({accuracy_main*100:.2f}%)")
        print(f"  Sub Category Accuracy: {accuracy_sub:.4f} ({accuracy_sub*100:.2f}%)")
        
        return {
            'accuracy_combined': accuracy_combined,
            'accuracy_main': accuracy_main,
            'accuracy_sub': accuracy_sub
        }
    
    def categorize_all_products(self):
        """
        Categorizes all products using the trained model
        
        Returns:
        - DataFrame with products and predicted categories
        """
        if self.model is None:
            raise ValueError("Model not trained. Call train_model() first.")
        
        if self.label_encoder is None:
            raise ValueError("Label encoder not initialized. Call train_model() first.")
        
        print("\nCategorizing all products...")
        
        # Prepare features for all products
        X_text = self.products_df['combined_text']
        X_price = self.products_df['price']
        
        # Transform (using sparse matrices for speed)
        X_text_tfidf = self.tfidf_vectorizer.transform(X_text)
        X_price_scaled = self.price_scaler.transform(X_price.values.reshape(-1, 1))
        from scipy.sparse import hstack as sparse_hstack
        X_combined = sparse_hstack([X_text_tfidf, X_price_scaled])
        
        # Predict (returns numeric labels)
        y_pred_encoded = self.model.predict(X_combined)
        # Convert predictions back to category strings
        y_pred = self.label_encoder.inverse_transform(y_pred_encoded)
        
        # Add predictions to DataFrame
        self.products_df['predicted_category'] = y_pred
        
        # Split combined category into main and sub categories
        # Use apply with Python's split() method - this is more reliable
        def split_category(cat_str):
            """Split category string into main and sub"""
            if pd.isna(cat_str) or not isinstance(cat_str, str):
                return '', ''
            parts = cat_str.split(' || ', 1)  # Split at most once
            main = parts[0] if len(parts) > 0 else ''
            sub = parts[1] if len(parts) > 1 else ''
            return main, sub
        
        # Apply split function to get main and sub categories
        split_results = pd.Series(y_pred, index=self.products_df.index).apply(split_category)
        self.products_df['predicted_main_category'] = split_results.apply(lambda x: x[0])
        self.products_df['predicted_sub_category'] = split_results.apply(lambda x: x[1])
        
        # Verify predictions were saved correctly
        if len(self.products_df) > 0:
            sample_pred = self.products_df['predicted_category'].iloc[0]
            sample_main = self.products_df['predicted_main_category'].iloc[0]
            sample_sub = self.products_df['predicted_sub_category'].iloc[0]
            print(f"  Sample prediction: '{sample_pred}' -> Main: '{sample_main}', Sub: '{sample_sub}'")
        
        self.products_with_categories = self.products_df.copy()
        
        print(f"  Categorized {len(self.products_df)} products")
        
        return self.products_with_categories
    
    def get_tfidf_matrix_for_descriptions(self, max_features=100):
        """
        Creates TF-IDF matrix for product descriptions (for content-based recommendations)
        
        This is separate from the categorization TF-IDF, optimized for recommendations:
        - Uses only descriptions (not combined text)
        - Fewer features for faster similarity calculations
        - Optimized parameters for recommendation use case
        
        Parameters:
        - max_features: Maximum number of features (default: 100)
        
        Returns:
        - TF-IDF matrix (sparse matrix) for product descriptions
        - TfidfVectorizer instance used
        """
        if self.products_df is None:
            raise ValueError("products_df is not loaded. Call load_data() first.")
        
        if 'description' not in self.products_df.columns:
            raise ValueError("products_df must contain 'description' column")
        
        print("Creating TF-IDF matrix for product descriptions (for recommendations)...")
        
        # Create a separate TF-IDF vectorizer optimized for recommendations
        from sklearn.feature_extraction.text import TfidfVectorizer
        
        recommendation_tfidf = TfidfVectorizer(
            max_features=max_features,
            stop_words='english',
            ngram_range=(1, 2),  # unigrams and bigrams
            min_df=2,  # word must appear in at least 2 products
            max_df=0.95  # word must not appear in more than 95% of products
        )
        
        # Fill missing descriptions with empty string
        descriptions = self.products_df['description'].fillna('')
        
        # Create TF-IDF matrix
        tfidf_matrix = recommendation_tfidf.fit_transform(descriptions)
        
        print(f"Created TF-IDF matrix: {tfidf_matrix.shape}")
        print(f"  - {tfidf_matrix.shape[0]} products")
        print(f"  - {tfidf_matrix.shape[1]} features (words/phrases)")
        
        return tfidf_matrix, recommendation_tfidf
    
    def categorize_single_product(self, product_id, use_model=True):
        """
        Categorizes a single product and returns its category
        
        What it does:
        1. Finds the product in the dataset
        2. Prepares features for the product (text + price)
        3. Uses the trained XGBoost model to predict category (if use_model=True and model is trained)
        4. OR uses rule-based categorization (if use_model=False or model not trained)
        
        Parameters:
        - product_id: Product ID to categorize
        - use_model: If True, uses trained XGBoost model. If False, uses rule-based categorization
        
        Returns:
        - Dictionary containing:
          * product_id: Product ID
          * predicted_category: Predicted combined category (string)
          * predicted_main_category: Predicted main category (string)
          * predicted_sub_category: Predicted sub category (string)
          * method: 'model' or 'rule_based'
          * product_info: Dictionary with product details (name, price, etc.)
        """
        # Check if data is loaded
        if self.products_df is None:
            raise ValueError("Data not loaded. Call load_data() first.")
        
        # Clean data if not already done
        if 'combined_text' not in self.products_df.columns:
            self.clean_data()
        
        # Find the product
        product_row = self.products_df[self.products_df['id'] == product_id]
        if len(product_row) == 0:
            raise ValueError(f"Product {product_id} not found in dataset")
        
        product_row = product_row.iloc[0]
        
        # Get product info
        product_info = {
            'id': product_id,
            'name': product_row.get('product_name', ''),
            'price': product_row.get('price', 0),
            'description': product_row.get('description', ''),
            'main_category': product_row.get('main_category', ''),
            'category': product_row.get('category', ''),
            'sub_category': product_row.get('sub_category', '')
        }
        
        # Try to use model if available and use_model=True
        if use_model and self.model is not None and self.tfidf_vectorizer is not None and self.price_scaler is not None:
            try:
                # Prepare features for single product
                # Ensure combined_text exists
                if 'combined_text' not in self.products_df.columns:
                    # Create combined_text if not exists
                    product_name = str(product_row.get('product_name', ''))
                    description = str(product_row.get('description', ''))
                    combined_text = f"{product_name} {description} {description}"  # Double description like in prepare_features
                else:
                    combined_text = product_row['combined_text']
                
                # Transform text to TF-IDF
                X_text_tfidf = self.tfidf_vectorizer.transform([combined_text])
                
                # Transform price
                price_value = float(product_row.get('price', 0))
                X_price_scaled = self.price_scaler.transform([[price_value]])
                
                # Combine features
                from scipy.sparse import hstack as sparse_hstack
                X_combined = sparse_hstack([X_text_tfidf, X_price_scaled])
                
                # Predict
                y_pred_encoded = self.model.predict(X_combined)
                predicted_category = self.label_encoder.inverse_transform(y_pred_encoded)[0]
                
                # Split combined category into main and sub
                if ' || ' in predicted_category:
                    parts = predicted_category.split(' || ')
                    predicted_main_category = parts[0] if len(parts) > 0 else predicted_category
                    predicted_sub_category = parts[1] if len(parts) > 1 else ''
                else:
                    predicted_main_category = predicted_category
                    predicted_sub_category = ''
                
                return {
                    'product_id': product_id,
                    'predicted_category': predicted_category,
                    'predicted_main_category': predicted_main_category,
                    'predicted_sub_category': predicted_sub_category,
                    'method': 'model',
                    'product_info': product_info
                }
            except Exception as e:
                print(f"Warning: Could not use model for product {product_id}: {e}")
                print("Falling back to rule-based categorization...")
                use_model = False
        
        # Rule-based categorization (fallback or if use_model=False)
        # Use existing categories if available
        main_category = product_row.get('main_category', '')
        sub_category = product_row.get('sub_category', '')
        category = product_row.get('category', '')
        
        if main_category and sub_category:
            predicted_category = f"{main_category} || {sub_category}"
            predicted_main_category = main_category
            predicted_sub_category = sub_category
        elif main_category:
            predicted_category = main_category
            predicted_main_category = main_category
            predicted_sub_category = ''
        elif category:
            predicted_category = category
            predicted_main_category = category
            predicted_sub_category = ''
        else:
            predicted_category = 'Unknown'
            predicted_main_category = 'Unknown'
            predicted_sub_category = ''
        
        return {
            'product_id': product_id,
            'predicted_category': predicted_category,
            'predicted_main_category': predicted_main_category,
            'predicted_sub_category': predicted_sub_category,
            'method': 'rule_based',
            'product_info': product_info
        }
    
    def save_results(self):
        """
        Saves product categorization results to CSV files
        
        Returns:
        - output_path: Path where results were saved
        """
        print("\nSaving product categorization results...")
        
        output_path = self.data_path / "datasets" / "results" / "phase1"
        output_path.mkdir(parents=True, exist_ok=True)
        
        if self.products_with_categories is None:
            raise ValueError("No categorized products. Call categorize_all_products() first.")
        
        # Save products with categories
        products_file = output_path / "products_with_categories.csv"
        
        # Handle case where file might be open in another program
        try:
            self.products_with_categories.to_csv(products_file, index=False)
            print(f"  Saved: {products_file.name}")
        except PermissionError:
            print(f"  Warning: Could not save {products_file.name} - file may be open in another program")
            print(f"  Please close the file and try again, or the file will be saved on next run")
            # Try to save with a different name as backup
            backup_file = output_path / "products_with_categories_backup.csv"
            try:
                self.products_with_categories.to_csv(backup_file, index=False)
                print(f"  Saved backup to: {backup_file.name}")
            except Exception as e:
                print(f"  Error saving backup: {e}")
            raise
        
        return output_path
    
    def run_product_categorization(self):
        """
        Runs the complete product categorization pipeline
        
        Returns:
        - Dictionary with results and metrics
        """
        print("="*80)
        print("Phase 1: Product Categorization")
        print("="*80)
        
        # Load data
        self.load_data()
        
        # Clean data
        self.clean_data()
        
        # Train model
        metrics = self.train_model()
        
        # Categorize all products
        products_with_categories = self.categorize_all_products()
        
        # Save results
        output_path = self.save_results()
        
        print(f"\n" + "="*80)
        print("Product Categorization completed successfully!")
        print("="*80)
        print(f"Products: {len(products_with_categories)} categorized")
        print(f"Accuracy: {metrics['accuracy_combined']:.4f} ({metrics['accuracy_combined']*100:.2f}%)")
        print(f"Results saved to: {output_path}")
        print("="*80)
        
        return {
            'products_with_categories': products_with_categories,
            'metrics': metrics,
            'output_path': output_path
        }

if __name__ == "__main__":
    import os
    # Get the project root directory (parent of src)
    project_root = Path(__file__).parent.parent.parent
    pc = ProductCategorization(str(project_root))
    results = pc.run_product_categorization()

