package com.example.smart_air.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.R;
import com.example.smart_air.Repository.HistoryRepository;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.example.smart_air.adapter.HistoryAdapter;
import com.example.smart_air.modelClasses.HistoryItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.io.FileOutputStream;
import java.util.Map;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.annotation.Nullable;

import android.graphics.pdf.PdfDocument;

public class HistoryFragment extends Fragment {

    private View view;
    private HistoryRepository repo;
    private HistoryAdapter adapter;
    public String [] filters = {"","","","","",""};
    public boolean [] options = {true,true,true,true,true}; // {pef, rescue, symptoms, triage, triggers}
    String childUid;
    private SharedChildViewModel sharedModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.history_page, container, false);
    }

    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        repo = new HistoryRepository();
        GridLayout filterContainerInitial = view.findViewById(R.id.filterGrid);
        filterContainerInitial.setVisibility(View.GONE);

        // adapter
        RecyclerView recyclerView = view.findViewById(R.id.historyRecyclerView);
        adapter = new HistoryAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // set past 6 months
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -6);
        Date sixMonthsAgo = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdf.format(sixMonthsAgo);
        filters[4] = formattedDate;

        MaterialButton filter = view.findViewById(R.id.buttonFilters);
        filter.setOnClickListener(v -> {
            GridLayout filterContainer = view.findViewById(R.id.filterGrid);
            if(filterContainer.getVisibility() == View.GONE){
                filterContainer.setVisibility(View.VISIBLE);
                filter.setText("FILTERS ▲");
            }
            else{
                filterContainer.setVisibility(View.GONE);
                filter.setText("FILTERS ▼");
            }
        });

        setUpFilterUI();
        // shared viewmodal
        repo.getChildUid(this); // set up child uid if it is a child
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        sharedModel.getAllChildren().observe(getViewLifecycleOwner(), children -> { // set up intial child
            if (children != null && !children.isEmpty()) {
                int currentIndex = sharedModel.getCurrentChild().getValue() != null
                        ? sharedModel.getCurrentChild().getValue()
                        : 0;

                String currentChildUid = children.get(currentIndex).getChildUid();
                this.childUid = currentChildUid;
                repo.getCards(childUid,this);
                repo.updateToggles(childUid,this);
            }
        });

        sharedModel.getCurrentChild().observe(getViewLifecycleOwner(), currentIndex -> { // update each time child index changed
            List<Child> children = sharedModel.getAllChildren().getValue();
            if (children != null && !children.isEmpty() && currentIndex != null) {
                this.childUid = children.get(currentIndex).getChildUid();
                repo.getCards(childUid,this);
                repo.updateToggles(childUid,this);
            }
        });

        // night filter
        AutoCompleteTextView nightDropdown = view.findViewById(R.id.selectNightWaking);
        nightDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            if(selected.equals("YES")){
                filters[0] = "true";
            }
            else if(selected.equals("NO")){
                filters[0] = "false";
            }
            else{
                filters[0] = selected;
            }
            repo.getCards(childUid,this);
            repo.updateToggles(childUid,this);
        });

        // activity filter
        AutoCompleteTextView activityDropdown = view.findViewById(R.id.selectActivityLimits);
        activityDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            filters[1] = selected;
            repo.getCards(childUid,this);
            repo.updateToggles(childUid,this);
        });

        // coughing filter
        AutoCompleteTextView coughingDropdown = view.findViewById(R.id.selectCoughingLevel);
        coughingDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            filters[2] = selected;
            repo.getCards(childUid,this);
            repo.updateToggles(childUid,this);
        });

        // triggers filter
        AutoCompleteTextView triggerDropdown = view.findViewById(R.id.selectTriggers);
        triggerDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            filters[3] = selected;
            repo.getCards(childUid,this);
            repo.updateToggles(childUid,this);
        });

        // date filter
        AutoCompleteTextView dateDropdown = view.findViewById(R.id.selectDate);
        dateDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();

            // set date to compare too based on it
            Calendar today = Calendar.getInstance();
            switch (selected) {
                case "Past 3 months":
                    today.add(Calendar.MONTH, -3);
                    break;
                case "Past month":
                    today.add(Calendar.MONTH, -1);
                    break;
                case "Past 2 weeks":
                    today.add(Calendar.DAY_OF_YEAR, -14);
                    break;
                case "Past week":
                    today.add(Calendar.DAY_OF_YEAR, -7);
                    break;
                case "Past 2 days":
                    today.add(Calendar.DAY_OF_YEAR, -2);
                    break;
                default:
                    today.add(Calendar.MONTH, -6);
            }
            Date filterDate = today.getTime();
            String formattedFilterDate = sdf.format(filterDate);
            filters[4] = formattedFilterDate;

            repo.getCards(childUid,this);
            repo.updateToggles(childUid,this);
        });

        // triage filter
        AutoCompleteTextView triageDropdown = view.findViewById(R.id.selectTriage);
        triageDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            filters[5] = selected;
            repo.getCards(childUid,this);
            repo.updateToggles(childUid,this);
        });

        // TODO: make this button provider only later
        // export button
        MaterialButton export = view.findViewById(R.id.buttonExport);
        export.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Export")
                    .setItems(new String[]{"Export as PDF", "Export as CSV"}, (dialog, which) -> {
                        if (which == 0) {
                            exportPDF();
                        } else {
                            exportCSV();
                        }
                    })
                    .show();
        });
    }

    private void exportCSV() {
        try {
            List<HistoryItem> listToExport = adapter.getCurrentList();
            String childName = sharedModel.getCurrentChildName();
            String fileName = "historyLog_"+childName+".csv";

            File csvFile = new File(requireContext().getExternalFilesDir(null), fileName);
            try (FileOutputStream fos = new FileOutputStream(csvFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos)) {

                // daily table
                writer.append("DAILY DATA\n");
                if(options[2]){
                    writer.append("Date,Zone,Night Terrors,Activity Limits,Coughing/Wheezing");
                }
                else{
                    writer.append("Date,Zone");
                }
                if(options[4]){
                    writer.append(",Triggers\n");
                }
                else{
                    writer.append("\n");
                }

                for (HistoryItem card : listToExport) {
                    if (!(card.passFilter && card.cardType != HistoryItem.typeOfCard.triage)) continue;

                    if(card.removeSymptoms){
                        writer.append(card.date).append(",");
                        writer.append(card.zone).append(",");
                    }
                    else {
                        writer.append(card.date).append(",");
                        writer.append(card.zone).append(",");
                        writer.append(card.nightStatus).append(",");
                        writer.append(card.activityStatus).append(",");
                        writer.append(card.coughingStatus).append(",");
                    }
                    if(card.removeTrigger){
                        writer.append("\n");
                    }
                    else{
                        writer.append(String.join("|", card.triggers)).append("\n"); // newline at end
                    }
                }

                // triage table
                if(options[3]) { // only runs when triage filter is true
                    writer.append("\nTRIAGE DATA\n");
                    writer.append("Date,Time,Present Flags,PEF,Rescue Attempts,Emergency Call,User Response\n");
                    for (HistoryItem card : listToExport) {
                        if (!(card.passFilter && card.cardType == HistoryItem.typeOfCard.triage))
                            continue;

                        writer.append(card.date).append(",");
                        writer.append(card.time).append(",");
                        writer.append(String.join("|", card.flaglist)).append(",");
                        if (card.pef == -10) {
                            writer.append("not available").append(",");
                        } else if (card.pef == -5) {
                            writer.append("not entered").append(",");
                        } else {
                            writer.append(Integer.toString(card.pef)).append(",");
                        }
                        if (card.rescueAttempts == -10) {
                            writer.append("n/a").append(",");
                        } else if (card.rescueAttempts == -5) {
                            writer.append("0").append(",");
                        } else {
                            writer.append(Integer.toString(card.rescueAttempts)).append(",");
                        }
                        writer.append(card.emergencyCall).append(",");
                        writer.append(String.join("|", card.userRes)).append("\n");
                    }
                }

                writer.flush();
                Toast.makeText(getContext(), "CSV saved to: " + csvFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error exporting CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportPDF() {
        List<HistoryItem> listToExport = adapter.getCurrentList(); // data from adapter

        String childName = sharedModel.getCurrentChildName();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int pageWidth = 612;
        int pageHeight = 792;
        int currentY = 75; // leave space for title

        PdfDocument pdfDocument = new PdfDocument();

        // title style
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#0473AE"));
        titlePaint.setTextSize(24f);
        titlePaint.setTypeface(Typeface.create("dm_sans", Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);

        // child info style
        Paint infoPaint = new Paint();
        infoPaint.setColor(Color.BLACK);
        infoPaint.setTextSize(12f);
        infoPaint.setTypeface(Typeface.create("dm_sans", Typeface.NORMAL));
        infoPaint.setTextAlign(Paint.Align.CENTER);

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // draw title
        canvas.drawText("HISTORY LOG", pageWidth / 2f, 40, titlePaint);
        // draw info
        canvas.drawText("Name: " + childName, pageWidth / 2f, 70, infoPaint);

        for (HistoryItem card : listToExport) {
            if (card.passFilter && card.cardType != HistoryItem.typeOfCard.triage) {

                View cardView = inflater.inflate(R.layout.pdf_card_daily, null, false);

                ((TextView) cardView.findViewById(R.id.dateText))
                        .setText(card.date);
                if(card.removeSymptoms){
                    (cardView.findViewById(R.id.coughStatus))
                            .setVisibility(View.GONE);
                    (cardView.findViewById(R.id.activityStatus))
                            .setVisibility(View.GONE);
                    (cardView.findViewById(R.id.nightStatus))
                            .setVisibility(View.GONE);
                    (cardView.findViewById(R.id.cough)).setVisibility(View.GONE);
                    (cardView.findViewById(R.id.activity)).setVisibility(View.GONE);
                    (cardView.findViewById(R.id.night)).setVisibility(View.GONE);
                }
                else {
                    (cardView.findViewById(R.id.coughStatus))
                            .setVisibility(View.VISIBLE);
                    (cardView.findViewById(R.id.activityStatus))
                            .setVisibility(View.VISIBLE);
                    (cardView.findViewById(R.id.nightStatus))
                            .setVisibility(View.VISIBLE);
                    (cardView.findViewById(R.id.cough)).setVisibility(View.VISIBLE);
                    (cardView.findViewById(R.id.activity)).setVisibility(View.VISIBLE);
                    (cardView.findViewById(R.id.night)).setVisibility(View.VISIBLE);
                    ((TextView) cardView.findViewById(R.id.coughStatus))
                            .setText(card.coughingStatus);
                    ((TextView) cardView.findViewById(R.id.activityStatus))
                            .setText(card.activityStatus);
                    ((TextView) cardView.findViewById(R.id.nightStatus))
                            .setText(card.nightStatus);
                }
                if(card.removeTrigger){
                    (cardView.findViewById(R.id.triggersText))
                            .setVisibility(View.GONE);
                    (cardView.findViewById(R.id.trigger)).setVisibility(View.GONE);
                }
                else{
                    cardView.findViewById(R.id.triggersText)
                            .setVisibility(View.VISIBLE);
                    (cardView.findViewById(R.id.trigger)).setVisibility(View.VISIBLE);
                    ((TextView) cardView.findViewById(R.id.triggersText))
                            .setText(String.join(", ", card.triggers).isEmpty() ? "None" : String.join(", ", card.triggers));
                }

                TextView zoneStatus = cardView.findViewById(R.id.zoneStatus);
                if (card.zone == null || card.zone.isEmpty()) {
                    zoneStatus.setBackgroundColor(Color.parseColor("#F5F6F6"));
                    zoneStatus.setText("No Zone");
                } else {
                    switch (card.zone.toLowerCase()) {
                        case "green":
                            zoneStatus.setBackgroundColor(Color.parseColor("#9FD46A"));
                            break;
                        case "yellow":
                            zoneStatus.setBackgroundColor(Color.parseColor("#FABF24"));
                            break;
                        case "red":
                            zoneStatus.setBackgroundColor(Color.parseColor("#FB633D"));
                            break;
                        default:
                            zoneStatus.setBackgroundColor(Color.parseColor("#000000"));
                            break;
                    }
                    zoneStatus.setText(card.zone.toUpperCase());
                }

                cardView.measure(
                        View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                cardView.layout(0, 0, cardView.getMeasuredWidth(), cardView.getMeasuredHeight());

                // start a new page if the card would overflow
                if (currentY + cardView.getMeasuredHeight() > pageHeight - 20) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    currentY = 50;
                }

                // draw the card at current yOffset
                canvas.save();
                canvas.translate(0, currentY);
                cardView.draw(canvas);
                canvas.restore();

                // increment yOffset for next card
                currentY += cardView.getMeasuredHeight() + 10; // spacing between cards
            }
            if (card.passFilter && card.cardType == HistoryItem.typeOfCard.triage && options[3]) {
                View cardView = inflater.inflate(R.layout.pdf_card_triage, null, false);

                TextView triageTitle = cardView.findViewById(R.id.triageTitle);
                triageTitle.setText("Incident Log @ " + card.time);
                TextView presentFlags = cardView.findViewById(R.id.presentFlags);
                presentFlags.setText(String.join(", ",card.flaglist));
                TextView pefValue = cardView.findViewById(R.id.pefValue);
                if(card.pef == -10){pefValue.setText("N/A");}
                else if(card.pef == -5){pefValue.setText("Not Entered");}
                else{pefValue.setText(Integer.toString(card.pef));}
                TextView rescueAttempts = cardView.findViewById(R.id.rescueAttempts);
                if(card.rescueAttempts == -5){rescueAttempts.setText("0");}
                else if(card.rescueAttempts == -10){rescueAttempts.setText("N/A");}
                else{rescueAttempts.setText(Integer.toString(card.rescueAttempts));}
                TextView emergencyCall = cardView.findViewById(R.id.emergencyCall);
                emergencyCall.setText(card.emergencyCall);
                TextView userResponseList = cardView.findViewById(R.id.userResponseList);
                userResponseList.setText(card.userBullets);

                cardView.measure(
                        View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                cardView.layout(0, 0, cardView.getMeasuredWidth(), cardView.getMeasuredHeight());

                // start a new page if the card would overflow
                if (currentY + cardView.getMeasuredHeight() > pageHeight - 20) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    currentY = 50;
                }

                // draw the card at current yOffset
                canvas.save();
                canvas.translate(0, currentY);
                cardView.draw(canvas);
                canvas.restore();

                // increment yOffset for next card
                currentY += cardView.getMeasuredHeight() + 10; // spacing between cards
            }
        }

        // finish the last page
        pdfDocument.finishPage(page);

        File pdfFile = new File(requireContext().getExternalFilesDir(null), "historyLog_"+childName+".pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(getContext(), "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving PDF", Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
        }
    }

    public void setChildUid(String childUid) {
        if(childUid.equals("")){
            return;
        }
        this.childUid = childUid;
        repo.getCards(childUid,this);
        repo.updateToggles(childUid,this);
    }

    public void exitScreen(){
        //meant to do nothing
    }

    private void setUpOneFilterUI(int type, String [] items){
        AutoCompleteTextView dropdown = view.findViewById(type);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                com.google.android.material.R.layout.support_simple_spinner_dropdown_item,
                items
        );

        dropdown.clearFocus();
        dropdown.setAdapter(adapter);
    }

    private void setUpFilterUI(){
        String [] nightWakingOptions = {"","YES","NO"};
        String [] activityLimitsOptions = {"","0-1","2-3","4-5","6-7","8-9","10"};
        String [] coughingLevelOptions = {"","No Coughing", "Wheezing", "Coughing", "Extreme Coughing"};
        String [] triggersOptions = {"","Allergies", "Smoke","Flu","Strong smells", "Running", "Exercise", "Cold Air", "Dust/Pets", "Illness"};
        String [] triageOptions = {"","Days with Triage","Days without Triage"};
        String [] dateOptions = {"", "Past 3 months", "Past month", "Past 2 weeks", "Past week", "Past 2 days"};
        setUpOneFilterUI(R.id.selectNightWaking,nightWakingOptions);
        setUpOneFilterUI(R.id.selectActivityLimits,activityLimitsOptions);
        setUpOneFilterUI(R.id.selectCoughingLevel,coughingLevelOptions);
        setUpOneFilterUI(R.id.selectTriggers,triggersOptions);
        setUpOneFilterUI(R.id.selectTriage,triageOptions);
        setUpOneFilterUI(R.id.selectDate,dateOptions);
    }

    public void createRecycleView(List<HistoryItem> results) {
        adapter.updateList(results);
        RecyclerView recyclerView = view.findViewById(R.id.historyRecyclerView);
        HistoryAdapter adapter = new HistoryAdapter(results); // your list of HistoryItem
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    public void fixToggles(Map<String, Boolean> sharing, Boolean allTrue) {
        if(allTrue){
            for(int i = 0; i < options.length; i++){
                options[i] = true;
            }
        }
        else {
            options[0] = sharing != null && sharing.containsKey("pef") ? sharing.get("pef") : false;
            options[1] = sharing != null && sharing.containsKey("rescue") ? sharing.get("rescue") : false;
            options[2] = sharing != null && sharing.containsKey("symptoms") ? sharing.get("symptoms") : false;
            options[3] = sharing != null && sharing.containsKey("triage") ? sharing.get("triage") : false;
            options[4] = sharing != null && sharing.containsKey("triggers") ? sharing.get("triggers") : false;
        }

        AutoCompleteTextView selectNightWaking = view.findViewById(R.id.selectNightWaking);
        selectNightWaking.setEnabled(options[2]);
        selectNightWaking.setClickable(options[2]);
        selectNightWaking.setFocusable(options[2]);
        selectNightWaking.setFocusableInTouchMode(options[2]);
        AutoCompleteTextView selectCoughing = view.findViewById(R.id.selectCoughingLevel);
        selectCoughing.setEnabled(options[2]);
        selectCoughing.setClickable(options[2]);
        selectCoughing.setFocusable(options[2]);
        selectCoughing.setFocusableInTouchMode(options[2]);
        AutoCompleteTextView selectActivity = view.findViewById(R.id.selectActivityLimits);
        selectActivity.setEnabled(options[2]);
        selectActivity.setClickable(options[2]);
        selectActivity.setFocusable(options[2]);
        selectActivity.setFocusableInTouchMode(options[2]);
        AutoCompleteTextView selectTriggers = view.findViewById(R.id.selectTriggers);
        selectTriggers.setEnabled(options[4]);
        selectTriggers.setClickable(options[4]);
        selectTriggers.setFocusable(options[4]);
        selectTriggers.setFocusableInTouchMode(options[4]);
        AutoCompleteTextView selectTriage = view.findViewById(R.id.selectTriage);
        selectTriage.setEnabled(options[3]);
        selectTriage.setClickable(options[3]);
        selectTriage.setFocusable(options[3]);
        selectTriage.setFocusableInTouchMode(options[3]);

        repo.getCards(childUid,this);

    }
}
