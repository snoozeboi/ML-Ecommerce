import { useEffect, useState } from 'react';
import { Product, FilterOptions, Category } from '@/types';
import { getProducts, getCategories } from '@/services/api';
import { ProductGrid } from '@/components/product';
import { Filters } from '@/components/filters';
import { Button } from '@/components/ui/button';
import { Filter, Tag } from 'lucide-react';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';

const OnSale = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [allOnSaleProducts, setAllOnSaleProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filters, setFilters] = useState<FilterOptions>({
    sortBy: 'rating',
    priceRange: [0, 500],
    categories: [],
    inStockOnly: false,
  });

  // Fetch all categories for the filter dropdown
  useEffect(() => {
    const fetchCategories = async () => {
      const res = await getCategories();
      setCategories(res.data);
    };
    fetchCategories();
  }, []);

  // Fetch all products and filter for on-sale items
  useEffect(() => {
    const fetchProducts = async () => {
      setIsLoading(true);
      // Get all products without category filter first
      const res = await getProducts({
        sortBy: 'rating',
        priceRange: [0, 500],
        categories: [],
        inStockOnly: false,
      });
      
      // Filter to show only products on sale (have originalPrice and it's greater than price)
      const onSaleProducts = res.data.filter(
        (product) => product.originalPrice && product.originalPrice > product.price
      );
      
      setAllOnSaleProducts(onSaleProducts);
      setIsLoading(false);
    };
    fetchProducts();
  }, []);

  // Apply filters to on-sale products
  useEffect(() => {
    setIsLoading(true);
    
    let filtered = [...allOnSaleProducts];
    
    // Apply category filter if selected
    if (filters.categories && filters.categories.length > 0) {
      filtered = filtered.filter(p => filters.categories!.includes(p.category));
    }
    
    // Apply price filter
    if (filters.priceRange) {
      filtered = filtered.filter(
        p => p.price >= filters.priceRange[0] && p.price <= filters.priceRange[1]
      );
    }
    
    // Apply in stock filter
    if (filters.inStockOnly) {
      filtered = filtered.filter(p => p.inStock);
    }
    
    // Apply sorting
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
    
    setProducts(filtered);
    setIsLoading(false);
  }, [filters, allOnSaleProducts]);

  const handleFilterChange = (newFilters: FilterOptions) => {
    setFilters(newFilters);
  };

  const clearFilters = () => {
    setFilters({
      sortBy: 'rating',
      priceRange: [0, 500],
      categories: [],
      inStockOnly: false,
    });
  };

  // Get all available categories from the categories API for filter options
  const categoryNames = categories.map(c => c.name);

  return (
    <div className="w-full max-w-[1600px] mx-auto px-4 md:px-8 xl:px-12 py-10">
      {/* Header – card style with accent */}
      <div className="mb-10 rounded-2xl border border-accent/15 bg-gradient-to-br from-accent/5 via-background to-background p-6 shadow-sm">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 rounded-xl bg-accent/10 flex items-center justify-center shadow-sm">
            <Tag className="w-5 h-5 text-accent" />
          </div>
          <h1 className="text-3xl font-display font-bold text-foreground tracking-tight">On Sale</h1>
        </div>
        <p className="text-muted-foreground max-w-xl">
          Discover amazing deals{filters.categories && filters.categories.length > 0
            ? ` in ${filters.categories.join(', ')}`
            : ' on all categories'}
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-10">
        {/* Products Grid */}
        <div>
          <div className="flex items-center justify-between mb-6">
            <p className="text-sm text-muted-foreground">
              {products.length} product{products.length !== 1 ? 's' : ''} on sale
              {filters.categories && filters.categories.length > 0 && (
                <span className="ml-1">in selected {filters.categories.length === 1 ? 'category' : 'categories'}</span>
              )}
            </p>
            {/* Mobile Filter Button */}
            <Sheet>
              <SheetTrigger asChild>
                <Button variant="outline" size="sm" className="lg:hidden rounded-xl shadow-sm">
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
                    categories={categoryNames}
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
        </div>

        {/* Desktop Filters Sidebar – card style */}
        <aside className="hidden lg:block">
          <div className="sticky top-24 rounded-2xl border border-border/60 bg-card p-6 shadow-sm">
            <h2 className="font-semibold text-foreground mb-5 pb-3 border-b border-border/60">Filter By</h2>
            <Filters
              filters={filters}
              onFilterChange={handleFilterChange}
              categories={categoryNames}
            />
          </div>
        </aside>
      </div>
    </div>
  );
};

export default OnSale;
