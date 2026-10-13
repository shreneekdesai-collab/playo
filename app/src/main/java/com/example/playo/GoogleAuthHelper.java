package com.example.playo;

import android.content.Context;
import android.util.Log;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/** Shared Credential Manager and Firebase Google sign-in flow. */
public final class GoogleAuthHelper {
    public interface Callback {
        void onSuccess(FirebaseUser user);

        void onFailure(String message, boolean cancelled);
    }

    private GoogleAuthHelper() {
    }

    public static void signIn(Context context, Callback callback) {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CredentialManager credentialManager = CredentialManager.create(context);
        credentialManager.getCredentialAsync(
                context,
                request,
                null,
                context.getMainExecutor(),
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleCredential(response, callback);
                    }

                    @Override
                    public void onError(GetCredentialException exception) {
                        if (exception instanceof GetCredentialCancellationException) {
                            callback.onFailure("", true);
                        } else if (exception instanceof NoCredentialException) {
                            callback.onFailure("Add a Google account to your phone and try again", false);
                        } else if (isNetworkError(exception)) {
                            callback.onFailure("Check your internet connection", false);
                        } else {
                            Log.e("GoogleAuthHelper", "Google credential request failed", exception);
                            callback.onFailure("Google sign-in failed. Please try again", false);
                        }
                    }
                });
    }

    private static void handleCredential(GetCredentialResponse response, Callback callback) {
        try {
            Credential credential = response.getCredential();
            if (!(credential instanceof CustomCredential)
                    || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(credential.getType())) {
                throw new IllegalStateException("Unsupported Google credential type");
            }

            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(
                    ((CustomCredential) credential).getData());
            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(
                    googleCredential.getIdToken(), null);
            FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                    .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                    .addOnFailureListener(exception -> {
                        if (exception instanceof FirebaseNetworkException || isNetworkError(exception)) {
                            callback.onFailure("Check your internet connection", false);
                        } else {
                            Log.e("GoogleAuthHelper", "Firebase Google sign-in failed", exception);
                            callback.onFailure("Google sign-in failed. Please try again", false);
                        }
                    });
        } catch (Exception exception) {
            Log.e("GoogleAuthHelper", "Unable to read Google credential", exception);
            callback.onFailure("Google sign-in failed. Please try again", false);
        }
    }

    private static boolean isNetworkError(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(java.util.Locale.US);
        return lower.contains("network") || lower.contains("internet")
                || lower.contains("timeout") || lower.contains("unavailable");
    }
}
