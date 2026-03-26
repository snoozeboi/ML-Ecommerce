# הגדרת Stripe לתשלומים

## סקירה כללית

נוספה תמיכה מלאה בתשלומים באמצעות Stripe למערכת ה-E-Commerce. המערכת כוללת:

### Backend (Java Spring Boot)
- ✅ מודל Payment - שמירת תשלומים
- ✅ מודל PaymentMethod - שמירת אמצעי תשלום של משתמשים
- ✅ PaymentService - שירותי תשלום עם Stripe
- ✅ PaymentController - API endpoints לתשלומים

### Frontend (React)
- ✅ דף Checkout עם Stripe Elements
- ✅ אינטגרציה עם Cart Modal
- ✅ פונקציות API לתשלומים

## הגדרה

### 1. קבלת מפתחות Stripe

1. היכנס לחשבון Stripe שלך: https://dashboard.stripe.com/
2. עבור ל-Developers > API keys
3. העתק את:
   - **Secret Key** (מתחיל ב-`sk_test_` או `sk_live_`)
   - **Publishable Key** (מתחיל ב-`pk_test_` או `pk_live_`)

### 2. הגדרת Backend

ערוך את `backend/src/main/resources/application.properties`:

```properties
# Stripe Configuration
stripe.secret.key=sk_test_your_actual_secret_key_here
stripe.publishable.key=pk_test_your_actual_publishable_key_here
```

או הגדר משתני סביבה:
```bash
export STRIPE_SECRET_KEY=sk_test_your_actual_secret_key_here
export STRIPE_PUBLISHABLE_KEY=pk_test_your_actual_publishable_key_here
```

### 3. הגדרת Frontend

צור קובץ `.env` בתיקיית `frontend/curated-cart-main/`:

```env
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_your_actual_publishable_key_here
VITE_API_URL=http://localhost:8080
```

### 4. התקנת תלויות

**Backend:**
```bash
cd backend
mvn clean install
```

**Frontend:**
```bash
cd frontend/curated-cart-main
npm install
```

## API Endpoints

### יצירת Payment Intent
```
POST /api/payments/create-intent
Body: {
  "userId": 1,
  "amount": 99.99,
  "currency": "usd"
}
Response: {
  "success": true,
  "clientSecret": "pi_xxx_secret_xxx",
  "paymentIntentId": "pi_xxx",
  "paymentId": 1
}
```

### אישור תשלום
```
POST /api/payments/confirm
Body: {
  "paymentIntentId": "pi_xxx",
  "paymentMethodId": "pm_xxx" (optional)
}
```

### שמירת אמצעי תשלום
```
POST /api/payments/save-method
Body: {
  "userId": 1,
  "stripePaymentMethodId": "pm_xxx"
}
```

### קבלת אמצעי תשלום של משתמש
```
GET /api/payments/methods/{userId}
```

### הגדרת אמצעי תשלום כברירת מחדל
```
PUT /api/payments/methods/{userId}/default
Body: {
  "paymentMethodId": 1
}
```

### מחיקת אמצעי תשלום
```
DELETE /api/payments/methods/{userId}/{paymentMethodId}
```

### היסטוריית תשלומים
```
GET /api/payments/history/{userId}
```

## זרימת תשלום

1. משתמש מוסיף מוצרים לעגלה
2. משתמש לוחץ "Proceed to Checkout"
3. המערכת יוצרת Payment Intent ב-Stripe
4. משתמש מזין פרטי כרטיס אשראי
5. Stripe מאמת את התשלום
6. המערכת מאשרת את התשלום ב-backend
7. העגלה מתנקה והזמנה נוצרת

## בדיקות

### כרטיסי בדיקה של Stripe

לבדיקות, השתמש בכרטיסי הבדיקה של Stripe:

**תשלום מוצלח:**
- מספר כרטיס: `4242 4242 4242 4242`
- תאריך תפוגה: כל תאריך עתידי
- CVC: כל 3 ספרות
- ZIP: כל 5 ספרות

**תשלום נכשל:**
- מספר כרטיס: `4000 0000 0000 0002`

רשימה מלאה: https://stripe.com/docs/testing

## הערות חשובות

1. **מפתחות Test vs Live:**
   - במצב פיתוח, השתמש במפתחות Test (`sk_test_`, `pk_test_`)
   - במצב ייצור, השתמש במפתחות Live (`sk_live_`, `pk_live_`)

2. **אבטחה:**
   - לעולם אל תחשוף את Secret Key ב-frontend
   - השתמש ב-Publishable Key רק ב-frontend
   - שמור את Secret Key במשתני סביבה או בקובץ `.env` שלא נשמר ב-Git

3. **Webhooks:**
   - מומלץ להגדיר Stripe Webhooks לעדכונים אוטומטיים על סטטוס תשלומים
   - Endpoint: `POST /api/payments/webhook` (ניתן להוסיף בעתיד)

## פתרון בעיות

### שגיאת "Stripe API key not set"
- ודא שהגדרת את `stripe.secret.key` ב-`application.properties`
- ודא שהמפתח מתחיל ב-`sk_test_` או `sk_live_`

### שגיאת "Invalid API Key"
- ודא שהעתקת את המפתח במלואו
- ודא שאין רווחים או תווים נוספים

### Frontend לא מצליח ליצור Payment Intent
- ודא שה-backend רץ על פורט 8080
- ודא שה-`VITE_API_URL` מוגדר נכון
- בדוק את Console בדפדפן לשגיאות

## תמיכה

לשאלות נוספות, ראה:
- [תיעוד Stripe](https://stripe.com/docs)
- [Stripe Java SDK](https://github.com/stripe/stripe-java)
- [Stripe React](https://stripe.com/docs/stripe-js/react)
