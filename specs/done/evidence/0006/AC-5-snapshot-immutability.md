- `PriceSnapshotTest` proves the payload is deterministic and immutable.
- `PricingIT.shouldKeepOldSnapshotUnchangedAndProduceDifferentSnapshot_whenPriceChanges`
  calls `PricingQuoteApi.snapshot()`, changes the base price, and proves the old payload is
  unchanged while the new payload differs.
- `QuoteValidityTest` proves the 15-minute `quotedAt`/`expiresAt` window.
- Persistent snapshot storage (booking side) belongs to the booking spec.
