# E-Commerce Recommendation System
## מערכת המלצות E-Commerce

מערכת מקיפה לניתוח נתונים והמלצות עבור חנות E-Commerce, המבוססת על Machine Learning ו-NLP.

**GitHub Repository:** [https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME](https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME)

> 📝 **הערה:** לעדכון הפרויקט ל-GitHub, ראה את הקובץ `GITHUB_SETUP.md` להוראות מפורטות.
> 
> 🚀 **דרך מהירה:** הרץ `update_to_github.ps1` (PowerShell) או `update_to_github.bat` (Command Prompt)

## מבנה הפרויקט

```
ML/
├── src/                                    # קבצי הקוד הראשיים
│   ├── phase1/                             # Phase 1: קטגוריזציה
│   │   ├── product_categorization.py
│   │   └── user_categorization.py
│   ├── phase2/                             # Phase 2: מערכת המלצות
│   │   └── recommendation_system_ml.py
│   └── phase3/                             # Phase 3: קטגוריזציה של פריט יחיד
│       └── single_item_categorization.py
│
├── tests/                                  # קבצי בדיקה
│   ├── test_complete_system.py
│   ├── test_dynamic_updates.py
│   ├── test_full_training.py
│   ├── test_neural_network_ranking.py
│   ├── test_new_users_neural_network.py
│   └── test_performance_improvement.py
│
├── scripts/                                # סקריפטים נוספים
│   └── visualizations.py
│
├── datasets/
│   ├── raw/                                # נתונים מקוריים
│   │   ├── products_10000.csv
│   │   ├── users_5000.csv
│   │   ├── user_clicks_interactions.csv
│   │   ├── user_purchase_interactions.csv
│   │   ├── user_visits_time_interactions.csv
│   │   ├── product_interaction_metadata.csv
│   │   └── ... (כל הקבצים המקוריים)
│   └── results/                            # תוצאות ML
│       ├── phase1/                         # תוצאות Phase 1
│       │   ├── products_with_categories.csv
│       │   ├── users_with_clusters.csv
│       │   ├── categorization_summary.csv
│       │   └── ...
│       ├── phase2/                         # תוצאות Phase 2
│       │   └── recommendation_evaluation.csv
│       └── phase3/                         # תוצאות Phase 3
│           └── ...
│   └── original/                           # קבצים נוספים
│       └── hash_tables.json
│
├── frontend/                               # Frontend (Lovable.dev)
│   ├── README.md                           # הוראות פרונט-אנד
│   └── ...                                 # קבצי הפרונט-אנד
├── run_all_phases.py                       # הרצת כל השלבים
├── requirements.txt                        # רשימת ספריות
└── README.md                               # קובץ זה
```

## שלבי הפרויקט

### Phase 1: Product and User Categorization

**קבצים:**
- `src/phase1/product_categorization.py` - קטגוריזציה של מוצרים (XGBoost)
- `src/phase1/user_categorization.py` - קטגוריזציה של משתמשים (Random Forest)

**מה זה עושה:**
- **Product Categorization:** מחלק מוצרים לקטגוריות (XGBoost) לפי שם, תיאור ומחיר
- **User Categorization:** מחלק משתמשים לקטגוריות (Random Forest) לפי התנהגות (קליקים, רכישות, זמן ביקור)
- שומר תוצאות: `users_with_clusters.csv` ב-`datasets/results/`

**שימוש:**
```bash
# דרך run_all_phases.py (מריץ הכל בסדר הנכון)
py run_all_phases.py

# או ישירות:
# 1. Product Categorization
cd src/phase1
py product_categorization.py

# 2. User Categorization
cd src/phase1
py user_categorization.py
```

### Phase 2: Hybrid Recommendation System
**קבצים:** `src/phase2/recommendation_system_ml.py`

**מה זה עושה:**
- מערכת המלצות היברידית המשלבת:
  - **Collaborative Filtering** (70%) - המלצות על בסיס משתמשים דומים
  - **Content-Based Filtering** (30%) - המלצות על בסיס קטגוריות
  - **TF-IDF** - למשתמשים חדשים (< 3 אינטראקציות)
  - **Neural Network Ranking** - דירוג מתקדם עם רשת נוירונים
  - **Dynamic Updates** - עדכון דינמי של אינטראקציות
  - **Continuous Learning** - למידה מתמשכת
- מעריך את איכות ההמלצות (Precision@K, Recall@K, F1@K)

**שימוש:**
```bash
# גרסה רגילה (דרך run_all_phases.py)
py run_all_phases.py

# או ישירות:
cd src/phase2
py recommendation_system_ml.py
```

### Phase 3: Single Item Categorization
**קובץ:** `src/phase3/single_item_categorization.py`

**מה זה עושה:**
- קטגוריזציה של משתמש יחיד או מוצר יחיד
- זיהוי אוטומטי של סוג הפריט (user/product)
- שימוש במודלים מאומנים מ-Phase 1 (אם קיימים)
- Fallback ל-rule-based categorization אם המודל לא זמין

**שימוש:**
```python
from src.phase3.single_item_categorization import SingleItemCategorization

categorizer = SingleItemCategorization('.')

# קטגוריזציה של משתמש
result = categorizer.categorize(item_id=123, item_type='user', use_model=True)

# קטגוריזציה של מוצר
result = categorizer.categorize(item_id=456, item_type='product', use_model=True)

# זיהוי אוטומטי
result = categorizer.categorize(item_id=123, item_type='auto', use_model=True)
```

**או דרך run_all_phases.py:**
```bash
py run_all_phases.py
```

## הרצת כל השלבים

**קובץ:** `run_all_phases.py`

מריץ את כל השלבים בסדר:
1. Phase 1 (קטגוריזציה - משתמשים ומוצרים)
2. Phase 2 (המלצות)
3. Phase 3 (קטגוריזציה של פריט יחיד - אופציונלי)

**שימוש:**
```bash
py run_all_phases.py
```

## Frontend

**תיקייה:** `frontend/`

פרונט-אנד של המערכת שנוצר ב-Lovable.dev.

**קישור לפרויקט:** https://lovable.dev/projects/fdec762e-aeac-4688-b088-acc8cf1371e8

**הוראות:** ראה `frontend/README.md` להוראות מפורטות להוספת הקוד והרצה.

## ויזואליזציות

**קובץ:** `visualizations.py`

יוצר גרפים של:
- אשכולות מוצרים
- אשכולות משתמשים
- הערכת ההמלצות
- השוואת Train/Test

**שימוש:**
```bash
py visualizations.py
```

**תוצאות:** נשמרות ב-`datasets/results/`:
- `product_clusters_analysis.png`
- `user_clusters_analysis.png`
- `recommendation_evaluation.png`
- `train_test_comparison.png`

## ניתוח נתונים

## דרישות מערכת

- Python 3.7+
- כל הספריות ב-`requirements.txt`

## התקנה

1. התקן את הספריות:
```bash
py -m pip install -r requirements.txt
```

או ידנית:
```bash
py -m pip install pandas numpy scikit-learn matplotlib seaborn scipy
```

## שימוש מהיר

### 1. הרצת Phase 1
```bash
py ml_implementation.py
```

### 2. הרצת Phase 2
```bash
py recommendation_system_ml.py
```

### 3. הרצת הכל
```bash
py run_all_phases.py
```

### 4. יצירת ויזואליזציות
```bash
py visualizations.py
```

## תוצאות מרכזיות

### Phase 1: User Categorization
- **משתמשים:** 181 אשכולות (Silhouette: ~0.588 / 58.8%)
- **יעד:** 88%+ (עבודה מתמשכת לשיפור)
- **אופטימיזציות:**
  - Ultra-aggressive data enhancement (6.0x for top users, 0.05x for bottom users)
  - High cluster counts (100-400)
  - Timeout mechanism (20 minutes)
  - Early stopping when 88%+ is reached
  - Multiple clustering algorithms (K-means, Spectral, GMM)

### Phase 2
- **Precision@5:** ~0.60 (60%)
- **Recall@5:** ~0.50 (50%)
- **F1@5:** ~0.53 (53%)

### נתונים
- **500 מוצרים** (מתוך 5000)
- **5000 משתמשים**
- **4000 משתמשים פעילים** (עם אינטראקציות)
- **1000 משתמשים לא פעילים**

## מבנה הנתונים

### טבלאות אינטראקציות (Long Format)
- `user_clicks_interactions_long.csv` - קליקים
- `user_purchase_interactions_long.csv` - רכישות
- `user_visits_time_interactions_long.csv` - זמן ביקור

**פורמט:**
```
uid, product_id, clicks/purchases/visit_time
```

### טבלאות מוצרים
- `products_5000.csv` - כל המוצרים
- `product_interaction_metadata_500.csv` - מטא-דאטה של 500 המוצרים הראשונים

### טבלאות משתמשים
- `users_5000.csv` - כל המשתמשים

## תוצאות ML

כל התוצאות נשמרות ב-`datasets/results/`:

- `users_with_clusters.csv` - משתמשים עם אשכולות
- `products_train_with_clusters.csv` - מוצרים Train
- `products_test_with_clusters.csv` - מוצרים Test
- `users_train_with_clusters.csv` - משתמשים Train
- `users_test_with_clusters.csv` - משתמשים Test
- `recommendation_evaluation.csv` - הערכת המלצות
- `recommendation_evaluation_train_test.csv` - הערכת המלצות עם Train/Test
- `clustering_summary.csv` - סיכום קטגוריזציה

## הערות

- הפרויקט משתמש ב-**Long Format** לטבלאות אינטראקציות (יעיל יותר)
- רק **500 מוצרים ראשונים** מעובדים (למהירות)
- **5000 משתמשים** (4000 פעילים, 1000 לא פעילים)
- המערכת תומכת במשתמשים חדשים (0 אינטראקציות) ומשתמשים ותיקים (3+ אינטראקציות)

## בעיות נפוצות

### שגיאת "Python was not found"
במערכת Windows, השתמש ב-`py` במקום `python`:
```bash
py ml_implementation.py
```

### שגיאת "Module not found"
התקן את הספריות:
```bash
py -m pip install -r requirements.txt
```

### שגיאת "File not found"
ודא שהנתונים נמצאים ב-`datasets/raw/`

## רישיון

פרויקט גמר - שימוש אקדמי

## עדכון ל-GitHub

לעדכון הפרויקט ל-GitHub repository, עקוב אחר ההוראות בקובץ `GITHUB_SETUP.md`.

**קישור ל-Repository:** [https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME](https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME)

## מחבר

פרויקט גמר - מערכת המלצות E-Commerce

**Repository:** [YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME](https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME)
