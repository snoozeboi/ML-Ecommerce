# מסמך עיצוב תוכנה (Software Design Document - SDD)
## מערכת E-Commerce עם Machine Learning

---

## 1. מבוא

### 1.1 מטרת המסמך
מסמך זה מתאר את עיצוב המערכת, הארכיטקטורה, הרכיבים והזרימות הטכניות של מערכת E-Commerce חכמה המשלבת ML.

### 1.2 היקף
- Backend (Spring Boot)
- Frontend (React)
- שירות ML (Flask/Python)
- מסד נתונים (PostgreSQL)
- שירותי תשתית (RabbitMQ, Elasticsearch)

---

## 2. ארכיטקטורה כללית

### 2.1 דיאגרמת שכבות

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React)                          │
│  דפי בית, מוצרים, עגלה, פרופיל, Admin, חיפוש                    │
└─────────────────────────────────────────────────────────────────┘
                                    │ HTTP/REST
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Backend (Spring Boot)                        │
│  Controllers: Product, Recommendation, Cart, Auth, ML, Admin     │
│  Services: ProductService, RecommendationService, MLService      │
└─────────────────────────────────────────────────────────────────┘
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌────────────────┐    ┌──────────────────────┐
│ PostgreSQL   │    │ ML Service     │    │ RabbitMQ /            │
│ (DB)         │    │ (Flask, :5000) │    │ Elasticsearch         │
└──────────────┘    └────────────────┘    └──────────────────────┘
                            │
                            ▼
                    ┌──────────────────────┐
                    │ Python ML Scripts    │
                    │ Phase 1, 2, 3        │
                    │ (XGBoost, RF, etc.)  │
                    └──────────────────────┘
```

### 2.2 תקשורת בין רכיבים

| ערוץ | פרוטוקול | תיאור |
|------|----------|-------|
| Frontend ↔ Backend | HTTP REST | API על פורט 8080 |
| Backend ↔ ML Service | HTTP REST | קטגוריזציה והמלצות על פורט 5000 |
| Backend ↔ PostgreSQL | JDBC | נתונים, משתמשים, מוצרים, עגלה |
| Backend ↔ RabbitMQ | AMQP | הודעות אירועים (אופציונלי) |
| Backend ↔ Elasticsearch | HTTP | חיפוש מוצרים |

---

## 3. רכיבי המערכת

### 3.1 Backend (Spring Boot)

#### 3.1.1 Controllers

| Controller | Base Path | תפקיד |
|------------|-----------|--------|
| ProductController | /api/products | CRUD מוצרים, קטגוריות |
| RecommendationController | /api/recommendations | המלצות אישיות, פופולריים, דומים |
| CartController | /api/cart | עגלת קניות |
| AuthController | /auth | התחברות, הרשמה |
| MLController | /api/ml | Phase 1, סנכרון CSV, קטגוריזציה בודדת |
| AdminController | /api/admin | ניהול משתמשים, מוצרים |
| HomeController | /api/home | דף בית, מוצרים פופולריים |
| EventController | /api/events | אירועי צפייה |
| HealthController | /health | בדיקת חיוניות |

#### 3.1.2 Services עיקריים

| Service | תפקיד |
|---------|-------|
| ProductService | לוגיקת מוצרים, קטגוריזציה על שמירה |
| RecommendationService | בניית payload ל-ML, fallback DB |
| MLService | הרצת Phase 1, סנכרון CSV, עדכון DB |
| CartService | ניהול עגלה |
| AuthService | אימות והרשאות |
| ProductSearchService | חיפוש (Elasticsearch) |

#### 3.1.3 Repositories (JPA)

- UserRepository, ProductRepository, CartItemRepository
- UserSearchHistoryRepository, UserViewHistoryRepository

### 3.2 שירות ML (Flask)

| Endpoint | תיאור |
|----------|-------|
| POST /data/load | טעינת נתונים (מוצרים, משתמשים, אינטראקציות) |
| POST /categorize/product | קטגוריזציה של מוצר בודד |
| POST /categorize/user | קטגוריזציה של משתמש בודד |
| GET /recommend/personalized/{userId} | המלצות אישיות |
| GET /recommend/similar/{productId} | מוצרים דומים |
| GET /recommend/popular | מוצרים פופולריים |

### 3.3 Python ML Scripts

| שלב | קובץ | אלגוריתם | פלט |
|-----|------|----------|------|
| Phase 1 - מוצרים | product_categorization.py | XGBoost + TF-IDF | products_with_categories.csv |
| Phase 1 - משתמשים | user_categorization.py | Random Forest | users_with_clusters.csv |
| Phase 2 | recommendation_system_ml.py | Collaborative + Content-Based + Neural | recommendation_evaluation.csv |
| Phase 3 | single_item_categorization.py | שימוש במודלים מאומנים | קטגוריה ליחיד |

---

## 4. עיצוב מסד נתונים

### 4.1 ישויות עיקריות

| טבלה | שדות מרכזיים |
|------|---------------|
| users | id, user_name, email, password_hash, segment, ml_category, last_classified_at |
| products | id, product_name, description, price, category, sub_category, ml_category, image_url |
| cart_items | id, user_id, product_id, quantity, added_at |
| user_search_history | user_id, search_term, search_count |
| user_view_history | user_id, product_id |
| user_purchase_history | user_id, product_id (ElementCollection) |

### 4.2 יחסים

- User 1:N CartItem
- User 1:N UserSearchHistory, UserViewHistory
- Product 1:N CartItem
- User M:N Product (דרך purchase_history, view_history)

---

## 5. זרימות עיקריות

### 5.1 זרימת קטגוריזציה של מוצר חדש

```
1. Admin מוסיף מוצר (POST /api/products)
2. ProductService שומר ב-DB
3. אם ml.categorize.on.save=true:
   - שליחה ל-ML Service (POST /categorize/product)
   - ML מחזיר main_category, sub_category
   - ProductService מעדכן את המוצר
4. המוצר נשמר עם קטגוריה
```

### 5.2 זרימת Phase 1

```
1. POST /api/ml/phase1
2. MLService מייצא מוצרים ל-current_products_for_phase1.csv
3. הרצת Python: product_categorization.py, user_categorization.py
4. קריאת products_with_categories.csv, users_with_clusters.csv
5. עדכון products ו-users ב-PostgreSQL
   - רק מוצרים ללא קטגוריה מתעדכנים (Unclassified)
```

### 5.3 זרימת המלצות

```
1. משתמש נכנס לדף הבית
2. Frontend קורא GET /api/recommendations/personalized/{userId}
3. RecommendationController בודק אם ML Service זמין
4. אם כן: קריאה ל-ML Service, החזרת המלצות
5. אם לא: fallback מ-DB (מוצרים פופולריים לפי קטגוריה)
6. אם נתוני ML ישנים: טעינת נתונים ב-background (loadDataToMLServiceAsync)
```

---

## 6. עיצוב ML

### 6.1 Phase 1 - קטגוריזציית מוצרים

| שלב | תיאור |
|-----|-------|
| קלט | products_10000.csv / seed/products.csv |
| פיצ'רים | TF-IDF (שם+תיאור) + מחיר מנורמל |
| מודל | XGBoost Classifier |
| פלט | predicted_main_category, predicted_sub_category |

### 6.2 Phase 1 - קטגוריזציית משתמשים

| שלב | תיאור |
|-----|-------|
| קלט | users_5000.csv, אינטראקציות (clicks, purchases, visit_time) |
| פיצ'רים | 35 פיצ'רים (clicks, purchases, conversion_rate, וכו') |
| מודל | Random Forest |
| פלט | ml_category (high_value, explorer, active_browser, וכו') |

### 6.3 Phase 2 - מערכת המלצות

- Collaborative Filtering (70%)
- Content-Based Filtering (30%)
- TF-IDF למשתמשים חדשים
- Neural Network Ranking

---

## 7. הגדרות והרחבה

### 7.1 application.properties (Backend)

| מפתח | ברירת מחדל | תיאור |
|------|------------|-------|
| ml.categorize.on.save | true | קטגוריזציה אוטומטית בעת שמירת מוצר |
| ml.categorize.fallback-to-python | false | Fallback ל-Python אם ML Service down |
| ml.service.auto-start | true | הפעלת שירות ML אוטומטית |

### 7.2 Endpoints לסנכרון

| Endpoint | תיאור |
|----------|-------|
| POST /api/ml/sync-categories-from-seed | סנכרון קטגוריות מ-seed/products.csv |
| POST /api/ml/sync-products-from-csv | סנכרון מ-products_with_categories.csv |
| POST /api/ml/sync-products-from-csv?byRowOrder=true | סנכרון לפי סדר שורות |

---

## 8. תיעוד נוסף

- `PHASE1_DESIGN_AND_DATA.md` – פירוט Phase 1
- `ML_API_DOCUMENTATION.md` – API של ML
- `FRONTEND_BACKEND_API_DOCUMENTATION.md` – מיפוי Frontend-Backend
- `FLOW_DIAGRAMS.md` – דיאגרמות זרימה

---

*תאריך עדכון אחרון: מרץ 2026*
