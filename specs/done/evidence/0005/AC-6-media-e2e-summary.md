# 0005 Media E2E Summary

- authorized SUPER_ADMIN login: passed
- invalid SVG (415) and oversized upload (400) rejection: passed
- real WebP upload (`home-hero-poster-1280.webp`) -> PROCESSING -> READY: passed
- TR/EN alt text saved through the admin UI: passed
- room type assignment, publish, cover: passed
- public `room-card-image` WebP, TR alt and EN alt: passed
- public API `altTextTr`/`altTextEn` fields: passed
- FRONT_DESK admin media 403: passed
- unpublish -> placeholder: passed
- archive + confirmation: passed
- loading/error admin states: covered by Vitest/RTL (see `AC-4-admin-media-states.md`)