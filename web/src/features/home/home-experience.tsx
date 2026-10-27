import { Coffee, Flame, Landmark, Sunrise } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Section } from "@/components/ui/section";
import type { Dictionary } from "@/lib/i18n";

const CARD_ICONS = [Landmark, Sunrise, Flame, Coffee] as const;

export function HomeExperience({ dict }: { dict: Dictionary }) {
  const experience = dict.home.experience;
  const cards = [
    experience.cards.shelter,
    experience.cards.terrace,
    experience.cards.comfort,
    experience.cards.breakfast,
  ];

  return (
    <div data-motion-track="home-experience" className="relative">
      <Section
        as="section"
        id="home-experience"
        data-testid="home-experience"
        data-motion-scene="home-experience"
        data-motion="static"
        tone="paper"
        spacing="none"
        container={false}
        className="relative flex overflow-hidden py-section lg:min-h-[calc(100svh-5rem)] lg:items-center lg:py-0"
      >
        <div
          aria-hidden="true"
          data-parallax
          data-parallax-amplitude="24"
          className="fine-grid pointer-events-none absolute inset-0 opacity-30"
        />

        <div className="site-container relative z-10">
          <div className="grid gap-8 lg:grid-cols-12 lg:items-end lg:gap-12">
            <div className="lg:col-span-7" data-reveal>
              <p className="eyebrow">{experience.eyebrow}</p>
              <h2 className="mt-4 max-w-2xl text-display-md text-fg">
                {experience.title}
              </h2>
            </div>
            <div className="lg:col-span-4 lg:col-start-9" data-reveal>
              <p className="max-w-xl border-l border-border-strong pl-5 text-small leading-relaxed text-fg-muted">
                {experience.intro}
              </p>
            </div>
          </div>

          <div className="mt-8 grid gap-4 lg:mt-10 lg:grid-cols-12 lg:gap-5">
            <div className="lg:col-span-5" data-reveal>
              <div
                data-parallax
                data-parallax-amplitude="24"
                className="relative h-full min-h-[18rem] overflow-hidden rounded-[1.5rem_1.5rem_0.5rem_0.5rem] border border-border bg-surface-inverse shadow-elevated"
              >
                <div className="aspect-[4/3] h-full lg:aspect-auto">
                  {/* Decorative still derived from the approved D18 hero source. */}
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    data-testid="home-experience-image"
                    data-slow-zoom
                    src="/media/experience/home-experience-panorama.webp"
                    alt=""
                    aria-hidden="true"
                    width={1200}
                    height={900}
                    loading="eager"
                    fetchPriority="low"
                    decoding="async"
                    className="h-full w-full object-cover object-center"
                  />
                </div>
                <div
                  aria-hidden="true"
                  className="absolute inset-0 bg-gradient-to-t from-surface-inverse/80 via-surface-inverse/10 to-transparent"
                />
                <div className="absolute inset-x-0 bottom-0 flex items-end justify-between p-5 sm:p-6">
                  <span
                    aria-hidden="true"
                    className="font-display text-[5.5rem] leading-none text-fg-inverse/50"
                  >
                    02
                  </span>
                  <span aria-hidden="true" className="mb-2 h-px w-14 bg-accent-soft/70" />
                </div>
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 lg:col-span-7">
              {cards.map((card, index) => {
                const Icon = CARD_ICONS[index];
                return (
                  <Card
                    key={card.title}
                    data-reveal
                    className="group relative overflow-hidden rounded-2xl border-border bg-surface-raised/90 p-5 hover:border-border-strong hover:shadow-elevated"
                  >
                    <div className="flex items-start justify-between gap-6">
                      <div className="flex h-10 w-10 items-center justify-center rounded-full border border-border bg-surface text-accent-strong">
                        <Icon
                          data-icon-draw
                          className="h-4 w-4"
                          aria-hidden="true"
                        />
                      </div>
                      <span
                        aria-hidden="true"
                        className="font-display text-3xl leading-none text-accent/30"
                      >
                        0{index + 1}
                      </span>
                    </div>
                    <h3 className="mt-5 text-xl text-fg">{card.title}</h3>
                    <p className="mt-2 text-small leading-relaxed text-fg-muted">
                      {card.description}
                    </p>
                    <div
                      aria-hidden="true"
                      className="mt-4 h-px w-12 bg-accent/45 transition-fast group-hover:bg-accent"
                    />
                  </Card>
                );
              })}
            </div>
          </div>
        </div>
      </Section>
    </div>
  );
}
