package com.example.playo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/** Firestore-backed payment attempts with audit timeline and support reporting. */
public class PaymentHistoryActivity extends AppCompatActivity {
    private RecyclerView recycler;
    private TextView emptyView;
    private TextView attentionBanner;
    private ProgressBar loadingView;
    private TextView errorView;
    private final ArrayList<Payment> payments = new ArrayList<>();
    private PaymentAdapter adapter;
    private final SupportRepository supportRepository = new SupportRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment_history);
        PressEffect.attachToButtons(findViewById(android.R.id.content));
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        final int baseToolbarHeight = toolbar.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), v.getPaddingBottom());
            android.view.ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = baseToolbarHeight + bars.top;
            v.setLayoutParams(lp);
            return insets;
        });
        recycler = findViewById(R.id.paymentsRecycler);
        emptyView = findViewById(R.id.emptyView);
        attentionBanner = findViewById(R.id.attentionBanner);
        loadingView = findViewById(R.id.loadingView);
        errorView = findViewById(R.id.errorView);
        adapter = new PaymentAdapter(payments, this::showPaymentDetails);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        errorView.setOnClickListener(v -> loadPayments());
        ReconcileHelper.run(this);
        loadPayments();
    }

    @Override
    protected void onStop() {
        AnimUtils.cancelTree(recycler);
        super.onStop();
    }

    private void loadPayments() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            showError("Please sign in to view payment history.");
            return;
        }
        loadingView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        FirebaseFirestore.getInstance().collection("payments").whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    payments.clear();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        payments.add(PaymentRepository.fromDocument(document));
                    }
                    Collections.sort(payments, (left, right) -> Long.compare(time(right.getCreatedAt()), time(left.getCreatedAt())));
                    adapter.notifyDataSetChanged();
                    loadingView.setVisibility(View.GONE);
                    recycler.setVisibility(payments.isEmpty() ? View.GONE : View.VISIBLE);
                    emptyView.setVisibility(payments.isEmpty() ? View.VISIBLE : View.GONE);
                    int needsReview = 0;
                    for (Payment payment : payments) if ("needs_review".equals(payment.getStatus())) needsReview++;
                    attentionBanner.setVisibility(needsReview == 0 ? View.GONE : View.VISIBLE);
                    attentionBanner.setText(needsReview + (needsReview == 1
                            ? " payment needs attention" : " payments need attention"));
                    AnimUtils.staggeredEntrance(recycler);
                }).addOnFailureListener(error -> {
                    loadingView.setVisibility(View.GONE);
                    ErrorLogger.log(this, "PaymentHistory", "load_payments", error);
                    showError("Payment history could not be loaded. Tap to retry.");
                });
    }

    private long time(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toDate().getTime();
    }

    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private void showPaymentDetails(Payment payment) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 32);
        TextView title = text(payment.getTurfName(), 20, true);
        root.addView(title);
        root.addView(text("Payment ID: " + value(payment.getPaymentId()), 14, false));
        TextView booking = text("Booking ID: " + value(payment.getBookingId()), 14, false);
        root.addView(booking);
        TextView method = text("Method: " + value(payment.getMethod()) + "   Amount: ₹" + (int) payment.getAmount(), 14, false);
        root.addView(method);
        TextView copy = text("Copy payment ID", 14, true);
        copy.setTextColor(getColor(R.color.colorPrimary));
        copy.setPadding(0, 16, 0, 16);
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Payment ID", payment.getPaymentId()));
            Snackbar.make(root, "Payment ID copied", Snackbar.LENGTH_SHORT).show();
        });
        root.addView(copy);
        if (payment.getFailureMessage() != null) root.addView(text("Message: " + payment.getFailureMessage(), 14, false));
        root.addView(text("Timeline", 16, true));
        TextView timeline = text("Loading timeline...", 14, false);
        root.addView(timeline);
        if ("failed".equals(payment.getStatus()) || "timeout".equals(payment.getStatus())
                || "needs_review".equals(payment.getStatus())) {
            TextView report = text("Report a problem", 15, true);
            report.setTextColor(getColor(R.color.colorPrimary));
            report.setPadding(0, 20, 0, 8);
            report.setOnClickListener(v -> showReportDialog(payment, dialog));
            root.addView(report);
        }
        dialog.setContentView(root);
        dialog.show();
        FirebaseFirestore.getInstance().collection("payments").document(payment.getPaymentId())
                .collection("events").get().addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> events = new ArrayList<>(snapshot.getDocuments());
                    events.sort(Comparator.comparingLong(event -> time(event.getTimestamp("createdAt"))));
                    StringBuilder builder = new StringBuilder();
                    for (DocumentSnapshot event : events) {
                        builder.append(label(event.getString("type"))).append("\n")
                                .append(event.getString("message") == null ? "" : event.getString("message"))
                                .append("\n\n");
                    }
                    timeline.setText(builder.length() == 0 ? "No events recorded." : builder.toString().trim());
                }).addOnFailureListener(error -> timeline.setText("Timeline unavailable."));
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTextSize(size);
        view.setPadding(0, 6, 0, 6);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private void showReportDialog(Payment payment, BottomSheetDialog sheet) {
        EditText input = new EditText(this);
        input.setHint("Tell us what went wrong");
        input.setMinLines(3);
        new MaterialAlertDialogBuilder(this).setTitle("Report a problem").setView(input)
                .setNegativeButton("Cancel", null).setPositiveButton("Submit", (dialog, which) -> {
                    supportRepository.createTicket(input.getText().toString(), payment.getPaymentId(),
                            payment.getBookingId(), new PaymentRepository.Callback<Void>() {
                                @Override public void onSuccess(Void value) {
                                    sheet.dismiss();
                                    Snackbar.make(findViewById(R.id.paymentsRecycler),
                                            "Thanks, we will look into it", Snackbar.LENGTH_LONG).show();
                                }
                                @Override public void onFailure(Exception error) {
                                    ErrorLogger.log(PaymentHistoryActivity.this, "PaymentHistory", "create_ticket", error);
                                }
                            });
                }).show();
    }

    private String label(String type) {
        if (type == null) return "Payment update";
        return type.replace('_', ' ');
    }

    private String value(String value) { return value == null || value.isEmpty() ? "Not available" : value; }
}
