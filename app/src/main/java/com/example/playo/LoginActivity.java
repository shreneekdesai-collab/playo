package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Email/password login screen. Already-authenticated users are
 * forwarded straight to MainActivity from onStart().
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private Button googleSignInButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        PressEffect.attachToButtons(findViewById(android.R.id.content));

        // Reserve the system bar insets for the edge-to-edge layout.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        progressBar = findViewById(R.id.loginProgress);

        clearErrorOnChange(emailLayout, emailInput);
        clearErrorOnChange(passwordLayout, passwordInput);

        loginButton.setOnClickListener(v -> attemptLogin());
        googleSignInButton.setOnClickListener(v -> attemptGoogleSignIn());

        findViewById(R.id.signUpLink).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
        animateForm();
    }

    private void attemptGoogleSignIn() {
        googleSignInButton.setEnabled(false);
        setSubmitting(true, googleSignInButton);
        GoogleAuthHelper.signIn(this, new GoogleAuthHelper.Callback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                ensureGoogleProfile(user);
            }

            @Override
            public void onFailure(String message, boolean cancelled) {
                resetGoogleSignIn();
                if (!cancelled) {
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void ensureGoogleProfile(FirebaseUser user) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        openMainAfterGoogleSignIn();
                        return;
                    }
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("name", user.getDisplayName() == null ? "" : user.getDisplayName());
                    profile.put("email", user.getEmail() == null ? "" : user.getEmail());
                    profile.put("role", "customer");
                    profile.put("mobile", "");
                    profile.put("gender", "");
                    profile.put("dob", "");
                    profile.put("address", "");
                    profile.put("phoneVerified", false);
                    profile.put("createdAt", FieldValue.serverTimestamp());
                    firestore.collection("users").document(user.getUid()).set(profile)
                            .addOnSuccessListener(unused -> openMainAfterGoogleSignIn())
                            .addOnFailureListener(exception -> handleGoogleProfileFailure(exception));
                })
                .addOnFailureListener(exception -> handleGoogleProfileFailure(exception));
    }

    private void openMainAfterGoogleSignIn() {
        resetGoogleSignIn();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void handleGoogleProfileFailure(Exception exception) {
        Log.e("LoginActivity", "Failed to load or create Google profile", exception);
        ErrorLogger.log(this, "Login", "google_sign_in", exception);
        resetGoogleSignIn();
        Toast.makeText(this, "Google sign-in failed. Please try again", Toast.LENGTH_LONG).show();
    }

    private void resetGoogleSignIn() {
        setSubmitting(false, googleSignInButton);
        googleSignInButton.setEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (firebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void attemptLogin() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        boolean valid = true;
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            AnimUtils.shake(emailLayout);
            valid = false;
        }
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            AnimUtils.shake(passwordLayout);
            valid = false;
        }
        if (!valid) {
            return;
        }

        setSubmitting(true, loginButton);
        loginButton.setEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setSubmitting(false, loginButton);
                    loginButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        if (!isExpectedCredentialFailure(task.getException())) {
                            ErrorLogger.log(this, "Login", "email_sign_in", task.getException());
                        }
                        Toast.makeText(this, "Login failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Clears the inline error on a field as soon as the user starts typing again. */
    private void clearErrorOnChange(TextInputLayout layout, TextInputEditText input) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private boolean isExpectedCredentialFailure(Exception error) {
        if (!(error instanceof FirebaseAuthException)) return false;
        String code = ((FirebaseAuthException) error).getErrorCode();
        return "ERROR_WRONG_PASSWORD".equals(code) || "ERROR_USER_NOT_FOUND".equals(code);
    }

    private void animateForm() {
        View[] fields = {emailLayout, passwordLayout, loginButton, googleSignInButton};
        for (int index = 0; index < fields.length; index++) {
            AnimUtils.slideUpFadeIn(fields[index], index * Motion.STAGGER_STEP);
        }
    }

    private void setSubmitting(boolean submitting, View button) {
        if (!Motion.enabled(this)) {
            progressBar.setVisibility(submitting ? View.VISIBLE : View.GONE);
            button.setAlpha(1f);
            return;
        }
        if (submitting) {
            AnimUtils.fadeIn(progressBar);
            button.animate().alpha(0f).setDuration(Motion.DURATION_SHORT)
                    .setInterpolator(Motion.EASE_OUT).start();
        } else {
            AnimUtils.fadeOut(progressBar);
            button.setAlpha(0f);
            button.animate().alpha(1f).setDuration(Motion.DURATION_SHORT)
                    .setInterpolator(Motion.EASE_OUT).start();
        }
    }

    @Override
    protected void onDestroy() {
        AnimUtils.cancelTree(findViewById(R.id.loginRoot));
        super.onDestroy();
    }
}
