import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { useUser } from '@/contexts/UserContext';
import { CartItem } from '@/types';
import { checkProductsExist } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { AlertCircle, ShoppingCart, Trash2, Plus, Minus, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useToast } from '@/hooks/use-toast';

interface CartModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const CartModal = ({ isOpen, onClose }: CartModalProps) => {
  const { cart, removeFromCart, updateCartQuantity, clearCart } = useUser();
  const [removedProducts, setRemovedProducts] = useState<Set<number>>(new Set());
  const [isChecking, setIsChecking] = useState(true);
  const { toast } = useToast();
  const navigate = useNavigate();

  // Check which products have been removed
  useEffect(() => {
    const checkRemovedProducts = async () => {
      if (cart.length === 0) {
        setIsChecking(false);
        return;
      }

      setIsChecking(true);
      try {
        const productIds = cart.map((item) => item.productId);
        const existingIds = await checkProductsExist(productIds);
        const removed = new Set<number>();
        
        productIds.forEach((id) => {
          if (!existingIds.has(id)) {
            removed.add(id);
          }
        });
        
        setRemovedProducts(removed);
      } catch (error) {
        console.error('Error checking removed products:', error);
      } finally {
        setIsChecking(false);
      }
    };

    if (isOpen) {
      checkRemovedProducts();
    }
  }, [cart, isOpen]);

  const handleRemoveItem = (productId: string) => {
    removeFromCart(productId);
    toast({
      title: 'Item removed',
      description: 'Item has been removed from your cart.',
    });
  };

  const handleQuantityChange = (productId: string, newQuantity: number) => {
    if (newQuantity <= 0) {
      handleRemoveItem(productId);
    } else {
      updateCartQuantity(productId, newQuantity);
    }
  };

  const handleRemoveRemovedItems = () => {
    const itemsToRemove = cart.filter((item) => removedProducts.has(item.productId));
    itemsToRemove.forEach((item) => {
      removeFromCart(item.productId.toString());
    });
    setRemovedProducts(new Set());
    toast({
      title: 'Removed items cleared',
      description: 'All unavailable items have been removed from your cart.',
    });
  };

  // Calculate totals - exclude removed products
  const validCartItems = cart.filter((item) => !removedProducts.has(item.productId));
  const subtotal = validCartItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const tax = subtotal * 0.1; // 10% tax
  const total = subtotal + tax;
  const removedCount = removedProducts.size;

  if (!isOpen) return null;

  return createPortal(
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-foreground/60 backdrop-blur-sm animate-fade-in"
        onClick={onClose}
      />
      
      {/* Modal - centered with max-height and overflow */}
      <div className="relative z-10 w-full max-w-4xl bg-card rounded-2xl shadow-xl border border-border animate-scale-in max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center gap-3">
            <ShoppingCart className="w-6 h-6 text-primary" />
            <h2 className="text-2xl font-display font-semibold text-foreground">
              Shopping Cart
            </h2>
            {cart.length > 0 && (
              <Badge variant="secondary" className="ml-2">
                {cart.length} item{cart.length > 1 ? 's' : ''}
              </Badge>
            )}
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-secondary transition-colors"
          >
            <X className="h-5 w-5 text-muted-foreground" />
          </button>
        </div>

        {/* Content - Scrollable */}
        <div className="flex-1 overflow-y-auto">
          {cart.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 px-6">
              <ShoppingCart className="w-24 h-24 text-muted-foreground mb-6" />
              <h3 className="text-2xl font-semibold mb-2">Your cart is empty</h3>
              <p className="text-muted-foreground mb-8 text-center">
                Start shopping to add items to your cart!
              </p>
              <Button onClick={onClose} asChild>
                <Link to="/categories">Browse Products</Link>
              </Button>
            </div>
          ) : (
            <div className="p-6 space-y-4">
              {/* Alert for removed products */}
              {removedCount > 0 && (
                <Card className="border-destructive/50 bg-destructive/10">
                  <CardContent className="pt-6">
                    <div className="flex items-start gap-3">
                      <AlertCircle className="w-5 h-5 text-destructive mt-0.5 flex-shrink-0" />
                      <div className="flex-1">
                        <p className="font-semibold text-destructive mb-1">
                          {removedCount} item{removedCount > 1 ? 's' : ''} no longer available
                        </p>
                        <p className="text-sm text-muted-foreground mb-3">
                          Some items in your cart have been removed from the store and are not included in your total.
                        </p>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={handleRemoveRemovedItems}
                          className="border-destructive text-destructive hover:bg-destructive hover:text-destructive-foreground"
                        >
                          Remove unavailable items
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )}

              {isChecking && (
                <Card>
                  <CardContent className="pt-6">
                    <p className="text-sm text-muted-foreground">Checking product availability...</p>
                  </CardContent>
                </Card>
              )}

              {/* Cart Items */}
              <div className="space-y-4">
                {cart.map((item) => {
                  const isRemoved = removedProducts.has(item.productId);
                  
                  return (
                    <Card
                      key={item.id}
                      className={isRemoved ? 'border-destructive/50 bg-destructive/5 opacity-75' : ''}
                    >
                      <CardContent className="pt-6">
                        <div className="flex gap-4">
                          {/* Product Image */}
                          <div className="relative flex-shrink-0">
                            <img
                              src={item.imageUrl || item.product?.image || '/placeholder.svg'}
                              alt={item.productName}
                              className="w-20 h-20 object-cover rounded-lg"
                            />
                            {isRemoved && (
                              <div className="absolute -top-2 -right-2 w-5 h-5 bg-destructive rounded-full flex items-center justify-center">
                                <AlertCircle className="w-3 h-3 text-destructive-foreground" />
                              </div>
                            )}
                          </div>

                          {/* Product Info */}
                          <div className="flex-1 min-w-0">
                            <div className="flex items-start justify-between gap-4">
                              <div className="flex-1 min-w-0">
                                <h3 className="font-semibold text-base mb-1 flex items-center gap-2">
                                  {item.productName}
                                  {isRemoved && (
                                    <Badge variant="destructive" className="text-xs">
                                      Removed
                                    </Badge>
                                  )}
                                </h3>
                                {isRemoved ? (
                                  <p className="text-sm text-destructive mb-2">
                                    This product is no longer available
                                  </p>
                                ) : (
                                  <p className="text-base font-semibold text-primary mb-2">
                                    ${item.price.toFixed(2)}
                                  </p>
                                )}
                              </div>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => handleRemoveItem(item.productId.toString())}
                                className="flex-shrink-0 h-8 w-8"
                              >
                                <Trash2 className="w-4 h-4" />
                              </Button>
                            </div>

                            {/* Quantity Controls */}
                            {!isRemoved && (
                              <div className="flex items-center gap-3 mt-3">
                                <span className="text-sm text-muted-foreground">Quantity:</span>
                                <div className="flex items-center gap-2 border rounded-md">
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7"
                                    onClick={() => handleQuantityChange(item.productId.toString(), item.quantity - 1)}
                                  >
                                    <Minus className="w-3 h-3" />
                                  </Button>
                                  <Input
                                    type="number"
                                    min="1"
                                    value={item.quantity}
                                    onChange={(e) => {
                                      const newQty = parseInt(e.target.value) || 1;
                                      handleQuantityChange(item.productId.toString(), newQty);
                                    }}
                                    className="w-14 h-7 text-center border-0 focus-visible:ring-0 text-sm"
                                  />
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7"
                                    onClick={() => handleQuantityChange(item.productId.toString(), item.quantity + 1)}
                                  >
                                    <Plus className="w-3 h-3" />
                                  </Button>
                                </div>
                                <span className="text-sm text-muted-foreground ml-auto">
                                  ${(item.price * item.quantity).toFixed(2)}
                                </span>
                              </div>
                            )}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Footer - Order Summary */}
        {cart.length > 0 && (
          <div className="border-t border-border bg-secondary/30 p-6">
            <div className="space-y-4">
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Subtotal ({validCartItems.length} items)</span>
                  <span>${subtotal.toFixed(2)}</span>
                </div>
                {removedCount > 0 && (
                  <div className="flex justify-between text-sm text-destructive">
                    <span className="text-muted-foreground">
                      {removedCount} unavailable item{removedCount > 1 ? 's' : ''} excluded
                    </span>
                    <span>—</span>
                  </div>
                )}
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Tax</span>
                  <span>${tax.toFixed(2)}</span>
                </div>
                <div className="border-t border-border pt-2 flex justify-between font-semibold text-lg">
                  <span>Total</span>
                  <span>${total.toFixed(2)}</span>
                </div>
              </div>

              <div className="flex gap-3">
                <Button 
                  className="flex-1" 
                  size="lg" 
                  disabled={validCartItems.length === 0}
                  onClick={() => {
                    if (validCartItems.length > 0) {
                      onClose();
                      navigate('/checkout');
                    }
                  }}
                >
                  Proceed to Checkout
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    clearCart();
                    toast({
                      title: 'Cart cleared',
                      description: 'All items have been removed from your cart.',
                    });
                  }}
                >
                  Clear
                </Button>
              </div>

              {validCartItems.length === 0 && removedCount > 0 && (
                <p className="text-sm text-center text-muted-foreground">
                  Please remove unavailable items to continue
                </p>
              )}

              <Link 
                to="/categories" 
                onClick={onClose}
                className="block text-center text-sm text-primary hover:underline"
              >
                Continue Shopping
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
};
