import { useState, useRef, useEffect } from 'react';
import { ChevronLeft, ChevronRight, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ProductCard } from '@/components/product/ProductCard';
import { ProductSkeleton } from '@/components/shared/ProductSkeleton';
import { EmptyState } from '@/components/shared/EmptyState';
import { RecommendationBadge } from './RecommendationBadge';
import { Product } from '@/types';

type RecommendationType = 'personalized' | 'trending' | 'popular' | 'similar';

interface RecommendationSectionProps {
  title: string;
  subtitle?: string;
  products: Product[];
  type: RecommendationType;
  isLoading?: boolean;
  showBadge?: boolean;
  /** Fixed categories we recommend from (ML-predetermined). Shown at top; does not grow from products. */
  predeterminedCategories?: string[];
  // Optional paging handlers: when provided, arrows can load more products
  onNextPage?: () => void;
  onPrevPage?: () => void;
  hasNextPage?: boolean;
  hasPrevPage?: boolean;
}

export function RecommendationSection({
  title,
  subtitle,
  products,
  type,
  isLoading,
  showBadge = true,
  predeterminedCategories,
  onNextPage,
  onPrevPage,
  hasNextPage,
  hasPrevPage,
}: RecommendationSectionProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(true);

  const checkScroll = () => {
    if (scrollRef.current) {
      const { scrollLeft, scrollWidth, clientWidth } = scrollRef.current;
      setCanScrollLeft(scrollLeft > 0);
      setCanScrollRight(scrollLeft < scrollWidth - clientWidth - 10);
    }
  };

  useEffect(() => {
    checkScroll();
    const ref = scrollRef.current;
    if (ref) {
      ref.addEventListener('scroll', checkScroll);
      return () => ref.removeEventListener('scroll', checkScroll);
    }
  }, [products, hasNextPage, hasPrevPage]);

  const scroll = (direction: 'left' | 'right') => {
    if (scrollRef.current) {
      const container = scrollRef.current;
      const scrollAmount = 240;

      if (direction === 'right') {
        const atRightEdge =
          container.scrollLeft >= container.scrollWidth - container.clientWidth - 10;

        if (atRightEdge && onNextPage && hasNextPage && !isLoading) {
          // Ask parent to load/append more products instead of scrolling
          onNextPage();
          return;
        }
      }

      if (direction === 'left') {
        const atLeftEdge = container.scrollLeft <= 0;
        if (atLeftEdge && onPrevPage && hasPrevPage && !isLoading) {
          onPrevPage();
          return;
        }
      }

      container.scrollBy({
        left: direction === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth',
      });
    }
  };

  const isPersonalized = type === 'personalized' || type === 'similar';

  // For personalized: show predetermined ML categories at top (fixed, does not grow from products)
  const categoryLabels = isPersonalized && predeterminedCategories && predeterminedCategories.length > 0
    ? { categories: predeterminedCategories, subcategories: [] as string[] }
    : isPersonalized && products.length > 0
      ? (() => {
          const categories = new Set<string>();
          const subcategories = new Set<string>();
          products.forEach((p) => {
            if (p.category?.trim()) categories.add(p.category.trim());
            if (p.subcategory?.trim()) subcategories.add(p.subcategory.trim());
          });
          return {
            categories: Array.from(categories),
            subcategories: Array.from(subcategories),
          };
        })()
      : null;

  return (
    <section
      className={`relative rounded-2xl overflow-hidden shadow-sm border ${
        isPersonalized
          ? 'bg-gradient-to-br from-accent/5 via-background to-primary/5 border-accent/15'
          : 'bg-card border-border/60'
      } p-6`}
    >
      {/* Decorative elements for personalized sections */}
      {isPersonalized && (
        <>
          <div className="absolute top-0 right-0 w-72 h-72 bg-accent/6 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 pointer-events-none" />
          <div className="absolute bottom-0 left-0 w-56 h-56 bg-primary/5 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2 pointer-events-none" />
        </>
      )}

      {/* Header */}
      <div className="relative flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            {isPersonalized && (
              <div className="w-9 h-9 rounded-xl bg-accent/10 flex items-center justify-center shadow-sm">
                <Sparkles className="w-4 h-4 text-accent" />
              </div>
            )}
            <h2 className="text-2xl font-display font-semibold text-foreground tracking-tight">
              {title}
            </h2>
          </div>
          {subtitle && (
            <p className="text-sm text-muted-foreground max-w-xl">{subtitle}</p>
          )}
          {categoryLabels && (categoryLabels.categories.length > 0 || categoryLabels.subcategories.length > 0) && (
            <div className="flex flex-wrap items-center gap-2 mt-3">
              {categoryLabels.categories.slice(0, 8).map((label) => (
                <span
                  key={`cat-${label}`}
                  className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-white/95 text-foreground shadow-sm border border-white/80"
                >
                  {label}
                </span>
              ))}
              {categoryLabels.subcategories.slice(0, 8).map((label) => (
                <span
                  key={`sub-${label}`}
                  className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-white/95 text-foreground shadow-sm border border-white/80"
                >
                  {label}
                </span>
              ))}
              {(categoryLabels.categories.length > 8 || categoryLabels.subcategories.length > 8) && (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-white/95 text-foreground/80 shadow-sm border border-white/80">
                  +{Math.max(0, categoryLabels.categories.length - 8) + Math.max(0, categoryLabels.subcategories.length - 8)}
                </span>
              )}
            </div>
          )}
          {showBadge && <RecommendationBadge type={type} />}
        </div>

        {/* Navigation */}
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="icon"
            onClick={() => scroll('left')}
            disabled={(!canScrollLeft && !hasPrevPage) || isLoading}
            className="w-10 h-10 rounded-full bg-card border-border/80 shadow-sm hover:bg-muted/80 transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
          </Button>
          <Button
            variant="outline"
            size="icon"
            onClick={() => scroll('right')}
            disabled={(!canScrollRight && !hasNextPage) || isLoading}
            className="w-10 h-10 rounded-full bg-card border-border/80 shadow-sm hover:bg-muted/80 transition-colors"
          >
            <ChevronRight className="w-4 h-4" />
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="relative">
        {/* Overlay loading screen that sits on top of the section while ההמלצות נטענות */}
        {isLoading && (
          <div className="absolute inset-0 z-10 flex flex-col items-center justify-center bg-background/80 backdrop-blur-sm">
            <div className="h-10 w-10 rounded-full border-2 border-accent border-t-transparent animate-spin" />
            <p className="mt-3 text-sm text-muted-foreground">
              Finding the best picks for you...
            </p>
          </div>
        )}

        {isLoading && products.length === 0 ? (
          // שלב טעינה ראשוני – שלד כרטיסים מתחת לאוברליי
          <ProductSkeleton count={5} />
        ) : products.length === 0 ? (
          <EmptyState
            title="No recommendations yet"
            description="Browse more products to get personalized suggestions"
          />
        ) : (
          <div
            ref={scrollRef}
            className="flex gap-4 overflow-x-auto scrollbar-hide py-2 px-1 -mx-1"
          >
            {products.map((product, index) => (
              <div
                key={product.id}
                className="animate-fade-in"
                style={{ animationDelay: `${index * 80}ms` }}
              >
                <ProductCard
                  product={product}
                  showQuickAdd={isPersonalized}
                  highlight={isPersonalized && index === 0}
                />
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
