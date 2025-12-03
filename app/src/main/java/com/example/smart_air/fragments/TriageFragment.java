package com.example.smart_air.fragments;
import java.util.List;

import static android.content.ContentValues.TAG;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.modelClasses.Notification;
import com.example.smart_air.modelClasses.enums.NotifType;
import com.google.firebase.Timestamp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smart_air.R;
import com.example.smart_air.TriageState;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

public class TriageFragment extends Fragment {
    ImageButton checktimerbutton;
    TextView timertextview;
    Double time=0.0;
    boolean timervisible=false;
    Timer timer;
    TimerTask timertask;
    EditText rraText;
    int recentresattNum=-1;
    EditText pefTriText;
    boolean changedPEF=false;
    int optTriPEF=-1;
    boolean checkedsen, checkedpull, checkedretract, checkedbluelips;
    boolean emergcalled=false;
    Button emergcall, homestart;
    boolean homestartpressed=false;
    String decisioncardchoice;
    String[] stringflags;
    String [] guidance=new String[2];
    String [] userRes;
    MaterialButton startTriageBtn, endTriageBtn, flgchestpull, flgBluelips, flgsentences, flgretract;
    boolean triageRunning = false;
    static final long TRIAGE_DURATION_MS =10*60*1000;
    String uid;
    private String selectedChildUid = null;
    private String selectedZone = "greenZone";
    private int currentStepCount = 0;
    String zonecolour;
    private SharedChildViewModel sharedModel;
    private String role;

    public TriageFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.triage_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View triageContent      = view.findViewById(R.id.triageContent);
        View parentContent      = view.findViewById(R.id.parentContent);
        View childStepsContent  = view.findViewById(R.id.childStepsContent);
        View loadingView        = view.findViewById(R.id.loadingView);

        // hide everything first
        triageContent.setVisibility(View.GONE);
        parentContent.setVisibility(View.GONE);
        childStepsContent.setVisibility(View.GONE);
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        }
        // getting role to put correct view
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //fetch role from vm
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        sharedModel.getCurrentRole().observe(getViewLifecycleOwner(), role -> {
            if (role != null) {
                this.role = role;
                if (loadingView != null) {
                    loadingView.setVisibility(View.GONE);
                }
                // child view
                if ("child".equals(role)) {
                    parentContent.setVisibility(View.GONE);
                    childStepsContent.setVisibility(View.GONE);
                    triageContent.setVisibility(View.VISIBLE);
                    triagesession(view);
                }
                //parent view
                else {
                    triageContent.setVisibility(View.GONE);
                    childStepsContent.setVisibility(View.GONE);
                    parentContent.setVisibility(View.VISIBLE);
                    parentactionsession(view);
                }
            }
        });

    }

    public void triagesession(View view) {
        //initialize pef
        initializeoptPEF();
        // timer setup
        checktimerbutton = view.findViewById(R.id.timer);
        timertextview = view.findViewById(R.id.timerTextV);
        timer = new Timer();
        // set up triage session buttons
        startTriageBtn = view.findViewById(R.id.startriage);
        endTriageBtn   = view.findViewById(R.id.endtriage);
        // check if there is an active triage from before (persisted in TriageState)
        long now  = System.currentTimeMillis();
        boolean triageStillActive = (TriageState.triageendtime > now);
        if (triageStillActive==true) {
            // triage is running from earlier (even if they logged out)
            triageRunning = true;
            timervisible  = true;
            timertextview.setVisibility(View.VISIBLE);
            startTriageBtn.setEnabled(false);
            endTriageBtn.setEnabled(true);
            showtimer();   // resume countdown from TriageState.triageendtime
        }
        else {
            // no current triage session
            triageRunning = false;
            timervisible  = false;
            timertextview.setVisibility(View.GONE);
            startTriageBtn.setEnabled(true);
            endTriageBtn.setEnabled(false);
            TriageState.triageendtime = 0L;  // reset
        }

        // toggle timer visibility on icon click
        checktimerbutton.setOnClickListener(v -> toggletimerdisplay());
        startTriageBtn.setOnClickListener(v -> {
            startTriage();
            sendtriageAlert();
        });
        endTriageBtn.setOnClickListener(v -> {
            flagschecked(checkedpull,checkedsen,checkedretract,checkedbluelips);
            useresponseadd();
            decisionpressed(emergcalled,homestartpressed);
            guidanceadd(decisioncardchoice);
            savetriagesession();
            endTriage();
        });

        checktimerbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggletimerdisplay();
            }
        });
        // inside triagesession(View view)
        homestart = view.findViewById(R.id.button4);
        homestart.setOnClickListener(v -> {
            homestartpressed = true;
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                return;
            }
            String childUid = user.getUid();
            getlatestcolour(childUid, zone -> {
                zonecolour=zone;
                if (zone == null) {
                    Toast.makeText(requireContext(), "No recent zone found", Toast.LENGTH_SHORT).show();
                    return;
                }
                // show the Start Home Steps page inside this fragment
                openstarthomesteps(childUid, zone);
            });
        });
        // emergency call click
        emergcall=view.findViewById(R.id.button3);
        emergcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emergcalled=true;
                Intent intentcall = new Intent(Intent.ACTION_DIAL);
                intentcall.setData(Uri.parse("tel:911"));
                startActivity(intentcall);
            }
        });
        // PEF and recent rescue attempts
        rraText=view.findViewById(R.id.textrra);
        pefTriText=view.findViewById(R.id.textpef);
        rraText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String txt = s.toString().trim();
                if (txt.isEmpty()==false) {
                    try {
                        recentresattNum = Integer.parseInt(txt);
                    } catch (NumberFormatException e) {
                        recentresattNum = -1;
                        Log.e(TAG, "Invalid number in recent rescue attempts: " + txt, e);
                    }
                }
                else { recentresattNum = -1; }
            }
            @Override
            public void afterTextChanged(Editable s)  {}
        });

        pefTriText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String txt = s.toString().trim();
                if (txt.isEmpty()==false) {
                    changedPEF=true;
                    try {
                        optTriPEF = Integer.parseInt(txt);
                    }
                    catch (NumberFormatException e) {
                        initializeoptPEF();
                        Log.e(TAG, "Invalid number in recent rescue attempts: " + txt, e);
                    }
                }
                else {
                    changedPEF=false;
                    initializeoptPEF();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        // quick red flag checks on triage session
        flgsentences=view.findViewById(R.id.btn_flag_sentences);
        flgsentences.setOnClickListener(v -> {
            checkedsen=!checkedsen;
            updateflgbtn(flgsentences,checkedsen);
        });
        flgchestpull=view.findViewById(R.id.btn_flag_chest);
        flgchestpull.setOnClickListener(v -> {
            checkedpull=!checkedpull;
            updateflgbtn(flgchestpull,checkedpull);
        });
        flgretract=view.findViewById(R.id.btn_flag_retractions);
        flgretract.setOnClickListener(v -> {
            checkedretract=!checkedretract;
            updateflgbtn(flgretract,checkedretract);
        });
        flgBluelips=view.findViewById(R.id.btn_flag_blue);
        flgBluelips.setOnClickListener(v -> {
            checkedbluelips=!checkedbluelips;
            updateflgbtn(flgBluelips,checkedbluelips);
        });

    }
    private void toggletimerdisplay() {
        if (timervisible) {
            timervisible=false;
            timertextview.setVisibility(View.GONE);
            if (timertask!=null) {
                timertask.cancel();
            }
        }
        else {
            timervisible=true;
            timertextview.setVisibility(View.VISIBLE);
            showtimer();
        }
    }
    // functions to manage 10 minute timer
    public void showtimer() {
        if (!isAdded()) { return; }

        long now=System.currentTimeMillis();
        long diff= TriageState.triageendtime-now;
        if (diff<=0) {
            if (timertextview!=null) { timertextview.setText("00:00:00"); }
            return;
        }
        if (timertask!=null) { timertask.cancel(); }

        time=diff/1000.0;
        timertask = new TimerTask() {
            @Override
            public void run() {
                if (!isAdded()) { return; }

                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (timertextview==null) { return; }
                        if (time<=0) {
                            timertextview.setText("00:00:00");
                            timertask.cancel();
                            sendtriageAlert();
                            return;
                        }
                        time--;
                        timertextview.setText(gettimertext());
                    }
                });
            }
        };
        if (timer==null) {
            timer = new Timer();
        }
        timer.scheduleAtFixedRate(timertask,0,1000);
    }
    public String gettimertext() {
        int round=(int)Math.round(time);
        int sec=((round%86400)%3600)%60;
        int min=((round%86400)%3600)/60;
        int hrs=((round%86400)/3600);
        return formatTime(sec,min,hrs);
    }
    public String formatTime(double sec, double min, double hrs) {
        return String.format(Locale.US, "%02d",(int)hrs)+":"+String.format(Locale.US, "%02d",(int)min)+":"+String.format(Locale.US, "%02d",(int)sec);
    }

    /* runs when fragment UI is destroyed, fragment is alive
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (timertask!=null) {
            timertask.cancel();
            timertask=null;
        }

        if (timer!=null) {
            timer.cancel();
            timer=null;
        }
        timertextview=null;
        checktimerbutton=null;
        rraText=null;
        pefTriText=null;
        flgsentences=null;
        flgchestpull=null;
        flgretract=null;
        flgBluelips=null;
        emergcall=null;
        homestart=null;
        startTriageBtn=null;
        endTriageBtn=null;
    }
    /*
    changes button when checked in quick red flags
     */
    public void updateflgbtn(MaterialButton btn, boolean checked) {
        if (checked==true) {
            btn.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.checkbox));
            btn.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.white)));
        }
        else { btn.setIcon(null); }
    }
    /*
    records which buttons user presses in decision card
     */
    public void decisionpressed(boolean ecall, boolean hsteps)
    {
        if (!ecall && !hsteps) { decisioncardchoice="None"; }
        else if (ecall && !hsteps) { decisioncardchoice="Call Emergency Now button clicked"; }
        else if (!ecall && hsteps) { decisioncardchoice="Start Home Steps button clicked"; }
        else { decisioncardchoice="Call Emergency Now and Start Home Steps buttons clicked"; }
    }

    /*
  if optional PEF isn't filled, use one from daily check-ins
   */
    public void initializeoptPEF() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "initializeoptPEF: User not logged in");
            optTriPEF = -1;
            return;
        }
        String childUid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    Log.d(TAG, "initializeoptPEF: snapshot size = " + snap.size());
                    if (snap.isEmpty()) {
                        optTriPEF = -1;
                        Log.d(TAG, "initializeoptPEF: No daily check-in entries found for child");
                        return;
                    }
                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    Log.d(TAG, "initializeoptPEF: latest entry id = " + doc.getId());
                    Number pefNumber = (Number) doc.get("pef");
                    if (pefNumber != null) {
                        optTriPEF = pefNumber.intValue();
                        Log.d(TAG, "initializeoptPEF: Fetched latest daily PEF = " + optTriPEF);
                    } else {
                        optTriPEF = -1;
                        Log.d(TAG, "initializeoptPEF: Latest daily check-in had no 'pef' field");
                    }
                })
                .addOnFailureListener(e -> {
                    optTriPEF = -1;
                    Log.e(TAG, "initializeoptPEF: Error fetching latest daily check-in PEF", e);
                });
    }


    /*
    records which quick red flags in triage session are pressed
     */
    public void flagschecked(boolean chtpull, boolean sen, boolean inretrt, boolean clrnails) {
        ArrayList<String> flagslist=new ArrayList<>();
        if (sen) { flagslist.add("Can't speak full sentences"); }
        if (chtpull) { flagslist.add("Chest Pulling"); }
        if (inretrt) { flagslist.add("In/Retractions"); }
        if (clrnails) { flagslist.add("Blue/Gray lips/Nails"); }
        stringflags = flagslist.toArray(new String[0]);
    }
    /*
    records guidance shown by app
     */
    public void guidanceadd(String choice) {
        guidance[0]=choice;
        guidance[1]=zonecolour;
    }
    /*
    documents what user response is (by checking which buttons have been pressed during triage session)
     */
    public void useresponseadd() {
        ArrayList<String> usereslist=new ArrayList<>();
        // check if any flags have been pressed
        if (checkedsen || checkedpull || checkedretract || checkedbluelips) {usereslist.add("Flags pressed");}
        // put in values
        if (changedPEF) {usereslist.add("Updated PEF");}
        if (recentresattNum!=-1) {usereslist.add("Updated recent rescue attempts");}
        // check what decision card has been pressed
        if (emergcalled) { usereslist.add("Emergency called"); }
        if (homestartpressed) {usereslist.add("Home start steps pressed");}
        userRes=usereslist.toArray(new String[0]);
    }
    /*
    runs timer and documents start of triage session
     */
    public void startTriage() {
        if (triageRunning) {return;}
        if (timertextview == null) {
            Log.e(TAG, "startTriageSession: timertextview is null");
            return;
        }
        triageRunning=true;
        long now=System.currentTimeMillis();
        TriageState.triageendtime=now+TRIAGE_DURATION_MS;
        timervisible=true;
        timertextview.setVisibility(View.VISIBLE);
        showtimer();
        startTriageBtn.setEnabled(false);
        endTriageBtn.setEnabled(true);
    }
    /*
    documents end of triage session and ends timer
     */
    public void endTriage() {
        if (!triageRunning) {return;}
        if (timertextview == null) {
            Log.e(TAG, "endTriageSession: timertextview is null");
            return;
        }
        triageRunning=false;
        if (timertask!=null) {
            timertask.cancel();
            timertask=null;
        }
        TriageState.triageendtime=System.currentTimeMillis();
        timertextview.setText("00:00:00");
        timervisible=false;
        timertextview.setVisibility(View.GONE);
        startTriageBtn.setEnabled(true);
        endTriageBtn.setEnabled(false);
    }
    /*
    documents triage session information into Firebase database
     */
    public void savetriagesession() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user==null) { return; }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> tridata = new HashMap<>();
        if (stringflags != null) { tridata.put("flagList", Arrays.asList(stringflags)); }
        else { tridata.put("flagList",null); }

        tridata.put("PEF",optTriPEF);

        if (recentresattNum!=-1) { tridata.put("rescueAttempts",recentresattNum); }
        else { tridata.put("rescueAttempts",0); }

        tridata.put("date", new Timestamp(new Date()));

        if (guidance != null) { tridata.put("guidance", Arrays.asList(guidance)); }
        else { tridata.put("guidance",null); }

        if (user!=null) { uid=user.getUid(); }
        else{ uid=null; }

        tridata.put("user", uid);

        if (userRes != null) { tridata.put("userRes", Arrays.asList(userRes)); }
        else { tridata.put("userRes",null); }

        db.collection("incidentLog")
                .document(uid)
                .set(new HashMap<>())
                .addOnSuccessListener(aVoid -> {
                    db.collection("incidentLog")
                            .document(uid)
                            .collection("triageSessions")
                            .add(tridata)
                            .addOnSuccessListener(docRef -> {
                                Log.d(TAG, "Triage session saved: " + docRef.getId());
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error writing triage session", e);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating parent incidentLog doc", e);
                });
    }
    /*
    get the child's name
     */
    public void getchildname(String childUid, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String childName = doc.getString("name");
                        onSuccess.onSuccess(childName);
                    }
                    else {
                        onFailure.onFailure(new Exception("Child document not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /*
    used to send notifications to parent
     */
    public void sendtriageAlert() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String cUid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        getchildname(cUid, childName -> {
            db.collection("users")
                    .document(cUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Log.e("Triage", "Child user document missing");
                            return;
                        }
                        List<String> parentUids = (List<String>) doc.get("parentUid");
                        if (parentUids == null || parentUids.isEmpty()) {
                            Log.e("Triage", "No parentUid array found");
                            return;
                        }
                        NotificationRepository notifRepo = new NotificationRepository();
                        for (String pUid : parentUids) {
                            if (pUid == null){ continue; }
                            Notification notif = new Notification(cUid, false, Timestamp.now(), NotifType.TRIAGE, childName);
                            notifRepo.createNotification(pUid, notif)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d("NotificationRepo", "Notification created for parent " + pUid))
                                    .addOnFailureListener(e ->
                                            Log.e("NotificationRepo", "Failed to notify parent " + pUid, e));
                        }

                    })
                    .addOnFailureListener(e ->
                            Log.e("Triage", "Failed to load child document", e)
                    );

        }, error -> {
            Log.e("Triage", "Failed to fetch child name", error);
        });
    }
    /*
    sets up page for parents to make action plan
     */
    public void parentactionsession(View view) {

        MaterialButton btnGreen  = view.findViewById(R.id.btnZoneGreen);
        MaterialButton btnYellow = view.findViewById(R.id.btnZoneYellow);
        MaterialButton btnRed    = view.findViewById(R.id.btnZoneRed);
        MaterialButton btnAddStep   = view.findViewById(R.id.btnAddStep);
        LinearLayout stepsContainer = view.findViewById(R.id.stepsContainer);

        // toggle zone
        selectedZone = "greenZone";
        updatezonebutton(btnGreen, btnYellow, btnRed); //update selected zone button look

        View.OnClickListener zoneClick = v -> {
            if (v == btnGreen) { selectedZone = "greenZone"; }
            else if (v == btnYellow) { selectedZone = "yellowZone"; }
            else if (v == btnRed) { selectedZone = "redZone"; }
            // recolor buttons
            updatezonebutton(btnGreen, btnYellow, btnRed);
            // load steps for this zone if a child is already selected
            if (selectedChildUid != null) {
                loadstepsforchildzone(selectedChildUid, selectedZone, stepsContainer);
            }
        };
        btnGreen.setOnClickListener(zoneClick);
        btnYellow.setOnClickListener(zoneClick);
        btnRed.setOnClickListener(zoneClick);
        // shared viewmodal
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        sharedModel.getAllChildren().observe(getViewLifecycleOwner(), children -> { // set up initial child
            if (children != null && !children.isEmpty()) {
                int currentIndex = sharedModel.getCurrentChild().getValue() != null
                        ? sharedModel.getCurrentChild().getValue()
                        : 0;
                String currentChildUid = children.get(currentIndex).getChildUid();
                this.selectedChildUid = currentChildUid;
                currentStepCount = 0;
            }
        });

        sharedModel.getCurrentChild().observe(getViewLifecycleOwner(), currentIndex -> { // update each time child index changed
            List<Child> children = sharedModel.getAllChildren().getValue();
            if (children != null && !children.isEmpty() && currentIndex != null) {
                selectedChildUid = children.get(currentIndex).getChildUid();
                currentStepCount = 0;
                loadstepsforchildzone(selectedChildUid, selectedZone, stepsContainer);
            }
        });

        // add step button
        btnAddStep.setOnClickListener(v -> {
            if (selectedChildUid == null) {
                Toast.makeText(requireContext(),
                        "Select a child first",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            currentStepCount++;
            View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_step_card, stepsContainer, false);
            EditText desc  = card.findViewById(R.id.edtStepDesc);
            Button save    = card.findViewById(R.id.btnSaveStep);
            int stepNumForThisCard = currentStepCount;
            save.setOnClickListener(btn -> {
                String text = desc.getText().toString().trim();
                if (text.isEmpty()) {
                    desc.setError("Description required");
                    return;
                }
                // write to Firestore and then refresh UI
                savesteptofirestore(
                        selectedChildUid,
                        selectedZone,
                        stepNumForThisCard,
                        text,
                        () -> {
                            // reload all steps and remove the edit card
                            loadstepsforchildzone(selectedChildUid, selectedZone, stepsContainer);
                        }
                );
            });

            // show edit card at top
            stepsContainer.addView(card, 0);
        });
    }

    /*
    to change colour of selected zone in parent action plan screen
     */
    public void updatezonebutton(MaterialButton btnGreen, MaterialButton btnYellow, MaterialButton btnRed) {
        // colours of selected/non selected zone type
        int selected = ContextCompat.getColor(requireContext(), R.color.colour_blue);
        int normal = ContextCompat.getColor(requireContext(), android.R.color.darker_gray);
        // set background colours of buttons
        btnGreen.setBackgroundTintList(ColorStateList.valueOf(normal));
        btnYellow.setBackgroundTintList(ColorStateList.valueOf(normal));
        btnRed.setBackgroundTintList(ColorStateList.valueOf(normal));
        // change colour if button selected
        if ("greenZone".equals(selectedZone)) {
            btnGreen.setBackgroundTintList(ColorStateList.valueOf(selected));
        }
        else if ("yellowZone".equals(selectedZone)) {
            btnYellow.setBackgroundTintList(ColorStateList.valueOf(selected));
        }
        else {
            btnRed.setBackgroundTintList(ColorStateList.valueOf(selected));
        }

    }
    public interface StepSaveCallback {
        void onSaved();
    }
    /*
    add step to action plan to Firestore database
     */
    public void savesteptofirestore(String childUid, String zoneCollection, int stepNum, String desc, StepSaveCallback cb) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("desc", desc);
        data.put("stepnum", stepNum);
        db.collection("actionPlan")
                .document(childUid)
                .set(new HashMap<>())
                .addOnSuccessListener(aVoid -> {
                    db.collection("actionPlan")
                            .document(childUid)
                            .collection(zoneCollection)
                            .add(data)
                            .addOnSuccessListener(ref -> {
                                Log.d("ActionPlan", "Saved step " + stepNum);
                                if (cb != null) {
                                    cb.onSaved();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Log.e("ActionPlan", "Failed to save step", e));
                })
                .addOnFailureListener(e ->
                        Log.e("ActionPlan", "Error creating parent actionPlan doc", e));
    }
    /*
    loads steps for child start home steps button
     */
    public void loadstepsforchildzone(String childUid, String zoneCollection, LinearLayout stepsContainer) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        stepsContainer.removeAllViews();
        db.collection("actionPlan")
                .document(childUid)
                .collection(zoneCollection)
                .orderBy("stepnum")
                .get()
                .addOnSuccessListener(snap -> {
                    // remember the highest step number so the parent can continue later
                    int maxStep = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Long stepNumLong = doc.getLong("stepnum");
                        int stepNum = 0;
                        if (stepNumLong != null) {
                            stepNum = stepNumLong.intValue();
                        }
                        if (stepNum > maxStep) {
                            maxStep = stepNum;
                        }
                        String desc = doc.getString("desc");
                        if (desc == null) {
                            desc = "";
                        }

                        String docId = doc.getId();
                        View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_step_saved, stepsContainer, false);
                        TextView txtDesc = card.findViewById(R.id.txtSavedStepDesc);
                        Button btnEdit   = card.findViewById(R.id.btnEditStep);
                        Button btnDelete = card.findViewById(R.id.btnDeleteStep);
                        txtDesc.setText(desc);
                        String finalDesc = desc;
                        btnEdit.setOnClickListener(v ->
                                editstepdialog(childUid, zoneCollection, docId, finalDesc, stepsContainer));

                        btnDelete.setOnClickListener(v ->
                                deleteStep(childUid, zoneCollection, docId, stepsContainer));

                        stepsContainer.addView(card);
                    }

                    // so "next" step continues after the last one
                    currentStepCount = maxStep;
                })
                .addOnFailureListener(e ->
                        Log.e("ActionPlan", "Failed to load steps", e));
    }


    /*
    delete steps function for parents action plan
     */
    public void deleteStep(String childUid, String zoneCollection, String docId, LinearLayout stepsContainer) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("actionPlan")
                .document(childUid)
                .collection(zoneCollection)
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    loadstepsforchildzone(childUid, zoneCollection, stepsContainer);
                });
    }
    /*
    app functionality when parent wants to edit step in action plan
     */
    public void editstepdialog(String childUid, String zoneCollection, String docId, String currentDesc, LinearLayout stepsContainer) {

        EditText input = new EditText(requireContext());
        input.setText(currentDesc);
        input.setSelection(currentDesc.length());
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Edit Step")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newDesc = input.getText().toString().trim();
                    if (newDesc.isEmpty()) {
                        Toast.makeText(requireContext(), "Description cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateStepdesc(childUid, zoneCollection, docId, newDesc, stepsContainer);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    /*
    allows parent to edit step in action plan and save to firebase
    */
    public void updateStepdesc(String childUid, String zoneCollection, String docId, String newDesc, LinearLayout stepsContainer) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("actionPlan")
                .document(childUid)
                .collection(zoneCollection)
                .document(docId)
                .update("desc", newDesc)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ActionPlan", "Step updated");
                    // reload UI so the edited text shows
                    loadstepsforchildzone(childUid, zoneCollection, stepsContainer);
                })
                .addOnFailureListener(e ->
                        Log.e("ActionPlan", "Failed to update step", e));
    }
    public interface LatestZoneCallback {
        void onFound(String zoneColour);
    }

    /*
    obtains current zone colour
     */
    public void getlatestcolour(String childUid, LatestZoneCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("dailyCheckins")
                .document(childUid)
                .collection("entries")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        cb.onFound(null);   // no entries
                        return;
                    }
                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String zone = doc.getString("zoneColour");
                    // optional fallback if earlier docs used triggersparent.zoneColour
                    if (zone == null) {
                        zone = doc.getString("triggersparent.zoneColour");
                    }

                    cb.onFound(zone);
                })
                .addOnFailureListener(e -> {
                    Log.e("zone", "Failed loading latest zone", e);
                    cb.onFound(null);
                });
    }
    /*
    sets start home steps page from xml when clicked in triage session
     */
    public void openstarthomesteps(String childUid, String zoneColour) {
        if (isAdded()==false) { return; }

        View root = getView();

        if (root == null) { return; }

        View triageContent = root.findViewById(R.id.triageContent);
        View childStepsContent = root.findViewById(R.id.childStepsContent);
        triageContent.setVisibility(View.GONE);
        childStepsContent.setVisibility(View.VISIBLE);
        setupstarthomesteps(childStepsContent, childUid, zoneColour);
    }

   /*
   loads start home step screen features
    */
    public void setupstarthomesteps(View childStepsContent, String childUid, String zoneColour) {

        LinearLayout root = childStepsContent.findViewById(R.id.childStepsRoot);
        TextView txtZone  = childStepsContent.findViewById(R.id.txtChildZone);
        LinearLayout steps = childStepsContent.findViewById(R.id.childStepsContainer);
        ImageButton btnBack = childStepsContent.findViewById(R.id.btnBackToTriage);
        // back button action
        btnBack.setOnClickListener(v -> {
            if (isAdded()==false) { return; }

            View rootView = getView();

            if (rootView == null) { return; }

            View triageContent = rootView.findViewById(R.id.triageContent);
            View childContent  = rootView.findViewById(R.id.childStepsContent);
            childContent.setVisibility(View.GONE);
            triageContent.setVisibility(View.VISIBLE);
        });

        if (zoneColour == null) { zoneColour = "green"; }

        txtZone.setText(zoneColour);

        // background colour by zone
        int bg;
        if ("yellow".equals(zoneColour)) { bg = ContextCompat.getColor(requireContext(), R.color.zone_yellow_bg); }
        else if ("red".equals(zoneColour)) { bg = ContextCompat.getColor(requireContext(), R.color.zone_red_bg); }
        else { bg = ContextCompat.getColor(requireContext(), R.color.zone_green_bg); }
        root.setBackgroundColor(bg);
        // load the home steps for this child + zone
        loadchildstepsforzone(childUid, zoneColour, steps);
        // check for red flags in zone and send parent alert
        zoneredflagactions(childStepsContent);
    }

    /*
    loads steps from parent action plan for child based on zone colour
     */
    public void loadchildstepsforzone(String childUid, String zoneColour, LinearLayout container) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String zoneCollection;
        if ("yellow".equals(zoneColour)) { zoneCollection = "yellowZone"; }
        else if ("red".equals(zoneColour)) { zoneCollection = "redZone"; }
        else { zoneCollection = "greenZone"; }
        db.collection("actionPlan")
                .document(childUid)
                .collection(zoneCollection)
                .orderBy("stepnum")
                .get()
                .addOnSuccessListener(snap -> {

                    container.removeAllViews();

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        String desc = doc.getString("desc");
                        if (desc == null) { desc = "";}
                        View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_child_step, container, false);
                        TextView text  = card.findViewById(R.id.txtStepDesc);
                        text.setText(desc);
                        container.addView(card);
                    }
                });
    }
    /*
    sends notifications when user has quick red flags in start home steps screen
     */
    public void zoneredflagactions(View childStepsContent) {

        MaterialButton btnSentence = childStepsContent.findViewById(R.id.btn_flag_sentences);
        MaterialButton btnChest    = childStepsContent.findViewById(R.id.btn_flag_chest);
        MaterialButton btnRetract  = childStepsContent.findViewById(R.id.btn_flag_retractions);
        MaterialButton btnBlue     = childStepsContent.findViewById(R.id.btn_flag_blue);
        btnSentence.setOnClickListener(v -> {
            checkedsen = !checkedsen;
            updateflgbtn(btnSentence, checkedsen);
            if (triageRunning==true&&checkedsen==true) {
                sendtriageAlert();
            }
        });

        btnChest.setOnClickListener(v -> {
            checkedpull = !checkedpull;
            updateflgbtn(btnChest, checkedpull);
            if (triageRunning==true&&checkedpull==true) {
                sendtriageAlert();
            }
        });

        btnRetract.setOnClickListener(v -> {
            checkedretract = !checkedretract;
            updateflgbtn(btnRetract, checkedretract);
            if (triageRunning==true&&checkedretract==true) {
                sendtriageAlert();
            }
        });

        btnBlue.setOnClickListener(v -> {
            checkedbluelips = !checkedbluelips;
            updateflgbtn(btnBlue, checkedbluelips);
            if (triageRunning==true&&checkedbluelips==true) {
                sendtriageAlert();
            }
        });
    }
}