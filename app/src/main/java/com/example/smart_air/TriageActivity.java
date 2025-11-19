package com.example.smart_air;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smart_air.Repository.AuthRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.NotificationManager;

public class TriageActivity extends AppCompatActivity{
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


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.triage_page);
        checktimerbutton=findViewById(R.id.timer);
        timertextview=findViewById(R.id.timerTextV);
        timer = new Timer();
        timervisible = true;
        timertextview.setVisibility(View.VISIBLE);
        showtimer();
        checktimerbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggletimerdisplay();
            }
        });
        // home start steps click
        homestart=findViewById((R.id.button4));
        homestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homestartpressed=true;
            }
        });
        // emergency call click
        emergcall=findViewById(R.id.button3);
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
        rraText=findViewById(R.id.textrra);
        pefTriText=findViewById(R.id.textpef);

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
        flgsentences=findViewById(R.id.btn_flag_sentences);
        flgsentences.setOnClickListener(v ->
        {
            checkedsen=!checkedsen;
            updateflgbtn(flgsentences,checkedsen);
        });
        flgchestpull=findViewById(R.id.btn_flag_chest);
        flgchestpull.setOnClickListener(v ->
        {
            checkedpull=!checkedpull;
            updateflgbtn(flgchestpull,checkedpull);
        });
        flgretract=findViewById(R.id.btn_flag_retractions);
        flgretract.setOnClickListener(v ->
        {
            checkedretract=!checkedretract;
            updateflgbtn(flgretract,checkedretract);
        });
        flgBluelips=findViewById(R.id.btn_flag_blue);
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
        long diff=TriageState.triageendtime-now;
        if (diff<=0)
        {
            timertextview.setText("00:00:00");
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
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
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
    public void destorytimer()
    {
        super.onDestroy();
        if (timertask!=null)
        {
            timertask.cancel();
        }
        if (timer!=null)
        {
            timer.cancel();
        }
    }
    public void updateflgbtn(MaterialButton btn, boolean checked) {
        if (checked==true) {
            btn.setIcon(ContextCompat.getDrawable(TriageActivity.this, R.drawable.checkbox));
            btn.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(TriageActivity.this, android.R.color.white)));
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


}






