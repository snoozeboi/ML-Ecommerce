import { Skeleton } from '@/components/ui/skeleton';

interface ProductSkeletonProps {
  count?: number;
  size?: 'sm' | 'md' | 'lg';
}

export function ProductSkeleton({ count = 4, size = 'md' }: ProductSkeletonProps) {
  const sizeClasses = {
    sm: 'w-36 h-48',
    md: 'w-48 h-64',
    lg: 'w-64 h-80',
  };

  return (
    <div className="flex gap-4 overflow-hidden">
      {[...Array(count)].map((_, i) => (
        <div
          key={i}
          className={`${sizeClasses[size]} rounded-xl flex-shrink-0 overflow-hidden`}
          style={{ animationDelay: `${i * 100}ms` }}
        >
          <Skeleton className="w-full h-3/5" />
          <div className="p-3 space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-3 w-2/3" />
            <Skeleton className="h-5 w-1/3" />
          </div>
        </div>
      ))}
    </div>
  );
}
