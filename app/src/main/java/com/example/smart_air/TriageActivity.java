package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smart_air.Repository.AuthRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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
    int recentresattNum=0;
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
}

