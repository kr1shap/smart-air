package com.example.smart_air.fragments;

import static android.content.ContentValues.TAG;
import static androidx.core.content.ContextCompat.getSystemService;
import com.google.firebase.Timestamp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;

public class TriageFragment extends Fragment {

    static final String CHANNEL_ID ="smartairnotif";
    private View view;

    ImageButton checktimerbutton;
    TextView timertextview;
    Double time=0.0;
    boolean timervisible=false;
    Timer timer;
    TimerTask timertask;
    EditText rraText;
    int recentresattNum=-1;
    EditText pefTriText;
    int optTriPEF=-1;
    MaterialButton flgsentences;
    boolean checkedsen;
    MaterialButton flgchestpull;
    boolean checkedpull;
    MaterialButton flgretract;
    boolean checkedretract;
    MaterialButton flgBluelips;
    boolean checkedbluelips;
    Button emergcall;
    boolean emergcalled=false;
    Button homestart;
    boolean homestartpressed=false;
    String decisioncardchoice;
    String[] stringflags;
    String [] guidance=new String[2];
    String [] userRes;

    MaterialButton startTriageBtn;
    MaterialButton endTriageBtn;
    boolean triageRunning = false;
    static final long TRIAGE_DURATION_MS =10*60*1000;
    String uid;

    public TriageFragment() {
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.triage_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        View triageContent = view.findViewById(R.id.triageContent);
        View noAccessMessage = view.findViewById(R.id.noAccessMessage);

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }

                    String role = doc.getString("role");

                    if (!"child".equals(role)) {
                        triageContent.setVisibility(View.GONE);
                        noAccessMessage.setVisibility(View.VISIBLE);      // 👈 updated
                        return;
                    }

                    triageContent.setVisibility(View.VISIBLE);
                    noAccessMessage.setVisibility(View.GONE);              // 👈 updated
                    triagesession(view);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error checking role", e);
                });
    }



    private void triagesession(View view)
    {
        //timer setup
        checktimerbutton=view.findViewById(R.id.timer);
        timertextview=view.findViewById(R.id.timerTextV);
        timer = new Timer();
        timervisible = true;
        timertextview.setVisibility(View.VISIBLE);
        showtimer();
        //ensureNotificationPermission();
        //createNotifChannel(); // makes notification channel
        checktimerbutton.setOnClickListener(v -> toggletimerdisplay());
        // set up triage session buttons
        startTriageBtn=view.findViewById(R.id.startriage);
        endTriageBtn=view.findViewById(R.id.endtriage);
        triageRunning=false;
        startTriageBtn.setEnabled(true);
        endTriageBtn.setEnabled(false);
        startTriageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTriageSession();
                //parentalertnotif();
            }
        });
        endTriageBtn.setOnClickListener(v -> {
            flagschecked(checkedpull,checkedsen,checkedretract,checkedbluelips);
            useresponseadd();
            decisionpressed(emergcalled,homestartpressed);
            guidanceadd(decisioncardchoice,"Red"); //dummy red
            savetriagesession();
            endTriageSession();
        });
        checktimerbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggletimerdisplay();
            }
        });
        // home start steps click
        homestart=view.findViewById((R.id.button4));
        homestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homestartpressed=true;
            }
        });
        // emergency call click
        emergcall=view.findViewById(R.id.button3);
        emergcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emergcalled=true;
                Intent intentcall = new Intent(Intent.ACTION_DIAL);
                intentcall.setData(Uri.parse("tel:6767676767")); //change to 911
                startActivity(intentcall);
            }
        });
        // PEF and recent rescue attempts
        rraText=view.findViewById(R.id.textrra);
        pefTriText=view.findViewById(R.id.textpef);

        rraText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }
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
                else
                {
                    recentresattNum = -1;
                }
            }
            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        pefTriText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String txt = s.toString().trim();
                if (txt.isEmpty()==false) {
                    try
                    {
                        optTriPEF = Integer.parseInt(txt);
                    }
                    catch (NumberFormatException e)
                    {
                        recentresattNum = -1;
                        Log.e(TAG, "Invalid number in recent rescue attempts: " + txt, e);
                    }
                }
                else
                {
                    optTriPEF = -1;
                }
            }
            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });
        // quick red flag checks
        flgsentences=view.findViewById(R.id.btn_flag_sentences);
        flgsentences.setOnClickListener(v ->
        {
            checkedsen=!checkedsen;
            updateflgbtn(flgsentences,checkedsen);
        });
        flgchestpull=view.findViewById(R.id.btn_flag_chest);
        flgchestpull.setOnClickListener(v ->
        {
            checkedpull=!checkedpull;
            updateflgbtn(flgchestpull,checkedpull);
        });
        flgretract=view.findViewById(R.id.btn_flag_retractions);
        flgretract.setOnClickListener(v ->
        {
            checkedretract=!checkedretract;
            updateflgbtn(flgretract,checkedretract);
        });
        flgBluelips=view.findViewById(R.id.btn_flag_blue);
        flgBluelips.setOnClickListener(v ->
        {
            checkedbluelips=!checkedbluelips;
            updateflgbtn(flgBluelips,checkedbluelips);
        });

    }

    private void toggletimerdisplay()
    {
        if (timervisible==true)
        {
            timervisible=false;
            timertextview.setVisibility(View.GONE);
            if (timertask!=null)
            {
                timertask.cancel();
            }
        }
        else
        {
            timervisible=true;
            timertextview.setVisibility(View.VISIBLE);
            showtimer();
        }
    }
    public void showtimer()
    {
        if (isAdded()==false)
        {
            return;
        }
        long now=System.currentTimeMillis();
        long diff= TriageState.triageendtime-now;
        if (diff<=0)
        {
            if (timertextview!=null)
            {
                timertextview.setText("00:00:00");
            }
            return;
        }
        if (timertask!=null)
        {
            timertask.cancel();
        }
        time=diff/1000.0;
        timertask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (isAdded()==false)
                {
                    return;
                }
                requireActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (timertextview==null)
                        {
                            return;
                        }
                        if (time<=0)
                        {
                            timertextview.setText("00:00:00");
                            timertask.cancel();
                            return;
                        }
                        time--;
                        timertextview.setText(getTimerText());
                    }
                });
            }
        };
        if (timer==null)
        {
            timer = new Timer();
        }
        timer.scheduleAtFixedRate(timertask,0,1000);
    }
    public String getTimerText()
    {
        int round=(int)Math.round(time);
        int sec=((round%86400)%3600)%60;
        int min=((round%86400)%3600)/60;
        int hrs=((round%86400)/3600);
        return formatTime(sec,min,hrs);
    }
    public String formatTime(int sec, int min, int hrs)
    {
        return String.format("%02d",hrs)+":"+String.format("%02d",min)+":"+String.format("%02d",sec);
    }
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

    public void updateflgbtn(MaterialButton btn, boolean checked) {
        if (checked==true)
        {
            btn.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.checkbox));
            btn.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.white)));
        }
        else
        {
            btn.setIcon(null);
        }
    }
    public void decisionpressed(boolean ecall, boolean hsteps)
    {
        if (ecall==false&&hsteps==false)
        {
            decisioncardchoice="None";
        }
        else if (ecall==true&&hsteps==false)
        {
            decisioncardchoice="Call Emergency Now button clicked";
        }
        else if (ecall==false&&hsteps==true)
        {
            decisioncardchoice="Start Home Steps button clicked";
        }
        else
        {
            decisioncardchoice="Call Emergency Now and Start Home Steps buttons clicked";
        }
    }
    public void flagschecked(boolean chtpull, boolean sen, boolean inretrt, boolean clrnails)
    {
        ArrayList<String> flagslist=new ArrayList<>();
        if (sen==true)
        {
            flagslist.add("Can't speak full sentences");
        }
        if (chtpull==true)
        {
            flagslist.add("Chest Pulling");
        }
        if (inretrt==true)
        {
            flagslist.add("In/Retractions");
        }
        if (clrnails==true)
        {
            flagslist.add("Blue/Gray lips/Nails");
        }
        stringflags = flagslist.toArray(new String[0]);
    }
    public void guidanceadd(String choice, String zone)
    {
        guidance[0]=choice;
        guidance[1]=zone;//
    }
    public void useresponseadd()
    {
        ArrayList<String> usereslist=new ArrayList<>();
        // check if any flags have been pressed
        if (checkedsen==true||checkedpull==true||checkedretract==true||checkedbluelips==true)
        {
            usereslist.add("Flags pressed");
        }
        // put in values
        if (optTriPEF!=-1)
        {
            usereslist.add("Updated PEF");
        }
        if (recentresattNum!=-1)
        {
            usereslist.add("Updated recent rescue attempts");
        }
        // check what decision card has been pressed
        if (emergcalled==true)
        {
            usereslist.add("Emergency called");
        }
        if (homestartpressed==true)
        {
            usereslist.add("Home start steps pressed");
        }
        userRes=usereslist.toArray(new String[0]);
    }
    public void startTriageSession() {
        if (triageRunning==true)
        {
            return;
        }
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

    public void endTriageSession() {
        if (triageRunning==false)
        {
            return;
        }
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
    public void savetriagesession()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user==null)
        {
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> tridata = new HashMap<>();
        if (stringflags != null)
        {
            tridata.put("flagList", Arrays.asList(stringflags));
        }
        else
        {
            tridata.put("flagList",null);
        }
        // do not need this if, uses old pef when it has
        if (optTriPEF!=-1)
        {
            tridata.put("PEF",optTriPEF);
        }
        else
        {
            tridata.put("PEF",null);
        }
        if (recentresattNum!=-1)
        {
            tridata.put("rescueAttempts",recentresattNum);
        }
        else
        {
            tridata.put("rescueAttempts",0);
        }
        tridata.put("date", new Timestamp(new Date()));
        if (guidance != null)
        {
            tridata.put("guidance", Arrays.asList(guidance));
        }
        else
        {
            tridata.put("guidance",null);
        }
        if (user!=null)
        {
            uid=user.getUid();
        }
        else{
            uid=null;
        }
        tridata.put("user", uid);
        if (userRes != null)
        {
            tridata.put("userRes", Arrays.asList(userRes));
        }
        else
        {
            tridata.put("userRes",null);
        }

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
    }



