import { Sparkles, TrendingUp, Heart, Zap } from 'lucide-react';

type BadgeType = 'personalized' | 'trending' | 'popular' | 'similar';

interface RecommendationBadgeProps {
  type: BadgeType;
}

const badgeConfig = {
  personalized: {
    icon: Sparkles,
    label: 'Picked for you',
    className: 'bg-accent/10 text-accent border-accent/20',
  },
  trending: {
    icon: TrendingUp,
    label: 'Trending now',
    className: 'bg-success/10 text-success border-success/20',
  },
  popular: {
    icon: Heart,
    label: 'Customer favorite',
    className: 'bg-sale/10 text-sale border-sale/20',
  },
  similar: {
    icon: Zap,
    label: 'You might like',
    className: 'bg-info/10 text-info border-info/20',
  },
};

export function RecommendationBadge({ type }: RecommendationBadgeProps) {
  const config = badgeConfig[type];
  const Icon = config.icon;

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-medium border shadow-sm ${config.className}`}
    >
      <Icon className="w-3.5 h-3.5" />
      {config.label}
    </span>
  );
}
