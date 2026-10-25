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
        className="fine-grid pointer-events-none absolute inset-0 opacity-30"
      />

      <div className="site-container relative z-10">
        <div className="grid gap-8 lg:grid-cols-12 lg:items-end lg:gap-12">
          <div className="lg:col-span-7" data-reveal>
            <p className="eyebrow">{experience.eyebrow}</p>
            <h2 className="mt-4 max-w-2xl text-display-md text-fg">{experience.title}</h2>
          </div>
          <div className="lg:col-span-4 lg:col-start-9" data-reveal>
            <p className="max-w-xl border-l border-border-strong pl-5 text-small leading-relaxed text-fg-muted">
              {experience.intro}
            </p>
          </div>
        </div>

        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:mt-10 lg:gap-4">
          {cards.map((card, index) => {
            const Icon = CARD_ICONS[index];
            return (
              <Card
                key={card.title}
                data-reveal
                className="group relative overflow-hidden rounded-2xl border-border bg-surface-raised/80 p-5 hover:border-border-strong"
              >
                <div className="flex items-start justify-between gap-6">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full border border-border bg-surface text-accent-strong">
                    <Icon className="h-4 w-4" aria-hidden="true" />
                  </div>
                  <span
                    aria-hidden="true"
                    className="font-display text-3xl leading-none text-fg/[0.08]"
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
                  className="mt-4 h-px w-12 bg-accent/45 transition-fast group-hover:w-20"
                />
              </Card>
            );
          })}
        </div>
      </div>
      </Section>
    </div>
  );
}
