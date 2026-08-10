package com.example.playo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;

/**
 * Shows the user's payment history as a list of cards.
 */
public class PaymentHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        // --- Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recycler = findViewById(R.id.paymentsRecycler);
        TextView emptyView = findViewById(R.id.emptyView);

        // --- Sample data ---
        ArrayList<Payment> payments = buildSamplePayments();

        PaymentAdapter adapter = new PaymentAdapter(payments);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        emptyView.setVisibility(payments.isEmpty() ? View.VISIBLE : View.GONE);

        // Sync total spent into the shared profile header stats.
        int total = 0;
        for (Payment p : payments) {
            total += (int) p.getAmount();
        }
        SharedPreferences prefs = getSharedPreferences("playo_prefs", MODE_PRIVATE);
        prefs.edit().putInt("total_spent", total).apply();
    }

    /** Provides sample payments to render (replace with a data source later). */
    private ArrayList<Payment> buildSamplePayments() {
        ArrayList<Payment> list = new ArrayList<>();
        list.add(new Payment("BK1001", "Green Field Turf", "05 Aug 2026", "UPI", 1500, "Paid"));
        list.add(new Payment("BK1002", "Smash Badminton Court", "28 Jul 2026", "Card", 800, "Paid"));
        list.add(new Payment("BK1003", "Elite Futsal Court", "20 Jul 2026", "UPI", 1800, "Refunded"));
        return list;
    }
}