package com.example.smart_air.fragments;

import static androidx.core.content.ContextCompat.getSystemService;

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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
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
    String [] guidance;
    String [] userRes;
    MaterialButton startTriageBtn;
    MaterialButton endTriageBtn;
    boolean triageRunning = false;
    static final long TRIAGE_DURATION_MS =10*60*1000;


    public TriageFragment() {
        // Required empty public constructor
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

        checktimerbutton=view.findViewById(R.id.timer);
        timertextview=view.findViewById(R.id.timerTextV);
        timer = new Timer();
        timervisible = true;
        timertextview.setVisibility(View.VISIBLE);
        showtimer();
        //ensureNotificationPermission();
        //createNotifChannel(); // makes notification channel
        checktimerbutton.setOnClickListener(v -> toggletimerdisplay());
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
        endTriageBtn.setOnClickListener(v -> endTriageSession());

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
                String txt = s.toString();
                if (!txt.isEmpty()) {
                    recentresattNum = Integer.parseInt(txt);
                }
                else {
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
                String txt = s.toString();
                if (!txt.isEmpty()) {
                    optTriPEF = Integer.parseInt(txt);
                }
                else {
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
        if (checked==true) {
            btn.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.checkbox));

            btn.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), android.R.color.white)
            ));
        } else {
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
        if (chtpull==true)
        {
            flagslist.add("Can't speak full sentences");
        }
        if (sen==true)
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

    }
    private void startTriageSession() {
        if (triageRunning==true)
        {
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

    private void endTriageSession() {
        if (triageRunning==false)
        {
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
    /*public void parentalertnotif() {
        NotificationCompat.Builder mbuilder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setSmallIcon(R.drawable.parentalert)
                .setContentTitle("Parent Alert")
                .setContentText("A triage session has started!")
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, mbuilder.build());
    }
    public void createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SmartAir Notifications";
            String description = "General notifications for SmartAir app";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private static final int REQ_POST_NOTIFS = 1001;

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFS
                );
            }
        }
    }*/

}


