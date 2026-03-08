# Video Action Feedback Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rebuild video detail like / coin / favorite feedback into a unified bottom feedback system with theme-colored active states and an elegant triple-action animation that adapts to portrait and fullscreen layouts.

**Architecture:** Keep the existing `PlayerViewModel` action event flow, but replace the center popup presentation in `VideoDetailScreen` with a reusable bottom feedback host. Move action-state coloring and triple-motion timing into small policy helpers so the Compose UI stays declarative and reduced-motion behavior can be tested without relying on rendering tests.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, StateFlow, existing video detail policy test pattern, Gradle unit tests

---

### Task 1: Add feedback visual policy helpers

**Files:**
- Create: `app/src/main/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicy.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicyTest.kt`

**Step 1: Write the failing test**

Add tests that define:

- inactive action tint resolves to `onSurfaceVariant`
- active like / coin / favorite tint resolves to `primary`
- portrait feedback anchor resolves to bottom-centered placement
- fullscreen feedback anchor resolves to a safe-area-aware bottom or side placement

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.VideoActionFeedbackPolicyTest"`

Expected: FAIL because the new policy file and helpers do not exist yet.

**Step 3: Write minimal implementation**

Implement small pure helpers for:

- `resolveVideoActionTint(isActive: Boolean, activeColor: Color, inactiveColor: Color): Color`
- `resolveVideoActionCountTint(...)`
- `resolveVideoFeedbackPlacement(isFullscreen: Boolean, isLandscape: Boolean, bottomInsetDp: Int): VideoFeedbackPlacement`

Keep the API pure and dependency-light so it is easy to unit-test.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.VideoActionFeedbackPolicyTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicy.kt app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicyTest.kt
git commit -m "test: add video action feedback policy"
```

### Task 2: Add triple motion spec helpers

**Files:**
- Create: `app/src/main/java/com/android/purebilibili/feature/video/ui/feedback/TripleActionMotionSpec.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/TripleActionMotionSpecTest.kt`

**Step 1: Write the failing test**

Add tests that define:

- normal motion contains activation, convergence, resolution, and dissolve phases
- total duration stays within the designed window
- reduced motion resolves directly to the completion capsule path

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.TripleActionMotionSpecTest"`

Expected: FAIL because the spec file does not exist.

**Step 3: Write minimal implementation**

Create a small spec model such as:

```kotlin
data class TripleActionMotionSpec(
    val activationDurationMillis: Int,
    val convergenceDurationMillis: Int,
    val resolutionDurationMillis: Int,
    val dwellDurationMillis: Int,
    val usesConvergence: Boolean
)
```

Add helpers for normal motion and reduced motion variants.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.TripleActionMotionSpecTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/ui/feedback/TripleActionMotionSpec.kt app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/TripleActionMotionSpecTest.kt
git commit -m "test: define triple action motion spec"
```

### Task 3: Refactor action row colors to the unified theme model

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoActionSection.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicyTest.kt`

**Step 1: Write the failing test**

Extend the policy test with the intended active-state contract:

- like, coin, and favorite all resolve through the shared primary-based tint helper
- no hard-coded yellow or orange active colors remain for the three primary actions

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.VideoActionFeedbackPolicyTest"`

Expected: FAIL because the UI still hard-codes separate colors.

**Step 3: Write minimal implementation**

Update `ActionButtonsRow` and its button calls to:

- use the policy helper for active and inactive tint resolution
- keep watch-later and download untouched unless the shared helper cleanly applies there
- remove hard-coded active colors for coin and favorite

Keep the visual emphasis subtle by lowering text emphasis compared with icon emphasis.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.VideoActionFeedbackPolicyTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoActionSection.kt app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicyTest.kt
git commit -m "feat: unify video action button colors"
```

### Task 4: Build a reusable bottom feedback host

**Files:**
- Create: `app/src/main/java/com/android/purebilibili/feature/video/ui/components/VideoActionFeedbackHost.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicyTest.kt`

**Step 1: Write the failing test**

Add tests that define:

- portrait placement uses bottom center with bottom inset spacing
- fullscreen placement switches to the alternate anchor strategy
- reduced-motion mode still allows the host to show text-only confirmations

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.VideoActionFeedbackPolicyTest"`

Expected: FAIL because the feedback host behavior is not implemented.

**Step 3: Write minimal implementation**

Create `VideoActionFeedbackHost` that accepts:

- `message: String?`
- `visible: Boolean`
- `placement: VideoFeedbackPlacement`
- optional leading icon or status style

Wire `VideoDetailScreen` to:

- stop rendering the current center popup for generic action messages
- show the new host near the bottom
- preserve auto-dismiss timing

Leave other dialogs and sheets unchanged.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.VideoActionFeedbackPolicyTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/ui/components/VideoActionFeedbackHost.kt app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicyTest.kt
git commit -m "feat: add bottom video action feedback host"
```

### Task 5: Redesign the triple celebration composable

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/components/CelebrationAnimations.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/TripleActionMotionSpecTest.kt`

**Step 1: Write the failing test**

Extend the motion spec test so it covers:

- convergence is used only in normal motion
- reduced motion bypasses convergence
- dwell duration stays within the approved range

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.TripleActionMotionSpecTest"`

Expected: FAIL until the composable is updated to use the new spec.

**Step 3: Write minimal implementation**

Replace the current emoji / firework composition with:

- sequential or near-sequential action activation
- short inward arc motion
- compact completion badge or ring
- clean `三连完成` label

Adapt scale, travel distance, and opacity for portrait vs fullscreen.

Do not reintroduce emojis or dense confetti particles.

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.TripleActionMotionSpecTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/ui/components/CelebrationAnimations.kt app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/TripleActionMotionSpecTest.kt
git commit -m "feat: redesign triple action celebration"
```

### Task 6: Align view-model message semantics with the new host

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/viewmodel/PlayerViewModel.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicyTest.kt`

**Step 1: Write the failing test**

Add assertions or test helpers that define the new message contract:

- concise confirmation messages for like / coin / favorite
- triple action emits a short in-progress or success message appropriate for the bottom capsule

**Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.VideoActionFeedbackPolicyTest"`

Expected: FAIL because the old summary wording is still in use.

**Step 3: Write minimal implementation**

Update the emitted messages only where needed so they fit the new presentation:

- keep error semantics intact
- keep business logic intact
- avoid long summary strings that would bloat the capsule

**Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.VideoActionFeedbackPolicyTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/viewmodel/PlayerViewModel.kt app/src/test/java/com/android/purebilibili/feature/video/ui/feedback/VideoActionFeedbackPolicyTest.kt
git commit -m "refactor: align video action feedback messages"
```

### Task 7: Run focused verification and update release notes if needed

**Files:**
- Modify: `CHANGELOG.md`
- Verify: `app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoActionSection.kt`
- Verify: `app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt`
- Verify: `app/src/main/java/com/android/purebilibili/feature/video/ui/components/CelebrationAnimations.kt`

**Step 1: Run focused unit tests**

Run:

- `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.VideoActionFeedbackPolicyTest"`
- `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.feedback.TripleActionMotionSpecTest"`

Expected: PASS

**Step 2: Run compile verification**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

**Step 3: Perform manual verification**

Check:

- portrait detail like / coin / favorite feedback placement
- portrait long-press triple animation
- fullscreen placement and scale adaptation
- dark theme readability
- reduced-motion fallback

**Step 4: Update release notes**

Add a short changelog note only if the repository is currently tracking user-facing polish updates for this cycle.

**Step 5: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs: note video action feedback redesign"
```
