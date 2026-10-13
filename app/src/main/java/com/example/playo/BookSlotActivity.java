package com.example.playo;

import android.graphics.Color;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Real slot booking with Firestore holds and transaction-protected confirmation. */
public class BookSlotActivity extends AppCompatActivity {

    private final Handler slotHandler = new Handler(Looper.getMainLooper());
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private ListenerRegistration slotsListener;
    private TimeSlotAdapter timeSlotAdapter;
    private PaymentMethodAdapter paymentMethodAdapter;
    private Button btnProceedPay;
    private View bookingContent;
    private LinearLayout paymentStep;
    private TextView paymentTurfName;
    private TextView paymentSlotDetails;
    private TextView paymentAmount;
    private TextView paymentMethod;
    private TextView paymentCountdown;
    private MaterialButton btnSimulatedPay;
    private Spinner simulateOutcomeSpinner;
    private Payment activePayment;
    private PaymentRequest activeRequest;
    private String activeGatewayPaymentId;
    private int finalizeAttempts;
    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final BookingService bookingService = new BookingService();

    private String turfId;
    private String turfName;
    private String ownerId;
    private String selectedDate;
    private double amount;
    private String heldSlotKey;
    private String heldSlotLabel;
    private String heldPaymentMethod;
    private long holdStartMillis;
    private boolean holdActive;
    private boolean bookingConfirmed;
    private long lastCountdownSecond = -1L;

    private final Runnable slotRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }
            timeSlotAdapter.refreshStates();
            clearUnavailableSelectionIfNeeded();
            slotHandler.postDelayed(this, 10_000L);
        }
    };

    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (!holdActive) {
                return;
            }
            long remaining = SlotConfig.HOLD_MILLIS - (System.currentTimeMillis() - holdStartMillis);
            if (remaining <= 0) {
                if (activePayment != null && ("created".equals(activePayment.getStatus())
                        || "processing".equals(activePayment.getStatus()))) {
                    markPaymentTimeout();
                }
                releaseHold();
                closePaymentStep();
                showMessage("Your hold expired");
                return;
            }
            long totalSeconds = (remaining + 999L) / 1000L;
            paymentCountdown.setText(String.format(Locale.US, "Complete payment in %02d:%02d",
                    totalSeconds / 60L, totalSeconds % 60L));
            if (totalSeconds <= 60L && totalSeconds > 59L && lastCountdownSecond != 60L) {
                AnimUtils.pulse(paymentCountdown);
            }
            if (totalSeconds <= 30L) {
                paymentCountdown.setTextColor(ContextCompat.getColor(BookSlotActivity.this, R.color.error_red));
            } else {
                paymentCountdown.setTextColor(ContextCompat.getColor(BookSlotActivity.this, R.color.colorPrimary));
            }
            lastCountdownSecond = totalSeconds;
            slotHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_slot);
        PressEffect.attachToButtons(findViewById(android.R.id.content));
        setupStatusBar();

        turfId = getIntent().getStringExtra("TURF_ID");
        turfName = getIntent().getStringExtra("TURF_NAME");
        ownerId = getIntent().getStringExtra("OWNER_ID");
        amount = getIntent().getDoubleExtra("TURF_PRICE_AMOUNT", 0.0);
        if (turfId == null || turfId.trim().isEmpty()) {
            Toast.makeText(this, "This turf cannot be booked yet", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to book a turf", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (ownerId != null && ownerId.equals(user.getUid())) {
            Toast.makeText(this, "You cannot book your own turf", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bookSlotRoot), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.bookSlotToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        bookingContent = findViewById(R.id.bookingContent);
        paymentStep = findViewById(R.id.paymentStep);
        paymentTurfName = findViewById(R.id.paymentTurfName);
        paymentSlotDetails = findViewById(R.id.paymentSlotDetails);
        paymentAmount = findViewById(R.id.paymentAmount);
        paymentMethod = findViewById(R.id.paymentMethod);
        paymentCountdown = findViewById(R.id.paymentCountdown);
        btnSimulatedPay = findViewById(R.id.btnSimulatedPay);
        simulateOutcomeSpinner = findViewById(R.id.simulateOutcomeSpinner);
        if (BuildConfig.DEBUG) {
            simulateOutcomeSpinner.setVisibility(View.VISIBLE);
            simulateOutcomeSpinner.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"Simulate: success", "Simulate: failed", "Simulate: cancelled", "Simulate: slow (35 s)"}));
        }
        findViewById(R.id.btnCancelHold).setOnClickListener(v -> cancelHoldAndReturn());
        btnSimulatedPay.setOnClickListener(v -> completePayment());

        selectedDate = SlotConfig.formatDate(Calendar.getInstance());
        setupDateChips();
        setupTimeSlots();
        setupPaymentMethods();

        btnProceedPay = findViewById(R.id.btnProceedPay);
        btnProceedPay.setOnClickListener(v -> claimSlot());
        updateProceedButtonState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (turfId != null) {
            attachSlotsListener();
            startSlotRefresh();
            if (paymentStep != null && paymentStep.getVisibility() == View.VISIBLE && !holdActive
                    && !bookingConfirmed) {
                closePaymentStep();
            }
        }
    }

    @Override
    protected void onStop() {
        AnimUtils.cancelTree(bookingContent);
        AnimUtils.cancelTree(paymentStep);
        stopSlotRefresh();
        removeSlotsListener();
        if (holdActive && !bookingConfirmed) {
            releaseHold();
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (holdActive && !bookingConfirmed) {
            releaseHold();
            closePaymentStep();
            finish();
            return;
        }
        super.onBackPressed();
    }

    private void setupDateChips() {
        LinearLayout container = findViewById(R.id.dateChipContainer);
        container.removeAllViews();
        Calendar date = Calendar.getInstance();
        for (int index = 0; index < 14; index++) {
            Calendar chipDate = (Calendar) date.clone();
            chipDate.add(Calendar.DAY_OF_YEAR, index);
            String dateValue = SlotConfig.formatDate(chipDate);
            MaterialButton chip = new MaterialButton(this);
            chip.setText(String.format(Locale.getDefault(), "%s\n%d\n%s",
                    new SimpleDateFormat("EEE", Locale.getDefault()).format(chipDate.getTime()),
                    chipDate.get(Calendar.DAY_OF_MONTH),
                    new SimpleDateFormat("MMM", Locale.getDefault()).format(chipDate.getTime())));
            chip.setTextSize(11);
            chip.setAllCaps(false);
            chip.setMinWidth(64);
            chip.setMinHeight(68);
            chip.setPadding(4, 0, 4, 0);
            chip.setCornerRadius(12);
            PressEffect.attach(chip);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(70, 68);
            params.setMargins(0, 0, 8, 0);
            container.addView(chip, params);
            chip.setOnClickListener(v -> {
                if (dateValue.equals(selectedDate)) {
                    return;
                }
                selectedDate = dateValue;
                updateDateChipStyles();
                timeSlotAdapter.setSelectedDate(selectedDate);
                attachSlotsListener();
                updateProceedButtonState();
            });
        }
        updateDateChipStyles();
    }

    private void updateDateChipStyles() {
        LinearLayout container = findViewById(R.id.dateChipContainer);
        for (int index = 0; index < container.getChildCount(); index++) {
            MaterialButton chip = (MaterialButton) container.getChildAt(index);
            boolean selected = index == 0
                    ? selectedDate.equals(SlotConfig.formatDate(Calendar.getInstance()))
                    : chip.getTag() != null && selectedDate.equals(chip.getTag());
            if (chip.getTag() == null) {
                Calendar date = Calendar.getInstance();
                date.add(Calendar.DAY_OF_YEAR, index);
                chip.setTag(SlotConfig.formatDate(date));
                selected = selectedDate.equals(chip.getTag());
            }
            int targetColor = ContextCompat.getColor(this,
                    selected ? R.color.button_bg : R.color.surface_white);
            boolean wasSelected = chip.isSelected();
            chip.setSelected(selected);
            if (wasSelected != selected && Motion.enabled(this)) {
                int oldColor = ContextCompat.getColor(this,
                        wasSelected ? R.color.button_bg : R.color.surface_white);
                ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, targetColor);
                colorAnimator.setDuration(Motion.DURATION_SHORT);
                colorAnimator.addUpdateListener(value -> chip.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf((Integer) value.getAnimatedValue())));
                colorAnimator.start();
            } else {
                chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(targetColor));
            }
            chip.setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.button_text : R.color.text_primary));
            chip.setStrokeWidth(1);
            chip.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.card_stroke)));
        }
    }

    private void setupTimeSlots() {
        timeSlotAdapter = new TimeSlotAdapter(SlotConfig.hourlySlots(),
                FirebaseAuth.getInstance().getCurrentUser().getUid(), selectedDate);
        timeSlotAdapter.setOnSlotSelectedListener(slotKey -> updateProceedButtonState());
        RecyclerView recycler = findViewById(R.id.timeSlotRecycler);
        recycler.setLayoutManager(new GridLayoutManager(this, 4));
        recycler.setAdapter(timeSlotAdapter);
    }

    private void setupPaymentMethods() {
        List<PaymentMethodAdapter.PaymentMethod> methods = new ArrayList<>();
        methods.add(new PaymentMethodAdapter.PaymentMethod("UPI", "Pay by any UPI app", R.drawable.ic_payment));
        methods.add(new PaymentMethodAdapter.PaymentMethod("Credit / Debit Card", "Visa, Mastercard, RuPay", R.drawable.ic_payment));
        methods.add(new PaymentMethodAdapter.PaymentMethod("Net Banking", "All major banks", R.drawable.ic_payment));
        methods.add(new PaymentMethodAdapter.PaymentMethod("Wallets", "Paytm, PhonePe, Amazon Pay", R.drawable.ic_wallet));
        methods.add(new PaymentMethodAdapter.PaymentMethod("Cash on Arrival", "Pay at the venue", R.drawable.ic_payment));
        paymentMethodAdapter = new PaymentMethodAdapter(methods);
        paymentMethodAdapter.setOnPaymentMethodSelectedListener(method -> updateProceedButtonState());
        RecyclerView recycler = findViewById(R.id.paymentMethodRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(paymentMethodAdapter);
    }

    private void updateProceedButtonState() {
        if (btnProceedPay == null) {
            return;
        }
        btnProceedPay.setEnabled(timeSlotAdapter.getSelectedSlotKey() != null
                && paymentMethodAdapter.getSelectedMethod() != null && !holdActive);
    }

    private void attachSlotsListener() {
        removeSlotsListener();
        if (turfId == null || selectedDate == null) {
            return;
        }
        slotsListener = firestore.collection("turfs").document(turfId).collection("slots")
                .whereEqualTo("date", selectedDate)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        android.util.Log.e("BookSlotActivity", "Failed to listen for slots", error);
                        ErrorLogger.log(this, "BookSlot", "load_slots", error);
                        return;
                    }
                    Map<String, TimeSlotAdapter.SlotData> data = new HashMap<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            String key = document.getString("slotKey");
                            if (key == null) {
                                key = document.getId().substring(document.getId().lastIndexOf('_') + 1);
                            }
                            data.put(key, new TimeSlotAdapter.SlotData(
                                    document.getString("status"), document.getString("heldBy"),
                                    document.getTimestamp("heldAt")));
                        }
                    }
                    timeSlotAdapter.setSlotData(data);
                    clearUnavailableSelectionIfNeeded();
                });
    }

    private void clearUnavailableSelectionIfNeeded() {
        if (timeSlotAdapter != null && timeSlotAdapter.isSelectedSlotUnavailable()) {
            timeSlotAdapter.clearSelection();
            updateProceedButtonState();
            showMessage("That slot was just taken. Please choose another.");
        }
    }

    private void removeSlotsListener() {
        if (slotsListener != null) {
            slotsListener.remove();
            slotsListener = null;
        }
    }

    private void startSlotRefresh() {
        stopSlotRefresh();
        slotHandler.postDelayed(slotRefreshRunnable, 10_000L);
    }

    private void stopSlotRefresh() {
        slotHandler.removeCallbacks(slotRefreshRunnable);
    }

    private void claimSlot() {
        String slotKey = timeSlotAdapter.getSelectedSlotKey();
        String slotLabel = timeSlotAdapter.getSelectedSlotLabel();
        String method = paymentMethodAdapter.getSelectedMethod();
        if (slotKey == null || method == null || holdActive) {
            return;
        }
        btnProceedPay.setEnabled(false);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference slotRef = firestore.collection("turfs").document(turfId)
                .collection("slots").document(SlotConfig.slotId(selectedDate, slotKey));
        final String finalSlotKey = slotKey;
        final String finalSlotLabel = slotLabel;
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(slotRef);
            if (!snapshot.exists()) {
                transaction.set(slotRef, holdData(uid, finalSlotKey, finalSlotLabel));
                return null;
            }
            String status = snapshot.getString("status");
            Timestamp heldAt = snapshot.getTimestamp("heldAt");
            if ("held".equals(status) && SlotConfig.isExpired(heldAt, System.currentTimeMillis())) {
                transaction.set(slotRef, holdData(uid, finalSlotKey, finalSlotLabel));
                return null;
            }
            if ("held".equals(status) && uid.equals(snapshot.getString("heldBy"))
                    && !SlotConfig.isExpired(heldAt, System.currentTimeMillis())) {
                return null;
            }
            throw new SlotUnavailableException();
        }).addOnSuccessListener(unused -> {
            heldSlotKey = finalSlotKey;
            heldSlotLabel = finalSlotLabel;
            heldPaymentMethod = method;
            holdStartMillis = System.currentTimeMillis();
            holdActive = true;
            showPaymentStep();
        }).addOnFailureListener(error -> {
            btnProceedPay.setEnabled(true);
            if (isSlotUnavailable(error)) {
                showMessage("Someone just took this slot. Please choose another.");
                attachSlotsListener();
            } else {
                android.util.Log.e("BookSlotActivity", "Failed to hold slot", error);
                ErrorLogger.log(this, "BookSlot", "hold_slot", error);
                showMessage("The slot could not be held. Please try again.");
            }
        });
    }

    private Map<String, Object> holdData(String uid, String slotKey, String slotLabel) {
        Map<String, Object> data = new HashMap<>();
        data.put("turfId", turfId);
        data.put("date", selectedDate);
        data.put("slotKey", slotKey);
        data.put("slotLabel", slotLabel);
        data.put("status", "held");
        data.put("heldBy", uid);
        data.put("heldAt", FieldValue.serverTimestamp());
        return data;
    }

    private boolean isSlotUnavailable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SlotUnavailableException) {
                return true;
            }
            if (current instanceof FirebaseFirestoreException
                    && ((FirebaseFirestoreException) current).getCode()
                    == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void showPaymentStep() {
        bookingContent.setVisibility(View.GONE);
        paymentStep.setVisibility(View.VISIBLE);
        AnimUtils.slideUpFadeIn(paymentStep, 0L);
        paymentTurfName.setText(turfName == null ? "Turf booking" : turfName);
        paymentSlotDetails.setText(selectedDate + " • " + heldSlotLabel);
        paymentAmount.setText(String.format(Locale.US, "Amount: Rs %.2f", amount));
        paymentMethod.setText("Payment method: " + heldPaymentMethod);
        btnSimulatedPay.setText(String.format(Locale.US, "Pay Rs %.2f", amount));
        lastCountdownSecond = -1L;
        slotHandler.removeCallbacks(countdownRunnable);
        slotHandler.post(countdownRunnable);
    }

    private void completePayment() {
        if (!holdActive) {
            return;
        }
        if (System.currentTimeMillis() - holdStartMillis >= SlotConfig.HOLD_MILLIS) {
            markPaymentTimeout();
            releaseHold();
            closePaymentStep();
            return;
        }
        btnSimulatedPay.setEnabled(false);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showPaymentError("Please sign in again to complete payment.");
            return;
        }
        String holdSlotId = SlotConfig.slotId(selectedDate, heldSlotKey);
        String userName = user.getDisplayName() == null ? "Player" : user.getDisplayName();
        PaymentRequest request = new PaymentRequest(null, user.getUid(), ownerId, turfId,
                turfName, selectedDate, heldSlotKey, heldSlotLabel, amount, heldPaymentMethod,
                holdSlotId, userName);
        paymentRepository.createAttempt(request, new PaymentRepository.Callback<Payment>() {
            @Override public void onSuccess(Payment payment) {
                activePayment = payment;
                activeRequest = new PaymentRequest(payment.getPaymentId(), request.userId, request.ownerId,
                        request.turfId, request.turfName, request.date, request.slotKey, request.slotLabel,
                        request.amount, request.method, request.holdSlotId, request.userName);
                paymentRepository.updateStatus(payment.getPaymentId(), "processing", null, null,
                        new PaymentRepository.Callback<Void>() {
                            @Override public void onSuccess(Void value) {
                                paymentRepository.addEvent(payment.getPaymentId(), "gateway_started", "Gateway started");
                                startGateway();
                            }
                            @Override public void onFailure(Exception error) {
                                ErrorLogger.log(BookSlotActivity.this, "BookSlot", "start_payment", error);
                                btnSimulatedPay.setEnabled(true);
                                showPaymentError("Payment could not be started. Please try again.");
                            }
                        });
            }
            @Override public void onFailure(Exception error) {
                ErrorLogger.log(BookSlotActivity.this, "BookSlot", "create_payment", error);
                btnSimulatedPay.setEnabled(true);
                showPaymentError("Payment could not be started. Please try again.");
            }
        });
    }

    private void startGateway() {
        SimulatedGateway.Outcome outcome = SimulatedGateway.Outcome.SUCCESS;
        if (BuildConfig.DEBUG && simulateOutcomeSpinner != null) {
            outcome = new SimulatedGateway.Outcome[]{SimulatedGateway.Outcome.SUCCESS,
                    SimulatedGateway.Outcome.FAILED, SimulatedGateway.Outcome.CANCELLED,
                    SimulatedGateway.Outcome.SLOW}[simulateOutcomeSpinner.getSelectedItemPosition()];
        }
        // TODO: Select the Razorpay implementation here when server verification is available.
        new SimulatedGateway(outcome).pay(this, activeRequest, new PaymentGateway.Callback() {
            @Override public void onSuccess(String gatewayPaymentId) {
                activeGatewayPaymentId = gatewayPaymentId;
                paymentRepository.addEvent(activePayment.getPaymentId(), "gateway_success", "Gateway reported success");
                finalizeWithRetry(gatewayPaymentId, 0);
            }
            @Override public void onFailure(String code, String message) {
                paymentRepository.updateStatus(activePayment.getPaymentId(), "failed", code, message,
                        new PaymentRepository.Callback<Void>() {
                            @Override public void onSuccess(Void value) {
                                paymentRepository.addEvent(activePayment.getPaymentId(), "gateway_failed", "Gateway failed");
                            }
                            @Override public void onFailure(Exception error) { }
                        });
                btnSimulatedPay.setEnabled(true);
                if (holdActive) showPaymentFailureDialog(message);
                else { releaseHold(); closePaymentStep(); }
            }
            @Override public void onCancelled() {
                paymentRepository.updateStatus(activePayment.getPaymentId(), "cancelled", "USER_CANCELLED", "Payment cancelled",
                        new PaymentRepository.Callback<Void>() {
                            @Override public void onSuccess(Void value) { paymentRepository.addEvent(activePayment.getPaymentId(), "gateway_cancelled", "Payment cancelled"); }
                            @Override public void onFailure(Exception error) { }
                        });
                releaseHold();
                closePaymentStep();
            }
        });
    }

    private void finalizeWithRetry(String gatewayPaymentId, int attempt) {
        if (activeRequest == null || activePayment == null) return;
        finalizeAttempts = attempt;
        bookingService.finalizeBooking(activeRequest, gatewayPaymentId, new BookingService.Callback() {
            @Override public void onSuccess(String bookingId) {
                holdActive = false;
                bookingConfirmed = true;
                slotHandler.removeCallbacks(countdownRunnable);
                showConfirmationDialog();
            }
            @Override public void onFailure(String code, String message, Exception cause) {
                paymentRepository.addEvent(activePayment.getPaymentId(), "booking_write_failed", message);
                ErrorLogger.log(BookSlotActivity.this, "BookSlot", "finalize_booking", cause);
                if (attempt < 2 && holdActive && !"HOLD_EXPIRED".equals(code) && !"SLOT_TAKEN".equals(code)) {
                    slotHandler.postDelayed(() -> finalizeWithRetry(gatewayPaymentId, attempt + 1), 2_000L);
                } else {
                    paymentRepository.updateStatus(activePayment.getPaymentId(), "needs_review", code, message,
                            new PaymentRepository.Callback<Void>() {
                                @Override public void onSuccess(Void value) { }
                                @Override public void onFailure(Exception error) { }
                            });
                    showNeedsReviewDialog();
                }
            }
        });
    }

    private void markPaymentTimeout() {
        if (activePayment == null) return;
        paymentRepository.updateStatus(activePayment.getPaymentId(), "timeout", "HOLD_EXPIRED",
                "The payment hold expired", new PaymentRepository.Callback<Void>() {
                    @Override public void onSuccess(Void value) { paymentRepository.addEvent(activePayment.getPaymentId(), "hold_expired", "Payment hold expired"); }
                    @Override public void onFailure(Exception error) { }
                });
    }

    private void showPaymentFailureDialog(String rawMessage) {
        String message = "The payment could not be completed. Please try again.";
        new MaterialAlertDialogBuilder(this).setTitle("Payment failed").setMessage(message)
                .setPositiveButton("Try again", (dialog, which) -> {
                    paymentRepository.addEvent(activePayment.getPaymentId(), "retry_started", "Payment retry started");
                    completePayment();
                }).setNegativeButton("Cancel", null).show();
    }

    private void showNeedsReviewDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Payment received but booking is not confirmed yet")
                .setMessage("Payment ID: " + (activePayment == null ? "unknown" : activePayment.getPaymentId())
                        + ". We are checking it, see Payment History.")
                .setPositiveButton("Retry", (dialog, which) -> finalizeWithRetry(
                        activeGatewayPaymentId, 0))
                .setNegativeButton("Report a problem", (dialog, which) -> showQuickReportDialog())
                .show();
    }

    private void showQuickReportDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Tell us what went wrong");
        input.setMinLines(3);
        new MaterialAlertDialogBuilder(this).setTitle("Report a problem").setView(input)
                .setNegativeButton("Cancel", null).setPositiveButton("Submit", (dialog, which) ->
                        new SupportRepository().createTicket(input.getText().toString(),
                                activePayment == null ? null : activePayment.getPaymentId(), null,
                                new PaymentRepository.Callback<Void>() {
                                    @Override public void onSuccess(Void value) { showMessage("Thanks, we will look into it"); }
                                    @Override public void onFailure(Exception error) {
                                        ErrorLogger.log(BookSlotActivity.this, "BookSlot", "create_ticket", error);
                                    }
                                })).show();
    }

    private void showConfirmationDialog() {
        View confirmationView = getLayoutInflater().inflate(R.layout.dialog_booking_confirmed, null);
        new MaterialAlertDialogBuilder(this).setView(confirmationView).setCancelable(false)
                .setPositiveButton("View my bookings", (dialog, which) -> {
                    startActivity(new android.content.Intent(this, MyBookingsActivity.class));
                    finish();
                }).show();
        android.widget.ImageView checkIcon = confirmationView.findViewById(R.id.bookingCheckIcon);
        if (Motion.enabled(this) && checkIcon.getDrawable() instanceof android.graphics.drawable.AnimatedVectorDrawable) {
            ((android.graphics.drawable.AnimatedVectorDrawable) checkIcon.getDrawable()).start();
        }
    }

    private boolean isHoldExpired(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof HoldExpiredException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void releaseHold() {
        if (!holdActive || heldSlotKey == null) {
            return;
        }
        holdActive = false;
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            return;
        }
        DocumentReference slotRef = firestore.collection("turfs").document(turfId)
                .collection("slots").document(SlotConfig.slotId(selectedDate, heldSlotKey));
        slotRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && "held".equals(snapshot.getString("status"))
                    && uid.equals(snapshot.getString("heldBy"))) {
                slotRef.delete().addOnFailureListener(error ->
                        android.util.Log.e("BookSlotActivity", "Failed to release hold", error));
            }
        }).addOnFailureListener(error ->
                android.util.Log.e("BookSlotActivity", "Failed to check hold before release", error));
    }

    private void cancelHoldAndReturn() {
        releaseHold();
        closePaymentStep();
    }

    private void closePaymentStep() {
        slotHandler.removeCallbacks(countdownRunnable);
        paymentStep.setVisibility(View.GONE);
        bookingContent.setVisibility(View.VISIBLE);
        heldSlotKey = null;
        heldSlotLabel = null;
        heldPaymentMethod = null;
        timeSlotAdapter.clearSelection();
        timeSlotAdapter.refreshStates();
        updateProceedButtonState();
    }

    private void showPaymentError(String message) {
        showMessage(message);
    }

    private void showMessage(String message) {
        if (!isFinishing()) {
            Snackbar.make(findViewById(R.id.bookSlotRoot), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void setupStatusBar() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        if (controller != null) {
            boolean darkMode = (getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            controller.setAppearanceLightStatusBars(!darkMode);
            controller.setAppearanceLightNavigationBars(!darkMode);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    private static class SlotUnavailableException extends RuntimeException {
    }

    private static class HoldExpiredException extends RuntimeException {
    }
}
