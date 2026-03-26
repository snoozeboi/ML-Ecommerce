# User Profile Management API - Architecture Guide

## 📊 Data Flow Architecture

### Current System Structure:

```
┌─────────────────┐
│   Frontend      │
│  (React/TS)     │
└────────┬────────┘
         │ HTTP API
         ▼
┌─────────────────┐
│   Backend       │
│  (Spring Boot)  │
└────────┬────────┘
         │ JPA/Hibernate
         ▼
┌─────────────────┐      ┌──────────────┐
│  PostgreSQL     │      │  CSV Files   │
│  (Source of     │      │  (Read-only  │
│   Truth)        │      │   ML Data)   │
└─────────────────┘      └──────────────┘
         │
         │ RabbitMQ (optional)
         ▼
┌─────────────────┐
│   ML Service    │
│   (Python)      │
└─────────────────┘
```

## ✅ Correct Approach: PostgreSQL First

### **YES, User Profile Management should have an API**

### **Update Order: PostgreSQL → (Optional) ML Service**

**PostgreSQL is the source of truth** - All user data should be stored and updated in PostgreSQL.

**CSV files are read-only** - They are source data for ML training, NOT meant to be updated by the backend.

---

## 🔄 Recommended Update Flow

### 1. **Primary Update: PostgreSQL**
```
User updates profile → Backend API → PostgreSQL Database
```

### 2. **Optional: Notify ML Service (if needed)**
```
PostgreSQL updated → RabbitMQ message → ML Service (for reclassification)
```

### 3. **CSV Files: DO NOT UPDATE**
- CSV files in `datasets/raw/` are **read-only source data**
- Used by ML service for training/analysis
- Backend should **never write to CSV files**
- ML service reads from PostgreSQL or RabbitMQ messages

---

## 📝 Implementation Plan

### Step 1: Create User Profile API Endpoints

**Backend Endpoints Needed:**
- `GET /api/users/{userId}` - Get user profile
- `PUT /api/users/{userId}` - Update user profile (username, email)
- `PUT /api/users/{userId}/password` - Change password

### Step 2: Update Flow

```java
// 1. Update PostgreSQL (PRIMARY)
User user = userRepository.findById(userId).orElseThrow();
user.setUserName(newUsername);
user.setEmail(newEmail);
userRepository.save(user); // Saves to PostgreSQL

// 2. (Optional) Notify ML Service if profile change affects classification
if (usernameChanged || emailChanged) {
    // Only if ML service needs to know about profile changes
    mlEventPublisher.publishUserReadyCsv(user);
}
```

### Step 3: Frontend Integration

```typescript
// Frontend calls backend API
const response = await fetch(`/api/users/${userId}`, {
  method: 'PUT',
  body: JSON.stringify({ username, email })
});
// Updates PostgreSQL, frontend gets updated data
```

---

## ❌ What NOT to Do

### **DO NOT:**
1. ❌ Write to CSV files from backend
2. ❌ Update CSV files when user profile changes
3. ❌ Use CSV files as primary data storage
4. ❌ Sync CSV files with PostgreSQL

### **CSV Files Purpose:**
- ✅ Read-only source data for ML training
- ✅ Initial data import (one-time)
- ✅ ML service reads from them for analysis
- ✅ Backend sends CSV messages via RabbitMQ (not file writes)

---

## 🎯 Recommended Implementation Order

### **Priority 1: PostgreSQL Update (Required)**
1. Create `UserController` with profile endpoints
2. Create `UserService` with update logic
3. Update PostgreSQL database
4. Return updated user data to frontend

### **Priority 2: ML Service Notification (Optional)**
1. If profile changes affect ML classification
2. Send RabbitMQ message to ML service
3. ML service can reclassify user if needed

### **Priority 3: CSV Files (Never)**
- ❌ Do not update CSV files
- CSV files remain read-only source data

---

## 📋 Example Implementation

### Backend Service:

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MLEventPublisher mlPublisher; // Optional
    
    public User updateUserProfile(int userId, String username, String email) {
        // 1. Get user from PostgreSQL
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // 2. Update fields
        boolean usernameChanged = !user.getUserName().equals(username);
        user.setUserName(username);
        user.setEmail(email);
        user.setLastActivity(LocalDateTime.now());
        
        // 3. Save to PostgreSQL (PRIMARY STORAGE)
        User updatedUser = userRepository.save(user);
        
        // 4. (Optional) Notify ML service if needed
        if (usernameChanged) {
            mlPublisher.publishUserReadyCsv(updatedUser);
        }
        
        return updatedUser;
    }
}
```

---

## 🔍 Current Status

### ✅ What's Working:
- User registration saves to PostgreSQL (`POST /auth/register`)
- User login reads from PostgreSQL (`POST /auth/login`)
- User model exists with all necessary fields
- `GET /api/users/{userId}` endpoint - ✅ **IMPLEMENTED**
- `PUT /api/users/{userId}` endpoint - ✅ **IMPLEMENTED**
- Frontend calls backend API for profile updates
- Admin email change prevention (security feature)

### ⚠️ What's Missing:
- `PUT /api/users/{userId}/password` endpoint - ❌ Not yet implemented
- Password change functionality (frontend has UI but backend endpoint missing)

---

## 📝 Summary

**Answer to your questions:**

1. **Should user profile have API?** 
   - ✅ **YES** - It should have API endpoints

2. **Should it update PostgreSQL?**
   - ✅ **YES** - PostgreSQL is the source of truth

3. **Should it update CSV files?**
   - ❌ **NO** - CSV files are read-only source data for ML

4. **Update order?**
   - ✅ **PostgreSQL FIRST** (required)
   - ✅ **Then optionally notify ML service** (if needed)
   - ❌ **Never update CSV files**

---

**The correct flow is:**
```
User Profile Update → Backend API → PostgreSQL Database → (Optional) RabbitMQ → ML Service
```

**NOT:**
```
User Profile Update → CSV Files → PostgreSQL ❌
```
