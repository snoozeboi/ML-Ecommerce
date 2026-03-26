import { Product, Recommendation, Category, ApiResponse, FilterOptions, SuggestResponseDto } from '@/types';

// Mock data for development - replace with actual API calls
const mockProducts: Product[] = [
  {
    id: '1',
    name: 'Premium Wireless Headphones',
    description: 'High-quality wireless headphones with noise cancellation technology. Perfect for music lovers and professionals.',
    price: 199.99,
    originalPrice: 249.99,
    image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80',
    images: [
      'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80',
      'https://images.unsplash.com/photo-1484704849700-f032a568e944?w=400&q=80',
      'https://images.unsplash.com/photo-1524678606370-a47ad25cb82a?w=400&q=80',
    ],
    category: 'Electronics',
    subcategory: 'Audio',
    rating: 4.8,
    reviewCount: 234,
    inStock: true,
    tags: ['wireless', 'bluetooth', 'noise-cancelling'],
  },
  {
    id: '2',
    name: 'Smart Watch Pro',
    description: 'Advanced smartwatch with health monitoring, GPS tracking, and seamless smartphone integration.',
    price: 349.99,
    image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&q=80',
    category: 'Electronics',
    subcategory: 'Wearables',
    rating: 4.6,
    reviewCount: 189,
    inStock: true,
    tags: ['smart', 'fitness', 'health'],
  },
  {
    id: '3',
    name: 'Minimalist Leather Bag',
    description: 'Handcrafted genuine leather bag with modern minimalist design. Perfect for daily use.',
    price: 129.99,
    originalPrice: 159.99,
    image: 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=400&q=80',
    category: 'Fashion',
    subcategory: 'Bags',
    rating: 4.9,
    reviewCount: 312,
    inStock: true,
    tags: ['leather', 'minimalist', 'handmade'],
  },
  {
    id: '4',
    name: 'Running Sneakers Ultra',
    description: 'Lightweight running shoes with advanced cushioning technology for maximum comfort.',
    price: 89.99,
    image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&q=80',
    category: 'Fashion',
    subcategory: 'Shoes',
    rating: 4.7,
    reviewCount: 456,
    inStock: true,
    tags: ['running', 'sports', 'comfortable'],
  },
  {
    id: '5',
    name: 'Ceramic Coffee Mug Set',
    description: 'Set of 4 elegant ceramic mugs with modern geometric patterns. Microwave safe.',
    price: 34.99,
    image: 'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=400&q=80',
    category: 'Home',
    subcategory: 'Kitchen',
    rating: 4.5,
    reviewCount: 178,
    inStock: true,
    tags: ['ceramic', 'kitchen', 'set'],
  },
  {
    id: '6',
    name: 'Portable Bluetooth Speaker',
    description: 'Waterproof portable speaker with 360° sound and 24-hour battery life.',
    price: 79.99,
    originalPrice: 99.99,
    image: 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=400&q=80',
    category: 'Electronics',
    subcategory: 'Audio',
    rating: 4.4,
    reviewCount: 267,
    inStock: true,
    tags: ['bluetooth', 'portable', 'waterproof'],
  },
  {
    id: '7',
    name: 'Organic Skincare Set',
    description: 'Complete skincare routine with organic ingredients. Includes cleanser, toner, and moisturizer.',
    price: 64.99,
    image: 'https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400&q=80',
    category: 'Beauty',
    subcategory: 'Skincare',
    rating: 4.8,
    reviewCount: 521,
    inStock: true,
    tags: ['organic', 'skincare', 'natural'],
  },
  {
    id: '8',
    name: 'Mechanical Keyboard RGB',
    description: 'Premium mechanical keyboard with customizable RGB lighting and tactile switches.',
    price: 149.99,
    image: 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?w=400&q=80',
    category: 'Electronics',
    subcategory: 'Computer',
    rating: 4.7,
    reviewCount: 342,
    inStock: true,
    tags: ['mechanical', 'gaming', 'rgb'],
  },
  {
    id: '9',
    name: 'Yoga Mat Premium',
    description: 'Extra thick eco-friendly yoga mat with alignment lines. Non-slip surface.',
    price: 49.99,
    image: 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=400&q=80',
    category: 'Sports',
    subcategory: 'Yoga',
    rating: 4.6,
    reviewCount: 198,
    inStock: true,
    tags: ['yoga', 'fitness', 'eco-friendly'],
  },
  {
    id: '10',
    name: 'Stainless Steel Water Bottle',
    description: 'Insulated water bottle keeps drinks cold for 24 hours or hot for 12 hours.',
    price: 29.99,
    image: 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=400&q=80',
    category: 'Sports',
    subcategory: 'Accessories',
    rating: 4.5,
    reviewCount: 387,
    inStock: true,
    tags: ['insulated', 'stainless', 'eco'],
  },
  {
    id: '11',
    name: 'Designer Sunglasses',
    description: 'UV400 protection sunglasses with polarized lenses and titanium frame.',
    price: 189.99,
    originalPrice: 229.99,
    image: 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=400&q=80',
    category: 'Fashion',
    subcategory: 'Accessories',
    rating: 4.4,
    reviewCount: 156,
    inStock: true,
    tags: ['sunglasses', 'designer', 'polarized'],
  },
  {
    id: '12',
    name: 'Aromatherapy Diffuser',
    description: 'Ultrasonic essential oil diffuser with color-changing LED lights and timer.',
    price: 39.99,
    image: 'https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=400&q=80',
    category: 'Home',
    subcategory: 'Wellness',
    rating: 4.6,
    reviewCount: 234,
    inStock: true,
    tags: ['aromatherapy', 'diffuser', 'relaxation'],
  },
];

const mockCategories: Category[] = [
  { id: '1', name: 'Electronics', subcategories: ['Audio', 'Wearables', 'Computer', 'Mobile'], productCount: 4 },
  { id: '2', name: 'Fashion', subcategories: ['Bags', 'Shoes', 'Accessories', 'Clothing'], productCount: 3 },
  { id: '3', name: 'Home', subcategories: ['Kitchen', 'Wellness', 'Decor', 'Furniture'], productCount: 2 },
  { id: '4', name: 'Beauty', subcategories: ['Skincare', 'Makeup', 'Hair', 'Fragrance'], productCount: 1 },
  { id: '5', name: 'Sports', subcategories: ['Yoga', 'Accessories', 'Fitness', 'Outdoor'], productCount: 2 },
];

// API Base URL - In dev use '' so Vite proxy forwards /api and /auth to backend (avoids CORS / Failed to fetch)
// @ts-ignore - Vite environment variables
const API_BASE_URL = import.meta.env.DEV ? '' : (import.meta.env.VITE_API_URL || 'http://localhost:8080');

/**
 * Maps backend Product to frontend Product format
 */
const FALLBACK_PRODUCT_IMAGE = 'https://images.pexels.com/photos/4475708/pexels-photo-4475708.jpeg'; // generic bag/backpack

function mapBackendProductToFrontend(backendProduct: any): Product {
  const imageUrl = backendProduct.imageUrl || backendProduct.image_url || backendProduct.image || FALLBACK_PRODUCT_IMAGE;
  return {
    id: String(backendProduct.id),
    name: backendProduct.productName || backendProduct.name || '',
    description: backendProduct.description || '',
    price: backendProduct.price || 0,
    originalPrice: backendProduct.discount ? 
      (backendProduct.price + (backendProduct.discount.value || 0)) : undefined,
    image: imageUrl,
    images: imageUrl ? [imageUrl] : undefined,
    category: backendProduct.category || '',
    subcategory: backendProduct.subCategory || backendProduct.sub_category || undefined,
    rating: backendProduct.rating || 0,
    reviewCount: 0, // Backend doesn't have reviewCount
    inStock: (backendProduct.quantity || 0) > 0,
    tags: backendProduct.tags || [],
  };
}

/**
 * Fetch personalized recommendations for a user
 * Endpoint: GET /api/recommendations/personalized/{userId} or /guest
 * Use limit and offset for pagination: initial load 5, then 10 more each time.
 */
export async function getRecommendations(
  userId?: string,
  limit: number = 5,
  offset: number = 0
): Promise<ApiResponse<Recommendation>> {
  try {
    if (!userId) {
      const response = await fetch(
        `${API_BASE_URL}/api/recommendations/guest?limit=${limit}&offset=${offset}`
      );
      if (!response.ok) throw new Error('Failed to fetch recommendations');
      const products = await response.json();
      return {
        data: {
          products: products.map(mapBackendProductToFrontend),
          type: 'personalized',
          confidence: 0.87,
        },
        success: true,
      };
    }

    const response = await fetch(
      `${API_BASE_URL}/api/recommendations/personalized/${userId}?limit=${limit}&offset=${offset}`
    );
    if (!response.ok) throw new Error('Failed to fetch recommendations');
    const products = await response.json();
    return {
      data: {
        products: products.map(mapBackendProductToFrontend),
        type: 'personalized',
        confidence: 0.87,
      },
      success: true,
    };
  } catch (error) {
    console.error('Error fetching recommendations:', error);
    const shuffled = [...mockProducts].sort(() => 0.5 - Math.random());
    return {
      data: {
        products: shuffled.slice(0, 5),
        type: 'personalized',
        confidence: 0.87,
      },
      success: true,
    };
  }
}

/**
 * Fetch popular purchases (trending products). Use offset for "load next" (server uses pre-ranked list).
 * Endpoint: GET /api/recommendations/trending?limit=&offset=
 */
export async function getPopularPurchases(
  limit: number = 5,
  offset: number = 0
): Promise<ApiResponse<Recommendation>> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/recommendations/trending?limit=${limit}&offset=${offset}`
    );
    if (!response.ok) throw new Error('Failed to fetch trending products');
    const products = await response.json();
    return {
      data: {
        products: products.map(mapBackendProductToFrontend),
        type: 'popular',
      },
      success: true,
    };
  } catch (error) {
    console.error('Error fetching trending products:', error);
    // Fallback to mock data
    const sorted = [...mockProducts].sort((a, b) => b.reviewCount - a.reviewCount);
    return {
      data: {
        products: sorted.slice(0, 5),
        type: 'popular',
      },
      success: true,
    };
  }
}

/**
 * Fetch similar items for a product (same category or subcategory).
 * Endpoint: GET /api/recommendations/similar/{productId}?limit={limit}
 * @param limit Max number to return (default 5; use 30 for full similar list with load-more)
 */
export async function getSimilarItems(
  productId: string,
  limit: number = 5
): Promise<ApiResponse<Recommendation>> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/recommendations/similar/${productId}?limit=${Math.min(limit, 30)}`
    );
    if (!response.ok) throw new Error('Failed to fetch similar products');
    const products = await response.json();
    return {
      data: {
        products: products.map(mapBackendProductToFrontend),
        type: 'similar',
      },
      success: true,
    };
  } catch (error) {
    console.error('Error fetching similar products:', error);
    // Fallback to mock data
    const product = mockProducts.find(p => p.id === productId);
    const similar = mockProducts
      .filter(p => p.id !== productId && p.category === product?.category)
      .slice(0, limit);
    return {
      data: {
        products: similar,
        type: 'similar',
      },
      success: true,
    };
  }
}

/**
 * Fetch paginated products for admin panel
 * Endpoint: GET /api/products?page={page}&size={size}
 * Requires: Admin access (userEmail must be admin email)
 */
export async function getProductsPaginated(
  page: number = 0,
  size: number = 100,
  userEmail?: string,
  search?: string
): Promise<ApiResponse<{
  products: Product[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}>> {
  try {
    const headers: HeadersInit = {};
    if (userEmail) {
      headers['X-User-Email'] = userEmail;
    }
    
    let url = `${API_BASE_URL}/api/products/list?page=${page}&size=${size}`;
    if (search && search.trim()) {
      url += `&search=${encodeURIComponent(search.trim())}`;
    }
    console.log('Fetching paginated products from:', url, 'with headers:', headers);
    
    const response = await fetch(url, {
      method: 'GET',
      headers,
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error('Failed to fetch products:', response.status, errorText);
      throw new Error(`Failed to fetch products: ${response.status} ${response.statusText}`);
    }
    
    const data = await response.json();
    console.log('Received products data:', { 
      hasContent: !!data.content, 
      isArray: Array.isArray(data),
      contentLength: data.content?.length || data.length || 0,
      totalElements: data.totalElements || data.length || 0
    });
    
    // Handle both paginated response and list response
    if (data.content && Array.isArray(data.content)) {
      // Paginated response
      return {
        data: {
          products: data.content.map(mapBackendProductToFrontend),
          totalElements: data.totalElements || 0,
          totalPages: data.totalPages || 0,
          currentPage: data.currentPage || 0,
          pageSize: data.pageSize || size,
          hasNext: data.hasNext || false,
          hasPrevious: data.hasPrevious || false,
        },
        success: true,
      };
    } else if (Array.isArray(data)) {
      // List response (fallback for non-admin or old endpoint)
      const totalElements = data.length;
      const totalPages = Math.ceil(totalElements / size);
      return {
        data: {
          products: data.slice(page * size, (page + 1) * size).map(mapBackendProductToFrontend),
          totalElements,
          totalPages,
          currentPage: page,
          pageSize: size,
          hasNext: page < totalPages - 1,
          hasPrevious: page > 0,
        },
        success: true,
      };
    } else {
      throw new Error('Unexpected response format');
    }
  } catch (error) {
    console.error('Error fetching paginated products:', error);
    return {
      data: {
        products: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 100,
        hasNext: false,
        hasPrevious: false,
      },
      success: false,
      message: error instanceof Error ? error.message : 'Failed to fetch products',
    };
  }
}

/**
 * Fetch the maximum product price for a given category.
 * Backend endpoint: GET /api/products/category/{category}/max-price
 *
 * For the "All" view we pass a synthetic category value (e.g. "all"),
 * which the backend can handle specially to return the global max price.
 */
export async function getMaxPriceForCategory(category: string): Promise<number> {
  try {
    const safeCategory = encodeURIComponent(category || 'all');
    const response = await fetch(
      `${API_BASE_URL}/api/products/category/${safeCategory}/max-price`
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch max price for category: ${category}`);
    }

    const data = await response.json();
    const raw = (data && (data.maxPrice ?? data.max_price)) as number | string | undefined;
    const value =
      typeof raw === 'number'
        ? raw
        : typeof raw === 'string'
          ? parseFloat(raw)
          : NaN;

    if (!Number.isFinite(value) || value <= 0) {
      return 1000;
    }

    return value;
  } catch (error) {
    console.error('Error fetching max price for category:', category, error);
    // Fallback to a sensible default if backend call fails
    return 1000;
  }
}

/**
 * Fetch products page for the Categories view using backend filtering + sorting.
 *
 * Uses GET /api/products/list with:
 * - page / size: real backend pagination
 * - category, minPrice, maxPrice
 * - sort: NAME_ASC, NAME_DESC, PRICE_ASC, PRICE_DESC, RATING_DESC
 */
export async function getCategoryProducts(params: {
  page: number;
  size: number;
  category?: string | null;
  minPrice?: number;
  maxPrice?: number;
  sort: 'name-asc' | 'name-desc' | 'price-asc' | 'price-desc' | 'rating';
}): Promise<
  ApiResponse<{
    products: Product[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
    hasNext: boolean;
    hasPrevious: boolean;
  }>
> {
  try {
    const query = new URLSearchParams();
    query.set('page', String(params.page));
    query.set('size', String(params.size));

    if (params.category && params.category !== 'all') {
      query.set('category', params.category);
    }
    if (params.minPrice != null) {
      query.set('minPrice', String(params.minPrice));
    }
    if (params.maxPrice != null) {
      query.set('maxPrice', String(params.maxPrice));
    }

    // Map UI sort value -> backend sort enum
    const sortParam = (() => {
      switch (params.sort) {
        case 'name-asc':
          return 'NAME_ASC';
        case 'name-desc':
          return 'NAME_DESC';
        case 'price-asc':
          return 'PRICE_ASC';
        case 'price-desc':
          return 'PRICE_DESC';
        case 'rating':
        default:
          return 'RATING_DESC';
      }
    })();
    query.set('sort', sortParam);

    const url = `${API_BASE_URL}/api/products/list?${query.toString()}`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to fetch category products: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    const content = Array.isArray(data?.content) ? data.content : [];
    const products = content.map(mapBackendProductToFrontend);

    return {
      data: {
        products,
        totalElements: data.totalElements || products.length || 0,
        totalPages: data.totalPages || 0,
        currentPage: data.currentPage || params.page,
        pageSize: data.pageSize || params.size,
        hasNext: !!data.hasNext,
        hasPrevious: !!data.hasPrevious,
      },
      success: true,
    };
  } catch (error) {
    console.error('Error fetching category products:', error);
    return {
      data: {
        products: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: params.page,
        pageSize: params.size,
        hasNext: false,
        hasPrevious: false,
      },
      success: false,
      message: error instanceof Error ? error.message : 'Failed to fetch category products',
    };
  }
}

/**
 * Fetch all products with optional filters
 * Prefers GET /api/products/list (same as Admin); fallback GET /api/recommendations/products
 */
export async function getProducts(filters?: FilterOptions): Promise<ApiResponse<Product[]>> {
  try {
    let rawProducts: any[] = [];
    // Prefer products list (same source as Admin) so Categories always has catalog.
    // Use a moderate page size to avoid loading the entire catalog at once.
    const listRes = await fetch(`${API_BASE_URL}/api/products/list?page=0&size=500`);
    if (listRes.ok) {
      const listData = await listRes.json();
      rawProducts = listData.content && Array.isArray(listData.content) ? listData.content : [];
    }
    if (rawProducts.length === 0) {
      const recRes = await fetch(`${API_BASE_URL}/api/recommendations/products`);
      if (recRes.ok) {
        const data = await recRes.json();
        rawProducts = Array.isArray(data) ? data : [];
      }
    }
    let filtered = rawProducts.map(mapBackendProductToFrontend);
    
    if (filters) {
      // Price filter
      if (filters.priceRange) {
        filtered = filtered.filter(
          p => p.price >= filters.priceRange[0] && p.price <= filters.priceRange[1]
        );
      }
      
      // Category filter
      if (filters.categories && filters.categories.length > 0) {
        filtered = filtered.filter(p => filters.categories!.includes(p.category));
      }
      
      // In stock filter
      if (filters.inStockOnly) {
        filtered = filtered.filter(p => p.inStock);
      }
      
      // Sorting
      switch (filters.sortBy) {
        case 'name-asc':
          filtered.sort((a, b) => a.name.localeCompare(b.name));
          break;
        case 'name-desc':
          filtered.sort((a, b) => b.name.localeCompare(a.name));
          break;
        case 'price-asc':
          filtered.sort((a, b) => a.price - b.price);
          break;
        case 'price-desc':
          filtered.sort((a, b) => b.price - a.price);
          break;
        case 'rating':
          filtered.sort((a, b) => b.rating - a.rating);
          break;
      }
    }
    
    return {
      data: filtered,
      success: true,
    };
  } catch (error) {
    console.error('Error fetching products:', error);
    // Fallback to mock data
    let filtered = [...mockProducts];
    if (filters) {
      if (filters.priceRange) {
        filtered = filtered.filter(
          p => p.price >= filters.priceRange[0] && p.price <= filters.priceRange[1]
        );
      }
      if (filters.categories && filters.categories.length > 0) {
        filtered = filtered.filter(p => filters.categories!.includes(p.category));
      }
      if (filters.inStockOnly) {
        filtered = filtered.filter(p => p.inStock);
      }
      switch (filters.sortBy) {
        case 'name-asc':
          filtered.sort((a, b) => a.name.localeCompare(b.name));
          break;
        case 'name-desc':
          filtered.sort((a, b) => b.name.localeCompare(a.name));
          break;
        case 'price-asc':
          filtered.sort((a, b) => a.price - b.price);
          break;
        case 'price-desc':
          filtered.sort((a, b) => b.price - a.price);
          break;
        case 'rating':
          filtered.sort((a, b) => b.rating - a.rating);
          break;
      }
    }
    return {
      data: filtered,
      success: true,
    };
  }
}

/**
 * Check if products exist by IDs
 * Returns a Set of existing product IDs.
 * When the API fails or is unavailable, returns all requested IDs as "existing"
 * so cart items are not wrongly marked as unavailable.
 */
export async function checkProductsExist(productIds: number[]): Promise<Set<number>> {
  if (productIds.length === 0) return new Set();

  try {
    const response = await fetch(`${API_BASE_URL}/api/recommendations/products`);
    if (!response.ok) {
      // Don't mark all items as removed when endpoint fails (e.g. 404, 500)
      return new Set(productIds);
    }
    const data = await response.json();
    const products = Array.isArray(data) ? data : (data.products || data.data || []);
    const existingIds = new Set<number>();

    products.forEach((product: any) => {
      const id = product.id;
      const productId = typeof id === 'string' ? parseInt(id, 10) : id;
      if (!Number.isNaN(productId) && productIds.includes(productId)) {
        existingIds.add(productId);
      }
    });

    return existingIds;
  } catch (error) {
    console.error('Error checking products:', error);
    // On network/parse error, assume all cart items still exist
    return new Set(productIds);
  }
}

/**
 * Fetch single product by ID
 * Endpoint: GET /api/products/{id}
 */
export async function getProductById(id: string): Promise<ApiResponse<Product | null>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/products/${id}`);
    if (!response.ok) {
      if (response.status === 404) {
        return {
          data: null,
          success: false,
          message: 'Product not found',
        };
      }
      throw new Error('Failed to fetch product');
    }
    const product = await response.json();
    return {
      data: mapBackendProductToFrontend(product),
      success: true,
    };
  } catch (error) {
    console.error('Error fetching product:', error);
    // Fallback to mock data
    const product = mockProducts.find(p => p.id === id);
    return {
      data: product || null,
      success: !!product,
      message: product ? undefined : 'Product not found',
    };
  }
}

/**
 * Search suggestions for searchbox live search.
 * Endpoint: GET /api/products/suggest?q=...&category=...
 */
export async function getSearchSuggestions(q: string, category?: string): Promise<SuggestResponseDto> {
  const params = new URLSearchParams({ q: q.trim() });
  if (category) params.set('category', category);
  const response = await fetch(`${API_BASE_URL}/api/products/suggest?${params.toString()}`);
  if (!response.ok) throw new Error('Failed to fetch suggestions');
  return response.json();
}

/**
 * Fetch all distinct categories and subcategories from the database (for admin edit dropdowns).
 * Endpoint: GET /api/products/categories
 */
export async function getAllProductCategories(): Promise<
  ApiResponse<{ categories: string[]; subcategories: string[] }>
> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/products/categories`);
    if (!response.ok) throw new Error('Failed to fetch categories');
    const data = await response.json();
    return {
      data: {
        categories: Array.isArray(data.categories) ? data.categories : [],
        subcategories: Array.isArray(data.subcategories) ? data.subcategories : [],
      },
      success: true,
    };
  } catch (error) {
    console.error('Error fetching product categories:', error);
    return {
      data: { categories: [], subcategories: [] },
      success: false,
      message: error instanceof Error ? error.message : 'Failed to fetch categories',
    };
  }
}

/**
 * Fetch all categories
 * Note: Backend doesn't have a categories endpoint, so we extract from products (same source as getProducts)
 */
export async function getCategories(): Promise<ApiResponse<Category[]>> {
  try {
    let products: any[] = [];
    // Only fetch a limited batch of products to derive category names quickly
    const listRes = await fetch(`${API_BASE_URL}/api/products/list?page=0&size=500`);
    if (listRes.ok) {
      const listData = await listRes.json();
      products = listData.content && Array.isArray(listData.content) ? listData.content : [];
    }
    if (products.length === 0) {
      const recRes = await fetch(`${API_BASE_URL}/api/recommendations/products`);
      if (recRes.ok) {
        const data = await recRes.json();
        products = Array.isArray(data) ? data : [];
      }
    }
    const categoryMap = new Map<string, { subcategories: Set<string>, count: number }>();
    products.forEach((product: any) => {
      const category = product.category || product.mainCategory || 'Uncategorized';
      if (!categoryMap.has(category)) {
        categoryMap.set(category, { subcategories: new Set(), count: 0 });
      }
      const catData = categoryMap.get(category)!;
      catData.count++;
    });
    const categories: Category[] = Array.from(categoryMap.entries()).map(([name, data], index) => ({
      id: String(index + 1),
      name,
      subcategories: Array.from(data.subcategories),
      productCount: data.count,
    }));
    return {
      data: categories.length > 0 ? categories : mockCategories,
      success: true,
    };
  } catch (error) {
    console.error('Error fetching categories:', error);
    return {
      data: mockCategories,
      success: true,
    };
  }
}

/**
 * Search products
 * Note: Backend doesn't have a search endpoint, so we filter products client-side
 */
export async function searchProducts(query: string): Promise<ApiResponse<Product[]>> {
  try {
    // Get all products and filter client-side
    const response = await fetch(`${API_BASE_URL}/api/recommendations/products`);
    if (!response.ok) throw new Error('Failed to fetch products');
    const products = await response.json();
    
    const lowerQuery = query.toLowerCase();
    const results = products
      .map(mapBackendProductToFrontend)
      .filter(
        (p: Product) =>
          p.name.toLowerCase().includes(lowerQuery) ||
          p.description.toLowerCase().includes(lowerQuery) ||
          p.category.toLowerCase().includes(lowerQuery) ||
          p.tags?.some(t => t.toLowerCase().includes(lowerQuery))
      );
    
    return {
      data: results,
      success: true,
    };
  } catch (error) {
    console.error('Error searching products:', error);
    // Fallback to mock data
    const lowerQuery = query.toLowerCase();
    const results = mockProducts.filter(
      p =>
        p.name.toLowerCase().includes(lowerQuery) ||
        p.description.toLowerCase().includes(lowerQuery) ||
        p.category.toLowerCase().includes(lowerQuery) ||
        p.tags?.some(t => t.toLowerCase().includes(lowerQuery))
    );
    return {
      data: results,
      success: true,
    };
  }
}

// ============================================================================
// PRODUCT CRUD OPERATIONS (ProductController)
// ============================================================================

/**
 * Create a new product
 * Endpoint: POST /api/products
 * Request Body: ProductUpsertRequest
 * Requires: Admin access (userEmail must be admin email)
 */
export async function createProduct(
  productData: {
    productName: string;
    description: string;
    category?: string;
    subCategory?: string;
    price: number;
    quantity: number;
    imageUrl?: string;
    tags?: string[];
    rating?: number;
    views?: number;
  },
  userEmail?: string
): Promise<ApiResponse<Product>> {
  try {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };
    
    // Add admin email header if provided
    if (userEmail) {
      headers['X-User-Email'] = userEmail;
    }
    
    const response = await fetch(`${API_BASE_URL}/api/products`, {
      method: 'POST',
      headers,
      body: JSON.stringify(productData),
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({})) as { message?: string; errorType?: string };
      const msg = errorData.message || 'Failed to create product';
      const withType = errorData.errorType ? `[${errorData.errorType}] ${msg}` : msg;
      throw new Error(withType);
    }
    
    const product = await response.json();
    return {
      data: mapBackendProductToFrontend(product),
      success: true,
    };
  } catch (error) {
    console.error('Error creating product:', error);
    return {
      data: mockProducts[0],
      success: false,
      message: error instanceof Error ? error.message : 'Failed to create product',
    };
  }
}

/**
 * Update an existing product
 * Endpoint: PUT /api/products/{id}
 * Request Body: ProductUpsertRequest
 * Requires: Admin access (userEmail must be admin email)
 */
export async function updateProduct(
  id: string,
  productData: {
    productName?: string;
    description?: string;
    category?: string;
    subCategory?: string;
    price?: number;
    quantity?: number;
    imageUrl?: string;
    tags?: string[];
    rating?: number;
    views?: number;
  },
  userEmail?: string
): Promise<ApiResponse<Product>> {
  try {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };
    
    // Add admin email header if provided
    if (userEmail) {
      headers['X-User-Email'] = userEmail;
    }
    
    const response = await fetch(`${API_BASE_URL}/api/products/${id}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify(productData),
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || 'Failed to update product');
    }
    
    const product = await response.json();
    return {
      data: mapBackendProductToFrontend(product),
      success: true,
    };
  } catch (error) {
    console.error('Error updating product:', error);
    return {
      data: mockProducts[0],
      success: false,
      message: error instanceof Error ? error.message : 'Failed to update product',
    };
  }
}

/**
 * Delete a product
 * Endpoint: DELETE /api/products/{id}
 * Requires: Admin access (userEmail must be admin email)
 */
export async function deleteProduct(id: string, userEmail?: string): Promise<ApiResponse<void>> {
  try {
    const headers: HeadersInit = {};
    
    // Add admin email header if provided
    if (userEmail) {
      headers['X-User-Email'] = userEmail;
    }
    
    const response = await fetch(`${API_BASE_URL}/api/products/${id}`, {
      method: 'DELETE',
      headers,
    });
    
    // Check if response has content before trying to parse JSON
    const contentType = response.headers.get('content-type');
    let data: any = {};
    
    if (contentType && contentType.includes('application/json')) {
      try {
        const text = await response.text();
        if (text) {
          data = JSON.parse(text);
        }
      } catch (parseError) {
        console.error('Error parsing response:', parseError);
        // If parsing fails but status is OK, assume success
        if (response.ok) {
          data = { success: true, message: 'Product deleted successfully' };
        }
      }
    } else if (response.ok) {
      // Response is OK but no JSON body
      data = { success: true, message: 'Product deleted successfully' };
    }
    
    if (!response.ok) {
      throw new Error(data.message || `Failed to delete product: ${response.status} ${response.statusText}`);
    }
    
    // Backend returns {success: true, message: "..."}
    return {
      data: undefined,
      success: data.success !== false, // Use backend success if available, default to true for 200 OK
      message: data.message,
    };
  } catch (error) {
    console.error('Error deleting product:', error);
    return {
      data: undefined,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to delete product',
    };
  }
}

// ============================================================================
// AUTHENTICATION (AuthController)
// ============================================================================

/**
 * Login user
 * Endpoint: POST /auth/login
 * Request Body: { email: string, password: string }
 */
export async function login(email: string, password: string): Promise<ApiResponse<any>> {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });
    
    const data = await response.json();
    
    // Backend returns { success: true/false, message: "...", user: {...} }
    if (!data.success) {
      return {
        data: null,
        success: false,
        message: data.message || 'Login failed',
      };
    }
    
    // Backend returns user object directly in the response
    return {
      data: data.user || data,
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error logging in:', error);
    return {
      data: null,
      success: false,
      message: error instanceof Error ? error.message : 'Login failed',
    };
  }
}

/**
 * Test login (GET method)
 * Endpoint: GET /auth/test-login?email={email}&password={password}
 */
export async function testLogin(email: string, password: string): Promise<ApiResponse<any>> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/auth/test-login?email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
    );
    
    if (!response.ok) throw new Error('Test login failed');
    const data = await response.json();
    return {
      data,
      success: true,
    };
  } catch (error) {
    console.error('Error in test login:', error);
    return {
      data: null,
      success: false,
      message: 'Test login failed',
    };
  }
}

/**
 * Register new user
 * Endpoint: POST /auth/register
 * Request Body: { name: string, email: string, password: string }
 */
export async function register(name: string, email: string, password: string): Promise<ApiResponse<any>> {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ name, email, password }),
    });
    
    const data = await response.json();
    
    // Backend returns { success: true/false, message: "...", data: {...} }
    if (!response.ok || !data.success) {
      return {
        data: null,
        success: false,
        message: data.message || 'Registration failed',
      };
    }
    
    return {
      data: data.data || data,
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error registering:', error);
    return {
      data: null,
      success: false,
      message: error instanceof Error ? error.message : 'Registration failed',
    };
  }
}

/**
 * Check credentials
 * Endpoint: GET /auth/check-credentials?email={email}&password={password}
 */
export async function checkCredentials(email: string, password: string): Promise<ApiResponse<{ valid: boolean; message: string }>> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/auth/check-credentials?email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
    );
    
    if (!response.ok) throw new Error('Failed to check credentials');
    const data = await response.json();
    return {
      data,
      success: true,
    };
  } catch (error) {
    console.error('Error checking credentials:', error);
    return {
      data: { valid: false, message: 'Failed to check credentials' },
      success: false,
    };
  }
}

// ============================================================================
// EVENTS (EventController)
// ============================================================================

/**
 * Record product view event
 * Endpoint: POST /api/events/product-view/{productId}?userId={userId}
 */
export async function recordProductView(productId: string, userId: number): Promise<ApiResponse<void>> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/events/product-view/${productId}?userId=${userId}`,
      {
        method: 'POST',
      }
    );
    
    if (!response.ok) throw new Error('Failed to record product view');
    return {
      data: undefined,
      success: true,
    };
  } catch (error) {
    console.error('Error recording product view:', error);
    return {
      data: undefined,
      success: false,
      message: 'Failed to record product view',
    };
  }
}

// ============================================================================
// ADDITIONAL RECOMMENDATION ENDPOINTS (RecommendationController)
// ============================================================================

/**
 * Get recommendations for a specific user
 * Endpoint: GET /api/recommendations/user/{userId}?limit={limit}
 */
export async function getUserRecommendations(userId: number, limit: number = 10): Promise<ApiResponse<Recommendation>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/recommendations/user/${userId}?limit=${limit}`);
    if (!response.ok) throw new Error('Failed to fetch user recommendations');
    const products = await response.json();
    return {
      data: {
        products: products.map(mapBackendProductToFrontend),
        type: 'personalized',
        confidence: 0.87,
      },
      success: true,
    };
  } catch (error) {
    console.error('Error fetching user recommendations:', error);
    return {
      data: {
        products: [],
        type: 'personalized',
      },
      success: false,
    };
  }
}

/**
 * Get recommendations by category
 * Endpoint: GET /api/recommendations/category/{category}?limit={limit}
 */
export async function getRecommendationsByCategory(category: string, limit: number = 10): Promise<ApiResponse<Recommendation>> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/recommendations/category/${encodeURIComponent(category)}?limit=${limit}`
    );
    if (!response.ok) throw new Error('Failed to fetch category recommendations');
    const products = await response.json();
    return {
      data: {
        products: products.map(mapBackendProductToFrontend),
        type: 'popular',
      },
      success: true,
    };
  } catch (error) {
    console.error('Error fetching category recommendations:', error);
    return {
      data: {
        products: [],
        type: 'popular',
      },
      success: false,
    };
  }
}

/**
 * Record product view (via RecommendationController). Persists to DB when userId is passed so ML can use it later.
 * Endpoint: POST /api/recommendations/view/{productId}?userId={userId}
 */
export async function recordProductViewViaRecommendations(
  productId: string,
  userId?: number | null
): Promise<ApiResponse<{ message: string }>> {
  try {
    const url = userId != null && userId > 0
      ? `${API_BASE_URL}/api/recommendations/view/${productId}?userId=${userId}`
      : `${API_BASE_URL}/api/recommendations/view/${productId}`;
    const response = await fetch(url, {
      method: 'POST',
    });
    
    if (!response.ok) throw new Error('Failed to record product view');
    const data = await response.json();
    return {
      data,
      success: true,
    };
  } catch (error) {
    console.error('Error recording product view:', error);
    return {
      data: { message: 'Failed to record view' },
      success: false,
    };
  }
}

/**
 * Record search
 * Endpoint: POST /api/recommendations/search/{userId}
 * Request Body: { searchTerm: string }
 */
export async function recordSearch(userId: number, searchTerm: string): Promise<ApiResponse<{ message: string }>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/recommendations/search/${userId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ searchTerm }),
    });
    
    if (!response.ok) throw new Error('Failed to record search');
    const data = await response.json();
    return {
      data,
      success: true,
    };
  } catch (error) {
    console.error('Error recording search:', error);
    return {
      data: { message: 'Failed to record search' },
      success: false,
    };
  }
}

/**
 * Record purchase
 * Endpoint: POST /api/recommendations/purchase/{userId}
 * Request Body: { productId: number }
 */
export async function recordPurchase(userId: number, productId: number): Promise<ApiResponse<{ message: string }>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/recommendations/purchase/${userId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ productId }),
    });
    
    if (!response.ok) throw new Error('Failed to record purchase');
    const data = await response.json();
    return {
      data,
      success: true,
    };
  } catch (error) {
    console.error('Error recording purchase:', error);
    return {
      data: null,
      success: false,
      message: 'Failed to record purchase',
    };
  }
}

// ============================================================================
// USER PROFILE OPERATIONS (UserController)
// ============================================================================

/**
 * Get user profile
 * Endpoint: GET /api/users/{userId}
 */
export async function getUserProfile(userId: string | number): Promise<ApiResponse<any>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/${userId}`);
    
    const data = await response.json();
    
    if (!data.success) {
      return {
        data: null,
        success: false,
        message: data.message || 'Failed to get user profile',
      };
    }
    
    return {
      data: data.data,
      success: true,
    };
  } catch (error) {
    console.error('Error getting user profile:', error);
    return {
      data: null,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to get user profile',
    };
  }
}

/**
 * Update user profile
 * Endpoint: PUT /api/users/{userId}
 * Request Body: { username?: string, email?: string }
 */
export async function updateUserProfile(userId: string | number, username?: string, email?: string): Promise<ApiResponse<any>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/users/${userId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, email }),
    });
    
    const data = await response.json();
    
    if (!data.success) {
      return {
        data: null,
        success: false,
        message: data.message || 'Failed to update profile',
      };
    }
    
    return {
      data: data.data,
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error updating user profile:', error);
    return {
      data: null,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to update profile',
    };
  }
}
/**
 * Get trending products (via HomeController)
 * Endpoint: GET /api/home/trending?limit={limit}
 */
export async function getTrendingProducts(limit: number = 5): Promise<ApiResponse<Product[]>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/home/trending?limit=${limit}`);
    if (!response.ok) throw new Error('Failed to fetch trending products');
    const products = await response.json();
    return {
      data: products.map((p: any) => mapBackendProductToFrontend(p)),
      success: true,
    };
  } catch (error) {
    console.error('Error fetching trending products:', error);
    // Fallback to getPopularPurchases
    return getPopularPurchases().then(result => ({
      data: result.data.products,
      success: result.success,
    }));
  }
}

// ============================================================================
// PAYMENT OPERATIONS (PaymentController)
// ============================================================================

/**
 * Create a payment intent for checkout or top-up
 * Endpoint: POST /api/payments/create-intent
 * Request Body: { userId: number, amount: number, currency?: string, type?: 'checkout' | 'top_up' }
 */
export async function createPaymentIntent(
  userId: number,
  amount: number,
  currency: string = 'usd',
  type: 'checkout' | 'top_up' = 'checkout'
): Promise<ApiResponse<{ clientSecret: string; paymentIntentId: string; paymentId: number }>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/payments/create-intent`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ userId, amount, currency, type }),
    });

    let data: { success?: boolean; message?: string; clientSecret?: string; paymentIntentId?: string; paymentId?: number };
    try {
      data = await response.json();
    } catch {
      return {
        data: null as any,
        success: false,
        message: response.ok ? 'Invalid response from server' : 'Server error. Please try again.',
      };
    }

    if (!response.ok || !data?.success) {
      return {
        data: null as any,
        success: false,
        message: (data?.message as string) || 'Failed to create payment intent',
      };
    }

    if (!data.clientSecret) {
      return {
        data: null as any,
        success: false,
        message: data.message || 'Payment could not be initialized',
      };
    }

    return {
      data: {
        clientSecret: data.clientSecret,
        paymentIntentId: data.paymentIntentId || '',
        paymentId: data.paymentId ?? 0,
      },
      success: true,
    };
  } catch (error) {
    console.error('Error creating payment intent:', error);
    return {
      data: null as any,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to create payment intent',
    };
  }
}

/**
 * Create a payment intent for guest checkout (no user required)
 * Endpoint: POST /api/payments/create-guest-intent
 * Request Body: { amount: number, currency?: string, productIds?: number[] }
 */
export async function createGuestPaymentIntent(
  amount: number,
  currency: string = 'usd',
  productIds?: number[]
): Promise<ApiResponse<{ clientSecret: string; paymentIntentId: string; paymentId: number }>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/payments/create-guest-intent`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ amount, currency, productIds }),
    });

    let data: { success?: boolean; message?: string; clientSecret?: string; paymentIntentId?: string; paymentId?: number };
    try {
      data = await response.json();
    } catch {
      console.error('Failed to parse guest payment intent response:', response.status, response.statusText);
      return {
        data: null as any,
        success: false,
        message: response.ok ? 'Invalid response from server' : `Server error (${response.status}): ${response.statusText}`,
      };
    }

    if (!response.ok || !data?.success) {
      console.error('Guest payment intent failed:', {
        status: response.status,
        statusText: response.statusText,
        data: data,
        url: `${API_BASE_URL}/api/payments/create-guest-intent`
      });
      return {
        data: null as any,
        success: false,
        message: (data?.message as string) || `Failed to create payment intent (${response.status}: ${response.statusText})`,
      };
    }

    if (!data.clientSecret) {
      console.error('Guest payment intent missing clientSecret:', data);
      return {
        data: null as any,
        success: false,
        message: data.message || 'Payment could not be initialized',
      };
    }

    return {
      data: {
        clientSecret: data.clientSecret,
        paymentIntentId: data.paymentIntentId || '',
        paymentId: data.paymentId ?? 0,
      },
      success: true,
    };
  } catch (error) {
    console.error('Error creating guest payment intent:', error);
    return {
      data: null as any,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to create payment intent',
    };
  }
}

/**
 * Pay with wallet balance. Deducts from user wallet and clears cart.
 * Endpoint: POST /api/payments/pay-with-wallet
 */
export async function payWithWallet(
  userId: number,
  amount: number
): Promise<ApiResponse<{ newBalance: number }>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/payments/pay-with-wallet`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, amount }),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok || !data.success) {
      return {
        data: null as any,
        success: false,
        message: (data.message as string) || 'Payment failed',
      };
    }
    return {
      data: { newBalance: data.newBalance ?? 0 },
      success: true,
    };
  } catch (error) {
    return {
      data: null as any,
      success: false,
      message: error instanceof Error ? error.message : 'Payment failed',
    };
  }
}

/**
 * Confirm a payment
 * Endpoint: POST /api/payments/confirm
 * Request Body: { paymentIntentId: string, paymentMethodId?: string }
 */
export async function confirmPayment(
  paymentIntentId: string,
  paymentMethodId?: string
): Promise<ApiResponse<{ status: string; success: boolean }>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/payments/confirm`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ paymentIntentId, paymentMethodId }),
    });

    let data: { success?: boolean; status?: string; message?: string } = {};
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      try {
        data = await response.json();
      } catch {
        data = {};
      }
    }

    const success = data.success === true;
    const message = !response.ok
      ? (data.message || `Server error ${response.status}`)
      : data.message;

    return {
      data: {
        status: data.status || (success ? 'succeeded' : 'failed'),
        success,
      },
      success,
      message,
    };
  } catch (error) {
    console.error('Error confirming payment:', error);
    return {
      data: { status: 'failed', success: false },
      success: false,
      message: error instanceof Error ? error.message : 'Failed to confirm payment',
    };
  }
}

/**
 * Save a payment method for a user
 * Endpoint: POST /api/payments/save-method
 * Request Body: { userId: number, stripePaymentMethodId: string }
 */
export async function savePaymentMethod(
  userId: number,
  stripePaymentMethodId: string
): Promise<ApiResponse<any>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/payments/save-method`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ userId, stripePaymentMethodId }),
    });

    const data = await response.json();

    if (!data.success) {
      return {
        data: null,
        success: false,
        message: data.message || 'Failed to save payment method',
      };
    }

    return {
      data: data.paymentMethod,
      success: true,
    };
  } catch (error) {
    console.error('Error saving payment method:', error);
    return {
      data: null,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to save payment method',
    };
  }
}

/**
 * Get all payment methods for a user
 * Endpoint: GET /api/payments/methods/{userId}
 */
export async function getUserPaymentMethods(userId: number): Promise<ApiResponse<any[]>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/payments/methods/${userId}`);

    const data = await response.json();

    if (!data.success) {
      return {
        data: [],
        success: false,
        message: data.message || 'Failed to get payment methods',
      };
    }

    return {
      data: data.paymentMethods || [],
      success: true,
    };
  } catch (error) {
    console.error('Error getting payment methods:', error);
    return {
      data: [],
      success: false,
      message: error instanceof Error ? error.message : 'Failed to get payment methods',
    };
  }
}

/**
 * Set default payment method
 * Endpoint: PUT /api/payments/methods/{userId}/default
 * Request Body: { paymentMethodId: number }
 */
export async function setDefaultPaymentMethod(
  userId: number,
  paymentMethodId: number
): Promise<ApiResponse<void>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/payments/methods/${userId}/default`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ paymentMethodId }),
    });

    const data = await response.json();

    if (!data.success) {
      return {
        data: undefined,
        success: false,
        message: data.message || 'Failed to set default payment method',
      };
    }

    return {
      data: undefined,
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error setting default payment method:', error);
    return {
      data: undefined,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to set default payment method',
    };
  }
}

/**
 * Delete a payment method
 * Endpoint: DELETE /api/payments/methods/{userId}/{paymentMethodId}
 */
export async function deletePaymentMethod(
  userId: number,
  paymentMethodId: number
): Promise<ApiResponse<void>> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/payments/methods/${userId}/${paymentMethodId}`,
      {
        method: 'DELETE',
      }
    );

    const data = await response.json();

    if (!data.success) {
      return {
        data: undefined,
        success: false,
        message: data.message || 'Failed to delete payment method',
      };
    }

    return {
      data: undefined,
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error deleting payment method:', error);
    return {
      data: undefined,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to delete payment method',
    };
  }
}

/**
 * Get payment history for a user
 * Endpoint: GET /api/payments/history/{userId}
 */
export async function getPaymentHistory(userId: number): Promise<ApiResponse<any[]>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/payments/history/${userId}`);

    const data = await response.json();

    if (!data.success) {
      return {
        data: [],
        success: false,
        message: data.message || 'Failed to get payment history',
      };
    }

    return {
      data: data.payments || [],
      success: true,
    };
  } catch (error) {
    console.error('Error getting payment history:', error);
    return {
      data: [],
      success: false,
      message: error instanceof Error ? error.message : 'Failed to get payment history',
    };
  }
}

/**
 * Cart Management API Functions
 */

/**
 * Get all cart items for a user
 * Endpoint: GET /api/cart/{userId}
 */
export async function getCartItems(userId: number): Promise<ApiResponse<any[]>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/cart/${userId}`);

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return {
        data: [],
        success: false,
        message: errorData.message || 'Failed to get cart items',
      };
    }

    const data = await response.json();

    if (!data.success) {
      return {
        data: [],
        success: false,
        message: data.message || 'Failed to get cart items',
      };
    }

    // Map backend cart items to frontend format
    const cartItems = (data.data || []).map((item: any) => ({
      id: item.id,
      productId: item.productId,
      productName: item.productName,
      price: item.price,
      quantity: item.quantity,
      imageUrl: item.productImageUrl || item.product?.imageUrl,
      product: item.product ? mapBackendProductToFrontend(item.product) : null,
      addedAt: item.addedAt,
    }));

    return {
      data: cartItems,
      success: true,
    };
  } catch (error) {
    console.error('Error getting cart items:', error);
    return {
      data: [],
      success: false,
      message: error instanceof Error ? error.message : 'Failed to get cart items',
    };
  }
}

/**
 * Add item to cart
 * Endpoint: POST /api/cart/{userId}/items
 */
export async function addToCart(userId: number, productId: number, quantity: number = 1, options?: Record<string, string>): Promise<ApiResponse<any>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/cart/${userId}/items`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        productId,
        quantity,
        options: options || {},
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return {
        data: null,
        success: false,
        message: errorData.message || 'Failed to add item to cart',
      };
    }

    const data = await response.json();

    if (!data.success) {
      return {
        data: null,
        success: false,
        message: data.message || 'Failed to add item to cart',
      };
    }

    return {
      data: data.data,
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error adding to cart:', error);
    return {
      data: null,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to add item to cart',
    };
  }
}

/**
 * Update cart item quantity
 * Endpoint: PUT /api/cart/{userId}/items/{itemId}
 */
export async function updateCartItem(userId: number, itemId: number, quantity: number): Promise<ApiResponse<any>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/cart/${userId}/items/${itemId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        quantity,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return {
        data: null,
        success: false,
        message: errorData.message || 'Failed to update cart item',
      };
    }

    const data = await response.json();

    if (!data.success) {
      return {
        data: null,
        success: false,
        message: data.message || 'Failed to update cart item',
      };
    }

    return {
      data: data.data,
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error updating cart item:', error);
    return {
      data: null,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to update cart item',
    };
  }
}

/**
 * Remove item from cart
 * Endpoint: DELETE /api/cart/{userId}/items/{itemId}
 */
export async function removeFromCart(userId: number, itemId: number): Promise<ApiResponse<void>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/cart/${userId}/items/${itemId}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return {
        data: undefined,
        success: false,
        message: errorData.message || 'Failed to remove item from cart',
      };
    }

    const data = await response.json();

    if (!data.success) {
      return {
        data: undefined,
        success: false,
        message: data.message || 'Failed to remove item from cart',
      };
    }

    return {
      data: undefined,
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error removing from cart:', error);
    return {
      data: undefined,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to remove item from cart',
    };
  }
}

/**
 * Clear all items from cart
 * Endpoint: DELETE /api/cart/{userId}
 */
export async function clearCart(userId: number): Promise<ApiResponse<void>> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/cart/${userId}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return {
        data: undefined,
        success: false,
        message: errorData.message || 'Failed to clear cart',
      };
    }

    const data = await response.json();

    if (!data.success) {
      return {
        data: undefined,
        success: false,
        message: data.message || 'Failed to clear cart',
      };
    }

    return {
      data: undefined,
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error clearing cart:', error);
    return {
      data: undefined,
      success: false,
      message: error instanceof Error ? error.message : 'Failed to clear cart',
    };
  }
}

// Export mock data for components that need direct access
export { mockProducts, mockCategories };
