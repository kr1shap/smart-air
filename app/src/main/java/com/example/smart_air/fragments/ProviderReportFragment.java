package com.example.smart_air.fragments;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.smart_air.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ProviderReportFragment extends Fragment {

    private Spinner spinnerMonths;
    private String childId;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ProviderReportFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerMonths = view.findViewById(R.id.spinnerMonths);
        Button btnGenerate = view.findViewById(R.id.btnGenerate);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        if (getArguments() != null) {
            childId = getArguments().getString("childId");
        }

        if (childId == null) {
            Toast.makeText(requireContext(), "Missing child ID", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        List<String> options = Arrays.asList(
                "Past 3 months",
                "Past 4 months",
                "Past 5 months",
                "Past 6 months"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options);
        spinnerMonths.setAdapter(adapter);

        btnCancel.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        btnGenerate.setOnClickListener(v -> {
            String selected = spinnerMonths.getSelectedItem().toString();
            int months = Integer.parseInt(selected.replaceAll("\\D+", ""));
            generateProviderReport(months);
        });
    }

    private void generateProviderReport(int months) {
        LocalDate now = LocalDate.now();
        LocalDate cutoff = now.minusMonths(months);

        db.collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(childDoc -> {
                    if (!childDoc.exists()) {
                        Toast.makeText(requireContext(), "Child data not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String childName = childDoc.getString("name");
                    Timestamp dobTs = childDoc.getTimestamp("dob");

                    String childDob = dobTs != null ?
                            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    .format(dobTs.toDate())
                            : "-";

                    String parentUid = childDoc.getString("parentUid");

                    if (parentUid == null) {
                        Toast.makeText(requireContext(), "Parent info missing", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("users")
                            .document(parentUid)
                            .get()
                            .addOnSuccessListener(parentDoc -> {

                                String parentEmail = parentDoc.getString("email");
                                if (parentEmail == null) parentEmail = "-";

                                fetchTriageAndDailyData(
                                        months,
                                        cutoff,
                                        childName,
                                        childDob,
                                        parentEmail
                                );
                            });
                });
    }

    private void fetchTriageAndDailyData(int months, LocalDate cutoff, String childName, String childDob, String parentEmail) {
        LocalDate now = LocalDate.now();

        int totalDays = (int) ChronoUnit.DAYS.between(cutoff, now) + 1;
        if (totalDays <= 0) totalDays = 1;

        int[] rescueCounts = new int[totalDays];
        List<TriageIncident> incidents = new ArrayList<>();

        int finalTotalDays = totalDays;
        db.collection("incidentLog")
                .document(childId)
                .collection("triageSessions")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {

                    for (DocumentSnapshot doc : snap) {

                        Timestamp ts = doc.getTimestamp("date");
                        if (ts == null) continue;

                        LocalDate entryDate = ts.toDate().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDate();

                        if (entryDate.isBefore(cutoff) || entryDate.isAfter(now)) continue;

                        String formattedDate =
                                new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        .format(ts.toDate());

                        Object rawFlags = doc.get("flagList");
                        String symptoms = "-";

                        if (rawFlags instanceof List) {
                            List<String> list = (List<String>) rawFlags;
                            if (list != null && !list.isEmpty())
                                symptoms = TextUtils.join(", ", list);
                        } else if (rawFlags instanceof String) {
                            symptoms = (String) rawFlags;
                        }

                        incidents.add(new TriageIncident(formattedDate, symptoms));

                        int rescue = 0;
                        Object rawRescue = doc.get("rescueAttempts");
                        if (rawRescue instanceof Number) rescue = ((Number) rawRescue).intValue();
                        else if (rawRescue instanceof String) {
                            try { rescue = Integer.parseInt((String) rawRescue); } catch (Exception ignored) {}
                        }

                        if (rescue > 0) {
                            int idx = (int) ChronoUnit.DAYS.between(cutoff, entryDate);
                            if (idx >= 0 && idx < finalTotalDays) rescueCounts[idx] += rescue;
                        }
                    }

                    fetchDailyCheckinsForReport(
                            months, cutoff, childName, childDob, parentEmail,
                            incidents, rescueCounts, finalTotalDays
                    );
                });
    }

    private void fetchDailyCheckinsForReport(int months, LocalDate cutoff, String childName, String childDob, String parentEmail, List<TriageIncident> incidents, int[] rescueCounts, int totalDays) {
        LocalDate now = LocalDate.now();

        final int[] problemDays = {0};
        final int[] zoneGreen = {0};
        final int[] zoneYellow = {0};
        final int[] zoneRed = {0};

        db.collection("dailyCheckins")
                .document(childId)
                .collection("entries")
                .get()
                .addOnSuccessListener(snap -> {

                    for (DocumentSnapshot doc : snap) {
                        LocalDate entryDate;
                        try { entryDate = LocalDate.parse(doc.getId()); }
                        catch (Exception e) { continue; }

                        if (entryDate.isBefore(cutoff) || entryDate.isAfter(now)) continue;

                        // symptom burden
                        Object trig = doc.get("triggersparent");
                        boolean has = false;

                        if (trig instanceof List) {
                            List<String> list = (List<String>) trig;
                            has = list != null && !list.isEmpty();
                        } else if (trig instanceof String) {
                            has = !((String) trig).trim().isEmpty();
                        }
                        if (has) problemDays[0]++;

                        // zone
                        String zone = doc.getString("zoneColour");
                        if (zone != null) switch (zone.toLowerCase()) {
                            case "green": zoneGreen[0]++; break;
                            case "yellow": zoneYellow[0]++; break;
                            case "red": zoneRed[0]++; break;
                        }
                    }

                    createPdfReport(
                            months, childName, childDob, parentEmail,
                            incidents, rescueCounts, cutoff,
                            totalDays, problemDays[0],
                            zoneGreen[0], zoneYellow[0], zoneRed[0]
                    );
                });
    }

    private static class PageHolder {
        PdfDocument.Page page;
        Canvas canvas;
        int pageNumber = 1;
        int y;
    }

    private void newPage(PdfDocument pdf, PageHolder ph) {
        PdfDocument.PageInfo info =
                new PdfDocument.PageInfo.Builder(595, 842, ph.pageNumber).create();

        ph.page = pdf.startPage(info);
        ph.canvas = ph.page.getCanvas();
        ph.y = 40;
    }

    private void ensureSpace(PdfDocument pdf, PageHolder ph, int needed) {
        int margin = 40;
        int height = 842;

        if (ph.y + needed < height - margin) return;

        pdf.finishPage(ph.page);
        ph.pageNumber++;
        newPage(pdf, ph);
    }

    private int drawWrappedLine(Canvas c, String text, int x, int y, Paint p, int maxWidth) {String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            if (p.measureText(line + w) > maxWidth) {
                c.drawText(line.toString(), x, y, p);
                y += p.getTextSize() + 6;
                line = new StringBuilder(w + " ");
            } else {
                line.append(w).append(" ");
            }
        }

        c.drawText(line.toString(), x, y, p);
        return y + (int)(p.getTextSize() + 6);
    }

    private void drawRescueMiniChart(Canvas canvas, int[] values, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        int max = 1;
        for (int v : values) if (v > max) max = v;

        float barWidth = width / (float) values.length;

        Paint p = new Paint();
        p.setColor(Color.parseColor("#3F51B5"));

        for (int i = 0; i < values.length; i++) {
            float x1 = left + i * barWidth + barWidth * 0.2f;
            float x2 = left + i * barWidth + barWidth * 0.8f;

            float barHeight = (values[i] / (float) max) * (height - 20);

            canvas.drawRect(x1, bottom - barHeight, x2, bottom, p);
        }
    }

    private void drawZoneBars(Canvas canvas, int g, int y, int r, int left, int top, int right, int bottom) {
        int[] vals = {g, y, r};
        String[] colors = {"#31ad36", "#ffcc12", "#b50000"};

        int width = right - left;
        int height = bottom - top;

        int max = Math.max(vals[0], Math.max(vals[1], vals[2]));
        if (max == 0) max = 1;

        float barWidth = width / 3f;

        Paint p = new Paint();

        for (int i = 0; i < 3; i++) {
            p.setColor(Color.parseColor(colors[i]));

            float x1 = left + i * barWidth + barWidth * 0.2f;
            float x2 = left + i * barWidth + barWidth * 0.8f;

            float barHeight = (vals[i] / (float) max) * (height - 40);

            canvas.drawRect(x1, bottom - barHeight, x2, bottom, p);
        }
    }

    private void createPdfReport(int months, String childName, String childDob, String parentEmail, List<TriageIncident> incidents, int[] rescueCounts, LocalDate cutoff, int totalDays, int problemDays, int zoneGreen, int zoneYellow, int zoneRed) {

        PdfDocument pdf = new PdfDocument();
        PageHolder ph = new PageHolder();
        newPage(pdf, ph);

        Paint titlePaint = new Paint();
        Paint textPaint = new Paint();
        Paint boxPaint = new Paint();

        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);

        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(14);

        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(2);

        int left = 40;
        int right = 555;

        titlePaint.setTextSize(26);
        ph.canvas.drawText("Provider Report", 595 / 2f, ph.y, titlePaint);
        ph.y += 40;

        titlePaint.setTextSize(18);
        ph.canvas.drawText("Last " + months + " Months", 595 / 2f, ph.y, titlePaint);
        ph.y += 40;

        ensureSpace(pdf, ph, 150);

        int boxTop = ph.y;
        ph.y += 20;

        ph.y = drawWrappedLine(ph.canvas, "Child Name: " + childName, left + 20, ph.y, textPaint, right - left - 40);
        ph.y = drawWrappedLine(ph.canvas, "Date of Birth: " + childDob, left + 20, ph.y, textPaint, right - left - 40);
        ph.y = drawWrappedLine(ph.canvas, "Parent Email: " + parentEmail, left + 20, ph.y, textPaint, right - left - 40);

        int boxBottom = ph.y + 20;
        ph.canvas.drawRect(left, boxTop, right, boxBottom, boxPaint);
        ph.y = boxBottom + 30;

        ensureSpace(pdf, ph, 100);

        int sbTop = ph.y;
        ph.y += 20;

        ph.y = drawWrappedLine(ph.canvas,
                "Symptom Burden (Problem Days): " + problemDays,
                left + 20, ph.y, textPaint, right - left - 40);

        int sbBottom = ph.y + 20;
        ph.canvas.drawRect(left, sbTop, right, sbBottom, boxPaint);
        ph.y = sbBottom + 30;

        ensureSpace(pdf, ph, 80);
        textPaint.setTextSize(16);
        ph.canvas.drawText("Notable Triage Incidents", left + 10, ph.y, textPaint);
        ph.y += 30;

        textPaint.setTextSize(14);

        for (TriageIncident incident : incidents) {

            ensureSpace(pdf, ph, 160);

            int rowTop = ph.y;
            ph.y += 20;

            ph.y = drawWrappedLine(ph.canvas,
                    "• " + incident.date,
                    left + 20, ph.y, textPaint, right - left - 40);

            ph.y = drawWrappedLine(ph.canvas,
                    "Symptoms: " + incident.symptoms,
                    left + 40, ph.y, textPaint, right - left - 60);

            int rowBottom = ph.y + 20;

            ph.canvas.drawRect(left, rowTop, right, rowBottom, boxPaint);
            ph.y = rowBottom + 20;
        }

        ensureSpace(pdf, ph, 220);

        textPaint.setTextSize(16);
        ph.canvas.drawText("Rescue Attempts Over Time", left + 10, ph.y, textPaint);
        ph.y += 20;

        int chartTop = ph.y;
        int chartBottom = chartTop + 150;

        ph.canvas.drawRect(left, chartTop, right, chartBottom, boxPaint);

        drawRescueMiniChart(
                ph.canvas, rescueCounts,
                left + 10, chartTop + 10,
                right - 10, chartBottom - 10
        );

        ph.y = chartBottom + 30;

        ensureSpace(pdf, ph, 220);

        ph.canvas.drawText("Zone Distribution", left + 10, ph.y, textPaint);
        ph.y += 20;

        int zoneTop = ph.y;
        int zoneBottom = zoneTop + 150;

        ph.canvas.drawRect(left, zoneTop, right, zoneBottom, boxPaint);

        drawZoneBars(ph.canvas, zoneGreen, zoneYellow, zoneRed, left + 10, zoneTop + 10, right - 10, zoneBottom - 10);

        ph.y = zoneBottom + 40;

        ensureSpace(pdf, ph, 80);

        textPaint.setTextSize(15);

        ph.y = drawWrappedLine(ph.canvas, "Rescue frequency and controller adherence: 50% of planned days completed.", left, ph.y, textPaint, right - left);

        pdf.finishPage(ph.page);

        File path =
                new File(requireContext().getExternalFilesDir(null),
                        "provider_report.pdf");

        try (FileOutputStream fos = new FileOutputStream(path)) {
            pdf.writeTo(fos);
        } catch (Exception e) {
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
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
