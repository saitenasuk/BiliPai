# Performance And Network Optimization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Improve first-load speed, list smoothness, and player interaction responsiveness without downgrading animations or blur effects.

**Architecture:** Rework the shared networking client first, then reduce first-screen request fanout and root-level recomposition on home/dynamic screens, and finally isolate high-frequency player UI state from the rendering core. Keep visual structure intact while changing request ordering, cache policy, and state ownership.

**Tech Stack:** Kotlin, Jetpack Compose, OkHttp, Retrofit, Coil, Media3 ExoPlayer, Kotlin Coroutines/Flow

---

### Task 1: Rework Shared Network Client

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/core/network/ApiClient.kt`
- Test: `app/src/test/java/com/android/purebilibili/core/network/NetworkClientPolicyTest.kt`

**Step 1: Write the failing test**

- Add a policy-level test verifying the shared client no longer forces HTTP/1.1 only.
- Add a policy-level test verifying API cache budget and connection pool settings match the new targets.

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.core.network.NetworkClientPolicyTest`

Expected: FAIL because the current client still forces `Protocol.HTTP_1_1` and old cache sizing.

**Step 3: Write minimal implementation**

- Extract client tuning constants into testable policy helpers.
- Remove the hard lock to `Protocol.HTTP_1_1`.
- Increase API cache budget.
- Keep existing timeout, DNS, cookie, and request-header behavior intact.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.core.network.NetworkClientPolicyTest`

Expected: PASS

### Task 2: Collapse External One-Off Clients

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/data/repository/SponsorBlockRepository.kt`
- Modify: `app/src/main/java/com/android/purebilibili/core/network/ApiClient.kt`
- Test: `app/src/test/java/com/android/purebilibili/data/repository/SponsorBlockClientPolicyTest.kt`

**Step 1: Write the failing test**

- Add a test that verifies SponsorBlock requests reuse a shared client policy rather than a bespoke default builder.

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.data.repository.SponsorBlockClientPolicyTest`

Expected: FAIL because SponsorBlock currently creates its own standalone `OkHttpClient`.

**Step 3: Write minimal implementation**

- Introduce a derived/shared client path for SponsorBlock with only the repository-specific timeout differences preserved.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.data.repository.SponsorBlockClientPolicyTest`

Expected: PASS

### Task 3: Reduce Dynamic Screen Startup Fanout

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/dynamic/DynamicStartupPolicyTest.kt`

**Step 1: Write the failing test**

- Add a policy test verifying startup loads the main feed first and defers full followings hydration.
- Add a policy test verifying followings are fetched lazily and incrementally rather than pulling up to 5 pages immediately.

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.dynamic.DynamicStartupPolicyTest`

Expected: FAIL because init currently refreshes the feed and also starts loading up to 250 followings immediately.

**Step 3: Write minimal implementation**

- Introduce a startup policy helper.
- Keep cached dynamics restore and first feed refresh.
- Delay full followings fetch until after the main feed settles.
- Reduce the first followings fetch batch and allow later expansion on demand or stale refresh.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.dynamic.DynamicStartupPolicyTest`

Expected: PASS

### Task 4: Tighten Home Screen Background Preload Scope

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/HomePerformancePolicy.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/home/HomePerformancePolicyTest.kt`

**Step 1: Write the failing test**

- Add a test verifying preload count stays conservative and active-page-only under the new performance policy.

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.home.HomePerformancePolicyTest`

Expected: FAIL because current preload strategy still allows broader ahead-of-viewport loading.

**Step 3: Write minimal implementation**

- Reduce preload-ahead defaults.
- Ensure preload logic only follows the active category/page.
- Keep images and visual effects unchanged.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.home.HomePerformancePolicyTest`

Expected: PASS

### Task 5: Isolate Root-Level State Churn On Dynamic Screen

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/dynamic/DynamicScreenStatePolicyTest.kt`

**Step 1: Write the failing test**

- Add a policy test for which state must remain page-root state versus local dialog/comment state.

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.dynamic.DynamicScreenStatePolicyTest`

Expected: FAIL if current state policy still mixes high-churn local state into the root tree.

**Step 3: Write minimal implementation**

- Move local-only state closer to the owning subtree.
- Keep root composable focused on feed, selection, and layout mode state.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.dynamic.DynamicScreenStatePolicyTest`

Expected: PASS

### Task 6: First Verification Sweep

**Files:**
- Test only

**Step 1: Run focused regression suite**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.android.purebilibili.core.network.NetworkClientPolicyTest \
  --tests com.android.purebilibili.data.repository.SponsorBlockClientPolicyTest \
  --tests com.android.purebilibili.feature.dynamic.DynamicStartupPolicyTest \
  --tests com.android.purebilibili.feature.home.HomePerformancePolicyTest \
  --tests com.android.purebilibili.feature.dynamic.DynamicScreenStatePolicyTest
```

Expected: PASS

**Step 2: Run adjacent regression tests**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.android.purebilibili.feature.dynamic.* \
  --tests com.android.purebilibili.feature.home.* \
  --tests com.android.purebilibili.feature.video.* \
  --tests com.android.purebilibili.core.network.*
```

Expected: PASS or a short actionable failure list

### Task 7: Player High-Frequency State Isolation

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerSection.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/FullscreenPlayerOverlay.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerStatePartitionPolicyTest.kt`

**Step 1: Write the failing test**

- Add a policy test that separates render-core state from high-frequency overlay state and low-frequency settings state.

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.video.ui.section.VideoPlayerStatePartitionPolicyTest`

Expected: FAIL until state ownership is split.

**Step 3: Write minimal implementation**

- Extract policy helpers for render-core/high-frequency/low-frequency responsibilities.
- Move high-frequency state closer to overlay/gesture hosts.
- Keep render containers stable.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.video.ui.section.VideoPlayerStatePartitionPolicyTest`

Expected: PASS

### Task 8: Final Verification Sweep

**Files:**
- Test only

**Step 1: Run final targeted suite**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.android.purebilibili.core.network.* \
  --tests com.android.purebilibili.feature.home.* \
  --tests com.android.purebilibili.feature.dynamic.* \
  --tests com.android.purebilibili.feature.video.ui.section.* \
  --tests com.android.purebilibili.feature.video.ui.overlay.*
```

Expected: PASS

**Step 2: Manual verification checklist**

- Open home and confirm first feed appears before non-critical enrichments.
- Open dynamic and confirm the main list appears without waiting for the full following roster.
- Scroll home/dynamic and confirm bottom-bar and list behavior stay stable.
- Open video detail, full-screen player, and mini-player; verify controls, gestures, danmaku, and ratio/quality menus still work.
