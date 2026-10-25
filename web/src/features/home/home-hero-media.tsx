import { HomeHeroVideo } from "./hero-video";

export function HomeHeroMedia() {
  return (
    <div className="absolute inset-0 overflow-hidden" aria-hidden="true">
      <picture>
        <source
          media="(max-width: 767px)"
          srcSet="/media/hero/home-hero-poster-960.webp"
        />
        <source
          media="(max-width: 1023px)"
          srcSet="/media/hero/home-hero-poster-1280.webp"
        />
        {/* A plain <img> keeps the poster as the LCP element without the image optimiser round-trip. */}
        <img
          data-testid="home-hero-poster"
          src="/media/hero/home-hero-poster.webp"
          alt=""
          aria-hidden="true"
          width={1920}
          height={1080}
          fetchPriority="high"
          loading="eager"
          className="absolute inset-0 h-full w-full object-cover object-center"
        />
      </picture>
      <HomeHeroVideo />
    </div>
  );
}
