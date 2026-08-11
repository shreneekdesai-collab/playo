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
    private TextView statBookingCount;
    private TextView statTotalSpent;
    private boolean isBottomNavVisible = true;

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
        statBookingCount = view.findViewById(R.id.statBookingCount);
        statTotalSpent = view.findViewById(R.id.statTotalSpent);
        MaterialCardView cardBookings = view.findViewById(R.id.cardBookings);
        MaterialCardView cardPayments = view.findViewById(R.id.cardPayments);
        FrameLayout avatarWrapper = view.findViewById(R.id.avatarWrapper);
        RecyclerView menuRecycler = view.findViewById(R.id.profileMenuRecycler);

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
        ArrayList<ProfileOption> options = buildMenuOptions();
        ProfileAdapter adapter = new ProfileAdapter(options, this::onOptionClicked);
        menuRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        menuRecycler.setAdapter(adapter);

        // --- Hide/show the bottom bar while scrolling ---
        setupBottomBarScroll(view);

        return view;
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
                        bottomNav.animate()
                                .translationY(bottomNav.getHeight() + 30f)
                                .setDuration(250)
                                .start();
                    } else if (scrollY < oldScrollY && !isBottomNavVisible) {
                        isBottomNavVisible = true;
                        bottomNav.animate()
                                .translationY(0)
                                .setDuration(250)
                                .start();
                    }
                });
    }

    /** Populates name/email and stats from SharedPreferences. */
    private void loadUserData() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString("user_name", "Player One");
        String email = prefs.getString("user_email", "player@example.com");
        int bookings = prefs.getInt("booking_count", 0);
        int totalSpent = prefs.getInt("total_spent", 0);

        profileName.setText(name);
        profileEmail.setText(email);
        statBookingCount.setText(String.valueOf(bookings));
        statTotalSpent.setText("₹" + totalSpent);

        // Restore the previously chosen profile photo, if any.
        String imageUri = prefs.getString(PROFILE_IMAGE_KEY, null);
        if (imageUri != null) {
            profileAvatar.setImageURI(Uri.parse(imageUri));
        }
    }

    /** Builds the list of account menu options with their target activities. */
    private ArrayList<ProfileOption> buildMenuOptions() {
        ArrayList<ProfileOption> list = new ArrayList<>();
        list.add(new ProfileOption(getString(R.string.menu_personal_details), R.drawable.ic_person, PersonalDetailsActivity.class));
        list.add(new ProfileOption(getString(R.string.menu_my_turfs), R.drawable.ic_store, MyListingsActivity.class));
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
        if (option.getTargetActivity() == null) {
            showLogoutDialog();
        } else {
            openActivity(option.getTargetActivity());
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
                    // Clear locally stored session data, if any.
                    getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().clear().apply();
                    Toast.makeText(getContext(), getString(R.string.logged_out), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}