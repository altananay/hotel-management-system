import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { HomeContact } from "@/features/home/home-contact";
import { HomeExperience } from "@/features/home/home-experience";
import { HomeHero } from "@/features/home/home-hero";
import { HomeMotion } from "@/features/home/home-motion";
import { HomeRoomsCta } from "@/features/home/home-rooms-cta";
import { HomeStory } from "@/features/home/home-story";
import { getDictionary, isValidLocale, type Locale } from "@/lib/i18n";
import { buildPageMetadata } from "@/lib/seo";

interface LocalePageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: LocalePageProps): Promise<Metadata> {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    return {};
  }
  return buildPageMetadata({
    locale: locale as Locale,
    route: "home",
    dict: getDictionary(locale),
  });
}

export default async function LocalePage({ params }: LocalePageProps) {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    notFound();
  }
  const dict = getDictionary(locale);

  return (
    <>
      <HomeHero locale={locale} dict={dict} />
      <HomeStory dict={dict} />
      <HomeExperience dict={dict} />
      <HomeRoomsCta locale={locale} dict={dict} />
      <HomeContact dict={dict} />
      <HomeMotion />
    </>
  );
}
