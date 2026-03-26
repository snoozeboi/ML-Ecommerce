import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { Product } from '@/types';
import { getProductById, getSimilarItems, recordProductViewViaRecommendations } from '@/services/api';
import { RecommendationSection } from '@/components/recommendations';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Star, ShoppingCart, AlertCircle, ChevronLeft, Minus, Plus, Heart } from 'lucide-react';
import { useUser } from '@/contexts/UserContext';
import { toast } from 'sonner';

const SIMILAR_PAGE_SIZE = 5;
const SIMILAR_MAX = 30;

const ProductDetails = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToCart, addToWishlist, isInWishlist, user } = useUser();
  const [product, setProduct] = useState<Product | null>(null);
  const [addingToCart, setAddingToCart] = useState(false);
  const [buyingNow, setBuyingNow] = useState(false);
  const [similarAll, setSimilarAll] = useState<Product[]>([]);
  const [similarLoadedCount, setSimilarLoadedCount] = useState(SIMILAR_PAGE_SIZE);
  const [selectedImage, setSelectedImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [similarLoading, setSimilarLoading] = useState(true);

  // Always start product page at the top when navigated to (e.g. from search)
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  useEffect(() => {
    const fetchData = async () => {
      if (!id) return;
      setIsLoading(true);
      setSimilarLoading(true);
      try {
        const productRes = await getProductById(id);
        setProduct(productRes.data);
        setSelectedImage(0);
        setQuantity(1);
        setIsLoading(false);

        const similarRes = await getSimilarItems(id, SIMILAR_MAX);
        const list = (similarRes.data.products || []).slice(0, SIMILAR_MAX);
        setSimilarAll(list);
        setSimilarLoadedCount(SIMILAR_PAGE_SIZE);
      } finally {
        setSimilarLoading(false);
      }
    };
    fetchData();
  }, [id]);

  const similar = similarAll.slice(0, similarLoadedCount);
  const similarHasNext = similarLoadedCount < Math.min(similarAll.length, SIMILAR_MAX);

  const handleSimilarNext = () => {
    if (!similarHasNext) return;
    const next = Math.min(similarLoadedCount + SIMILAR_PAGE_SIZE, similarAll.length, SIMILAR_MAX);
    setSimilarLoadedCount(next);
  };

  // Record view so it's saved to DB (user_view_history, products.views) and used for recommendations later
  useEffect(() => {
    if (!id || !product) return;
    const userId = user?.id != null ? Number(user.id) : undefined;
    recordProductViewViaRecommendations(id, userId).catch(() => {});
  }, [id, product?.id, user?.id]);

  if (isLoading) {
    return (
      <div className="container py-8">
        <Skeleton className="h-6 w-32 mb-6" />
        <div className="grid grid-cols-1 md:grid-cols-[1fr_minmax(280px,380px)] gap-8">
          <div className="order-1 space-y-4">
            <Skeleton className="h-10 w-3/4" />
            <Skeleton className="h-8 w-1/4" />
            <Skeleton className="h-6 w-1/3" />
            <Skeleton className="h-32 w-full" />
            <div className="flex gap-4">
              <Skeleton className="h-12 w-36" />
              <Skeleton className="h-12 w-28" />
            </div>
          </div>
          <div className="order-2">
            <Skeleton className="aspect-square rounded-xl max-w-sm" />
            <div className="flex gap-2 mt-4">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="w-20 h-20 rounded-lg" />
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="container py-16 text-center">
        <h1 className="text-2xl font-semibold mb-4">Product not found</h1>
        <Button asChild>
          <Link to="/categories">
            <ChevronLeft className="w-4 h-4 mr-2" />
            Back to Categories
          </Link>
        </Button>
      </div>
    );
  }

  const fallbackImage = product.image || 'https://images.pexels.com/photos/4475708/pexels-photo-4475708.jpeg';
  const images = (product.images?.length ? product.images : [product.image || fallbackImage]).filter(Boolean) as string[];
  const displayImages = images.length ? images : [fallbackImage];
  const hasDiscount = product.originalPrice && product.originalPrice > product.price;

  return (
    <div className="container py-8">
      {/* Back Link */}
      <Link
        to="/categories"
        className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-6"
      >
        <ChevronLeft className="w-4 h-4 mr-1" />
        Back to Categories
      </Link>

      {/* Product Info */}
      <div className="grid grid-cols-1 md:grid-cols-[1fr_minmax(280px,380px)] gap-8 mb-12">
        {/* Left: name, description, price, actions */}
        <div className="order-1 min-w-0">
          <div className="flex flex-wrap items-center gap-3 mb-4">
            <h1 className="text-3xl font-display font-semibold text-foreground">
              {product.name}
            </h1>
            {(product.category?.trim() || product.subcategory?.trim()) && (
              <div className="flex flex-wrap items-center gap-2">
                {product.category?.trim() && (
                  <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-white/95 text-foreground shadow-sm border border-white/80 dark:bg-card dark:border-border">
                    {product.category.trim()}
                  </span>
                )}
                {product.subcategory?.trim() && (
                  <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-accent/15 text-accent-foreground border border-accent/25 dark:bg-accent/20">
                    {product.subcategory.trim()}
                  </span>
                )}
              </div>
            )}
          </div>

          {/* Price */}
          <div className="flex items-baseline gap-3 mb-4">
            <span className="text-3xl font-bold text-foreground">
              ${product.price.toFixed(2)}
            </span>
            {hasDiscount && (
              <span className="text-lg text-muted-foreground line-through">
                ${product.originalPrice!.toFixed(2)}
              </span>
            )}
            {hasDiscount && (
              <span className="px-2 py-1 bg-sale text-accent-foreground text-sm font-semibold rounded">
                Save ${(product.originalPrice! - product.price).toFixed(2)}
              </span>
            )}
          </div>

          {/* Rating */}
          <div className="flex items-center gap-2 mb-6">
            <div className="flex items-center gap-1">
              {[...Array(5)].map((_, i) => (
                <Star
                  key={i}
                  className={`w-5 h-5 ${
                    i < Math.floor(product.rating)
                      ? 'fill-warning text-warning'
                      : 'text-muted'
                  }`}
                />
              ))}
            </div>
            <span className="text-sm text-muted-foreground">
              {product.rating} ({product.reviewCount} reviews)
            </span>
          </div>

          {/* Description */}
          <div className="mb-8">
            <h2 className="font-semibold mb-2 text-foreground">Description</h2>
            <p className="text-muted-foreground leading-relaxed">
              {product.description}
            </p>
          </div>

          {/* Quantity */}
          <div className="flex items-center gap-3 mb-6">
            <span className="text-sm font-medium text-foreground">Quantity</span>
            <div className="flex items-center border border-border rounded-lg">
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="h-10 w-10 rounded-r-none"
                disabled={quantity <= 1}
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
              >
                <Minus className="w-4 h-4" />
              </Button>
              <span className="w-12 text-center font-medium tabular-nums">{quantity}</span>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="h-10 w-10 rounded-l-none"
                disabled={quantity >= 99}
                onClick={() => setQuantity((q) => Math.min(99, q + 1))}
              >
                <Plus className="w-4 h-4" />
              </Button>
            </div>
          </div>

          {/* Actions */}
          <div className="flex flex-wrap items-center gap-4 mb-8">
            {!product.inStock && (
              <span className="px-4 py-2 rounded-lg bg-muted text-muted-foreground font-medium">
                Out of stock
              </span>
            )}
            <Button 
              size="lg" 
              className="bg-accent hover:bg-accent/90 text-accent-foreground"
              disabled={!product.inStock || addingToCart}
              onClick={async () => {
                setAddingToCart(true);
                try {
                  await addToCart(product, quantity);
                  toast.success(
                    quantity === 1
                      ? `${product.name} added to cart`
                      : `${quantity} × ${product.name} added to cart`
                  );
                } catch (err) {
                  toast.error(err instanceof Error ? err.message : 'Failed to add to cart');
                } finally {
                  setAddingToCart(false);
                }
              }}
            >
              <ShoppingCart className="w-5 h-5 mr-2" />
              {addingToCart ? 'Adding...' : 'Add to Cart'}
            </Button>
            <Button 
              size="lg" 
              variant="outline"
              disabled={!product.inStock || buyingNow}
              onClick={async () => {
                if (!product) return;
                setBuyingNow(true);
                try {
                  await addToCart(product, quantity);
                  navigate('/checkout');
                } catch (err) {
                  toast.error(err instanceof Error ? err.message : 'Failed to proceed to checkout');
                  setBuyingNow(false);
                }
              }}
            >
              {buyingNow ? 'Processing...' : 'Buy Now'}
            </Button>
            <Button 
              size="lg" 
              variant="outline"
              disabled={!product.inStock}
              onClick={() => addToWishlist(product)}
              className={isInWishlist(product.id) ? 'border-accent text-accent' : ''}
            >
              <Heart className={`w-4 h-5 mr-2 ${isInWishlist(product.id) ? 'fill-accent' : ''}`} />
              {isInWishlist(product.id) ? 'In Wishlist' : 'Add to Wishlist'}
            </Button>
          </div>

          {/* Report Link */}
          <Link
            to="/support"
            className="inline-flex items-center text-sm text-accent hover:underline"
          >
            <AlertCircle className="w-4 h-4 mr-1" />
            Report a problem
          </Link>
        </div>

        {/* Right: image */}
        <div className="order-2 flex flex-col items-center md:items-end">
          <div className="w-full max-w-sm aspect-square rounded-xl overflow-hidden bg-secondary mb-4 shrink-0">
            <img
              src={displayImages[selectedImage] ?? fallbackImage}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          </div>

          {/* Thumbnails */}
          {displayImages.length > 1 && (
            <div className="flex gap-2">
              {displayImages.map((img, index) => (
                <button
                  key={index}
                  onClick={() => setSelectedImage(index)}
                  className={`w-20 h-20 rounded-lg overflow-hidden border-2 transition-all ${
                    index === selectedImage
                      ? 'border-accent'
                      : 'border-transparent hover:border-border'
                  }`}
                >
                  <img
                    src={img}
                    alt={`${product.name} ${index + 1}`}
                    className="w-full h-full object-cover"
                  />
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Similar Items - same category/subcategory, load more up to 30 (like homepage recommendation block) */}
      <section className="border-t border-border pt-8">
        <RecommendationSection
          title="Similar Items"
          subtitle="Products in the same category — load more to see up to 30"
          products={similar}
          type="similar"
          isLoading={similarLoading}
          onNextPage={handleSimilarNext}
          hasNextPage={similarHasNext}
        />
      </section>
    </div>
  );
};

export default ProductDetails;
