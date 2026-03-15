# 7.0.0 Beta1 Changelog

## Playback

- Fixed random playback for audio/video playlists so shuffle no longer keeps circling the same few tracks.
- Fixed fullscreen `4:3` aspect ratio switching and kept the rest of the aspect ratio modes aligned with the new viewport policy.

## Login

- Removed the phone login UI entry.
- Kept QR login as the visible login path and added on-screen explanation for why QR login is currently required.

## UI Polish

- Tightened bottom bar and top header proportions to improve space usage.
- Reduced excess width and spacing in dynamic feed cards and the horizontal UP list.
- Fixed the dark-mode bottom bar turning white when blur/glass effects are disabled.

## Performance And Network

- Restored HTTP/2 on the shared API client.
- Increased API HTTP cache budget.
- Reused the shared OkHttp stack for SponsorBlock requests.
- Reduced home background preload budget.
- Reduced dynamic page startup fanout by delaying and limiting followings hydration.
- Moved dynamic comment and sub-reply overlay state off the main feed tree.
- Aggregated top-tab, app-navigation, home easter-egg, and player-interaction settings to reduce root-level Compose subscriptions.

## Testing

- Added or updated focused policy/unit tests covering:
  - network client policy
  - SponsorBlock client policy
  - dynamic startup/state policy
  - home top-tab and home settings mapping
  - player interaction settings mapping
  - playlist shuffle policy
  - video aspect ratio layout policy
