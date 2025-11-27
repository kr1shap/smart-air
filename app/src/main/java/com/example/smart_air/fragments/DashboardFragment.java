package com.example.smart_air.fragments;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.smart_air.R;

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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class DashboardFragment extends Fragment {

    private float x1, x2;
    private final int MIN_DISTANCE = 150;
    private View zoneBar;

    private String zoneColour = "none";
    private View barContainer;
    private LineChart zoneChart;
    private BarChart rescueChart;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String CHILD_ID = FirebaseAuth.getInstance().getUid();


    // view creation

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        zoneBar = view.findViewById(R.id.zoneBar);
        rescueChart = view.findViewById(R.id.rescueChart);
        zoneChart = view.findViewById(R.id.zoneChart);

        barContainer = view.findViewById(R.id.barContainer);
        barContainer.post(() -> updateZoneBar());
        loadTodayZone();
        super.onViewCreated(view, savedInstanceState);


        Button btnReport = view.findViewById(R.id.btnProviderReport);
        btnReport.setOnClickListener(v -> generateProviderReport(4));

        btnReport.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ProviderReportFragment())
                    .addToBackStack(null)
                    .commit();
        });

        db = FirebaseFirestore.getInstance();

        loadZoneHistory();
        loadWeeklyRescues();

        final ViewFlipper flipper = view.findViewById(R.id.trendsCarousel);
        if (flipper == null) {
            return;
        }
        flipper.post(() -> {
            flipper.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        x2 = event.getX();
                        float deltaX = x2 - x1;

                        if (Math.abs(deltaX) > MIN_DISTANCE) {
                            if (deltaX > 0) {
                                flipper.showPrevious();
                            } else {
                                flipper.showNext();
                            }

                            updateDots(flipper.getDisplayedChild(), view);
                        }
                        return true;
                }
                return false;
            });

            updateDots(0, view);

        });

    }


    // zone widget
    private void loadTodayZone() {
        String childId = FirebaseAuth.getInstance().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("dailycheckin")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        zoneColour = doc.getString("zoneColour");
                        if (zoneColour == null) zoneColour = "none";
                    } else {
                        zoneColour = "none";
                    }

                    updateZoneBar();
                })
                .addOnFailureListener(e -> {
                    zoneColour = "none";
                    updateZoneBar();
                });
    }

    private void updateZoneBar() {
        if (barContainer == null || zoneBar == null) return;

        int maxWidth = barContainer.getWidth();
        int newWidth;

        if ("green".equalsIgnoreCase(zoneColour)) {
            newWidth = (int) (0.33f * maxWidth);
            zoneBar.setBackgroundColor(Color.parseColor("#31ad36"));
        }
        else if ("yellow".equalsIgnoreCase(zoneColour)) {
            newWidth = (int) (0.66f * maxWidth);
            zoneBar.setBackgroundColor(Color.parseColor("#ffcc12"));
        }
        else if ("red".equalsIgnoreCase(zoneColour)) {
            newWidth = (int) (1.00f * maxWidth);
            zoneBar.setBackgroundColor(Color.parseColor("#b50000"));
        }
        else {
            newWidth = 0;
            zoneBar.setBackgroundColor(Color.GRAY);
        }

        ViewGroup.LayoutParams params = zoneBar.getLayoutParams();
        params.width = newWidth;
        zoneBar.setLayoutParams(params);
    }


    // weekly rescue and last rescue widgets
    private void loadWeeklyRescues() {

        String childId = FirebaseAuth.getInstance().getUid();

        db.collection("IncidentLog")
                .document(childId)
                .collection("TriageSession")
                .get()
                .addOnSuccessListener(sessionSnap -> {

                    ArrayList<Long> timestamps = new ArrayList<>();

                    if (sessionSnap.isEmpty()) {

                        updateWeeklyRescuesUI(0, "No rescues yet");
                        return;
                    }

                    final int totalSessions = sessionSnap.size();
                    final int[] loadedSessions = {0};

                    for (DocumentSnapshot sessionDoc : sessionSnap) {

                        db.collection("IncidentLog")
                                .document(childId)
                                .collection("TriageSession")
                                .document(sessionDoc.getId())
                                .collection("rescueAttempts")
                                .get()
                                .addOnSuccessListener(rescueSnap -> {

                                    for (DocumentSnapshot rescueDoc : rescueSnap) {
                                        Long ts = rescueDoc.getLong("timestamp");
                                        if (ts != null) timestamps.add(ts);
                                    }

                                    loadedSessions[0]++;

                                    if (loadedSessions[0] == totalSessions) {
                                        processRescues(timestamps);
                                    }

                                });
                    }

                });
    }

    private void processRescues(ArrayList<Long> timestamps) {

        int[] counts = countRescuesThisWeek(timestamps);
        int weeklyTotal = 0;
        for (int c : counts) weeklyTotal += c;

        String lastRescueDate = "No rescues yet";

        if (!timestamps.isEmpty()) {
            long lastTs = Collections.max(timestamps);

            LocalDate date = Instant.ofEpochMilli(lastTs)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            lastRescueDate = date.toString();
        }

        updateWeeklyRescuesUI(weeklyTotal, lastRescueDate);

        drawWeeklyRescueChart(counts);
        drawWeeklyRescueChart(counts);
    }

    private void updateWeeklyRescuesUI(int weeklyTotal, String lastRescueDate) {
        TextView txtWeekly = getView().findViewById(R.id.tvWeeklyRescues);
        TextView txtLastRescue = getView().findViewById(R.id.tvLastRescue);

        txtWeekly.setText("Rescues this week: " + weeklyTotal);
        txtLastRescue.setText("Last rescue: " + lastRescueDate);
    }


    // trends widget
    private void updateDots(int index, View view) {

        View dot1 = view.findViewById(R.id.dot1);
        View dot2 = view.findViewById(R.id.dot2);
        View dot3 = view.findViewById(R.id.dot3);

        dot1.setBackgroundResource(index == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
        dot2.setBackgroundResource(index == 1 ? R.drawable.dot_active : R.drawable.dot_inactive);
        dot3.setBackgroundResource(index == 2 ? R.drawable.dot_active : R.drawable.dot_inactive);
    }

    private void loadUserRole() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String role = doc.getString("role");
                    if (role != null) {
                        applyRoleUI(role.toLowerCase());
                    }
                });
    }

    private void applyRoleUI(String role) {

        View todaysZoneWidget = getView().findViewById(R.id.zoneSection);
        View sevenDayChart = getView().findViewById(R.id.trendSection);
        View providerReportButton = getView().findViewById(R.id.btnProviderReport);
        View manageChildrenButton = getView().findViewById(R.id.btnManageChildren);

        switch (role) {

            case "parent":
                todaysZoneWidget.setVisibility(View.VISIBLE);
                sevenDayChart.setVisibility(View.VISIBLE);
                providerReportButton.setVisibility(View.VISIBLE);
                manageChildrenButton.setVisibility(View.VISIBLE);
                break;

            case "child":
                todaysZoneWidget.setVisibility(View.VISIBLE);
                sevenDayChart.setVisibility(View.VISIBLE);
                providerReportButton.setVisibility(View.GONE);
                manageChildrenButton.setVisibility(View.GONE);
                break;

            case "provider":
                todaysZoneWidget.setVisibility(View.GONE);
                sevenDayChart.setVisibility(View.VISIBLE);
                providerReportButton.setVisibility(View.VISIBLE);
                manageChildrenButton.setVisibility(View.GONE);
                break;

            default:
                todaysZoneWidget.setVisibility(View.GONE);
                sevenDayChart.setVisibility(View.GONE);
                providerReportButton.setVisibility(View.GONE);
                manageChildrenButton.setVisibility(View.GONE);
        }
    }

    private void loadZoneHistory() {

        LocalDate today = LocalDate.now();
        Entry[] entryArray = new Entry[7];

        String childId = FirebaseAuth.getInstance().getUid();

        for (int i = 0; i < 7; i++) {

            LocalDate day = today.minusDays(6 - i);
            String dateKey = day.toString();
            int index = i;

            db.collection("dailycheckin")
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
        drawZoneHistoryChart(new ArrayList<>(Arrays.asList(entryArray)));
    }

    private void drawZoneHistoryChart(ArrayList<Entry> entries) {

        zoneChart.setTouchEnabled(false);
        zoneChart.setPinchZoom(false);
        zoneChart.setDoubleTapToZoomEnabled(false);
        zoneChart.setDragEnabled(false);
        zoneChart.setScaleEnabled(false);
        zoneChart.setHighlightPerTapEnabled(false);
        zoneChart.setHighlightPerDragEnabled(false);

        LineDataSet dataSet = new LineDataSet(entries, "Zone (Past 7 Days)");
        dataSet.setColor(Color.BLACK);
        dataSet.setCircleColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);

        YAxis left = zoneChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(30f);
        left.setDrawGridLines(false);
        zoneChart.getAxisRight().setEnabled(false);

        String[] labels = {"S", "M", "T", "W", "T", "F", "S"};

        XAxis x = zoneChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setLabelCount(7, true);
        x.setDrawGridLines(false);

        x.setAxisMaximum(6.3f);
        x.setAvoidFirstLastClipping(false);

        x.setEnabled(true);
        x.setDrawLabels(true);

        zoneChart.setExtraBottomOffset(20f);

        zoneChart.getDescription().setEnabled(false);
        zoneChart.getLegend().setEnabled(false);

        zoneChart.setData(new LineData(dataSet));
        zoneChart.invalidate();
    }


    private LocalDate getStartOfWeek() {
        LocalDate today = LocalDate.now();
        return today.with(DayOfWeek.SUNDAY);
    }

    private LocalDate getEndOfWeek() {
        LocalDate today = LocalDate.now();
        return today.with(DayOfWeek.SATURDAY);
    }

    private int[] countRescuesThisWeek(ArrayList<Long> timestamps) {
        int[] counts = new int[7];

        LocalDate start = getStartOfWeek();
        LocalDate end = getEndOfWeek();

        for (long ts : timestamps) {
            LocalDate date = Instant.ofEpochMilli(ts)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            if (!date.isBefore(start) && !date.isAfter(end)) {
                int dayIndex = date.getDayOfWeek().getValue() % 7;

                counts[dayIndex]++;
            }
        }

        return counts;
    }

    private void drawWeeklyRescueChart(int[] counts) {
        ArrayList<BarEntry> entries = buildEntriesForWeek(counts);

        BarDataSet dataSet = new BarDataSet(entries, "Rescue Uses");
        dataSet.setColor(Color.parseColor("#3F51B5"));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        XAxis xAxis = rescueChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        xAxis.setDrawGridLines(false);

        rescueChart.getAxisRight().setEnabled(false);
        rescueChart.getAxisLeft().setAxisMinimum(0f);

        rescueChart.setData(barData);
        rescueChart.setFitBars(true);
        rescueChart.invalidate();
    }

    private ArrayList<BarEntry> buildEntriesForWeek(int[] counts) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, counts[i]));
        }
        return entries;
    }

    public void generateProviderReport(int months) {

        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusMonths(months);

        long fromMillis = fromDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        Task<QuerySnapshot> triageTask = db.collection("IncidentLog")
                .document(CHILD_ID)
                .collection("TriageSession")
                .whereGreaterThanOrEqualTo("timestamp", fromMillis)
                .get();

        triageTask.addOnSuccessListener(triageSnap -> {

            List<Task<QuerySnapshot>> rescueTasks = new ArrayList<>();
            List<DocumentSnapshot> triageDocs = triageSnap.getDocuments();

            if (triageDocs.isEmpty()) {
                loadZoneAndBuildPdf(months, 0, 0L, new ArrayList<>());
                return;
            }

            for (DocumentSnapshot sessionDoc : triageDocs) {
                Task<QuerySnapshot> t = sessionDoc.getReference()
                        .collection("rescueAttempts")
                        .get();
                rescueTasks.add(t);
            }

            Tasks.whenAllSuccess(rescueTasks)
                    .addOnSuccessListener(results -> {

                        int totalRescues = 0;
                        long latestRescueTs = 0L;

                        for (Object obj : results) {
                            QuerySnapshot snap = (QuerySnapshot) obj;

                            for (DocumentSnapshot rescueDoc : snap.getDocuments()) {
                                Long ts = rescueDoc.getLong("timestamp");
                                if (ts != null) {
                                    totalRescues++;
                                    if (ts > latestRescueTs) {
                                        latestRescueTs = ts;
                                    }
                                }
                            }
                        }

                        List<DocumentSnapshot> notableTriage = new ArrayList<>(triageDocs);

                        loadZoneAndBuildPdf(months, totalRescues, latestRescueTs, notableTriage);
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error loading rescues: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading triage sessions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadZoneAndBuildPdf(int months,
                                     int totalRescues,
                                     long latestRescueTs,
                                     List<DocumentSnapshot> triageDocs) {

        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusMonths(months);

        db.collection("dailycheckin")
                .document(CHILD_ID)
                .collection("entries")
                .get()
                .addOnSuccessListener(entriesSnap -> {

                    int greenCount = 0;
                    int yellowCount = 0;
                    int redCount = 0;

                    for (DocumentSnapshot doc : entriesSnap.getDocuments()) {

                        String docId = doc.getId();
                        try {
                            LocalDate entryDate = LocalDate.parse(docId);
                            if (entryDate.isBefore(fromDate)) {
                                continue;
                            }
                        } catch (Exception ignored) {
                            continue;
                        }

                        String zone = doc.getString("zoneColour");
                        if (zone == null) continue;

                        zone = zone.toLowerCase();
                        switch (zone) {
                            case "green":
                                greenCount++;
                                break;
                            case "yellow":
                                yellowCount++;
                                break;
                            case "red":
                                redCount++;
                                break;
                        }
                    }

                    buildProviderReportPdf(months,
                            totalRescues,
                            latestRescueTs,
                            greenCount,
                            yellowCount,
                            redCount,
                            triageDocs);

                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error loading daily check-ins: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void buildProviderReportPdf(int months,
                                        int totalRescues,
                                        long latestRescueTs,
                                        int greenCount,
                                        int yellowCount,
                                        int redCount,
                                        List<DocumentSnapshot> triageDocs) {

        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int centerX = pageInfo.getPageWidth() / 2;
        int y = 80;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(28);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Provider Report", centerX, y, paint);

        y += 40;
        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Past " + months + " months", centerX, y, paint);

        paint.setTextAlign(Paint.Align.LEFT);
        y += 40;

        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Rescue frequency", 40, y, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 25;
        canvas.drawText("Total rescues in period: " + totalRescues, 60, y, paint);

        y += 25;
        String lastRescueText;
        if (latestRescueTs == 0L) {
            lastRescueText = "Last rescue: none recorded in this period";
        } else {
            LocalDate lastDate = Instant.ofEpochMilli(latestRescueTs)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            String formatted = lastDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            lastRescueText = "Last rescue: " + formatted;
        }
        canvas.drawText(lastRescueText, 60, y, paint);

        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Zone distribution over time", 40, y, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 25;
        canvas.drawText("Green days:  " + greenCount, 60, y, paint);
        y += 20;
        canvas.drawText("Yellow days: " + yellowCount, 60, y, paint);
        y += 20;
        canvas.drawText("Red days:    " + redCount, 60, y, paint);

        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Notable triage incidents", 40, y, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 25;

        int maxIncidentsToShow = 3;
        int count = 0;
        for (DocumentSnapshot triageDoc : triageDocs) {
            if (count >= maxIncidentsToShow) break;
            count++;

            Long ts = triageDoc.getLong("timestamp");
            String severity = triageDoc.getString("severity");
            String trigger = triageDoc.getString("trigger");

            String dateStr = "Unknown date";
            if (ts != null) {
                LocalDate d = Instant.ofEpochMilli(ts)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                dateStr = d.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            }

            canvas.drawText("- " + dateStr, 60, y, paint);
            y += 18;
            if (severity != null) {
                canvas.drawText("  Severity: " + severity, 80, y, paint);
                y += 18;
            }
            if (trigger != null) {
                canvas.drawText("  Trigger: " + trigger, 80, y, paint);
                y += 18;
            }
            y += 10;
        }

        if (triageDocs.isEmpty()) {
            canvas.drawText("No triage incidents recorded in this period.", 60, y, paint);
        }

        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Controller adherence", 40, y, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 25;
        canvas.drawText("Planned vs actual controller use: (to be added once schedule is defined)", 60, y, paint);

        y += 40;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Symptom burden", 40, y, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 25;
        canvas.drawText("Problem day counts: (to be added once symptom fields are finalized)", 60, y, paint);

        pdf.finishPage(page);

        File path = new File(requireContext().getExternalFilesDir(null), "provider_report.pdf");

        try {
            FileOutputStream fos = new FileOutputStream(path);
            pdf.writeTo(fos);
            fos.close();
            Toast.makeText(getContext(), "PDF saved: " + path.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdf.close();
        openPdf(path);
    }

    private void openPdf(File file) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);
    }


}