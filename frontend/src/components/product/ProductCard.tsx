import { Link } from 'react-router-dom';
import { Star, ShoppingCart, Sparkles } from 'lucide-react';
import { Product } from '@/types';
import { Button } from '@/components/ui/button';
import { useUser } from '@/contexts/UserContext';
import { toast } from 'sonner';

interface ProductCardProps {
  product: Product;
  size?: 'sm' | 'md' | 'lg';
  showQuickAdd?: boolean;
  highlight?: boolean;
}

export function ProductCard({
  product,
  size = 'md',
  showQuickAdd = false,
  highlight = false,
}: ProductCardProps) {
  const { addToCart } = useUser();
  const sizeClasses = {
    sm: 'w-36',
    md: 'w-48',
    lg: 'w-64',
  };

  const imageSizes = {
    sm: 'h-36',
    md: 'h-48',
    lg: 'h-64',
  };

  const hasDiscount = product.originalPrice && product.originalPrice > product.price;
  const discountPercent = hasDiscount
    ? Math.round(((product.originalPrice! - product.price) / product.originalPrice!) * 100)
    : 0;

  return (
    <div
      className={`product-card relative ${sizeClasses[size]} flex-shrink-0 rounded-2xl bg-card border overflow-hidden group ${
        highlight
          ? 'border-accent/30 shadow-glow ring-1 ring-accent/20'
          : 'border-border/80'
      }`}
    >
      {/* Highlight badge */}
      {highlight && (
        <div className="absolute top-2.5 right-2.5 z-10 w-7 h-7 rounded-full bg-accent flex items-center justify-center shadow-md">
          <Sparkles className="w-3.5 h-3.5 text-accent-foreground" />
        </div>
      )}

      <Link to={`/product/${product.id}`} className="block">
        {/* Image Container */}
        <div className={`relative ${imageSizes[size]} overflow-hidden bg-muted/50 rounded-t-2xl`}>
          <img
            src={product.image || 'https://images.pexels.com/photos/4475708/pexels-photo-4475708.jpeg'}
            alt={product.name}
            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
            loading="lazy"
          />
          {hasDiscount && (
            <span className="absolute top-2.5 left-2.5 px-2.5 py-1 bg-sale text-accent-foreground text-xs font-semibold rounded-lg shadow-sm">
              -{discountPercent}%
            </span>
          )}
          
          {/* Quick add overlay */}
          {showQuickAdd && (
            <div className="absolute inset-0 bg-primary/60 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
              {product.inStock ? (
                <Button
                  size="sm"
                  className="bg-accent hover:bg-accent/90 text-accent-foreground transform translate-y-4 group-hover:translate-y-0 transition-transform duration-300"
                  onClick={async (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    try {
                      await addToCart(product, 1);
                      toast.success(`${product.name} added to cart`);
                    } catch (err) {
                      toast.error(err instanceof Error ? err.message : 'Failed to add to cart');
                    }
                  }}
                >
                  <ShoppingCart className="w-4 h-4 mr-2" />
                  Quick Add
                </Button>
              ) : (
                <span className="px-4 py-2 rounded-lg bg-muted text-muted-foreground text-sm font-medium transform translate-y-4 group-hover:translate-y-0 transition-transform duration-300">
                  Out of stock
                </span>
              )}
            </div>
          )}
        </div>

        {/* Content */}
        <div className="p-4">
          <h3 className="font-medium text-sm text-foreground line-clamp-2 mb-1.5 group-hover:text-accent transition-colors leading-snug">
            {product.name}
          </h3>

          {/* Rating */}
          <div className="flex items-center gap-1.5 mb-2">
            <Star className="w-3.5 h-3.5 fill-warning text-warning" />
            <span className="text-xs text-muted-foreground">
              {product.rating} ({product.reviewCount})
            </span>
          </div>

          {/* Price */}
          <div className="flex items-center gap-2">
            <span className="font-semibold text-foreground text-base">${product.price.toFixed(2)}</span>
            {hasDiscount && (
              <span className="text-xs text-muted-foreground line-through">
                ${product.originalPrice!.toFixed(2)}
              </span>
            )}
          </div>
        </div>
      </Link>
    </div>
  );
}
