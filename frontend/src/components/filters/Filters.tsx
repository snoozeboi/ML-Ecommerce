import * as React from 'react';
import { FilterOptions } from '@/types';
import { Label } from '@/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Slider } from '@/components/ui/slider';
import { Checkbox } from '@/components/ui/checkbox';

interface FiltersProps {
  filters: FilterOptions;
  onFilterChange: (filters: FilterOptions) => void;
  maxPrice?: number;
}

const sortOptions = [
  { value: 'name-asc', label: 'A–Z' },
  { value: 'name-desc', label: 'Z–A' },
  { value: 'price-asc', label: 'Price: Low to High' },
  { value: 'price-desc', label: 'Price: High to Low' },
  { value: 'rating', label: 'Rating' },
] as const;

export function Filters({ filters, onFilterChange, maxPrice = 1000 }: FiltersProps) {
  // Local state so slider can move smoothly without triggering a backend
  // request on every pixel; we only commit on release.
  const [localPriceRange, setLocalPriceRange] = React.useState<number[]>(filters.priceRange);

  // Keep local slider value in sync if filters change from outside
  React.useEffect(() => {
    setLocalPriceRange(filters.priceRange);
  }, [filters.priceRange]);

  const handleSortChange = (value: string) => {
    onFilterChange({ ...filters, sortBy: value as FilterOptions['sortBy'] });
  };

  // Commit price range only when the user releases the thumb
  const handlePriceCommit = (value: number[]) => {
    const [a, b] = value;
    const nextMin = Math.min(a, b);
    const nextMax = Math.max(a, b);
    onFilterChange({ ...filters, priceRange: [nextMin, nextMax] });
  };

  return (
    <div className="filter-section space-y-6">
      {/* Sort By */}
      <div>
        <h3 className="font-semibold text-sm mb-3 text-foreground">Sort By</h3>
        <RadioGroup value={filters.sortBy} onValueChange={handleSortChange}>
          {sortOptions.map((option) => (
            <div key={option.value} className="flex items-center space-x-2">
              <RadioGroupItem value={option.value} id={option.value} />
              <Label htmlFor={option.value} className="text-sm cursor-pointer">
                {option.label}
              </Label>
            </div>
          ))}
        </RadioGroup>
      </div>

      {/* Price Range */}
      <div>
        <h3 className="font-semibold text-sm mb-3 text-foreground">Price Range</h3>
        <div className="px-2">
          <Slider
            value={localPriceRange}
            onValueChange={setLocalPriceRange}
            onValueCommit={handlePriceCommit}
            max={maxPrice}
            min={0}
            step={10}
            className="mb-2"
          />
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>${localPriceRange[0]}</span>
            <span>${localPriceRange[1]}</span>
          </div>
        </div>
      </div>

      {/* In Stock */}
      <div className="flex items-center space-x-2">
        <Checkbox
          id="in-stock"
          checked={filters.inStockOnly}
          onCheckedChange={(checked) =>
            onFilterChange({ ...filters, inStockOnly: checked as boolean })
          }
        />
        <Label htmlFor="in-stock" className="text-sm cursor-pointer">
          In Stock Only
        </Label>
      </div>
    </div>
  );
}
