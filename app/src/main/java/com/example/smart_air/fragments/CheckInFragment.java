package com.example.smart_air.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.R;
import com.example.smart_air.Repository.CheckInRepository;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Nullable;

public class CheckInFragment extends Fragment {
    private View view;
    int personalBest;
    String userRole = "";
    String correspondingUid;
    String currentTriggers = "Tap to Select";
    String [] triggers = {"Allergies", "Smoke","Flu","Strong smells", "Running", "Exercise", "Cold Air", "Dust/Pets", "Illness"};
    private SharedChildViewModel sharedModel;
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
        this.personalBest = 400; //TODO: get personal best from parent's original set up

        CheckInRepository repo = new CheckInRepository();
        repo.getUserInfo(this);

        // shared viewmodal
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        sharedModel.getAllChildren().observe(getViewLifecycleOwner(), children -> { // set up intial child
            if (children != null && !children.isEmpty()) {
                int currentIndex = sharedModel.getCurrentChild().getValue() != null
                        ? sharedModel.getCurrentChild().getValue()
                        : 0;

                String currentChildUid = children.get(currentIndex).getChildUid();
                this.correspondingUid = currentChildUid;
            }
        });

        sharedModel.getCurrentChild().observe(getViewLifecycleOwner(), currentIndex -> { // update each time child index changed
            List<Child> children = sharedModel.getAllChildren().getValue();
            if (children != null && !children.isEmpty() && currentIndex != null) {
                correspondingUid = children.get(currentIndex).getChildUid();
                refreshUINewChild(repo);
            }
        });

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
            boolean nightWaking = (radioNight.getCheckedRadioButtonId() == R.id.radioYes);
            SeekBar seekBar = view.findViewById(R.id.seekBar);          // activity limit
            int activityValue = (int)seekBar.getProgress();
            Slider slider = view.findViewById(R.id.sliderCough);                         // coughing/wheezing
            int coughingValue = (int)slider.getValue();
            int pef[] = {0}; // array to hold current pef
            if(userRole.equals("parent")){
                EditText myNumberEditText = view.findViewById(R.id.editTextNumber);
                String myNumberEditTextString = myNumberEditText.getText().toString().trim();
                int inputPef = Integer.parseInt(myNumberEditTextString);

                EditText preText = view.findViewById(R.id.editTextPreMed);
                String preTextString = preText.getText().toString().trim();
                EditText postText = view.findViewById(R.id.editTextPostMed);
                String postTextString = postText.getText().toString().trim();
                int pre;
                int post;

                if(postTextString.isEmpty() || preTextString.isEmpty()){
                    pre = 0;
                    post = 0;
                }
                else{
                    pre = Integer.parseInt(preTextString);
                    post = Integer.parseInt(postTextString);
                }
                repo.maxPef(correspondingUid, userRole, inputPef, maxValue -> {
                    pef[0] = maxValue; // if pef changes it runs with new
                    repo.saveUserData(CheckInFragment.this, userRole, triggers, selectedTriggers, correspondingUid, nightWaking, activityValue, coughingValue,pef[0],pre,post);
                });
            }
            else{
                repo.saveUserData(CheckInFragment.this, userRole, triggers, selectedTriggers, correspondingUid, nightWaking, activityValue, coughingValue,0,0,0); // otherwise runs with no pef
            }
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
                   repo.getUserInputOther(CheckInFragment.this,correspondingUid,userRole);
                }
                else if(userRole.equals("parent")){
                   repo.getUserInput(this,userRole,correspondingUid);
                }
            }
            else if (checkedId == R.id.buttonChild && isChecked){
                buttonParent.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_default_bg));
                buttonChild.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_selected_bg));
                if(userRole.equals("child")){
                  repo.getUserInput(this,userRole,correspondingUid);

                }
                else if(userRole.equals("parent")){
                   repo.getUserInputOther(CheckInFragment.this,correspondingUid,userRole);
                }
            }
        });
    }

    private void refreshUINewChild(CheckInRepository repo) {
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleRole);
        int checkedId = toggleGroup.getCheckedButtonId();

        if (checkedId == R.id.buttonParent){
            if(userRole.equals("child")){
                repo.getUserInputOther(CheckInFragment.this,correspondingUid,userRole);
            }
            else if(userRole.equals("parent")){
                repo.getUserInput(this,userRole,correspondingUid);
            }
        }
        else if (checkedId == R.id.buttonChild){
            if(userRole.equals("child")){
                repo.getUserInput(this,userRole,correspondingUid);

            }
            else if(userRole.equals("parent")){
                repo.getUserInputOther(CheckInFragment.this,correspondingUid,userRole);
            }
        }
    }

    public void updateInfoInput(Boolean nightWaking, Long activityLimits, Long coughingWheezing, List<String> selection, Long pef, int pre, int post) {
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);
        CardView PEFCard = view.findViewById(R.id.PEF);

        if(userRole.equals("parent")){PEFCard.setVisibility(View.VISIBLE);}
        else{PEFCard.setVisibility(View.INVISIBLE);}

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

        if(userRole.equals("parent")){
            EditText myNumberEditText = view.findViewById(R.id.editTextNumber);
            myNumberEditText.setText(pef.toString());
        }

        if(!(pre == 0 && post == 0)){
            EditText preText = view.findViewById(R.id.editTextPreMed);
            preText.setText(Integer.toString(pre));
            EditText postText = view.findViewById(R.id.editTextPostMed);
            postText.setText(Integer.toString(post));
        }

        setCardCurrent();

    }

    public void updateInfoInputWithoutValues() {
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);
        CardView PEFCard = view.findViewById(R.id.PEF);

        if(userRole.equals("parent")){PEFCard.setVisibility(View.VISIBLE);}
        else{PEFCard.setVisibility(View.INVISIBLE);}

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

    public void updateInfoInputOther(Boolean nightWaking, Long activityLimits, Long coughingWheezing, List<String> selection, Long pef) {
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);
        CardView PEFCard = view.findViewById(R.id.PEF);

        if(userRole.equals("parent")){PEFCard.setVisibility(View.VISIBLE);}
        else{PEFCard.setVisibility(View.INVISIBLE);}

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

        if(userRole.equals("child")){
            EditText myNumberEditText = view.findViewById(R.id.editTextNumber);
            myNumberEditText.setText(pef.toString());
        }


        setCardOther();

    }

    public void updateInfoInputOtherWithoutValues() {
        CardView nightWakingCard = view.findViewById(R.id.nightCard);
        CardView activityLimitsCard = view.findViewById(R.id.activity);
        CardView coughWheezeCard = view.findViewById(R.id.coughing);
        CardView triggersCard = view.findViewById(R.id.triggers);
        CardView PEFCard = view.findViewById(R.id.PEF);
        PEFCard.setVisibility(View.INVISIBLE);

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
        CardView PEFCard = view.findViewById(R.id.PEF);

        if(userRole.equals("parent")){PEFCard.setVisibility(View.VISIBLE);}
        else{PEFCard.setVisibility(View.INVISIBLE);}

        nightWakingCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        activityLimitsCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        coughWheezeCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        triggersCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        PEFCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
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

        EditText pefNumber = view.findViewById(R.id.editTextNumber);
        pefNumber.setEnabled(true);

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
        CardView PEFCard = view.findViewById(R.id.PEF);

        if(userRole.equals("parent")){PEFCard.setVisibility(View.VISIBLE);}
        else{PEFCard.setVisibility(View.INVISIBLE);}


        nightWakingCard.setVisibility(View.VISIBLE);
        activityLimitsCard.setVisibility(View.VISIBLE);
        coughWheezeCard.setVisibility(View.VISIBLE);
        triggersCard.setVisibility(View.VISIBLE);

        nightWakingCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colour_grey));
        activityLimitsCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colour_grey));
        coughWheezeCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colour_grey));
        triggersCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colour_grey));
        PEFCard.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colour_grey));

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

        EditText pefNumber = view.findViewById(R.id.editTextNumber);
        pefNumber.setEnabled(false);

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
    public void noUserFound() {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment instanceof CheckInFragment) {
            fm.beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }

    /**
     * a method that loads info about the user into variables
     * @param role, stores what type of user it is
     */
    public void userInfoLoaded(String role, String correspondingUid){
        userRole = role;
        updateUIBasedOnRole(role);
        if(correspondingUid.equals("")){
            return;
        }
        this.correspondingUid = correspondingUid;
    }

    /**
     * changes the text prompts based on type of user
     * @param userRole is the current type of user
     */
    public void updateUIBasedOnRole(String userRole ){
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleRole);  // which form
        MaterialButton buttonParent = view.findViewById(R.id.buttonParent);          // which form
        MaterialButton buttonChild = view.findViewById(R.id.buttonChild);            // which form
        CardView PEFCard = view.findViewById(R.id.PEF);

        // fix ui based on role
        TextView nightPrompt = view.findViewById(R.id.textView5);
        TextView activityPrompt = view.findViewById(R.id.textView6);
        TextView coughingPrompt = view.findViewById(R.id.textView67);
        if (userRole.equals("child")){
            toggleGroup.check(R.id.buttonChild);
            buttonParent.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_default_bg));
            buttonChild.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_selected_bg));
            nightPrompt.setText("Did you experience any night waking last night?");
            activityPrompt.setText("How limited was your activity level today?");
            coughingPrompt.setText("How often we’re you coughing or wheezing today?");
            PEFCard.setVisibility(View.INVISIBLE);

        }
        else if(userRole.equals("parent")){
            toggleGroup.check(R.id.buttonParent);
            buttonChild.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_default_bg));
            buttonParent.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.role_selected_bg));
            nightPrompt.setText("Did your child experience any night waking last night?");
            activityPrompt.setText("How limited was your child's activity level today?");
            coughingPrompt.setText("How often was your child coughing or wheezing today?");
            PEFCard.setVisibility(View.VISIBLE);
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

    public String zoneColour(int pef) {
        int percent = (int) Math.round((pef * 100.0) / personalBest);
        if(percent >= 80){
            return "green";
        }
        if(percent >= 50){
            return "yellow";
        }
        return "red";

    }
    public int zoneNumber(int pef){
        return (int) Math.round((pef * 100.0) / personalBest);
    }
}