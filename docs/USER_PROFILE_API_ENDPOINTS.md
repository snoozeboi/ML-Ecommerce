# User Profile API Endpoints - Complete Reference

## 📍 API Endpoint Locations

### Backend Controller
**File:** `backend/src/main/java/com/shop/ecommerce/controller/UserController.java`
**Base Path:** `/api/users`

---

## 🔌 Available Endpoints

### 0. **POST Register User** (Auth API)
- **Endpoint:** `POST /auth/register`
- **Controller Method:** `register(@RequestBody Map<String, String> registerRequest)`
- **Service Method:** `AuthService.registerUser(userName, email, password)`
- **Purpose:** Create a new user account
- **Request Body:** `{ "name": "string", "email": "string", "password": "string" }`
- **Returns:** User data (id, username, email, wallet, avatar)

**Validations:**
- Email must be unique (case-insensitive, normalized to lowercase)
- Username must be unique
- Password must be at least 6 characters
- Email is required
- Username is derived from `name` field, or email prefix if name not provided

**Error Responses:**
- `"Email already registered"` - Email already exists
- `"Username already taken"` - Username already exists
- `"Password must be at least 6 characters"` - Password too short
- `"Email is required"` - Email field missing
- `"Username is required"` - Name/username field missing

**Location in Code:**
- Controller: `AuthController.java` line 26-43
- Service: `AuthService.java` line 66-139

**Frontend Function:**
- File: `frontend/curated-cart-main/src/services/api.ts`
- Function: `register(name, email, password)` (line 724-758)
- Called from: `UserContext.tsx` → `register()` → `AuthModal.tsx` → `handleSubmit()`

---

### 1. **GET User Profile**
- **Endpoint:** `GET /api/users/{userId}`
- **Controller Method:** `getUserProfile(@PathVariable int userId)`
- **Service Method:** `UserService.getUserProfile(userId)`
- **Purpose:** Retrieve user profile information
- **Returns:** User data (id, username, email, wallet, avatar, createdAt)

**Location in Code:**
- Controller: `UserController.java` line 17-20
- Service: `UserService.java` line 23-46

---

### 2. **PUT Update Profile (Username & Email)**
- **Endpoint:** `PUT /api/users/{userId}`
- **Controller Method:** `updateUserProfile(@PathVariable int userId, @RequestBody Map<String, String> updateRequest)`
- **Service Method:** `UserService.updateUserProfile(userId, username, email)`
- **Purpose:** Update username and/or email
- **Request Body:** `{ "username": "string", "email": "string" }`
- **Returns:** Updated user data

**Location in Code:**
- Controller: `UserController.java` line 22-30
- Service: `UserService.java` line 48-116

**Frontend Function:**
- File: `frontend/curated-cart-main/src/services/api.ts`
- Function: `updateUserProfile(userId, username?, email?)` (line 1007-1040)
- Called from: `UserContext.tsx` → `updateProfile()` → `Profile.tsx` → `handleSaveProfile()`

**⚠️ Admin Restrictions:**
- **Admin users cannot change their email** (for security reasons)
- Admin email field is disabled in the UI
- Backend validation prevents admin email changes
- Error message: "Admin email cannot be changed for security reasons"
- Admin can still change their username

---

### 3. **PUT Change Password**
- **Endpoint:** `PUT /api/users/{userId}/password`
- **Controller Method:** `changePassword(@PathVariable int userId, @RequestBody Map<String, String> passwordRequest)`
- **Service Method:** `UserService.changePassword(userId, currentPassword, newPassword)`
- **Purpose:** Change user password
- **Request Body:** `{ "currentPassword": "string", "newPassword": "string" }`
- **Returns:** Success/error message

**Location in Code:**
- Controller: `UserController.java` line 32-40
- Service: `UserService.java` line 118-160

**Frontend Function:**
- File: `frontend/curated-cart-main/src/services/api.ts`
- Function: `changePassword(userId, currentPassword, newPassword)` (line 1047-1079)
- Called from: `Profile.tsx` → `handleChangePassword()`

---

## 🎯 Frontend Implementation

### Profile Page
**File:** `frontend/curated-cart-main/src/pages/Profile.tsx`

#### Profile Settings Section (Username & Email)
- **UI Location:** Settings Tab → "Profile Settings" Card
- **Fields:** Username, Email
- **Button:** "Save Changes"
- **Handler:** `handleSaveProfile()` (line 22-30)
- **Calls:** `updateProfile({ username, email })` from UserContext
- **API Endpoint:** `PUT /api/users/{userId}`

#### Change Password Section
- **UI Location:** Settings Tab → "Change Password" Card
- **Fields:** Current Password, New Password, Confirm New Password
- **Button:** "Change Password"
- **Handler:** `handleChangePassword()` (line 32-60)
- **Calls:** `changePassword(userId, currentPassword, newPassword)` from api.ts
- **API Endpoint:** `PUT /api/users/{userId}/password`

---

## 🔄 Data Flow

### Username/Email Update Flow:
```
Profile.tsx (handleSaveProfile)
  ↓
UserContext.tsx (updateProfile)
  ↓
api.ts (updateUserProfile)
  ↓
PUT /api/users/{userId}
  ↓
UserController.updateUserProfile()
  ↓
UserService.updateUserProfile()
  ↓
PostgreSQL Database (users table)
```

### Password Change Flow:
```
Profile.tsx (handleChangePassword)
  ↓
api.ts (changePassword)
  ↓
PUT /api/users/{userId}/password
  ↓
UserController.changePassword()
  ↓
UserService.changePassword()
  ↓
PostgreSQL Database (users table - password_hash column)
```

---

## 🐛 Troubleshooting

### If Password Change Doesn't Work:

1. **Check Browser Console:**
   - Open DevTools (F12) → Console tab
   - Look for error messages when clicking "Change Password"

2. **Check Network Tab:**
   - Open DevTools (F12) → Network tab
   - Click "Change Password"
   - Look for request to `/api/users/{userId}/password`
   - Check response status and body

3. **Verify Backend is Running:**
   - Check if Spring Boot server is running
   - Check Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Look for endpoint: `PUT /api/users/{userId}/password`

4. **Common Issues:**
   - ❌ Wrong current password → Error: "Current password is incorrect"
   - ❌ New password too short → Error: "New password must be at least 6 characters"
   - ❌ Passwords don't match → Frontend validation error
   - ❌ User ID not found → Error: "User not found"
   - ❌ Backend not running → Network error

---

## 📋 Summary

| Feature | Endpoint | Frontend Location | Status |
|---------|----------|-------------------|--------|
| **Register User** | `POST /auth/register` | Auth Modal → Register tab | ✅ Working |
| **Get Profile** | `GET /api/users/{userId}` | Not used in UI | ✅ Working |
| **Update Username/Email** | `PUT /api/users/{userId}` | Profile → Settings → "Save Changes" | ✅ Working<br/>⚠️ Admin cannot change email |
| **Change Password** | `PUT /api/users/{userId}/password` | Profile → Settings → "Change Password" | ✅ Should Work |

---

## 🔍 Quick Test

### Test Username/Email Update:
1. Go to Profile page
2. Change username or email
3. Click "Save Changes"
4. Check PostgreSQL: `SELECT * FROM users WHERE id = {userId};`

### Test Password Change:
1. Go to Profile page → Settings tab
2. Scroll to "Change Password" section
3. Enter:
   - Current password (your actual password)
   - New password (min 6 characters)
   - Confirm new password (must match)
4. Click "Change Password"
5. Try logging in with new password

---

## 📝 Notes

- **Registration** creates a new user with unique email and username
- **Username and Email** are updated together via the same endpoint
- **Password** is updated separately via a different endpoint
- Both endpoints require the user to be logged in (userId from session)
- Password change requires verification of current password
- All changes are saved to PostgreSQL database
- **Admin users** cannot change their email (restricted in both frontend UI and backend validation)
- Email is normalized to lowercase for consistency
- Duplicate email registration is prevented (database unique constraint + application validation)