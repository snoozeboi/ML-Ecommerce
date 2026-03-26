import { useEffect, useRef, useState } from 'react';
import { Product } from '@/types';
import { getRecommendations, getPopularPurchases } from '@/services/api';
import { RecommendationSection } from '@/components/recommendations';
import { AdBanner, SideAd } from '@/components/ads';
import { useUser } from '@/contexts/UserContext';

const INITIAL_PAGE_SIZE = 5;   // first batch shown immediately
const LOAD_MORE_PAGE_SIZE = 10; // each "load more" fetches this many

const Index = () => {
  const { user, orders } = useUser();
  const [recommendedAll, setRecommendedAll] = useState<Product[]>([]);
  const [popularAll, setPopularAll] = useState<Product[]>([]);
  const [recommended, setRecommended] = useState<Product[]>([]);
  const [popular, setPopular] = useState<Product[]>([]);
  const [recommendedLoadedCount, setRecommendedLoadedCount] = useState(0);
  const [popularLoadedCount, setPopularLoadedCount] = useState(0);
  const [isLoadingRec, setIsLoadingRec] = useState(true);
  const [isLoadingPopular, setIsLoadingPopular] = useState(true);
  const [noMoreRecommended, setNoMoreRecommended] = useState(false);
  const [noMorePopular, setNoMorePopular] = useState(false);
  const loadedCountRef = useRef({ rec: 0, pop: 0 });

  // Pass userId when logged in so backend returns personalized (purchase-history) recommendations.
  // When logged out (undefined), we fetch guest recommendations (= same as Popular / trending).
  const recommendationUserId = user?.id != null ? String(user.id) : undefined;

  // Fetch recommendations and popular. When recommendationUserId changes (switch user or logout),
  // always clear recommended first so we never show the previous user's list, then refetch.
  useEffect(() => {
    let cancelled = false;
    setNoMoreRecommended(false);
    setNoMorePopular(false);

    // Always clear recommended when the "recommendation user" changes (different user or guest)
    // so we never show the previous user's categories.
    setRecommendedAll([]);
    setRecommended([]);
    setRecommendedLoadedCount(0);
    loadedCountRef.current.rec = 0;

    // Recommendations: when logged in = personalized; when logged out = same as Popular (trending)
    (async () => {
      setIsLoadingRec(true);
      try {
        const recRes = recommendationUserId != null
          ? await getRecommendations(recommendationUserId, INITIAL_PAGE_SIZE, 0)
          : await getPopularPurchases(INITIAL_PAGE_SIZE, 0);
        if (cancelled) return;
        const recProducts = recRes.data.products || [];
        setRecommendedAll(recProducts);
        setRecommended(recProducts);
        setRecommendedLoadedCount(recProducts.length);
        loadedCountRef.current.rec = recProducts.length;
      } catch {
        // ignore
      } finally {
        if (!cancelled) setIsLoadingRec(false);
      }
    })();

    // Popular: fetch first 5 only
    (async () => {
      setIsLoadingPopular(true);
      try {
        const popRes = await getPopularPurchases(INITIAL_PAGE_SIZE, 0);
        if (cancelled) return;
        const popProducts = popRes.data.products || [];
        setPopularAll(popProducts);
        setPopular(popProducts);
        setPopularLoadedCount(popProducts.length);
        loadedCountRef.current.pop = popProducts.length;
      } finally {
        if (!cancelled) setIsLoadingPopular(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [recommendationUserId, orders?.length ?? 0]);

  // Show "load more" when we have buffered items or we might have more from server
  const recommendedHasNext =
    recommendedLoadedCount < recommendedAll.length ||
    (recommendedAll.length > 0 && !noMoreRecommended);
  const popularHasNext =
    popularLoadedCount < popularAll.length ||
    (popularAll.length > 0 && !noMorePopular);

  const handleRecommendedNext = async () => {
    if (recommendedLoadedCount < recommendedAll.length) {
      const newCount = Math.min(recommendedLoadedCount + LOAD_MORE_PAGE_SIZE, recommendedAll.length);
      setRecommendedLoadedCount(newCount);
      setRecommended(recommendedAll.slice(0, newCount));
      loadedCountRef.current.rec = newCount;
      return;
    }
    const res = recommendationUserId != null
      ? await getRecommendations(recommendationUserId, LOAD_MORE_PAGE_SIZE, recommendedAll.length)
      : await getPopularPurchases(LOAD_MORE_PAGE_SIZE, recommendedAll.length);
    const incoming = res.data?.products || [];
    if (incoming.length === 0) {
      setNoMoreRecommended(true);
      return;
    }

    const updatedAll = [...recommendedAll, ...incoming];
    const newCount = recommendedLoadedCount + incoming.length;
    setRecommendedAll(updatedAll);
    setRecommendedLoadedCount(newCount);
    setRecommended(updatedAll.slice(0, newCount));
    loadedCountRef.current.rec = newCount;
  };

  const handlePopularNext = async () => {
    if (popularLoadedCount < popularAll.length) {
      const newCount = Math.min(popularLoadedCount + LOAD_MORE_PAGE_SIZE, popularAll.length);
      setPopularLoadedCount(newCount);
      setPopular(popularAll.slice(0, newCount));
      loadedCountRef.current.pop = newCount;
      return;
    }

    const res = await getPopularPurchases(LOAD_MORE_PAGE_SIZE, popularAll.length);
    const incoming = res.data?.products || [];
    if (incoming.length === 0) {
      setNoMorePopular(true);
      return;
    }

    const updatedAll = [...popularAll, ...incoming];
    const newCount = popularLoadedCount + incoming.length;
    setPopularAll(updatedAll);
    setPopularLoadedCount(newCount);
    setPopular(updatedAll.slice(0, newCount));
    loadedCountRef.current.pop = newCount;
  };

  return (
    <div className="w-full max-w-[1600px] mx-auto px-4 md:px-8 xl:px-12 py-10">
      {/* Main Grid Layout matching wireframe */}
      <div className="grid grid-cols-1 lg:grid-cols-[260px_minmax(0,2.1fr)_320px] gap-10">
        {/* Left Side Ads */}
        <aside className="hidden lg:block">
          {/* Fixed, vertically centered left ads */}
          <div className="fixed left-12 top-1/2 -translate-y-1/2 w-[260px] flex flex-col gap-6">
            <SideAd variant={1} />
            <SideAd variant={2} />
          </div>
        </aside>

        {/* Center Content */}
        <div className="flex flex-col gap-8">
          {/* Recommended: when logged in = personalized; when logged out = same as Popular (guest) */}
          <RecommendationSection
            title="Recommended"
            subtitle={recommendationUserId
              ? "Products handpicked just for you based on your browsing history"
              : "Popular picks for everyone — sign in for personalized recommendations"}
            products={recommended}
            type="personalized"
            isLoading={isLoadingRec}
            onNextPage={handleRecommendedNext}
            hasNextPage={recommendedHasNext}
          />

          {/* Popular Purchases */}
          <RecommendationSection
            title="Popular Purchases"
            subtitle="What other customers are loving right now"
            products={popular}
            type="popular"
            isLoading={isLoadingPopular}
            onNextPage={handlePopularNext}
            hasNextPage={popularHasNext}
          />
        </div>

        {/* Right Side Banner */}
        <aside className="hidden lg:block">
          {/* Make the ad vertically centered and fixed so it doesn't move on scroll */}
          <div className="fixed right-12 top-1/2 -translate-y-1/2 w-[320px]">
            <AdBanner />
          </div>
        </aside>
      </div>

      {/* Mobile Ads - Show at bottom on mobile */}
      <div className="lg:hidden mt-8 grid grid-cols-2 gap-4">
        <SideAd variant={1} />
        <SideAd variant={2} />
      </div>
    </div>
  );
};

export default Index;
