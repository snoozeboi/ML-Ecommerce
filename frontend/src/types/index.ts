// Product interface matching ML recommendation system
export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  originalPrice?: number;
  image: string;
  images?: string[];
  category: string;
  subcategory?: string;
  rating: number;
  reviewCount: number;
  inStock: boolean;
  tags?: string[];
}

export interface User {
  id: string;
  username: string;
  email?: string;
  avatar?: string;
  wallet?: number;
}

export interface Recommendation {
  products: Product[];
  type: 'personalized' | 'popular' | 'similar' | 'trending';
  confidence?: number;
}

export interface Category {
  id: string;
  name: string;
  subcategories: string[];
  productCount: number;
}

export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  price: number;
  quantity: number;
  imageUrl?: string;
  addedAt?: string;
  product?: Product; // Full product data when loaded
}

export interface FilterOptions {
  sortBy: 'name-asc' | 'name-desc' | 'price-asc' | 'price-desc' | 'rating';
  priceRange: [number, number];
  categories?: string[];
  inStockOnly?: boolean;
}

export interface ApiResponse<T> {
  data: T;
  success: boolean;
  message?: string;
}

/** One suggestion item from search suggest API (searchbox live search). */
export interface ProductSuggestionDto {
  id: number;
  name: string;
  category: string;
  price: number;
  finalPrice: number;
  discountPercent: number | null;
  imageUrl: string;
  /** Rating (e.g. 4.3) - shown with stars in the search dropdown when provided by backend */
  rating?: number;
}

/** Response from GET /api/products/suggest. */
export interface SuggestResponseDto {
  query: string;
  suggestions: ProductSuggestionDto[];
}
