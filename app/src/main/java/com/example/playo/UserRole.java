package com.example.playo;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/** Loads and caches the signed-in user's normalized application role. */
public final class UserRole {
    public interface Callback {
        void onRoleLoaded(String role);
    }

    private static String cachedRole;

    private UserRole() {
    }

    public static void fetchRole(Callback callback) {
        if (cachedRole != null) {
            callback.onRoleLoaded(cachedRole);
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            cachedRole = "customer";
            callback.onRoleLoaded(cachedRole);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    String role = document.exists() ? document.getString("role") : null;
                    cachedRole = "owner".equals(role) ? "owner" : "customer";
                    callback.onRoleLoaded(cachedRole);
                })
                .addOnFailureListener(exception -> {
                    cachedRole = "customer";
                    callback.onRoleLoaded(cachedRole);
                });
    }

    public static String getCachedRole() {
        return cachedRole;
    }

    public static void clearCache() {
        cachedRole = null;
    }
}
