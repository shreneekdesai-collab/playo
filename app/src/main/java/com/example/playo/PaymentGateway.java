package com.example.playo;

import android.app.Activity;

/** Gateway boundary. A Razorpay implementation can replace the simulated one. */
public interface PaymentGateway {
    void pay(Activity activity, PaymentRequest request, Callback callback);

    interface Callback {
        void onSuccess(String gatewayPaymentId);
        void onFailure(String code, String message);
        void onCancelled();
    }
}
