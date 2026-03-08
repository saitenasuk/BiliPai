# Comment Scroll Performance And UP Like Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Improve comment-sheet scrolling smoothness and add desktop-aligned `UP主觉得很赞` handling under special comments.

**Architecture:** Keep the existing comment repository, view model, and Compose screen structure intact. Extend response parsing for desktop comment-label/config fields, introduce small pure policy helpers for special-label resolution and hot-path item derivations, then wire the Compose list to use those helpers and lighter lazy-list settings.

**Tech Stack:** Kotlin, kotlinx.serialization, Jetpack Compose, StateFlow, ViewModel, Gradle unit tests

---

### Task 1: Parse desktop comment label fields

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/data/model/response/ResponseModels.kt`
- Test: `app/src/test/java/com/android/purebilibili/data/model/response/CommentSpecialLabelParsingTest.kt`

**Step 1: Write the failing test**

Cover:

- comment payload parses `card_label[].text_content`
- comment payload parses config `show_up_flag`
- existing `up_action.like` still parses alongside the new fields

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests 'com.android.purebilibili.data.model.response.CommentSpecialLabelParsingTest'`

Expected: FAIL because the new response fields do not exist yet.

**Step 3: Write minimal implementation**

Add minimal serializable models and properties for:

- top-level comment config carrying `show_up_flag`
- per-comment `card_label` entries carrying `text_content`

Keep defaults defensive so malformed payloads do not break comment rendering.

**Step 4: Run test to verify it passes**

Run the same command and expect PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/data/model/response/ResponseModels.kt app/src/test/java/com/android/purebilibili/data/model/response/CommentSpecialLabelParsingTest.kt
git commit -m "test: cover comment special label parsing"
```

### Task 2: Add pure special-label resolution policy

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/components/ReplyComponentsPolicyTest.kt`

**Step 1: Write the failing test**

Cover:

- server `card_label` text wins over fallback
- blank labels are ignored
- fallback `UP主觉得很赞` appears only when `show_up_flag=true` and `up_action.like=true`
- no label is shown for ordinary comments

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests 'com.android.purebilibili.feature.video.ui.components.ReplyComponentsPolicyTest'`

Expected: FAIL until the label-resolution helper exists.

**Step 3: Write minimal implementation**

Add a pure helper such as `resolveReplySpecialLabelText(...)` that accepts the parsed labels, config flag, and `up_action` state and returns one display string or `null`.

**Step 4: Run test to verify it passes**

Run the same command and expect PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt app/src/test/java/com/android/purebilibili/feature/video/ui/components/ReplyComponentsPolicyTest.kt
git commit -m "test: cover comment special label policy"
```

### Task 3: Reduce comment item hot-path work

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/components/ReplyComponentsPolicyTest.kt`

**Step 1: Write the failing test**

Extend policy coverage for:

- optimistic like-count derivation
- normalized location text derivation
- inline sub-reply prefix generation

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests 'com.android.purebilibili.feature.video.ui.components.ReplyComponentsPolicyTest'`

Expected: FAIL because the new helpers do not exist yet.

**Step 3: Write minimal implementation**

Extract pure helpers for the derived values now calculated inline in `ReplyItemView`, then wire the composable to use those helpers and stable remembered inputs instead of rebuilding the same work ad hoc in the render body.

**Step 4: Run test to verify it passes**

Run the same command and expect PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt app/src/test/java/com/android/purebilibili/feature/video/ui/components/ReplyComponentsPolicyTest.kt
git commit -m "refactor: stabilize comment item derived state"
```

### Task 4: Wire label config into comment state and render the lightweight chip row

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/viewmodel/VideoCommentViewModel.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/components/ReplyComponentsPolicyTest.kt`

**Step 1: Write the failing test**

Cover:

- comment rows render a label when the resolved text is non-null
- fallback label remains gated by the comment config flag stored from loaded data

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests 'com.android.purebilibili.feature.video.ui.components.ReplyComponentsPolicyTest'`

Expected: FAIL until the state wiring and chip-row render path exist.

**Step 3: Write minimal implementation**

- Store `show_up_flag` in `CommentUiState`
- Populate it when comments load
- Pass the flag into `ReplyItemView`
- Render a lightweight text-chip row between body/media and footer actions

Do not add extra animation, network work, or layout branches beyond what is needed for the label row.

**Step 4: Run test to verify it passes**

Run the same command and expect PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/viewmodel/VideoCommentViewModel.kt app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt app/src/test/java/com/android/purebilibili/feature/video/ui/components/ReplyComponentsPolicyTest.kt
git commit -m "feat: show desktop-aligned up-like comment label"
```

### Task 5: Tighten lazy-list reuse for the portrait comment sheet

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/pager/PortraitCommentSheet.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt`
- Test: `app/src/test/java/com/android/purebilibili/feature/video/ui/components/ReplyComponentsPolicyTest.kt`

**Step 1: Write the failing test**

Add or extend a pure helper test for comment-row content typing / render classification if needed, or extend the existing component-policy tests with the row variants used by the lazy list.

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests 'com.android.purebilibili.feature.video.ui.components.ReplyComponentsPolicyTest'`

Expected: FAIL until the new helper or row classification exists.

**Step 3: Write minimal implementation**

- Add `contentType` to the main comment list items
- Keep keys stable
- Ensure the label row and extracted helpers do not introduce extra work during footer-driven pagination

Prefer the smallest change set that reduces list churn without changing behavior.

**Step 4: Run test to verify it passes**

Run the same command and expect PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video/ui/pager/PortraitCommentSheet.kt app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt app/src/test/java/com/android/purebilibili/feature/video/ui/components/ReplyComponentsPolicyTest.kt
git commit -m "perf: reduce comment sheet scroll cost"
```

### Task 6: Run focused regression suite

**Files:**
- No code changes

**Step 1: Run related tests**

Run:

`./gradlew testDebugUnitTest --tests 'com.android.purebilibili.data.model.response.CommentSpecialLabelParsingTest' --tests 'com.android.purebilibili.feature.video.ui.components.ReplyComponentsPolicyTest' --tests 'com.android.purebilibili.feature.video.viewmodel.CommentPaginationPolicyTest'`

Expected: PASS

**Step 2: Build the app module**

Run:

`./gradlew :app:compileDebugKotlin`

Expected: PASS

**Step 3: Commit**

```bash
git add
git commit -m "feat: optimize comment scroll and add up-like label"
```
