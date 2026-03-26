import json
import random
import string
import sys
import time
from urllib import request, error


BASE_URL = "http://localhost:8080"


def http_request(method: str, path: str, data: dict | None = None, timeout: float = 10.0):
    url = f"{BASE_URL}{path}"
    body = None
    headers = {}
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = request.Request(url, data=body, headers=headers, method=method)
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


def _random_suffix() -> str:
    return "".join(random.choices(string.ascii_lowercase + string.digits, k=8))


def random_email() -> str:
    suffix = _random_suffix()
    return f"test_{suffix}@example.com"


def random_username() -> str:
    suffix = _random_suffix()
    return f"TestUser_{suffix}"


def register_and_login():
    print("== Step 1: register new user ==")
    email = random_email()
    username = random_username()
    payload = {"name": username, "email": email, "password": "password123"}
    status, data = http_request("POST", "/auth/register", payload)
    print("  register status:", status, "response:", data)
    if status != 200 or not data.get("success"):
        raise RuntimeError("register failed")

    print("== Step 2: login with new user ==")
    status, data = http_request("POST", "/auth/login", {"email": email, "password": "password123"})
    print("  login status:", status, "response keys:", list(data.keys()))
    if status != 200 or not data.get("success"):
        raise RuntimeError("login failed")
    user = data.get("user") or {}
    user_id = int(user.get("id"))
    print("  logged-in user_id:", user_id)
    return user_id


def pick_product_ids(limit: int = 3):
    print("== Step 3: fetch some products to use in cart ==")
    # 1) Try lightweight products list endpoint (faster, no ML dependency)
    status, data = http_request("GET", "/api/products/list?page=0&size=20", timeout=15.0)
    if status == 200 and isinstance(data, dict) and data.get("content"):
        products = data["content"]
        print("  fetched products from /api/products/list, count:", len(products))
    else:
        # 2) Fallback: try ML recommendations/products endpoint (may be heavier)
        print("  fallback: /api/products/list failed, trying /api/recommendations/products ...")
        status, data = http_request("GET", "/api/recommendations/products", timeout=20.0)
        if status != 200:
            raise RuntimeError(f"failed to fetch products, status={status}, resp={data}")
        products = data if isinstance(data, list) else data.get("products") or data.get("data") or []
        if not products:
            raise RuntimeError("no products returned from /api/recommendations/products")

    ids = [int(p.get("id")) for p in products if p.get("id") is not None][:limit]
    print("  picked product IDs:", ids)
    return ids


def test_cart_flow(user_id: int, product_ids: list[int]):
    print("== Step 4: ensure cart starts empty ==")
    status, data = http_request("GET", f"/api/cart/{user_id}")
    print("  get cart:", status, data)
    if status == 200 and isinstance(data, dict):
        print("  initial cart count:", data.get("count"))

    print("== Step 5: add items to cart ==")
    added_items = []
    for i, pid in enumerate(product_ids):
        qty = i + 1
        status, data = http_request(
            "POST",
            f"/api/cart/{user_id}/items",
            {"productId": pid, "quantity": qty},
        )
        print(f"  add product {pid} x{qty} ->", status, data.get("message"))
        if status != 200 or not data.get("success"):
            raise RuntimeError(f"addToCart failed for product {pid}")
        added_items.append(data["data"])

    print("== Step 6: verify cart contents after adds ==")
    status, data = http_request("GET", f"/api/cart/{user_id}")
    print("  cart after adds:", status, "count:", data.get("count"), "items:", data.get("data"))
    items = data.get("data") or []
    if len(items) < len(product_ids):
        raise RuntimeError("cart item count mismatch after adds")

    print("== Step 7: update quantity of first cart item ==")
    first_item = items[0]
    item_id = int(first_item["id"])
    new_qty = first_item.get("quantity", 1) + 2
    status, data = http_request(
        "PUT",
        f"/api/cart/{user_id}/items/{item_id}",
        {"quantity": new_qty},
    )
    print("  update item:", status, data.get("message"))
    if status != 200 or not data.get("success"):
        raise RuntimeError("updateCartItem failed")

    print("== Step 8: remove second cart item (if exists) ==")
    if len(items) > 1:
        second_item_id = int(items[1]["id"])
        status, data = http_request("DELETE", f"/api/cart/{user_id}/items/{second_item_id}")
        print("  remove item:", status, data.get("message"))
        if status != 200 or not data.get("success"):
            raise RuntimeError("removeFromCart failed")

    print("== Step 9: simulate checkout with wallet (expect likely failure if no balance) ==")
    # Use a small amount; we mainly care that endpoint is reachable and validates input.
    status, data = http_request(
        "POST",
        "/api/payments/pay-with-wallet",
        {"userId": user_id, "amount": 10.0},
    )
    print("  pay-with-wallet:", status, data)

    print("== Step 10: clear cart at end ==")
    status, data = http_request("DELETE", f"/api/cart/{user_id}")
    print("  clear cart:", status, data.get("message"))
    if status != 200 or not data.get("success"):
        raise RuntimeError("clearCart failed")


def main():
    print("=== CART + CHECKOUT SANITY TEST ===")
    try:
        user_id = register_and_login()
        product_ids = pick_product_ids()
        test_cart_flow(user_id, product_ids)
        print("\n=== ALL CART SANITY CHECKS PASSED ===")
        sys.exit(0)
    except Exception as e:
        print("\n[ERROR]", e)
        sys.exit(1)


if __name__ == "__main__":
    main()

