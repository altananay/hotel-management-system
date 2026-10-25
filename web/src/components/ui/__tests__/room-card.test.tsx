import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RoomCard } from "../room-card";
import tr from "@/lib/i18n/tr.json";

const room = {
  id: "0199f2c0-0000-7000-8000-000000000301",
  nameTr: "Kemerli Mağara Süit",
  nameEn: "Arched Cave Suite",
  descriptionTr: "Tarihi tüf kayalara oyulmuş yüksek kemerli mağara süit.",
  descriptionEn: "Carved into historic tuff rock with high vaulted ceilings.",
  maxGuests: 2,
  amenities: [
    {
      code: "cave_jacuzzi",
      nameTr: "Özel Taş Oyma Jakuzi",
      nameEn: "Private Carved-Stone Jacuzzi",
    },
  ],
};

const labels = {
  placeholderBadge: tr.rooms.placeholderBadge,
  placeholderCaption: tr.rooms.placeholderCaption,
  capacityTemplate: tr.rooms.capacityBadge,
  amenitiesHeading: tr.rooms.amenitiesHeading,
};

describe("RoomCard (AC-3, B-16)", () => {
  it("shouldRenderThe0002ContractWithoutBeingInteractiveOrShowingPrice", () => {
    render(<RoomCard room={room} locale="tr" labels={labels} />);

    const card = screen.getByTestId("room-card");
    expect(card.tagName).toBe("DIV");
    expect(card).not.toHaveAttribute("role");
    expect(card).not.toHaveAttribute("tabindex");
    expect(card.className).not.toContain("cursor-pointer");
    expect(card.querySelectorAll('[data-testid^="price"]')).toHaveLength(0);
    expect(card.textContent).not.toMatch(/[₺€$]/);

    const scope = within(card);
    expect(scope.getByTestId("room-card-name")).toHaveTextContent("Kemerli Mağara Süit");
    expect(scope.getByTestId("room-card-capacity")).toHaveTextContent("En fazla 2 misafir");
    expect(scope.getByTestId("room-card-placeholder")).toBeInTheDocument();
    expect(scope.getByTestId("room-card-amenity")).toHaveTextContent("Özel Taş Oyma Jakuzi");
  });
});
