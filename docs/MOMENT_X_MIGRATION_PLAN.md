# Moment X Android Migration Plan

This repository is the Moment fork of `element-hq/element-x-android`.

The goal is not to make Element X look or behave like the Classic Android
rewrite. Element X is the preferred baseline when it already has a better
implementation: Compose UI, Appyx navigation, feature modules, and Matrix Rust
SDK bindings should stay X-native.

Classic Android is the reference only for the critical Moment-specific
contracts, primarily WB.ID authentication and production endpoint values. Other
differences should be evaluated case by case. If the server contract can be
fixed in the BFF or backend without weakening Android X, prefer fixing the
contract outside the app.

## Repository Baseline

- Upstream: `https://github.com/element-hq/element-x-android`
- Fork: `https://github.com/simple-matrix-chat/app-android-x`
- Initial upstream head: `20d0d115d4d115ed5bce8c97bf3a1cc9f9e19865`
- Upstream tag at fork time: `v26.05.2`
- Classic Android reference: `simple-matrix-chat/app-android`, `origin/main`

Keep `upstream` configured locally and regularly rebase early Moment X work on
top of Element X while the customization surface is still small.

## Product Strategy

- X-first: keep Element X behavior where it is already cleaner, newer, or
  technically stronger than the Classic rewrite.
- Critical Android customization: WB.ID-first authentication against Moment
  production.
- Prefer standard Matrix behavior for room list, rooms, messages, media, sync,
  and push unless Moment production cannot work without a small adapter.
- Do not port Classic UI chrome, timeline styling, settings screens, AI
  affordances, or BFF-specific assumptions by default.
- If Android X and Classic expect different BFF contracts, first decide whether
  the BFF can support both clients or expose a Matrix-compatible surface.
- Keep Matrix protocol behavior as the compatibility baseline; app code should
  adapt only where the Moment auth/product contract is truly different.

## Production API Contract

Carry these values from Classic Android prod config for the auth and production
connectivity work:

| Concern | Production value |
| --- | --- |
| Homeserver | `https://unmoment.app/` |
| OAuth/BFF base URL | `https://unmoment.app` |
| WB.ID client ID | `wb_m` |
| WB.ID scope | `openid phone read:profile` |
| WB.ID audience | `https://unmoment.app` |
| WB.ID redirect URI | `https://unmoment.app/auth/callback` |
| Android App Link scheme | `https` |
| Android App Link host | `unmoment.app` |
| Android App Link path prefix | `/auth/callback` |
| Push gateway | `https://unmoment.app/api/push/v1/notify` |
| Production pusher app ID | `app.unmoment.android.prod` |
| Stage pusher app ID | `app.unmoment.android.stage` |

The first X port should use the production auth contract, but keep environment
selection explicit so stage/local can be added without touching feature code.
Only introduce Android-side BFF adapters when X cannot use the same Matrix/OIDC
contract directly.

## X Entry Points To Change First

- `appconfig/src/main/kotlin/io/element/android/appconfig/AuthenticationConfig.kt`
- `appconfig/src/main/kotlin/io/element/android/appconfig/MatrixConfiguration.kt`
- `appconfig/src/main/kotlin/io/element/android/appconfig/PushConfig.kt`
- `app/src/main/kotlin/io/element/android/x/oidc/DefaultOAuthRedirectUrlProvider.kt`
- `app/src/main/AndroidManifest.xml`
- `features/login/api`
- `features/login/impl`
- `features/home/impl` only for smoke-path blockers
- `features/roomdetails/impl` only for smoke-path blockers
- `features/messages/impl` only for smoke-path blockers

Do not begin by patching low-level Rust SDK bindings unless a product behavior
is blocked there. Prefer app config, feature presenters, and service wrappers.
Also avoid broad UI rewrites until auth and a minimal Matrix smoke path are
stable.

## Migration Phases

### Phase 0. Fork Hygiene

- Rename app identity to Moment without broad product rewrites.
- Set package/application ID strategy for prod, stage, debug, and local builds.
- Add Moment app icons, display name, and signing placeholders.
- Keep `upstream` remote and document the sync command.
- Verify a clean debug build before auth work.

### Phase 1. Production Connectivity

- Point default account provider/homeserver to `https://unmoment.app/`.
- Configure push gateway and pusher app IDs only if X does not derive them from
  the production homeserver contract.
- Add environment configuration in a single X-native config layer.
- Verify app startup reaches the Moment server without exposing Matrix.org as a
  normal user choice.
- Leave Matrix permalink/public link behavior as X implements it unless a
  production smoke test shows a concrete mismatch.

### Phase 2. WB.ID Authentication

- Replace Element account-provider selection with WB.ID-first sign-in.
- Use Classic Android as behavioral reference:
  - BFF base: `https://unmoment.app`
  - client ID: `wb_m`
  - scope: `openid phone read:profile`
  - audience: `https://unmoment.app`
  - redirect URI: `https://unmoment.app/auth/callback`
- Reuse X OIDC/MAS primitives where possible.
- Register Android App Link handling for `https://unmoment.app/auth/callback`.
- Disable ordinary Matrix password registration/login in the product path.
- Verify session restore, logout, expired auth, and failed callback recovery.

### Phase 3. Minimal Messenger Slice

- Keep the Element X UI intact except for product-critical auth and endpoint
  wording.
- Deliver one production path end-to-end:
  1. Launch app.
  2. Sign in through WB.ID.
  3. Restore existing session.
  4. Show room list.
  5. Open existing DM.
  6. Send and receive text.
- Treat this as the first gate before considering any non-auth product
  adjustments.

### Phase 4. Selective Product Adjustments

- Keep X behavior by default.
- Compare Classic and X only where users hit a real Moment production gap.
- Add public identity, phone visibility, and unified BFF user search only if X
  lacks a Matrix-compatible path for the same product need.
- Adjust profile/settings/privacy screens only after auth and messaging are
  stable.
- Adjust public link/share behavior only if production links fail or expose a
  hard product mismatch.
- Port attachment scope, forwarding, saved messages, and message actions only
  after validating that X's native implementation is insufficient.
- Treat AI features as a separate product decision, not part of the X migration
  baseline.

### Phase 5. SDK-Level Decisions

Review these Classic changes carefully before copying behavior into X. Most
should stay out of X unless a production smoke test proves they are required:

- No-op or hidden crypto/security prompts.
- Media proxy and Matrix API aliasing.
- Push gateway API aliasing.
- Public permalink rewriting.
- Auto-accept invites.

Element X already has a different Rust SDK layer, so these should be validated
against X APIs and server contracts instead of translated line by line.

## First Implementation Branches

Recommended order:

1. `codex/x-moment-branding-config`
2. `codex/x-prod-api-config`
3. `codex/x-wbid-auth`
4. `codex/x-session-restore-smoke`
5. `codex/x-roomlist-dm-text-smoke`

Each branch should keep a small verification note with the exact APK variant and
manual path tested.

## Definition Of Done For The First Port

- Debug build compiles from a clean checkout.
- App launches as Moment.
- WB.ID login succeeds against production API.
- Session restore works after process restart.
- Room list loads from `unmoment.app`.
- Existing DM opens.
- Plain text send/receive works.
- No normal user path advertises Matrix.org as the default server.
- Push registration uses the Moment pusher app ID for the selected environment.
- No Classic UI parity work is included unless needed to pass the auth/session
  smoke path.
