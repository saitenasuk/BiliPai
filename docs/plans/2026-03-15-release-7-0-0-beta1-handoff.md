# 7.0.0 Beta1 Handoff

Date: 2026-03-15
Version: 7.0.0 Beta1
Status: Beta handoff, continue tomorrow

## What Landed

- Fixed audio playlist shuffle so one random cycle does not repeatedly reuse the same small subset.
- Removed phone login UI entry and kept QR login as the single visible login path, with reason text on the login screen.
- Tightened bottom bar, home header, dynamic feed cards, and horizontal UP list spacing to improve space efficiency.
- Fixed dark-mode bottom bar color when blur/glass effects are disabled.
- Fixed fullscreen `4:3` aspect ratio switching and kept other aspect ratio modes on the new viewport policy.
- Started the first phase of app smoothness and network optimization:
  - Restored HTTP/2 on the shared network stack.
  - Raised API HTTP cache budget.
  - Reused the shared network stack for SponsorBlock requests.
  - Reduced dynamic screen startup request fanout.
  - Reduced home background preload budget.
  - Moved dynamic comment/sub-reply overlay state off the main feed tree.
  - Aggregated top-tab, app-navigation, and player-interaction settings to reduce root-level Compose subscriptions.

## Current Verification State

- Focused policy/unit tests for dynamic screen state, home settings/top tabs, player interaction settings, network client policy, SponsorBlock client policy, and startup policy were run during this cycle.
- Full project compile/test remains noisy because local Gradle/Kotlin incremental caches on this machine can still become unstable between runs.
- Latest targeted verification passed with:

```bash
./gradlew --no-daemon -Dkotlin.incremental=false -Dkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest --tests com.android.purebilibili.core.store.HomeSettingsMappingPolicyTest --tests com.android.purebilibili.core.store.HomeTopTabSettingsMappingPolicyTest --tests com.android.purebilibili.core.store.PlayerInteractionSettingsMappingPolicyTest --tests com.android.purebilibili.feature.dynamic.DynamicScreenStatePolicyTest
```

## Recommended Next Steps

1. Continue state isolation in `VideoDetailScreen` by moving dialog/overlay flows off the root screen.
2. Add a dedicated aggregated settings model for the remaining video detail overlays if the root screen still has too many direct subscriptions after step 1.
3. Do one device-level measurement pass:
   - Home and dynamic scroll `gfxinfo`
   - Startup/request fanout comparison
   - Video detail entry interaction pass
4. If local build instability persists, clean up the Gradle/Kotlin cache workflow as a separate environment task instead of mixing it into feature work.

## Watch Items

- `HomeScreen.kt` still carries a large amount of state and side effects even after the latest settings aggregation.
- `VideoDetailScreen.kt` remains the biggest single recomposition surface in the app.
- Local build cache instability is environmental; do not confuse it with feature regressions without a direct source-level error.
