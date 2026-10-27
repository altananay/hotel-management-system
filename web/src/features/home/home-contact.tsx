import { Mail, MapPin, Phone } from "lucide-react";
import { Section } from "@/components/ui/section";
import type { Dictionary } from "@/lib/i18n";

export function HomeContact({ dict }: { dict: Dictionary }) {
  const contact = dict.home.contact;

  return (
    <Section
      as="section"
      id="home-contact"
      data-testid="home-contact"
      data-motion-scene="home-contact"
      data-motion="static"
      tone="raised"
      spacing="default"
      container={false}
      className="relative overflow-hidden"
    >
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-y-0 right-0 w-1/2 bg-gradient-to-l from-accent-soft/35 to-transparent"
      />
      <div className="site-container relative z-10">
        <div className="grid gap-12 lg:grid-cols-12 lg:gap-16">
          <div className="lg:col-span-5" data-reveal>
            <p className="eyebrow">{contact.eyebrow}</p>
            <h2 className="mt-5 max-w-lg text-display-lg text-fg">{contact.title}</h2>
            <p className="mt-6 max-w-md text-body leading-relaxed text-fg-muted">
              {contact.body}
            </p>
          </div>

          <div className="lg:col-span-6 lg:col-start-7" data-reveal>
            <div className="divide-y divide-border border-y border-border">
              <div className="grid gap-3 py-6 sm:grid-cols-12 sm:gap-6">
                <p className="flex items-center gap-2 text-eyebrow font-semibold uppercase tracking-[0.18em] text-fg-muted sm:col-span-4">
                  <MapPin className="h-3.5 w-3.5 text-accent-strong" aria-hidden="true" />
                  {contact.addressLabel}
                </p>
                <p
                  data-testid="home-contact-address"
                  className="text-body leading-relaxed text-fg sm:col-span-8"
                >
                  {contact.address}
                </p>
              </div>

              <div className="grid gap-3 py-6 sm:grid-cols-12 sm:gap-6">
                <p className="flex items-center gap-2 text-eyebrow font-semibold uppercase tracking-[0.18em] text-fg-muted sm:col-span-4">
                  <Phone className="h-3.5 w-3.5 text-accent-strong" aria-hidden="true" />
                  {contact.phoneLabel}
                </p>
                <p className="text-body text-fg sm:col-span-8">
                  <a
                    data-testid="home-contact-phone"
                    href={`tel:${contact.phone.replace(/\s+/g, "")}`}
                    className="underline decoration-border-strong underline-offset-4 transition-fast hover:text-accent-strong hover:decoration-accent"
                  >
                    {contact.phone}
                  </a>
                </p>
              </div>

              <div className="grid gap-3 py-6 sm:grid-cols-12 sm:gap-6">
                <p className="flex items-center gap-2 text-eyebrow font-semibold uppercase tracking-[0.18em] text-fg-muted sm:col-span-4">
                  <Mail className="h-3.5 w-3.5 text-accent-strong" aria-hidden="true" />
                  {contact.emailLabel}
                </p>
                <p className="text-body text-fg sm:col-span-8">
                  <a
                    data-testid="home-contact-email"
                    href={`mailto:${contact.email}`}
                    className="underline decoration-border-strong underline-offset-4 transition-fast hover:text-accent-strong hover:decoration-accent"
                  >
                    {contact.email}
                  </a>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Section>
  );
}

