package com.example.smart_air;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.cardview.widget.CardView;

import com.example.smart_air.Repository.CheckInRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.annotation.Nullable;

public class CheckInPageActivity extends Activity {
    String userRole = "";
    String correspondingUid;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkin_page);

        CheckInRepository repo = new CheckInRepository();
        repo.getUserInfo(this);
        repo.getUserInput(this);


        // fixing bottom navigation
        navigationBar(R.id.checkin);

        // setting date
        TextView textView3 = findViewById(R.id.textView3);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy");
        String date = formatDate.format(calendar.getTime());
        textView3.setText(date);

        // fixing input things for coughing/wheezing
        fixingCoughingUI();

        // setting up multiselect option
        TextView multiSelectTriggers = findViewById(R.id.multiSelect);          // setting up triggers
        String [] triggers = {"Allergies", "Smoke","Flu","Strong smells", "Running"};
        boolean [] selectedTriggers = new boolean[triggers.length];
        multiSelectTriggers.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Select Symptoms")
                    .setMultiChoiceItems(triggers, selectedTriggers, (dialog, which, isChecked) -> {
                        selectedTriggers[which] = isChecked;
                    })
                    .setPositiveButton("OK", (dialog, which) -> {
                        StringBuilder selected = new StringBuilder();
                        for (int i = 0; i < triggers.length; i++) {
                            if (selectedTriggers[i]) selected.append(triggers[i]).append(", ");
                        }
                        if (selected.length() > 0) {
                            selected.setLength(selected.length() - 2); // remove trailing comma
                            multiSelectTriggers.setText(selected.toString());
                        } else {
                            multiSelectTriggers.setText("Tap to select");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // clicking save button and adding to firestore
        MaterialButton save = findViewById(R.id.buttonSave);
        save.setOnClickListener(v-> {
            RadioGroup radioNight = findViewById(R.id.radioNight); // night waking
            RadioButton radioYes = findViewById(R.id.radioYes);    // night waking
            boolean nightWaking = (radioNight.getCheckedRadioButtonId() == R.id.radioYes);
            SeekBar seekBar = findViewById(R.id.seekBar);          // activity limit
            int activityValue = (int)seekBar.getProgress();
            Slider slider = findViewById(R.id.sliderCough);                         // coughing/wheezing
            int coughingValue = (int)slider.getValue();
            repo.saveUserData(this, userRole, triggers, selectedTriggers, correspondingUid, nightWaking, activityValue, coughingValue);
        });


        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleRole);  // which form
        MaterialButton buttonParent = findViewById(R.id.buttonParent);          // which form
        MaterialButton buttonChild = findViewById(R.id.buttonChild);            // which form

        // switch entry page
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (checkedId == R.id.buttonParent && isChecked){
                buttonChild.setBackgroundColor(getColor(R.color.role_default_bg));
                buttonParent.setBackgroundColor(getColor(R.color.role_selected_bg));
                if(userRole.equals("child")){
                    setCardOther();
                }
                else if(userRole.equals("parent")){
                    repo.getUserInput(this);
                }
            }
            else if (checkedId == R.id.buttonChild && isChecked){
                buttonParent.setBackgroundColor(getColor(R.color.role_default_bg));
                buttonChild.setBackgroundColor(getColor(R.color.role_selected_bg));
                if(userRole.equals("child")){
                    repo.getUserInput(this);

                }
                else if(userRole.equals("parent")){
                    setCardOther();
                }
            }
        });


    }

    public void updateInfoInput(Boolean nightWaking, Long activityLimits, Long coughingWheezing, List<String> triggers) {
        CardView nightWakingCard = findViewById(R.id.nightCard);
        CardView activityLimitsCard = findViewById(R.id.activity);
        CardView coughWheezeCard = findViewById(R.id.coughing);
        CardView triggersCard = findViewById(R.id.triggers);

        nightWakingCard.setVisibility(View.VISIBLE);
        activityLimitsCard.setVisibility(View.VISIBLE);
        coughWheezeCard.setVisibility(View.VISIBLE);
        triggersCard.setVisibility(View.VISIBLE);

        SeekBar seekbar = findViewById(R.id.seekBar);
        seekbar.setProgress(Math.toIntExact(activityLimits));

        Slider slider = findViewById(R.id.sliderCough);
        slider.setValue((float) coughingWheezing);

        RadioGroup radioNight = findViewById(R.id.radioNight); // night waking
        RadioButton radioYes = findViewById(R.id.radioYes);    // night waking
        RadioButton radioNo = findViewById(R.id.radioNo);    // night waking
        if(nightWaking == true){
            radioNight.check(R.id.radioYes);
        }
        else{
            radioNight.check(R.id.radioNo);
        }


    }

    public void updateInfoInputWithoutValues() {
        CardView nightWakingCard = findViewById(R.id.nightCard);
        CardView activityLimitsCard = findViewById(R.id.activity);
        CardView coughWheezeCard = findViewById(R.id.coughing);
        CardView triggersCard = findViewById(R.id.triggers);

        nightWakingCard.setVisibility(View.VISIBLE);
        activityLimitsCard.setVisibility(View.VISIBLE);
        coughWheezeCard.setVisibility(View.VISIBLE);
        triggersCard.setVisibility(View.VISIBLE);


    }

    /**
     * making cards appear for other based on toggle
     */
    public void setCardOther(){
        CardView nightWakingCard = findViewById(R.id.nightCard);
        CardView activityLimitsCard = findViewById(R.id.activity);
        CardView coughWheezeCard = findViewById(R.id.coughing);
        CardView triggersCard = findViewById(R.id.triggers);

        nightWakingCard.setVisibility(View.INVISIBLE);
        activityLimitsCard.setVisibility(View.INVISIBLE);
        coughWheezeCard.setVisibility(View.INVISIBLE);
        triggersCard.setVisibility(View.INVISIBLE);
    }

    /**
     * fixing the ui of coughing/wheezing
     */
    private void fixingCoughingUI() {
        // fixing text based on bar on coughing/wheezing card
        Slider slider = findViewById(R.id.sliderCough);
        TextView label = findViewById(R.id.sliderLabel);
        slider.addOnChangeListener((s, value, fromUser) -> {
            if ((int) value == 0) {
                label.setText("No Coughing");
            } else if ((int) value == 1) {
                label.setText("Wheezing");
            }
            else if((int)value == 2){
                label.setText("Coughing");
            }
            else {
                label.setText("Extreme Coughing");
            }
        });
    }

    /**
     * a method called when an error occurs, goes back the main activity
     */
    public void noUserFound(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /**
     * a method that loads info about the user into variables
     * @param role, stores what type of user it is
     * @param correspondingUid, stores it's corresponding child or user
     */
    public void userInfoLoaded(String role, String correspondingUid){
        userRole = role;
        this.correspondingUid = correspondingUid;
        updateUIBasedOnRole(role);
    }

    /**
     * Fixes the navigation bar and gets it working
     * @param currentPage, an R.id  on what the current page is
     */
    private void navigationBar(int currentPage){
        // getting bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.checkin);

        // setting bottom navigation controls
        bottomNavigationView.setOnItemSelectedListener(page -> {
            int id = page.getItemId();

            if (id == R.id.home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.triage) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.history) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.medicine) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.checkin){
                Intent intent = new Intent(this, CheckInPageActivity.class);
                startActivity(intent);
                return true;
            }
            else {
                return false;
            }
        });
    }

    /**
     * changes the text prompts based on type of user
     * @param userRole is the current type of user
     */
    public void updateUIBasedOnRole(String userRole ){
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleRole);  // which form
        MaterialButton buttonParent = findViewById(R.id.buttonParent);          // which form
        MaterialButton buttonChild = findViewById(R.id.buttonChild);            // which form

        // fix ui based on role
        TextView nightPrompt = findViewById(R.id.textView5);
        TextView activityPrompt = findViewById(R.id.textView6);
        TextView coughingPrompt = findViewById(R.id.textView67);
        if (userRole.equals("child")){
            toggleGroup.check(R.id.buttonChild);
            buttonParent.setBackgroundColor(getColor(R.color.role_default_bg));
            buttonChild.setBackgroundColor(getColor(R.color.role_selected_bg));
            nightPrompt.setText("Did you experience any night walking last night?");
            activityPrompt.setText("How limited was your activity level today?");
            coughingPrompt.setText("How often we’re you coughing or wheezing today?");

        }
        else if(userRole.equals("parent")){
            toggleGroup.check(R.id.buttonParent);
            buttonChild.setBackgroundColor(getColor(R.color.role_default_bg));
            buttonParent.setBackgroundColor(getColor(R.color.role_selected_bg));
            nightPrompt.setText("Did your child experience any night walking last night?");
            activityPrompt.setText("How limited was your child's activity level today?");
            coughingPrompt.setText("How often was your child coughing or wheezing today?");
        }

    }
}