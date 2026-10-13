package com.example.playo;

import android.view.MotionEvent;
import android.view.View;

/** Adds non-consuming scale feedback while preserving click and ripple handling. */
public final class PressEffect {
    private PressEffect() {
    }

    public static void attach(View view) {
        if (view == null || view.getTag(R.id.pressEffectAttachedTag) != null) return;
        view.setTag(R.id.pressEffectAttachedTag, Boolean.TRUE);
        view.setOnTouchListener((target, event) -> {
            if (!Motion.enabled(target.getContext())) {
                target.setScaleX(1f);
                target.setScaleY(1f);
                return false;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                target.animate().cancel();
                target.animate().scaleX(0.97f).scaleY(0.97f)
                        .setDuration(100L)
                        .setInterpolator(Motion.EASE_OUT)
                        .start();
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                target.animate().scaleX(1f).scaleY(1f)
                        .setDuration(Motion.DURATION_SHORT)
                        .setInterpolator(Motion.EASE_OUT)
                        .start();
            }
            return false;
        });
    }

    public static void attachToButtons(View root) {
        if (root == null) return;
        if (root instanceof android.widget.Button) {
            attach(root);
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) {
                attachToButtons(group.getChildAt(index));
            }
        }
    }
}
