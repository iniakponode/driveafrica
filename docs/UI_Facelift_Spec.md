## Safe Drive Africa UI Facelift Spec

### 1. Goals
- Refresh the visual language so the app feels like a commercial product rather than a research proof-of-concept.
- Guide users through a polished splash → welcome → disclaimer → onboarding flow that mirrors the backend driver profile onboarding lifecycle.
- Ensure the navigation chrome vanishes while these entry screens run and reappears only after a driver profile is established.
- Address Google Play policy concerns (play-ready splash, transparent use of sensitive permissions, no unnecessary storage access, no systemic cleartext traffic).

### 2. Updated Theme
| Token | Light Value | Dark Value | Notes |
| --- | --- | --- | --- |
| `primary` | `#0E8F79` | `#6CE8CD` | Deep teal used for buttons, hero illustrations, and the navigation indicator. |
| `onPrimary` | `#FFFFFF` | `#00382B` | White text on primary backgrounds. |
| `secondary` | `#F9A826` | `#FFD360` | Accent for chips and supportive buttons (e.g., “Daily check-in”). |
| `background` | `#F6F5F9` | `#0F1114` | Very pale lavender / charcoal to keep the focus on cards. |
| `surface` | `#FFFFFF` | `#16191E` | Cards and sheets; higher elevation uses `surfaceTint`. |
| `error` | `#B00020` | `#FF8A80` | Material defaults for form validation. |

**Typography**
- Use a geometric sans (e.g., `FontFamily.SansSerif` with medium/bold weights) for headings, paired with a friendly `bodyLarge` weight.
- Increase headline spacing for the welcome screen so the copy breathes on tall devices.

**Shapes**
- Rounded corners (12–16 dp) on cards and buttons.
- Elevated surfaces use `shadow` with `MaterialTheme.shapes.medium`.

### 3. Splash & Welcome Flow
1. **Launch**: Android 12+ splash uses a themable window background gradient (`#EEF7F4` → `#F2F0F6`) with a centered `sda_2` mark.
2. **Animated signal**: After 1.5 s, fade to a `WelcomeScreen` card that introduces the brand (“Safe Driving, African roads”) with three feature bullets (“Trip detection,” “Daily checks,” “Actionable insights”).
3. **CTA**: “Get started” moves to disclaimer; the bottom nav/top bar stay hidden until onboarding completes.

### 4. Onboarding Aligning with Backend
1. **Disclaimer**: Short reminder about safe driving, notification triggers, and the privacy policy (kept from the old design but reformatted into letter-spacing). CTA: “Agree & continue.”
2. **Profile creation**:
   - Ask for the email (unique identifier) and optionally display a secure profile ID the driver can copy for support.
   - On submit: create a local driver profile, store the generated UUID + email, and call `DriverProfileApiRepository.createDriverProfile` to register on the backend. Show inline feedback (success/error) via the snackbar.
   - Allow submission even if offline; the background worker will sync later. The snackbar communicates “Saved locally – will sync once online” when the API call fails.
3. **Permissions + readout**: Stitch in text about required location/activity/notification permissions before the first trip (this still lives inside the sensor screen but a short summary on onboarding keeps users informed).

### 5. Navigation Behavior
- `Scaffold` hides top/bottom bars when the current route is in `{SPLASH, WELCOME, DISCLAIMER, ONBOARDING, ENTRYPOINT}`.
- After onboarding, the bottom navigation exposes `Home`, `Reports`, and `Record Trip` with iconography and haptics similar to Material3 navigation bar items.
- Deep links (via `EXTRA_NAVIGATE_ROUTE`) still work by routing through `DAAppState`.

### 6. Play Policy Compliance
- **Splash**: Use an Android 12+ window background splash to avoid a blank launcher frame; the Compose splash mirrors that design so there’s no abrupt transition.
- **Sensitive permissions**: Location/activity/notification access is explained before services start; `BootReceiver` only triggers when the device grants location access.
- **Storage**: `READ_EXTERNAL_STORAGE` has been removed because the app only reads sensors and doesn’t need media files.
- **Cleartext traffic**: `usesCleartextTraffic` is turned off in `core` so all networking goes over HTTPS (the API base URL is already HTTPS in staging/prod, with a local emulator override added via build config for debug builds).

### 7. Visual References
- Splash layout: gradient background, circular badge, tagline `“Drive with Safe Drive Africa”`, subtle drop shadow.
- Welcome layout: large headline, supporting paragraph, feature chips (icons + text), rounded “Agree & continue” button.
- Onboarding card: email field + “Create profile” button + inline status text.

Deliverables for this pass:
1. Compose theme assets (colors/typography/shapes).
2. Splash/Welcome/Disclaimer/Onboarding routes with the new flow and nav gating logic.
3. Manifest cleanup and permission explanation so the product can move toward Play release readiness.
