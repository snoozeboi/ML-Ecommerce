import { useState, useEffect, Component, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { loadStripe, Stripe, StripeElementsOptions } from '@stripe/stripe-js';
import {
  Elements,
  CardElement,
  useStripe,
  useElements,
} from '@stripe/react-stripe-js';
import { useUser } from '@/contexts/UserContext';
import type { Order } from '@/contexts/UserContext';
import { createPaymentIntent, createGuestPaymentIntent, confirmPayment, payWithWallet, getUserPaymentMethods } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Loader2, CheckCircle2, XCircle, ArrowLeft, CreditCard } from 'lucide-react';
import { Link } from 'react-router-dom';

const stripePublishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || '';

interface SavedPaymentMethod {
  id: number;
  stripePaymentMethodId: string;
  type: string;
  last4?: string;
  brand?: string;
  isDefault: boolean;
}

function CheckoutForm() {
  const stripe = useStripe();
  const elements = useElements();
  const navigate = useNavigate();
  const { cart, user, clearCart, refreshUserProfile, addOrder } = useUser();
  const [isProcessing, setIsProcessing] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [paymentSuccess, setPaymentSuccess] = useState(false);
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [savedMethods, setSavedMethods] = useState<SavedPaymentMethod[]>([]);

  const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const tax = subtotal * 0.1;
  const total = subtotal + tax;
  // Wallet balance only available for logged-in users
  const walletBalance = user && typeof user.wallet === 'number' ? user.wallet : 0;
  const canPayWithWallet = user && total > 0 && walletBalance >= total;

  useEffect(() => {
    const createIntent = async () => {
      if (cart.length === 0) {
        navigate('/');
        return;
      }
      if (total <= 0) return;

      try {
        let response;
        
        if (user) {
          // Logged-in user: use regular payment intent
          response = await createPaymentIntent(
            parseInt(user.id),
            total,
            'usd',
            'checkout'
          );
        } else {
          // Guest checkout: use guest payment intent
          const productIds = cart.map(item => item.productId);
          response = await createGuestPaymentIntent(
            total,
            'usd',
            productIds
          );
        }

        if (response.success && response.data) {
          setClientSecret(response.data.clientSecret);
        } else {
          console.error('Payment intent creation failed:', response);
          setPaymentError(response.message || 'Failed to create payment intent');
        }
      } catch (error) {
        console.error('Payment intent creation error:', error);
        setPaymentError(error instanceof Error ? error.message : 'Failed to initialize payment');
      }
    };

    createIntent();
  }, [user, cart.length, total, navigate]);

  useEffect(() => {
    if (!user || !clientSecret) return;
    // Only load saved payment methods for logged-in users
    getUserPaymentMethods(parseInt(user.id)).then((res) => {
      if (res.success && Array.isArray(res.data)) setSavedMethods(res.data);
    });
  }, [user?.id, clientSecret]);

  const payWithSavedMethod = async (pm: SavedPaymentMethod) => {
    if (!stripe || !clientSecret || !user) return;
    setIsProcessing(true);
    setPaymentError(null);
    try {
      const { error: stripeError, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: pm.stripePaymentMethodId,
      });
      if (stripeError) {
        setPaymentError(stripeError.message || 'Payment failed');
        return;
      }
      if (paymentIntent?.status === 'succeeded') {
        const confirmResponse = await confirmPayment(paymentIntent.id);
        if (confirmResponse.success) {
          // Only add order to user context if user is logged in
          if (user) {
            addOrder(createOrderFromCart());
          }
          setPaymentSuccess(true);
          clearCart();
          setTimeout(() => navigate('/'), 3000);
        } else {
          setPaymentError(confirmResponse.message || 'Payment confirmation failed');
        }
      } else {
        setPaymentError('Payment not completed');
      }
    } catch (err) {
      setPaymentError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!stripe || !elements || !clientSecret) {
      return;
    }

    setIsProcessing(true);
    setPaymentError(null);

    const cardElement = elements.getElement(CardElement);

    if (!cardElement) {
      setPaymentError('Card element not found');
      setIsProcessing(false);
      return;
    }

    try {
      const { error: stripeError, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: {
          card: cardElement,
          billing_details: {
            name: user?.username || 'Customer',
            email: user?.email,
          },
        },
      });

      if (stripeError) {
        setPaymentError(stripeError.message || 'Payment failed');
        return;
      }

      if (paymentIntent?.status === 'succeeded') {
        const confirmResponse = await confirmPayment(paymentIntent.id);
        if (confirmResponse.success) {
          // Only add order to user context if user is logged in
          if (user) {
            addOrder(createOrderFromCart());
          }
          setPaymentSuccess(true);
          clearCart();
          setTimeout(() => navigate('/'), 3000);
        } else {
          setPaymentError(confirmResponse.message || 'Payment confirmation failed');
        }
        return;
      }

      if (paymentIntent?.status === 'requires_action') {
        setPaymentError('Additional verification was required. Please try again or use another card.');
        return;
      }

      if (paymentIntent?.status === 'requires_payment_method') {
        setPaymentError('Payment was declined. Please check your card or try another payment method.');
        return;
      }

      setPaymentError('Payment was not completed. Please try again.');
    } catch (error) {
      setPaymentError(error instanceof Error ? error.message : 'An error occurred');
    } finally {
      setIsProcessing(false);
    }
  };

  if (paymentSuccess) {
    return (
      <div className="container py-16">
        <Card className="max-w-md mx-auto">
          <CardContent className="pt-6 text-center">
            <CheckCircle2 className="w-16 h-16 text-green-500 mx-auto mb-4" />
            <h2 className="text-2xl font-bold mb-2">Payment Successful!</h2>
            <p className="text-muted-foreground mb-6">
              Your order has been processed successfully. You will be redirected shortly.
            </p>
            <Button asChild>
              <Link to="/">Return to Home</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // Guest checkout is now supported, so we don't require login
  // The payment intent creation will handle both logged-in and guest users

  if (cart.length === 0) {
    return (
      <div className="container py-16">
        <Card className="max-w-md mx-auto">
          <CardContent className="pt-6 text-center">
            <h2 className="text-2xl font-bold mb-2">Your cart is empty</h2>
            <p className="text-muted-foreground mb-6">
              Add items to your cart before checkout.
            </p>
            <Button asChild>
              <Link to="/">Continue Shopping</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const cardElementOptions = {
    hidePostalCode: true,
    style: {
      base: {
        fontSize: '16px',
        color: '#424770',
        '::placeholder': {
          color: '#aab7c4',
        },
      },
      invalid: {
        color: '#9e2146',
      },
    },
  };

  const savedCardLabel = (pm: SavedPaymentMethod) => {
    const brand = (pm.brand || 'Card').charAt(0).toUpperCase() + (pm.brand || '').slice(1).toLowerCase();
    const last4 = pm.last4 || '****';
    return `${brand} **** ${last4}`;
  };

  const createOrderFromCart = (): Order => ({
    id: `order-${Date.now()}`,
    items: cart.map((item) => ({
      product: {
        id: String(item.productId),
        name: item.productName,
        description: '',
        price: item.price,
        image: item.imageUrl || '',
        category: '',
        rating: 0,
        reviewCount: 0,
        inStock: true,
      },
      quantity: item.quantity,
    })),
    total,
    status: 'processing',
    date: new Date().toLocaleDateString(),
  });

  const handlePayWithWallet = async () => {
    if (!user || total <= 0 || !canPayWithWallet) return;
    setIsProcessing(true);
    setPaymentError(null);
    try {
      const res = await payWithWallet(parseInt(user.id), total);
      if (res.success) {
        addOrder(createOrderFromCart());
        await clearCart();
        await refreshUserProfile();
        setPaymentSuccess(true);
        setTimeout(() => navigate('/'), 3000);
      } else {
        setPaymentError(res.message || 'Payment failed');
      }
    } catch (err) {
      setPaymentError(err instanceof Error ? err.message : 'Payment failed');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="container py-8 max-w-5xl">
      <div className="mb-6">
        <Button variant="ghost" asChild>
          <Link to="/cart" className="text-muted-foreground hover:text-foreground">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Cart
          </Link>
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:gap-10">
        {/* Payment Form */}
        <div className="lg:col-span-2">
          <Card className="border-border shadow-sm">
            <CardHeader className="pb-4">
              <CardTitle className="text-xl">Payment</CardTitle>
              {user && (
                <div className="mt-3 p-3 rounded-lg bg-muted/50">
                  <p className="text-sm text-muted-foreground">
                    Wallet balance: <span className="font-semibold text-foreground">${walletBalance.toFixed(2)}</span>
                    {walletBalance < total && (
                      <span className="ml-2">
                        · <Link to="/wallet" className="text-primary hover:underline font-medium">Add funds</Link>
                      </span>
                    )}
                  </p>
                </div>
              )}
            </CardHeader>
            <CardContent className="space-y-6 pt-0">
              {user && canPayWithWallet && (
                <Button
                  type="button"
                  className="w-full"
                  size="lg"
                  disabled={isProcessing}
                  onClick={handlePayWithWallet}
                >
                  {isProcessing ? (
                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" /> Processing...</>
                  ) : (
                    `Pay $${total.toFixed(2)} from wallet`
                  )}
                </Button>
              )}
              {user && !canPayWithWallet && walletBalance > 0 && (
                <p className="text-sm text-muted-foreground">
                  Wallet balance (${walletBalance.toFixed(2)}) is less than total. Pay with card below or <Link to="/wallet" className="text-primary hover:underline">add funds</Link>.
                </p>
              )}
              {user && walletBalance === 0 && (
                <p className="text-sm text-muted-foreground">
                  No wallet balance. <Link to="/wallet" className="text-primary hover:underline">Add funds</Link> or pay with card below.
                </p>
              )}
              <div className="relative py-2">
                <div className="absolute inset-0 flex items-center">
                  <span className="w-full border-t border-border" />
                </div>
                <div className="relative flex justify-center text-xs uppercase tracking-wider">
                  <span className="bg-card px-3 text-muted-foreground">Or pay with card</span>
                </div>
              </div>
              {savedMethods.length > 0 && (
                <div className="space-y-3">
                  <p className="text-sm font-medium text-foreground">Use saved card</p>
                  <div className="space-y-2">
                    {savedMethods.map((pm) => (
                      <Button
                        key={pm.id}
                        type="button"
                        variant="outline"
                        className="w-full justify-start h-auto py-3 px-4"
                        disabled={isProcessing}
                        onClick={() => payWithSavedMethod(pm)}
                      >
                        <CreditCard className="w-4 h-4 mr-3 text-muted-foreground shrink-0" />
                        <span>{savedCardLabel(pm)}</span>
                        {pm.isDefault && (
                          <Badge variant="secondary" className="ml-2 text-xs">Default</Badge>
                        )}
                      </Button>
                    ))}
                  </div>
                  <div className="relative py-2">
                    <div className="absolute inset-0 flex items-center">
                      <span className="w-full border-t border-border" />
                    </div>
                    <div className="relative flex justify-center text-xs uppercase tracking-wider">
                      <span className="bg-card px-2 text-muted-foreground">Or new card</span>
                    </div>
                  </div>
                </div>
              )}
              <form onSubmit={handleSubmit} className="space-y-6">
                <div>
                  <label className="text-sm font-medium mb-2 block text-foreground">
                    {savedMethods.length > 0 ? 'New card details' : 'Card details'}
                  </label>
                  <div className="border border-input rounded-lg p-4 bg-background focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2">
                    <CardElement options={cardElementOptions} />
                  </div>
                </div>

                {paymentError && (
                  <Alert variant="destructive" className="py-3">
                    <XCircle className="h-4 w-4" />
                    <AlertDescription>{paymentError}</AlertDescription>
                  </Alert>
                )}

                <Button
                  type="submit"
                  className="w-full h-12 text-base"
                  size="lg"
                  disabled={!stripe || isProcessing || !clientSecret}
                >
                  {isProcessing ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      Processing...
                    </>
                  ) : (
                    `Pay $${total.toFixed(2)}`
                  )}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>

        {/* Order Summary */}
        <div className="lg:col-span-1">
          <Card className="sticky top-24 border-border shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-lg">Order Summary</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 pt-0">
              <div className="space-y-2">
                {cart.map((item) => (
                  <div key={item.id} className="flex justify-between text-sm">
                    <span className="text-muted-foreground">
                      {item.productName} x{item.quantity}
                    </span>
                    <span>${(item.price * item.quantity).toFixed(2)}</span>
                  </div>
                ))}
              </div>

              <div className="border-t pt-4 space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Subtotal</span>
                  <span>${subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Tax</span>
                  <span>${tax.toFixed(2)}</span>
                </div>
                <div className="border-t pt-2 flex justify-between font-semibold text-lg">
                  <span>Total</span>
                  <span>${total.toFixed(2)}</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

/** Catches any error in checkout (e.g. payment flow) so the page never goes blank */
class CheckoutErrorBoundary extends Component<
  { children: ReactNode },
  { hasError: boolean; message: string }
> {
  state = { hasError: false, message: '' };

  static getDerivedStateFromError(error: unknown) {
    return {
      hasError: true,
      message: error instanceof Error ? error.message : 'Something went wrong',
    };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="container py-16">
          <Card className="max-w-md mx-auto">
            <CardContent className="pt-6 text-center space-y-4">
              <XCircle className="w-12 h-12 text-destructive mx-auto" />
              <h2 className="text-xl font-semibold">Checkout error</h2>
              <p className="text-sm text-muted-foreground">{this.state.message}</p>
              <p className="text-sm text-muted-foreground">
                Your cart was not charged. You can go back and try again or contact support.
              </p>
              <div className="flex flex-wrap gap-3 justify-center pt-2">
                <Button asChild variant="outline">
                  <Link to="/cart">Back to Cart</Link>
                </Button>
                <Button asChild variant="outline">
                  <Link to="/">Home</Link>
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      );
    }
    return this.props.children;
  }
}

export default function Checkout() {
  const { cart } = useUser();
  const [stripeInstance, setStripeInstance] = useState<Stripe | null>(null);
  const [stripeError, setStripeError] = useState<string | null>(null);

  const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const total = subtotal + subtotal * 0.1;
  const amountInCents = Math.round(total * 100);

  useEffect(() => {
    if (!stripePublishableKey || !stripePublishableKey.startsWith('pk_')) {
      setStripeError('Payment is not configured. Set VITE_STRIPE_PUBLISHABLE_KEY in .env');
      return;
    }
    let cancelled = false;
    loadStripe(stripePublishableKey)
      .then((stripe) => {
        if (!cancelled && stripe) setStripeInstance(stripe);
      })
      .catch((err) => {
        if (!cancelled) setStripeError(err?.message || 'Failed to load payment');
      });
    return () => { cancelled = true; };
  }, []);

  const options: StripeElementsOptions = {
    mode: 'payment',
    amount: amountInCents > 0 ? amountInCents : 100,
    currency: 'usd',
  };

  if (!stripeError && !stripeInstance) {
    return (
      <div className="container py-16 flex flex-col items-center justify-center min-h-[50vh]">
        <Loader2 className="w-10 h-10 animate-spin text-primary mb-4" />
        <p className="text-muted-foreground">Loading checkout...</p>
      </div>
    );
  }

  if (stripeError) {
    return (
      <CheckoutFallback message={stripeError} />
    );
  }

  return (
    <CheckoutErrorBoundary>
      <Elements stripe={stripeInstance!} options={options}>
        <CheckoutForm />
      </Elements>
    </CheckoutErrorBoundary>
  );
}

/** Shown when Stripe is not available – order summary + message, no blank page */
function CheckoutFallback({ message }: { message: string }) {
  const { cart, user } = useUser();
  const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const tax = subtotal * 0.1;
  const total = subtotal + tax;

  if (!user) {
    return (
      <div className="container py-16">
        <Card className="max-w-md mx-auto">
          <CardContent className="pt-6 text-center">
            <p className="text-muted-foreground mb-6">Please log in to proceed.</p>
            <Button asChild><Link to="/">Go to Home</Link></Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (cart.length === 0) {
    return (
      <div className="container py-16">
        <Card className="max-w-md mx-auto">
          <CardContent className="pt-6 text-center">
            <p className="text-muted-foreground mb-6">Your cart is empty.</p>
            <Button asChild><Link to="/categories">Continue Shopping</Link></Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container py-8">
      <div className="mb-6">
        <Button variant="ghost" asChild>
          <Link to="/cart"><ArrowLeft className="w-4 h-4 mr-2" /> Back to Cart</Link>
        </Button>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CreditCard className="w-5 h-5" />
              Payment
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Alert variant="destructive">
              <AlertDescription>{message}</AlertDescription>
            </Alert>
            <p className="text-sm text-muted-foreground mt-4">
              You can review your order below. Configure Stripe in the project to enable payments.
            </p>
          </CardContent>
        </Card>
        <div className="lg:col-span-1">
          <Card className="sticky top-24">
            <CardHeader><CardTitle>Order Summary</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              {cart.map((item) => (
                <div key={item.id} className="flex justify-between text-sm">
                  <span className="text-muted-foreground">{item.productName} x{item.quantity}</span>
                  <span>${(item.price * item.quantity).toFixed(2)}</span>
                </div>
              ))}
              <div className="border-t pt-4 space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Subtotal</span>
                  <span>${subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Tax</span>
                  <span>${tax.toFixed(2)}</span>
                </div>
                <div className="border-t pt-2 flex justify-between font-semibold text-lg">
                  <span>Total</span>
                  <span>${total.toFixed(2)}</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
