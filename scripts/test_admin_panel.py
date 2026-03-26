import json
import sys
from urllib import request, error


BASE_URL = "http://localhost:8080"
ADMIN_EMAIL = "admin@ecommerce.com"
ADMIN_PASSWORD = "admin123"


def http_request(method: str, path: str, data: dict | None = None, headers: dict | None = None, timeout: float = 10.0):
    url = f"{BASE_URL}{path}"
    body = None
    all_headers = headers.copy() if headers else {}

    if data is not None:
        body = json.dumps(data).encode("utf-8")
        all_headers["Content-Type"] = "application/json"

    req = request.Request(url, data=body, headers=all_headers, method=method)
    try:
        with request.urlopen(req, timeout=timeout) as resp:
            text = resp.read().decode("utf-8") or "{}"
            try:
                return resp.status, json.loads(text)
            except json.JSONDecodeError:
                return resp.status, {"raw": text}
    except error.HTTPError as e:
        try:
            text = e.read().decode("utf-8") or "{}"
            payload = json.loads(text)
        except Exception:
            payload = {"raw": text}
        return e.code, payload
    except Exception as e:
        return 0, {"error": str(e)}


def login_admin():
    print("== Admin Step 1: login as admin ==")
    status, data = http_request(
        "POST",
        "/auth/login",
        {"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD},
        timeout=15.0,
    )
    print("  login status:", status, "response keys:", list(data.keys()))
    if status != 200 or not data.get("success"):
        raise RuntimeError(f"admin login failed: {data}")
    user = data.get("user") or {}
    print("  admin userId:", user.get("id"))


def test_admin_info():
    print("== Admin Step 2: /admin/info ==")
    status, data = http_request("GET", "/admin/info", timeout=10.0)
    print("  status:", status, "keys:", list(data.keys()))
    if status != 200:
        raise RuntimeError(f"/admin/info failed: {status}, {data}")
    if not data.get("adminUser"):
        raise RuntimeError("adminUser missing in /admin/info response")


def test_admin_users():
    print("== Admin Step 3: /admin/users ==")
    status, data = http_request("GET", "/admin/users", timeout=10.0)
    print("  status:", status, "success:", data.get("success"), "count:", data.get("count"))
    if status != 200 or not data.get("success"):
        raise RuntimeError(f"/admin/users failed: {status}, {data}")


def test_product_crud_as_admin():
    print("== Admin Step 4: product CRUD as admin ==")
    headers = {"X-User-Email": ADMIN_EMAIL}

    # Create
    payload = {
        "productName": "Admin Test Product",
        "description": "Created from admin panel sanity script",
        "category": "TestCategory",
        "price": 12.34,
        "quantity": 5,
        "imageUrl": "https://example.com/admin-test-product.jpg",
        "tags": ["admin", "test"],
    }
    status, data = http_request("POST", "/api/products", payload, headers=headers, timeout=15.0)
    print("  create product:", status, data)
    if status != 200 or not data.get("success"):
        raise RuntimeError(f"create product as admin failed: {status}, {data}")
    created = data.get("product") or data.get("data") or {}
    product_id = created.get("id")
    if not product_id:
        raise RuntimeError("no product id returned from create")
    product_id = int(product_id)

    # Update
    update_payload = {
        "productName": "Admin Test Product - Updated",
        "description": "Updated via admin panel sanity script",
        "category": "TestCategory",
        "price": 23.45,
        "quantity": 7,
        "imageUrl": "https://example.com/admin-test-product-updated.jpg",
        "tags": ["admin", "test", "updated"],
    }
    status, data = http_request(
        "PUT",
        f"/api/products/{product_id}",
        update_payload,
        headers=headers,
        timeout=15.0,
    )
    print("  update product:", status, data.get("message"))
    if status != 200 or not data.get("success"):
        raise RuntimeError(f"update product as admin failed: {status}, {data}")

    # Delete
    status, data = http_request(
        "DELETE",
        f"/api/products/{product_id}",
        headers=headers,
        timeout=15.0,
    )
    print("  delete product:", status, data.get("message") if isinstance(data, dict) else data)
    if status != 200 or not data.get("success"):
        raise RuntimeError(f"delete product as admin failed: {status}, {data}")


def test_product_create_as_non_admin():
    print("== Admin Step 5: verify non-admin is blocked ==")
    headers = {"X-User-Email": "user@example.com"}  # not admin
    payload = {
        "productName": "NonAdmin Test Product",
        "description": "Should be forbidden",
        "category": "TestCategory",
        "price": 9.99,
        "quantity": 1,
        "imageUrl": "https://example.com/non-admin-test-product.jpg",
    }
    status, data = http_request("POST", "/api/products", payload, headers=headers, timeout=10.0)
    print("  non-admin create status:", status, "response:", data)
    if status != 403 and (not isinstance(data, dict) or data.get("success") is not False):
        raise RuntimeError("expected non-admin create to be forbidden (403)")


def main():
    print("=== ADMIN PANEL SANITY TEST ===")
    try:
        login_admin()
        test_admin_info()
        test_admin_users()
        test_product_crud_as_admin()
        test_product_create_as_non_admin()
        print("\n=== ALL ADMIN SANITY CHECKS PASSED ===")
        sys.exit(0)
    except Exception as e:
        print("\n[ERROR]", e)
        sys.exit(1)


if __name__ == "__main__":
    main()

