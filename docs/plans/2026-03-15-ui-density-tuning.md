# UI Density Tuning Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Tighten oversized chrome and dynamic-feed spacing so the app uses space more efficiently without hurting readability or tap comfort.

**Architecture:** Keep the current component structure and visual language, but move key size and spacing choices into policy-style functions where possible. Tune bottom bar width/height, home top chrome heights/padding, and dynamic feed/list spacing with focused tests so the changes stay intentional and bounded.

**Tech Stack:** Kotlin, Jetpack Compose, existing policy/unit test suite

---

### Task 1: Lock expected density with failing tests

**Files:**
- Modify: `app/src/test/java/com/android/purebilibili/feature/home/components/BottomBarLayoutPolicyTest.kt`
- Modify: `app/src/test/java/com/android/purebilibili/feature/home/components/iOSHomeHeaderVisualPolicyTest.kt`
- Create: `app/src/test/java/com/android/purebilibili/feature/dynamic/DynamicLayoutPolicyTest.kt`

**Step 1: Write failing tests**

- Update bottom bar expectations so floating phone bars are a bit wider with smaller side inset.
- Add home header tests for tighter search/tab heights and slightly reduced horizontal padding.
- Add dynamic layout tests for narrower card padding, tighter horizontal UP list spacing, and slimmer sidebar widths.

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.home.components.BottomBarLayoutPolicyTest --tests com.android.purebilibili.feature.home.components.iOSHomeHeaderVisualPolicyTest --tests com.android.purebilibili.feature.dynamic.DynamicLayoutPolicyTest`

Expected: FAIL on old layout constants.

### Task 2: Implement minimal policy changes

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSidebar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt`

**Step 1: Add or extract policy helpers**

- Expose the home-top heights/padding as small pure functions.
- Expose dynamic spacing and sidebar width choices as pure functions.

**Step 2: Tune bottom bar**

- Slightly widen floating bars on phone.
- Reduce floating height and bottom gap.

**Step 3: Tune home top chrome**

- Reduce search row height, floating tab height, and horizontal padding modestly.

**Step 4: Tune dynamic feed**

- Slightly reduce feed max width on large screens.
- Reduce dynamic card outer and inner horizontal padding.
- Tighten horizontal UP-list padding and spacing.
- Slightly narrow sidebar widths.

### Task 3: Verify

**Files:**
- No additional file changes required.

**Step 1: Run focused tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.feature.home.components.BottomBarLayoutPolicyTest --tests com.android.purebilibili.feature.home.components.iOSHomeHeaderVisualPolicyTest --tests com.android.purebilibili.feature.dynamic.DynamicLayoutPolicyTest`

Expected: PASS

**Step 2: Review diff**

Run: `git diff -- app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt app/src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSidebar.kt app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt app/src/test/java/com/android/purebilibili/feature/home/components/BottomBarLayoutPolicyTest.kt app/src/test/java/com/android/purebilibili/feature/home/components/iOSHomeHeaderVisualPolicyTest.kt app/src/test/java/com/android/purebilibili/feature/dynamic/DynamicLayoutPolicyTest.kt`

Expected: only density-related policy and test updates.
