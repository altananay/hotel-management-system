# 0005 Admin Media States

- empty state: captured in `AC-4-admin-media-states.png`
- loading -> empty transition: covered by `admin-web/src/features/media/__tests__/media-manager.test.tsx` (`shouldRenderLoadingThenEmptyState_whenNoMediaExists`)
- list error + retry: covered by the same suite (`shouldRetryLoad_genericErrorWithoutLeakingDetails_whenBackendFails`)
- room-type options error + disabled assignment: covered by the same suite (`shouldShowRoomTypeOptionsErrorAndDisableAssignment_whenOptionsFail`)