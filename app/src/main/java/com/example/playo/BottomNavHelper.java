package com.example.playo;

import android.app.Activity;
import android.content.Intent;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Shared bottom navigation styling, inset handling, and cross-screen routing. */
public final class BottomNavHelper {
    private BottomNavHelper() {
    }

    public static void setup(Activity activity, BottomNavigationView nav, int selectedItemId) {
        applyInsets(nav);
        nav.setSelectedItemId(selectedItemId);
        nav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == selectedItemId) {
                return true;
            }

            Intent intent = new Intent(activity, MainActivity.class);
            if (itemId == R.id.Category) {
                intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, MainActivity.FRAGMENT_CATEGORY);
            } else if (itemId == R.id.Profile) {
                intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, MainActivity.FRAGMENT_PROFILE);
            } else if (itemId != R.id.Home) {
                return false;
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            if (Motion.enabled(activity)) {
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            } else {
                activity.overridePendingTransition(0, 0);
            }
            return true;
        });
    }

    /** Applies only the nav inset so screen root inset listeners remain untouched. */
    public static void applyInsets(BottomNavigationView nav) {
        final int baseBottomPadding = nav.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(nav, (view, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                    baseBottomPadding + bottomInset);
            return insets;
        });
        ViewCompat.requestApplyInsets(nav);
    }

    public static void hide(BottomNavigationView nav) {
        nav.animate().cancel();
        float distance = nav.getHeight() + nav.getPaddingBottom() + 30f;
        if (!Motion.enabled(nav.getContext())) {
            nav.setTranslationY(distance);
            nav.setVisibility(android.view.View.GONE);
            return;
        }
        nav.animate()
                .translationY(distance)
                .setDuration(Motion.DURATION_MEDIUM)
                .setInterpolator(Motion.EASE_OUT)
                .withEndAction(() -> nav.setVisibility(android.view.View.GONE))
                .start();
    }

    public static void show(BottomNavigationView nav) {
        nav.animate().cancel();
        if (!Motion.enabled(nav.getContext())) {
            nav.setVisibility(android.view.View.VISIBLE);
            nav.setTranslationY(0f);
            return;
        }
        nav.setVisibility(android.view.View.VISIBLE);
        nav.animate().translationY(0).setDuration(Motion.DURATION_MEDIUM)
                .setInterpolator(Motion.EASE_OUT).start();
    }

    public static void hideImmediately(BottomNavigationView nav) {
        nav.animate().cancel();
        nav.setVisibility(android.view.View.GONE);
    }

    public static void cancel(BottomNavigationView nav) {
        if (nav != null) {
            nav.animate().cancel();
        }
    }
}
