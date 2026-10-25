import { Section } from "@/components/ui/section";
import type { Dictionary } from "@/lib/i18n";

export function HomeStory({ dict }: { dict: Dictionary }) {
  const story = dict.home.story;

  return (
    <div data-motion-track="home-story" className="relative">
      <Section
        as="section"
        id="home-story"
        data-testid="home-story"
        data-motion-scene="home-story"
        data-motion="static"
        tone="inverse"
        spacing="none"
        container={false}
        className="isolate flex overflow-hidden py-section lg:min-h-[calc(100svh-5rem)] lg:items-center lg:py-0"
      >
      <div
        data-parallax
        aria-hidden="true"
        className="pointer-events-none absolute inset-[-12%] opacity-80"
      >
        <div className="paper-grain absolute inset-0 opacity-25" />
        <div className="fine-grid absolute inset-0 opacity-20" />
        <div className="absolute -left-32 top-1/4 h-[30rem] w-[30rem] rounded-full bg-accent/12 blur-3xl" />
        <div className="absolute -right-20 bottom-0 h-[26rem] w-[26rem] rounded-full bg-accent-soft/8 blur-3xl" />
        <div className="absolute left-1/2 top-1/2 h-[34rem] w-[24rem] -translate-x-1/2 -translate-y-1/2 rounded-[999px_999px_0_0] border border-fg-inverse/10" />
      </div>

      <div className="site-container relative z-10">
        <div className="grid gap-12 lg:grid-cols-12 lg:gap-16">
          <div className="lg:col-span-4" data-reveal>
            <p className="eyebrow text-accent-soft">{story.eyebrow}</p>
            <p
              aria-hidden="true"
              data-parallax
              data-parallax-amplitude="20"
              className="mt-10 font-display text-[7rem] leading-none text-fg-inverse/8 lg:text-[9rem]"
            >
              01
            </p>
            <div className="mt-8 h-px w-16 bg-accent-soft/50" aria-hidden="true" />
          </div>

          <div className="lg:col-span-8">
            <h2 className="max-w-3xl text-display-lg text-fg-inverse" data-reveal>
              {story.title}
            </h2>

            <div className="mt-10 grid gap-8 border-t border-fg-inverse/15 pt-8 lg:grid-cols-12 lg:gap-10">
              <p
                className="text-lg leading-relaxed text-fg-inverse-muted lg:col-span-7 lg:text-xl"
                data-reveal
              >
                {story.paragraph1}
              </p>
              <div className="space-y-6 lg:col-span-5">
                <p className="text-small leading-relaxed text-fg-inverse-muted" data-reveal>
                  {story.paragraph2}
                </p>
                <p className="border-l border-accent-soft/45 pl-5 text-small font-semibold leading-relaxed text-fg-inverse" data-reveal>
                  {story.paragraph3}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
      </Section>
    </div>
  );
}
