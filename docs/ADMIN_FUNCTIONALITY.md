# Admin Functionality Documentation

## Overview

The system implements admin functionality using email-based authentication. Only users with the configured admin email can perform administrative actions.

---

## 🔐 Admin Configuration

### Backend Configuration
**File:** `backend/src/main/resources/application.properties`

```properties
admin.email=${ADMIN_EMAIL:admin@ecommerce.com}
```

- Default admin email: `admin@ecommerce.com`
- Can be overridden via environment variable: `ADMIN_EMAIL`
- Admin email is used for authentication checks in both frontend and backend

### Default Admin Credentials
- **Email:** `admin@ecommerce.com`
- **Password:** `admin123`
- Created automatically by `DataInitializer.java` on first startup

---

## 🛡️ Admin Authentication

### Frontend (`UserContext.tsx`)
- **Constant:** `ADMIN_EMAIL = 'admin@ecommerce.com'`
- **Check:** `isAdmin = user?.email?.toLowerCase().trim() === ADMIN_EMAIL.toLowerCase().trim()`
- Automatically updates when user email changes
- Exposed via `useUser()` hook: `const { isAdmin } = useUser()`

### Backend (`AuthService.java`)
- **Method:** `isAdmin(String email)`
- **Check:** Compares provided email against configured admin email from `application.properties`
- Used by `ProductController` to validate admin access

---

## 📋 Admin Features

### 1. Product Management
**Location:** `/admin` page (frontend)

**Capabilities:**
- ✅ View all products
- ✅ Create new products
- ✅ Edit existing products
- ✅ Delete products (with confirmation)
- ✅ Preview product images while editing
- ✅ Bulk import products from CSV file

**Access:**
- Only visible to admin users
- Non-admin users are redirected to home page
- Admin link appears in navbar (desktop and mobile)

### 2. Protected Endpoints

All product modification endpoints require admin authentication:

| Endpoint | Method | Header Required | Description |
|----------|--------|----------------|-------------|
| `/api/products` | POST | `X-User-Email: {adminEmail}` | Create new product |
| `/api/products/{id}` | PUT | `X-User-Email: {adminEmail}` | Update product |
| `/api/products/{id}` | DELETE | `X-User-Email: {adminEmail}` | Delete product |
| `/api/products/{id}` | GET | None | View product (public) |
| `/api/products/import` | POST | `X-User-Email: {adminEmail}` | Bulk import products from CSV |

**Response Codes:**
- `200 OK` - Success
- `403 Forbidden` - User is not admin
- `404 Not Found` - Product not found
- `400 Bad Request` - Invalid request data

### 3. CSV Bulk Import

**Location:** Admin panel → "Import CSV" button

**Features:**
- Upload CSV file to import multiple products at once
- Automatic parsing of CSV format
- Flexible column mapping (handles various column name formats)
- Error handling with detailed feedback
- Shows import statistics (success count, error count)

**CSV Format:**
- **Required columns:** `product_name`, `category`, `price`, `quantity`
- **Optional columns:** `description`, `sub_category`, `brand`, `image_url`, `views`, `rating`, `tags`
- **Tags format:** Comma-separated (e.g., "tag1,tag2,tag3")
- **Template:** Available at `backend/src/main/resources/product_import_template.csv`

**How it works:**
1. Admin clicks "Import CSV" button in admin panel
2. Selects a CSV file
3. System parses CSV and creates products
4. Returns import results with success/error counts
5. Products are automatically published to RabbitMQ for Elasticsearch indexing

**Example CSV:**
```csv
product_name,description,category,sub_category,brand,price,quantity,image_url,views,rating,tags
"Product 1","Description","Electronics","Smartphones","BrandA",99.99,50,"https://example.com/image.jpg",0,4.5,"electronics,smartphone"
```

### 4. Product Deletion with Cart Cleanup

**Automatic Cart Cleanup:**
- When admin deletes a product, all cart items referencing that product are automatically removed
- Prevents foreign key constraint violations
- Users will see removed products marked in their cart with an indicator

**Implementation:**
- Backend automatically deletes `CartItem` records before deleting the product
- Uses `CartItemRepository.deleteByProductId()` method
- Frontend detects removed products and shows visual indicators

---

## 🚫 Admin Restrictions

### Email Change Prevention

**Why:** Admin email is used for authentication. Changing it would lock the admin out.

**Implementation:**

1. **Frontend (`Profile.tsx`):**
   - Email field is disabled for admin users
   - Shows helper text: "Admin email cannot be changed for security reasons"
   - Only sends username in update request for admin

2. **Backend (`UserService.java`):**
   - Validates if user is admin before allowing email change
   - Returns error: "Admin email cannot be changed for security reasons"
   - Admin can still change username

**Code Location:**
- Frontend: `frontend/curated-cart-main/src/pages/Profile.tsx` (line 113-121)
- Backend: `backend/src/main/java/com/shop/ecommerce/service/UserService.java` (line 76-88)

---

## 📁 File Changes Summary

### Backend Files Modified:
1. **`AuthService.java`**
   - Added `@Value("${admin.email:admin@ecommerce.com}")` injection
   - Added `isAdmin(String email)` method

2. **`ProductController.java`**
   - Added admin authentication checks
   - Protected POST, PUT, DELETE endpoints
   - Added `X-User-Email` header requirement
   - Added CSV import endpoint (`POST /api/products/import`)
   - Added error handling for delete operations

3. **`ProductService.java`**
   - Added `importProductsFromCsv(MultipartFile file)` method
   - Added CSV parsing logic with flexible column mapping
   - Updated `delete()` method to remove cart items before product deletion
   - Added helper methods for CSV parsing

4. **`CartItemRepository.java`**
   - Added `deleteByProductId(int productId)` method
   - Uses `@Modifying` and `@Transactional` annotations

5. **`UserService.java`**
   - Added admin email validation
   - Prevents admin from changing email

6. **`application.properties`**
   - Added `admin.email` configuration

### Frontend Files Modified:
1. **`UserContext.tsx`**
   - Added `isAdmin` boolean to context
   - Added `ADMIN_EMAIL` constant
   - Calculates `isAdmin` based on user email

2. **`api.ts`**
   - Updated `createProduct()` to accept `userEmail` parameter
   - Updated `updateProduct()` to accept `userEmail` parameter
   - Updated `deleteProduct()` to accept `userEmail` parameter
   - All send `X-User-Email` header

3. **`Admin.tsx`** (NEW)
   - Complete admin product management interface
   - Create, edit, delete products
   - Image preview functionality
   - CSV import functionality with file upload
   - Import progress and error reporting

4. **`App.tsx`**
   - Added `/admin` route

5. **`Navbar.tsx`**
   - Added "Admin" link (visible only to admins)

6. **`Profile.tsx`**
   - Disabled email field for admin users
   - Added helper text for admin email restriction

---

## 🔄 API Request Examples

### Create Product (Admin Only)
```http
POST /api/products
Headers:
  Content-Type: application/json
  X-User-Email: admin@ecommerce.com
Body:
{
  "productName": "New Product",
  "description": "Product description",
  "category": "Electronics",
  "price": 99.99,
  "quantity": 10,
  "imageUrl": "https://example.com/image.jpg",
  "tags": ["tag1", "tag2"]
}
```

### Update Product (Admin Only)
```http
PUT /api/products/1
Headers:
  Content-Type: application/json
  X-User-Email: admin@ecommerce.com
Body:
{
  "productName": "Updated Product",
  "price": 149.99
}
```

### Delete Product (Admin Only)
```http
DELETE /api/products/1
Headers:
  X-User-Email: admin@ecommerce.com
```

### Import Products from CSV (Admin Only)
```http
POST /api/products/import
Headers:
  X-User-Email: admin@ecommerce.com
Body: (multipart/form-data)
  file: [CSV file]
Response:
{
  "success": true,
  "message": "Import completed",
  "totalRows": 100,
  "successCount": 95,
  "errorCount": 5,
  "errors": ["Row 10: Product name is required", ...]
}
```

### Failed Request (Non-Admin)
```http
POST /api/products
Headers:
  Content-Type: application/json
  X-User-Email: user@example.com
Response:
{
  "success": false,
  "message": "Unauthorized: Admin access required"
}
Status: 403 Forbidden
```

---

## 🧪 Testing Admin Functionality

### 1. Test Admin Login
1. Navigate to frontend: `http://localhost:5173`
2. Click login
3. Enter:
   - Email: `admin@ecommerce.com`
   - Password: `admin123`
4. Verify "Admin" link appears in navbar

### 2. Test Admin Panel Access
1. Click "Admin" link in navbar
2. Should see product management interface
3. Try creating/editing/deleting products

### 3. Test Non-Admin Access
1. Login as regular user
2. Try accessing `/admin` directly
3. Should be redirected to home page
4. "Admin" link should not appear in navbar

### 4. Test Email Change Restriction
1. Login as admin
2. Go to Profile page
3. Verify email field is disabled
4. Try changing username (should work)
5. Verify helper text appears

### 5. Test Backend Protection
1. Use Postman/curl to call product endpoints
2. Without `X-User-Email` header → Should return 403
3. With non-admin email → Should return 403
4. With admin email → Should succeed

---

## ⚠️ Important Notes

1. **Single Admin System:** Currently supports only one admin (email-based)
2. **Email is Key:** Admin access is determined by email matching
3. **No Role Field:** System uses email comparison, not a database role field
4. **Security:** Admin email cannot be changed to prevent lockout
5. **Configuration:** Admin email is configurable via `application.properties`

---

## 🔮 Future Enhancements

Potential improvements:
- [ ] Add role-based system (isAdmin field in User entity)
- [ ] Support multiple admins
- [ ] Admin activity logging
- [ ] Admin permission levels
- [ ] Admin dashboard with statistics

---

**Created:** 2026-01-25  
**Last Updated:** 2026-01-25

---

## 🛒 Cart Management Features

### Removed Product Detection

When admin deletes a product:
- Product is removed from database
- All cart items referencing the product are automatically deleted
- Frontend detects removed products when cart is opened
- Removed products are marked with visual indicators:
  - Red circle with `!` icon on product image
  - "Removed" badge next to product name
  - Red border and reduced opacity
  - Warning message: "This product is no longer available"

### Cart Modal

**Location:** Cart icon in navbar (opens modal overlay)

**Features:**
- Modal overlay (similar to login/register modals)
- Closes when clicking outside or X button
- Shows all cart items with quantity controls
- Displays removed products with indicators
- Calculates totals excluding removed products
- Option to remove unavailable items
- Proceed to checkout (disabled if only removed items)

**Implementation:**
- Component: `components/cart/CartModal.tsx`
- Uses `createPortal` for modal rendering
- Integrated into `Navbar.tsx`
- Cart page redirects to home (cart is now modal-only)
