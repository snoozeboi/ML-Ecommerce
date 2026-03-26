import { Product } from '@/types';
import { ProductCard } from './ProductCard';
import { ProductSkeleton } from '@/components/shared/ProductSkeleton';
import { EmptyState } from '@/components/shared/EmptyState';
import { Search } from 'lucide-react';

interface ProductGridProps {
  products: Product[];
  isLoading?: boolean;
  columns?: 2 | 3 | 4;
  onClearFilters?: () => void;
}

// Fixed gap (gap-4 = 1rem); fixed column width so gap never grows; grid centered so extra space on sides.
// Column = 16rem when room, or 100% when single column so cards don't overlap on narrow screens.
const GRID_BASE_CLASS = 'grid gap-4 justify-center';
const GRID_TEMPLATE_COLUMNS = 'repeat(auto-fill, min(16rem, 100%))';

export function ProductGrid({
  products,
  isLoading,
  columns = 3,
  onClearFilters,
}: ProductGridProps) {
  if (isLoading) {
    return (
      <div
        className={GRID_BASE_CLASS}
        style={{ gridTemplateColumns: GRID_TEMPLATE_COLUMNS }}
      >
        {[...Array(6)].map((_, i) => (
          <div
            key={i}
            className="aspect-[3/4] rounded-xl bg-muted animate-pulse"
          />
        ))}
      </div>
    );
  }

  if (products.length === 0) {
    return (
      <EmptyState
        icon={Search}
        title="No products found"
        description="Try adjusting your filters or search terms to find what you're looking for"
        action={onClearFilters ? { label: 'Clear Filters', onClick: onClearFilters } : undefined}
      />
    );
  }

  return (
    <div
      className={GRID_BASE_CLASS}
      style={{ gridTemplateColumns: GRID_TEMPLATE_COLUMNS }}
    >
      {products.map((product, index) => (
        <div
          key={product.id}
          className="min-w-0 w-full animate-fade-in [&_.product-card]:max-w-full"
          style={{ animationDelay: `${index * 50}ms` }}
        >
          <ProductCard product={product} size="lg" />
        </div>
      ))}
    </div>
  );
}
