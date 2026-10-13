package com.example.playo;

import android.content.Context;
import android.os.Build;
import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/** Best-effort, bounded application error logging without personal data. */
public final class ErrorLogger {
    private static int sessionCount;

    private ErrorLogger() { }

    public static void log(Context context, String screen, String action, Throwable throwable) {
        log(context, screen, action, throwable == null ? "Unexpected error" : throwable.getMessage());
    }

    public static void log(Context context, String screen, String action, @Nullable String message) {
        if (sessionCount >= 20 || context == null) {
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            return;
        }
        sessionCount++;
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("screen", safe(screen, 100));
        data.put("action", safe(action, 100));
        data.put("errorCode", "UNEXPECTED_ERROR");
        data.put("message", safe(message, 300));
        data.put("appVersion", BuildConfig.VERSION_NAME);
        data.put("androidVersion", Build.VERSION.RELEASE);
        data.put("deviceModel", Build.MANUFACTURER + " " + Build.MODEL);
        data.put("createdAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("appErrors").add(data)
                .addOnFailureListener(error -> { /* Logging must never affect the user flow. */ });
    }

    private static String safe(String value, int max) {
        if (value == null || value.trim().isEmpty()) {
            return "Unknown error";
        }
        String result = value.replaceAll("[\\r\\n]+", " ").trim();
        return result.length() > max ? result.substring(0, max) : result;
    }
}
