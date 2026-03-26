import { useState, useEffect, useRef, useCallback } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Search, ShoppingCart, User, Menu, X, Shield, Wallet, Loader2, Star } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { AuthModal } from '@/components/auth';
import { CartModal } from '@/components/cart';
import { useUser } from '@/contexts/UserContext';
import { getSearchSuggestions } from '@/services/api';
import type { ProductSuggestionDto } from '@/types';

const navItems = [
  { label: 'Home', path: '/' },
  { label: 'Categories', path: '/categories' },
  { label: 'Support', path: '/support' },
  { label: 'On Sale', path: '/on-sale' },
];

const SUGGEST_DEBOUNCE_MS = 300;
const MIN_QUERY_LENGTH = 2;

export function Navbar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isLoggedIn, isAdmin, cart } = useUser();
  const cartCount = cart?.length ?? 0;
  const [searchQuery, setSearchQuery] = useState('');
  const [suggestions, setSuggestions] = useState<ProductSuggestionDto[] | null>(null);
  const [suggestLoading, setSuggestLoading] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const suggestCloseTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [isCartOpen, setIsCartOpen] = useState(false);

  // Debounced live suggest
  useEffect(() => {
    const q = searchQuery.trim();
    if (q.length < MIN_QUERY_LENGTH) {
      setSuggestions(null);
      return;
    }
    const t = setTimeout(() => {
      setSuggestLoading(true);
      getSearchSuggestions(q)
        .then((res) => setSuggestions(res.suggestions || []))
        .catch(() => setSuggestions([]))
        .finally(() => setSuggestLoading(false));
    }, SUGGEST_DEBOUNCE_MS);
    return () => clearTimeout(t);
  }, [searchQuery]);

  const openSuggestions = useCallback(() => {
    if (suggestCloseTimerRef.current) {
      clearTimeout(suggestCloseTimerRef.current);
      suggestCloseTimerRef.current = null;
    }
    setShowSuggestions(true);
  }, []);

  const closeSuggestions = useCallback(() => {
    suggestCloseTimerRef.current = setTimeout(() => setShowSuggestions(false), 150);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      setShowSuggestions(false);
      navigate(`/categories?search=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  const handleSelectSuggestion = (s: ProductSuggestionDto) => {
    setShowSuggestions(false);
    setSearchQuery('');
    setSuggestions(null);
    setIsMenuOpen(false);
    navigate(`/product/${s.id}`);
  };

  const showDropdown = showSuggestions && searchQuery.trim().length >= MIN_QUERY_LENGTH;
  const renderSuggestDropdown = () => {
    if (!showDropdown) return null;
    return (
      <div className="absolute top-full left-0 right-0 mt-1 rounded-xl border border-border bg-card shadow-lg z-[100] max-h-[min(70vh,320px)] overflow-y-auto">
        {suggestLoading ? (
          <div className="flex items-center justify-center gap-2 py-6 text-muted-foreground">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span className="text-sm">Searching...</span>
          </div>
        ) : suggestions && suggestions.length > 0 ? (
          <ul className="py-1">
            {suggestions.map((s) => (
              <li key={s.id}>
                <button
                  type="button"
                  onClick={() => handleSelectSuggestion(s)}
                  className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-muted/80 transition-colors"
                >
                  {s.imageUrl ? (
                    <img src={s.imageUrl} alt="" className="w-10 h-10 rounded-lg object-cover shrink-0" />
                  ) : (
                    <div className="w-10 h-10 rounded-lg bg-muted shrink-0" />
                  )}
                  <div className="min-w-0 flex-1 flex flex-col gap-0.5">
                    <div className="flex justify-between items-start gap-2">
                      <p className="text-sm font-medium truncate">{s.name}</p>
                      <div className="shrink-0 text-sm font-medium text-right">
                        {s.discountPercent != null && s.discountPercent > 0 ? (
                          <>
                            <span className="text-muted-foreground line-through mr-1">${s.price.toFixed(2)}</span>
                            <span className="text-accent">${s.finalPrice.toFixed(2)}</span>
                          </>
                        ) : (
                          <span>${s.finalPrice.toFixed(2)}</span>
                        )}
                      </div>
                    </div>
                    <div className="flex justify-between items-center gap-2">
                      <p className="text-xs text-muted-foreground">{s.category}</p>
                      {s.rating != null && (
                        <div className="flex items-center gap-1 shrink-0">
                          <div className="flex">
                            {[...Array(5)].map((_, i) => (
                              <Star
                                key={i}
                                strokeWidth={0}
                                className={`w-3 h-3 shrink-0 ${
                                  i < Math.floor(s.rating!)
                                    ? 'fill-amber-400 text-amber-400'
                                    : 'fill-gray-300 text-gray-300'
                                }`}
                              />
                            ))}
                          </div>
                          <span className="text-xs text-muted-foreground">{s.rating}</span>
                        </div>
                      )}
                    </div>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="py-4 px-3 text-sm text-muted-foreground text-center">No matches</p>
        )}
      </div>
    );
  };

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path.split('?')[0]);
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border bg-card/95 backdrop-blur-md supports-[backdrop-filter]:bg-card/90 shadow-sm">
      <div className="w-full max-w-[1600px] mx-auto px-4 md:px-8 xl:px-12 flex h-20 items-center justify-between gap-4">
        {/* User Section – card with accent border */}
        <div className="flex items-center gap-2 min-w-0">
          <div
            className="flex items-center gap-2 pl-1 pr-2.5 py-1.5 rounded-xl border border-border bg-card cursor-pointer hover:bg-muted/50 hover:border-accent/30 transition-all duration-200 shadow-sm border-l-4 border-l-accent"
            onClick={() => {
              if (isLoggedIn) {
                navigate('/profile');
              } else {
                setAuthMode("login");
                setIsAuthOpen(true);
              }
            }}
          >
            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-muted to-muted/80 flex items-center justify-center overflow-hidden ring-2 ring-white shadow-inner shrink-0">
              {user?.avatar ? (
                <img src={user.avatar} alt={user.username} className="w-full h-full object-cover" />
              ) : (
                <User className="w-4 h-4 text-muted-foreground" />
              )}
            </div>
            {isLoggedIn ? (
              <div className="hidden sm:block min-w-0">
                <p className="text-sm font-semibold text-foreground truncate">{user?.username}</p>
                <Link
                  to="/wallet"
                  className="flex items-center gap-1 text-xs text-muted-foreground hover:text-accent transition-colors"
                  onClick={(e) => e.stopPropagation()}
                >
                  <Wallet className="w-3.5 h-3.5 shrink-0" />
                  <span>${typeof user?.wallet === 'number' ? user.wallet.toFixed(2) : '0.00'}</span>
                  {(typeof user?.wallet !== 'number' ? 0 : user.wallet) === 0 && <span className="text-accent">· Add funds</span>}
                </Link>
              </div>
            ) : (
              <div className="hidden sm:flex items-center gap-1.5">
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 px-2 text-xs rounded-md"
                  onClick={(e) => {
                    e.stopPropagation();
                    setAuthMode("login");
                    setIsAuthOpen(true);
                  }}
                >
                  Login
                </Button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setAuthMode("register");
                    setIsAuthOpen(true);
                  }}
                  className="h-7 px-2 text-xs font-medium rounded-md bg-accent/10 text-accent hover:bg-accent hover:text-accent-foreground transition-colors"
                >
                  Register
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Desktop Navigation – compact pill tabs, visible from md so they fit on smaller widths */}
        <nav className="hidden md:flex items-center gap-1 p-1 rounded-xl bg-muted/60 border border-border/60">
          {navItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`px-2.5 py-1.5 rounded-lg text-xs font-medium transition-all duration-200 whitespace-nowrap ${
                isActive(item.path)
                  ? 'bg-accent text-accent-foreground shadow-sm'
                  : 'text-foreground/80 hover:text-foreground hover:bg-background/80'
              }`}
            >
              {item.label}
            </Link>
          ))}
          {isAdmin && (
            <Link
              to="/admin"
              className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-medium transition-all duration-200 ${
                isActive('/admin')
                  ? 'bg-accent text-accent-foreground shadow-sm'
                  : 'text-foreground/80 hover:text-foreground hover:bg-background/80'
              }`}
            >
              <Shield className="w-3.5 h-3.5" />
              Admin
            </Link>
          )}
        </nav>

        {/* Right Section - Search, Cart */}
        <div className="flex items-center gap-3">
          {/* Search with live suggestions */}
          <form onSubmit={handleSearch} className="hidden md:flex items-center gap-2">
            <div className="relative w-48 lg:w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none z-10" />
              <Input
                type="search"
                placeholder="Search..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={openSuggestions}
                onBlur={closeSuggestions}
                className="pl-9 w-full rounded-xl border-border/80"
              />
              {renderSuggestDropdown()}
            </div>
          </form>

          {/* Cart with item count bubble */}
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setIsCartOpen(true)}
            className="relative"
          >
            <ShoppingCart className="w-5 h-5" />
            {cartCount > 0 && (
              <span
                className="absolute -top-0.5 -right-0.5 min-w-[1.25rem] h-5 px-1 flex items-center justify-center rounded-full bg-accent text-accent-foreground text-xs font-semibold"
                aria-label={`${cartCount} items in cart`}
              >
                {cartCount > 99 ? '99+' : cartCount}
              </span>
            )}
          </Button>

          {/* Mobile Menu Toggle */}
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
          >
            {isMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </Button>
        </div>
      </div>

      {/* Mobile Menu */}
      {isMenuOpen && (
        <div className="md:hidden border-t border-border bg-card animate-fade-in">
          <nav className="w-full max-w-[1600px] mx-auto px-4 md:px-8 py-4 flex flex-col gap-2">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setIsMenuOpen(false)}
                className={`px-4 py-2 rounded-lg transition-colors ${
                  isActive(item.path)
                    ? 'bg-accent/10 text-accent font-medium'
                    : 'hover:bg-secondary'
                }`}
              >
                {item.label}
              </Link>
            ))}
            {isAdmin && (
              <Link
                to="/admin"
                onClick={() => setIsMenuOpen(false)}
                className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
                  isActive('/admin')
                    ? 'bg-accent/10 text-accent font-medium'
                    : 'hover:bg-secondary'
                }`}
              >
                <Shield className="w-4 h-4" />
                Admin
              </Link>
            )}
            <form onSubmit={handleSearch} className="mt-2 px-4">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none z-10" />
                <Input
                  type="search"
                  placeholder="Search products..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onFocus={openSuggestions}
                  onBlur={closeSuggestions}
                  className="pl-9"
                />
                {renderSuggestDropdown()}
              </div>
            </form>
          </nav>
        </div>
      )}

      {/* Auth Modal */}
      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
        initialMode={authMode}
      />

      {/* Cart Modal */}
      <CartModal
        isOpen={isCartOpen}
        onClose={() => setIsCartOpen(false)}
      />
    </header>
  );
}
