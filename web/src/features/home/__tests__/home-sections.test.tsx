import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { HomeContact } from "../home-contact";
import { HomeExperience } from "../home-experience";
import { HomeHero } from "../home-hero";
import { HomeRoomsCta } from "../home-rooms-cta";
import { HomeStory } from "../home-story";
import tr from "@/lib/i18n/tr.json";

function HomeComposition() {
  return (
    <>
      <HomeHero locale="tr" dict={tr} />
      <HomeStory dict={tr} />
      <HomeExperience dict={tr} />
      <HomeRoomsCta locale="tr" dict={tr} />
      <HomeContact dict={tr} />
    </>
  );
}

describe("Home sections and approved copy (AC-2, AC-4)", () => {
  it("shouldRenderEverySectionWithTheApprovedCopyAndASingleH1", () => {
    render(<HomeComposition />);

    for (const testId of [
      "home-hero",
      "home-story",
      "home-experience",
      "home-rooms-cta",
      "home-contact",
    ]) {
      expect(screen.getByTestId(testId)).toBeInTheDocument();
    }

    const headings = screen.getAllByRole("heading", { level: 1 });
    expect(headings).toHaveLength(1);
    expect(headings[0]).toHaveTextContent(tr.home.heroTitle);

    expect(screen.getByTestId("home-hero-poster")).toBeInTheDocument();
    expect(screen.queryByTestId("home-hero-video")).not.toBeInTheDocument();
    expect(screen.getByTestId("home-rooms-link")).toHaveAttribute("href", "/tr/rooms");

    expect(screen.getByText(tr.home.story.title)).toBeInTheDocument();
    expect(screen.getByText(tr.home.experience.cards.shelter.title)).toBeInTheDocument();
    expect(screen.getByText(tr.home.experience.cards.breakfast.title)).toBeInTheDocument();
    expect(screen.getByText(tr.home.roomsCta.title)).toBeInTheDocument();
    expect(screen.getByText(tr.home.contact.title)).toBeInTheDocument();

    expect(screen.getByTestId("home-contact-phone")).toHaveAttribute(
      "href",
      `tel:${tr.home.contact.phone.replace(/\s+/g, "")}`,
    );
    expect(screen.getByTestId("home-contact-email")).toHaveAttribute(
      "href",
      `mailto:${tr.home.contact.email}`,
    );
    expect(screen.queryByText("Özgün Taş ve Mağara Mimarisi")).not.toBeInTheDocument();
  });
});
