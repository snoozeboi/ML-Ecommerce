# Phase 3: Single Item Categorization - הוראות שימוש

## סקירה כללית

Phase 3 מאפשר קטגוריזציה של **משתמש יחיד** או **מוצר יחיד** - תהליך מהיר לשימוש יומיומי.

## תכונות

- ✅ **זיהוי אוטומטי** - מזהה אם הקלט הוא user_id או product_id
- ✅ **שימוש במודלים מאומנים** - משתמש במודלים מ-Phase 1 (אם קיימים)
- ✅ **Fallback ל-rule-based** - עובד גם בלי מודלים מאומנים
- ✅ **תמיכה ב-batch** - יכול לקטגור מספר פריטים בבת אחת

## דרישות מוקדמות

**חשוב:** כדי להשתמש במודלים מאומנים, צריך להריץ Phase 1 קודם:

```bash
# שלב 1: אימון המודלים (Phase 1)
py src/phase1/user_categorization.py  # מאמן מודל משתמשים
py src/phase1/product_categorization.py  # מאמן מודל מוצרים
```

או:

```bash
py run_all_phases.py  # מריץ את כל Phase 1
```

## שימוש בסיסי

### דוגמה 1: קטגוריזציה של משתמש

```python
from src.phase3.single_item_categorization import SingleItemCategorization

# יצירת מופע
categorizer = SingleItemCategorization('.')

# קטגוריזציה של משתמש
result = categorizer.categorize(item_id=3, item_type='user', use_model=True)

print(f"User category: {result['category']}")
print(f"Method: {result['method']}")  # 'model' או 'rule_based'
print(f"Total clicks: {result['details']['total_clicks']}")
```

### דוגמה 2: קטגוריזציה של מוצר

```python
# קטגוריזציה של מוצר
result = categorizer.categorize(item_id=1, item_type='product', use_model=True)

print(f"Product category: {result['category']}")
print(f"Main category: {result['main_category']}")
print(f"Sub category: {result['sub_category']}")
print(f"Product name: {result['details']['product_name']}")
```

### דוגמה 3: זיהוי אוטומטי

```python
# המערכת מזהה אוטומטית אם זה user או product
result = categorizer.categorize(item_id=3, item_type='auto', use_model=True)

print(f"Item type: {result['item_type']}")  # 'user' או 'product'
print(f"Category: {result['category']}")
```

## שימוש מתקדם

### קטגוריזציה של מספר פריטים

```python
# קטגוריזציה של מספר משתמשים
user_ids = [3, 5, 10, 15]
results = categorizer.categorize_batch(user_ids, item_type='user', use_model=True)

for result in results:
    print(f"User {result['item_id']}: {result['category']}")
```

### שימוש ב-rule-based (ללא מודל)

```python
# משתמש ב-rule-based categorization (מהיר, לא צריך מודל מאומן)
result = categorizer.categorize(item_id=3, item_type='user', use_model=False)
```

## הרצת Phase 3 המלא

```python
from src.phase3.single_item_categorization import SingleItemCategorization

categorizer = SingleItemCategorization('.')
results = categorizer.run_phase3()  # מדגים עם דוגמאות
```

או:

```bash
py src/phase3/single_item_categorization.py
```

## פורמט התוצאה

### תוצאה למשתמש:

```python
{
    'item_id': 3,
    'item_type': 'user',
    'category': 'explorer',
    'category_encoded': 5,  # אם משתמש במודל
    'method': 'model',  # או 'rule_based'
    'details': {
        'features': {...},  # כל 36 התכונות
        'total_clicks': 25,
        'total_purchases': 2,
        'unique_products': 8,
        'engagement_score': 45.5
    }
}
```

### תוצאה למוצר:

```python
{
    'item_id': 1,
    'item_type': 'product',
    'category': 'Electronics || Audio',
    'main_category': 'Electronics',
    'sub_category': 'Audio',
    'method': 'model',  # או 'rule_based'
    'details': {
        'product_name': 'Wireless Headphones',
        'price': 199.99,
        'description': '...',
        'original_main_category': 'Electronics',
        'original_sub_category': 'Audio'
    }
}
```

## הערות חשובות

1. **Phase 1 חובה** - כדי להשתמש במודלים מאומנים, צריך להריץ Phase 1 קודם
2. **Rule-based תמיד עובד** - גם בלי מודלים מאומנים, המערכת תעבוד עם rule-based
3. **זיהוי אוטומטי** - אם לא בטוחים אם זה user או product, השתמשו ב-`item_type='auto'`
4. **מהירות** - קטגוריזציה של פריט יחיד לוקחת מילישניות (אם המודל נטען)

## דוגמאות שימוש

### שימוש יומיומי - משתמש חדש נרשם

```python
# משתמש חדש נרשם - קטגוריזציה מהירה
categorizer = SingleItemCategorization('.')
result = categorizer.categorize(item_id=new_user_id, item_type='user', use_model=True)

# שמירת הקטגוריה במסד הנתונים
user_category = result['category']
```

### שימוש יומיומי - מוצר חדש נוסף

```python
# מוצר חדש נוסף - קטגוריזציה מהירה
result = categorizer.categorize(item_id=new_product_id, item_type='product', use_model=True)

# שמירת הקטגוריות במסד הנתונים
main_category = result['main_category']
sub_category = result['sub_category']
```

## אינטגרציה עם Phase 2

Phase 3 יכול לעבוד יחד עם Phase 2:

```python
from src.phase2.recommendation_system_ml import RecommendationSystem
from src.phase3.single_item_categorization import SingleItemCategorization

# מערכת המלצות
rs = RecommendationSystem('.')
rs.load_data()
rs.create_user_interaction_matrix()

# קטגוריזציה של משתמש (Phase 3)
categorizer = SingleItemCategorization('.')
user_result = categorizer.categorize(item_id=123, item_type='user', use_model=True)

# המלצות (Phase 2)
recommendations = rs.hybrid_recommendations(user_id=123, n_recommendations=5)
```
