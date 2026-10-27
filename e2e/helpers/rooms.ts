import type { Page } from "@playwright/test";

export const SAMPLE_ROOMS = {
  items: [
    {
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
        {
          code: "espresso_bar",
          nameTr: "Özel Seçki Kahve ve Çay Köşesi",
          nameEn: "Artisan Espresso & Tea Bar",
        },
        {
          code: "wifi",
          nameTr: "Yüksek Hızlı Kablosuz İnternet",
          nameEn: "High-Speed Wireless Internet",
        },
      ],
    },
    {
      id: "0199f2c0-0000-7000-8000-000000000302",
      nameTr: "Peri Bacası Manzaralı Deluxe Taş Oda",
      nameEn: "Fairy Chimney View Deluxe Stone Room",
      descriptionTr: "Özel terası ve taş şöminesiyle ferah taş oda.",
      descriptionEn: "Spacious stone room with private terrace and fireplace.",
      maxGuests: 3,
      amenities: [
        {
          code: "stone_fireplace",
          nameTr: "Geleneksel Taş Şömine",
          nameEn: "Traditional Stone Fireplace",
        },
        {
          code: "terrace_view",
          nameTr: "Panoramik Vadi ve Balon Terası",
          nameEn: "Panoramic Valley & Balloon Terrace",
        },
        {
          code: "wifi",
          nameTr: "Yüksek Hızlı Kablosuz İnternet",
          nameEn: "High-Speed Wireless Internet",
        },
      ],
    },
    {
      id: "0199f2c0-0000-7000-8000-000000000303",
      nameTr: "Sultan Panoramik Konak Süiti",
      nameEn: "Sultan Panoramic Mansion Suite",
      descriptionTr: "Konaklamanın en geniş süiti; vadi manzarası ve özel oturma alanı.",
      descriptionEn: "The mansion's largest suite with valley views and a private lounge.",
      maxGuests: 4,
      amenities: [
        {
          code: "cave_jacuzzi",
          nameTr: "Özel Taş Oyma Jakuzi",
          nameEn: "Private Carved-Stone Jacuzzi",
        },
        {
          code: "stone_fireplace",
          nameTr: "Geleneksel Taş Şömine",
          nameEn: "Traditional Stone Fireplace",
        },
        {
          code: "terrace_view",
          nameTr: "Panoramik Vadi ve Balon Terası",
          nameEn: "Panoramic Valley & Balloon Terrace",
        },
      ],
    },
  ],
  page: 0,
  size: 100,
  total: 3,
};

export async function mockRooms(page: Page): Promise<void> {
  await page.route("**/api/room-types*", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(SAMPLE_ROOMS),
    });
  });
}
