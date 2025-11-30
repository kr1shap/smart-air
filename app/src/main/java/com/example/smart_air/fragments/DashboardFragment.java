package com.example.smart_air.fragments;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.R;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {
    private SharedChildViewModel sharedModel;
    private float x1, x2;
    private static final int MIN_DISTANCE = 150;
    private ViewFlipper trendsCarousel;
    private View zoneBar;
    private View barContainer;
    private LineChart pefChart;
    private BarChart rescueChart;
    private String zoneColour = "none";
    private String parentId;
    private String childId;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private boolean isChildUser = false;
    private TextView tvWeeklyRescues;
    private TextView tvRescueName;
    private TextView tvRescuePurchase;
    private TextView tvRescueExpiry;
    private TextView tvControllerName;
    private TextView tvControllerPurchase;
    private TextView tvControllerExpiry;
    private String correspondingUid;
    private String userRole;
    private AuthRepository repo;
    private TextView tvLatestRescue;
    private int currentTrendIndex = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new AuthRepository();
        TextView tvTitle = view.findViewById(R.id.tvDashboardTitle);
        View root = view;

        // using viewmodel to load dashboard

        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);

        // linking xml ids to fragment
        zoneBar = view.findViewById(R.id.zoneBar);
        barContainer = view.findViewById(R.id.barContainer);
        rescueChart = view.findViewById(R.id.rescueChart);
        pefChart = view.findViewById(R.id.pefChart);
        tvWeeklyRescues = view.findViewById(R.id.tvWeeklyRescues);

        tvRescueName = view.findViewById(R.id.tvRescueName);
        tvRescuePurchase = view.findViewById(R.id.tvRescuePurchase);
        tvRescueExpiry = view.findViewById(R.id.tvRescueExpiry);

        tvControllerName = view.findViewById(R.id.tvControllerName);
        tvControllerPurchase = view.findViewById(R.id.tvControllerPurchase);
        tvControllerExpiry = view.findViewById(R.id.tvControllerExpiry);
        tvLatestRescue = view.findViewById(R.id.tvLastRescue);


        Button btnManage = view.findViewById(R.id.btnManageChildren);
        Button btnProviderReport = view.findViewById(R.id.btnProviderReport);


        Spinner trendSpinner = view.findViewById(R.id.spinnerTrendRange);
        trendsCarousel = view.findViewById(R.id.trendsCarousel);

        // to help with trend animation sliding correctly
        Animation inLeft = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_right);
        Animation outLeft = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_left);
        Animation inRight = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_left);
        Animation outRight = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_right);

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                freezeOtherPage();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                unfreezeAllPages();
            }

            @Override public void onAnimationRepeat(Animation animation) {}
        };

        inLeft.setAnimationListener(listener);
        outLeft.setAnimationListener(listener);
        inRight.setAnimationListener(listener);
        outRight.setAnimationListener(listener);

        trendsCarousel.setInAnimation(inLeft);
        trendsCarousel.setOutAnimation(outLeft);

        // setOnTouchListeners
        trendsCarousel.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    return true;

                case MotionEvent.ACTION_UP:
                    x2 = event.getX();
                    float deltaX = x2 - x1;

                    if (Math.abs(deltaX) > MIN_DISTANCE) {

                        // swipe right
                        if (deltaX > 0) {
                            if (currentTrendIndex == 0) {
                                return true;
                            }

                            trendsCarousel.setInAnimation(getContext(), R.anim.slide_in_left);
                            trendsCarousel.setOutAnimation(getContext(), R.anim.slide_out_right);
                            trendsCarousel.showPrevious();

                            currentTrendIndex--;
                        }

                        // swipe left
                        else {
                            if (currentTrendIndex == 1) {
                                return true;
                            }

                            trendsCarousel.setInAnimation(getContext(), R.anim.slide_in_right);
                            trendsCarousel.setOutAnimation(getContext(), R.anim.slide_out_left);
                            trendsCarousel.showNext();

                            currentTrendIndex++;
                        }

                        updateDots(currentTrendIndex, root);
                    }

                    return true;
            }
            return false;
        });

        // use viewmodel to load role and dashboard

        // first checks the role
        sharedModel.getCurrentRole().observe(getViewLifecycleOwner(), role -> {
            if (role == null) return;

            userRole = role;

            if (role.equals("child")|| role.equals("provider")) {

                // loads childId and childName from firestore db
                loadUserAndChildIds(tvTitle, () -> {
                    correspondingUid = childId;
                    loadDashboardForChild(correspondingUid);
                });

                btnProviderReport.setVisibility(View.GONE);
                btnManage.setVisibility(View.GONE);
            } else if (role.equals("parent")) {

                // show full dashboard
                btnProviderReport.setVisibility(View.VISIBLE);
                btnManage.setVisibility(View.VISIBLE);

                btnProviderReport.setOnClickListener(v -> {
                    ProviderReportFragment frag = new ProviderReportFragment();

                    Bundle args = new Bundle();
                    args.putString("childId", correspondingUid);
                    frag.setArguments(args);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, frag)
                            .addToBackStack(null)
                            .commit();
                });



            }
        });

        // after getting full list of children
        sharedModel.getAllChildren().observe(getViewLifecycleOwner(), children -> {
            if (children == null || children.isEmpty()) return;
            Integer idx = sharedModel.getCurrentChild().getValue();
            if (idx == null) idx = 0;

            correspondingUid = children.get(idx).getChildUid();

            if (!"child".equals(userRole)) {
                Child child = children.get(idx);
                tvTitle.setText(child.getName() + "’s Dashboard");
                loadDashboardForChild(correspondingUid);
            }
        });

        // for when parent switches child
        sharedModel.getCurrentChild().observe(getViewLifecycleOwner(), idx -> {
            List<Child> list = sharedModel.getAllChildren().getValue();
            if (list == null || list.isEmpty() || idx == null) return;

            correspondingUid = list.get(idx).getChildUid();

            if (!"child".equals(userRole)) {
                loadDashboardForChild(correspondingUid);
            }
        });

        // trend dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Past 7 Days", "Past 30 Days"}
        );
        trendSpinner.setAdapter(adapter);

        trendSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (childId == null) return;

                if (position == 0) {
                    loadWeeklyRescues(7);
                    loadPEFTrend(7);
                } else {
                    loadWeeklyRescues(30);
                    loadPEFTrend(30);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }

    private void loadUserAndChildIds(TextView titleView, @Nullable Runnable onComplete) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("children")
                .document(uid)
                .get()
                .addOnSuccessListener(childDoc -> {

                    if (childDoc.exists()) {

                        isChildUser = true;
                        childId = uid;
                        parentId = childDoc.getString("parentUid");

                        String childName = childDoc.getString("name");
                        if (childName != null) {
                            titleView.setText(childName + "’s Dashboard");
                        }

                        if (onComplete != null) onComplete.run();
                        return;
                    }

                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(parentDoc -> {
                                if (parentDoc.exists()) {
                                    isChildUser = false;
                                    parentId = uid;

                                    List<String> children = (List<String>) parentDoc.get("childrenUid");
                                    if (children == null || children.isEmpty()) {
                                        Toast.makeText(requireContext(), "No children linked", Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    childId = children.get(0);

                                    titleView.setText("Dashboard");

                                    if (onComplete != null) onComplete.run();
                                } else {
                                    Toast.makeText(requireContext(),
                                            "Profile not found in users or children",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                });
    }

    private void freezeOtherPage() {
        int displayed = trendsCarousel.getDisplayedChild();

        for (int i = 0; i < trendsCarousel.getChildCount(); i++) {
            View child = trendsCarousel.getChildAt(i);

            if (i != displayed) {
                child.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void unfreezeAllPages() {
        for (int i = 0; i < trendsCarousel.getChildCount(); i++) {
            trendsCarousel.getChildAt(i).setVisibility(View.VISIBLE);
        }
    }

    private void loadDashboardForChild(String childUid) {
        if (childUid == null) return;

        this.childId = childUid;

        loadTodayZone();
        loadZoneHistory();
        loadWeeklyRescues(7);
        loadLatestRescueDate();
        loadPEFTrend(7);
        loadInventory();
    }


    private void loadTodayZone() {
        if (childId == null) return;

        String dateKey = LocalDate.now().toString();

        db.collection("dailyCheckins")
                .document(childId)
                .collection("entries")
                .document(dateKey)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {

                        zoneColour = doc.getString("zoneColour");
                        if (zoneColour == null) zoneColour = "none";

                        Long zoneNum = doc.getLong("zoneNumber");
                        if (zoneNum == null) zoneNum = 0L;

                        updateZoneBar(zoneColour, zoneNum.intValue());

                    } else {
                        updateZoneBar("none", 0);
                    }
                })
                .addOnFailureListener(e -> updateZoneBar("none", 0));
    }

    private void updateZoneBar(String zoneColour, int zoneNum) {
        if (barContainer == null || zoneBar == null) return;

        barContainer.post(() -> {
            int maxWidth = barContainer.getWidth();
            int newWidth;

            newWidth = (int) (maxWidth * (zoneNum / 100f));

            if ("green".equalsIgnoreCase(zoneColour)) {
                zoneBar.setBackgroundColor(Color.parseColor("#31ad36"));
            } else if ("yellow".equalsIgnoreCase(zoneColour)) {
                zoneBar.setBackgroundColor(Color.parseColor("#ffcc12"));
            } else if ("red".equalsIgnoreCase(zoneColour)) {
                zoneBar.setBackgroundColor(Color.parseColor("#b50000"));
            } else {

                if (zoneNum <= 33) {
                    zoneBar.setBackgroundColor(Color.parseColor("#31ad36"));
                } else if (zoneNum <= 66) {
                    zoneBar.setBackgroundColor(Color.parseColor("#ffcc12"));
                } else if (zoneNum <= 100) {
                    zoneBar.setBackgroundColor(Color.parseColor("#b50000"));
                } else {
                    zoneBar.setBackgroundColor(Color.GRAY);
                }
            }

            ViewGroup.LayoutParams params = zoneBar.getLayoutParams();
            params.width = newWidth;
            zoneBar.setLayoutParams(params);
        });
    }

    private void loadWeeklyRescues(int days) {
        if (childId == null) return;

        LocalDate cutoff = LocalDate.now().minusDays(days - 1);

        db.collection("incidentLog")
                .document(childId)
                .collection("triageSessions")
                .get()
                .addOnSuccessListener(snap -> {

                    int[] counts = new int[days];

                    for (DocumentSnapshot doc : snap) {

                        Object rawRescue = doc.get("rescueAttempts");
                        int rescueCount = 0;

                        if (rawRescue instanceof Number) {
                            rescueCount = ((Number) rawRescue).intValue();
                        } else if (rawRescue instanceof String) {
                            try {
                                rescueCount = Integer.parseInt((String) rawRescue);
                            } catch (Exception ignored) {}
                        }

                        if (rescueCount <= 0) continue;


                        Timestamp ts = doc.getTimestamp("date");
                        if (ts == null) continue;


                        LocalDate rescueDate = ts.toDate()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();

                        if (rescueDate.isBefore(cutoff)) continue;


                        int index = (int) ChronoUnit.DAYS.between(cutoff, rescueDate);

                        if (index >= 0 && index < days) {
                            counts[index] += rescueCount;
                        }
                    }

                    drawRescueChartDynamic(counts, days);

                    int total = 0;
                    for (int c : counts) total += c;

                    if (tvWeeklyRescues != null) {
                        tvWeeklyRescues.setText(total + " rescues");
                    }

                });
    }

    private void loadLatestRescueDate() {
        if (childId == null) return;

        db.collection("incidentLog")
                .document(childId)
                .collection("triageSessions")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    TextView tvLatestRescue = requireView().findViewById(R.id.tvLastRescue);

                    if (snapshot.isEmpty()) {
                        tvLatestRescue.setText("No rescues yet");
                        return;
                    }

                    Date latestRescueDate = null;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Long count = doc.getLong("rescueAttempts");
                        Timestamp ts = doc.getTimestamp("date");

                        if (count != null && ts != null && count >= 1) {
                            latestRescueDate = ts.toDate();
                            break;
                        }
                    }

                    if (latestRescueDate == null) {
                        tvLatestRescue.setText("No rescues yet");
                    } else {
                        String formatted = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                .format(latestRescueDate);
                        tvLatestRescue.setText(formatted);
                    }
                })
                .addOnFailureListener(e -> {
                    TextView tvLatestRescue = requireView().findViewById(R.id.tvLastRescue);
                    tvLatestRescue.setText("Error loading");
                });
    }

    private void drawRescueChartDynamic(int[] counts, int days) {
        if (rescueChart == null) return;


        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        LocalDate start = LocalDate.now().minusDays(days - 1);

        for (int i = 0; i < days; i++) {
            entries.add(new BarEntry(i, counts[i]));
            LocalDate date = start.plusDays(i);
            labels.add(date.getMonthValue() + "/" + date.getDayOfMonth());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Rescue Attempts");
        dataSet.setColor(Color.parseColor("#003B7A"));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        XAxis xAxis = rescueChart.getXAxis();


        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (days == 30) {
                    if (index == 0) return labels.get(0);
                    if (index == days - 1) return labels.get(days - 1);
                    return "";
                } else {
                    return labels.get(index);
                }
            }
        });

        if (days == 30) {
            xAxis.setTextSize(2f);
            xAxis.setLabelRotationAngle(280f);
        } else {
            xAxis.setTextSize(10f);
            xAxis.setLabelRotationAngle(0f);
        }

        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(days);
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);

        rescueChart.getAxisLeft().setAxisMinimum(0);
        rescueChart.getAxisRight().setEnabled(false);
        rescueChart.getLegend().setEnabled(false);

        rescueChart.getDescription().setEnabled(false);
        rescueChart.setFitBars(true);
        rescueChart.setTouchEnabled(false);
        rescueChart.setPinchZoom(false);

        rescueChart.setData(barData);
        rescueChart.invalidate();
    }

    private void loadPEFTrend(int days) {
        if (childId == null) return;

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        Map<LocalDate, Integer> pefByDate = new HashMap<>();

        FirebaseFirestore.getInstance()
                .collection("dailyCheckins")
                .document(childId)
                .collection("entries")
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String dateKey = doc.getId();
                        LocalDate entryDate;

                        try {
                            entryDate = LocalDate.parse(dateKey);
                        } catch (Exception e) {
                            continue;
                        }

                        if (entryDate.isBefore(startDate) || entryDate.isAfter(today)) {
                            continue;
                        }

                        Long pef = doc.getLong("pef");
                        if (pef != null) {
                            pefByDate.put(entryDate, pef.intValue());
                        }
                    }

                    int[] pefValues = new int[days];
                    for (int i = 0; i < days; i++) {
                        LocalDate d = startDate.plusDays(i);
                        Integer val = pefByDate.get(d);
                        pefValues[i] = (val != null) ? val : 0;
                    }

                    drawPEFLineChart(pefValues, days);
                });
    }

    private void drawPEFLineChart(int[] pefValues, int days) {
        if (pefChart == null) return;

        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            entries.add(new Entry(i, pefValues[i]));
        }

        pefChart.setTouchEnabled(false);
        pefChart.setPinchZoom(false);
        pefChart.setDoubleTapToZoomEnabled(false);
        pefChart.setDragEnabled(false);
        pefChart.setScaleEnabled(false);
        pefChart.setHighlightPerTapEnabled(false);
        pefChart.setHighlightPerDragEnabled(false);
        pefChart.getLegend().setEnabled(false);
        pefChart.getDescription().setEnabled(false);

        LineDataSet dataSet = new LineDataSet(entries, "Best PEF");
        dataSet.setColor(Color.parseColor("#003B7A"));
        dataSet.setCircleColor(Color.parseColor("#003B7A"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        pefChart.setData(new LineData(dataSet));

        XAxis xAxis = pefChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1);

        String[] xLabels = new String[days];
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            xLabels[i] = d.getMonthValue() + "/" + d.getDayOfMonth();
        }

        if (days == 7) {

            xAxis.setEnabled(true);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
            xAxis.setLabelCount(7, true);
        } else {

            xAxis.setEnabled(true);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
            xAxis.setLabelCount(6, true);
        }

        YAxis yAxis = pefChart.getAxisLeft();
        pefChart.getAxisRight().setEnabled(false);

        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(500f);
        yAxis.setGranularity(100f);
        yAxis.setLabelCount(6, true);

        pefChart.invalidate();
    }

    private void updateDots(int index, View root) {
        View dot1 = root.findViewById(R.id.dot1);
        View dot2 = root.findViewById(R.id.dot2);

        if (dot1 != null && dot2 != null) {
            dot1.setBackgroundResource(index == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
            dot2.setBackgroundResource(index == 1 ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    private void loadZoneHistory() {
        if (childId == null) return;

        LocalDate today = LocalDate.now();
        Entry[] entryArray = new Entry[7];

        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(6 - i);
            String dateKey = day.toString();
            int index = i;

            db.collection("dailyCheckins")
                    .document(childId)
                    .collection("entries")
                    .document(dateKey)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String zone = doc.getString("zoneColour");
                            if (zone == null) {
                                entryArray[index] = new Entry(index, 0);
                            } else {
                                switch (zone.toLowerCase()) {
                                    case "green":
                                        entryArray[index] = new Entry(index, 1);
                                        break;
                                    case "yellow":
                                        entryArray[index] = new Entry(index, 2);
                                        break;
                                    case "red":
                                        entryArray[index] = new Entry(index, 3);
                                        break;
                                    default:
                                        entryArray[index] = new Entry(index, 0);
                                }
                            }
                        } else {
                            entryArray[index] = new Entry(index, 0);
                        }

                        checkIfComplete(entryArray);
                    });
        }
    }

    private void checkIfComplete(Entry[] entryArray) {
        for (Entry e : entryArray) {
            if (e == null) return;
        }
    }


    // inventory

    private void loadInventory() {
        if (childId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference rescueRef = db.collection("children")
                .document(childId)
                .collection("inventory")
                .document("rescue");

        DocumentReference controllerRef = db.collection("children")
                .document(childId)
                .collection("inventory")
                .document("controller");

        rescueRef.get().addOnSuccessListener(rescueDoc -> {
            if (rescueDoc.exists()) {
                String name = rescueDoc.getString("name");
                Long amount = rescueDoc.getLong("amount");

                Timestamp purchase = rescueDoc.getTimestamp("purchaseDate");
                Timestamp expiry = rescueDoc.getTimestamp("expiryDate");

                String display = "";

                if (name != null) display += name;
                if (amount != null) display += ":  " + amount;
                tvRescueName.setText(display.isEmpty() ? "" : display);

                tvRescuePurchase.setText(purchase != null ? "Purchase Date: " +  formatDate(purchase) : "-");
                tvRescueExpiry.setText(expiry != null ? "Expiry Date: " + formatDate(expiry) : "-");
            }
        });

        controllerRef.get().addOnSuccessListener(ctrlDoc -> {
            if (ctrlDoc.exists()) {
                String name = ctrlDoc.getString("name");
                Long amount = ctrlDoc.getLong("amount");

                Timestamp purchase = ctrlDoc.getTimestamp("purchaseDate");
                Timestamp expiry = ctrlDoc.getTimestamp("expiryDate");

                String display = "";

                if (name != null) display += name;
                if (amount != null) display += ":  " + amount;
                tvControllerName.setText(display.isEmpty() ? "" : display);
                tvControllerPurchase.setText(purchase != null ? "Purchase Date: " + formatDate(purchase) : "-");
                tvControllerExpiry.setText(expiry != null ? "Expiry Date: " + formatDate(expiry) : "-");
            }
        });
    }

    private String formatDate(Timestamp ts) {
        Date date = ts.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(date);
    }


    public static class TriageIncident {
        String date;
        String symptoms;

        public TriageIncident(String date, String symptoms) {
            this.date = date;
            this.symptoms = symptoms;
        }
    }
}
