package com.example.smart_air.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.R;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.modelClasses.InventoryData;
import com.example.smart_air.modelClasses.formatters.StringFormatters;
import com.example.smart_air.viewmodel.DashboardViewModel;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DashboardFragment extends Fragment {
    private SharedChildViewModel sharedModel;
    private float x1, x2;
    private static final int MIN_DISTANCE = 150;
    private ViewFlipper trendsCarousel;
    private View zoneBar, barContainer;
    private LineChart pefChart;
    private BarChart rescueChart;
    private final FirebaseFirestore db = FirebaseInitalizer.getDb();
    private TextView tvWeeklyRescues, tvRescueName, tvRescuePurchase, tvRescueExpiry, tvControllerName, tvControllerPurchase, tvControllerExpiry;
    private TextView rescueChartUnavailableRescue, rescueChartUnavailablePEF;
    private String correspondingUid, userRole;
    private AuthRepository repo;
    private int currentTrendIndex = 0;
    private boolean allowRescue = false, allowController = false, allowPEF = false, allowCharts = false;
    TextView tvLastRescue;
    private LinearLayout cardLastRescue, cardWeekly, inventoryGroup, trendSection;
    private Button btnManage, btnProviderReport;
    //For shared label tag on top
    View sharedProviderLabel;
    TextView sharedLabelText;
    // CACHES
    private final Map<String, Map<String, Pair<String, Integer>>> zoneCache = new HashMap<>(); //uid - > date -> (zonecol, zonenum)
    private final Map<String, List<DocumentSnapshot>> weeklyRescueCache = new HashMap<>();
    private final Map<String, Date> latestRescueCache = new HashMap<>();
    private final Map<String, List<DocumentSnapshot>> pefCache = new HashMap<>();
    private final Map<String, InventoryData> inventoryCache = new HashMap<>();
    private final Map<String, Map<String, Boolean>> childSharingCache = new HashMap<>(); //for parent - as parent can only change cache
    //LISTENER FOR TOGGLES
    private ListenerRegistration childListener;
    //VIEW MODEL FOR PDF (TO NOT REGENERATE EXTRA INFO)
    DashboardViewModel cacheVM;

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new AuthRepository();
        if(repo.getCurrentUser() == null) { return; } //unauth

        TextView tvTitle = view.findViewById(R.id.tvDashboardTitle);
        View root = view;

        // using viewmodel to load dashboard
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        cacheVM = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

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
        tvLastRescue = view.findViewById(R.id.tvLastRescue);

        //load the cards for toggling
        cardWeekly = view.findViewById(R.id.cardWeekly);
        cardLastRescue = view.findViewById(R.id.cardLastRescue);
        inventoryGroup = view.findViewById(R.id.inventoryGroup);
        trendSection = view.findViewById(R.id.trendSection);
        //load the not avail text
        rescueChartUnavailableRescue = view.findViewById(R.id.rescueChartUnavailableRescue);
        rescueChartUnavailablePEF = view.findViewById(R.id.rescueChartUnavailablePEF);
        //load the shared provider tag
        sharedProviderLabel = view.findViewById(R.id.sharedProviderLabel);
        sharedLabelText = view.findViewById(R.id.sharedLabelText);

        btnManage = view.findViewById(R.id.btnManageChildren);
        btnProviderReport = view.findViewById(R.id.btnProviderReport);


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
                            if (currentTrendIndex == 0) { return true; }

                            trendsCarousel.setInAnimation(getContext(), R.anim.slide_in_left);
                            trendsCarousel.setOutAnimation(getContext(), R.anim.slide_out_right);
                            trendsCarousel.showPrevious();

                            currentTrendIndex--;
                        }

                        // swipe left
                        else {
                            if (currentTrendIndex == 1) { return true; }

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


        btnProviderReport.setOnClickListener(v -> {
            if(correspondingUid == null) return;
            ProviderReportFragment frag = new ProviderReportFragment();
            Bundle args = new Bundle();
            args.putString("childId", correspondingUid);
//            if (childSharingCache.containsKey(correspondingUid)) {
//                HashMap<String, Boolean> sharingMap = (HashMap<String, Boolean>) childSharingCache.get(correspondingUid);
//                args.putSerializable("sharing", sharingMap);
//            }
            frag.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction().replace(R.id.fragment_container, frag)
                    .addToBackStack(null).commit();
        });

        // first checks the role
        sharedModel.getCurrentRole().observe(getViewLifecycleOwner(), role -> {
            if (role == null) return;
            userRole = role;
            if (role.equals("child")) {
                correspondingUid = repo.getCurrentUser().getUid(); //load right UID right away
                btnProviderReport.setVisibility(View.GONE);
                btnManage.setVisibility(View.GONE);
                tvTitle.setText("Your Dashboard");
                //in general, inventory is parent-only
                inventoryGroup.setVisibility(View.GONE);
                loadDashboardForChild(correspondingUid);
            }
            if(role.equals("parent")) {
                // show full dashboard
                btnProviderReport.setVisibility(View.VISIBLE);
                btnManage.setVisibility(View.VISIBLE);
            } else if(role.equals("provider")) {
                btnManage.setVisibility(View.GONE);
                //in general, inventory is parent-only
                inventoryGroup.setVisibility(View.GONE);
            }

        });

        // use viewmodel to load role and dashboard
        sharedModel.getAllChildren().observe(getViewLifecycleOwner(), children -> {
            if (children == null || children.isEmpty()) return;
            Integer idx = sharedModel.getCurrentChild().getValue();
            if (idx == null) idx = 0;
            correspondingUid = children.get(idx).getChildUid();
            if (!"child".equals(userRole)) {
                Child child = children.get(idx);
                //extract name only
                String fullName = child.getName();
                String displayName = fullName.replaceFirst("\\[.*", "").trim();
                tvTitle.setText(displayName + "’s Dashboard");
                if("parent".equals(userRole)) loadTogglesParent(correspondingUid); //load the current toggles for the child
                loadDashboardForChild(correspondingUid);
            }
        });

        // for when parent or provider switches child
        sharedModel.getCurrentChild().observe(getViewLifecycleOwner(), idx -> {
            List<Child> list = sharedModel.getAllChildren().getValue();
            if (list == null || list.isEmpty() || idx == null) return;
            correspondingUid = list.get(idx).getChildUid();
            if("parent".equals(userRole)) {
                loadTogglesParent(correspondingUid); //load the current toggles for the child
            }
            if (!"child".equals(userRole)) {
                //extract name only
                String fullName = list.get(idx).getName();
                String displayName = fullName.replaceFirst("\\[.*", "").trim();
                tvTitle.setText(displayName + "’s Dashboard");
                loadDashboardForChild(correspondingUid);
            }
        });

        //adapter to switch between days
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_black, //so text is black instead of white
                new String[]{"Past 7 Days", "Past 30 Days"}
        );
        trendSpinner.setAdapter(adapter);
        trendSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (correspondingUid == null) return;
                if (userRole.equals("provider") && !allowCharts) { return; }
                //else move on with positions
                if (position == 0) {
                    if (userRole.equals("provider")) {
                        if(allowPEF)  loadPEFTrend(7);
                        if(allowRescue)  loadWeeklyRescues(7);
                        return;
                    }
                    loadWeeklyRescues(7); loadPEFTrend(7);
                } else {
                    if (userRole.equals("provider")) {
                        if(allowPEF)  loadPEFTrend(30);
                        if(allowRescue)  loadWeeklyRescues(30);
                        return;
                    }
                    loadWeeklyRescues(30); loadPEFTrend(30);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }

    private void freezeOtherPage() {
        int displayed = trendsCarousel.getDisplayedChild();
        for (int i = 0; i < trendsCarousel.getChildCount(); i++) {
            View child = trendsCarousel.getChildAt(i);
            if (i != displayed) { child.setVisibility(View.INVISIBLE); }
        }
    }
    private void unfreezeAllPages() {
        for (int i = 0; i < trendsCarousel.getChildCount(); i++) { trendsCarousel.getChildAt(i).setVisibility(View.VISIBLE); }
    }

    //Load toggles for the parent
    private void loadTogglesParent(String childUid) {
        if (childUid == null) return;

        // check cache first
        if (childSharingCache.containsKey(childUid)) { applySharingTogglesParent(childSharingCache.get(childUid)); return; }

        // fetch
        db.collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) return;
                    Map<String, Boolean> sharing = (Map<String, Boolean>) doc.get("sharing");
                    if (sharing == null) sharing = Child.initalizeSharing();
                    // cache
                    childSharingCache.put(childUid, sharing);
                    cacheVM.putChildSharing(childUid, sharing);
                    // apply toggles to the UI for parent
                    applySharingTogglesParent(sharing);
                });
    }

    private void applySharingTogglesParent(Map<String, Boolean> sharing) {
        //now apply the 'visible to provider' tag on top
        if (sharing == null || sharing.isEmpty()) {
            sharedLabelText.setText("Shared with Provider: None");
            sharedProviderLabel.setVisibility(View.GONE);
            return;
        }
        StringBuilder sb = new StringBuilder("Shared with Provider: ");
        boolean first = true;

        for (Map.Entry<String, Boolean> entry : sharing.entrySet()) {
            if (entry.getValue() != null && entry.getValue()) {
                String labelText = StringFormatters.getLabelForKey(entry.getKey());
                if (!first) sb.append(", "); //dont append comma if its not first
                sb.append(labelText);
                first = false;
            }
        }

        if (first) { sharedLabelText.setText("Shared with Provider: None"); }
        else { sharedLabelText.setText(sb.toString()); }
        sharedProviderLabel.setVisibility(View.VISIBLE);
    }

    private void loadDashboardForChild(String childUid) {
        if (childUid == null) return;
        this.correspondingUid = childUid;
        loadTodayZone(); //for everyone (zone)
        if(userRole!= null && userRole.equals("provider")) {
            attachChildToggleListener(childUid); //load toggles for child and then init dashboard
            //do not load inventory for provider
        } else if( userRole != null && userRole.equals("child")) {
            loadWeeklyRescues(7);
            loadPEFTrend(7);
            loadLatestRescueDate();
            //do not load inventory for child
        }
        else if(userRole != null && userRole.equals("parent")) {
            loadInventory(); //parent only thing
            loadWeeklyRescues(7);
            loadPEFTrend(7);
            loadLatestRescueDate();
        }

    }

    //Helper function for toggle listener
    private void attachChildToggleListener(String childUid) {
        // remove prev listener if child switched
        if (childListener != null) { childListener.remove(); }
        //fetch toggles
        childListener = db.collection("children")
                .document(childUid)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    //re-init all to false
                    allowRescue = false;
                    allowController = false;
                    allowPEF = false;
                    allowCharts = false;
                    Map<String, Boolean> sharing = (Map<String, Boolean>) doc.get("sharing");
                    //change sharing toggles
                    if (sharing != null) {
                        allowRescue = sharing.getOrDefault("rescue", false);
                        allowController = sharing.getOrDefault("controller", false);
                        allowPEF = sharing.getOrDefault("pef", false);
                        allowCharts = sharing.getOrDefault("charts", false);
                    }
                    // provider dashboard visibility update
                    cardWeekly.setVisibility(allowRescue ? View.VISIBLE : View.GONE);
                    cardLastRescue.setVisibility(allowRescue ? View.VISIBLE : View.GONE);
                    trendSection.setVisibility(allowCharts ? View.VISIBLE : View.GONE);
                    //visibility updates on the charts as well
                    rescueChart.setVisibility(allowRescue ? View.VISIBLE : View.GONE);
                    pefChart.setVisibility(allowPEF ? View.VISIBLE : View.GONE);
                    //visibility updates on whether chart available or not
                    rescueChartUnavailablePEF.setVisibility(allowPEF ? View.GONE : View.VISIBLE);
                    rescueChartUnavailableRescue.setVisibility(allowRescue ? View.GONE : View.VISIBLE);
                    // reload data based on toggles
                    if (allowRescue) { loadWeeklyRescues(7); loadLatestRescueDate(); }
                    if (allowPEF) { loadPEFTrend(7); }
                });
    }

    /* Functions to load different sections of the dashboard */

    //Function loads the 'today zone bar'
    private void loadTodayZone() {
        if (correspondingUid == null) return;
        String dateKey = LocalDate.now().toString();
        // check cache first
        if (zoneCache.containsKey(correspondingUid) && Objects.requireNonNull(zoneCache.get(correspondingUid)).containsKey(dateKey)) {
            Pair<String, Integer> cached = zoneCache.get(correspondingUid).get(dateKey);
            updateZoneBar(cached.first, cached.second);
            return;
        }
        //if not cached fetch
        db.collection("dailyCheckins")
                .document(correspondingUid)
                .collection("entries")
                .document(dateKey)
                .get()
                .addOnSuccessListener(doc -> {
                    String zoneColour;
                    int zoneNum;
                    if (doc.exists()) {
                        zoneColour = doc.getString("zoneColour");
                        if (zoneColour == null) zoneColour = "none";

                        Long zoneNumber = doc.getLong("zoneNumber");
                        zoneNum = (zoneNumber != null) ? zoneNumber.intValue() : 0;
                    } else {
                        zoneColour = "none";
                        zoneNum = 0;
                    }
                    // cache the result
                    zoneCache
                            .computeIfAbsent(correspondingUid, k -> new HashMap<>())
                            .put(dateKey, new Pair<>(zoneColour, zoneNum));
                    updateZoneBar(zoneColour, zoneNum);
                })
                .addOnFailureListener(e -> updateZoneBar("none", 0));
    }

    //Function updates the zone bar
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

    /*
     * Function loads the weekly rescue data and caches data
     */
    private void loadWeeklyRescues(int days) {
        if (correspondingUid == null) return;

        LocalDate cutoff = LocalDate.now().minusDays(days - 1);

        List<DocumentSnapshot> cachedDocs = weeklyRescueCache.get(correspondingUid);
        if (cachedDocs != null) { processWeeklyRescueDocuments(cachedDocs, cutoff, days); processWeeklyRescueStat(cachedDocs); return; }
        //if not cache all snapshots (childre/{childUid}/rescueLog
        db.collection("children")
                .document(correspondingUid)
                .collection("rescueLog")
                .get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> docs = snap.getDocuments();
                    weeklyRescueCache.put(correspondingUid, docs);
                    cacheVM.putWeeklyRescue(correspondingUid, docs);
                    processWeeklyRescueDocuments(docs, cutoff, days);
                    processWeeklyRescueStat(docs);
                });
    }

    //Function processes the weekly rescue stat independently (one week fixed)
    private void processWeeklyRescueStat(List<DocumentSnapshot> docs) {
        //define week threshold
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        int weeklyTotal = 0;
        for (DocumentSnapshot doc : docs) {
            Timestamp ts = doc.getTimestamp("timeTaken");
            if (ts == null) continue;
            LocalDate rescueDate = ts.toDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            //if doc in current week we count it
            if (!rescueDate.isBefore(weekStart) && !rescueDate.isAfter(weekEnd)) { weeklyTotal += 1; }
        }
        if (tvWeeklyRescues != null) { tvWeeklyRescues.setText(weeklyTotal > 0 ? weeklyTotal + " rescue(s)" : "No rescues yet"); }
    }


    //process each document independently
    private void processWeeklyRescueDocuments(List<DocumentSnapshot> docs, LocalDate cutoff, int days) {
        int[] counts = new int[days];
        for (DocumentSnapshot doc : docs) {
            Timestamp ts = doc.getTimestamp("timeTaken");
            if (ts == null) continue;
            LocalDate rescueDate = ts.toDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();

            if (rescueDate.isBefore(cutoff)) continue;

            int index = (int) ChronoUnit.DAYS.between(cutoff, rescueDate);
            if (index >= 0 && index < days) {
                counts[index] += 1; // count 1 per document
            }
        }
        drawRescueChartDynamic(counts, days);
    }


    /*
    * Function loads the latest rescue date with one fetch call and caches data
    */
    private void loadLatestRescueDate() {
        if (correspondingUid == null) return;
        // check cache first
        if (latestRescueCache.containsKey(correspondingUid)) {
            Date cachedDate = latestRescueCache.get(correspondingUid);
            if (cachedDate == null) {tvLastRescue.setText("No rescues yet"); }
            else {
                String formatted = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        .format(cachedDate);
                tvLastRescue.setText(formatted);
            }
            return;
        }

        db.collection("children")
                .document(correspondingUid)
                .collection("rescueLog")
                .orderBy("timeTaken", Query.Direction.DESCENDING)
                .limit(1) // only need one
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        tvLastRescue.setText("No rescues yet");
                        latestRescueCache.put(correspondingUid, null);
                        return;
                    }

                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    Timestamp ts = doc.getTimestamp("timeTaken");
                    Date latestRescueDate = (ts != null) ? ts.toDate() : null;

                    // cache the result
                    latestRescueCache.put(correspondingUid, latestRescueDate);

                    if (latestRescueDate == null) { tvLastRescue.setText("No rescues yet"); }
                    else {
                        String formatted = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                .format(latestRescueDate);
                        tvLastRescue.setText(formatted);
                    }
                })
                .addOnFailureListener(e -> tvLastRescue.setText("Error loading"));
    }

    //Function loads the PEF trend and also caches data
    private void loadPEFTrend(int days) {
        if (correspondingUid == null) return;
        if ("provider".equals(userRole) && !allowRescue) return;

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        List<DocumentSnapshot> cachedDocs = pefCache.get(correspondingUid);
        if (cachedDocs != null) {
            processPEFDocuments(cachedDocs, startDate, today, days);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("dailyCheckins")
                .document(correspondingUid)
                .collection("entries")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    pefCache.put(correspondingUid, docs); // cache if not in there yet
                    cacheVM.putPefCache(correspondingUid, docs);
                    processPEFDocuments(docs, startDate, today, days);
                });
    }

    //process each document independently
    private void processPEFDocuments(List<DocumentSnapshot> docs, LocalDate startDate, LocalDate endDate, int days) {
        Map<LocalDate, Integer> pefByDate = new HashMap<>();

        for (DocumentSnapshot doc : docs) {
            LocalDate entryDate;
            try {
                entryDate = LocalDate.parse(doc.getId());
            } catch (Exception e) {
                continue;
            }

            if (entryDate.isBefore(startDate) || entryDate.isAfter(endDate)) continue;

            Long pef = doc.getLong("pef");
            if (pef != null) pefByDate.put(entryDate, pef.intValue());
        }

        int[] pefValues = new int[days];
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            pefValues[i] = pefByDate.getOrDefault(d, 0);
        }

        drawPEFLineChart(pefValues, days);
    }

    // inventory [NOT CACHED]
    private void loadInventory() {
        if (correspondingUid == null) return;

        // Check permissions first
        if ("provider".equals(userRole) && !allowRescue) {
            tvRescueName.setText("Not Available");
            tvRescueExpiry.setText(" ");
            tvRescuePurchase.setText(" ");
        }
        if ("provider".equals(userRole) && !allowController) {
            tvControllerName.setText("Not Available");
            tvControllerExpiry.setText(" ");
            tvControllerPurchase.setText(" ");
        }

        //check if in cache
        InventoryData cached = inventoryCache.get(correspondingUid);
        if (cached != null) { updateInventoryUI(cached); return; }

        //load data references; inventory -> rescue & controller
        DocumentReference rescueRef = db.collection("children")
                .document(correspondingUid)
                .collection("inventory")
                .document("rescue");

        DocumentReference controllerRef = db.collection("children")
                .document(correspondingUid)
                .collection("inventory")
                .document("controller");

        InventoryData data = new InventoryData();

        rescueRef.get().addOnSuccessListener(rescueDoc -> {
            if (rescueDoc.exists()) {
                data.rescueName = rescueDoc.getString("name");
                data.rescueAmount = rescueDoc.getLong("amount");
                data.rescuePurchase = rescueDoc.getTimestamp("purchaseDate");
                data.rescueExpiry = rescueDoc.getTimestamp("expiryDate");
            }

            controllerRef.get().addOnSuccessListener(ctrlDoc -> {
                if (ctrlDoc.exists()) {
                    data.controllerName = ctrlDoc.getString("name");
                    data.controllerAmount = ctrlDoc.getLong("amount");
                    data.controllerPurchase = ctrlDoc.getTimestamp("purchaseDate");
                    data.controllerExpiry = ctrlDoc.getTimestamp("expiryDate");
                }

                //cache data if just fetched
                inventoryCache.put(correspondingUid, data);

                //update ui
                updateInventoryUI(data);
            });
        });
    }

    private void updateInventoryUI(InventoryData data) {
        //rescue
        if (data.rescueName != null || data.rescueAmount != null) {
            String display = "";
            if (data.rescueName != null) display += data.rescueName;
            if (data.rescueAmount != null) display += ":  " + data.rescueAmount;
            tvRescueName.setText(display);
            tvRescuePurchase.setText(data.rescuePurchase != null ?
                    "Purchase Date: " + formatDate(data.rescuePurchase) : "-");
            tvRescueExpiry.setText(data.rescueExpiry != null ?
                    "Expiry Date: " + formatDate(data.rescueExpiry) : "-");
        }

        //controller
        if (data.controllerName != null || data.controllerAmount != null) {
            String display = "";
            if (data.controllerName != null) display += data.controllerName;
            if (data.controllerAmount != null) display += ":  " + data.controllerAmount;
            tvControllerName.setText(display);
            tvControllerPurchase.setText(data.controllerPurchase != null ?
                    "Purchase Date: " + formatDate(data.controllerPurchase) : "-");
            tvControllerExpiry.setText(data.controllerExpiry != null ?
                    "Expiry Date: " + formatDate(data.controllerExpiry) : "-");
        }
    }

    private void drawRescueChartDynamic(int[] counts, int days) {
        if (rescueChart == null) return;
        if ("provider".equals(userRole) && !allowRescue){ return; }

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

        if (days == 30) { xAxis.setTextSize(2f); xAxis.setLabelRotationAngle(280f); }
        else { xAxis.setTextSize(10f); xAxis.setLabelRotationAngle(0f); }

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

    private String formatDate(Timestamp ts) {
        Date date = ts.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (childListener != null) childListener.remove();
    }
}
