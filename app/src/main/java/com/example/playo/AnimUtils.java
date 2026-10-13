package com.example.playo;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

/** Small, cancellable motion primitives used by screens and adapters. */
public final class AnimUtils {
    private AnimUtils() {
    }

    public static void fadeIn(View view) {
        fadeIn(view, 0L);
    }

    public static void fadeIn(View view, long delay) {
        if (view == null) return;
        cancel(view);
        if (!Motion.enabled(view.getContext())) {
            view.setVisibility(View.VISIBLE);
            view.setAlpha(1f);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.animate().alpha(1f)
                .setStartDelay(delay)
                .setDuration(Motion.DURATION_MEDIUM)
                .setInterpolator(Motion.FAST_OUT_SLOW_IN)
                .start();
    }

    public static void fadeOut(View view) {
        if (view == null) return;
        cancel(view);
        if (!Motion.enabled(view.getContext())) {
            view.setAlpha(1f);
            view.setVisibility(View.GONE);
            return;
        }
        view.setAlpha(1f);
        view.animate().alpha(0f)
                .setDuration(Motion.DURATION_MEDIUM)
                .setInterpolator(Motion.EASE_OUT)
                .withEndAction(() -> {
                    view.setAlpha(1f);
                    view.setVisibility(View.GONE);
                })
                .start();
    }

    public static void slideUpFadeIn(View view, long delay) {
        if (view == null) return;
        cancel(view);
        if (!Motion.enabled(view.getContext())) {
            view.setVisibility(View.VISIBLE);
            view.setAlpha(1f);
            view.setTranslationY(0f);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(24f * view.getResources().getDisplayMetrics().density);
        view.animate().alpha(1f).translationY(0f)
                .setStartDelay(delay)
                .setDuration(Motion.DURATION_MEDIUM)
                .setInterpolator(Motion.FAST_OUT_SLOW_IN)
                .start();
    }

    public static void shake(View view) {
        if (view == null || !Motion.enabled(view.getContext())) return;
        cancel(view);
        float distance = 8f * view.getResources().getDisplayMetrics().density;
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
                0f, -distance, distance, -distance, distance, 0f);
        animator.setDuration(300L);
        animator.setInterpolator(Motion.EASE_OUT);
        track(view, animator);
        animator.start();
    }

    public static void countUp(TextView view, int from, int to, String prefix) {
        if (view == null) return;
        cancel(view);
        if (!Motion.enabled(view.getContext())) {
            view.setText(prefix + to);
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(600L);
        animator.setInterpolator(Motion.EASE_OUT);
        animator.addUpdateListener(value -> view.setText(prefix + value.getAnimatedValue()));
        track(view, animator);
        animator.start();
    }

    public static void pulse(View view) {
        if (view == null || !Motion.enabled(view.getContext())) return;
        cancel(view);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.08f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.08f, 1f);
        AnimatorSet animator = new AnimatorSet();
        animator.playTogether(scaleX, scaleY);
        animator.setDuration(200L);
        animator.setInterpolator(Motion.FAST_OUT_SLOW_IN);
        track(view, animator);
        animator.start();
    }

    public static void staggeredEntrance(RecyclerView recyclerView) {
        if (recyclerView == null) return;
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        if (!Motion.enabled(recyclerView.getContext())) return;
        recyclerView.post(() -> {
            if (!recyclerView.isAttachedToWindow()) return;
            int count = Math.min(recyclerView.getChildCount(), Motion.STAGGER_MAX);
            for (int index = 0; index < count; index++) {
                slideUpFadeIn(recyclerView.getChildAt(index), index * Motion.STAGGER_STEP);
            }
        });
    }

    public static void cancel(View view) {
        if (view == null) return;
        view.animate().cancel();
        Object tag = view.getTag(R.id.motionAnimatorTag);
        if (tag instanceof Animator) {
            ((Animator) tag).cancel();
        }
        view.setTag(R.id.motionAnimatorTag, null);
        view.setAlpha(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }

    public static void cancelTree(View root) {
        if (root == null) return;
        cancel(root);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) {
                cancelTree(group.getChildAt(index));
            }
        }
    }

    private static void track(View view, Animator animator) {
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (view.getTag(R.id.motionAnimatorTag) == animation) {
                    view.setTag(R.id.motionAnimatorTag, null);
                }
            }
        });
        view.setTag(R.id.motionAnimatorTag, animator);
    }
}
