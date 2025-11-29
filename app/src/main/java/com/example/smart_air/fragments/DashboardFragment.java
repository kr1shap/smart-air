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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
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
    private float x1, x2;
    private static final int MIN_DISTANCE = 150;
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
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        zoneBar = view.findViewById(R.id.zoneBar);
        barContainer = view.findViewById(R.id.barContainer);
        rescueChart = view.findViewById(R.id.rescueChart);
        pefChart = view.findViewById(R.id.pefChart);
        tvWeeklyRescues = view.findViewById(R.id.tvWeeklyRescues);

        Button btnManage = view.findViewById(R.id.btnManageChildren);
        Button btnProviderReport = view.findViewById(R.id.btnProviderReport);


        TextView tvDashboardTitle = view.findViewById(R.id.tvDashboardTitle);

        loadUserAndChildIds(tvDashboardTitle, () -> {
            if (isChildUser) {
                btnProviderReport.setVisibility(View.GONE);
                btnManage.setVisibility(View.GONE);
            }

            loadTodayZone();
            loadZoneHistory();
            loadWeeklyRescues(7);
            loadLatestRescueDate();
            loadPEFTrend(7);
        });

        Spinner trendSpinner = view.findViewById(R.id.spinnerTrendRange);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Past 7 Days", "Past 30 Days"}
        );
        trendSpinner.setAdapter(adapter);
        trendSpinner.setSelection(0);

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

        Button btnReport = view.findViewById(R.id.btnProviderReport);
        btnReport.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ProviderReportFragment())
                .addToBackStack(null)
                .commit()
        );

        final ViewFlipper flipper = view.findViewById(R.id.trendsCarousel);
        if (flipper != null) {
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
                                    flipper.setInAnimation(requireContext(), R.anim.slide_in_left);
                                    flipper.setOutAnimation(requireContext(), R.anim.slide_out_right);
                                    flipper.showPrevious();
                                } else {
                                    flipper.setInAnimation(requireContext(), R.anim.slide_in_right);
                                    flipper.setOutAnimation(requireContext(), R.anim.slide_out_left);
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
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    TextView tvLatestRescue = requireView().findViewById(R.id.tvLastRescue);

                    if (snapshot.isEmpty()) {
                        tvLatestRescue.setText("No rescues yet");
                        return;
                    }

                    DocumentSnapshot doc = snapshot.getDocuments().get(0);

                    Long count = doc.getLong("rescueAttempts");
                    Timestamp ts = doc.getTimestamp("date");

                    if (count == null || ts == null || count == 0) {
                        tvLatestRescue.setText("No rescues yet");
                        return;
                    }

                    Date date = ts.toDate();
                    String formatted = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            .format(date);

                    tvLatestRescue.setText(formatted);
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
        dataSet.setColor(Color.parseColor("#3F51B5"));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        XAxis xAxis = rescueChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(days);
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);

        rescueChart.getAxisLeft().setAxisMinimum(0);
        rescueChart.getAxisRight().setEnabled(false);

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

        LineDataSet dataSet = new LineDataSet(entries, "Best PEF");
        dataSet.setColor(Color.parseColor("#009688"));
        dataSet.setCircleColor(Color.parseColor("#009688"));
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
        drawZoneHistoryChart(new ArrayList<>(Arrays.asList(entryArray)));
    }

    private void drawZoneHistoryChart(ArrayList<Entry> entries) {
        if (pefChart == null) return;

        pefChart.setTouchEnabled(false);
        pefChart.setPinchZoom(false);
        pefChart.setDoubleTapToZoomEnabled(false);
        pefChart.setDragEnabled(false);
        pefChart.setScaleEnabled(false);
        pefChart.setHighlightPerTapEnabled(false);
        pefChart.setHighlightPerDragEnabled(false);

        LineDataSet dataSet = new LineDataSet(entries, "Zone (Past 7 Days)");
        dataSet.setColor(Color.BLACK);
        dataSet.setCircleColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);

        YAxis left = pefChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(3f);
        left.setDrawGridLines(false);
        pefChart.getAxisRight().setEnabled(false);

        String[] labels = {"S", "M", "T", "W", "T", "F", "S"};

        XAxis x = pefChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setLabelCount(7, true);
        x.setDrawGridLines(false);

        x.setAxisMaximum(6.3f);
        x.setAvoidFirstLastClipping(false);

        x.setEnabled(true);
        x.setDrawLabels(true);

        pefChart.setExtraBottomOffset(20f);

        pefChart.getDescription().setEnabled(false);
        pefChart.getLegend().setEnabled(false);

        pefChart.setData(new LineData(dataSet));
        pefChart.invalidate();
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

    public void generateProviderReport(int months) {
        // TODO: fetch real values from Firestore
        String childName = "John Doe";
        String childDob = "August 12, 2006";
        String parentName = "Jane Doe";
        String parentContact = "faiza.khanc@gmail.com";

        List<TriageIncident> incidents = new ArrayList<>();
        incidents.add(new TriageIncident("Jan 12, 2025", "Wheeze, Cough, Chest Tightness"));
        incidents.add(new TriageIncident("Feb 03, 2025", "Shortness of Breath"));

        PdfDocument pdf = new PdfDocument();
        Paint titlePaint = new Paint();
        Paint textPaint = new Paint();
        Paint boxPaint = new Paint();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTextSize(28);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Provider Report", pageInfo.getPageWidth() / 2f, 80, titlePaint);

        titlePaint.setTextSize(20);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Last " + months + " Months", pageInfo.getPageWidth() / 2f, 120, titlePaint);

        int left = 40;
        int top = 160;
        int right = pageInfo.getPageWidth() - 40;
        int bottom = top + 180;

        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3);
        canvas.drawRect(left, top, right, bottom, boxPaint);

        textPaint.setTextSize(18);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.LEFT);

        int textY = top + 40;
        int padding = 35;

        canvas.drawText("Child’s Name: " + childName, left + 20, textY, textPaint);
        canvas.drawText("Date of Birth: " + childDob, left + 20, textY + padding, textPaint);
        canvas.drawText("Parent’s Name: " + parentName, left + 20, textY + padding * 2, textPaint);
        canvas.drawText("Parent’s Contact: " + parentContact, left + 20, textY + padding * 3, textPaint);

        int tableTop = bottom + 40;
        int headerHeight = 50;

        textPaint.setTextSize(18);
        canvas.drawRect(left, tableTop, right, tableTop + headerHeight, boxPaint);
        canvas.drawText("Triage Incidents", left + 20, tableTop + 32, textPaint);

        int currentY = tableTop + headerHeight;

        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setTextSize(16);

        for (TriageIncident incident : incidents) {
            float symptomsX = left + 200;
            float availableWidth = right - symptomsX - 20;

            List<String> wrappedLines = wrapTextLines(
                    "Symptoms: " + incident.symptoms,
                    textPaint,
                    availableWidth
            );

            float lineHeight = textPaint.getTextSize() + 6;
            float dynamicRowHeight = Math.max(50, lineHeight * wrappedLines.size() + 20);

            canvas.drawRect(left, currentY, right, currentY + dynamicRowHeight, boxPaint);
            canvas.drawText("• " + incident.date, left + 20, currentY + 32, textPaint);

            float lineY = currentY + 32;
            for (String line : wrappedLines) {
                canvas.drawText(line, symptomsX, lineY, textPaint);
                lineY += lineHeight;
            }

            currentY += dynamicRowHeight;
        }

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

    private List<String> wrapTextLines(String text, Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String test = current + word + " ";
            if (paint.measureText(test) > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(word + " ");
            } else {
                current.append(word).append(" ");
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
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

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No PDF viewer found", Toast.LENGTH_SHORT).show();
        }
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
