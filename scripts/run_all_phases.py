"""
Main script to run all phases of the E-Commerce Recommendation System
מריץ את כל השלבים של מערכת ההמלצות
"""

import sys
import subprocess
from pathlib import Path

# Get project root directory (parent of scripts/)
project_root = Path(__file__).resolve().parent.parent

# Add src directories to path so imports like "product_categorization" work
sys.path.insert(0, str(project_root / "src" / "phase1"))
sys.path.insert(0, str(project_root / "src" / "phase2"))
sys.path.insert(0, str(project_root / "src" / "phase3"))

def run_product_categorization():
    """Runs Product Categorization"""
    print("\n" + "="*80)
    print("Starting Product Categorization")
    print("="*80)
    
    from product_categorization import ProductCategorization  # type: ignore
    
    pc = ProductCategorization(str(project_root))
    results = pc.run_product_categorization()
    
    print("\nProduct Categorization completed successfully!")
    return results

def run_phase1():
    """Runs Phase 1: User Categorization"""
    print("\n" + "="*80)
    print("Starting Phase 1: User Categorization")
    print("="*80)
    
    from user_categorization import UserCategorization  # type: ignore
    
    uc = UserCategorization(str(project_root))
    results = uc.run_phase1()
    
    print("\nPhase 1 completed successfully!")
    return results

def run_phase2():
    """Runs Phase 2: Hybrid Recommendation System"""
    print("\n" + "="*80)
    print("Starting Phase 2: Hybrid Recommendation System")
    print("="*80)
    
    from recommendation_system_ml import RecommendationSystem  # type: ignore
    
    rec_system = RecommendationSystem(str(project_root))
    results = rec_system.run_phase2()
    
    print("\nPhase 2 completed successfully!")
    return results

def run_phase3():
    """Runs Phase 3: Single Item Categorization (interactive)"""
    print("\n" + "="*80)
    print("Starting Phase 3: Single Item Categorization")
    print("="*80)
    
    from single_item_categorization import SingleItemCategorization  # type: ignore
    
    categorizer = SingleItemCategorization(str(project_root))
    results = categorizer.run_phase3()
    
    print("\nPhase 3 completed successfully!")
    return results


def run_phase3_noninteractive():
    """Runs Phase 3 validation in non-interactive mode (e.g. when called from Java API).
    Validates the Phase 3 pipeline by categorizing sample users and products."""
    print("\n" + "="*80)
    print("Phase 3: Single Item Categorization (non-interactive validation)")
    print("="*80)
    
    from single_item_categorization import SingleItemCategorization  # type: ignore
    
    categorizer = SingleItemCategorization(str(project_root))
    results = []
    
    # Categorize sample user and product to validate pipeline
    for item_id in [1]:
        try:
            result = categorizer.run_phase3(item_id=item_id, item_type='auto', use_model=True)
            if result:
                results.append(result)
        except Exception as e:
            print(f"  Warning: Could not categorize item {item_id}: {e}")
    
    print("\nPhase 3 validation completed successfully!")
    return results

def main():
    """Main function to run phases based on command-line arguments"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Run E-Commerce ML Pipeline Phases')
    parser.add_argument('--phase1', action='store_true', help='Run Phase 1 (Product and User Categorization)')
    parser.add_argument('--phase2', action='store_true', help='Run Phase 2 (Recommendation System)')
    parser.add_argument('--phase3', action='store_true', help='Run Phase 3 (Single Item Categorization)')
    parser.add_argument('--all', action='store_true', help='Run all phases (default if no phase specified)')
    
    args = parser.parse_args()
    
    # If no specific phase is specified, run all phases (default behavior)
    should_run_phase1 = args.phase1 or args.all or (not args.phase2 and not args.phase3)
    should_run_phase2 = args.phase2 or args.all
    should_run_phase3 = args.phase3 or args.all
    
    print("="*80)
    print("E-Commerce Recommendation System - ML Pipeline")
    print("="*80)
    print(f"Phases to run: Phase 1={should_run_phase1}, Phase 2={should_run_phase2}, Phase 3={should_run_phase3}")
    print("="*80)
    
    phase1_results = None
    phase2_results = None
    phase3_results = None
    
    # Phase 1: Product and User Categorization
    if should_run_phase1:
        print("\nStep 1: Running Phase 1 (Product Categorization)...")
        product_results = run_product_categorization()
        
        print("\nStep 2: Running Phase 1 (User Categorization)...")
        user_results = run_phase1()
        
        # Combine Phase 1 results
        phase1_results = {
            'product_categorization': product_results,
            'user_categorization': user_results
        }
    
    # Phase 2: Recommendation System (depends on Phase 1)
    if should_run_phase2:
        if not should_run_phase1:
            print("\nWarning: Phase 2 depends on Phase 1. Running Phase 1 first...")
            if phase1_results is None:
                print("\nStep 1: Running Phase 1 (Product Categorization)...")
                product_results = run_product_categorization()
                
                print("\nStep 2: Running Phase 1 (User Categorization)...")
                user_results = run_phase1()  # This is the function, not the boolean
                
                phase1_results = {
                    'product_categorization': product_results,
                    'user_categorization': user_results
                }
        
        print("\nStep 3: Running Phase 2 (Recommendation System)...")
        phase2_results = run_phase2()
    
    # Phase 3 (optional)
    if should_run_phase3:
        try:
            import sys
            # In non-interactive mode (when called from Java/API), run Phase 3 validation without prompt
            if sys.stdin.isatty():
                print("\n" + "="*80)
                print("Phase 3: Single Item Categorization")
                print("="*80)
                print("Do you want to run Phase 3?")
                print("Enter 'y' for yes, 'n' for no, then press Enter:")
                sys.stdout.flush()
                run_phase3_choice = input().strip().lower()
                should_run_phase3 = run_phase3_choice == 'y' or run_phase3_choice == 'yes'
                if should_run_phase3:
                    print("\nStep 4: Running Phase 3 (Single Item Categorization)...")
                    phase3_results = run_phase3()
            else:
                # Non-interactive: run Phase 3 validation (categorize sample items to verify pipeline)
                print("\nStep 4: Running Phase 3 (Single Item Categorization - non-interactive validation)...")
                phase3_results = run_phase3_noninteractive()
        except (EOFError, KeyboardInterrupt):
            print("\nSkipping Phase 3 (non-interactive mode)")
        except Exception as e:
            print(f"\nError in Phase 3: {e}")
            print("Skipping Phase 3")
    
    print("\n" + "="*80)
    print("Completed phases successfully!")
    print("="*80)
    
    results = {}
    if phase1_results:
        results['phase1'] = phase1_results
    if phase2_results:
        results['phase2'] = phase2_results
    if phase3_results:
        results['phase3'] = phase3_results
    
    return results

if __name__ == "__main__":
    try:
        results = main()
    except Exception as e:
        print(f"\nError occurred: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

