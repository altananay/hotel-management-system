-- Repeatable development/CI seed for catalog module (REQ-5, AC-5, QA-1, QA-9, QA-17, QA-18).
-- Executed explicitly as migrator outside standard production Flyway locations.

-- Local/CI-only guard: the hotel_core database comment is set to 'local-ci-seed'
-- by infra/postgres/init on empty local/CI volumes and is intentionally absent in
-- production. The marker is read from the target database itself, so overriding
-- Flyway's effective URL (FLYWAY_URL, -Dflyway.url, config files, or initSql)
-- cannot apply seed data to prod.
SELECT 1 / CASE
    WHEN shobj_description(
        (SELECT oid FROM pg_database WHERE datname = current_database()),
        'pg_database'
    ) = 'local-ci-seed' THEN 1
    ELSE 0
END;

INSERT INTO catalog.amenities (id, code, name_tr, name_en, updated_at)
VALUES
    (
        '0199f2c0-0000-7000-8000-000000000201',
        'wifi',
        'Yüksek Hızlı Kablosuz İnternet',
        'High-Speed Wireless Internet',
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000000202',
        'cave_jacuzzi',
        'Özel Taş Oyma Jakuzi',
        'Private Carved-Stone Jacuzzi',
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000000203',
        'terrace_view',
        'Panoramik Vadi ve Balon Terası',
        'Panoramic Valley & Balloon Terrace',
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000000204',
        'stone_fireplace',
        'Geleneksel Taş Şömine',
        'Traditional Stone Fireplace',
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000000205',
        'espresso_bar',
        'Özel Seçki Kahve ve Çay Köşesi',
        'Artisan Espresso & Tea Bar',
        NOW()
    )
ON CONFLICT (id) DO UPDATE
SET
    code = EXCLUDED.code,
    name_tr = EXCLUDED.name_tr,
    name_en = EXCLUDED.name_en,
    updated_at = NOW();

INSERT INTO catalog.room_types (
    id,
    name_tr,
    name_en,
    description_tr,
    description_en,
    max_guests,
    active,
    display_order,
    updated_at
)
VALUES
    (
        '0199f2c0-0000-7000-8000-000000000301',
        'Kemerli Mağara Süit',
        'Arched Cave Suite',
        'Tarihi tüf kayalara oyulmuş yüksek kemerli tavanı, el dokuması Kapadokya halıları ve vadiye açılan dingin atmosferiyle çiftler için otantik bir konaklama deneyimi sunar.',
        'Carved into historic tuff rock with high vaulted ceilings, hand-woven Cappadocian rugs, and a serene atmosphere opening to the valley for an authentic stay.',
        2,
        TRUE,
        10,
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000000302',
        'Peri Bacası Manzaralı Deluxe Taş Oda',
        'Fairy Chimney View Deluxe Stone Room',
        'Gün doğumunda yükselen sıcak hava balonlarını izleyebileceğiniz özel terası, doğal taş işçiliği ve taş şöminesiyle Kapadokya ruhunu yaşatan ferah oda.',
        'Spacious stone-crafted room featuring a private terrace to watch sunrise hot-air balloons and a warm stone fireplace capturing the spirit of Cappadocia.',
        3,
        TRUE,
        10,
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000000303',
        'Sultan Panoramik Konak Süiti',
        'Sultan Panoramic Mansion Suite',
        'Geniş aileler ve özel konuklar için tasarlanmış, özel teraslı, taş oyma jakuzili ve şömineli üst kategori konak süiti.',
        'Signature mansion suite designed for families and distinguished guests, complete with a private panoramic terrace, carved-stone jacuzzi, and fireplace.',
        4,
        TRUE,
        20,
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000000304',
        'Restorasyondaki Avlu Odası',
        'Courtyard Room Under Restoration',
        'Mevsimsel bakım ve taş restorasyon çalışmaları nedeniyle geçici olarak satışa kapalı oda kategorisi.',
        'Room category temporarily closed to bookings due to seasonal maintenance and stone restoration works.',
        2,
        FALSE,
        5,
        NOW()
    )
ON CONFLICT (id) DO UPDATE
SET
    name_tr = EXCLUDED.name_tr,
    name_en = EXCLUDED.name_en,
    description_tr = EXCLUDED.description_tr,
    description_en = EXCLUDED.description_en,
    max_guests = EXCLUDED.max_guests,
    active = EXCLUDED.active,
    display_order = EXCLUDED.display_order,
    updated_at = NOW();

DELETE FROM catalog.room_type_amenities
WHERE room_type_id IN (
    '0199f2c0-0000-7000-8000-000000000301',
    '0199f2c0-0000-7000-8000-000000000302',
    '0199f2c0-0000-7000-8000-000000000303',
    '0199f2c0-0000-7000-8000-000000000304'
);

INSERT INTO catalog.room_type_amenities (room_type_id, amenity_id)
VALUES
    -- Kemerli Mağara Süit: cave_jacuzzi, espresso_bar, wifi
    ('0199f2c0-0000-7000-8000-000000000301', '0199f2c0-0000-7000-8000-000000000202'),
    ('0199f2c0-0000-7000-8000-000000000301', '0199f2c0-0000-7000-8000-000000000205'),
    ('0199f2c0-0000-7000-8000-000000000301', '0199f2c0-0000-7000-8000-000000000201'),
    -- Peri Bacası Manzaralı Deluxe Taş Oda: stone_fireplace, terrace_view, wifi
    ('0199f2c0-0000-7000-8000-000000000302', '0199f2c0-0000-7000-8000-000000000204'),
    ('0199f2c0-0000-7000-8000-000000000302', '0199f2c0-0000-7000-8000-000000000203'),
    ('0199f2c0-0000-7000-8000-000000000302', '0199f2c0-0000-7000-8000-000000000201'),
    -- Sultan Panoramik Konak Süiti: cave_jacuzzi, stone_fireplace, terrace_view
    ('0199f2c0-0000-7000-8000-000000000303', '0199f2c0-0000-7000-8000-000000000202'),
    ('0199f2c0-0000-7000-8000-000000000303', '0199f2c0-0000-7000-8000-000000000204'),
    ('0199f2c0-0000-7000-8000-000000000303', '0199f2c0-0000-7000-8000-000000000203'),
    -- Restorasyondaki Avlu Odası (pasif): espresso_bar, wifi
    ('0199f2c0-0000-7000-8000-000000000304', '0199f2c0-0000-7000-8000-000000000205'),
    ('0199f2c0-0000-7000-8000-000000000304', '0199f2c0-0000-7000-8000-000000000201')
ON CONFLICT (room_type_id, amenity_id) DO NOTHING;
