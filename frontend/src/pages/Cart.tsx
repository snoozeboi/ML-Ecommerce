import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Cart() {
  const navigate = useNavigate();

  // Redirect to home since cart is now a modal
  useEffect(() => {
    navigate('/', { replace: true });
  }, [navigate]);

  return null;

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

    checkRemovedProducts();
  }, [cart]);

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

  if (cart.length === 0) {
    return (
      <div className="container py-16">
        <div className="max-w-md mx-auto text-center">
          <ShoppingCart className="w-24 h-24 mx-auto text-muted-foreground mb-6" />
          <h1 className="text-3xl font-bold mb-4">Your cart is empty</h1>
          <p className="text-muted-foreground mb-8">
            Start shopping to add items to your cart!
          </p>
          <Button asChild>
            <Link to="/categories">Browse Products</Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container py-8">
      <h1 className="text-3xl font-bold mb-6">Shopping Cart</h1>

      {/* Alert for removed products */}
      {removedCount > 0 && (
        <Card className="mb-6 border-destructive/50 bg-destructive/10">
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Cart Items */}
        <div className="lg:col-span-2 space-y-4">
          {isChecking && (
            <Card>
              <CardContent className="pt-6">
                <p className="text-sm text-muted-foreground">Checking product availability...</p>
              </CardContent>
            </Card>
          )}

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
                        className="w-24 h-24 object-cover rounded-lg"
                      />
                      {isRemoved && (
                        <div className="absolute -top-2 -right-2 w-6 h-6 bg-destructive rounded-full flex items-center justify-center">
                          <AlertCircle className="w-4 h-4 text-destructive-foreground" />
                        </div>
                      )}
                    </div>

                    {/* Product Info */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex-1 min-w-0">
                          <h3 className="font-semibold text-lg mb-1 flex items-center gap-2">
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
                            <p className="text-lg font-semibold text-primary mb-2">
                              ${item.price.toFixed(2)}
                            </p>
                          )}
                        </div>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleRemoveItem(item.productId.toString())}
                          className="flex-shrink-0"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>

                      {/* Quantity Controls */}
                      {!isRemoved && (
                        <div className="flex items-center gap-3 mt-4">
                          <span className="text-sm text-muted-foreground">Quantity:</span>
                          <div className="flex items-center gap-2 border rounded-md">
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
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
                              className="w-16 h-8 text-center border-0 focus-visible:ring-0"
                            />
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
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

        {/* Order Summary */}
        <div className="lg:col-span-1">
          <Card className="sticky top-24">
            <CardHeader>
              <CardTitle>Order Summary</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
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
                <div className="border-t pt-2 flex justify-between font-semibold text-lg">
                  <span>Total</span>
                  <span>${total.toFixed(2)}</span>
                </div>
              </div>

              <Button 
                className="w-full" 
                size="lg" 
                disabled={validCartItems.length === 0}
                asChild
              >
                <Link to="/checkout">Proceed to Checkout</Link>
              </Button>

              {validCartItems.length === 0 && removedCount > 0 && (
                <p className="text-sm text-center text-muted-foreground">
                  Please remove unavailable items to continue
                </p>
              )}

              <Button
                variant="outline"
                className="w-full"
                onClick={() => {
                  clearCart();
                  toast({
                    title: 'Cart cleared',
                    description: 'All items have been removed from your cart.',
                  });
                }}
              >
                Clear Cart
              </Button>

              <Link to="/categories" className="block text-center text-sm text-primary hover:underline">
                Continue Shopping
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
