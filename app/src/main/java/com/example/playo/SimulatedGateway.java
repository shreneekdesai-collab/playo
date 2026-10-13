package com.example.playo;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

/** Debug-only gateway used to exercise every payment outcome without credentials. */
public class SimulatedGateway implements PaymentGateway {
    public enum Outcome { SUCCESS, FAILED, CANCELLED, SLOW }

    private final Outcome outcome;

    public SimulatedGateway(Outcome outcome) {
        this.outcome = outcome;
    }

    @Override
    public void pay(Activity activity, PaymentRequest request, Callback callback) {
        // TODO: Release builds must not use the simulated gateway once real payments go live.
        if (!BuildConfig.DEBUG) {
            callback.onFailure("GATEWAY_DECLINED", "Payments are not available in this build.");
            return;
        }
        long delay = outcome == Outcome.SLOW ? 35_000L : 2_000L;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (outcome == Outcome.FAILED) {
                callback.onFailure("GATEWAY_DECLINED", "The simulated gateway declined this payment.");
            } else if (outcome == Outcome.CANCELLED) {
                callback.onCancelled();
            } else {
                callback.onSuccess("simulated_payment_" + System.currentTimeMillis());
            }
        }, delay);
    }
}
