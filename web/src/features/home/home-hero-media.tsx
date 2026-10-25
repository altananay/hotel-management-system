import { HomeHeroVideo } from "./hero-video";

const HERO_POSTER = "/media/hero/home-hero-poster.webp";
const HERO_POSTER_SRCSET = [
  "/media/hero/home-hero-poster-960.webp 960w",
  "/media/hero/home-hero-poster-1280.webp 1280w",
  `${HERO_POSTER} 1920w`,
].join(", ");
const HERO_POSTER_SIZES = "100vw";

export function HomeHeroMedia() {
  return (
    <div className="absolute inset-0 overflow-hidden" aria-hidden="true">
      {/* A plain <img> keeps the poster as the LCP element without the image optimiser round-trip. */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        data-testid="home-hero-poster"
        src={HERO_POSTER}
        srcSet={HERO_POSTER_SRCSET}
        sizes={HERO_POSTER_SIZES}
        alt=""
        aria-hidden="true"
        width={1920}
        height={1080}
        fetchPriority="high"
        loading="eager"
        className="absolute inset-0 h-full w-full object-cover object-center"
      />
      <HomeHeroVideo />
    </div>
  );
}
