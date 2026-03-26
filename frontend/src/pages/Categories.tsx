import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Product, FilterOptions, Category } from '@/types';
import { getCategories, getMaxPriceForCategory, getCategoryProducts } from '@/services/api';
import { ProductGrid } from '@/components/product';
import { Filters } from '@/components/filters';
import { Button } from '@/components/ui/button';
import { Filter } from 'lucide-react';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';

const Categories = () => {
  const [searchParams] = useSearchParams();
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  // Load 50 products per page instead of 100 for faster responses
  const pageSize = 50;
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const [maxPrice, setMaxPrice] = useState<number>(500);
  // Global "preferred" price range chosen by the user (last slider commit)
  const [basePriceRange, setBasePriceRange] = useState<[number, number]>([0, 500]);
  // Remember a preferred price range per category (including "all")
  const [savedPriceRanges, setSavedPriceRanges] = useState<Record<string, [number, number]>>({
    all: [0, 500],
  });
  const [filters, setFilters] = useState<FilterOptions>({
    sortBy: 'rating',
    priceRange: [0, 500],
    categories: [],
    inStockOnly: false,
  });

  useEffect(() => {
    const fetchCategories = async () => {
      const res = await getCategories();
      setCategories(res.data);
    };
    fetchCategories();
  }, []);

  // Fetch max price for the current category (or "all" when no category is active)
  useEffect(() => {
    const fetchMaxPrice = async () => {
      try {
        const categoryKey = activeCategory ?? 'all';
        const value = await getMaxPriceForCategory(categoryKey);
        setMaxPrice(value);
        // Start from the remembered range for this category (if any),
        // otherwise from the global base range, then clamp to [min, max]
        // with min <= max <= value.
        const remembered = savedPriceRanges[categoryKey];
        let [min, max] = remembered ?? basePriceRange;
        const limit = value || max || 0;

        if (max > limit) max = limit;
        if (min > max) min = max;

        setFilters((prev) => {
          return {
            ...prev,
            priceRange: [min, max],
          };
        });
        // Also persist the clamped range for this category
        setSavedPriceRanges((prev) => ({
          ...prev,
          [categoryKey]: [min, max],
        }));
      } catch {
        // On failure, keep previous maxPrice and filters
      }
    };
    fetchMaxPrice();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeCategory]);

  // Fetch products from backend when filters, category, sort, or page changes.
  // Sorting & pagination are handled by the backend via /api/products/list.
  useEffect(() => {
    const fetchProducts = async () => {
      setIsLoading(true);
      try {
        const res = await getCategoryProducts({
          page: currentPage,
          size: pageSize,
          category: activeCategory ?? 'all',
          minPrice: filters.priceRange?.[0],
          maxPrice: filters.priceRange?.[1],
          sort: filters.sortBy,
        });

        if (res.success && res.data) {
          setProducts(res.data.products);
          setTotalElements(res.data.totalElements);
          setTotalPages(res.data.totalPages);
        } else {
          setProducts([]);
          setTotalElements(0);
          setTotalPages(0);
        }
      } finally {
        setIsLoading(false);
      }
    };

    fetchProducts();
  }, [activeCategory, filters.priceRange, filters.sortBy, currentPage, pageSize]);

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
      // Scroll to top whenever page changes so user starts at the beginning
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleFilterChange = (newFilters: FilterOptions) => {
    setFilters(newFilters);
    // Persist this price range as the preferred one for the current category
    const categoryKey = activeCategory ?? 'all';
    setBasePriceRange(newFilters.priceRange);
    setSavedPriceRanges((prev) => ({
      ...prev,
      [categoryKey]: newFilters.priceRange,
    }));
    // When filters change, jump back to the first page
    setCurrentPage(0);
  };

  const clearFilters = () => {
    const categoryKey = activeCategory ?? 'all';
    const resetRange: [number, number] = [0, maxPrice];

    setBasePriceRange(resetRange);
    setFilters({
      sortBy: 'rating',
      priceRange: resetRange,
      categories: [],
      inStockOnly: false,
    });
    setSavedPriceRanges((prev) => ({
      ...prev,
      [categoryKey]: resetRange,
    }));
    setActiveCategory(null);
  };

  return (
    <div className="container py-8">
      {/* Category Tabs */}
      <div className="mb-8 overflow-x-auto category-scroll">
        <div className="flex gap-2 min-w-max pb-2 bg-muted/60 border border-border/60 rounded-2xl px-3 py-3 backdrop-blur-sm">
          <Button
            variant={activeCategory === null ? 'default' : 'outline'}
            size="sm"
            onClick={() => setActiveCategory(null)}
            className="rounded-full"
          >
            All
          </Button>
          {categories.map((cat) => (
            <Button
              key={cat.id}
              variant={activeCategory === cat.name ? 'default' : 'outline'}
              size="sm"
              onClick={() => setActiveCategory(cat.name)}
              className="rounded-full"
            >
              {cat.name}
            </Button>
          ))}
        </div>
      </div>

      {/* Subcategories dropdown indicator */}
      {activeCategory && (
        <div className="mb-6 animate-fade-in">
          <p className="text-sm text-muted-foreground mb-2">Subcategories:</p>
          <div className="flex flex-wrap gap-2">
            {categories
              .find((c) => c.name === activeCategory)
              ?.subcategories.map((sub) => (
                <Button key={sub} variant="ghost" size="sm" className="text-sm">
                  {sub}
                </Button>
              ))}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_280px] gap-8">
        {/* Products Grid */}
        <div>
          <div className="flex items-center justify-between mb-6">
            <p className="text-sm text-muted-foreground">
              {totalElements || products.length} products found
            </p>
            {/* Mobile Filter Button */}
            <Sheet>
              <SheetTrigger asChild>
                <Button variant="outline" size="sm" className="lg:hidden">
                  <Filter className="w-4 h-4 mr-2" />
                  Filters
                </Button>
              </SheetTrigger>
              <SheetContent side="right" className="w-[300px]">
                <SheetHeader>
                  <SheetTitle>Filters</SheetTitle>
                </SheetHeader>
                <div className="mt-6">
                  <Filters
                    filters={filters}
                    onFilterChange={handleFilterChange}
                    maxPrice={maxPrice}
                  />
                </div>
              </SheetContent>
            </Sheet>
          </div>

          <ProductGrid
            products={products}
            isLoading={isLoading}
            onClearFilters={clearFilters}
          />

          {/* Pagination Controls */}
          {!isLoading && totalPages > 1 && (
            <div className="flex items-center justify-between mt-6">
              <div className="text-sm text-muted-foreground">
                Showing {currentPage * pageSize + 1} to{' '}
                {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} products
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0}
                >
                  Previous
                </Button>
                <div className="flex items-center gap-1">
                  {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                    let pageNum: number;
                    if (totalPages <= 5) {
                      pageNum = i;
                    } else if (currentPage < 3) {
                      pageNum = i;
                    } else if (currentPage > totalPages - 3) {
                      pageNum = totalPages - 5 + i;
                    } else {
                      pageNum = currentPage - 2 + i;
                    }
                    return (
                      <Button
                        key={pageNum}
                        variant={currentPage === pageNum ? 'default' : 'outline'}
                        size="sm"
                        onClick={() => handlePageChange(pageNum)}
                        className="min-w-[40px]"
                      >
                        {pageNum + 1}
                      </Button>
                    );
                  })}
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </div>

        {/* Desktop Filters Sidebar */}
        <aside className="hidden lg:block">
          <div className="sticky top-24">
            <h2 className="font-semibold mb-4 text-foreground">Filter By</h2>
            <Filters
              filters={filters}
              onFilterChange={handleFilterChange}
              maxPrice={maxPrice}
            />
          </div>
        </aside>
      </div>
    </div>
  );
};

export default Categories;
