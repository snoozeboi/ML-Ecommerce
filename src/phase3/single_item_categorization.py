"""
Phase 3: קטגוריזציה של משתמש יחיד או מוצר יחיד
Single User or Single Product Categorization
"""

import pandas as pd
import numpy as np
from pathlib import Path
import sys
import warnings
warnings.filterwarnings('ignore')

# Import from Phase 1 and Phase 2
sys.path.append(str(Path(__file__).parent.parent))
from phase1.user_categorization import UserCategorization
from phase1.product_categorization import ProductCategorization
from phase2.recommendation_system_ml import RecommendationSystem

class SingleItemCategorization:
    def __init__(self, data_path):
        """
        Initializes the SingleItemCategorization class
        
        Parameters:
        - data_path: Path to the data directory containing datasets
        
        What it does:
        - Creates instances of UserCategorization, ProductCategorization, and RecommendationSystem
        - These will be initialized when needed (lazy loading)
        """
        self.data_path = Path(data_path)
        
        # Instances (initialized when needed)
        self.user_categorizer = None
        self.product_categorizer = None
        self.recommendation_system = None
        
    def _initialize_user_categorizer(self):
        """Initializes UserCategorization if not already done"""
        if self.user_categorizer is None:
            self.user_categorizer = UserCategorization(str(self.data_path))
            self.user_categorizer.load_data()
            
            # Try to load saved model if not already loaded
            if self.user_categorizer.rf_model is None:
                try:
                    if hasattr(self.user_categorizer, 'load_model'):
                        self.user_categorizer.load_model()
                    else:
                        print("Warning: load_model() method not available. Model will be trained when needed.")
                except Exception as e:
                    print(f"Warning: Could not load saved model: {e}")
                    print("Will use rule-based categorization or train model when needed.")
    
    def _initialize_product_categorizer(self):
        """Initializes ProductCategorization if not already done. Uses demo catalog so single-product ML keeps good categories."""
        if self.product_categorizer is None:
            self.product_categorizer = ProductCategorization(str(self.data_path))
            self.product_categorizer.load_data(prefer_demo_catalog=True)
            self.product_categorizer.clean_data()
            
            # Try to train model if not already trained
            if self.product_categorizer.model is None:
                # Check if model was trained before (by checking if products_with_categories exists)
                products_with_categories_path = self.data_path / "datasets" / "results" / "phase1" / "products_with_categories.csv"
                if products_with_categories_path.exists():
                    # Model should be trained - try to train it
                    print("Training product categorization model...")
                    try:
                        self.product_categorizer.train_model()
                    except Exception as e:
                        print(f"Warning: Could not train product model: {e}")
                        print("Will use rule-based categorization instead.")
    
    def _initialize_recommendation_system(self):
        """Initializes RecommendationSystem if not already done"""
        if self.recommendation_system is None:
            self.recommendation_system = RecommendationSystem(str(self.data_path))
            self.recommendation_system.load_data()
            self.recommendation_system.prepare_tfidf_for_products()
            self.recommendation_system.create_user_interaction_matrix()
            self.recommendation_system.calculate_user_similarity()
    
    def categorize(self, item_id, item_type='auto', use_model=True):
        """
        Categorizes a single user or product based on the input
        
        What it does:
        1. Detects if item_id is a user_id or product_id (if item_type='auto')
        2. Calls appropriate categorization function
        3. Returns category information
        
        Parameters:
        - item_id: User ID or Product ID to categorize
        - item_type: 'auto', 'user', or 'product'
          * 'auto': Automatically detects if it's a user or product
          * 'user': Explicitly treat as user ID
          * 'product': Explicitly treat as product ID
        - use_model: If True, uses trained model. If False, uses rule-based categorization
        
        Returns:
        - Dictionary containing:
          * item_id: The ID that was categorized
          * item_type: 'user' or 'product'
          * category: Predicted category (string)
          * method: 'model' or 'rule_based'
          * details: Additional details (varies by item type)
        """
        print("="*60)
        print(f"Single Item Categorization - Item ID: {item_id}")
        print("="*60)
        
        # Auto-detect item type if needed
        if item_type == 'auto':
            item_type = self._detect_item_type(item_id)
            print(f"Detected item type: {item_type}")
        
        # Categorize based on type
        if item_type == 'user':
            return self._categorize_user(item_id, use_model)
        elif item_type == 'product':
            return self._categorize_product(item_id, use_model)
        else:
            raise ValueError(f"Unknown item_type: {item_type}. Must be 'user' or 'product'")
    
    def _detect_item_type(self, item_id):
        """
        Automatically detects if item_id is a user_id or product_id
        
        Parameters:
        - item_id: ID to check
        
        Returns:
        - 'user' or 'product'
        """
        # Try to load data to check
        try:
            # Check users first
            users_df = pd.read_csv(self.data_path / "datasets" / "raw" / "users_5000.csv")
            if item_id in users_df['id'].values:
                return 'user'
            
            # Check products
            products_df = pd.read_csv(self.data_path / "datasets" / "raw" / "products_10000.csv")
            if item_id in products_df['id'].values:
                return 'product'
            
            # If not found in either, check interactions
            # Users appear in interaction tables as 'uid'
            clicks_df = pd.read_csv(self.data_path / "datasets" / "raw" / "user_clicks_interactions.csv")
            if 'uid' in clicks_df.columns and item_id in clicks_df['uid'].values:
                return 'user'
            
            # Products appear in interaction tables as 'product_id'
            if 'product_id' in clicks_df.columns and item_id in clicks_df['product_id'].values:
                return 'product'
            
            # Default: assume user (more common in interactions)
            print(f"Warning: Could not determine item type for {item_id}. Assuming 'user'.")
            return 'user'
            
        except Exception as e:
            print(f"Warning: Error detecting item type: {e}")
            print("Assuming 'user' by default.")
            return 'user'
    
    def _categorize_user(self, user_id, use_model=True):
        """
        Categorizes a single user
        
        Parameters:
        - user_id: User ID to categorize
        - use_model: If True, uses trained model
        
        Returns:
        - Dictionary with user categorization results
        """
        print(f"\nCategorizing user: {user_id}")
        
        # Initialize user categorizer
        self._initialize_user_categorizer()
        
        # Categorize
        result = self.user_categorizer.categorize_single_user(user_id, use_model=use_model)
        
        # Format result
        return {
            'item_id': user_id,
            'item_type': 'user',
            'category': result['category'],
            'category_encoded': result.get('category_encoded'),
            'method': result['method'],
            'details': {
                'features': result['features'],
                'total_clicks': result['features'].get('total_clicks', 0),
                'total_purchases': result['features'].get('total_purchases', 0),
                'unique_products': result['features'].get('unique_products', 0),
                'engagement_score': result['features'].get('engagement_score', 0)
            }
        }
    
    def _categorize_product(self, product_id, use_model=True):
        """
        Categorizes a single product
        
        Parameters:
        - product_id: Product ID to categorize
        - use_model: If True, uses trained model
        
        Returns:
        - Dictionary with product categorization results
        """
        print(f"\nCategorizing product: {product_id}")
        
        # Initialize product categorizer
        self._initialize_product_categorizer()
        
        # Categorize
        result = self.product_categorizer.categorize_single_product(product_id, use_model=use_model)
        
        # Format result
        return {
            'item_id': product_id,
            'item_type': 'product',
            'category': result['predicted_category'],
            'main_category': result['predicted_main_category'],
            'sub_category': result['predicted_sub_category'],
            'method': result['method'],
            'details': {
                'product_name': result['product_info'].get('name', ''),
                'price': result['product_info'].get('price', 0),
                'description': result['product_info'].get('description', ''),
                'original_main_category': result['product_info'].get('main_category', ''),
                'original_sub_category': result['product_info'].get('sub_category', '')
            }
        }
    
    def categorize_batch(self, item_ids, item_type='auto', use_model=True):
        """
        Categorizes multiple users or products
        
        Parameters:
        - item_ids: List of user IDs or product IDs
        - item_type: 'auto', 'user', or 'product'
        - use_model: If True, uses trained model
        
        Returns:
        - List of categorization results (one per item)
        """
        print(f"\nCategorizing {len(item_ids)} items...")
        
        results = []
        for item_id in item_ids:
            try:
                result = self.categorize(item_id, item_type=item_type, use_model=use_model)
                results.append(result)
            except Exception as e:
                print(f"Error categorizing {item_id}: {e}")
                results.append({
                    'item_id': item_id,
                    'error': str(e)
                })
        
        return results
    
    def categorize_new_product(self, product_name, description, price, use_model=True):
        """
        Categorizes a NEW product (not in the dataset)
        
        Parameters:
        - product_name: Name of the product
        - description: Description of the product
        - price: Price of the product (float)
        - use_model: If True, uses trained model
        
        Returns:
        - Dictionary with categorization results
        """
        # Initialize product categorizer if needed
        self._initialize_product_categorizer()
        
        # Create a temporary product entry
        temp_product_id = 999999  # Temporary ID that won't conflict
        
        # Create a temporary DataFrame row
        temp_product = pd.DataFrame([{
            'id': temp_product_id,
            'product_name': str(product_name),
            'description': str(description),
            'price': float(price),
            'main_category': '',  # Will be predicted
            'sub_category': '',   # Will be predicted
            'category': ''        # Will be predicted
        }])
        
        # Temporarily add to products_df
        original_products_df = self.product_categorizer.products_df.copy()
        self.product_categorizer.products_df = pd.concat([self.product_categorizer.products_df, temp_product], ignore_index=True)
        
        try:
            # Clean data
            self.product_categorizer.clean_data()
            
            # Same name/description → use same category as existing product (so duplicates stay consistent)
            name_lower = str(product_name).strip().lower()
            desc_lower = (str(description) or '').strip().lower()
            orig = original_products_df
            if orig is not None and not orig.empty and 'product_name' in orig.columns:
                for _, row in orig.iterrows():
                    existing_name = (row.get('product_name') or '').strip().lower()
                    existing_desc = (row.get('description') or '').strip().lower()
                    if existing_name == name_lower and existing_desc == desc_lower:
                        main = row.get('main_category') or row.get('category') or ''
                        sub = row.get('sub_category') or ''
                        if main and main != 'Unknown':
                            self.product_categorizer.products_df = original_products_df
                            return {
                                'predicted_main_category': main,
                                'predicted_sub_category': sub,
                                'predicted_category': f"{main} || {sub}" if sub else main,
                                'method': 'match_by_name',
                                'product_info': {'name': product_name, 'description': description, 'price': price}
                            }
                # Fallback: same name only (e.g. same product name, different description)
                for _, row in orig.iterrows():
                    existing_name = (row.get('product_name') or '').strip().lower()
                    if existing_name == name_lower:
                        main = row.get('main_category') or row.get('category') or ''
                        sub = row.get('sub_category') or ''
                        if main and main != 'Unknown':
                            self.product_categorizer.products_df = original_products_df
                            return {
                                'predicted_main_category': main,
                                'predicted_sub_category': sub,
                                'predicted_category': f"{main} || {sub}" if sub else main,
                                'method': 'match_by_name',
                                'product_info': {'name': product_name, 'description': description, 'price': price}
                            }
            
            # Create combined_text if not exists (needed for categorization)
            if 'combined_text' not in self.product_categorizer.products_df.columns:
                self.product_categorizer.products_df['combined_text'] = (
                    self.product_categorizer.products_df['product_name'] + ' ' + 
                    self.product_categorizer.products_df['description'] + ' ' + 
                    self.product_categorizer.products_df['description']
                )
            
            # Categorize using the model
            result = self.product_categorizer.categorize_single_product(temp_product_id, use_model=use_model)
            
            # If we got Unknown, try to copy category from an existing product with same/similar name (e.g. duplicate "iPhone 16 Pro Max")
            pred_main = result.get('predicted_main_category', '') or ''
            pred_sub = result.get('predicted_sub_category', '') or ''
            if (pred_main.strip() == '' or pred_main == 'Unknown') and (product_name or '').strip():
                name_lower = str(product_name).strip().lower()
                orig = original_products_df
                if orig is not None and not orig.empty and 'product_name' in orig.columns:
                    found = False
                    for _, row in orig.iterrows():
                        existing_name = (row.get('product_name') or '').strip().lower()
                        if existing_name == name_lower:
                            main = row.get('main_category') or row.get('category') or ''
                            sub = row.get('sub_category') or ''
                            if main and main != 'Unknown':
                                result['predicted_main_category'] = main
                                result['predicted_sub_category'] = sub
                                result['predicted_category'] = f"{main} || {sub}" if sub else main
                                result['method'] = 'match_by_name'
                                found = True
                                break
                    if not found:
                        for _, row in orig.iterrows():
                            existing_name = (row.get('product_name') or '').strip().lower()
                            if existing_name and name_lower and (existing_name in name_lower or name_lower in existing_name):
                                main = row.get('main_category') or row.get('category') or ''
                                sub = row.get('sub_category') or ''
                                if main and main != 'Unknown':
                                    result['predicted_main_category'] = main
                                    result['predicted_sub_category'] = sub
                                    result['predicted_category'] = f"{main} || {sub}" if sub else main
                                    result['method'] = 'match_by_name'
                                    break
            
            # Remove temporary product
            self.product_categorizer.products_df = original_products_df
            
            # Update result with provided info
            result['product_info']['name'] = product_name
            result['product_info']['description'] = description
            result['product_info']['price'] = price
            
            return result
        except Exception as e:
            # Restore original products_df on error
            self.product_categorizer.products_df = original_products_df
            raise e
    
    def categorize_new_user(self, user_id, use_model=True):
        """
        Categorizes a NEW user (not in the dataset or with no interactions yet)
        
        Note: For a completely new user with no interactions, categorization will be
        based on default/zero features. This is useful for new users who just registered.
        
        Parameters:
        - user_id: User ID (will be added to dataset temporarily if needed)
        - use_model: If True, uses trained model
        
        Returns:
        - Dictionary with categorization results
        """
        # Initialize user categorizer if needed
        self._initialize_user_categorizer()
        
        # Check if user exists in dataset
        if self.user_categorizer.users_df is not None:
            user_exists = user_id in self.user_categorizer.users_df['id'].values
        else:
            user_exists = False
        
        if not user_exists:
            # Create temporary user entry
            temp_user = pd.DataFrame([{
                'id': user_id,
                'name': f'User_{user_id}',
                'email': f'user_{user_id}@example.com'
            }])
            
            if self.user_categorizer.users_df is None:
                self.user_categorizer.users_df = temp_user
            else:
                self.user_categorizer.users_df = pd.concat([self.user_categorizer.users_df, temp_user], ignore_index=True)
        
        # Categorize (will use zero/default features for new user)
        result = self.user_categorizer.categorize_single_user(user_id, use_model=use_model)
        
        return result
    
    def run_phase3(self, item_id=None, item_type='auto', use_model=True):
        """
        Runs Phase 3: Single Item Categorization
        
        What it does:
        - Interactive mode: Asks user to input new product or user details
        - If item_id is provided: Categorizes that specific existing item
        - If item_id is None: Interactive mode - asks for new item
        
        Parameters:
        - item_id: User ID or Product ID to categorize (optional)
        - item_type: 'auto', 'user', or 'product'
        - use_model: If True, uses trained model
        
        Returns:
        - Dictionary or list of categorization results
        """
        print("="*80)
        print("Phase 3: Single Item Categorization")
        print("="*80)
        
        if item_id is not None:
            # Categorize specific existing item
            result = self.categorize(item_id, item_type=item_type, use_model=use_model)
            
            # Print result
            print("\n" + "="*60)
            print("Categorization Result:")
            print("="*60)
            print(f"Item ID: {result['item_id']}")
            print(f"Item Type: {result['item_type']}")
            print(f"Category: {result['category']}")
            print(f"Method: {result['method']}")
            
            if result['item_type'] == 'user':
                print(f"\nUser Details:")
                print(f"  Total Clicks: {result['details']['total_clicks']}")
                print(f"  Total Purchases: {result['details']['total_purchases']}")
                print(f"  Unique Products: {result['details']['unique_products']}")
                print(f"  Engagement Score: {result['details']['engagement_score']:.2f}")
            else:
                print(f"\nProduct Details:")
                print(f"  Name: {result['details']['product_name']}")
                print(f"  Price: {result['details']['price']}")
                print(f"  Main Category: {result['main_category']}")
                print(f"  Sub Category: {result['sub_category']}")
            
            print("="*60)
            
            return result
        else:
            # Interactive mode - ask user what they want to check
            print("\n" + "="*60)
            print("Interactive Mode: Categorize & Get Recommendations")
            print("="*60)
            print("\nWhat would you like to check?")
            print("1. User (categorize user and get recommendations)")
            print("2. Product (categorize product)")
            print("3. Exit")
            
            try:
                import sys
                sys.stdout.flush()
                choice = input("\nEnter your choice (1/2/3): ").strip()
                
                if choice == '1':
                    # User - categorize and get recommendations
                    print("\n" + "-"*60)
                    print("Enter User ID:")
                    print("-"*60)
                    
                    sys.stdout.flush()
                    user_id_str = input("User ID: ").strip()
                    try:
                        user_id = int(user_id_str)
                    except ValueError:
                        print("Invalid user ID. Must be a number.")
                        return None
                    
                    print("\n" + "="*60)
                    print("Processing User...")
                    print("="*60)
                    
                    # Step 1: Categorize user
                    print("\nStep 1: Categorizing user...")
                    self._initialize_user_categorizer()
                    category_result = self.categorize(user_id, item_type='user', use_model=use_model)
                    
                    # Step 2: Get recommendations
                    print("\nStep 2: Getting recommendations...")
                    self._initialize_recommendation_system()
                    recommendations = self.recommendation_system.hybrid_recommendations(user_id, n_recommendations=5)
                    
                    # Get product names for recommendations
                    if self.recommendation_system.products_df is not None:
                        recommendation_names = []
                        for rec_id in recommendations:
                            product_row = self.recommendation_system.products_df[self.recommendation_system.products_df['id'] == rec_id]
                            if not product_row.empty:
                                recommendation_names.append(product_row.iloc[0].get('product_name', f'Product_{rec_id}'))
                            else:
                                recommendation_names.append(f'Product_{rec_id}')
                    else:
                        recommendation_names = [f'Product_{rec_id}' for rec_id in recommendations]
                    
                    # Print results
                    print("\n" + "="*60)
                    print("Results:")
                    print("="*60)
                    print(f"\nUser ID: {user_id}")
                    print(f"Category: {category_result['category']}")
                    print(f"Method: {category_result['method']}")
                    if 'details' in category_result:
                        print(f"\nUser Details:")
                        print(f"  Total Clicks: {category_result['details'].get('total_clicks', 0)}")
                        print(f"  Total Purchases: {category_result['details'].get('total_purchases', 0)}")
                        print(f"  Unique Products: {category_result['details'].get('unique_products', 0)}")
                        print(f"  Engagement Score: {category_result['details'].get('engagement_score', 0):.2f}")
                    
                    print(f"\nRecommendations ({len(recommendations)} products):")
                    for i, (rec_id, rec_name) in enumerate(zip(recommendations, recommendation_names), 1):
                        print(f"  {i}. {rec_name} (ID: {rec_id})")
                    
                    print("="*60)
                    
                    return {
                        'user_id': user_id,
                        'category': category_result['category'],
                        'category_method': category_result['method'],
                        'recommendations': recommendations,
                        'recommendation_names': recommendation_names,
                        'user_details': category_result.get('details', {})
                    }
                
                elif choice == '2':
                    # Product - categorize only
                    print("\n" + "-"*60)
                    print("Enter Product ID:")
                    print("-"*60)
                    
                    sys.stdout.flush()
                    product_id_str = input("Product ID: ").strip()
                    try:
                        product_id = int(product_id_str)
                    except ValueError:
                        print("Invalid product ID. Must be a number.")
                        return None
                    
                    print("\nCategorizing product...")
                    result = self.categorize(product_id, item_type='product', use_model=use_model)
                    
                    # Print result
                    print("\n" + "="*60)
                    print("Categorization Result:")
                    print("="*60)
                    print(f"Product ID: {result['item_id']}")
                    print(f"Product Name: {result['details']['product_name']}")
                    print(f"Price: ${result['details']['price']:.2f}")
                    print(f"Predicted Category: {result['category']}")
                    print(f"Main Category: {result['main_category']}")
                    print(f"Sub Category: {result['sub_category']}")
                    print(f"Method: {result['method']}")
                    print("="*60)
                    
                    return result
                
                elif choice == '3':
                    print("\nExiting Phase 3...")
                    return None
                
                else:
                    print("\nInvalid choice. Exiting...")
                    return None
                    
            except (EOFError, KeyboardInterrupt):
                print("\n\nExiting Phase 3...")
                return None
            except Exception as e:
                print(f"\nError: {e}")
                import traceback
                traceback.print_exc()
                return None

if __name__ == "__main__":
    from pathlib import Path
    # Get the project root directory
    project_root = Path(__file__).parent.parent.parent
    
    # Example usage
    categorizer = SingleItemCategorization(str(project_root))
    
    # Example 1: Categorize a user
    # user_result = categorizer.categorize(item_id=3, item_type='user', use_model=True)
    # print(f"User category: {user_result['category']}")
    
    # Example 2: Categorize a product
    # product_result = categorizer.categorize(item_id=1, item_type='product', use_model=True)
    # print(f"Product category: {product_result['category']}")
    
    # Example 3: Auto-detect and categorize
    # result = categorizer.categorize(item_id=3, item_type='auto', use_model=True)
    # print(f"Item type: {result['item_type']}, Category: {result['category']}")
    
    # Run Phase 3 with examples
    results = categorizer.run_phase3()
