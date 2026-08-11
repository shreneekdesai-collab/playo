package com.example.playo;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Booking screen: pick a time slot and a payment method, then proceed to pay.
 */
public class BookSlotActivity extends AppCompatActivity {

    private TimeSlotAdapter timeSlotAdapter;
    private PaymentMethodAdapter paymentMethodAdapter;
    private Button btnProceedPay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_slot);

        // --- Edge-to-edge: pad toolbar below the status bar ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bookSlotRoot), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            Log.d("BookSlotActivity", "WindowInsets topInset=" + topInset);
            MaterialToolbar toolbar = findViewById(R.id.bookSlotToolbar);
            if (toolbar != null) {
                toolbar.setPadding(toolbar.getPaddingLeft(), topInset, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            }
            return insets;
        });

        // --- Toolbar back arrow (explicitly tinted so it is always visible) ---
        MaterialToolbar toolbar = findViewById(R.id.bookSlotToolbar);
        Drawable navIcon = ContextCompat.getDrawable(this, R.drawable.ic_back);
        if (navIcon != null) {
            navIcon.setColorFilter(ContextCompat.getColor(this, R.color.toolbar_fg), PorterDuff.Mode.SRC_ATOP);
            toolbar.setNavigationIcon(navIcon);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // --- Status bar: dark icons on the light toolbar background ---
        setupStatusBar();

        btnProceedPay = findViewById(R.id.btnProceedPay);

        setupTimeSlots();
        setupPaymentMethods();

        btnProceedPay.setOnClickListener(v -> {
            String slot = timeSlotAdapter.getSelectedSlot();
            String method = paymentMethodAdapter.getSelectedMethod();
            // TODO: Replace with real payment/booking backend call later.
            Toast.makeText(this,
                    "Booking confirmed for " + slot + " via " + method,
                    Toast.LENGTH_LONG).show();
        });
    }

    private void setupStatusBar() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            // Dark status bar icons are visible on the light/white toolbar background.
            controller.setAppearanceLightStatusBars(true);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private void setupTimeSlots() {
        // TODO: This should come from backend/Firestore later.
        List<String> slots = generateHourlySlots();

        timeSlotAdapter = new TimeSlotAdapter(slots);
        timeSlotAdapter.setOnSlotSelectedListener(slot -> updateProceedButtonState());

        RecyclerView recycler = findViewById(R.id.timeSlotRecycler);
        recycler.setLayoutManager(new GridLayoutManager(this, 4));
        recycler.setAdapter(timeSlotAdapter);
    }

    private void setupPaymentMethods() {
        List<PaymentMethodAdapter.PaymentMethod> methods = new ArrayList<>();
        methods.add(new PaymentMethodAdapter.PaymentMethod(
                "UPI", "Pay by any UPI app", R.drawable.ic_payment));
        methods.add(new PaymentMethodAdapter.PaymentMethod(
                "Credit / Debit Card", "Visa, Mastercard, RuPay", R.drawable.ic_payment));
        methods.add(new PaymentMethodAdapter.PaymentMethod(
                "Net Banking", "All major banks", R.drawable.ic_payment));
        methods.add(new PaymentMethodAdapter.PaymentMethod(
                "Wallets", "Paytm, PhonePe, Amazon Pay", R.drawable.ic_wallet));
        methods.add(new PaymentMethodAdapter.PaymentMethod(
                "Cash on Arrival", "Pay at the venue", R.drawable.ic_payment));

        paymentMethodAdapter = new PaymentMethodAdapter(methods);
        paymentMethodAdapter.setOnPaymentMethodSelectedListener(method -> updateProceedButtonState());

        RecyclerView recycler = findViewById(R.id.paymentMethodRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(paymentMethodAdapter);
    }

    private void updateProceedButtonState() {
        boolean enabled = timeSlotAdapter.getSelectedSlot() != null
                && paymentMethodAdapter.getSelectedMethod() != null;
        btnProceedPay.setEnabled(enabled);
    }

    /** Builds a demo list of hourly slots from 6:00 AM to 10:00 PM. */
    private List<String> generateHourlySlots() {
        List<String> slots = new ArrayList<>();
        String[] labels = {
                "6:00 AM", "7:00 AM", "8:00 AM", "9:00 AM",
                "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM",
                "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM",
                "6:00 PM", "7:00 PM", "8:00 PM", "9:00 PM",
                "10:00 PM"
        };
        for (String label : labels) {
            slots.add(label);
        }
        return slots;
    }
}