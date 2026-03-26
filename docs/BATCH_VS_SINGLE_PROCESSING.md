# תהליכי Batch vs Single User Processing

## סקירה כללית

המערכת תומכת בשני תהליכים נפרדים:

1. **Batch Processing (תהליך לכל המשתמשים)** - לאימון המודל
2. **Single User Processing (תהליך למשתמש יחיד)** - לשימוש מהיר עם המודל המאומן

## תהליך 1: Batch Processing - אימון המודל

### מתי להשתמש:
- אימון ראשוני של המודל
- עדכון המודל עם נתונים חדשים
- פעם אחת או מדי פעם (לא כל פעם)

### איך להריץ:

```python
from src.phase1.user_categorization import UserCategorization

# יצירת מופע
uc = UserCategorization('.')

# טעינת נתונים
uc.load_data()

# אימון המודל על כל המשתמשים (5-10 דקות)
uc.user_categorization_random_forest()

# המודל נשמר אוטומטית ב:
# datasets/results/phase1/models/
```

### מה זה עושה:
1. מחשב תכונות לכל המשתמשים
2. מאמן מודל Random Forest עם GridSearchCV
3. שומר את המודל והתוצאות
4. שומר את המודל לשימוש מהיר (single user)

### קבצים שנשמרים:
- `datasets/results/phase1/users_with_clusters.csv` - תוצאות קטגוריזציה
- `datasets/results/phase1/models/rf_model.pkl` - המודל המאומן
- `datasets/results/phase1/models/scaler_robust.pkl` - RobustScaler
- `datasets/results/phase1/models/scaler_standard.pkl` - StandardScaler
- `datasets/results/phase1/models/feature_selector.pkl` - Feature Selector
- `datasets/results/phase1/models/label_encoder.pkl` - Label Encoder
- `datasets/results/phase1/models/model_metadata.json` - Metadata

## תהליך 2: Single User Processing - שימוש מהיר

### מתי להשתמש:
- קטגוריזציה של משתמש בודד (למשל אחרי אינטראקציה חדשה)
- המלצות למשתמש ספציפי
- שימוש יומיומי/real-time

### איך להריץ:

#### אופציה א': עם מודל מאומן (מומלץ)

```python
from src.phase2.recommendation_system_ml import RecommendationSystem

# יצירת מופע
rs = RecommendationSystem('.')

# טעינת נתונים
rs.load_data()

# קטגוריזציה של משתמש בודד (המודל נטען אוטומטית)
result = rs.categorize_single_user(user_id=123, use_model=True, update_clusters=True)

print(f"Category: {result['category']}")
print(f"Method: {result['method']}")  # 'model' או 'rule_based'
```

#### אופציה ב': עם UserCategorization ישירות

```python
from src.phase1.user_categorization import UserCategorization

# יצירת מופע
uc = UserCategorization('.')

# טעינת נתונים
uc.load_data()

# טעינת מודל מאומן (אם קיים)
uc.load_model()

# קטגוריזציה של משתמש בודד
result = uc.categorize_single_user(user_id=123, use_model=True)

print(f"Category: {result['category']}")
print(f"Method: {result['method']}")
```

#### אופציה ג': עדכון אחרי אינטראקציה

```python
from src.phase2.recommendation_system_ml import RecommendationSystem

rs = RecommendationSystem('.')
rs.load_data()
rs.create_user_interaction_matrix()

# משתמש לחץ על מוצר - מעדכן קטגוריה ומחזיר המלצות
result = rs.update_user_category_after_interaction(
    user_id=123,
    product_id=456,
    interaction_type='click',
    value=1,
    use_model=True  # משתמש במודל מאומן
)

print(f"New category: {result['user_category']}")
print(f"Recommendations: {result['recommendations']}")
```

### מה זה עושה:
1. טוען את המודל המאומן (אם קיים)
2. מחשב תכונות למשתמש בודד
3. משתמש במודל לחזות קטגוריה
4. מעדכן את users_with_clusters (אם requested)

## סדר מומלץ:

### 1. אימון ראשוני (פעם אחת):
```python
# שלב 1: אימון המודל על כל המשתמשים
uc = UserCategorization('.')
uc.load_data()
uc.user_categorization_random_forest()  # המודל נשמר אוטומטית
```

### 2. שימוש יומיומי (משתמש יחיד):
```python
# שלב 2: שימוש מהיר עם המודל המאומן
rs = RecommendationSystem('.')
rs.load_data()

# קטגוריזציה מהירה
result = rs.categorize_single_user(user_id=123, use_model=True)
```

## יתרונות:

### Batch Processing:
- ✅ דיוק מקסימלי (אימון על כל הנתונים)
- ✅ מודל מאומן היטב
- ✅ שמירה אוטומטית של המודל

### Single User Processing:
- ✅ מהיר מאוד (מילישניות)
- ✅ לא צריך לאמן מחדש
- ✅ עובד עם מודל מאומן או rule-based (fallback)

## הערות חשובות:

1. **חובה להריץ batch processing קודם** - כדי לאמן ולשמור את המודל
2. **המודל נשמר אוטומטית** - אחרי `user_categorization_random_forest()`
3. **Single user processing טוען את המודל אוטומטית** - אם קיים
4. **Fallback ל-rule-based** - אם המודל לא קיים או לא נטען

## דוגמה מלאה:

```python
# ============================================
# שלב 1: אימון המודל (פעם אחת)
# ============================================
from src.phase1.user_categorization import UserCategorization

uc = UserCategorization('.')
uc.load_data()
uc.user_categorization_random_forest()  # 5-10 דקות, שומר מודל אוטומטית

# ============================================
# שלב 2: שימוש מהיר (משתמש יחיד)
# ============================================
from src.phase2.recommendation_system_ml import RecommendationSystem

rs = RecommendationSystem('.')
rs.load_data()
rs.create_user_interaction_matrix()

# משתמש חדש לחץ על מוצר
result = rs.update_user_category_after_interaction(
    user_id=999,
    product_id=123,
    interaction_type='click',
    value=1,
    use_model=True  # משתמש במודל המאומן
)

print(f"User category: {result['user_category']}")
print(f"Recommendations: {result['recommendations']}")
```
