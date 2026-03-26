interface SideAdProps {
  variant: 1 | 2;
}

export function SideAd({ variant }: SideAdProps) {
  const isDark = variant === 1;

  return (
    <div
      className={`relative rounded-2xl overflow-hidden h-[260px] flex items-stretch shadow-md border ${
        isDark
          ? 'bg-gradient-to-b from-slate-900 via-slate-800 to-slate-900 border-slate-800'
          : 'bg-gradient-to-b from-orange-500 via-orange-400 to-amber-400 border-orange-400'
      }`}
    >
      {/* Decorative gradient glow */}
      <div
        className={`absolute -right-10 -top-10 h-32 w-32 rounded-full blur-3xl opacity-60 ${
          isDark ? 'bg-cyan-400/40' : 'bg-white/70'
        }`}
      />

      <div className="relative flex flex-col justify-between p-5 w-full">
        <div className="space-y-2">
          <span
            className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-[11px] font-medium tracking-wide ${
              isDark
                ? 'bg-slate-800/80 text-slate-100 border border-slate-700/80'
                : 'bg-white/15 text-white border border-white/40'
            }`}
          >
            {variant === 1 ? 'Smart deals for you' : 'Limited time offer'}
          </span>
          <h3
            className={`font-display text-xl leading-snug ${
              isDark ? 'text-slate-50' : 'text-white'
            }`}
          >
            {variant === 1
              ? 'Upgrade your everyday carry.'
              : 'Save more on your next order.'}
          </h3>
          <p
            className={`text-xs leading-relaxed ${
              isDark ? 'text-slate-300/80' : 'text-white/85'
            }`}
          >
            Curated picks based on what shoppers like you actually buy. Fresh
            recommendations every time you visit.
          </p>
        </div>

        <button
          className={`mt-4 inline-flex items-center justify-center rounded-full px-4 py-2 text-xs font-semibold shadow-sm transition hover:-translate-y-0.5 hover:shadow-md ${
            isDark
              ? 'bg-cyan-400 text-slate-950 hover:bg-cyan-300'
              : 'bg-white text-orange-600 hover:bg-amber-50'
          }`}
          type="button"
        >
          Discover deals
        </button>
      </div>
    </div>
  );
}
