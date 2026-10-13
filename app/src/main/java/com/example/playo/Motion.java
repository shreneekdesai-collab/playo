package com.example.playo;

import android.content.Context;
import android.provider.Settings;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

/** Shared motion tokens and the system animation-scale guard. */
public final class Motion {
    public static final long DURATION_SHORT = 150L;
    public static final long DURATION_MEDIUM = 250L;
    public static final long DURATION_LONG = 350L;
    public static final long STAGGER_STEP = 40L;
    public static final int STAGGER_MAX = 8;
    public static final Interpolator FAST_OUT_SLOW_IN = new FastOutSlowInInterpolator();
    public static final Interpolator EASE_OUT = new DecelerateInterpolator();

    private Motion() {
    }

    public static boolean enabled(Context context) {
        try {
            return Settings.Global.getFloat(context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
        } catch (RuntimeException exception) {
            return true;
        }
    }
}
