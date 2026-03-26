# קבצים שאפשר למחוק / לנקות (לא נמחק – רק רשימה)

**שימו לב:** לפני מחיקה – לגבות או לוודא שלא צריכים את הקובץ. מומלץ למחוק רק אחרי שמוודאים.

---

## 1. גיבויים (Backup) – אפשר למחוק אחרי וידוא

אלה נוצרו אוטומטית (למשל על ידי `convert_tables_to_long_format.py`). אם אתם בטוחים שעובדים רק עם הפורמט הנוכחי (long) ולא צריכים את ה־Wide המקורי:

| קובץ | הערה |
|------|------|
| `datasets/raw/product_interaction_metadata_backup.csv` | גיבוי לפני המרה – אפשר למחוק אם לא צריכים את הגרסה הישנה. |
| `datasets/raw/user_clicks_interactions_wide_backup.csv` | גיבוי Wide לפני המרה ל־long. |
| `datasets/raw/user_purchase_interactions_wide_backup.csv` | גיבוי Wide לפני המרה ל־long. |
| `datasets/raw/user_visits_time_interactions_wide_backup.csv` | גיבוי Wide לפני המרה ל־long. |
| `datasets/unused/results/phase1/products_with_categories_backup.csv` | גיבוי מתוך product_categorization – נשמר גם ב־phase1 בתיקייה הראשית. |

---

## 2. תיקיית `datasets/unused` – מועמדת למחיקה

תיקייה שסומנה במפורש כ־"unused". **אף סקריפט Python לא מפנה אליה.** אם אתם בטוחים שלא צריכים את הדאטה הזה:

| קובץ / תיקייה | הערה |
|----------------|------|
| `datasets/unused/raw/cart_item_options.csv` | דאטה שלא בשימוש. |
| `datasets/unused/raw/cart_items.csv` | דאטה שלא בשימוש. |
| `datasets/unused/raw/hash_tables.json` | דאטה שלא בשימוש. |
| `datasets/unused/raw/new_tables_summary.csv` | דאטה שלא בשימוש. |
| `datasets/unused/raw/product_tags.csv` | דאטה שלא בשימוש. |
| `datasets/unused/raw/user_purchase_history_enhanced.csv` | דאטה שלא בשימוש. |
| `datasets/unused/raw/user_search_history.csv` | דאטה שלא בשימוש. |
| `datasets/unused/raw/user_wishlist.csv` | דאטה שלא בשימוש. |
| `datasets/unused/results/phase1/clustering_summary.csv` | תוצאות ישנות. |
| `datasets/unused/results/phase1/final_report.json` | תוצאות ישנות. |
| `datasets/unused/results/phase1/products_with_categories_backup.csv` | גיבוי ישן. |

**אופציה:** למחוק את כל התיקייה `datasets/unused/` אם אין צורך בארכיון.

---

## 3. קבצי הערות / תיעוד ישן ב־backend

קבצים בלי סיומת או עם סיומת .txt – נראים כמו רשימות ועיצוב זרימה. אם העדכנתם את התיעוד במקום אחר (למשל README / DOCS), אפשר למחוק או למזג:

| קובץ | הערה |
|------|------|
| `backend/Flow` | תיאור זרימה (guest, segment, RabbitMQ). אפשר להעביר ל־DOCS ולמחוק. |
| `backend/Integration-Front-Back-ML` | כנראה הערות אינטגרציה. אין סיומת – קובץ טקסט. |
| `backend/Structure.txt` | מבנה Product/User ו־"Where should ML live". מידע יכול לעבור ל־README/docs. |
| `backend/TODOLIST` | רשימת TODO ישנה (RabbitMQ, Elastic, וכו'). **מוחלפת על ידי `TODO_LIST.md` בשורש.** מועמד חזק למחיקה. |

---

## 4. קבצים נוספים – אופציונלי

| קובץ | הערה |
|------|------|
| `datasets/original/hash_tables.json` | אם אף סקריפט לא משתמש – אפשר למחוק. |
| `backend/fix_payment_methods_table.sql` | סקריפט SQL חד־פעמי. אם כבר הרצתם ולא צריכים – אפשר למחוק. |
| `backend/add_wallet_balance.sql` | יש גם migration אוטומטי בקוד; הקובץ שימושי כתיעוד. **להשאיר** אלא אם מחליטים לאחד תיעוד SQL. |
| `update_github.bat` | סקריפט עזר ל־Git. להשאיר אם משתמשים, למחוק אם לא. |

---

## 5. לא למחוק (להשאיר)

- `backend/README.txt` – הוראות הרצה ל־backend, שימושי.
- `backend/product_import_template.csv` – תבנית ייבוא, בשימוש.
- `backend/src/main/resources/seed/*.csv` – דאטה seed, נדרש למילוי DB.
- כל קבצי ה־.md בתיעוד (README, QUICK_START, STRIPE_SETUP, וכו') – עדיף לעדכן ולא למחוק.

---

## סיכום מומלץ למחיקה (אחרי וידוא)

1. **גיבויים ב־datasets/raw:**  
   `*_backup.csv` ו־`*_wide_backup.csv` – רק אם עובדים רק עם הגרסאות הנוכחיות.

2. **כל התיקייה `datasets/unused/`** – אם אין צורך בארכיון דאטה ישן.

3. **ב־backend:**  
   `TODOLIST`, ואולי `Flow`, `Integration-Front-Back-ML`, `Structure.txt` – אם העברתם את התוכן לתיעוד מרכזי.

4. **אופציונלי:**  
   `datasets/original/hash_tables.json`, `backend/fix_payment_methods_table.sql` – אם וידאתם שלא בשימוש.

**לא נמחק שום קובץ אוטומטית – רק רשימה לעבודה ידנית.**
