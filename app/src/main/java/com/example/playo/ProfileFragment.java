package com.example.playo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

/**
 * Profile screen shown in the bottom navigation "Profile" tab.
 * Features edge-to-edge insets, stat cards, an account menu
 * and a bottom bar that hides/shows while scrolling.
 */
public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "playo_prefs";
    private static final String PROFILE_IMAGE_KEY = "profile_image";

    private View profileParent;
    private ImageView profileAvatar;
    private TextView profileName;
    private TextView profileEmail;
    private TextView profileRoleBadge;
    private TextView statBookingCount;
    private TextView statTotalSpent;
    private boolean isBottomNavVisible = true;
    private ArrayList<ProfileOption> menuOptions;
    private ProfileAdapter menuAdapter;

    @Override
    public void onDestroyView() {
        AnimUtils.cancelTree(profileParent);
        if (isAdded()) {
            BottomNavHelper.cancel(requireActivity().findViewById(R.id.bottomView));
        }
        profileParent = null;
        super.onDestroyView();
    }

    // Launcher for picking a new profile photo from the gallery.
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        profileAvatar.setImageURI(uri);
                        // Persist the chosen photo so it survives restarts.
                        getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString(PROFILE_IMAGE_KEY, uri.toString()).apply();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // --- Bind views ---
        profileParent = view.findViewById(R.id.profileParent);
        profileAvatar = view.findViewById(R.id.profileAvatar);
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileRoleBadge = view.findViewById(R.id.profileRoleBadge);
        statBookingCount = view.findViewById(R.id.statBookingCount);
        statTotalSpent = view.findViewById(R.id.statTotalSpent);
        MaterialCardView cardBookings = view.findViewById(R.id.cardBookings);
        MaterialCardView cardPayments = view.findViewById(R.id.cardPayments);
        FrameLayout avatarWrapper = view.findViewById(R.id.avatarWrapper);
        RecyclerView menuRecycler = view.findViewById(R.id.profileMenuRecycler);
        animateProfile(view, avatarWrapper, cardBookings, cardPayments, menuRecycler);

        // --- Edge-to-edge: pad content below the status bar ---
        applySystemBarInsets(view);

        // --- Load persisted user data + stats ---
        loadUserData();

        // --- Tap the avatar to change the profile photo ---
        avatarWrapper.setOnClickListener(v -> openImagePicker());

        // --- Stats card click actions ---
        cardBookings.setOnClickListener(v -> openActivity(MyBookingsActivity.class));
        cardPayments.setOnClickListener(v -> openActivity(PaymentHistoryActivity.class));

        // --- Build account menu ---
        menuOptions = buildMenuOptions();
        menuAdapter = new ProfileAdapter(menuOptions, this::onOptionClicked);
        menuRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        menuRecycler.setAdapter(menuAdapter);
        loadRoleAndRebuildMenu();

        // --- Hide/show the bottom bar while scrolling ---
        setupBottomBarScroll(view);

        return view;
    }

    private void loadRoleAndRebuildMenu() {
        UserRole.fetchRole(role -> {
            if (!isAdded() || getView() == null) {
                return;
            }
            profileRoleBadge.setText("owner".equals(role) ? "Turf Owner" : "Player");
            AnimUtils.fadeIn(profileRoleBadge);
            menuOptions.clear();
            menuOptions.addAll(buildMenuOptions());
            menuAdapter.notifyDataSetChanged();
            AnimUtils.staggeredEntrance((RecyclerView) getView().findViewById(R.id.profileMenuRecycler));
        });
    }

    /** Opens the system gallery to choose a new profile photo. */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /** Pads the profile content with the top status-bar inset (edge-to-edge safe area). */
    private void applySystemBarInsets(View view) {
        View profileContent = view.findViewById(R.id.profileContent);
        ViewCompat.setOnApplyWindowInsetsListener(profileParent, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            profileContent.setPadding(0, bars.top, 0, 0);
            return insets;
        });
        // Force a re-dispatch in case the root container already consumed insets.
        ViewCompat.requestApplyInsets(profileParent);

        // Fallback: pad using the measured status bar height if insets never arrive.
        profileParent.post(() -> {
            if (profileContent.getPaddingTop() == 0) {
                int statusBarHeight = getStatusBarHeight();
                if (statusBarHeight > 0) {
                    profileContent.setPadding(0, statusBarHeight, 0, 0);
                }
            }
        });
    }

    /** Reads the system status bar height (used only as a safe fallback). */
    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
    }

    /** Slides the bottom navigation bar out of sight on scroll-down and back on scroll-up. */
    private void setupBottomBarScroll(View view) {
        NestedScrollView scrollView = view.findViewById(R.id.profileScroll);
        if (scrollView == null) {
            return;
        }
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomView);

        scrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (scrollY > oldScrollY && isBottomNavVisible) {
                        isBottomNavVisible = false;
                        BottomNavHelper.hide(bottomNav);
                    } else if (scrollY < oldScrollY && !isBottomNavVisible) {
                        isBottomNavVisible = true;
                        BottomNavHelper.show(bottomNav);
                    }
                });
    }

    /** Populates name/email and booking stats from Firestore, plus the local avatar. */
    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return; // shouldn't happen if auth flow gates entry, but guard anyway
        }
        String uid = currentUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        profileName.setText(name != null ? name : "");
                        profileEmail.setText(email != null ? email : currentUser.getEmail());
                    } else {
                        // Fallback to Auth data if Firestore doc is somehow missing
                        profileEmail.setText(currentUser.getEmail());
                    }
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("ProfileFragment", "Failed to load profile", e));

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadBookingStats(uid);

        // Restore the previously chosen profile photo, if any.
        String imageUri = prefs.getString(PROFILE_IMAGE_KEY, null);
        if (imageUri != null) {
            profileAvatar.setImageURI(Uri.parse(imageUri));
        }
    }

    private void loadBookingStats(String uid) {
        FirebaseFirestore.getInstance().collection("bookings")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }
                    double totalSpent = 0.0;
                    for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
                        Number amount = (Number) document.get("amount");
                        if (amount != null) {
                            totalSpent += amount.doubleValue();
                        }
                    }
                    AnimUtils.countUp(statBookingCount, 0, snapshot.size(), "");
                    AnimUtils.countUp(statTotalSpent, 0, (int) totalSpent, "₹");
                })
                .addOnFailureListener(error ->
                        android.util.Log.e("ProfileFragment", "Failed to load booking stats", error));
    }

    private void animateProfile(View root, View avatar, View bookings, View payments,
                                RecyclerView menuRecycler) {
        View[] views = {avatar, profileName, profileEmail, profileRoleBadge, bookings, payments};
        for (int index = 0; index < views.length; index++) {
            AnimUtils.slideUpFadeIn(views[index], index * Motion.STAGGER_STEP);
        }
        PressEffect.attach(avatar);
        AnimUtils.staggeredEntrance(menuRecycler);
    }

    /** Builds the list of account menu options with their target activities. */
    private ArrayList<ProfileOption> buildMenuOptions() {
        ArrayList<ProfileOption> list = new ArrayList<>();
        if (!"owner".equals(UserRole.getCachedRole())) {
            list.add(new ProfileOption("Become a Turf Owner", R.drawable.ic_store, null));
        }
        list.add(new ProfileOption(getString(R.string.menu_personal_details), R.drawable.ic_person, PersonalDetailsActivity.class));
        if ("owner".equals(UserRole.getCachedRole())) {
            list.add(new ProfileOption(getString(R.string.menu_my_turfs), R.drawable.ic_store, MyListingsActivity.class));
            list.add(new ProfileOption(getString(R.string.menu_add_turf), R.drawable.ic_add, AddTurfActivity.class));
        }
        list.add(new ProfileOption(getString(R.string.menu_booking_history), R.drawable.ic_history, MyBookingsActivity.class));
        list.add(new ProfileOption(getString(R.string.menu_favorites), R.drawable.ic_favorite, FavoritesActivity.class));
        list.add(new ProfileOption(getString(R.string.menu_notifications), R.drawable.ic_notifications, NotificationSettingsActivity.class));
        list.add(new ProfileOption(getString(R.string.menu_help), R.drawable.ic_help, HelpActivity.class));
        list.add(new ProfileOption(getString(R.string.menu_about), R.drawable.ic_info, AboutActivity.class));
        list.add(new ProfileOption(getString(R.string.settings), R.drawable.ic_settings, SettingsActivity.class));
        list.add(new ProfileOption(getString(R.string.menu_logout), R.drawable.ic_logout, null));
        return list;
    }

    /** Handles menu option clicks; special-cases logout. */
    private void onOptionClicked(ProfileOption option) {
        if ("Become a Turf Owner".equals(option.getTitle())) {
            showUpgradeDialog();
        } else if (option.getTargetActivity() == null) {
            showLogoutDialog();
        } else {
            openActivity(option.getTargetActivity());
        }
    }

    private void showUpgradeDialog() {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Become a Turf Owner?")
                .setMessage("You will be able to list your turfs and receive bookings. "
                        + "You can still book turfs as a player.")
                .setPositiveButton("Upgrade", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> upgradeToOwner(dialog)));
        dialog.show();
    }

    private void upgradeToOwner(androidx.appcompat.app.AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            dialog.dismiss();
            showUpgradeError("Upgrade could not be completed. Please try again.", null);
            return;
        }
        android.widget.Button upgradeButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        upgradeButton.setEnabled(false);
        upgradeButton.setText("Upgrading...");
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .update("role", "owner")
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) {
                        return;
                    }
                    UserRole.clearCache();
                    dialog.dismiss();
                    UserRole.fetchRole(role -> {
                        if (!isAdded() || getView() == null) {
                            return;
                        }
                        profileRoleBadge.setText("Turf Owner");
                        menuOptions.clear();
                        menuOptions.addAll(buildMenuOptions());
                        menuAdapter.notifyDataSetChanged();
                        Snackbar.make(profileParent, "You are now a turf owner", Snackbar.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(exception -> {
                    android.util.Log.e("ProfileFragment", "Failed to upgrade user role", exception);
                    if (!isAdded()) {
                        return;
                    }
                    upgradeButton.setEnabled(true);
                    upgradeButton.setText("Upgrade");
                    showUpgradeError("Upgrade could not be completed. Please try again.", dialog);
                });
    }

    private void showUpgradeError(String message, androidx.appcompat.app.AlertDialog dialog) {
        if (isAdded()) {
            Snackbar.make(profileParent, message, Snackbar.LENGTH_LONG).show();
        }
    }

    /** Simple intent launcher helper. */
    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(getContext(), activityClass);
        startActivity(intent);
    }

    /** Shows a confirmation dialog before logging out. */
    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_message))
                .setPositiveButton(getString(R.string.logout_confirm), (dialog, which) -> {
                    // Sign out from Firebase and clear locally stored session data.
                    FirebaseAuth.getInstance().signOut();
                    UserRole.clearCache();
                    getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().clear().apply();
                    Toast.makeText(getContext(), getString(R.string.logged_out), Toast.LENGTH_SHORT).show();

                    // Return to the login screen with a cleared back stack.
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}
