package com.example.smart_air.fragments;

import static com.example.smart_air.modelClasses.formatters.StringFormatters.extractScheduledDays;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.R;
import com.example.smart_air.modelClasses.formatters.StringFormatters;
import com.example.smart_air.viewmodel.DashboardViewModel;
import com.google.firebase.Timestamp;
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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ProviderReportFragment extends Fragment {

    private Spinner spinnerMonths;
    private String childId;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private boolean allowRescue, allowController, allowPEF, allowSymptoms, allowTriage, allowCharts;
    //Cache VM for data from dashboard onto here
    DashboardViewModel cacheVM;
    // list of rescue log documents
    List<DocumentSnapshot> rescueLogs;
    // list of PEF (triage) documents
    List<DocumentSnapshot> pefLogs;
    // sharing toggle map
    Map<String, Boolean> childSharing;
    //parent information such as name, etc
    private String parentName, childName, childDob, monthString;
    //Provider report data
    int[] rescueCounts;
    double rescuePercentage; // percentage of days with rescue
    List<TriageLog> incidents = new ArrayList<>();
    int[] problemDays = {0};
    int[] zoneGreen = {0};
    int[] zoneYellow = {0};
    int[] zoneRed = {0};
    private double controllerAdherence = 0.0;

    private Map<String, Boolean> weeklySchedule;


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
        //get the cache to re-use data
        cacheVM = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        if (getArguments() != null) { childId = getArguments().getString("childId"); }

        if (childId == null) {
            Toast.makeText(requireContext(), "Missing child ID", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        //get all caches

        // uid - list of rescue log documents
        Map<String, List<DocumentSnapshot>> weeklyRescueCache = cacheVM.getWeeklyRescueCache();
        // uid - list of PEF documents
        Map<String, List<DocumentSnapshot>> pefCache = cacheVM.getPefCache();
        // child uid - sharing toggle map
        Map<String, Map<String, Boolean>> childSharingCache = cacheVM.getChildSharingCache();
        if(!weeklyRescueCache.containsKey(childId) || !pefCache.containsKey(childId) || !childSharingCache.containsKey(childId)) {
            Toast.makeText(requireContext(), "Missing child data to generate PDF, returning back to home!", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }
        //grab all correct items from cache
        childSharing = childSharingCache.get(childId);
        pefLogs = pefCache.get(childId);
        rescueLogs = weeklyRescueCache.get(childId);

        //grab booleans from cache
        allowRescue = childSharing.getOrDefault("rescue", false);
        allowController = childSharing.getOrDefault("controller", false);
        allowPEF = childSharing.getOrDefault("pef", false);
        allowSymptoms = childSharing.getOrDefault("symptoms", false);
        allowTriage = childSharing.getOrDefault("triage", false);
        allowCharts = childSharing.getOrDefault("charts", false);
        //prevent pdf generation if no permissions
        if(!allowCharts && !allowRescue && !allowPEF && !allowSymptoms && !allowTriage && !allowController) {
            Toast.makeText(requireContext(), "No permissions to make a valid PDF", Toast.LENGTH_SHORT);
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
                R.layout.force_black_text,
                options
        );
        adapter.setDropDownViewResource(R.layout.force_black_text);
        spinnerMonths.setAdapter(adapter);

        btnCancel.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        btnGenerate.setOnClickListener(v -> {
            String selected = spinnerMonths.getSelectedItem().toString();
            int months = Integer.parseInt(selected.replaceAll("\\D+", ""));
            monthString = Integer.toString(months);
            generateProviderReport(months);
        });
    }

    // where main info is generated
    private void generateProviderReport(int months) {
        incidents = new ArrayList<>(); //make empty
        LocalDate now = LocalDate.now();
        LocalDate cutoff = now.minusMonths(months);

        db.collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(childDoc -> {

                    weeklySchedule = (Map<String, Boolean>) childDoc.get("weeklySchedule");
                    if (weeklySchedule != null) {
                        Log.d("WeeklySchedule", "Weekly schedule: " + weeklySchedule);
                    } else {
                        Log.d("WeeklySchedule", "No weekly schedule found");
                    }

                    if (!childDoc.exists()) {
                        Toast.makeText(requireContext(), "Child data not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    childName = childDoc.getString("name");
                    Timestamp dobTs = childDoc.getTimestamp("dob");

                    childDob = dobTs != null
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
                                parentName = parentEmail;
                                fetchData(cutoff);
                            });
                });
    }

    //based on toggles fetch data async
    private void fetchData(LocalDate cutoff) {
        LocalDate now = LocalDate.now();

        //total days within range
        int totalDays = (int) ChronoUnit.DAYS.between(cutoff, now) + 1;
        if (totalDays <= 0) totalDays = 1;

        // scheduled days within range
        int totalScheduledDays = countScheduledDays(cutoff, now);

        // rescue
        if (allowRescue && rescueLogs != null) {
            processRescueLogsFromCache(cutoff, now, totalDays); //rescue freq is based on total #days
        }

        // symptoms
        fetchSymptomBurdenZone(cutoff, now);

        int finalTotalDaysForCharts = totalDays;

        // controller
        fetchControllerAdherence(cutoff, now, totalScheduledDays, () -> {
            if (allowTriage) {
                fetchTriageIncidents(cutoff, now, () -> {
                    Toast.makeText(requireContext(), "Generating PDF...", Toast.LENGTH_SHORT).show();
                    generatePDF(now, cutoff, finalTotalDaysForCharts);
                });
            } else {
                generatePDF(now, cutoff, finalTotalDaysForCharts);
            }
        });
    }


    private void fetchControllerAdherence(LocalDate cutoff, LocalDate now,
                                          int totalScheduledDays,
                                          Runnable onComplete) {

        if (!allowController) {
            controllerAdherence = 0.0;
            onComplete.run();
            return;
        }

        long startEpoch = cutoff.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long endEpoch = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

        Timestamp startTs = new Timestamp(startEpoch, 0);
        Timestamp endTs = new Timestamp(endEpoch, 0);

        db.collection("children")
                .document(childId)
                .collection("controllerLog")
                .whereGreaterThanOrEqualTo("timeTaken", startTs)
                .whereLessThan("timeTaken", endTs)
                .get()
                .addOnSuccessListener(snap -> {
                    Set<DayOfWeek> scheduledDays = StringFormatters.extractScheduledDays(weeklySchedule);
                    Set<LocalDate> countedDates = new HashSet<>();
                    int completedChecks = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("timeTaken");
                        if (ts == null) continue;
                        LocalDate date = ts.toDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        if (!scheduledDays.contains(date.getDayOfWeek())) { continue; }
                        if (countedDates.add(date)) { completedChecks++; }
                    }

                    //need to check for each snapshot whether the day is an adhered day

                    if (totalScheduledDays <= 0) {
                        controllerAdherence = 0.0;
                        Log.d("ControllerAdherence",
                                "No scheduled days → adherence = 0 (completed = " + completedChecks + ")");
                        onComplete.run();
                        return;
                    }

                    double raw = (completedChecks * 100.0) / totalScheduledDays;

                    raw = Math.min(raw, 100.0);

                    controllerAdherence = Math.round(raw * 100.0) / 100.0;

                    Log.d("ControllerAdherence",
                            "Completed=" + completedChecks +
                                    ", ScheduledDays=" + totalScheduledDays +
                                    ", %=" + controllerAdherence);

                    onComplete.run();

                })
                .addOnFailureListener(e -> {
                    controllerAdherence = 0.0;
                    onComplete.run();
                });
    }


    // takes the weekly schedule days and sums the number of accepted days in the month range returns the number of scheduled days
    private int countScheduledDays(LocalDate start, LocalDate end) {
        if (weeklySchedule == null || weeklySchedule.isEmpty()) return 0;

        Map<String, Boolean> normalized = new HashMap<>();
        for (String key : weeklySchedule.keySet()) {
            normalized.put(key.toLowerCase(), weeklySchedule.get(key));
        }

        int count = 0;
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String dayKey = cursor.getDayOfWeek().toString().toLowerCase();
            Boolean scheduled = normalized.get(dayKey);
            if (scheduled != null && scheduled) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }

    private void fetchTriageIncidents(LocalDate cutoff, LocalDate now, Runnable onComplete ) {
        //Fetch the incident docs
        db.collection("incidentLog")
                .document(childId)
                .collection("triageSessions")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap) {
                        try {
                            // get data from document
                            com.google.firebase.Timestamp timestamp = doc.getTimestamp("date");
                            Long pefLong = doc.getLong("PEF");
                            ArrayList<String> flagList = (ArrayList<String>) doc.get("flagList");
                            ArrayList<String> guidance = (ArrayList<String>) doc.get("guidance");
                            Long rescueAttemptsLong = doc.getLong("rescueAttempts");
                            ArrayList<String> userRes = (ArrayList<String>) doc.get("userRes");
                            // null check and default
                            if (timestamp == null) continue;
                            int pef = (pefLong != null) ? pefLong.intValue() : 0;
                            if (flagList == null) flagList = new ArrayList<>();
                            if (guidance == null) guidance = new ArrayList<>();
                            int rescueAttempts = (rescueAttemptsLong != null) ? rescueAttemptsLong.intValue() : 0;
                            if (userRes == null) userRes = new ArrayList<>();
                            //convert timestamp to date
                            Date date = timestamp.toDate();
                            LocalDate incidentDate = date.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            //check if within range
                            if ((incidentDate.isEqual(cutoff) || incidentDate.isAfter(cutoff)) &&
                                    (incidentDate.isEqual(now) || incidentDate.isBefore(now))) {
                                // made-up notable criteria
                                if (flagList.size() >= 3 && rescueAttempts > 3) {
                                    // Format date as string
                                    String dateString = incidentDate.toString();
                                    TriageLog log = new TriageLog(dateString, pef, flagList, guidance, rescueAttempts, userRes);
                                    incidents.add(log);
                                }
                            }

                            // stop once 10 incidents
                            if (incidents.size() >= 10) { break; }

                        } catch (Exception e) {
                            Log.e("TriageFetch", "Error processing document: " + doc.getId(), e);
                        }
                    }
                    // Notify completion
                    if (onComplete != null) {
                        onComplete.run();
                    }
                } );

    }

    private void fetchSymptomBurdenZone(LocalDate cutoff, LocalDate now) {
        int symptomBurdenCount = 0;
        if(pefLogs == null) {
            problemDays[0] = 0;
            zoneGreen[0] = 0;
            zoneYellow[0] = 0;
            zoneRed[0] = 0;
            return;
        }
        for (DocumentSnapshot doc : pefLogs) {
            try {
                String docId = doc.getId(); //YYYY-MM-DD
                LocalDate entryDate;
                try { entryDate = LocalDate.parse(docId);
                } catch (Exception e) {
                    Log.e("SymptomFetch", "Invalid date format in doc ID: " + docId, e);
                    continue;
                }
                // if date is within range
                if (entryDate.isBefore(cutoff) || entryDate.isAfter(now)) { continue; }
                // get the symptom values
                Long activityLimitsLongP = doc.getLong("activityLimitsparent");
                Long coughingWheezingLongP = doc.getLong("coughingWheezingparent");
                Long activityLimitsLongC = doc.getLong("activityLimitschild");
                Long coughingWheezingLongC = doc.getLong("coughingWheezingchild");
                // check if either field exists and is >= 4
                boolean isSymptomBurden = false;
                if (activityLimitsLongP != null && activityLimitsLongP >= 4) { isSymptomBurden = true; }
                if (coughingWheezingLongP != null && coughingWheezingLongP >= 4) { isSymptomBurden = true; }
                if (activityLimitsLongC != null && activityLimitsLongC >= 4) { isSymptomBurden = true; }
                if (coughingWheezingLongC != null && coughingWheezingLongC >= 4) { isSymptomBurden = true; }
                // count this day if it meets criteria
                if (isSymptomBurden) { symptomBurdenCount++; }
                // get and count zone color
                String zoneColour = doc.getString("zoneColour");
                if (zoneColour != null) {
                    switch (zoneColour.toLowerCase()) {
                        case "green":
                            zoneGreen[0]++;
                            break;
                        case "yellow":
                            zoneYellow[0]++;
                            break;
                        case "red":
                            zoneRed[0]++;
                            break;
                    }
                }

            } catch (Exception e) {
                Log.e("SymptomFetch", "Error processing document: " + doc.getId(), e);
            }
        }
        // store the result
        problemDays[0] = symptomBurdenCount;
        Log.d("SymptomFetch", "Symptom burden days: " + symptomBurdenCount);
    }

    private void processRescueLogsFromCache(LocalDate cutoff, LocalDate now,
                                            int totalDays) {

        rescueCounts = new int[totalDays];
        if (rescueLogs == null || rescueLogs.isEmpty()) {
            rescuePercentage = 0.0;
            return;
        }

        boolean[] hasRescueOnDay = new boolean[totalDays];
        int daysWithRescue = 0;

        for (DocumentSnapshot doc : rescueLogs) {
            try {
                Timestamp timestamp = doc.getTimestamp("timeTaken");
                if (timestamp == null) continue;

                LocalDate rescueDate = timestamp.toDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                if (rescueDate.isBefore(cutoff) || rescueDate.isAfter(now)) continue;

                int dayIndex = (int) ChronoUnit.DAYS.between(cutoff, rescueDate);
                if (dayIndex < 0 || dayIndex >= totalDays) continue;

                // For chart only
                rescueCounts[dayIndex]++;

                if (!hasRescueOnDay[dayIndex]) {
                    hasRescueOnDay[dayIndex] = true;
                    daysWithRescue++;
                }

            } catch (Exception e) {
                Log.e("RescueFetch", "Error processing rescue log: " + doc.getId(), e);
            }
        }

        if (totalDays <= 0) {
            rescuePercentage = 0.0;
        } else {
            rescuePercentage = (daysWithRescue * 100.0) / totalDays;
            rescuePercentage = Math.round(rescuePercentage * 100.0) / 100.0;
        }
    }



    /* HELPER FUNCTIONS FOR PDF GENERATION */

    private void addTriageCardToPdf(PdfDocument pdf, PageHolder ph, int x, int maxWidth,
                                    TriageLog log) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View triageView = inflater.inflate(R.layout.pdf_card_triage, null, false);

        //text view
        ((TextView) triageView.findViewById(R.id.triageTitle)).setText("Incident Log - " + log.date);
        TextView presentFlags = triageView.findViewById(R.id.presentFlags);
        presentFlags.setText((log.flagList != null && !log.flagList.isEmpty()) ?
                TextUtils.join(", ", log.flagList) : "None");
        if(log.PEF == -1) {
            ((TextView) triageView.findViewById(R.id.pefValue)).setText(String.valueOf("N/A"));
        }
        else ((TextView) triageView.findViewById(R.id.pefValue)).setText(String.valueOf(log.PEF));

        ((TextView) triageView.findViewById(R.id.rescueAttempts)).setText(String.valueOf(log.rescueAttempts));

        TextView emergencyCall = triageView.findViewById(R.id.emergencyCall);
        boolean hasEmergencyCall = log.guidance != null && log.guidance.stream()
                .anyMatch(g -> g != null && g.toLowerCase().contains("emergency"));
        emergencyCall.setText(hasEmergencyCall ? "YES" : "NO");
        emergencyCall.setTextColor(Color.parseColor(hasEmergencyCall ? "#D32F2F" : "#388E3C"));

        TextView userResponseList = triageView.findViewById(R.id.userResponseList);
        if (log.userRes != null && !log.userRes.isEmpty()) {
            StringBuilder responseBuilder = new StringBuilder();
            for (String response : log.userRes) {
                if (response != null && !response.trim().isEmpty()) {
                    responseBuilder.append("• ").append(response).append("\n");
                }
            }
            userResponseList.setText(responseBuilder.length() > 0 ? responseBuilder.toString().trim() : "• No response recorded");
        } else {
            userResponseList.setText("• No response recorded");
        }

        //exact width
        int widthSpec = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        triageView.measure(widthSpec, heightSpec);
        triageView.layout(0, 0, triageView.getMeasuredWidth(), triageView.getMeasuredHeight());

        int cardHeight = triageView.getMeasuredHeight();
        ensureSpace(pdf, ph, cardHeight + 10);

        // draw without shrinking
        Bitmap bitmap = Bitmap.createBitmap(cardHeight > 0 ? triageView.getMeasuredWidth() : maxWidth,
                cardHeight, Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(bitmap);
        tempCanvas.drawColor(Color.WHITE);
        triageView.draw(tempCanvas);

        ph.canvas.drawBitmap(bitmap, x, ph.y, null);

        ph.y += cardHeight + 10;
        bitmap.recycle();
    }

    /* ADDS THE REPORT HEADER TO THE PDF */
    private int addReportHeaderToPdf(Canvas pdfCanvas, int x, int y, int maxWidth,
                                     String childName, String dateOfBirth,
                                     String parentEmail, String months) {

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View headerView = inflater.inflate(R.layout.pdf_provider_summary_header, null, false);

        // Find and populate views
        TextView reportPeriod = headerView.findViewById(R.id.reportPeriod);
        TextView childNameValue = headerView.findViewById(R.id.childNameValue);
        TextView dateOfBirthValue = headerView.findViewById(R.id.dateOfBirthValue);
        TextView parentEmailValue = headerView.findViewById(R.id.parentEmailValue);

        // set values
        reportPeriod.setText("Last " + months + " Months");
        childNameValue.setText(childName != null ? childName : "-");
        dateOfBirthValue.setText(dateOfBirth != null ? dateOfBirth : "-");
        parentEmailValue.setText(parentEmail != null ? parentEmail : "-");

        // scale for quality
        int scaleFactor = 3;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(maxWidth * scaleFactor, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        headerView.measure(widthSpec, heightSpec);
        headerView.layout(0, 0, headerView.getMeasuredWidth(), headerView.getMeasuredHeight());

        //bitmap
        Bitmap bitmap = Bitmap.createBitmap(
                headerView.getMeasuredWidth(),
                headerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas tempCanvas = new Canvas(bitmap);
        tempCanvas.drawColor(Color.WHITE);
        headerView.draw(tempCanvas);
        //bitmap scale for pdf fitting
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth,
                headerView.getMeasuredHeight() / scaleFactor, true);
        //draw to the pdf
        pdfCanvas.drawBitmap(scaledBitmap, x, y, null);
        int headerHeight = scaledBitmap.getHeight();
        //cleanup
        bitmap.recycle();
        scaledBitmap.recycle();
        return y + headerHeight + 20; //next y pos
    }

    /* ADD THE STATS ROW TO THE PDF */
    private int addStatsRowToPdf(Canvas pdfCanvas, int x, int y, int maxWidth,
                                 int symptomBurden, double rescuePercentage,
                                 double controllerAdherence,
                                 boolean allowSymptoms, boolean allowRescue,
                                 boolean allowController) {

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View statsView = inflater.inflate(R.layout.pdf_provider_summary, null, false);

        TextView symptomValue = statsView.findViewById(R.id.symptomBurdenValue);
        TextView rescueValue = statsView.findViewById(R.id.rescueFrequencyValue);
        TextView controllerValue = statsView.findViewById(R.id.controllerAdherenceValue);

        if (allowSymptoms) { symptomValue.setText(String.valueOf(symptomBurden)); }
        else {
            symptomValue.setText("N/A");
            symptomValue.setTextColor(Color.parseColor("#9E9E9E"));
        }
        if (allowRescue) {
            rescueValue.setText(String.valueOf(rescuePercentage) + "%");
        } else {
            rescueValue.setText("N/A");
            rescueValue.setTextColor(Color.parseColor("#9E9E9E"));
        }
        if (allowController) {
            controllerValue.setText(String.valueOf(controllerAdherence) + "%");
        } else {
            controllerValue.setText("N/A");
            controllerValue.setTextColor(Color.parseColor("#9E9E9E"));
        }

        int scaleFactor = 3;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(maxWidth * scaleFactor, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        statsView.measure(widthSpec, heightSpec);
        statsView.layout(0, 0, statsView.getMeasuredWidth(), statsView.getMeasuredHeight());

        int rowHeight = statsView.getMeasuredHeight() / scaleFactor;

        //bitmap
        Bitmap bitmap = Bitmap.createBitmap(
                statsView.getMeasuredWidth(),
                statsView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas tempCanvas = new Canvas(bitmap);
        tempCanvas.drawColor(Color.WHITE);
        statsView.draw(tempCanvas);
        //scale it down for pdf first
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, rowHeight, true);
        pdfCanvas.drawBitmap(scaledBitmap, x, y, null);
        bitmap.recycle();
        scaledBitmap.recycle();

        return y + rowHeight + 15;
    }

    /* GENERATES THE PDF */
    private void generatePDF(LocalDate now, LocalDate cutoff, int totalDays) {
        PdfDocument pdf = new PdfDocument();
        PageHolder ph = new PageHolder();
        newPage(pdf, ph);

        Paint textPaint = new Paint();
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(14f);

        int left = 40;
        int right = 555;

        // add report header
        ph.y = addReportHeaderToPdf(
                ph.canvas,
                left,
                ph.y,
                right - left,
                childName,
                childDob,
                parentName,
                monthString
        );

        //stats row part
        ensureSpace(pdf, ph, 150);
        ph.y = addStatsRowToPdf(ph.canvas, left, ph.y, right - left,
                problemDays[0], rescuePercentage, controllerAdherence,
                allowSymptoms, allowRescue, allowController); //TODO: change adherence to right value

        ph.y += 20;

        //triage log part
        if (allowTriage) {
            ensureSpace(pdf, ph, 50);

            //title
            Paint titlePaint = new Paint();
            titlePaint.setTextSize(16f);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setColor(Color.BLACK);

            ph.canvas.drawText("Notable Triage Incidents", left + 10, ph.y, titlePaint);
            ph.y += 25;

            if (incidents == null || incidents.isEmpty()) {
                ensureSpace(pdf, ph, 60);
                textPaint.setTextSize(14f);
                ph.y = drawWrappedLine(ph.canvas,
                        "No notable triage incidents recorded in this period.",
                        left + 20, ph.y, textPaint, right - left - 40);
                ph.y += 30;
            } else {
                // Display up to 10 triage cards
                int displayCount = Math.min(incidents.size(), 10);
                for (int i = 0; i < displayCount; i++) {
                    TriageLog log = incidents.get(i);

                    // Pass pdf and ph so the method can call ensureSpace itself
                    addTriageCardToPdf(pdf, ph, left, right - left, log);
                }

                ph.y += 10;
            }
        }

        //rescue chart section
        if (allowRescue && allowCharts) {
            ensureSpace(pdf, ph, 220);
            Paint titlePaint = new Paint();
            titlePaint.setTextSize(16f);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setColor(Color.BLUE);
            ph.canvas.drawText("Rescue Attempts Over Time", left + 10, ph.y, titlePaint);
            ph.y += 20;
            int chartTop = ph.y;
            int chartBottom = chartTop + 180;
            Paint boxPaint = new Paint();
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(2f);
            ph.canvas.drawRect(left, chartTop, right, chartBottom, boxPaint);
            drawRescueMiniChart(ph.canvas, rescueCounts, cutoff,
                    left + 10, chartTop + 10, right - 10, chartBottom - 10);
            ph.y = chartBottom + 30;
        }

        // Zone distribution section
        if (allowCharts) {
            ensureSpace(pdf, ph, 220);
            Paint titlePaint = new Paint();
            titlePaint.setTextSize(16f);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setColor(Color.BLUE);
            ph.canvas.drawText("Zone Distribution", left + 10, ph.y, titlePaint);
            ph.y += 20;
            int zoneTop = ph.y;
            int zoneBottom = zoneTop + 180;
            Paint boxPaint = new Paint();
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(2f);
            ph.canvas.drawRect(left, zoneTop, right, zoneBottom, boxPaint);

            drawZoneBars(ph.canvas, zoneGreen[0], zoneYellow[0], zoneRed[0],
                    Integer.parseInt(monthString), totalDays,
                    left + 10, zoneTop + 10, right - 10, zoneBottom - 10);

            ph.y = zoneBottom + 40;
        }

        pdf.finishPage(ph.page);

        String fileName = "provider_report_" + System.currentTimeMillis() + ".pdf";
        File path = new File(requireContext().getExternalFilesDir(null), fileName);

        try (FileOutputStream fos = new FileOutputStream(path)) {
            pdf.writeTo(fos);
            Toast.makeText(requireContext(), "PDF saved: " + path.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            Log.e("PDFCreation", "Error creating PDF", e);
        }

        pdf.close();
        Toast.makeText(requireContext(), "PDF saved: " + path.getAbsolutePath(),
                Toast.LENGTH_LONG).show();
        openPdf(path);

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
     * Ensures there is at least needed vertical space remaining on the current page. if not,
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
     * Draws a wrapped text block within maxWidth. returns updated y.
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
        axisPaint.setColor(Color.BLUE);
        axisPaint.setStrokeWidth(2f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(10f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        int axisLeft = left + 30;
        int axisBottom = bottom - 20;
        int axisTop = top + 10;
        int axisRight = right - 10;

        // Draw axes
        canvas.drawLine(axisLeft, axisTop, axisLeft, axisBottom, axisPaint);
        canvas.drawLine(axisLeft, axisBottom, axisRight, axisBottom, axisPaint);

        // max val for scaling
        int max = 1;
        for (int v : values) if (v > max) max = v;

        //  tick count based on max value
        int tickCount = Math.min(4, max);
        if (max <= 4) {
            tickCount = max; // Show each integer value
        }

        // y-axis ticks and labels
        for (int i = 0; i <= tickCount; i++) {
            float y = axisBottom - ((float) i / tickCount) * (axisBottom - axisTop);
            int labelVal = (int) Math.round(((float) i / tickCount) * max);

            canvas.drawLine(axisLeft - 5, y, axisLeft, y, axisPaint);
            canvas.drawText(String.valueOf(labelVal), axisLeft - 25, y + 4, textPaint);
        }

        int chartWidth = axisRight - axisLeft;
        int chartHeight = axisBottom - axisTop;
        int dataCount = values.length;
        if (dataCount == 0) return;

        // paint line
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#3F51B5"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);
        linePaint.setAntiAlias(true);

        //  for data points
        Paint pointPaint = new Paint();
        pointPaint.setColor(Color.parseColor("#3F51B5"));
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        //  segments connecting data points
        Path linePath = new Path();

        for (int i = 0; i < dataCount; i++) {
            float x = axisLeft + (i / (float) (dataCount - 1)) * chartWidth;
            float y = axisBottom - (values[i] / (float) max) * chartHeight;
            if (i == 0) { linePath.moveTo(x, y);
            } else { linePath.lineTo(x, y); }
        }
        canvas.drawPath(linePath, linePaint);

        // data points
        for (int i = 0; i < dataCount; i++) {
            float x = axisLeft + (i / (float) (dataCount - 1)) * chartWidth;
            float y = axisBottom - (values[i] / (float) max) * chartHeight;
            canvas.drawCircle(x, y, 4f, pointPaint);
        }

        // month labels on x-axis
        LocalDate endDate = startDate.plusDays(dataCount - 1);
        LocalDate cursor = startDate.withDayOfMonth(1);
        if (cursor.isBefore(startDate)) {
            cursor = cursor.plusMonths(1);
        }

        while (!cursor.isAfter(endDate)) {
            long daysFromStart = ChronoUnit.DAYS.between(startDate, cursor);
            int index = (int) Math.min(Math.max(daysFromStart, 0), dataCount - 1);

            float x = axisLeft + (index / (float) (dataCount - 1)) * chartWidth;
            String label = cursor.getMonth().toString().substring(0, 3);
            label = label.charAt(0) + label.substring(1).toLowerCase();

            canvas.drawText(label, x - textPaint.measureText(label) / 2, axisBottom + 15, textPaint);

            cursor = cursor.plusMonths(1);
        }
    }

    private void drawZoneBars(Canvas canvas,
                              int green, int yellow, int red,
                              int months, int totalDays,
                              int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.BLUE);
        axisPaint.setStrokeWidth(2f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(11f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        int axisLeft = left + 40;
        int axisBottom = bottom - 25;
        int axisTop = top + 10;
        int axisRight = right - 10;

        canvas.drawLine(axisLeft, axisTop, axisLeft, axisBottom, axisPaint);
        canvas.drawLine(axisLeft, axisBottom, axisRight, axisBottom, axisPaint);

        int[] vals = {green, yellow, red};
        String[] labels = {"Green", "Yellow", "Red"};
        String[] colors = {"#31ad36", "#ffcc12", "#b50000"};

        // the actual max value in the data
        int dataMax = Math.max(Math.max(green, yellow), red);

        // a nice rounded max for the y-axis
        int max = calculateNiceMax(dataMax);
        if (max < 1) max = 1;

        // appropriate tick count based on max value
        int tickCount = Math.min(5, max);
        if (max <= 5) {
            tickCount = max;
        }

        // y-axis with better tick distribution
        for (int i = 0; i <= tickCount; i++) {
            float y = axisBottom - ((float) i / tickCount) * (axisBottom - axisTop);
            int labelVal = (int) Math.round(((float) i / tickCount) * max);

            canvas.drawLine(axisLeft - 5, y, axisLeft, y, axisPaint);
            canvas.drawText(String.valueOf(labelVal), axisLeft - 35, y + 4, textPaint);
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

    // rounded maximum
    private int calculateNiceMax(int dataMax) {
        if (dataMax <= 0) return 10;
        int magnitude = (int) Math.pow(10, Math.floor(Math.log10(dataMax)));
        double normalizedMax = dataMax / (double) magnitude;
        int niceMax;
        if (normalizedMax <= 1) {
            niceMax = magnitude;
        } else if (normalizedMax <= 2) {
            niceMax = 2 * magnitude;
        } else if (normalizedMax <= 5) {
            niceMax = 5 * magnitude;
        } else {
            niceMax = 10 * magnitude;
        }
        return niceMax;
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

    public static class TriageLog {
        String date;
        int PEF;
        ArrayList<String> flagList;
        ArrayList<String> guidance;
        int rescueAttempts;
        ArrayList<String> userRes;

        public TriageLog(String date, int PEF, ArrayList<String> flagList, ArrayList<String> guidance, int rescueAttempts, ArrayList<String> userRes) {
            this.date = date;
            this.PEF = PEF;
            this.flagList = flagList;
            this.guidance = guidance;
            this.rescueAttempts = rescueAttempts;
            this.userRes = userRes;
        }
    }
}
