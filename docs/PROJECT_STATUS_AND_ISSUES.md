# Project Status & Issues Report

**Last Updated:** January 2025

This document provides a comprehensive overview of what's missing, incomplete, or not working well in the e-commerce project.

---

## ✅ Recently Completed

1. **User Registration API** - ✅ **COMPLETED**
   - Backend endpoint: `POST /auth/register`
   - Frontend integration working
   - Users are saved to PostgreSQL database

2. **Login/Register UI** - ✅ **COMPLETED**
   - Login and Register buttons properly open respective modals
   - Error handling improved
   - Email validation added

3. **On Sale Page** - ✅ **COMPLETED**
   - Separate page from Categories
   - Shows only products on sale
   - Category filtering works while maintaining sale-only filter

---

## 🔴 Critical Missing Features

### 1. Cart Management API
- **Status:** ❌ Missing
- **Models:** `CartItem` model exists in backend
- **Repository:** `CartItemRepository` exists
- **Frontend:** Uses localStorage for cart (no backend integration)
- **Missing Endpoints:**
  - `GET /api/cart/{userId}` - Get user's cart
  - `POST /api/cart/{userId}/items` - Add item to cart
  - `PUT /api/cart/{userId}/items/{itemId}` - Update cart item quantity
  - `DELETE /api/cart/{userId}/items/{itemId}` - Remove item from cart
  - `DELETE /api/cart/{userId}` - Clear cart
- **Impact:** Cart data is not persisted, lost on logout/device change
- **Priority:** HIGH

### 2. Order Management API
- **Status:** ❌ Missing
- **Models:** No `Order` model exists in backend
- **Frontend:** Uses mock orders stored in localStorage
- **Missing Endpoints:**
  - `GET /api/orders/{userId}` - Get user's orders
  - `POST /api/orders` - Create new order from cart
  - `GET /api/orders/{orderId}` - Get order details
  - `PUT /api/orders/{orderId}/status` - Update order status
- **Impact:** No order history, no purchase tracking in backend
- **Priority:** HIGH

### 3. Wishlist API
- **Status:** ❌ Missing
- **Models:** `wishList` field exists in `User` model (List<Integer>)
- **Frontend:** Uses localStorage for wishlist
- **Missing Endpoints:**
  - `GET /api/wishlist/{userId}` - Get user's wishlist
  - `POST /api/wishlist/{userId}/items` - Add product to wishlist
  - `DELETE /api/wishlist/{userId}/items/{productId}` - Remove from wishlist
- **Impact:** Wishlist not persisted, lost on logout
- **Priority:** MEDIUM

### 4. User Profile Management API
- **Status:** ❌ Missing
- **Frontend:** Has profile page with update functionality
- **Missing Endpoints:**
  - `GET /api/users/{userId}` - Get user profile
  - `PUT /api/users/{userId}` - Update user profile
  - `PUT /api/users/{userId}/password` - Change password
- **Impact:** Users cannot update their profile information
- **Priority:** MEDIUM

### 5. JWT Authentication / Session Management
- **Status:** ❌ Missing
- **Current:** Basic password checking, no tokens
- **Missing:**
  - JWT token generation on login
  - Token refresh mechanism
  - Protected routes/endpoints
  - Session management
- **Impact:** No secure authentication, no stateless API, security vulnerability
- **Priority:** HIGH

### 6. Product Search API (Backend)
- **Status:** ⚠️ Partial
- **Current:** Frontend does client-side filtering (inefficient)
- **Backend:** Has `ProductRepository.findByProductNameContainingIgnoreCase()` but no controller endpoint
- **Missing:** 
  - `GET /api/products/search?q={query}` - Search products by name/description
  - Elasticsearch integration for advanced search (mentioned in TODOLIST)
- **Impact:** Inefficient for large product catalogs, no full-text search
- **Priority:** MEDIUM

---

## 🟡 Incomplete Implementations

### 7. Compilation Errors in Backend
- **Status:** ✅ **FIXED**
- **Location:** `DataInitializer.java` and `RecommendationService.java`
- **Resolution:** Product constructor already receives `views` and `rating`; `ProductSummaryDto` includes all fields. Backend compiles successfully.
- **Priority:** ~~HIGH~~ (resolved)

### 8. RabbitMQ Integration
- **Status:** ⚠️ Partial
- **TODOLIST Items:**
  - Setup RabbitMQ template (Queue, Exchange, Binding)
  - Setup RabbitMQ sender/consumer
  - Test ElasticSearch with RabbitMQ
- **Current:** `ElasticEventConsumer` exists but RabbitMQ configuration incomplete
- **Impact:** Event-driven architecture not fully functional
- **Priority:** MEDIUM

### 9. Elasticsearch Integration
- **Status:** ⚠️ Partial
- **Current:** 
  - `ElasticService` and `ProductIndexService` exist
  - Consumer exists but integration incomplete
- **Missing:**
  - Full Elasticsearch search endpoint
  - Search box feature through Elasticsearch (mentioned in TODOLIST)
  - Write to Elasticsearch on product updates
- **Impact:** No advanced search capabilities
- **Priority:** MEDIUM

### 10. ML Service Integration
- **Status:** ⚠️ Partial
- **Current:** Backend calls ML service at `http://localhost:5000`
- **Missing:**
  - Error handling when ML service is down
  - Fallback mechanisms
  - Health checks for ML service
  - Container setup for ML service (mentioned in TODOLIST)
- **Impact:** Recommendations fail when ML service unavailable
- **Priority:** MEDIUM

### 11. Popular Products Cache
- **Status:** ❌ Missing
- **Documentation:** Mentioned in `Structure.txt` - should cache top X products by purchase count
- **Missing:**
  - Periodic cache refresh (every 20-30 seconds)
  - TTL for cache
  - Fallback to PostgreSQL if Elasticsearch is down
- **Impact:** Popular products endpoint may be slow
- **Priority:** LOW

### 12. Error Handling & Validation
- **Status:** ⚠️ Partial
- **Missing:**
  - Consistent error response format across all endpoints
  - Input validation (Bean Validation) in backend
  - Global exception handler
  - Proper HTTP status codes (some endpoints return 200 on error)
- **Impact:** Inconsistent error responses, harder to debug
- **Priority:** MEDIUM

---

## 🟢 Missing Features (Nice to Have)

### 13. Payment Processing
- **Status:** ❌ Missing
- **Impact:** No payment integration, orders cannot be completed
- **Priority:** LOW (for MVP)

### 14. Email Notifications
- **Status:** ❌ Missing
- **Missing:**
  - Registration confirmation emails
  - Order confirmation emails
  - Password reset emails
- **Impact:** No user communication
- **Priority:** LOW

### 15. Password Reset
- **Status:** ❌ Missing
- **Missing Endpoints:**
  - `POST /auth/forgot-password` - Request password reset
  - `POST /auth/reset-password` - Reset password with token
- **Impact:** Users cannot recover forgotten passwords
- **Priority:** MEDIUM

### 16. Product Reviews/Ratings
- **Status:** ❌ Missing
- **Models:** No `Review` or `Rating` model
- **Missing Endpoints:**
  - `GET /api/products/{id}/reviews` - Get product reviews
  - `POST /api/products/{id}/reviews` - Add review
  - `PUT /api/reviews/{reviewId}` - Update review
  - `DELETE /api/reviews/{reviewId}` - Delete review
- **Impact:** No user-generated content, no social proof
- **Priority:** LOW

### 17. Product Images Management
- **Status:** ⚠️ Partial
- **Current:** Products have `imageUrl` field
- **Missing:**
  - Image upload endpoint
  - Multiple images per product
  - Image storage (S3, local storage, etc.)
- **Impact:** Limited product presentation
- **Priority:** LOW

### 18. Categories API
- **Status:** ⚠️ Partial
- **Current:** Frontend extracts categories from products list
- **Missing:**
  - `GET /api/categories` - Get all categories
  - `GET /api/categories/{category}/products` - Get products by category
- **Impact:** Inefficient category management
- **Priority:** LOW

### 19. Admin Dashboard API
- **Status:** ⚠️ Partial
- **Current:** `AdminController` exists but limited functionality
- **Missing:**
  - User management endpoints
  - Order management endpoints
  - Analytics endpoints
  - Dashboard statistics
- **Impact:** Limited admin capabilities
- **Priority:** LOW

### 20. API Documentation
- **Status:** ⚠️ Partial
- **Current:** `FRONTEND_BACKEND_API_DOCUMENTATION.md` exists (Hebrew)
- **Missing:**
  - Swagger/OpenAPI documentation
  - English documentation
  - API versioning
- **Impact:** Harder for developers to integrate
- **Priority:** LOW

### 21. Testing
- **Status:** ⚠️ Partial
- **Current:** Some test files exist in `tests/` (Python ML tests)
- **Missing:**
  - Backend unit tests
  - Backend integration tests
  - API endpoint tests
  - Frontend tests
- **Impact:** No automated testing, higher risk of bugs
- **Priority:** MEDIUM

### 22. Logging & Monitoring
- **Status:** ⚠️ Partial
- **Missing:**
  - Structured logging
  - Log aggregation
  - Application monitoring
  - Performance metrics
- **Impact:** Hard to debug production issues
- **Priority:** LOW

### 23. Docker Compose Setup
- **Status:** ⚠️ Partial
- **Current:** `docker-compose.yml` exists in backend
- **Missing:**
  - Frontend container setup (mentioned in TODOLIST)
  - ML service container setup (mentioned in TODOLIST)
  - Complete development environment
- **Impact:** Harder to set up development environment
- **Priority:** LOW

### 24. Environment Configuration
- **Status:** ⚠️ Partial
- **Missing:**
  - Environment-specific configs (dev, staging, prod)
  - Secrets management
  - Configuration validation
- **Impact:** Harder to deploy to different environments
- **Priority:** LOW

---

## 🐛 Known Issues & Bugs

### 1. Frontend Fallback to Mock Data
- **Issue:** Many frontend functions fall back to mock data when backend fails
- **Impact:** Users may see data even when backend is down, leading to confusion
- **Location:** `frontend/curated-cart-main/src/services/api.ts`
- **Priority:** MEDIUM

### 2. No Wallet Field in Database
- **Issue:** User model doesn't have wallet field, but frontend expects it
- **Current Workaround:** Backend returns wallet: 0 for all users
- **Impact:** Cannot track user balances
- **Priority:** LOW (if wallet feature is needed)

### 3. Search is Client-Side Only
- **Issue:** Product search happens in frontend, not backend
- **Impact:** Inefficient for large catalogs, all products must be loaded
- **Priority:** MEDIUM

### 4. No Input Validation in Backend
- **Issue:** Backend doesn't use Bean Validation annotations
- **Impact:** Invalid data can be saved, potential security issues
- **Priority:** MEDIUM

### 5. CORS Configuration
- **Status:** ✅ **FIXED**
- **Resolution:** Removed `@CrossOrigin(origins = "*")` from all controllers. Global CORS in `SecurityConfig` uses specific origins (localhost:4200, 3000, 5173).
- **Priority:** ~~MEDIUM~~ (resolved)

### 6. No Rate Limiting
- **Issue:** No rate limiting on API endpoints
- **Impact:** Vulnerable to abuse, DDoS attacks
- **Priority:** MEDIUM

---

## 📋 TODOLIST Items (From backend/TODOLIST)

1. Check if UserSearchHistory ("search_count") is needed for the ML
2. Understand and help to fix the ML with the new "search_count" field, if not needed then revert
3. Setup a container for ML service if needed
4. Setup a container for frontend
5. Setup RabbitMQ template (define Queue, Exchange, Binding), Inject the template into service
6. Setup RabbitMQ sender/consumer
7. Test ElasticSearch with RabbitMQ
8. Polish existing code
9. Discuss ElasticSearch features (like search box feature of the site through Elastic)
10. Update the code so that it writes to ElasticSearch as well

---

## 📊 Summary by Priority

### 🔴 High Priority (Critical for MVP)
1. **Cart Management API** - Users can't save cart items
2. **Order Management API** - No way to complete purchases
3. **JWT Authentication** - Security vulnerability
4. **Fix Compilation Errors** - Backend may not run
5. **Error Handling** - Inconsistent responses

### 🟡 Medium Priority (Important for Production)
6. **Wishlist API** - Data persistence
7. **User Profile Management API** - User experience
8. **Product Search API (Backend)** - Performance
9. **RabbitMQ Integration** - Event-driven architecture
10. **Elasticsearch Integration** - Advanced search
11. **ML Service Error Handling** - Reliability
12. **Testing** - Code quality

### 🟢 Low Priority (Nice to Have)
13. Payment Processing
14. Email Notifications
15. Password Reset
16. Product Reviews/Ratings
17. Admin Dashboard Enhancements
18. API Documentation (Swagger)
19. Logging & Monitoring
20. Docker Compose Setup

---

## 🔧 Quick Wins (Easy to Implement)

1. **Wishlist API** - Simple CRUD operations on `User.wishList` field
2. **User Profile GET/PUT** - Basic user information endpoints
3. **Product Search Endpoint** - Use existing repository method
4. **Fix CORS** - Replace `*` with specific origins
5. **Add Input Validation** - Add `@Valid` annotations to controllers

---

## 📝 Notes

- **Registration API:** ✅ Now implemented and working
- **Login/Register UI:** ✅ Fixed and working correctly
- **On Sale Page:** ✅ Separated from Categories page
- Frontend is well-prepared with API calls already implemented
- Many features use localStorage as fallback (cart, wishlist, orders)
- Backend models exist but many controllers/services are missing
- Documentation mentions features that aren't implemented yet

---

## 🎯 Recommended Next Steps

1. **Implement Cart Management API** - High user impact
2. **Add JWT Authentication** - Security critical
3. **Create Order Management API** - Essential for e-commerce
4. **Fix any compilation errors** - Ensure backend runs
5. **Add proper error handling** - Improve developer experience

---

**Generated:** January 2025
**Project:** ML-eCommers E-Commerce Platform
