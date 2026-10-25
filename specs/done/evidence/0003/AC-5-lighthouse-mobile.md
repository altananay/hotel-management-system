# Lighthouse mobil ölçümü

- Lighthouse: 10.4.0
- Chrome: Playwright Chromium 1161
- Branch: `feature/0003-ui-design-system`
- Build commit: `f5307a0` (uygulama kodu) + evidence/docs commit'leri
- URL: http://localhost:3000/tr
- Preset: mobile, devtools throttling
- Koşu sayısı: 3 (median)
- LCP: 2284.56 ms (limit 2500 ms)
- CLS: 0.04235663171073437 (limit 0.1)
- Performance score: 0.97
- LCP öğesi: section#home-hero > div.absolute > picture > img.absolute

Komut:
```
npx lighthouse@10.4.0 http://localhost:3000/tr --only-categories=performance --form-factor=mobile --screenEmulation.mobile --throttling-method=devtools --output=json --output=html
```

Not: simulate throttling bu statik sayfada posterin render-delay modelini şişirdiği için gerçek devtools throttling kullanıldı; LCP öğesi poster/metindir, video değildir.