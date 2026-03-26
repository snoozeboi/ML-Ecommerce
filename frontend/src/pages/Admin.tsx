import { useEffect, useState, useRef, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUser } from '@/contexts/UserContext';
import { Product } from '@/types';
import { getProductsPaginated, createProduct, updateProduct, deleteProduct, getAllProductCategories } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { useToast } from '@/hooks/use-toast';
import { Plus, Edit, Trash2, Package, Eye, EyeOff, Upload, ChevronLeft, ChevronRight, Search } from 'lucide-react';

interface ProductFormData {
  productName: string;
  description: string;
  category: string;
  subCategory: string;
  price: number;
  quantity: number;
  imageUrl: string;
  tags: string;
}

export default function Admin() {
  const { user, isAdmin } = useUser();
  const navigate = useNavigate();
  const { toast } = useToast();
  
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [deletingProduct, setDeletingProduct] = useState<Product | null>(null);
  
  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [hasPrevious, setHasPrevious] = useState(false);
  const pageSize = 100;
  const [formData, setFormData] = useState<ProductFormData>({
    productName: '',
    description: '',
    category: '',
    subCategory: '',
    price: 0,
    quantity: 1, // Default to 1 so products are purchasable
    imageUrl: '',
    tags: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showImagePreview, setShowImagePreview] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeSearchQuery, setActiveSearchQuery] = useState(''); // The actual search query used for API calls

  // When user picks "Type my own" we need to show the text input; Select alone doesn't allow typing
  const [categoryChoiceMode, setCategoryChoiceMode] = useState<'empty' | 'list' | 'custom'>('empty');
  const [subCategoryChoiceMode, setSubCategoryChoiceMode] = useState<'empty' | 'list' | 'custom'>('empty');

  // All categories/subcategories in the DB (fetched once for edit dropdowns)
  const [allCategories, setAllCategories] = useState<string[]>([]);
  const [allSubcategories, setAllSubcategories] = useState<string[]>([]);

  // Merge API categories with current page products so dropdown always has options (API may fail or return empty)
  const dropdownCategories = useMemo(() => {
    const set = new Set(allCategories);
    products.forEach((p) => {
      const c = (p.category ?? '').trim();
      if (c) set.add(c);
    });
    return Array.from(set).sort();
  }, [allCategories, products]);
  const dropdownSubcategories = useMemo(() => {
    const set = new Set(allSubcategories);
    products.forEach((p) => {
      const s = (p.subcategory ?? '').trim();
      if (s) set.add(s);
    });
    return Array.from(set).sort();
  }, [allSubcategories, products]);

  // Redirect if not admin
  useEffect(() => {
    if (!isAdmin) {
      toast({
        title: 'Access Denied',
        description: 'You must be an admin to access this page.',
        variant: 'destructive',
      });
      navigate('/');
    }
  }, [isAdmin, navigate, toast]);

  // Fetch all categories/subcategories from DB for edit dropdowns (not just current page)
  useEffect(() => {
    if (!isAdmin) return;
    getAllProductCategories().then((res) => {
      if (res.success && res.data) {
        setAllCategories(res.data.categories);
        setAllSubcategories(res.data.subcategories);
      }
    });
  }, [isAdmin]);

  // Define fetchProducts with useCallback to prevent unnecessary re-renders
  const fetchProducts = useCallback(async (page: number = 0, search: string = '') => {
    setIsLoading(true);
    try {
      console.log('Admin: Fetching products, page:', page, 'search:', search, 'user email:', user?.email);
      const response = await getProductsPaginated(page, pageSize, user?.email, search);
      console.log('Admin: Products response:', response);
      if (response.success && response.data) {
        setProducts(response.data.products);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setHasNext(response.data.hasNext);
        setHasPrevious(response.data.hasPrevious);
        console.log('Admin: Loaded', response.data.products.length, 'products, total:', response.data.totalElements);
      } else {
        console.error('Admin: Failed to fetch products:', response.message);
        toast({
          title: 'Error',
          description: response.message || 'Failed to fetch products.',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('Admin: Error fetching products:', error);
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'Failed to fetch products.',
        variant: 'destructive',
      });
    } finally {
      setIsLoading(false);
    }
  }, [user?.email, pageSize, toast]);

  // Reset to first page when active search query changes
  useEffect(() => {
    if (activeSearchQuery.trim() && currentPage !== 0) {
      setCurrentPage(0);
    }
  }, [activeSearchQuery]);

  // Fetch products when active search query or page changes
  useEffect(() => {
    if (isAdmin) {
      fetchProducts(currentPage, activeSearchQuery);
    }
  }, [isAdmin, currentPage, activeSearchQuery, fetchProducts]);

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  const handleOpenCreateDialog = () => {
    setEditingProduct(null);
    setFormData({
      productName: '',
      description: '',
      category: '',
      subCategory: '',
      price: 0,
      quantity: 1, // Default to 1 so products are purchasable
      imageUrl: '',
      tags: '',
    });
    setCategoryChoiceMode('empty');
    setSubCategoryChoiceMode('empty');
    setShowImagePreview(false);
    setIsDialogOpen(true);
  };

  const handleOpenEditDialog = async (product: Product) => {
    setEditingProduct(product);
    // Fetch full product details: list API returns ProductSummaryDto (no description), so we need GET /products/:id for description, quantity, etc.
    let quantity = 1;
    let fullProduct: Record<string, unknown> | null = null;
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/products/${product.id}`);
      if (response.ok) {
        fullProduct = await response.json();
        quantity = (fullProduct?.quantity as number) > 0 ? (fullProduct.quantity as number) : 1;
      }
    } catch (error) {
      console.warn('Could not fetch full product, using list data only:', error);
    }
    const desc = fullProduct?.description != null ? String(fullProduct.description) : (product.description ?? '');
    const cat = fullProduct?.category != null ? String(fullProduct.category) : (product.category ?? '');
    const subCat = fullProduct?.subCategory != null ? String(fullProduct.subCategory) : (product.subcategory ?? '');
    const img = fullProduct?.imageUrl ?? fullProduct?.image_url ?? product.image ?? '';
    const tagsArr = fullProduct?.tags as string[] | undefined;
    const tagsStr = Array.isArray(tagsArr) ? tagsArr.join(', ') : (product.tags?.join(', ') ?? '');
    setFormData({
      productName: (fullProduct?.productName ?? fullProduct?.name ?? product.name) as string,
      description: desc,
      category: cat,
      subCategory: subCat,
      price: (fullProduct?.price ?? product.price) as number,
      quantity: quantity,
      imageUrl: img,
      tags: tagsStr,
    });
    setCategoryChoiceMode(
      !cat.trim() ? 'empty' : dropdownCategories.includes(cat.trim()) ? 'list' : 'custom'
    );
    setSubCategoryChoiceMode(
      !subCat.trim() ? 'empty' : dropdownSubcategories.includes(subCat.trim()) ? 'list' : 'custom'
    );
    setShowImagePreview(false);
    setIsDialogOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.email) {
      toast({
        title: 'Error',
        description: 'User email not found.',
        variant: 'destructive',
      });
      return;
    }

    setIsSubmitting(true);
    try {
      // Validate quantity
      if (formData.quantity < 1) {
        toast({
          title: 'Validation Error',
          description: 'Quantity must be at least 1 for the product to be purchasable.',
          variant: 'destructive',
        });
        setIsSubmitting(false);
        return;
      }

      const tagsArray = formData.tags
        .split(',')
        .map(tag => tag.trim())
        .filter(tag => tag.length > 0);

      const productPayload = {
        productName: formData.productName,
        description: formData.description,
        category: formData.category.trim() || undefined,
        subCategory: formData.subCategory.trim() || undefined,
        price: formData.price,
        quantity: formData.quantity,
        imageUrl: formData.imageUrl || undefined,
        tags: tagsArray.length > 0 ? tagsArray : undefined,
      };

      if (editingProduct) {
        // Update product
        const response = await updateProduct(editingProduct.id, productPayload, user.email);
        if (response.success) {
          toast({
            title: 'Success',
            description: 'Product updated successfully.',
          });
          setIsDialogOpen(false);
          fetchProducts(currentPage, activeSearchQuery);
        } else {
          throw new Error(response.message || 'Failed to update product');
        }
      } else {
        // Create product
        const response = await createProduct(productPayload, user.email);
        if (response.success) {
          toast({
            title: 'Success',
            description: 'Product created successfully.',
          });
          setIsDialogOpen(false);
          fetchProducts(currentPage, activeSearchQuery);
        } else {
          throw new Error(response.message || 'Failed to create product');
        }
      }
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'Failed to save product.',
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteClick = (product: Product) => {
    setDeletingProduct(product);
    setIsDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deletingProduct || !user?.email) return;

    try {
      console.log('Deleting product:', deletingProduct.id, 'with email:', user.email);
      const response = await deleteProduct(deletingProduct.id, user.email);
      console.log('Delete response:', response);
      
      if (response.success) {
        toast({
          title: 'Success',
          description: 'Product deleted successfully.',
        });
        setIsDeleteDialogOpen(false);
        setDeletingProduct(null);
        fetchProducts(currentPage);
      } else {
        throw new Error(response.message || 'Failed to delete product');
      }
    } catch (error) {
      console.error('Delete error:', error);
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'Failed to delete product. Please check the console for details.',
        variant: 'destructive',
      });
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.name.endsWith('.csv')) {
        toast({
          title: 'Invalid File',
          description: 'Please select a CSV file.',
          variant: 'destructive',
        });
        return;
      }
      setImportFile(file);
      handleImportCsv(file);
    }
  };

  const handleImportCsv = async (file: File) => {
    if (!user?.email) {
      toast({
        title: 'Error',
        description: 'User email not found.',
        variant: 'destructive',
      });
      return;
    }

    setIsImporting(true);
    try {
      const formData = new FormData();
      formData.append('file', file);

      const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
      console.log('Attempting to import CSV to:', `${API_BASE_URL}/api/products/import`);
      console.log('File name:', file.name, 'Size:', file.size, 'bytes');
      console.log('User email:', user.email);
      
      const response = await fetch(`${API_BASE_URL}/api/products/import`, {
        method: 'POST',
        // Don't set Content-Type header - browser will set it automatically with boundary for FormData
        headers: {
          'X-User-Email': user.email,
        },
        body: formData,
      });
      
      console.log('Response status:', response.status, response.statusText);

      if (!response.ok) {
        let errorMessage = 'Failed to import CSV';
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch (e) {
          errorMessage = `HTTP ${response.status}: ${response.statusText}`;
        }
        throw new Error(errorMessage);
      }

      const data = await response.json();

      if (data.success) {
        toast({
          title: 'Import Successful',
          description: `Imported ${data.successCount} products. ${data.errorCount > 0 ? `${data.errorCount} errors occurred.` : ''}`,
        });
        
        // Show errors if any
        if (data.errors && data.errors.length > 0) {
          console.error('Import errors:', data.errors);
        }
        
        fetchProducts(currentPage);
      } else {
        throw new Error(data.message || 'Import failed');
      }
    } catch (error) {
      console.error('CSV Import Error:', error);
      let errorMessage = 'Failed to import CSV file.';
      
      if (error instanceof TypeError && error.message.includes('fetch')) {
        errorMessage = 'Network error: Cannot connect to backend server. Please ensure the backend is running on http://localhost:8080';
      } else if (error instanceof Error) {
        errorMessage = error.message;
      }
      
      // Log full error details for debugging
      if (error instanceof Error) {
        console.error('Error name:', error.name);
        console.error('Error message:', error.message);
        console.error('Error stack:', error.stack);
      }
      
      toast({
        title: 'Import Failed',
        description: errorMessage,
        variant: 'destructive',
      });
    } finally {
      setIsImporting(false);
      setImportFile(null);
      // Reset file input
      const fileInput = document.getElementById('csv-upload') as HTMLInputElement;
      if (fileInput) {
        fileInput.value = '';
      }
    }
  };

  if (!isAdmin) {
    return null;
  }

  return (
    <div className="container py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold">Admin Panel</h1>
          <p className="text-muted-foreground mt-1">Manage products in your store</p>
        </div>
        <div className="flex gap-2">
          <div className="relative group">
            <Button 
              variant="outline" 
              onClick={() => document.getElementById('csv-upload')?.click()}
              disabled={isImporting}
            >
              <Upload className="mr-2 h-4 w-4" />
              {isImporting ? 'Importing...' : 'Import CSV'}
            </Button>
            <input
              id="csv-upload"
              type="file"
              accept=".csv"
              className="hidden"
              onChange={handleFileSelect}
            />
            <div className="absolute bottom-full left-0 mb-2 hidden group-hover:block bg-popover text-popover-foreground p-3 rounded-lg border shadow-lg z-10 w-80 text-xs">
              <p className="font-semibold mb-2">CSV Format:</p>
              <p className="mb-1">Required: product_name, price, quantity</p>
              <p className="mb-1">Optional: description, category, sub_category, brand, image_url, views, rating, tags</p>
              <p className="text-muted-foreground mt-2 text-sm">
                💡 Category, sub_category, and tags will be automatically generated by ML algorithm if not provided
              </p>
              <p className="text-muted-foreground text-sm">Tags: comma-separated (e.g., "tag1,tag2")</p>
            </div>
          </div>
          <Button onClick={handleOpenCreateDialog}>
            <Plus className="mr-2 h-4 w-4" />
            Add Product
          </Button>
        </div>
      </div>

      {/* Search Bar - Always visible, not conditionally rendered */}
      <div className="mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
          <Input
            type="text"
            placeholder="Search products by name, description, category, or tags... (Press Enter to search)"
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                // Trigger search when Enter is pressed
                setActiveSearchQuery(searchQuery);
                if (currentPage !== 0) {
                  setCurrentPage(0);
                }
              }
            }}
            className="pl-10"
            autoComplete="off"
            disabled={isLoading}
          />
          </div>
          {activeSearchQuery.trim() && (
            <p className="text-sm text-muted-foreground mt-2">
              Found {totalElements} product{totalElements !== 1 ? 's' : ''} matching &quot;{activeSearchQuery}&quot; (across all pages)
            </p>
          )}
        </div>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="border rounded-lg p-4 animate-pulse">
              <div className="h-4 bg-muted w-3/4 mb-2"></div>
              <div className="h-3 bg-muted w-1/2"></div>
            </div>
          ))}
        </div>
      ) : products.length === 0 ? (
        <div className="text-center py-12">
          <Package className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
          <h3 className="text-lg font-semibold mb-2">No products found</h3>
          <p className="text-muted-foreground mb-4">Get started by creating your first product.</p>
          <Button onClick={handleOpenCreateDialog}>
            <Plus className="mr-2 h-4 w-4" />
            Add Product
          </Button>
        </div>
      ) : (
        <>
          {products.length === 0 && activeSearchQuery.trim() ? (
            <div className="text-center py-12">
              <Search className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">No products found</h3>
              <p className="text-muted-foreground mb-4">
                No products match your search query: &quot;{activeSearchQuery}&quot;
              </p>
              <Button variant="outline" onClick={() => {
                setSearchQuery('');
                setActiveSearchQuery('');
              }}>
                Clear Search
              </Button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {products.map((product) => (
            <div key={product.id} className="border rounded-lg p-4 hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between mb-2">
                <div className="flex-1">
                  <h3 className="font-semibold text-lg mb-1">{product.name}</h3>
                  <p className="text-sm text-muted-foreground line-clamp-2 mb-2">
                    {product.description}
                  </p>
                  <div className="flex items-center gap-4 text-sm">
                    <span className="font-medium">${product.price.toFixed(2)}</span>
                    <span className="text-muted-foreground">{product.category}</span>
                  </div>
                </div>
              </div>
              <div className="flex gap-2 mt-4">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleOpenEditDialog(product)}
                  className="flex-1"
                >
                  <Edit className="mr-2 h-4 w-4" />
                  Edit
                </Button>
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => handleDeleteClick(product)}
                  className="flex-1"
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  Delete
                </Button>
              </div>
            </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* Pagination Controls */}
      {!isLoading && totalPages > 1 && (
        <div className="flex items-center justify-between mt-6">
          <div className="text-sm text-muted-foreground">
            Showing {currentPage * pageSize + 1} to {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} products
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={!hasPrevious}
            >
              <ChevronLeft className="h-4 w-4" />
              Previous
            </Button>
            <div className="flex items-center gap-1">
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                let pageNum: number;
                if (totalPages <= 5) {
                  pageNum = i;
                } else if (currentPage < 3) {
                  pageNum = i;
                } else if (currentPage > totalPages - 4) {
                  pageNum = totalPages - 5 + i;
                } else {
                  pageNum = currentPage - 2 + i;
                }
                return (
                  <Button
                    key={pageNum}
                    variant={currentPage === pageNum ? "default" : "outline"}
                    size="sm"
                    onClick={() => handlePageChange(pageNum)}
                    className="min-w-[40px]"
                  >
                    {pageNum + 1}
                  </Button>
                );
              })}
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={!hasNext}
            >
              Next
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}

      {/* Create/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingProduct ? 'Edit Product' : 'Create New Product'}</DialogTitle>
            <DialogDescription>
              {editingProduct
                ? 'Update the product information below.'
                : 'Fill in the details to create a new product.'}
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="grid gap-4 py-4">
              <div className="grid gap-2">
                <Label htmlFor="productName">Product Name *</Label>
                <Input
                  id="productName"
                  value={formData.productName}
                  onChange={(e) => setFormData({ ...formData, productName: e.target.value })}
                  required
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="description">Description *</Label>
                <Textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  required
                  rows={4}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label>Category</Label>
                  <Select
                    value={
                      categoryChoiceMode === 'empty'
                        ? '__empty__'
                        : categoryChoiceMode === 'custom'
                          ? '__custom__'
                          : formData.category.trim()
                    }
                    onValueChange={(v) => {
                      if (v === '__empty__') {
                        setCategoryChoiceMode('empty');
                        setFormData((prev) => ({ ...prev, category: '' }));
                      } else if (v === '__custom__') {
                        setCategoryChoiceMode('custom');
                      } else {
                        setCategoryChoiceMode('list');
                        setFormData((prev) => ({ ...prev, category: v }));
                      }
                    }}
                  >
                    <SelectTrigger id="category">
                      <SelectValue placeholder="Choose or type below" />
                    </SelectTrigger>
                    <SelectContent sideOffset={6} className="max-h-56 overflow-y-auto">
                      <SelectItem value="__empty__">Leave empty (ML will set)</SelectItem>
                      {dropdownCategories.map((c) => (
                        <SelectItem key={c} value={c}>
                          {c}
                        </SelectItem>
                      ))}
                      <SelectItem value="__custom__">Type my own</SelectItem>
                    </SelectContent>
                  </Select>
                  {categoryChoiceMode === 'custom' && (
                    <Input
                      placeholder="Type category"
                      value={formData.category}
                      onChange={(e) => setFormData((prev) => ({ ...prev, category: e.target.value }))}
                      className="mt-1"
                      autoFocus
                    />
                  )}
                </div>
                <div className="grid gap-2">
                  <Label>Sub-category</Label>
                  <Select
                    value={
                      subCategoryChoiceMode === 'empty'
                        ? '__empty__'
                        : subCategoryChoiceMode === 'custom'
                          ? '__custom__'
                          : formData.subCategory.trim()
                    }
                    onValueChange={(v) => {
                      if (v === '__empty__') {
                        setSubCategoryChoiceMode('empty');
                        setFormData((prev) => ({ ...prev, subCategory: '' }));
                      } else if (v === '__custom__') {
                        setSubCategoryChoiceMode('custom');
                      } else {
                        setSubCategoryChoiceMode('list');
                        setFormData((prev) => ({ ...prev, subCategory: v }));
                      }
                    }}
                  >
                    <SelectTrigger id="subCategory">
                      <SelectValue placeholder="Choose or type below" />
                    </SelectTrigger>
                    <SelectContent sideOffset={6} className="max-h-56 overflow-y-auto">
                      <SelectItem value="__empty__">Leave empty (ML will set)</SelectItem>
                      {dropdownSubcategories.map((s) => (
                        <SelectItem key={s} value={s}>
                          {s}
                        </SelectItem>
                      ))}
                      <SelectItem value="__custom__">Type my own</SelectItem>
                    </SelectContent>
                  </Select>
                  {subCategoryChoiceMode === 'custom' && (
                    <Input
                      placeholder="Type sub-category"
                      value={formData.subCategory}
                      onChange={(e) => setFormData((prev) => ({ ...prev, subCategory: e.target.value }))}
                      className="mt-1"
                      autoFocus
                    />
                  )}
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="price">Price *</Label>
                  <Input
                    id="price"
                    type="number"
                    step="0.01"
                    min="0"
                    value={formData.price}
                    onChange={(e) => setFormData({ ...formData, price: parseFloat(e.target.value) || 0 })}
                    required
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="quantity">Quantity *</Label>
                  <Input
                    id="quantity"
                    type="number"
                    min="1"
                    value={formData.quantity}
                    onChange={(e) => setFormData({ ...formData, quantity: Math.max(1, parseInt(e.target.value) || 1) })}
                    required
                  />
                </div>
                <div className="grid gap-2">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="imageUrl">Image URL</Label>
                    {formData.imageUrl && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => setShowImagePreview(!showImagePreview)}
                        className="h-7 px-2 text-xs"
                      >
                        {showImagePreview ? (
                          <>
                            <EyeOff className="mr-1 h-3 w-3" />
                            Hide Image
                          </>
                        ) : (
                          <>
                            <Eye className="mr-1 h-3 w-3" />
                            Preview Image
                          </>
                        )}
                      </Button>
                    )}
                  </div>
                  <Input
                    id="imageUrl"
                    type="url"
                    value={formData.imageUrl}
                    onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                    placeholder="https://example.com/image.jpg"
                  />
                  {showImagePreview && formData.imageUrl && (
                    <div className="mt-2 border rounded-lg overflow-hidden bg-muted/50">
                      <div className="aspect-video relative flex items-center justify-center">
                        <img
                          src={formData.imageUrl}
                          alt="Product preview"
                          className="max-w-full max-h-full object-contain"
                          onError={(e) => {
                            const target = e.target as HTMLImageElement;
                            target.style.display = 'none';
                            const parent = target.parentElement;
                            if (parent) {
                              parent.innerHTML = '<div class="text-sm text-muted-foreground p-4 text-center">Failed to load image</div>';
                            }
                          }}
                        />
                      </div>
                    </div>
                  )}
                </div>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="tags">
                  Tags (comma-separated) <span className="text-muted-foreground text-xs">(auto-generated if empty)</span>
                </Label>
                <Input
                  id="tags"
                  value={formData.tags}
                  onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                  placeholder="Will be auto-generated by ML (or enter: tag1, tag2, tag3)"
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsDialogOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Saving...' : editingProduct ? 'Update' : 'Create'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the product{' '}
              <strong>{deletingProduct?.name}</strong>.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteConfirm} className="bg-destructive text-destructive-foreground">
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
