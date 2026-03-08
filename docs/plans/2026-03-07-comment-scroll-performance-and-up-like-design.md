# Comment Scroll Performance And UP Like Design

**Date:** 2026-03-07

**Goal:** Improve comment-sheet scrolling smoothness and add desktop-aligned `UP主觉得很赞` handling under special comments without broad architectural churn.

## Context

Current comment rendering already parses `up_action.like`, but the UI does not expose the desktop-style "UP主觉得很赞" signal in a unified way. The response model also does not yet expose the richer desktop comment-label fields and config flags that the desktop API documents for special comments.

At the same time, the comment sheet is still doing more per-item work than necessary during scroll:

- [`ReplyComponents.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt) builds several derived values inside each item render path, including emote merge maps, IP text, like-count display text, inline sub-reply prefixes, and rich-text match scanning.
- [`PortraitCommentSheet.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/ui/pager/PortraitCommentSheet.kt) uses a simple `LazyColumn` item setup that can be tightened for reuse and lower scroll cost.
- [`ResponseModels.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/data/model/response/ResponseModels.kt) already includes `ReplyItem.upAction`, but it does not yet model desktop comment labels or the server-side `show_up_flag` switch.

The user explicitly prioritized pure performance improvements over broader UI redesign, with visual structure staying effectively the same.

## API Alignment

Desktop comment API documentation shows two relevant signals:

- comment config includes a `show_up_flag` style switch controlling whether the UP-liked label should be shown
- comment items may include `card_label` entries whose `text_content` can directly carry desktop label text such as `UP主觉得很赞`

The safest client behavior is therefore:

1. parse server-provided special labels first
2. render server label text when present
3. only fall back to a local `UP主觉得很赞` label when `show_up_flag` permits it and `up_action.like=true`

This avoids inventing a custom rule that could diverge from desktop behavior.

## Options Considered

### Option 1: Performance-first targeted fixes

- Keep the current repository and comment-screen structure.
- Add missing response fields for special-comment labels and config.
- Introduce a small pure policy/helper layer for label resolution and hot-path render derivations.
- Apply localized `LazyColumn` reuse improvements and per-item computation reductions.

Pros:

- Lowest regression risk
- Best fit for the user's "pure performance" priority
- Fastest path to measurable scroll improvement

Cons:

- Does not cleanly separate all comment presentation concerns into a dedicated UI model

### Option 2: ViewModel-backed comment presentation model

- Map `ReplyItem` into dedicated comment-row UI models before Compose.
- Move rich-text and label derivation out of the composables completely.

Pros:

- Better long-term maintainability
- Stronger control over recomposition inputs

Cons:

- Larger change set
- Higher merge/conflict risk in an already dirty workspace

### Option 3: Aggressive visual trimming

- In addition to Option 1, further reduce or remove image crossfades and some decorative rendering inside the comment list.

Pros:

- Highest possible frame-time reduction

Cons:

- Higher risk of visible UI regression
- Pushes beyond the requested "visual basics unchanged" boundary

**Recommendation:** Option 1.

## Chosen Direction

Implement a narrow, policy-driven change set:

- Extend response models with desktop comment-label and config fields.
- Resolve special-comment labels through a small pure helper that prefers server `card_label` text and falls back to `up_action.like` only when allowed by config.
- Render the resolved label in a lightweight chip row placed between comment body/media and the footer action row.
- Keep the current comment UI structure, but reduce scroll-path work by extracting and stabilizing repeated derivations and by tightening lazy-list reuse hints.

## Data Flow

1. Repository fetches comment payloads as it does today.
2. Serialization layer now captures:
   - top-level comment config needed for label gating
   - per-comment special labels
3. ViewModel stores the config flag in comment UI state once per load sequence.
4. Compose comment rows resolve display data through pure helpers:
   - special label text
   - display location text
   - optimistic like count
   - sub-reply preview prefix
   - renderable emote set
5. `ReplyItemView` renders only the already-resolved lightweight row when a label should be shown.

## Rendering Rules

### Special comment detection

A comment is considered "special" for this feature when it has at least one renderable `card_label` entry from the API.

### Label priority

1. Use the first renderable server `card_label.text_content`.
2. If no renderable server label exists, use `UP主觉得很赞` only when:
   - comment config says `show_up_flag = true`
   - `ReplyItem.upAction?.like == true`
3. Otherwise render no extra label row.

### Placement

Render the label row:

- after comment text and optional images
- before the time/location + action footer

This keeps the new signal visible while preserving the current list rhythm.

## Performance Strategy

### Hot-path reduction

Extract pure helpers in or near [`ReplyComponents.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt) for:

- comment label resolution
- location display normalization
- optimistic like-count calculation
- inline sub-reply prefix creation
- any rich-text precomputation that can be expressed from stable inputs

Keep each helper deterministic and cheap so it is easy to cover with unit tests.

### Compose list tightening

- Add `contentType` to comment `LazyColumn` items where beneficial.
- Avoid rebuilding unnecessary mutable maps or annotated prefixes more often than needed.
- Keep the special-label UI text-only and animation-free.
- Preserve existing layout hierarchy unless removing a recomposition trigger yields a clear benefit.

## Error Handling

- Missing or malformed `card_label` data should be ignored without affecting comment rendering.
- `show_up_flag=false` suppresses fallback UP-like labels even if `up_action.like=true`.
- If server labels exist but are blank after trimming, they are ignored.
- Main comment content, image preview, like/reply actions, and sub-reply expansion continue to work unchanged if label parsing fails.

## Testing Strategy

- Add response-model parsing tests for:
  - `card_label`
  - `show_up_flag`
  - `up_action`
- Add pure helper tests for:
  - server-label priority
  - fallback label gating
  - malformed/blank label handling
  - optimistic like-count calculation
- Add component-policy tests for the new lightweight label resolution behavior.
- Run focused unit tests for response parsing, comment component policies, and comment pagination/viewmodel behavior affected by the new config field.
