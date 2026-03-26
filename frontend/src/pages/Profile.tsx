import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { User, Package, Heart, Settings, LogOut, Camera, Trash2, CreditCard } from 'lucide-react';
import { useUser } from '@/contexts/UserContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { toast } from 'sonner';
import { PaymentMethodsManager } from '@/components/payment/PaymentMethodsManager';

export default function Profile() {
  const { user, isLoggedIn, isAdmin, orders, wishlist, addToCart, updateProfile, removeFromWishlist, logout } = useUser();
  const [username, setUsername] = useState(user?.username || '');
  const [email, setEmail] = useState(user?.email || '');

  if (!isLoggedIn) {
    return <Navigate to="/" replace />;
  }

  const handleSaveProfile = async () => {
    try {
      // Admin cannot change email, so only send username for admin
      const updates = isAdmin ? { username } : { username, email };
      await updateProfile(updates);
      toast.success('Profile updated successfully!');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to update profile';
      toast.error(errorMessage);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'delivered':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
      case 'shipped':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
      case 'processing':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200';
      default:
        return 'bg-muted text-muted-foreground';
    }
  };

  return (
    <div className="container py-8">
      <div className="max-w-4xl mx-auto">
        {/* Profile Header */}
        <div className="flex items-center gap-6 mb-8 p-6 bg-card rounded-2xl border border-border">
          <div className="relative">
            <div className="w-24 h-24 rounded-full bg-secondary flex items-center justify-center">
              {user?.avatar ? (
                <img src={user.avatar} alt={user.username} className="w-full h-full rounded-full object-cover" />
              ) : (
                <User className="w-10 h-10 text-muted-foreground" />
              )}
            </div>
            <button className="absolute bottom-0 right-0 w-8 h-8 rounded-full bg-accent text-accent-foreground flex items-center justify-center hover:bg-accent/90 transition-colors">
              <Camera className="w-4 h-4" />
            </button>
          </div>
          <div className="flex-1">
            <h1 className="text-2xl font-display font-semibold">{user?.username}</h1>
            <p className="text-muted-foreground">{user?.email}</p>
            <div className="mt-2 flex items-center gap-2">
              <Badge variant="secondary" className="text-sm">
                Wallet: ${user?.wallet?.toFixed(2) || '0.00'}
              </Badge>
            </div>
          </div>
          <Button variant="outline" className="gap-2" onClick={logout}>
            <LogOut className="w-4 h-4" />
            Logout
          </Button>
        </div>

        {/* Tabs */}
        <Tabs defaultValue="settings" className="space-y-6">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="settings" className="gap-2">
              <Settings className="w-4 h-4" />
              <span className="hidden sm:inline">Settings</span>
            </TabsTrigger>
            <TabsTrigger value="payment-methods" className="gap-2">
              <CreditCard className="w-4 h-4" />
              <span className="hidden sm:inline">Payment Methods</span>
            </TabsTrigger>
            <TabsTrigger value="orders" className="gap-2">
              <Package className="w-4 h-4" />
              <span className="hidden sm:inline">Orders</span>
            </TabsTrigger>
            <TabsTrigger value="wishlist" className="gap-2">
              <Heart className="w-4 h-4" />
              <span className="hidden sm:inline">Wishlist</span>
            </TabsTrigger>
          </TabsList>

          {/* Settings Tab */}
          <TabsContent value="settings">
            <Card>
              <CardHeader>
                <CardTitle>Profile Settings</CardTitle>
                <CardDescription>Update your account information</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="username">Username</Label>
                    <Input
                      id="username"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      placeholder="Your username"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email">Email</Label>
                    <Input
                      id="email"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="your@email.com"
                      disabled={isAdmin}
                      className={isAdmin ? "bg-muted cursor-not-allowed" : ""}
                    />
                    {isAdmin && (
                      <p className="text-xs text-muted-foreground">
                        Admin email cannot be changed for security reasons
                      </p>
                    )}
                  </div>
                </div>
                <Button onClick={handleSaveProfile} className="bg-accent hover:bg-accent/90 text-accent-foreground">
                  Save Changes
                </Button>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Payment Methods Tab */}
          <TabsContent value="payment-methods">
            {user && <PaymentMethodsManager userId={parseInt(user.id)} />}
          </TabsContent>

          {/* Orders Tab */}
          <TabsContent value="orders">
            <Card>
              <CardHeader>
                <CardTitle>Order History</CardTitle>
                <CardDescription>View your past orders and their status</CardDescription>
              </CardHeader>
              <CardContent>
                {orders.length === 0 ? (
                  <div className="text-center py-12">
                    <Package className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
                    <h3 className="text-lg font-medium mb-2">No orders yet</h3>
                    <p className="text-muted-foreground">Start shopping to see your orders here!</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {orders.map((order) => (
                      <div
                        key={order.id}
                        className="p-4 border border-border rounded-xl hover:bg-secondary/50 transition-colors"
                      >
                        <div className="flex items-center justify-between mb-3">
                          <div>
                            <p className="font-medium">{order.id}</p>
                            <p className="text-sm text-muted-foreground">{order.date}</p>
                          </div>
                          <Badge className={getStatusColor(order.status)}>{order.status}</Badge>
                        </div>
                        <div className="flex items-center gap-4">
                          {order.items.map((item) => (
                            <div key={item.product.id} className="flex items-center gap-3">
                              <img
                                src={item.product.image || 'https://images.pexels.com/photos/4475708/pexels-photo-4475708.jpeg'}
                                alt={item.product.name}
                                className="w-12 h-12 rounded-lg object-cover"
                              />
                              <div>
                                <p className="text-sm font-medium">{item.product.name}</p>
                                <p className="text-xs text-muted-foreground">Qty: {item.quantity}</p>
                              </div>
                            </div>
                          ))}
                        </div>
                        <div className="mt-3 pt-3 border-t border-border flex justify-between items-center">
                          <span className="text-sm text-muted-foreground">Total</span>
                          <span className="font-semibold">${order.total.toFixed(2)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          {/* Wishlist Tab */}
          <TabsContent value="wishlist">
            <Card>
              <CardHeader>
                <CardTitle>My Wishlist</CardTitle>
                <CardDescription>Items you've saved for later</CardDescription>
              </CardHeader>
              <CardContent>
                {wishlist.length === 0 ? (
                  <div className="text-center py-12">
                    <Heart className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
                    <h3 className="text-lg font-medium mb-2">Your wishlist is empty</h3>
                    <p className="text-muted-foreground">Save items you love to find them easily later!</p>
                  </div>
                ) : (
                  <div className="grid gap-4 sm:grid-cols-2">
                    {wishlist.map((product) => (
                      <div
                        key={product.id}
                        className="flex gap-4 p-4 border border-border rounded-xl hover:bg-secondary/50 transition-colors"
                      >
                        <img
                          src={product.image || 'https://images.pexels.com/photos/4475708/pexels-photo-4475708.jpeg'}
                          alt={product.name}
                          className="w-20 h-20 rounded-lg object-cover"
                        />
                        <div className="flex-1 min-w-0">
                          <h4 className="font-medium truncate">{product.name}</h4>
                          <p className="text-accent font-semibold">${product.price.toFixed(2)}</p>
                          <div className="mt-2 flex items-center gap-2">
                            {product.inStock ? (
                              <Button
                                size="sm"
                                className="bg-accent hover:bg-accent/90 text-accent-foreground"
                                onClick={async () => {
                                  try {
                                    await addToCart(product, 1);
                                    toast.success(`${product.name} added to cart`);
                                  } catch (err) {
                                    toast.error(err instanceof Error ? err.message : 'Failed to add to cart');
                                  }
                                }}
                              >
                                Add to Cart
                              </Button>
                            ) : (
                              <span className="px-3 py-1.5 text-sm text-muted-foreground rounded-md bg-muted">
                                Out of stock
                              </span>
                            )}
                            <Button
                              size="sm"
                              variant="ghost"
                              className="text-destructive hover:text-destructive"
                              onClick={() => removeFromWishlist(product.id)}
                            >
                              <Trash2 className="w-4 h-4" />
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
