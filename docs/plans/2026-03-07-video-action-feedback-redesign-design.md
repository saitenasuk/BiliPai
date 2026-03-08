# Video Action Feedback Redesign Design

**Date:** 2026-03-07

**Goal:** Redesign like / coin / favorite feedback in video detail so the interaction feels more refined, uses a unified theme color system, and presents triple-action success with an elegant premium animation instead of a loud celebration.

## Context

Current inspection shows three issues in the video detail action experience:

- [`VideoDetailScreen.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt) renders like success, triple success, and generic action messages in the visual center of the screen, which competes with video content and detail text.
- [`VideoActionSection.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoActionSection.kt) gives like, coin, and favorite separate saturated accent colors, so the action row feels visually fragmented instead of belonging to one system.
- [`CelebrationAnimations.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/ui/components/CelebrationAnimations.kt) currently uses emoji and firework-like particles for triple success, which feels playful but not premium.

The requested direction is a unified design language across portrait and fullscreen playback, with separate placement and sizing adaptations per mode.

## Options Considered

### Option 1: Minimal polish

- Move the popup lower on screen
- Recolor coin and favorite to be closer to theme primary
- Keep the current celebration structure with lighter particle tuning

Pros:

- Small implementation surface
- Lowest regression risk

Cons:

- Still feels like an iteration on the old design
- Does not fully solve the "not coordinated" visual problem

### Option 2: Unified bottom feedback system

- Replace center popups with a bottom floating feedback host
- Unify like / coin / favorite active states to the app theme primary
- Redesign triple success as a restrained "merge into badge" animation

Pros:

- Strongest sense of polish
- Scales cleanly across portrait and fullscreen variants
- Matches the requested premium and elegant feel

Cons:

- Requires coordinated updates across layout, motion, and feedback rules

### Option 3: Full celebratory overlay

- Add a semi-fullscreen celebration layer for triple success
- Use hero motion and large-scale visual emphasis

Pros:

- Most memorable

Cons:

- Too disruptive for video playback
- Easier to feel flashy rather than elegant

**Recommendation:** Option 2.

## Chosen Direction

Introduce a single "action feedback language" for video interactions:

- Action buttons express state through one shared theme color family rather than separate semantic colors.
- Success feedback appears in a bottom floating capsule instead of the center of the screen.
- Triple action uses a short, refined aggregation animation where the three actions light up, converge, and resolve into a compact completion mark.

The result should feel calm, precise, and premium rather than celebratory in a loud or game-like way.

## Interaction Design

### 1. Action row state model

For like, coin, and favorite:

- Inactive state uses `MaterialTheme.colorScheme.onSurfaceVariant`.
- Active state uses `MaterialTheme.colorScheme.primary`.
- Active text count uses the same hue family with lower emphasis than the icon.
- Press state uses `primaryContainer` or a softened primary tint, not a completely different color.

This keeps the row readable while letting theme selection remain the dominant brand expression.

### 2. Success feedback placement

Replace center feedback surfaces with a bottom floating feedback capsule.

Portrait detail:

- Anchor the capsule above the lower action area and above the system navigation inset.
- Keep it clear of the recommendation list and avoid covering the video viewport.

Fullscreen / landscape:

- Anchor near the lower edge or near the action cluster, depending on available safe area.
- Avoid the progress bar, playback controls, and gesture-heavy zones.

The feedback capsule should behave like a lightweight overlay, not a modal. It confirms success without interrupting browsing or playback.

### 3. Message hierarchy

Use one shared feedback host for:

- Like success / unlike
- Coin success
- Favorite success / remove
- Triple in progress
- Triple success

Messages should be short and factual:

- `已点赞`
- `投币成功`
- `已收藏`
- `三连完成`

If a richer summary is needed, it should remain secondary to the motion and never expand into a tall card.

## Visual Design

### 1. Color system

Like, coin, and favorite should no longer compete through pink, gold, and yellow.

Instead:

- Use `MaterialTheme.colorScheme.primary` as the shared active hue.
- Differentiate actions through icon shape and subtle material treatment rather than separate colors.
- If needed, apply only extremely small tonal variation inside the same hue family, such as a faint highlight or glow, but not a new accent color.

This lets custom themes in [`Color.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/core/theme/Color.kt) drive the personality consistently.

### 2. Feedback capsule

The bottom feedback host should look like a premium transient overlay:

- Rounded capsule shape
- Dark translucent or haze-backed surface
- White or high-contrast text
- Small leading icon
- Soft shadow and restrained glow

It should feel lighter than a dialog and more intentional than a stock toast.

### 3. Triple completion mark

Avoid emoji, fireworks, and confetti.

The completion state should use:

- A compact center glyph or badge
- A thin ring or arc accent
- Gentle soft light instead of explosion particles
- Crisp typography for the completion text

The visual reference is "precision and finish", not "party".

## Motion Design

### Triple animation concept: "Merge into completion"

The triple motion runs in four short phases:

1. Activation
- Like, coin, and favorite illuminate in sequence or near-sequence.
- Each icon scales up slightly with a soft spring and settles quickly.

2. Convergence
- Three illuminated elements move inward along short arcs toward a shared center.
- A thin primary-colored trail or blur hint can follow the motion, but it must stay subtle.

3. Resolution
- The converged motion forms a compact completion badge or ring.
- The `三连完成` label fades in or rises in with minimal overshoot.

4. Dissolve
- The completion badge softens and collapses into the bottom feedback capsule.
- The capsule remains for a short dwell, then fades out.

### Motion timing

- Single action feedback: about 220-320ms
- Triple merge animation: about 700-900ms
- Completion dwell before fade: about 1.2-1.6s

Animations should prioritize ease-out and low-amplitude spring behavior. No bounce-heavy or rubbery movement.

### Reduced motion behavior

If animation level is reduced:

- Skip the convergence sequence
- Immediately activate the three states
- Show only the bottom capsule with `三连完成`

## Adaptive Layout Rules

### Portrait detail

- Keep the action row visually grounded in the content column.
- Run the triple convergence motion from the action row center area.
- Keep the feedback capsule centered horizontally near the bottom.

### Fullscreen / landscape

- Shrink motion scale and vertical travel distance.
- Prefer local animation near the visible action cluster instead of a large center-stage overlay.
- Reduce opacity and dwell time so playback remains the focus.

### Future extensibility

The same feedback host should be reusable for other low-interruption action confirmations in player surfaces if needed later.

## Implementation Areas

- [`VideoActionSection.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoActionSection.kt)
  - unify active color behavior
  - expose any geometry or anchors needed by the new triple motion
- [`VideoDetailScreen.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt)
  - replace center popup placement with a bottom feedback host
  - adapt placement between portrait and fullscreen layouts
- [`CelebrationAnimations.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/ui/components/CelebrationAnimations.kt)
  - replace emoji/fireworks celebration with the merge-based motion
- [`PlayerViewModel.kt`](/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/video/viewmodel/PlayerViewModel.kt)
  - keep the event source intact, but ensure message semantics match the new feedback host

## Testing Strategy

### Unit / policy tests

- Add a feedback placement policy test for portrait vs fullscreen placement rules.
- Add a motion spec test for triple timing and reduced-motion fallback.
- Add a visual-state policy test for action button tint resolution so active actions always map to theme primary.

### Focused verification

- Portrait detail: like, coin, favorite, long-press triple
- Landscape fullscreen: same actions while controls are visible
- Reduced-animation setting: verify graceful fallback
- Dark and light theme: confirm capsule contrast and active-state readability

## Success Criteria

- Success feedback no longer appears in the center of the screen.
- Like, coin, and favorite feel visually unified under the current theme color.
- Triple success feels premium and elegant rather than playful or noisy.
- Portrait and fullscreen share one visual language while remaining correctly adapted to their layouts.
