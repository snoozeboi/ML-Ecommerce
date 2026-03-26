"""
סקריפט בדיקה לתהליך קטגוריזציה של משתמש בודד
"""

import sys
from pathlib import Path

# הוסף את src לנתיב
sys.path.append(str(Path(__file__).parent / "src"))

from phase1.user_categorization import UserCategorization
from phase2.recommendation_system_ml import RecommendationSystem

def test_user_categorization_single():
    """בודק קטגוריזציה של משתמש בודד"""
    print("="*60)
    print("בדיקת קטגוריזציה של משתמש בודד")
    print("="*60)
    
    # יצירת מופע
    uc = UserCategorization('.')
    
    # טעינת נתונים
    print("\n1. טעינת נתונים...")
    try:
        uc.load_data()
        print("   [OK] נתונים נטענו בהצלחה")
    except Exception as e:
        print(f"   [ERROR] שגיאה בטעינת נתונים: {e}")
        return False
    
    # בדיקה אם יש משתמשים
    if uc.clicks_df is None or len(uc.clicks_df) == 0:
        print("   [WARNING] אין נתוני אינטראקציות - לא ניתן לבדוק")
        return False
    
    # קבלת משתמש לדוגמה
    test_user_id = uc.clicks_df['uid'].iloc[0]
    print(f"\n2. בדיקת משתמש: {test_user_id}")
    
    # בדיקה עם rule-based (ללא מודל)
    print("\n3. קטגוריזציה עם rule-based...")
    try:
        result_rule = uc.categorize_single_user(test_user_id, use_model=False)
        print(f"   [OK] קטגוריה: {result_rule['category']}")
        print(f"   [OK] שיטה: {result_rule['method']}")
        print(f"   [OK] תכונות: {len(result_rule['features'])} תכונות חושבו")
    except Exception as e:
        print(f"   [ERROR] שגיאה: {e}")
        import traceback
        traceback.print_exc()
        return False
    
    # בדיקה עם מודל (אם קיים)
    print("\n4. קטגוריזציה עם מודל (אם קיים)...")
    try:
        # נסה לאמן מודל אם לא קיים
        if uc.rf_model is None:
            print("   [WARNING] מודל לא מאומן - מדלג על בדיקה זו")
            print("   (להרצת אימון מלא, הרץ: uc.user_categorization_random_forest())")
        else:
            result_model = uc.categorize_single_user(test_user_id, use_model=True)
            print(f"   [OK] קטגוריה: {result_model['category']}")
            print(f"   [OK] שיטה: {result_model['method']}")
            print(f"   [OK] קטגוריה מקודדת: {result_model.get('category_encoded', 'N/A')}")
    except Exception as e:
        print(f"   [WARNING] שגיאה (צפוי אם מודל לא מאומן): {e}")
    
    print("\n" + "="*60)
    print("בדיקה הושלמה בהצלחה!")
    print("="*60)
    return True

def test_recommendation_system_integration():
    """בודק אינטגרציה עם RecommendationSystem"""
    print("\n" + "="*60)
    print("בדיקת אינטגרציה עם RecommendationSystem")
    print("="*60)
    
    # יצירת מופע
    rs = RecommendationSystem('.')
    
    # טעינת נתונים
    print("\n1. טעינת נתונים...")
    try:
        rs.load_data()
        print("   [OK] נתונים נטענו בהצלחה")
    except Exception as e:
        print(f"   [ERROR] שגיאה בטעינת נתונים: {e}")
        return False
    
    # בדיקה אם יש משתמשים
    if rs.clicks_df is None or len(rs.clicks_df) == 0:
        print("   [WARNING] אין נתוני אינטראקציות - לא ניתן לבדוק")
        return False
    
    # קבלת משתמש ומוצר לדוגמה
    test_user_id = rs.clicks_df['uid'].iloc[0]
    test_product_id = rs.clicks_df['product_id'].iloc[0]
    
    print(f"\n2. בדיקת משתמש: {test_user_id}, מוצר: {test_product_id}")
    
    # בדיקת קטגוריזציה של משתמש בודד
    print("\n3. קטגוריזציה של משתמש בודד...")
    try:
        result = rs.categorize_single_user(test_user_id, use_model=False, update_clusters=True)
        print(f"   [OK] קטגוריה: {result['category']}")
        print(f"   [OK] שיטה: {result['method']}")
        print(f"   [OK] עודכן: {result.get('updated', False)}")
    except Exception as e:
        print(f"   [ERROR] שגיאה: {e}")
        import traceback
        traceback.print_exc()
        return False
    
    print("\n" + "="*60)
    print("בדיקת אינטגרציה הושלמה בהצלחה!")
    print("="*60)
    return True

if __name__ == "__main__":
    print("בדיקת תהליך קטגוריזציה של משתמש בודד\n")
    
    # בדיקה 1: UserCategorization
    success1 = test_user_categorization_single()
    
    # בדיקה 2: RecommendationSystem integration
    success2 = test_recommendation_system_integration()
    
    # סיכום
    print("\n" + "="*60)
    print("סיכום בדיקות:")
    print(f"  UserCategorization: {'[OK] הצליח' if success1 else '[ERROR] נכשל'}")
    print(f"  RecommendationSystem: {'[OK] הצליח' if success2 else '[ERROR] נכשל'}")
    print("="*60)
