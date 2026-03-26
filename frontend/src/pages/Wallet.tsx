import { useState, useEffect } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { useUser } from '@/contexts/UserContext';
import { createPaymentIntent, confirmPayment, getUserPaymentMethods } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Loader2, CheckCircle2, XCircle, Wallet as WalletIcon, CreditCard } from 'lucide-react';
import { Link } from 'react-router-dom';

const stripePublishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || '';

interface SavedPaymentMethod {
  id: number;
  stripePaymentMethodId: string;
  type: string;
  last4?: string;
  brand?: string;
  expMonth?: number;
  expYear?: number;
  isDefault: boolean;
}

function AddFundsForm({ amount, onSuccess }: { amount: number; onSuccess: () => void }) {
  const stripe = useStripe();
  const elements = useElements();
  const { user, refreshUserProfile } = useUser();
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [savedMethods, setSavedMethods] = useState<SavedPaymentMethod[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [processing, setProcessing] = useState(false);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!user || amount <= 0) return;
    createPaymentIntent(parseInt(user.id), amount, 'usd', 'top_up').then((res) => {
      if (res.success && res.data?.clientSecret) setClientSecret(res.data.clientSecret);
      else setError(res.message || 'Failed to create payment');
    });
    getUserPaymentMethods(parseInt(user.id)).then((res) => {
      if (res.success && Array.isArray(res.data)) setSavedMethods(res.data);
    });
  }, [user, amount]);

  const payWithSavedMethod = async (pm: SavedPaymentMethod) => {
    if (!stripe || !clientSecret || !user) return;
    setProcessing(true);
    setError(null);
    try {
      const { error: stripeError, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: pm.stripePaymentMethodId,
      });
      if (stripeError) {
        const msg = stripeError.message || 'Payment failed';
        setError(msg);
        return;
      }
      if (paymentIntent?.status === 'succeeded') {
        const confirmRes = await confirmPayment(paymentIntent.id);
        if (confirmRes.success) {
          await refreshUserProfile();
          setSuccess(true);
          onSuccess();
        } else setError(confirmRes.message || 'Confirmation failed');
      } else setError('Payment not completed');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setProcessing(false);
    }
  };

  const isCustomerAttachmentError = (msg: string) =>
    /Customer attachment|cannot be used again|attach to a Customer/i.test(msg);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stripe || !elements || !clientSecret || !user) return;
    setProcessing(true);
    setError(null);
    try {
      const cardEl = elements.getElement(CardElement);
      if (!cardEl) {
        setError('Card element not found');
        return;
      }
      const { error: stripeError, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: { card: cardEl, billing_details: { name: user.username || 'Customer', email: user.email } },
      });
      if (stripeError) {
        setError(stripeError.message || 'Payment failed');
        return;
      }
      if (paymentIntent?.status === 'succeeded') {
        const confirmRes = await confirmPayment(paymentIntent.id);
        if (confirmRes.success) {
          await refreshUserProfile();
          setSuccess(true);
          onSuccess();
        } else setError(confirmRes.message || 'Confirmation failed');
      } else setError('Payment not completed');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setProcessing(false);
    }
  };

  if (success) {
    return (
      <Card>
        <CardContent className="pt-6 text-center">
          <CheckCircle2 className="w-12 h-12 text-green-500 mx-auto mb-4" />
          <h3 className="font-semibold text-lg mb-2">Funds added</h3>
          <p className="text-muted-foreground text-sm mb-4">${amount.toFixed(2)} was added to your wallet.</p>
          <Button asChild><Link to="/checkout">Back to Checkout</Link></Button>
        </CardContent>
      </Card>
    );
  }

  if (!clientSecret) {
    return (
      <Card>
        <CardContent className="pt-6">
          {error ? (
            <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>
          ) : (
            <p className="text-sm text-muted-foreground flex items-center gap-2">
              <Loader2 className="w-4 h-4 animate-spin" /> Preparing payment...
            </p>
          )}
        </CardContent>
      </Card>
    );
  }

  const label = (pm: SavedPaymentMethod) => {
    const brand = (pm.brand || 'Card').charAt(0).toUpperCase() + (pm.brand || '').slice(1).toLowerCase();
    const last4 = pm.last4 || '****';
    return `${brand} **** ${last4}`;
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Pay ${amount.toFixed(2)}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {savedMethods.length > 0 && (
          <div>
            <p className="text-sm font-medium mb-2 text-foreground">Use saved card</p>
            <div className="space-y-2">
              {savedMethods.map((pm) => (
                <Button
                  key={pm.id}
                  type="button"
                  variant="outline"
                  className="w-full justify-start h-auto py-3 px-4"
                  disabled={processing}
                  onClick={() => payWithSavedMethod(pm)}
                >
                  <CreditCard className="w-4 h-4 mr-3 text-muted-foreground" />
                  <span>{label(pm)}</span>
                  {pm.isDefault && (
                    <Badge variant="secondary" className="ml-2 text-xs">Default</Badge>
                  )}
                </Button>
              ))}
            </div>
            <div className="relative my-4">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t border-border" />
              </div>
              <div className="relative flex justify-center text-xs uppercase tracking-wider">
                <span className="bg-card px-2 text-muted-foreground">Or use a new card</span>
              </div>
            </div>
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-sm font-medium mb-2 block text-foreground">
              {savedMethods.length > 0 ? 'New card details' : 'Card details'}
            </label>
            <div className="border border-input rounded-lg p-4 bg-background">
              <CardElement
                options={{
                  hidePostalCode: true, // Hide ZIP / postal code field
                  style: { base: { fontSize: '16px', color: '#424770' }, invalid: { color: '#9e2146' } },
                }}
              />
            </div>
          </div>
          {error && (
            <Alert variant="destructive">
              <XCircle className="h-4 w-4" />
              <AlertDescription>
                {error}
                {isCustomerAttachmentError(error) && (
                  <span className="block mt-2 text-sm opacity-90">
                    Use a new card below or add a new saved card; previously saved cards may not work for adding funds.
                  </span>
                )}
              </AlertDescription>
            </Alert>
          )}
          <Button type="submit" className="w-full" size="lg" disabled={!stripe || processing}>
            {processing ? <><Loader2 className="w-4 h-4 mr-2 animate-spin" /> Processing...</> : `Pay $${amount.toFixed(2)}`}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

export default function Wallet() {
  const { user } = useUser();
  const [amount, setAmount] = useState<string>('50');
  const [showForm, setShowForm] = useState(false);
  const numAmount = parseFloat(amount) || 0;
  const walletBalance = typeof user?.wallet === 'number' ? user.wallet : 0;

  if (!user) {
    return (
      <div className="container py-16">
        <Card className="max-w-md mx-auto">
          <CardContent className="pt-6 text-center">
            <p className="text-muted-foreground mb-6">Please log in to view your wallet.</p>
            <Button asChild><Link to="/">Home</Link></Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const stripePromise = stripePublishableKey.startsWith('pk_') ? loadStripe(stripePublishableKey) : null;
  const canAddFunds = stripePublishableKey.startsWith('pk_');

  return (
    <div className="container py-8">
      <div className="max-w-md mx-auto space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <WalletIcon className="w-5 h-5" />
              Wallet
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">${walletBalance.toFixed(2)}</p>
            <p className="text-sm text-muted-foreground mt-1">Available balance</p>
          </CardContent>
        </Card>

        {!showForm ? (
          <Card>
            <CardHeader>
              <CardTitle>Add funds</CardTitle>
              <p className="text-sm text-muted-foreground">Add money to your wallet to pay at checkout.</p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="text-sm font-medium mb-2 block">Amount ($)</label>
                <Input
                  type="number"
                  min="1"
                  step="1"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="50"
                />
              </div>
              {!canAddFunds ? (
                <Alert variant="destructive">
                  <AlertDescription>Payment is not configured. Set VITE_STRIPE_PUBLISHABLE_KEY to add funds.</AlertDescription>
                </Alert>
              ) : (
                <Button
                  className="w-full"
                  size="lg"
                  disabled={numAmount < 1}
                  onClick={() => numAmount >= 1 && setShowForm(true)}
                >
                  Add ${numAmount >= 1 ? numAmount.toFixed(2) : '0.00'}
                </Button>
              )}
            </CardContent>
          </Card>
        ) : stripePromise && numAmount >= 1 ? (
          <>
            <Elements
              stripe={stripePromise}
              options={{ mode: 'payment', amount: Math.round(numAmount * 100), currency: 'usd' }}
            >
              <AddFundsForm
                amount={numAmount}
                onSuccess={() => setShowForm(false)}
              />
            </Elements>
            <Button variant="ghost" className="w-full" onClick={() => setShowForm(false)}>Cancel</Button>
          </>
        ) : null}
      </div>
    </div>
  );
}
