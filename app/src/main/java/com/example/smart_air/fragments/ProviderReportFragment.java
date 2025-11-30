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

    public ProviderReportFragment(){
        // empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // link xml to fragment
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                options
        );
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

    // where main info is generated
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

                    String childDob = dobTs != null
                            ? new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
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

                                fetchTriageSessionsForReport(months, cutoff, childName, childDob, parentEmail);
                            });
                });
    }

    private void fetchTriageSessionsForReport(
            int months,
            LocalDate cutoff,
            String childName,
            String childDob,
            String parentEmail
    ) {
        LocalDate now = LocalDate.now();

        int totalDays = (int) ChronoUnit.DAYS.between(cutoff, now) + 1;
        if (totalDays <= 0) totalDays = 1;

        final int[] rescueCounts = new int[totalDays];
        final List<TriageIncident> incidents = new ArrayList<>();

        final int[] problemDays = {0};
        final int[] zoneGreen = {0};
        final int[] zoneYellow = {0};
        final int[] zoneRed = {0};

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
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();

                        if (entryDate.isBefore(cutoff) || entryDate.isAfter(now)) continue;

                        String formattedDate =
                                new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        .format(ts.toDate());

                        Object rawFlags = doc.get("flagList");
                        String symptomsText = "-";

                        boolean hasSymptoms = false;

                        if (rawFlags instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> list = (List<String>) rawFlags;
                            if (list != null && !list.isEmpty()) {
                                symptomsText = TextUtils.join(", ", list);
                                hasSymptoms = true;
                            }
                        } else if (rawFlags instanceof String) {
                            String s = ((String) rawFlags).trim();
                            if (!s.isEmpty()) {
                                symptomsText = s;
                                hasSymptoms = true;
                            }
                        }

                        incidents.add(new TriageIncident(formattedDate, symptomsText));

                        if (hasSymptoms) {
                            problemDays[0]++;
                        }

                        String zone = doc.getString("zoneColour");
                        if (zone != null) {
                            switch (zone.toLowerCase()) {
                                case "green": zoneGreen[0]++; break;
                                case "yellow": zoneYellow[0]++; break;
                                case "red": zoneRed[0]++; break;
                            }
                        }

                        int rescue = 0;
                        Object rawRescue = doc.get("rescueAttempts");
                        if (rawRescue instanceof Number) {
                            rescue = ((Number) rawRescue).intValue();
                        } else if (rawRescue instanceof String) {
                            try {
                                rescue = Integer.parseInt((String) rawRescue);
                            } catch (Exception ignored) {}
                        }

                        if (rescue > 0) {
                            int idx = (int) ChronoUnit.DAYS.between(cutoff, entryDate);
                            if (idx >= 0 && idx < finalTotalDays) {
                                rescueCounts[idx] += rescue;
                            }
                        }
                    }

                    createPdfReport(
                            months,
                            childName,
                            childDob,
                            parentEmail,
                            cutoff,
                            finalTotalDays,
                            incidents,
                            rescueCounts,
                            problemDays[0],
                            zoneGreen[0],
                            zoneYellow[0],
                            zoneRed[0]
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

    /**
     * Ensures there is at least 'needed' vertical space remaining on the current page; if not,
     * finishes the page and starts a new one.
     */
    private void ensureSpace(PdfDocument pdf, PageHolder ph, int needed) {
        int margin = 40;
        int pageHeight = 842;

        if (ph.y + needed < pageHeight - margin) return;

        pdf.finishPage(ph.page);
        ph.pageNumber++;
        newPage(pdf, ph);
    }

    /**
     * Draws a wrapped text block within maxWidth; returns updated y.
     */
    private int drawWrappedLine(Canvas c, String text, int x, int y, Paint p, int maxWidth) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String candidate = line + w + " ";
            if (p.measureText(candidate) > maxWidth) {
                c.drawText(line.toString(), x, y, p);
                y += p.getTextSize() + 6;
                line = new StringBuilder(w + " ");
            } else {
                line.append(w).append(" ");
            }
        }

        c.drawText(line.toString(), x, y, p);
        return y + (int) (p.getTextSize() + 6);
    }

    private void drawRescueMiniChart(Canvas canvas,
                                     int[] values,
                                     LocalDate startDate,
                                     int left, int top, int right, int bottom) {

        int width = right - left;
        int height = bottom - top;

        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(2f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(10f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        int axisLeft = left + 30;
        int axisBottom = bottom - 20;
        int axisTop = top + 10;
        int axisRight = right - 10;

        canvas.drawLine(axisLeft, axisTop, axisLeft, axisBottom, axisPaint);
        canvas.drawLine(axisLeft, axisBottom, axisRight, axisBottom, axisPaint);

        int max = 1;
        for (int v : values) if (v > max) max = v;

        int tickCount = 4;
        for (int i = 0; i <= tickCount; i++) {
            float frac = i / (float) tickCount;
            float y = axisBottom - frac * (axisBottom - axisTop);
            int labelVal = Math.round(frac * max);

            canvas.drawLine(axisLeft - 5, y, axisLeft, y, axisPaint);
            canvas.drawText(String.valueOf(labelVal), axisLeft - 25, y + 4, textPaint);
        }

        int barAreaWidth = axisRight - axisLeft;
        int barCount = values.length;
        if (barCount == 0) return;

        float barWidth = barAreaWidth / (float) barCount;

        Paint barPaint = new Paint();
        barPaint.setColor(Color.parseColor("#3F51B5"));
        barPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < barCount; i++) {
            float xCenter = axisLeft + (i + 0.5f) * barWidth;
            float barHeight = (values[i] / (float) max) * (axisBottom - axisTop);
            float x1 = xCenter - barWidth * 0.3f;
            float x2 = xCenter + barWidth * 0.3f;
            float y1 = axisBottom - barHeight;

            canvas.drawRect(x1, y1, x2, axisBottom, barPaint);
        }

        LocalDate endDate = startDate.plusDays(barCount - 1);
        LocalDate cursor = startDate.withDayOfMonth(1);
        if (cursor.isBefore(startDate)) {
            cursor = cursor.plusMonths(1);
        }

        while (!cursor.isAfter(endDate)) {
            long daysFromStart = ChronoUnit.DAYS.between(startDate, cursor);
            int index = (int) Math.min(Math.max(daysFromStart, 0), barCount - 1);

            float xCenter = axisLeft + (index + 0.5f) * barWidth;
            String label = cursor.getMonth().toString().substring(0, 3);
            label = label.charAt(0) + label.substring(1).toLowerCase();

            canvas.drawText(label, xCenter - textPaint.measureText(label) / 2, axisBottom + 15, textPaint);

            cursor = cursor.plusMonths(1);
        }
    }

    private void drawZoneBars(Canvas canvas,
                              int green, int yellow, int red,
                              int left, int top, int right, int bottom) {

        int width = right - left;
        int height = bottom - top;

        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(2f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(11f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        int axisLeft = left + 30;
        int axisBottom = bottom - 25;
        int axisTop = top + 10;
        int axisRight = right - 10;

        canvas.drawLine(axisLeft, axisTop, axisLeft, axisBottom, axisPaint);
        canvas.drawLine(axisLeft, axisBottom, axisRight, axisBottom, axisPaint);

        int[] vals = {green, yellow, red};
        String[] labels = {"Green", "Yellow", "Red"};
        String[] colors = {"#31ad36", "#ffcc12", "#b50000"};

        int max = 0;
        for (int v : vals) if (v > max) max = v;
        if (max == 0) max = 1;

        int tickCount = 4;
        for (int i = 0; i <= tickCount; i++) {
            float frac = i / (float) tickCount;
            float y = axisBottom - frac * (axisBottom - axisTop);
            int labelVal = Math.round(frac * max);
            canvas.drawLine(axisLeft - 5, y, axisLeft, y, axisPaint);
            canvas.drawText(String.valueOf(labelVal), axisLeft - 25, y + 4, textPaint);
        }

        int barAreaWidth = axisRight - axisLeft;
        float barWidth = barAreaWidth / 3f;

        Paint barPaint = new Paint();
        barPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < 3; i++) {
            barPaint.setColor(Color.parseColor(colors[i]));

            float xCenter = axisLeft + (i + 0.5f) * barWidth;
            float barHeight = (vals[i] / (float) max) * (axisBottom - axisTop);

            float x1 = xCenter - barWidth * 0.3f;
            float x2 = xCenter + barWidth * 0.3f;
            float y1 = axisBottom - barHeight;

            canvas.drawRect(x1, y1, x2, axisBottom, barPaint);

            float labelWidth = textPaint.measureText(labels[i]);
            canvas.drawText(labels[i], xCenter - labelWidth / 2, axisBottom + 15, textPaint);
        }
    }

    private void createPdfReport(
            int months,
            String childName,
            String childDob,
            String parentEmail,
            LocalDate cutoff,
            int totalDays,
            List<TriageIncident> incidents,
            int[] rescueCounts,
            int problemDays,
            int zoneGreen,
            int zoneYellow,
            int zoneRed
    ) {

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
        textPaint.setTextSize(14f);

        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(2f);

        int left = 40;
        int right = 555;

        titlePaint.setTextSize(26f);
        ph.canvas.drawText("Provider Report", 595 / 2f, ph.y, titlePaint);
        ph.y += 40;

        titlePaint.setTextSize(18f);
        ph.canvas.drawText("Last " + months + " Months", 595 / 2f, ph.y, titlePaint);
        ph.y += 40;

        // child info box
        ensureSpace(pdf, ph, 150);
        int boxTop = ph.y;
        ph.y += 20;

        ph.y = drawWrappedLine(ph.canvas,
                "Child Name: " + (childName == null ? "-" : childName),
                left + 20, ph.y, textPaint, right - left - 40);

        ph.y = drawWrappedLine(ph.canvas,
                "Date of Birth: " + childDob,
                left + 20, ph.y, textPaint, right - left - 40);

        ph.y = drawWrappedLine(ph.canvas,
                "Parent Email: " + parentEmail,
                left + 20, ph.y, textPaint, right - left - 40);

        int boxBottom = ph.y + 20;
        ph.canvas.drawRect(left, boxTop, right, boxBottom, boxPaint);
        ph.y = boxBottom + 30;

        // symptom burden
        ensureSpace(pdf, ph, 100);
        int sbTop = ph.y;
        ph.y += 20;

        ph.y = drawWrappedLine(
                ph.canvas,
                "Symptom Burden (Problem Days): " + problemDays,
                left + 20, ph.y, textPaint, right - left - 40
        );

        int sbBottom = ph.y + 20;
        ph.canvas.drawRect(left, sbTop, right, sbBottom, boxPaint);
        ph.y = sbBottom + 30;

        // triage box
        ensureSpace(pdf, ph, 80);

        textPaint.setTextSize(16f);
        ph.canvas.drawText("Notable Triage Incidents", left + 10, ph.y, textPaint);
        ph.y += 30;
        textPaint.setTextSize(14f);

        for (TriageIncident incident : incidents) {
            ensureSpace(pdf, ph, 160);

            int rowTop = ph.y;
            ph.y += 20;

            ph.y = drawWrappedLine(
                    ph.canvas,
                    "• " + incident.date,
                    left + 20, ph.y, textPaint, right - left - 40
            );

            ph.y = drawWrappedLine(
                    ph.canvas,
                    "Symptoms: " + incident.symptoms,
                    left + 40, ph.y, textPaint, right - left - 60
            );

            int rowBottom = ph.y + 20;
            ph.canvas.drawRect(left, rowTop, right, rowBottom, boxPaint);
            ph.y = rowBottom + 20;
        }

        // rescue chart
        ensureSpace(pdf, ph, 220);
        textPaint.setTextSize(16f);
        ph.canvas.drawText("Rescue Attempts Over Time", left + 10, ph.y, textPaint);
        ph.y += 20;
        textPaint.setTextSize(12f);

        int chartTop = ph.y;
        int chartBottom = chartTop + 180;

        ph.canvas.drawRect(left, chartTop, right, chartBottom, boxPaint);

        drawRescueMiniChart(
                ph.canvas,
                rescueCounts,
                cutoff,
                left + 10, chartTop + 10,
                right - 10, chartBottom - 10
        );

        ph.y = chartBottom + 30;

        // zone distribution chart
        ensureSpace(pdf, ph, 220);
        textPaint.setTextSize(16f);
        ph.canvas.drawText("Zone Distribution", left + 10, ph.y, textPaint);
        ph.y += 20;
        textPaint.setTextSize(12f);

        int zoneTop = ph.y;
        int zoneBottom = zoneTop + 180;

        ph.canvas.drawRect(left, zoneTop, right, zoneBottom, boxPaint);

        drawZoneBars(
                ph.canvas,
                zoneGreen, zoneYellow, zoneRed,
                left + 10, zoneTop + 10,
                right - 10, zoneBottom - 10
        );

        ph.y = zoneBottom + 40;

        ensureSpace(pdf, ph, 80);
        textPaint.setTextSize(14f);

        pdf.finishPage(ph.page);

        // save file, and generate a new one each time
        String fileName = "provider_report_" + System.currentTimeMillis() + ".pdf";
        File path = new File(requireContext().getExternalFilesDir(null), fileName);

        try (FileOutputStream fos = new FileOutputStream(path)) {
            pdf.writeTo(fos);
            Toast.makeText(requireContext(), "PDF saved: " + path.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
