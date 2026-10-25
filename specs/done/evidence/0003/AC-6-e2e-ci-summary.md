# 0003 E2E ve CI özeti

- Web unit testleri (Vitest): **39/39 passed**
- 0002 smoke (`@empty` 1 + `@seeded` 3): **4/4 passed**
  - 0002 kanıt kökü geçici dizin kullanıldı; arşiv değişmedi.
- 0003 design (`@design @seeded`): **36/36 passed**
- Axe: altı rotada kritik/ciddi ihlal **0**
- JS bütçesi: **206.261 B / 256.000 B** (gzip-9)
- CLS (scroll): **0.0000**; Lighthouse CLS: **0.042**
- Lighthouse mobil (devtools throttling, 3 koşu median): **LCP 2284,56 ms**,
  performans **0.97**
- Video kanıtı: **8,4 sn / 984.665 B** (VP9, 1280×800)
- `0002` kanıt SHA-256: **UNCHANGED (12 dosya)**

## Komutlar

```bash
# web
cd web && npm run lint && npm test && npm run build

# 0002 regression (geçici kanıt kökü)
cd e2e
E2E_EVIDENCE_DIR="$RUNNER_TEMP/evidence-0002" npx playwright test tests/smoke.spec.ts --grep @empty
# seed...
E2E_EVIDENCE_DIR="$RUNNER_TEMP/evidence-0002" npx playwright test tests/smoke.spec.ts --grep @seeded

# 0003 design
node scripts/sha256-manifest.mjs capture ../specs/done/evidence/0002 "$RUNNER_TEMP/0002-before.json"
npx playwright test tests/design --grep @seeded
node scripts/sha256-manifest.mjs verify ../specs/done/evidence/0002 "$RUNNER_TEMP/0002-before.json" ../specs/done/evidence/0003/AC-6-0002-evidence-sha256.txt
```

## CI

- CI run linki: PR açıldığında `CI Quality Gate` run'ı ile eklenecek.
- CI akışı: `@empty` → seed → `@seeded` smoke (geçici kanıt kökü) →
  SHA manifest → `@seeded` design → SHA doğrulama → 0003 kanıt artefaktı.
