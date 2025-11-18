package com.example.smart_air.fragments;

import static androidx.core.app.NotificationCompat.getColor;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smart_air.CheckInPageActivity;
import com.example.smart_air.MainActivity;
import com.example.smart_air.R;
import com.example.smart_air.Repository.CheckInRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Nullable;

public class CheckInFragment extends Fragment {
    private View view;
    String userRole = "";
    String correspondingUid;
    String currentTriggers = "Tap to Select";
    String [] triggers = {"Allergies", "Smoke","Flu","Strong smells", "Running", "Exercise", "Cold Air", "Dust/Pets", "Illness"};
    @Nullable

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.checkin_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;

        CheckInRepository repo = new CheckInRepository();
        repo.getUserInfo(this);
        repo.getUserInput(this);


        // fixing bottom navigation
        //navigationBar(R.id.checkin);

        // setting date
        TextView textView3 = view.findViewById(R.id.textView3);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy");
        String date = formatDate.format(calendar.getTime());
        textView3.setText(date);

        // fixing input things for coughing/wheezing
        fixingCoughingUI();

        // setting up multiselect option
        boolean [] selectedTriggers = setUpTriggers(currentTriggers);

        // clicking save button and adding to firestore
        MaterialButton save = view.findViewById(R.id.buttonSave);
        save.setOnClickListener(v-> {
            RadioGroup radioNight = view.findViewById(R.id.radioNight); // night waking
            RadioButton radioYes = view.findViewById(R.id.radioYes);    // night waking
            boolean nightWaking = (radioNight.getCheckedRadioButtonId() == R.id.radioYes);
            SeekBar seekBar = view.findViewById(R.id.seekBar);          // activity limit
            int activityValue = (int)seekBar.getProgress();
            Slider slider = view.findViewById(R.id.sliderCough);                         // coughing/wheezing
            int coughingValue = (int)slider.getValue();
            repo.saveUserData(this, userRole, triggers, selectedTriggers, correspondingUid, nightWaking, activityValue, coughingValue);
        });


        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleRole);  // which form
        MaterialButton buttonParent = view.findViewById(R.id.buttonParent);          // which form
        MaterialButton buttonChild = view.findViewById(R.id.buttonChild);            // which form

        // switch entry page
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (checkedId == R.id.buttonParent && isChecked){
                buttonChild.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_default_bg));
                buttonParent.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_selected_bg));
                if(userRole.equals("child")){
                    repo.getUserInputOther(this,correspondingUid);
                }
                else if(userRole.equals("parent")){
                    repo.getUserInput(this);
                }
            }
            else if (checkedId == R.id.buttonChild && isChecked){
                buttonParent.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_default_bg));
                buttonChild.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_selected_bg));
                if(userRole.equals("child")){
                    repo.getUserInput(this);

                }
                else if(userRole.equals("parent")){
                    repo.getUserInputOther(this,correspondingUid);
                }
            }
        });
    }

    public void updateInfoInput(Boolean nightWaking, Long activityLimits, Long coughingWheezing, List<String> selection) {
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);

        nightWakingCard.setVisibility(View.VISIBLE);
        activityLimitsCard.setVisibility(View.VISIBLE);
        coughWheezeCard.setVisibility(View.VISIBLE);
        triggersCard.setVisibility(View.VISIBLE);

        SeekBar seekbar = view.findViewById(R.id.seekBar);
        seekbar.setProgress(Math.toIntExact(activityLimits));

        Slider slider = view.findViewById(R.id.sliderCough);
        slider.setValue((float) coughingWheezing);

        RadioGroup radioNight = view.findViewById(R.id.radioNight); // night waking
        if(nightWaking == true){
            radioNight.check(R.id.radioYes);
        }
        else{
            radioNight.check(R.id.radioNo);
        }

        currentTriggers = String.join(", ", selection);
        TextView multiSelectTriggers = view.findViewById(R.id.multiSelect);
        multiSelectTriggers.setText(currentTriggers);
        setCardCurrent();

    }

    public void updateInfoInputWithoutValues() {
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);

        nightWakingCard.setVisibility(View.VISIBLE);
        activityLimitsCard.setVisibility(View.VISIBLE);
        coughWheezeCard.setVisibility(View.VISIBLE);
        triggersCard.setVisibility(View.VISIBLE);

        // set default values
        SeekBar seekbar = view.findViewById(R.id.seekBar);
        seekbar.setProgress(Math.toIntExact(5));

        Slider slider = view.findViewById(R.id.sliderCough);
        slider.setValue(0);

        RadioGroup radioNight = view.findViewById(R.id.radioNight); // night waking
        radioNight.clearCheck();

        TextView multiSelectTriggers = view.findViewById(R.id.multiSelect);
        currentTriggers = "Tap to Select";
        multiSelectTriggers.setText(currentTriggers);

        setCardCurrent();


    }

    public void updateInfoInputOther(Boolean nightWaking, Long activityLimits, Long coughingWheezing, List<String> selection) {
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);

        nightWakingCard.setVisibility(View.VISIBLE);
        activityLimitsCard.setVisibility(View.VISIBLE);
        coughWheezeCard.setVisibility(View.VISIBLE);
        triggersCard.setVisibility(View.VISIBLE);

        SeekBar seekbar = view.findViewById(R.id.seekBar);
        seekbar.setProgress(Math.toIntExact(activityLimits));

        Slider slider = view.findViewById(R.id.sliderCough);
        slider.setValue((float) coughingWheezing);

        RadioGroup radioNight = view.findViewById(R.id.radioNight); // night waking
        if(nightWaking == true){
            radioNight.check(R.id.radioYes);
        }
        else{
            radioNight.check(R.id.radioNo);
        }

        currentTriggers = String.join(", ", selection);
        TextView multiSelectTriggers = view.findViewById(R.id.multiSelect);
        multiSelectTriggers.setText(currentTriggers);


        setCardOther();

    }

    public void updateInfoInputOtherWithoutValues() {
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);

        nightWakingCard.setVisibility(View.INVISIBLE);
        activityLimitsCard.setVisibility(View.INVISIBLE);
        coughWheezeCard.setVisibility(View.INVISIBLE);
        triggersCard.setVisibility(View.INVISIBLE);

        MaterialButton save = view.findViewById(R.id.buttonSave);
        save.setEnabled(false);
        TextView noInformation = view.findViewById(R.id.textView7);
        if(userRole.equals("child")){
            noInformation.setText("Parent hasn't entered information for the day.");
        }
        else{
            noInformation.setText("Child hasn't entered information for the day.");
        }
        noInformation.setVisibility(View.VISIBLE);

    }

    public void setCardCurrent(){
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);

        nightWakingCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        activityLimitsCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        coughWheezeCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        triggersCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        SeekBar seekbar = view.findViewById(R.id.seekBar);
        seekbar.setEnabled(true);
        Slider slider = view.findViewById(R.id.sliderCough);
        slider.setEnabled(true);
        RadioGroup radioNight = view.findViewById(R.id.radioNight); // night waking
        RadioButton radioYes = view.findViewById(R.id.radioYes);    // night waking
        RadioButton radioNo = view.findViewById(R.id.radioNo);    // night waking
        radioNight.setEnabled(true);
        radioYes.setEnabled(true);
        radioNo.setEnabled(true);

        MaterialButton save = view.findViewById(R.id.buttonSave);
        save.setEnabled(true);

        TextView multiSelectView = view.findViewById(R.id.multiSelect);
        multiSelectView.setEnabled(true);

        TextView noInformation = view.findViewById(R.id.textView7);
        noInformation.setVisibility(View.INVISIBLE);

        updateUIBasedOnRole(userRole);
    }
    /**
     * making cards appear for other based on toggle
     */
    public void setCardOther(){
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);

        nightWakingCard.setVisibility(View.VISIBLE);
        activityLimitsCard.setVisibility(View.VISIBLE);
        coughWheezeCard.setVisibility(View.VISIBLE);
        triggersCard.setVisibility(View.VISIBLE);

        nightWakingCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colour_grey));
        activityLimitsCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colour_grey));
        coughWheezeCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colour_grey));
        triggersCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colour_grey));

        SeekBar seekbar = view.findViewById(R.id.seekBar);
        seekbar.setEnabled(false);
        Slider slider = view.findViewById(R.id.sliderCough);
        slider.setEnabled(false);
        RadioGroup radioNight = view.findViewById(R.id.radioNight); // night waking
        RadioButton radioYes = view.findViewById(R.id.radioYes);    // night waking
        RadioButton radioNo = view.findViewById(R.id.radioNo);    // night waking
        radioNight.setEnabled(false);
        radioYes.setEnabled(false);
        radioNo.setEnabled(false);

        MaterialButton save = view.findViewById(R.id.buttonSave);
        save.setEnabled(false);

        TextView multiSelectView = view.findViewById(R.id.multiSelect);
        multiSelectView.setEnabled(false);

        if(userRole.equals("child")){
            updateUIBasedOnRole("parent");
        }
        else{
            updateUIBasedOnRole("child");
        }

        TextView noInformation = view.findViewById(R.id.textView7);
        noInformation.setVisibility(View.INVISIBLE);
    }

    /**
     * fixing the ui of coughing/wheezing
     */
    private void fixingCoughingUI() {
        // fixing text based on bar on coughing/wheezing card
        Slider slider = view.findViewById(R.id.sliderCough);
        TextView label = view.findViewById(R.id.sliderLabel);
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
        //Intent intent = new Intent(this, MainActivity.class);
        //startActivity(intent);
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

//    /**
//     * Fixes the navigation bar and gets it working
//     * @param currentPage, an R.id  on what the current page is
//     */
//    private void navigationBar(int currentPage){
//        // getting bottom navigation
//        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottom_navigation);
//        bottomNavigationView.setSelectedItemId(R.id.checkin);
//
//        // setting bottom navigation controls
//        bottomNavigationView.setOnItemSelectedListener(page -> {
//            int id = page.getItemId();
//
//            if (id == R.id.home) {
//                Intent intent = new Intent(this, MainActivity.class);
//                startActivity(intent);
//                return true;
//            } else if (id == R.id.triage) {
//                Intent intent = new Intent(this, MainActivity.class);
//                startActivity(intent);
//                return true;
//            } else if (id == R.id.history) {
//                Intent intent = new Intent(this, MainActivity.class);
//                startActivity(intent);
//                return true;
//            } else if (id == R.id.medicine) {
//                Intent intent = new Intent(this, MainActivity.class);
//                startActivity(intent);
//                return true;
//            } else if (id == R.id.checkin){
//                Intent intent = new Intent(this, CheckInPageActivity.class);
//                startActivity(intent);
//                return true;
//            }
//            else {
//                return false;
//            }
//        });
//    }

    /**
     * changes the text prompts based on type of user
     * @param userRole is the current type of user
     */
    public void updateUIBasedOnRole(String userRole ){
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleRole);  // which form
        MaterialButton buttonParent = view.findViewById(R.id.buttonParent);          // which form
        MaterialButton buttonChild = view.findViewById(R.id.buttonChild);            // which form

        // fix ui based on role
        TextView nightPrompt = view.findViewById(R.id.textView5);
        TextView activityPrompt = view.findViewById(R.id.textView6);
        TextView coughingPrompt = view.findViewById(R.id.textView67);
        if (userRole.equals("child")){
            toggleGroup.check(R.id.buttonChild);
            buttonParent.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_default_bg));
            buttonChild.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_selected_bg));
            nightPrompt.setText("Did you experience any night walking last night?");
            activityPrompt.setText("How limited was your activity level today?");
            coughingPrompt.setText("How often we’re you coughing or wheezing today?");

        }
        else if(userRole.equals("parent")){
            toggleGroup.check(R.id.buttonParent);
            buttonChild.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_default_bg));
            buttonParent.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_selected_bg));
            nightPrompt.setText("Did your child experience any night walking last night?");
            activityPrompt.setText("How limited was your child's activity level today?");
            coughingPrompt.setText("How often was your child coughing or wheezing today?");
        }

        TextView noInformation = view.findViewById(R.id.textView7);
        noInformation.setVisibility(View.INVISIBLE);

    }

    private boolean [] setUpTriggers(String currentTriggers){
        TextView multiSelectTriggers = view.findViewById(R.id.multiSelect);
        boolean [] selectedTriggers = new boolean[triggers.length];
        multiSelectTriggers.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
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
                            multiSelectTriggers.setText(currentTriggers);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return selectedTriggers;
    }
}