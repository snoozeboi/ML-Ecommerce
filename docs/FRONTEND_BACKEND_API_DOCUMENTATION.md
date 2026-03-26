# Frontend-Backend API Documentation

## סטטוס כללי
✅ **הפרונט-אנד מוכן לעבוד עם ה-backend!**

הפרונט-אנד מוגדר עם:
- API Base URL: `http://localhost:8080`
- Fallback ל-mock data אם ה-backend לא זמין
- Mapping function מ-backend format ל-frontend format
- Error handling עם try-catch

---

## 📋 רשימת Endpoints - התאמה בין Frontend ל-Backend

### ✅ **Recommendations API** (`/api/recommendations`)

| Frontend Function | Frontend Endpoint | Backend Endpoint | Status | Notes |
|------------------|------------------|------------------|--------|-------|
| `getRecommendations(userId?)` | `GET /api/recommendations/personalized/{userId}?limit=5` | ✅ `GET /api/recommendations/personalized/{userId}?limit=10` | ✅ **תואם** | Backend מקבל limit |
| `getRecommendations()` (guest) | `GET /api/recommendations/guest?limit=5` | ✅ `GET /api/recommendations/guest?limit=10` | ✅ **תואם** | Backend מקבל limit |
| `getPopularPurchases()` | `GET /api/recommendations/trending?limit=5` | ✅ `GET /api/recommendations/trending?limit=10` | ✅ **תואם** | Backend מקבל limit |
| `getSimilarItems(productId)` | `GET /api/recommendations/similar/{productId}?limit=4` | ✅ `GET /api/recommendations/similar/{productId}?limit=5` | ✅ **תואם** | Backend מקבל limit |
| `getProducts(filters?)` | `GET /api/recommendations/products` | ✅ `GET /api/recommendations/products` | ✅ **תואם** | Frontend עושה filtering client-side |
| `getUserRecommendations(userId, limit)` | `GET /api/recommendations/user/{userId}?limit={limit}` | ✅ `GET /api/recommendations/user/{userId}?limit=10` | ✅ **תואם** | Backend מקבל limit |
| `getRecommendationsByCategory(category, limit)` | `GET /api/recommendations/category/{category}?limit={limit}` | ✅ `GET /api/recommendations/category/{category}?limit=10` | ✅ **תואם** | Backend מקבל limit |
| `recordProductViewViaRecommendations(productId)` | `POST /api/recommendations/view/{productId}` | ✅ `POST /api/recommendations/view/{productId}` | ✅ **תואם** | |
| `recordSearch(userId, searchTerm)` | `POST /api/recommendations/search/{userId}` | ✅ `POST /api/recommendations/search/{userId}` | ✅ **תואם** | Body: `{ searchTerm: string }` |
| `recordPurchase(userId, productId)` | `POST /api/recommendations/purchase/{userId}` | ✅ `POST /api/recommendations/purchase/{userId}` | ✅ **תואם** | Body: `{ productId: number }` |

### ✅ **Products API** (`/api/products`)

| Frontend Function | Frontend Endpoint | Backend Endpoint | Status | Notes |
|------------------|------------------|------------------|--------|-------|
| `getProductById(id)` | `GET /api/products/{id}` | ✅ `GET /api/products/{id}` | ✅ **תואם** | **Public** - No authentication required |
| `createProduct(productData, userEmail)` | `POST /api/products` | ✅ `POST /api/products` | ✅ **תואם** | **Admin Only**<br/>Body: `ProductUpsertRequest`<br/>Header: `X-User-Email: {adminEmail}`<br/>Returns 403 if not admin |
| `updateProduct(id, productData, userEmail)` | `PUT /api/products/{id}` | ✅ `PUT /api/products/{id}` | ✅ **תואם** | **Admin Only**<br/>Body: `ProductUpsertRequest`<br/>Header: `X-User-Email: {adminEmail}`<br/>Returns 403 if not admin |
| `deleteProduct(id, userEmail)` | `DELETE /api/products/{id}` | ✅ `DELETE /api/products/{id}` | ✅ **תואם** | **Admin Only**<br/>Header: `X-User-Email: {adminEmail}`<br/>Returns 403 if not admin<br/>**Note:** Automatically deletes all cart items referencing this product |
| CSV Import (via Admin UI) | `POST /api/products/import` | ✅ `POST /api/products/import` | ✅ **תואם** | **Admin Only**<br/>Content-Type: `multipart/form-data`<br/>Header: `X-User-Email: {adminEmail}`<br/>Body: `file: [CSV file]`<br/>Returns import statistics |
| `checkProductsExist(productIds)` | `GET /api/recommendations/products` | ✅ `GET /api/recommendations/products` | ✅ **תואם** | Checks which product IDs exist (used for cart validation) |
| `checkProductsExist(productIds)` | `GET /api/recommendations/products` | ✅ `GET /api/recommendations/products` | ✅ **תואם** | Checks which product IDs exist (used for cart validation) |

### ✅ **Auth API** (`/auth`)

| Frontend Function | Frontend Endpoint | Backend Endpoint | Status | Notes |
|------------------|------------------|------------------|--------|-------|
| `login(email, password)` | `POST /auth/login` | ✅ `POST /auth/login` | ✅ **תואם** | Body: `{ email: string, password: string }` |
| `register(name, email, password)` | `POST /auth/register` | ✅ `POST /auth/register` | ✅ **תואם** | Body: `{ name: string, email: string, password: string }`<br/>**Validations:**<br/>- Email must be unique<br/>- Username must be unique<br/>- Password min 6 characters<br/>- Email normalized to lowercase |
| `testLogin(email, password)` | `GET /auth/test-login?email={email}&password={password}` | ✅ `GET /auth/test-login?email={email}&password={password}` | ✅ **תואם** | |
| `checkCredentials(email, password)` | `GET /auth/check-credentials?email={email}&password={password}` | ✅ `GET /auth/check-credentials?email={email}&password={password}` | ✅ **תואם** | |

### ✅ **Events API** (`/api/events`)

| Frontend Function | Frontend Endpoint | Backend Endpoint | Status | Notes |
|------------------|------------------|------------------|--------|-------|
| `recordProductView(productId, userId)` | `POST /api/events/product-view/{productId}?userId={userId}` | ✅ `POST /api/events/product-view/{productId}?userId={userId}` | ✅ **תואם** | |

### ✅ **Home API** (`/api/home`)

| Frontend Function | Frontend Endpoint | Backend Endpoint | Status | Notes |
|------------------|------------------|------------------|--------|-------|
| `getTrendingProducts(limit)` | `GET /api/home/trending?limit={limit}` | ✅ `GET /api/home/trending?limit=5` | ✅ **תואם** | Backend מחזיר `ProductSummaryDto[]` |

### ✅ **Health API**

| Frontend Function | Frontend Endpoint | Backend Endpoint | Status | Notes |
|------------------|------------------|------------------|--------|-------|
| Health Check | `GET /health` | ✅ `GET /health` | ✅ **תואם** | מחזיר `"Alive."` |
| Actuator Health | `GET /actuator/health` | ⚠️ לא מוגדר | ⚠️ **לא קיים** | Frontend מנסה לגשת אבל זה לא קיים |

---

## 🔍 Endpoints שלא קיימים ב-Backend (Frontend מטפל בהם)

| Frontend Function | Frontend Endpoint | Status | Notes |
|------------------|------------------|--------|-------|
| `getCategories()` | `GET /api/recommendations/products` (ואז extract categories) | ✅ **עובד** | Frontend מקבל products ומחלץ categories |
| `searchProducts(query)` | `GET /api/recommendations/products` (ואז filter client-side) | ✅ **עובד** | Frontend עושה search client-side |

---

## 📊 Data Mapping

### Backend Product → Frontend Product

הפרונט-אנד משתמש בפונקציה `mapBackendProductToFrontend()`:

```typescript
{
  id: String(backendProduct.id),
  name: backendProduct.productName || backendProduct.name || '',
  description: backendProduct.description || '',
  price: backendProduct.price || 0,
  originalPrice: backendProduct.discount ? 
    (backendProduct.price + (backendProduct.discount.value || 0)) : undefined,
  image: backendProduct.imageUrl || backendProduct.image || '',
  images: backendProduct.imageUrl ? [backendProduct.imageUrl] : undefined,
  category: backendProduct.category || '',
  subcategory: undefined, // Backend doesn't have subcategory
  rating: backendProduct.rating || 0,
  reviewCount: 0, // Backend doesn't have reviewCount
  inStock: (backendProduct.quantity || 0) > 0,
  tags: backendProduct.tags || [],
}
```

---

## 🧪 בדיקה עם Swagger

כאשר ה-backend רץ, ניתן לבדוק את כל ה-endpoints דרך Swagger UI:

### 1. גישה ל-Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

### 2. Endpoints שניתן לבדוק ב-Swagger:

#### RecommendationController (`/api/recommendations`)
- ✅ `GET /api/recommendations/guest?limit=10` - המלצות לאורחים
- ✅ `GET /api/recommendations/personalized/{userId}?limit=10` - המלצות מותאמות אישית
- ✅ `GET /api/recommendations/user/{userId}?limit=10` - המלצות למשתמש
- ✅ `GET /api/recommendations/trending?limit=10` - מוצרים פופולריים
- ✅ `GET /api/recommendations/similar/{productId}?limit=5` - מוצרים דומים
- ✅ `GET /api/recommendations/category/{category}?limit=10` - המלצות לפי קטגוריה
- ✅ `GET /api/recommendations/products` - כל המוצרים
- ✅ `POST /api/recommendations/view/{productId}` - רישום צפייה במוצר
- ✅ `POST /api/recommendations/search/{userId}` - רישום חיפוש
- ✅ `POST /api/recommendations/purchase/{userId}` - רישום רכישה

#### ProductController (`/api/products`)
- ✅ `GET /api/products/{id}` - קבלת מוצר לפי ID (Public)
- ✅ `POST /api/products` - יצירת מוצר חדש (Admin Only)
  - Header: `X-User-Email: {adminEmail}`
  - Body: `ProductUpsertRequest`
  - Returns 403 if not admin
- ✅ `PUT /api/products/{id}` - עדכון מוצר (Admin Only)
  - Header: `X-User-Email: {adminEmail}`
  - Body: `ProductUpsertRequest`
  - Returns 403 if not admin
- ✅ `DELETE /api/products/{id}` - מחיקת מוצר (Admin Only)
  - Header: `X-User-Email: {adminEmail}`
  - Returns 403 if not admin

#### AuthController (`/auth`)
- ✅ `POST /auth/login` - התחברות
- ✅ `POST /auth/register` - הרשמה (יצירת משתמש חדש)
  - Body: `{ "name": "string", "email": "string", "password": "string" }`
  - Validations: Email unique, Username unique, Password min 6 chars
- ✅ `GET /auth/test-login?email={email}&password={password}` - בדיקת התחברות
- ✅ `GET /auth/check-credentials?email={email}&password={password}` - בדיקת פרטי התחברות

#### EventController (`/api/events`)
- ✅ `POST /api/events/product-view/{productId}?userId={userId}` - רישום צפייה במוצר

#### HomeController (`/api/home`)
- ✅ `GET /api/home/trending?limit=5` - מוצרים פופולריים

#### HealthController
- ✅ `GET /health` - בדיקת בריאות השרת

---

## ⚠️ הערות חשובות

### 1. שגיאות קומפילציה ב-Backend
כרגע יש שגיאות קומפילציה ב-backend שמונעות ממנו לרוץ:
- `DataInitializer.java` - בעיה עם Product constructor (חסרים `views` ו-`rating`)
- `RecommendationService.java` - בעיה עם ProductSummaryDto constructor (חסר `rating`)

**לאחר תיקון השגיאות, ה-backend יוכל לרוץ והפרונט-אנד יעבוד איתו.**

### 2. ML Service Dependency
חלק מה-endpoints ב-backend תלויים ב-ML Service שרץ על `http://localhost:5000`:
- `/api/recommendations/personalized/{userId}`
- `/api/recommendations/similar/{productId}`
- `/api/recommendations/trending`
- `/api/recommendations/category/{category}`
- `/api/recommendations/guest`

אם ה-ML Service לא רץ, ה-endpoints יחזירו שגיאה או רשימה ריקה.

### 3. CORS
ה-backend מוגדר עם `@CrossOrigin(origins = "*")` ב-`RecommendationController`, כך שהפרונט-אנד יכול לגשת אליו.

### 4. Fallback Mechanism
הפרונט-אנד מוגדר עם fallback ל-mock data אם ה-backend לא זמין או מחזיר שגיאה. זה מאפשר לפרונט-אנד לעבוד גם כשה-backend לא רץ.

---

## ✅ סיכום

**הפרונט-אנד מוכן לחלוטין לעבוד עם ה-backend!**

- ✅ כל ה-endpoints מוגדרים נכון
- ✅ יש mapping בין backend format ל-frontend format
- ✅ יש error handling ו-fallback ל-mock data
- ✅ כל ה-HTTP requests מוגדרים נכון (GET, POST, PUT, DELETE)
- ✅ Headers ו-request bodies מוגדרים נכון

**מה שצריך לעשות:**
1. לתקן את שגיאות הקומפילציה ב-backend
2. להריץ את ה-backend
3. לבדוק עם Swagger UI שהכל עובד
4. הפרונט-אנד יעבוד אוטומטית!

---

## 📝 דוגמאות לבדיקה ב-Swagger

### 1. בדיקת Health:
```
GET http://localhost:8080/health
Response: "Alive."
```

### 2. קבלת כל המוצרים:
```
GET http://localhost:8080/api/recommendations/products
Response: List<Product>
```

### 3. קבלת המלצות לאורחים:
```
GET http://localhost:8080/api/recommendations/guest?limit=5
Response: List<Product>
```

### 4. קבלת מוצר לפי ID:
```
GET http://localhost:8080/api/products/1
Response: Product
```

### 5. התחברות:
```
POST http://localhost:8080/auth/login
Body: { "email": "user@example.com", "password": "password123" }
Response: { "success": true, "message": "Login successful", "user": {...} }
```

### 6. הרשמה:
```
POST http://localhost:8080/auth/register
Body: { "name": "John Doe", "email": "john@example.com", "password": "password123" }
Response: { "success": true, "message": "User registered successfully", "data": {...} }
```

### 7. יצירת מוצר (Admin Only):
```
POST http://localhost:8080/api/products
Headers: { "X-User-Email": "admin@ecommerce.com", "Content-Type": "application/json" }
Body: { "productName": "Product Name", "description": "...", "category": "...", "price": 99.99, "quantity": 10, ... }
Response: Product object
```

### 8. מחיקת מוצר (Admin Only):
```
DELETE http://localhost:8080/api/products/{id}
Headers: { "X-User-Email": "admin@ecommerce.com" }
Response: { "success": true, "message": "Product deleted successfully" }
```

---

---

## 🔐 Admin Functionality

### Admin Authentication
- **Admin Email:** Configured in `application.properties` as `admin.email` (default: `admin@ecommerce.com`)
- **Admin Check:** Both frontend and backend check if user email matches configured admin email
- **Admin Access:** Required for product creation, update, and deletion

### Admin Restrictions
- **Email Change:** Admin cannot change their email (prevented in both frontend and backend)
- **Username Change:** Admin can change their username
- **Product Management:** Only admin can create, update, or delete products

### Admin Endpoints
All product modification endpoints require:
- Header: `X-User-Email: {adminEmail}`
- The email must match the configured admin email
- Returns `403 Forbidden` if user is not admin

### CSV Import Endpoint
- **Endpoint:** `POST /api/products/import`
- **Content-Type:** `multipart/form-data`
- **Header:** `X-User-Email: {adminEmail}`
- **Body:** `file: [CSV file]`
- **Response:** Import statistics with success/error counts
- **CSV Format:** See `ADMIN_FUNCTIONALITY.md` for details

---

## 🛒 Cart Management Features

### Cart Modal
- **Location:** Cart icon in navbar (opens modal overlay)
- **Component:** `components/cart/CartModal.tsx`
- **Behavior:** Modal overlay (similar to login/register modals)
- **Features:**
  - Closes when clicking outside or X button
  - Shows all cart items with quantity controls
  - Detects and marks removed products
  - Calculates totals excluding removed products
  - Option to remove unavailable items

### Removed Product Detection
When admin deletes a product:
- Product is removed from database
- All cart items referencing the product are automatically deleted (backend)
- Frontend detects removed products when cart is opened
- Removed products are marked with visual indicators:
  - Red circle with `!` icon on product image
  - "Removed" badge next to product name
  - Red border and reduced opacity
  - Warning message: "This product is no longer available"
- Removed products are excluded from cart total calculation

### Cart Context (`UserContext.tsx`)
- **Storage:** localStorage (`user_cart`)
- **Functions:**
  - `addToCart(product, quantity)` - Add product to cart
  - `removeFromCart(productId)` - Remove product from cart
  - `updateCartQuantity(productId, quantity)` - Update quantity
  - `clearCart()` - Clear all items
- **State:** `cart: CartItem[]` - Array of cart items

---

**נוצר ב:** 2026-01-10
**עודכן ב:** 2026-01-25
