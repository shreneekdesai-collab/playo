# PlayZo Project Memory

## How to use this file

Read this file fully before doing any work. After EVERY task that changes the
project, update this file before finishing: edit the status sections, add a
line to the Change Log with the date, what changed, and which files, and tick
or untick items in the checklist. Never leave this file out of date. Keep it
concise.

## Project overview

PlayZo is a turf-booking marketplace. Roles are `customer` and `owner`, stored
in `users/{uid}.role`. Stack: Java, Firebase Auth, Firestore, Firebase
Firestore Blobs, Glide, and Material 3. Package: `com.example.playo`.

## Navigation flow

`LoginActivity` (launcher) -> `MainActivity` bottom navigation:
`HomeFragment`, `CategoryFragment`, `ProfileFragment`.

Home/category turf -> `ProductPage` -> `BookSlotActivity`.

Profile menu -> `PersonalDetailsActivity`, `MyListingsActivity`,
`AddTurfActivity`, `MyBookingsActivity`, `FavoritesActivity`,
`NotificationSettingsActivity`, `HelpActivity`, `AboutActivity`,
`SettingsActivity`, or Logout.

## Firestore data model

- `users/{uid}`: `name`, `email`, `mobile`, `gender`, `dob`, `address`, `role`, `createdAt`.
- `turfs/{turfId}`: `turfId`, `ownerId`, `name`, `price`, `location`, `sport`,
  `description`, `contactNumber`, `available`, `thumbnail` (Blob),
  `photoCount`, `rating`, `latitude`, `longitude`, `mapLink`, `createdAt`.
- Photos: `turfs/{turfId}/photos/{index}` with numeric `index` and Blob `data`.
- Reviews: `turfs/{turfId}/reviews/{uid}` with one review per user per turf;
  turf documents store the average `rating` and `reviewCount`.
- Slots: `turfs/{turfId}/slots/{yyyy-MM-dd_HHmm}` with `held` or `booked`
  state, server-timestamped holds, and a five-minute expiry.
- Bookings: `bookings/{bookingId}` stores the user, turf, date, slot, amount,
  payment method/status, `paymentId`, confirmed status, and server `createdAt`.
- Payments: `payments/{paymentId}` stores one attempt with user/owner/turf,
  date/slot, rupee amount, currency, method, attempt number, hold slot,
  lifecycle status, booking link, simulated/gateway IDs, failure details, and
  server timestamps. `payments/{paymentId}/events/{eventId}` stores the audit
  timeline. Statuses are `created`, `processing`, `success`, `failed`,
  `cancelled`, `timeout`, and `needs_review`.
- Operational records: `appErrors/{errorId}` stores bounded, non-personal
  client diagnostics; `supportTickets/{ticketId}` stores user reports linked
  to an optional payment or booking.
- Roles: `users/{uid}.role` is normalized to `customer` or `owner`; missing or
  unknown values default to `customer`.
- Legacy `imageUrls` lists remain readable; new writes omit them.
- Reference Firestore rules exist in `firestore.rules`; they must be pasted into
  Firebase Console and published manually.

## Working features

- Email/password login and signup.
- Profile and personal details from Firestore.
- Add turf with multiple photos.
- New photos are compressed locally and stored as Firestore Blobs; maximum 5 photos.
- My listings with delete.
- Product page with gallery.
- Home and Category turf lists from Firestore.
- Time-slot and payment-method selection UI.
- Settings dark-mode toggle.
- Light and dark palettes are defined in `values/colors.xml` and
  `values-night/colors.xml`; saved theme preference is applied at app startup.
- Reviews and ratings are stored in Firestore from the product page.
- Profile role menu, owner upgrade flow, and owner-only turf screens are enforced.
- Google sign-in is available on Login and Signup; new Google signup profiles use
  the selected role and existing profiles are never overwritten.
- Real slot booking uses deterministic slot IDs, Firestore transactions, and
  rules-backed five-minute holds; slot states and legends are shown live.
- Every payment attempt is persisted before gateway start. The gateway is behind
  `PaymentGateway`; debug builds use `SimulatedGateway` for success, failure,
  cancellation, and slow-payment testing.
- Booking finalization uses one Firestore transaction for the booking, held slot,
  and successful payment status. `ReconcileHelper` checks stale processing or
  incomplete successful payments at app start and from Payment History.

## Stubbed or fake (known)

- Payment uses the simulated gateway until Razorpay plus server verification.
- Payment statuses are written by the app and are not trustworthy until a server
  verifies payments with the gateway.
- No refunds flow, no admin screen, and owner earnings are not built.
- No cancel booking flow yet; holds are not extended during real gateway payment.
- Bookings are not yet tied to slots by a booking-specific rule.
- Payment History reads Firestore attempts and event timelines.
- Favorites, Notifications, and Help are "coming soon".
- Edit listing only passes the name.
- No production payment gateway or server-side payment verification yet.
- No double-booking prevention outside the hold/finalize transaction.
- Reviews are not yet restricted to users who completed a booking; add that
  restriction when real bookings exist.
- Email verification, forgot password, and FCM are not implemented.
- Editing a turf's location waits for the edit flow; old turfs use the address
  text search fallback for map opening.

## Conventions

- Use filled buttons with `@color/button_bg` and `@color/button_text`.
- Never hardcode colors in layouts or drawables; use palette resources.
- Use `colorPrimary` only for accents and input outlines.
- Use `EdgeToEdge.enable(this)` plus inset padding on the top view of every screen.
- Exactly ONE toolbar per layout.
- Hide the bottom navigation bar while the keyboard is open.
- All bottom bars use `layout_bottom_nav`, `BottomNavHelper`, and
  `Widget.PlayZo.BottomNav`; the bar uses theme-aware surface colors, visible
  labels, and a selected active indicator.
- Do not add duplicate listeners.
- Do not rename the package.
- Do not touch `google-services.json`.

## Motion

- Existing motion survey: `BottomNavHelper` translates the bottom bar during
  scroll hide/show; `HomeFragment` fades and translates the collapsing search
  bar; activity tab routing uses fade-through transitions; screen lists and
  forms use shared entrance/press feedback where implemented.
- Foundation plan: centralize animation durations/interpolators and the system
  animation-scale check in `Motion`, `AnimUtils`, and `PressEffect`; use them
  for screen transitions, first-load list entrances, and press feedback only.
- Motion tokens: `DURATION_SHORT=150ms`, `DURATION_MEDIUM=250ms`,
  `DURATION_LONG=350ms`, `STAGGER_STEP=40ms`, and `STAGGER_MAX=8`; all new
  screens must use these Motion tokens and respect the system Remove
  animations setting.
- Foundation implemented: `Motion.enabled(context)`, `AnimUtils` fade/slide,
  shake, count-up, pulse, staggered entrance, and cancellation helpers, plus
  `PressEffect.attach(View)` for non-consuming press scale feedback.
- Part 2 implemented: shimmer loading states and content crossfades on Home and
  Category; animated auth forms and validation feedback; Product gallery/detail
  and review motion; booking chip, countdown, payment, and confirmation motion;
  Profile stat count-up; and Add Turf photo preview transitions.

## Roadmap checklist

- [x] Home and Category read turfs from Firestore
- [x] Firestore photo security rules (reference file exists; publish manually)
- [ ] Real bookings with double-booking prevention (transaction)
- [x] Real bookings with double-booking prevention (transaction)
- [x] My Bookings and Booking Details from Firestore
- [ ] Payment gateway (Razorpay)
- [ ] Owner edit listing, owner dashboard
- [x] Reviews and ratings in Firestore
- [ ] Favorites per user
- [ ] Forgot password, email verification
- [ ] Notifications (FCM)
- [x] Maps location picker
- [ ] Loading, offline and retry handling
- [ ] Release prep (signing, privacy policy, R8)

Payment failures can currently be found by owners in the Firebase Console by
filtering `payments` for `needs_review`, then `failed`, followed by checking
`appErrors` and `supportTickets`.

## Change Log

- 2026-10-10: Created `AGENTS.md`.
- 2026-10-10: Home and Category now load Firestore turfs with loading, empty, error, retry, sorting, and image URL support; updated `Turfname.java`, `CustomAdapter.java`, `HomeFragment.java`, `OnclickCategory.java`, `fragment_home.xml`, and `onclick_category.xml`.
- 2026-10-10: Added reference Firebase rules in `firestore.rules` and `storage.rules`; rules still require manual publishing in Firebase Console.
- 2026-10-10: Added network permissions, inset handling, explicit ViewPager2/CardView dependencies, placeholder cleanup, and removed unused `ProductPageActivity`; updated `AndroidManifest.xml`, `PaymentHistoryActivity.java`, `BookingDetailsActivity.java`, `fragment_profile.xml`, `app/build.gradle.kts`, and deleted `ProductPageActivity.java`.
- 2026-10-10: Migrated turf photos to Firestore Blobs with compression, thumbnails, legacy URL fallback, photo deletion, and nested photo rules; updated `AddTurfActivity.java`, `ImageUtils.java`, `Turfname.java`, `TurfListing.java`, `CustomAdapter.java`, `ListingAdapter.java`, `HomeFragment.java`, `OnclickCategory.java`, `MyListingsActivity.java`, `ProductPage.java`, `ProductImagePagerAdapter.java`, `firestore.rules`, and deleted `storage.rules`.
- 2026-10-12: Made `ImageUtils` robust for picked Uris by buffering bytes before decoding, adding ImageDecoder fallback, safe EXIF handling, failure logging, and a friendly Add Turf error message; updated `ImageUtils.java` and `AddTurfActivity.java`.
- 2026-10-12: Added one-review-per-user turf reviews with transactional rating aggregates, review loading, editing, and owner protection; updated `ProductPage.java`, `product_page.xml`, `Review.java`, `ReviewAdapter.java`, `item_review.xml`, and `firestore.rules`.
- 2026-10-12: Added cached role loading, role-aware profile menus, player-to-owner upgrade flow, owner-only activity guards, and owner role enforcement in turf creation rules; updated `UserRole.java`, `ProfileFragment.java`, `fragment_profile.xml`, `AddTurfActivity.java`, `MyListingsActivity.java`, `firestore.rules`, and `AGENTS.md`.
- 2026-10-12: Applied saved light/dark mode at app startup, added real night palette and theme-aware button colors, and replaced hardcoded layout/drawable colors; updated `PlayzoApp.java`, `AndroidManifest.xml`, `SettingsActivity.java`, `BookSlotActivity.java`, `ProductPage.java`, theme/color resources, and affected layouts/drawables.
- 2026-10-12: Added Credential Manager Google sign-in to Login and Signup, including new-profile creation, selected-role handling, and existing-profile preservation; updated `libs.versions.toml`, `app/build.gradle.kts`, `GoogleAuthHelper.java`, `LoginActivity.java`, `SignupActivity.java`, and both auth layouts.
- 2026-10-12: Hid the Product Page bottom navigation while the keyboard is open, preserved inset handling, added keyboard-aware scroll padding, and kept the review field visible; updated `ProductPage.java`, `product_page.xml`, and `AGENTS.md`.
- 2026-10-12: Unified all bottom navigation bars with shared colors, layout, style, inset handling, and routing while preserving Home/Category scroll behavior; updated `BottomNavHelper.java`, `layout_bottom_nav.xml`, `nav_item_color.xml`, theme/color resources, `MainActivity.java`, `ProductPage.java`, `OnclickCategory.java`, and affected layouts.
- 2026-10-12: Fixed the shared bottom navigation active indicator to use the supported Material 3 indicator style attribute; updated `themes.xml` and `AGENTS.md`.
- 2026-10-12: Fixed the Product Page gallery indicator to hide for single images and render small dots for galleries, and updated shared bottom navigation theme colors, active labels, selected indicator, and divider; updated `ProductPage.java`, `product_page.xml`, `indicator_dot_selector.xml`, nav color resources, `themes.xml`, `layout_bottom_nav.xml`, and `AGENTS.md`.
- 2026-10-13: Refined shared bottom navigation to use one full-width surface with an in-drawable divider and reusable show/hide behavior; polished Home toolbar/search surfaces, collapse-aware search animation, clear-search interaction, and light palette colors; updated `BottomNavHelper.java`, `layout_bottom_nav.xml`, `bg_bottom_nav.xml`, `themes.xml`, `HomeFragment.java`, `CategoryFragment.java`, `ProfileFragment.java`, `MainActivity.java`, `ProductPage.java`, `fragment_home.xml`, `search_bar_background.xml`, and color resources.
- 2026-10-13: Fixed Home WELCOME title clipping by preserving font padding, adding safe line spacing and title padding, enabling small-screen autosizing, and disabling clipping on the header hierarchy; updated `fragment_home.xml`.
- 2026-10-13: Added OpenStreetMap/osmdroid turf location picking without an API key, saved `latitude`, `longitude`, and Google Maps `mapLink` fields, and added Product Page map opening with geo and browser fallbacks; updated `libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `MapPickerActivity.java`, `activity_map_picker.xml`, `AddTurfActivity.java`, `activity_add_turf.xml`, `ProductPage.java`, `product_page.xml`, map drawables, `strings.xml`, and `AGENTS.md`.
- 2026-10-13: Added transaction-protected five-minute slot holds, live slot states, date selection, simulated payment confirmation, Firestore My Bookings, and profile booking stats; replaced sample booking rules and data flow; updated `SlotConfig.java`, `TimeSlotAdapter.java`, `BookSlotActivity.java`, booking layouts/resources, `ProductPage.java`, `Booking.java`, `MyBookingsActivity.java`, `ProfileFragment.java`, `firestore.rules`, and `AGENTS.md`.
- 2026-10-13: Added Part 2 screen-specific motion, shimmer loading, animated auth feedback, gallery/review transitions, booking state/payment motion, profile stat count-up, turf photo transitions, and animated booking confirmation; updated `SignupActivity.java`, `ProductPage.java`, `BookSlotActivity.java`, `TimeSlotAdapter.java`, `ProfileFragment.java`, `PhotoPreviewAdapter.java`, shimmer/theme resources, confirmation dialog resources, `libs.versions.toml`, `app/build.gradle.kts`, and `AGENTS.md`.
- 2026-10-13: Added motion foundations, theme activity transitions, fade-through tab switching, first-load list entrances, lifecycle cancellation, and non-consuming press feedback; updated `Motion.java`, `AnimUtils.java`, `PressEffect.java`, animation resources, theme resources, motion-enabled screens/adapters, and `AGENTS.md`.
- 2026-10-13: Added persisted payment attempts and events, gateway abstraction with debug simulation modes, atomic booking/payment finalization, retry and timeout recovery, reconciliation, Firestore Payment History with timelines/support tickets, bounded error logging, payment rules, and status palettes; updated payment/booking Java classes, `PaymentHistoryActivity.java`, payment layouts/resources, `MainActivity.java`, `firestore.rules`, `app/build.gradle.kts`, and `AGENTS.md`.
