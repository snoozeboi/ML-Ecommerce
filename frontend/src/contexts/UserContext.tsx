import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, Product, CartItem } from '@/types';
import { 
  login as apiLogin, 
  register as apiRegister, 
  getUserProfile, 
  updateUserProfile as apiUpdateUserProfile,
  getCartItems,
  addToCart as apiAddToCart,
  updateCartItem as apiUpdateCartItem,
  removeFromCart as apiRemoveFromCart,
  clearCart as apiClearCart
} from '@/services/api';

export interface Order {
  id: string;
  items: { product: Product; quantity: number }[];
  total: number;
  status: 'pending' | 'processing' | 'shipped' | 'delivered';
  date: string;
}

interface UserContextType {
  user: User | null;
  isLoggedIn: boolean;
  isAdmin: boolean;
  orders: Order[];
  wishlist: Product[];
  cart: CartItem[];
  login: (email: string, password: string) => Promise<boolean>;
  register: (name: string, email: string, password: string) => Promise<boolean>;
  logout: () => void;
  updateProfile: (updates: Partial<User>) => Promise<void>;
  addToWishlist: (product: Product) => void;
  removeFromWishlist: (productId: string) => void;
  isInWishlist: (productId: string) => boolean;
  addToCart: (product: Product, quantity?: number) => Promise<void>;
  removeFromCart: (productId: string) => Promise<void>;
  updateCartQuantity: (productId: string, quantity: number) => Promise<void>;
  clearCart: () => Promise<void>;
  refreshUserProfile: () => Promise<void>;
  addOrder: (order: Order) => void;
}

// Admin email - should match backend configuration
const ADMIN_EMAIL = 'admin@ecommerce.com';

const UserContext = createContext<UserContextType | undefined>(undefined);

const STORAGE_KEYS = {
  USER: 'user_profile',
  ORDERS: 'user_orders',
  WISHLIST: 'user_wishlist',
  CART: 'user_cart',
};

// Mock orders for demo
const mockOrders: Order[] = [
  {
    id: 'ORD-001',
    items: [
      {
        product: {
          id: '1',
          name: 'Wireless Headphones',
          description: 'Premium wireless headphones',
          price: 149.99,
          image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=300',
          category: 'Electronics',
          rating: 4.5,
          reviewCount: 128,
          inStock: true,
        },
        quantity: 1,
      },
    ],
    total: 149.99,
    status: 'delivered',
    date: '2024-01-15',
  },
  {
    id: 'ORD-002',
    items: [
      {
        product: {
          id: '2',
          name: 'Smart Watch',
          description: 'Fitness tracking smartwatch',
          price: 299.99,
          image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300',
          category: 'Electronics',
          rating: 4.7,
          reviewCount: 256,
          inStock: true,
        },
        quantity: 1,
      },
    ],
    total: 299.99,
    status: 'shipped',
    date: '2024-01-20',
  },
];

export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.USER);
    return saved ? JSON.parse(saved) : null;
  });

  const [orders, setOrders] = useState<Order[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.ORDERS);
    return saved ? JSON.parse(saved) : [];
  });

  const [wishlist, setWishlist] = useState<Product[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.WISHLIST);
    return saved ? JSON.parse(saved) : [];
  });

  const [cart, setCart] = useState<CartItem[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.CART);
    return saved ? JSON.parse(saved) : [];
  });

  // Load cart from API when user logs in
  useEffect(() => {
    const loadCartFromAPI = async () => {
      if (user && user.id) {
        try {
          const userId = parseInt(user.id);
          if (!isNaN(userId)) {
            const response = await getCartItems(userId);
            if (response.success && response.data) {
              setCart(response.data);
            }
          }
        } catch (error) {
          console.error('Error loading cart from API:', error);
          // Fallback to localStorage if API fails
          const saved = localStorage.getItem(STORAGE_KEYS.CART);
          if (saved) {
            setCart(JSON.parse(saved));
          }
        }
      }
    };

    loadCartFromAPI();
  }, [user]);

  // Persist to localStorage
  useEffect(() => {
    if (user) {
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_KEYS.USER);
    }
  }, [user]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.ORDERS, JSON.stringify(orders));
  }, [orders]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.WISHLIST, JSON.stringify(wishlist));
  }, [wishlist]);

  // Persist cart to localStorage as backup
  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.CART, JSON.stringify(cart));
  }, [cart]);

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      // Try to call the backend API
      const response = await apiLogin(email, password);
      
      if (response.success && response.data) {
        // Use data from backend if available
        const backendUser = response.data;
        const userData: User = {
          id: String(backendUser.id || crypto.randomUUID()),
          username: backendUser.userName || backendUser.username || email.split('@')[0],
          email: backendUser.email || email,
          wallet: backendUser.wallet !== undefined ? backendUser.wallet : 0,
          avatar: backendUser.avatar,
        };
        setUser(userData);
        // Add mock orders on first login
        if (orders.length === 0) {
          setOrders(mockOrders);
        }
        return true;
      } else {
        // Login failed - throw error with message
        const errorMessage = response.message || 'Login failed. Please check your credentials.';
        throw new Error(errorMessage);
      }
    } catch (error) {
      console.error('Login error:', error);
      // Re-throw the error so the AuthModal can show it to the user
      throw error;
    }
  };

  const register = async (name: string, email: string, password: string): Promise<boolean> => {
    try {
      // Try to call the backend API
      const response = await apiRegister(name, email, password);
      
      if (response.success && response.data) {
        // Use data from backend if available
        const userData: User = {
          id: String(response.data.id || crypto.randomUUID()),
          username: response.data.username || name,
          email: response.data.email || email,
          wallet: response.data.wallet || 0,
          avatar: response.data.avatar,
        };
        setUser(userData);
        return true;
      } else {
        // Registration failed - throw error with message
        const errorMessage = response.message || 'Registration failed. Please try again.';
        throw new Error(errorMessage);
      }
    } catch (error) {
      console.error('Registration error:', error);
      // Re-throw the error so the AuthModal can show it to the user
      throw error;
    }
  };

  const logout = () => {
    setUser(null);
    setOrders([]);
    setWishlist([]);
    setCart([]);
    localStorage.removeItem(STORAGE_KEYS.USER);
    localStorage.removeItem(STORAGE_KEYS.ORDERS);
    localStorage.removeItem(STORAGE_KEYS.WISHLIST);
    localStorage.removeItem(STORAGE_KEYS.CART);
  };

  const updateProfile = async (updates: Partial<User>) => {
    if (!user) {
      throw new Error('User must be logged in to update profile');
    }

    try {
      // Call backend API to update profile in PostgreSQL
      const response = await apiUpdateUserProfile(
        user.id,
        updates.username,
        updates.email
      );

      if (response.success && response.data) {
        // Update local state with data from backend
        const updatedUser: User = {
          id: String(response.data.id || user.id),
          username: response.data.username || updates.username || user.username,
          email: response.data.email || updates.email || user.email,
          wallet: response.data.wallet !== undefined ? response.data.wallet : user.wallet,
          avatar: response.data.avatar || updates.avatar || user.avatar,
        };
        setUser(updatedUser);
      } else {
        throw new Error(response.message || 'Failed to update profile');
      }
    } catch (error) {
      console.error('Profile update error:', error);
      throw error;
    }
  };

  const addToWishlist = (product: Product) => {
    if (!wishlist.find((p) => p.id === product.id)) {
      setWishlist([...wishlist, product]);
    }
  };

  const removeFromWishlist = (productId: string) => {
    setWishlist(wishlist.filter((p) => p.id !== productId));
  };

  const isInWishlist = (productId: string) => {
    return wishlist.some((p) => p.id === productId);
  };

  const addToCart = async (product: Product, quantity: number = 1) => {
    // If user is logged in, use API
    if (user && user.id) {
      try {
        const userId = parseInt(user.id);
        if (!isNaN(userId)) {
          const productId = parseInt(product.id);
          if (!isNaN(productId)) {
            const response = await apiAddToCart(userId, productId, quantity);
            if (response.success) {
              // Reload cart from API to get updated data
              const cartResponse = await getCartItems(userId);
              if (cartResponse.success && cartResponse.data) {
                setCart(cartResponse.data);
                return;
              }
            }
          }
        }
      } catch (error) {
        console.error('Error adding to cart via API:', error);
        // Fall through to localStorage fallback
      }
    }

    // Fallback to localStorage if not logged in or API fails
    setCart((currentCart) => {
      const existingItem = currentCart.find((item) => item.productId === parseInt(product.id));
      if (existingItem) {
        return currentCart.map((item) =>
          item.productId === parseInt(product.id)
            ? { ...item, quantity: item.quantity + quantity }
            : item
        );
      } else {
        const newItem: CartItem = {
          id: Date.now(),
          productId: parseInt(product.id),
          productName: product.name,
          price: product.price,
          quantity,
          imageUrl: product.image,
          product,
          addedAt: new Date().toISOString(),
        };
        return [...currentCart, newItem];
      }
    });
  };

  const removeFromCart = async (productId: string) => {
    // If user is logged in, use API
    if (user && user.id) {
      try {
        const userId = parseInt(user.id);
        if (!isNaN(userId)) {
          // Find the cart item ID
          const cartItem = cart.find((item) => item.productId === parseInt(productId));
          if (cartItem) {
            const response = await apiRemoveFromCart(userId, cartItem.id);
            if (response.success) {
              // Reload cart from API
              const cartResponse = await getCartItems(userId);
              if (cartResponse.success && cartResponse.data) {
                setCart(cartResponse.data);
                return;
              }
            }
          }
        }
      } catch (error) {
        console.error('Error removing from cart via API:', error);
        // Fall through to localStorage fallback
      }
    }

    // Fallback to localStorage if not logged in or API fails
    setCart((currentCart) => currentCart.filter((item) => item.productId !== parseInt(productId)));
  };

  const updateCartQuantity = async (productId: string, quantity: number) => {
    if (quantity <= 0) {
      await removeFromCart(productId);
      return;
    }

    // If user is logged in, use API
    if (user && user.id) {
      try {
        const userId = parseInt(user.id);
        if (!isNaN(userId)) {
          // Find the cart item ID
          const cartItem = cart.find((item) => item.productId === parseInt(productId));
          if (cartItem) {
            const response = await apiUpdateCartItem(userId, cartItem.id, quantity);
            if (response.success) {
              // Reload cart from API
              const cartResponse = await getCartItems(userId);
              if (cartResponse.success && cartResponse.data) {
                setCart(cartResponse.data);
                return;
              }
            }
          }
        }
      } catch (error) {
        console.error('Error updating cart via API:', error);
        // Fall through to localStorage fallback
      }
    }

    // Fallback to localStorage if not logged in or API fails
    setCart((currentCart) =>
      currentCart.map((item) =>
        item.productId === parseInt(productId) ? { ...item, quantity } : item
      )
    );
  };

  const clearCart = async () => {
    // If user is logged in, use API
    if (user && user.id) {
      try {
        const userId = parseInt(user.id);
        if (!isNaN(userId)) {
          const response = await apiClearCart(userId);
          if (response.success) {
            setCart([]);
            return;
          }
        }
      } catch (error) {
        console.error('Error clearing cart via API:', error);
        // Fall through to localStorage fallback
      }
    }

    // Fallback to localStorage if not logged in or API fails
    setCart([]);
  };

  const addOrder = (order: Order) => {
    setOrders((prev) => [...prev, order]);
  };

  const refreshUserProfile = async () => {
    if (!user?.id) return;
    try {
      const res = await getUserProfile(parseInt(user.id));
      if (res.success && res.data) {
        const u = res.data as Record<string, unknown>;
        setUser({
          ...user,
          id: String(u.id ?? user.id),
          username: (u.username as string) ?? user.username,
          email: (u.email as string) ?? user.email,
          wallet: typeof u.wallet === 'number' ? u.wallet : user.wallet ?? 0,
          avatar: (u.avatar as string | undefined) ?? user.avatar,
        });
      }
    } catch (e) {
      console.error('Refresh profile failed', e);
    }
  };

  // Check if current user is admin
  const isAdmin = user?.email?.toLowerCase().trim() === ADMIN_EMAIL.toLowerCase().trim();

  return (
    <UserContext.Provider
      value={{
        user,
        isLoggedIn: !!user,
        isAdmin,
        orders,
        wishlist,
        cart,
        login,
        register,
        logout,
        updateProfile,
        addToWishlist,
        removeFromWishlist,
        isInWishlist,
        addToCart,
        removeFromCart,
        updateCartQuantity,
        clearCart,
        refreshUserProfile,
        addOrder,
      }}
    >
      {children}
    </UserContext.Provider>
  );
}

export function useUser() {
  const context = useContext(UserContext);
  if (context === undefined) {
    throw new Error('useUser must be used within a UserProvider');
  }
  return context;
}
