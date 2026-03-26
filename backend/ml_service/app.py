from flask import Flask, request, jsonify
from flask_cors import CORS
import json
import random
import sys
import time
from datetime import datetime
from pathlib import Path

app = Flask(__name__)
CORS(app)

# Simple in-memory storage
products_data = []
users_data = []
interactions_data = []

# Phase 2 recommendation system (live ML: hybrid recommendations + dynamic learning)
rec_system = None
rec_system_available = False

# Phase 1/3 product categorization (long-running, model loaded once in memory)
product_categorizer = None
product_categorizer_available = False

# Pre-computed popular ranking: avoid recomputing on every "next" click (refresh every 10 min)
_popular_ranking_cache = None  # list of product IDs in rank order
_popular_ranking_cache_time = 0
POPULAR_CACHE_TTL_SEC = 600  # 10 minutes
POPULAR_CACHE_SIZE = 500     # pre-compute top 500

def _project_root():
    """Resolve project root that contains src/phase2 (for RecommendationSystem)."""
    p = Path(__file__).resolve().parent.parent.parent
    if (p / "src" / "phase2").exists():
        return str(p)
    nested = p / "ML-eCommers-GitHub-9.2.26"
    if nested.exists() and (nested / "src" / "phase2").exists():
        return str(nested)
    return str(p)

def _init_phase2():
    """Import Phase 2 RecommendationSystem if available."""
    global rec_system_available
    try:
        root = _project_root()
        if root not in sys.path:
            sys.path.insert(0, root)
        src = str(Path(root) / "src")
        if src not in sys.path:
            sys.path.insert(0, src)
        from phase2.recommendation_system_ml import RecommendationSystem  # noqa: E402
        rec_system_available = True
        return RecommendationSystem
    except Exception as e:
        print(f"[ML] Phase 2 not available: {e}")
        rec_system_available = False
        return None

_RecommendationSystem = _init_phase2()


def _init_product_categorizer():
    """Import Phase 3 SingleItemCategorization for product categorization. Loads model once, keeps in memory."""
    global product_categorizer_available
    try:
        root = _project_root()
        if root not in sys.path:
            sys.path.insert(0, root)
        src = str(Path(root) / "src")
        if src not in sys.path:
            sys.path.insert(0, src)
        if str(Path(root) / "src" / "phase3") not in sys.path:
            sys.path.insert(0, str(Path(root) / "src" / "phase3"))
        from phase3.single_item_categorization import SingleItemCategorization  # noqa: E402
        product_categorizer_available = True
        return SingleItemCategorization
    except Exception as e:
        print(f"[ML] Product categorization not available: {e}")
        product_categorizer_available = False
        return None


_SingleItemCategorization = _init_product_categorizer()


def _get_product_categorizer():
    """Lazy init: load model on first request, keep in memory for fast subsequent calls."""
    global product_categorizer
    if product_categorizer is None and _SingleItemCategorization and product_categorizer_available:
        try:
            product_categorizer = _SingleItemCategorization(_project_root())
            product_categorizer._initialize_product_categorizer()
            print("[ML] Product categorization model loaded (kept in memory)")
        except Exception as e:
            print(f"[ML] Product categorization init failed: {e}")
    return product_categorizer


def _refresh_popular_cache():
    """Pre-compute full popular ranking so 'next' can serve from cache without recomputing."""
    global _popular_ranking_cache, _popular_ranking_cache_time
    if not rec_system or not products_data:
        return
    try:
        n = min(POPULAR_CACHE_SIZE, len(products_data))
        _popular_ranking_cache = rec_system._get_popular_products(n=n)
        _popular_ranking_cache_time = time.time()
        print(f"[ML] Popular ranking cache refreshed ({len(_popular_ranking_cache)} products)")
    except Exception as e:
        print(f"[ML] Popular cache refresh failed: {e}")

@app.route('/health', methods=['GET'])
def health():
    """
    Basic health check endpoint used by infrastructure tools.
    """
    return jsonify({"status": "healthy", "message": "ML Service is running"})

@app.route('/status', methods=['GET'])
def status():
    """
    Status endpoint for the Java backend / developers.
    This is what you're hitting with curl/HTTP.
    """
    return jsonify({
        "status": "ready",
        "message": "ML recommendation service is running",
        "products_loaded": len(products_data),
        "users_loaded": len(users_data),
        "interactions_loaded": len(interactions_data),
        "phase2_ready": rec_system is not None,
        "product_categorization_ready": product_categorizer_available,
        "endpoints": {
            "health": "/health",
            "load_data": "/data/load",
            "categorize_product": "/categorize-product",
            "guest": "/recommendations/guest",
            "personalized": "/recommendations/personalized/<user_id>",
            "similar": "/recommendations/similar/<product_id>",
            "trending": "/recommendations/trending",
            "category": "/recommendations/category/<category>",
            "record_interaction": "/interactions/record",
        },
    })

@app.route('/data/load', methods=['POST'])
def load_data():
    try:
        data = request.get_json()
        global products_data, users_data, interactions_data, rec_system
        
        products_data = data.get('products', [])
        users_data = data.get('users', [])
        interactions_data = data.get('interactions', [])
        
        # Build Phase 2 RecommendationSystem from payload for live ML recommendations + dynamic learning
        rec_system = None
        if rec_system_available and _RecommendationSystem and products_data:
            try:
                rs = _RecommendationSystem(_project_root())
                rs.load_data_from_payload(products_data, users_data or [], interactions_data or [])
                rs.prepare_tfidf_for_products()
                rs.create_user_interaction_matrix()
                rs.calculate_user_similarity()
                rec_system = rs
                print("[ML] Phase 2 RecommendationSystem ready (hybrid + dynamic updates)")
                _refresh_popular_cache()
            except Exception as e:
                print(f"[ML] Phase 2 init failed, using simple logic: {e}")
        
        return jsonify({
            "message": "Data loaded successfully",
            "products_count": len(products_data),
            "users_count": len(users_data),
            "interactions_count": len(interactions_data),
            "phase2_ready": rec_system is not None
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/categorize-product', methods=['POST'])
def categorize_product():
    """
    Categorize a single product by name, description, price.
    Model is loaded once and kept in memory → fast (milliseconds after first request).
    Used by backend when creating/updating products with Unclassified category.
    """
    try:
        data = request.get_json() or {}
        product_name = str(data.get('product_name', data.get('name', '') or ''))
        description = str(data.get('description', '') or '')
        try:
            price = float(data.get('price', 0) or 0)
        except (TypeError, ValueError):
            price = 0.0

        cat = _get_product_categorizer()
        if not cat or not product_categorizer_available:
            return jsonify({"error": "Product categorization not available", "main_category": "", "sub_category": ""}), 503

        result = cat.categorize_new_product(product_name, description, price, use_model=True)
        main_cat = result.get('predicted_main_category', '') or ''
        sub_cat = result.get('predicted_sub_category', '') or ''
        return jsonify({"main_category": main_cat, "sub_category": sub_cat, "method": result.get('method', 'model')})
    except Exception as e:
        print(f"[ML] categorize_product error: {e}")
        return jsonify({"error": str(e), "main_category": "", "sub_category": ""}), 500


@app.route('/recommendations/guest', methods=['GET'])
def guest_recommendations():
    try:
        limit = int(request.args.get('limit', 10))
        
        if not products_data:
            return jsonify([])

        # Use Phase 2 popular products when available (ML-based)
        if rec_system:
            try:
                pid_list = rec_system._get_popular_products(n=limit)
                out = []
                seen_names = set()
                for pid in pid_list:
                    p = _product_by_id(pid)
                    if p:
                        name = (p.get('productName') or p.get('name') or '').strip().lower()
                        if name and name in seen_names:
                            continue
                        if name:
                            seen_names.add(name)
                        out.append(p)
                        if len(out) >= limit:
                            break
                if out:
                    return jsonify(out)
            except Exception as e:
                print(f"[ML] Phase 2 guest fallback: {e}")

        def _norm_name(p):
            return (p.get('productName') or p.get('product_name') or p.get('name') or '').strip().lower()
        seen_ids = set()
        seen_names = set()
        unique = []
        for p in sorted(products_data, key=lambda x: x.get('views', 0), reverse=True):
            pid = p.get('id')
            name = _norm_name(p)
            if pid is None or pid in seen_ids or (name and name in seen_names):
                continue
            seen_ids.add(pid)
            if name:
                seen_names.add(name)
            unique.append(p)
            if len(unique) >= limit:
                break
        return jsonify(unique)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

def _product_by_id(pid):
    """Get product dict by id from products_data. Compares as int for consistency."""
    if pid is None:
        return None
    try:
        pid_int = int(pid)
    except (TypeError, ValueError):
        pid_int = pid
    for p in products_data:
        x = p.get('id')
        try:
            if x is not None and int(x) == pid_int:
                return p
        except (TypeError, ValueError):
            if x == pid:
                return p
    return None

@app.route('/recommendations/personalized/<int:user_id>', methods=['GET'])
def personalized_recommendations(user_id):
    try:
        limit = int(request.args.get('limit', 10))
        
        if not products_data:
            return jsonify([])
        
        # Use Phase 2 hybrid recommendations when available (ML-based)
        if rec_system:
            try:
                pid_list = rec_system.hybrid_recommendations(user_id, n_recommendations=limit)
                out = []
                seen_names = set()
                for pid in pid_list:
                    p = _product_by_id(pid)
                    if p:
                        name = (p.get('productName') or p.get('name') or '').strip().lower()
                        if name and name in seen_names:
                            continue
                        if name:
                            seen_names.add(name)
                        out.append(p)
                        if len(out) >= limit:
                            break
                if out:
                    return jsonify(out)
            except Exception as e:
                print(f"[ML] Phase 2 personalized fallback: {e}")
        
        # User interactions with weights: purchase=3, cart=2, view=1 (normalize id types for comparison)
        uid = int(user_id) if user_id is not None else None
        user_interactions = [i for i in interactions_data if i.get('user_id') is not None and int(i.get('user_id')) == uid]
        
        if not user_interactions:
            # No history: same as guest/trending – popular by views, unique by id and by name
            def _norm_name(p):
                return (p.get('productName') or p.get('product_name') or p.get('name') or '').strip().lower()
            seen_ids = set()
            seen_names = set()
            unique = []
            for p in sorted(products_data, key=lambda x: x.get('views', 0), reverse=True):
                pid = p.get('id')
                name = _norm_name(p)
                if pid is None or pid in seen_ids or (name and name in seen_names):
                    continue
                seen_ids.add(pid)
                if name:
                    seen_names.add(name)
                unique.append(p)
                if len(unique) >= limit:
                    break
            return jsonify(unique)
        
        # Normalize category for case-insensitive match (e.g. Electronics vs electronics)
        def _norm_cat(c):
            return (c or "").strip().lower() or None

        def _norm_name(p):
            return (p.get('productName') or p.get('product_name') or p.get('name') or '').strip().lower()

        # Set of product ids the user already has (bought, in cart, viewed) – normalize to int
        interacted_ids = set()
        # Product names the user has purchased (so we never recommend same name, even if different id in DB)
        purchased_names = set()
        # Sum weights per category; track which categories have at least one purchase (weight 3)
        category_weights = {}
        purchased_categories = set()  # categories where user made at least one purchase
        for i in user_interactions:
            pid = i.get('product_id')
            if pid is not None:
                try:
                    interacted_ids.add(int(pid))
                except (TypeError, ValueError):
                    interacted_ids.add(pid)
            w = i.get('weight', 1)
            if w is None:
                w = 1
            prod = _product_by_id(pid) if pid is not None else None
            if prod:
                name = _norm_name(prod)
                if name and w >= 3:
                    purchased_names.add(name)
                if prod.get('category'):
                    cat = _norm_cat(prod.get('category'))
                    if cat:
                        category_weights[cat] = category_weights.get(cat, 0) + w
                        if w >= 3:
                            purchased_categories.add(cat)

        # Debug: so you can see in ML console that history was used and that we exclude purchased/viewed
        print(f"[ML] user_id={user_id} interactions={len(user_interactions)} excluded_ids={len(interacted_ids)} purchased_names={len(purchased_names)} categories={list(category_weights.keys())}")

        # Score: weight * 100000 + views. Boost categories where user purchased so they appear in top
        scored = []
        seen_ids = set()
        def _pid(p):
            x = p.get('id')
            try:
                return int(x) if x is not None else None
            except (TypeError, ValueError):
                return x
        for p in products_data:
            pid = _pid(p)
            if pid is None or pid in interacted_ids or pid in seen_ids:
                continue
            if _norm_name(p) in purchased_names:
                continue
            seen_ids.add(pid)
            cat = _norm_cat(p.get('category'))
            weight = category_weights.get(cat, 0) if cat else 0
            views = p.get('views') or 0
            score = weight * 100000 + views
            # So purchased categories get represented: add 1M if this category was ever purchased
            if cat and cat in purchased_categories:
                score += 1000000
            scored.append((score, p))

        # Sort by score descending
        scored.sort(key=lambda x: -x[0])

        # Build list: (1) diversity - one product per purchased category, (2) fill by score; dedupe by product name
        recommended_products = []
        rec_ids = set()
        rec_names = set()

        def _add_if_new(p):
            pid = p.get('id')
            name = _norm_name(p)
            if pid in rec_ids or (name and name in rec_names):
                return False
            rec_ids.add(pid)
            if name:
                rec_names.add(name)
            recommended_products.append(p)
            return True

        # Phase 1: ensure at least one product from each purchased category,
        # ordered by how important that category is for this user (not alphabetically).
        ordered_categories = sorted(
            purchased_categories, key=lambda c: category_weights.get(c, 0), reverse=True
        )
        for cat in ordered_categories:
            for score, p in scored:
                if _norm_cat(p.get('category')) == cat and _add_if_new(p):
                    break

        # Phase 2: fill by score, skip if same product name already in list (no visual duplicates)
        for score, p in scored:
            if len(recommended_products) >= limit:
                break
            _add_if_new(p)

        # If still under limit, add by popularity without name dedupe
        if len(recommended_products) < limit:
            for p in sorted(products_data, key=lambda x: x.get('views', 0), reverse=True):
                if len(recommended_products) >= limit:
                    break
                pid = p.get('id')
                if pid is not None and pid not in interacted_ids and pid not in rec_ids:
                    rec_ids.add(pid)
                    recommended_products.append(p)

        return jsonify(recommended_products)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/recommendations/similar/<int:product_id>', methods=['GET'])
def similar_products(product_id):
    try:
        limit = int(request.args.get('limit', 5))
        
        if not products_data:
            return jsonify([])
        
        # Find the target product
        target_product = None
        for product in products_data:
            if product.get('id') == product_id:
                target_product = product
                break
        
        if not target_product:
            return jsonify([])
        
        target_cat = target_product.get('category')
        target_sub = target_product.get('sub_category')
        # Prefer same subcategory when available (e.g. Mobile Accessories), then same main category
        same_sub = []
        same_cat_only = []
        for p in products_data:
            if p.get('id') == product_id:
                continue
            if p.get('category') != target_cat:
                continue
            if target_sub and (p.get('sub_category') or '').strip() == (target_sub or '').strip():
                same_sub.append(p)
            else:
                same_cat_only.append(p)
        # Return same subcategory first, then fill with same main category up to limit
        combined = same_sub + same_cat_only
        return jsonify(combined[:limit])
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/recommendations/trending', methods=['GET'])
def trending_products():
    try:
        limit = int(request.args.get('limit', 10))
        offset = int(request.args.get('offset', 0))
        
        if not products_data:
            return jsonify([])

        # Use pre-computed ranking when available: serve slice [offset:offset+limit] (no recompute per request)
        if rec_system:
            try:
                global _popular_ranking_cache, _popular_ranking_cache_time
                now = time.time()
                if _popular_ranking_cache is None or (now - _popular_ranking_cache_time) > POPULAR_CACHE_TTL_SEC:
                    _refresh_popular_cache()
                if _popular_ranking_cache is not None:
                    pid_list = _popular_ranking_cache[offset:offset + limit]
                    out = []
                    seen_names = set()
                    for pid in pid_list:
                        p = _product_by_id(pid)
                        if p:
                            name = (p.get('productName') or p.get('name') or '').strip().lower()
                            if name and name in seen_names:
                                continue
                            if name:
                                seen_names.add(name)
                            out.append(p)
                    if out:
                        return jsonify(out)
            except Exception as e:
                print(f"[ML] Phase 2 trending fallback: {e}")

        # Fallback when Phase 2 not available: rank by rating and views (same idea as Popular)
        # so low-rated products don't appear first just because they have high views.
        def _norm_name(p):
            return (p.get('productName') or p.get('product_name') or p.get('name') or '').strip().lower()
        views_list = [p.get('views') for p in products_data if p.get('views') is not None]
        max_views = max(views_list, default=1) or 1
        max_rating = 5.0
        def _popular_score(p):
            v = p.get('views') or 0
            r = p.get('rating')
            if r is None:
                r = 0
            try:
                r = float(r)
            except (TypeError, ValueError):
                r = 0
            view_norm = min(float(v) / max_views, 1.0)
            rating_norm = min(max(r / max_rating, 0.0), 1.0)
            return (rating_norm * 2.0) + (view_norm * 1.5)
        trending = sorted(products_data, key=_popular_score, reverse=True)
        seen_names = set()
        unique = []
        for p in trending:
            name = _norm_name(p)
            if name and name in seen_names:
                continue
            if name:
                seen_names.add(name)
            unique.append(p)
            if len(unique) >= limit + offset:
                break
        slice_list = unique[offset:offset + limit]
        return jsonify(slice_list)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/recommendations/category/<category>', methods=['GET'])
def category_recommendations(category):
    try:
        limit = int(request.args.get('limit', 10))
        
        if not products_data:
            return jsonify([])
        
        # Filter products by category
        category_products = [p for p in products_data if p.get('category') == category]
        return jsonify(category_products[:limit])
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/interactions/record', methods=['POST'])
def record_interaction():
    try:
        interaction = request.get_json()
        interaction['timestamp'] = datetime.now().isoformat()
        interactions_data.append(interaction)
        
        # Phase 2 dynamic learning: update interaction matrix in real time
        if rec_system:
            try:
                uid = interaction.get('user_id')
                pid = interaction.get('product_id')
                w = interaction.get('weight', 1)
                if uid is not None and pid is not None:
                    itype = 'purchase' if w == 3 else 'click'
                    rec_system.update_interaction_dynamic(int(uid), int(pid), interaction_type=itype, value=1)
                    # Invalidate popular cache so next request gets fresh ranking (or wait for 10-min refresh)
                    global _popular_ranking_cache_time
                    _popular_ranking_cache_time = 0
            except Exception as e:
                print(f"[ML] Phase 2 update_interaction_dynamic: {e}")
        
        return jsonify({"message": "Interaction recorded successfully"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("Starting ML Service on http://localhost:5000")
    app.run(host='0.0.0.0', port=5000, debug=True) 