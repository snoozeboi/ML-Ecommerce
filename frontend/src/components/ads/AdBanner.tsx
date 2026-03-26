import { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface Banner {
  id: string;
  title: string;
  subtitle?: string;
  image: string;
  link?: string;
}

const defaultBanners: Banner[] = [
  {
    id: '1',
    title: 'Summer Sale',
    subtitle: 'Up to 50% off on selected items',
    image: 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=800&q=80',
  },
  {
    id: '2',
    title: 'New Arrivals',
    subtitle: 'Check out the latest trends',
    image: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=800&q=80',
  },
  {
    id: '3',
    title: 'Free Shipping',
    subtitle: 'On all orders over $50',
    image: 'https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=800&q=80',
  },
];

interface AdBannerProps {
  banners?: Banner[];
  autoPlay?: boolean;
  interval?: number;
  height?: 'default' | 'tall' | 'hero';
}

export function AdBanner({ 
  banners = defaultBanners, 
  autoPlay = true, 
  interval = 4000,
  height = 'tall'
}: AdBannerProps) {
  const [currentIndex, setCurrentIndex] = useState(0);

  const heightClasses = {
    default: 'h-[300px]',
    tall: 'h-[500px]',
    hero: 'h-[600px]',
  };

  useEffect(() => {
    if (!autoPlay) return;

    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % banners.length);
    }, interval);

    return () => clearInterval(timer);
  }, [autoPlay, interval, banners.length]);

  const goTo = (index: number) => setCurrentIndex(index);
  const prev = () => setCurrentIndex((prev) => (prev - 1 + banners.length) % banners.length);
  const next = () => setCurrentIndex((prev) => (prev + 1) % banners.length);

  return (
    <div className={`ad-banner relative w-full ${heightClasses[height]} rounded-2xl overflow-hidden`}>
      {/* Banner Images */}
      <div className="relative w-full h-full overflow-hidden">
        {banners.map((banner, index) => (
          <div
            key={banner.id}
            className={`absolute inset-0 transition-opacity duration-700 ease-in-out ${
              index === currentIndex 
                ? 'opacity-100 z-10' 
                : 'opacity-0 z-0 pointer-events-none'
            }`}
          >
            <img
              src={banner.image}
              alt={banner.title}
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-primary/90 via-primary/30 to-transparent" />
            <div className="absolute bottom-8 left-8 right-8 text-primary-foreground">
              <h3 className="font-display text-3xl md:text-4xl font-bold mb-2">{banner.title}</h3>
              {banner.subtitle && (
                <p className="text-lg md:text-xl opacity-90">{banner.subtitle}</p>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Left Arrow - Overlayed on edge */}
      <button
        onClick={prev}
        className="carousel-arrow left"
        aria-label="Previous slide"
      >
        <ChevronLeft className="w-6 h-6" />
      </button>

      {/* Right Arrow - Overlayed on edge */}
      <button
        onClick={next}
        className="carousel-arrow right"
        aria-label="Next slide"
      >
        <ChevronRight className="w-6 h-6" />
      </button>

      {/* Dots Navigation */}
      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
        {banners.map((_, index) => (
          <button
            key={index}
            onClick={() => goTo(index)}
            aria-label={`Go to slide ${index + 1}`}
            className={`h-2 rounded-full transition-all duration-300 ${
              index === currentIndex
                ? 'bg-accent w-8'
                : 'bg-primary-foreground/50 w-2 hover:bg-primary-foreground/80'
            }`}
          />
        ))}
      </div>
    </div>
  );
}
