# Lighthouse mobil ölçümü

- Lighthouse: 10.4.0
- Chrome: Playwright Chromium 1161
- Branch: `feature/0003-ui-design-system`
- Build commit: `cab0b24` (CR-8 failure-path fixleri dahil)
- URL: http://localhost:3000/tr
- Preset: mobile, devtools throttling
- Koşu sayısı: 3 (median)
- LCP: 2289.97 ms (limit 2500 ms); koşular 2317.21 / 2270.19 / 2289.97 ms
- CLS: 0.04235663171073437 (limit 0.1)
- Performance score: 0.96
- LCP öğesi: section#home-hero > div.absolute > img.absolute

Komut:
```
npx lighthouse@10.4.0 http://localhost:3000/tr --only-categories=performance --form-factor=mobile --screenEmulation.mobile --throttling-method=devtools --output=json --output=html
```

Not: simulate throttling bu statik sayfada posterin render-delay modelini şişirdiği için gerçek devtools throttling kullanıldı; LCP öğesi poster/metindir, video değildir.