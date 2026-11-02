# Artemis TODO

## Cursor tracking

- [x] Track an estimated host cursor position on the client for trackpad modes.
  - [x] Start from a sensible default (viewport center) on connect, mode change, and host resolution change.
  - [x] Accumulate the same scaled relative deltas that are sent through `conn.sendMouseMove(...)`.
  - [x] Clamp the estimate to the host video bounds.
  - [x] Handle absolute mouse mode separately; absolute events re-anchor the estimate.
  - [ ] Add a debug overlay or log so the estimate can be compared with the host-rendered cursor.
- [x] Confirm there is no GameStream cursor-position callback. The host cursor is normally rendered into the captured video; Moonlight/Artemis only receives relative/absolute input acknowledgements, not cursor coordinates.

## Viewport auto-pan

- [x] When the viewport is zoomed and a trackpad mode is active, pan the viewport to keep the estimated cursor visible.
  - [x] Add a safe margin around the viewport edge (currently 12%).
  - [x] Pan only when the cursor would leave the safe area, not on every cursor movement.
  - [x] Do not fight an in-progress two-finger pan or pinch; resume auto-pan after the gesture ends.
  - [x] Non-trackpad mouse modes are left alone. Input-disabled mode still allows hardware mouse tracking, but touch panning does not move the cursor.

## Later

- [ ] Optional absolute cursor re-anchor setting: occasionally send `sendMousePosition(...)` so the host cursor is forced back to the client estimate. This can cancel host-side acceleration drift but may not work in raw-input games.

## Reference notes

- RVNC renders its cursor locally in `CursorView`. The server callback `setCursor(Object bitmap, int hotspotX, int hotspotY)` supplies only the cursor bitmap and hotspot; RVNC updates the cursor position itself from input coordinates.
- Artemis currently renders the host cursor as part of the decoded video. `sendMouseMove(...)` sends relative deltas without any server cursor-position feedback.
- The "Use local mouse cursor" preference controls the Android system cursor for captured physical mice; it is not a host-cursor overlay for touch/trackpad input.
