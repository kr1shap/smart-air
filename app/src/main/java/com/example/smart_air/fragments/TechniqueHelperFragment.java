package com.example.smart_air.fragments;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.smart_air.FirebaseInitalizer;
import com.example.smart_air.R;
import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.modelClasses.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class TechniqueHelperFragment extends Fragment {
    private int currentStep = 0;
    AuthRepository repo;
    private boolean isPerfectSession = true;
    private ImageView stepImage;
    private TextView tvStepInstruction, tvStepCount, tvStepTitle, tvTips;
    private Button btnIDidIt, btnNext;
    private VideoView videoView;
    CardView stepCard;
    private final String[] steps = {
            "Attach the spacer or mask to your inhaler. This makes sure the medicine goes in smoothly.",
            "Seal your lips tightly around the mouthpiece so no air escapes while you breathe in.",
            "Take a slow, deep breath in. Imagine filling your lungs like a big balloon!",
            "Hold your breath for about 10 seconds so the medicine has time to work.",
            "Exhale slowly and gently, like blowing bubbles in a calm pond.",
            "If you need more puffs, wait 30–60 seconds before the next one. Take your time and relax."
    };
    private final String[] tips = {
            "Make sure the spacer or mask is clean and fits properly.",
            "Try not to bite the mouthpiece — just seal your lips gently around it.",
            "Breathe in slowly, don’t rush! Slow breaths help the medicine reach deep.",
            "Counting to 10 in your head can help you hold your breath long enough.",
            "Exhale slowly through your mouth — no hurrying!",
            "Use a timer or count slowly to avoid taking the next puff too soon."
    };

    private final String[] stepTitles = {
            "Attach Spacer/Mask",
            "Seal Lips",
            "Inhale Slowly",
            "Hold Breath",
            "Exhale",
            "Wait Before Next Puff"
    };

    private final int[] stepImages = {
            R.drawable.technique_1,
            R.drawable.technique_2,
            R.drawable.technique_1,
            R.drawable.technique_4,
            R.drawable.technique_5,
            R.drawable.technique_6,
    };

    private FirebaseFirestore db = FirebaseInitalizer.getDb();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.technique_helper_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        //init repo
        repo = new AuthRepository();
        //check if user is authenticated
        if (repo.getCurrentUser() == null) { destroyFragment(); return; }
        //extra check just to ensure role is child
        repo.getUserDoc(repo.getCurrentUser().getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user == null) { return; }
                        String role = user.getRole();
                        if (!role.equals("child")) { destroyFragment(); return; }
                    }
                });

        //TODO: CODE ASSUMES CURRENT USER IS A CHILD!
        //get all other info
        tvTips = view.findViewById(R.id.tvTip);
        tvStepTitle = view.findViewById(R.id.tvStepTitle);
        stepImage = view.findViewById(R.id.animationView);
        tvStepInstruction = view.findViewById(R.id.tvStepDescription);
        tvStepCount = view.findViewById(R.id.tvProgress);
        btnIDidIt = view.findViewById(R.id.btnIDidIt);
        btnNext = view.findViewById(R.id.btnSkip); //called 'skip this step' in the guide
        videoView = view.findViewById(R.id.videoView); //video view
        stepCard = view.findViewById(R.id.stepCard); //get the card

        loadStep();

        btnIDidIt.setOnClickListener(v -> advanceStep(true));
        btnNext.setOnClickListener(v -> advanceStep(false));
    }

    /*
     * function: loads the next step of the technique session
     * pre: N/A
     * post: N/A
     */
    @SuppressLint("SetTextI18n") //suppress text warning
    private void loadStep() {
        //animate the card
        stepCard.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            //Update content
            tvStepTitle.setText(stepTitles[currentStep]); //step title
            tvStepInstruction.setText(steps[currentStep]); //current step
            tvTips.setText(tips[currentStep]);
            tvStepCount.setText("Step " + (currentStep + 1) + " of " + steps.length);
            //set the next image in the array as the video instead
            if (currentStep == 2) {
                stepImage.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + R.raw.technique_3);
                videoView.setVideoURI(videoUri);
                videoView.start();
            } else {
                videoView.setVisibility(View.GONE);
                stepImage.setVisibility(View.VISIBLE);
                stepImage.setImageResource(stepImages[currentStep]);
            }

            // disable 'I did it button'
            if (currentStep == 3) {
                btnIDidIt.setEnabled(false);
                btnIDidIt.setText("Wait 10 seconds...");
                btnIDidIt.postDelayed(() -> {
                    btnIDidIt.setEnabled(true);
                    btnIDidIt.setText("I Did It!");
                }, 10_000); // re-enable after 10s
            } else {
                btnIDidIt.setEnabled(true);
                btnIDidIt.setText("I Did It!");
            }

            // Fade back in
            stepCard.animate().alpha(1f).setDuration(150).start();
        }).start();
    }

    /*
     * function: loads the next step of the technique session
     * pre: boolean perfect, indicating if it was a perfect step or not
     * post: N/A
     */
    private void advanceStep(boolean perfect) {
        if (!perfect) { isPerfectSession = false; } //set session to false if not perfect step
        currentStep++;  //increment to next step
        if (currentStep < steps.length) { loadStep(); }  //if still on prev step then load next one
        else { completeSession(); } //complete session if done
    }

    /*
     * function: called when completing session
     * pre: N/A
     * post: N/A, sends alert dialog
     */
    private void completeSession() {
        updateTechniqueStats(repo.getCurrentUser().getUid(), isPerfectSession);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog);
        builder.setTitle(isPerfectSession ? "Perfect Session!" : "Session Complete!"); //build dialog with title based on whether session was perfect or complete
        builder.setMessage(isPerfectSession
                ? "Amazing! You completed every step perfectly!"
                : "Good job completing your technique practice!");
        builder.setPositiveButton("OK", (dialog, which) -> destroyFragment());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*
     * function: UPDATE data or POST data (stats for technique) [considers when the technique map doesnt exist as well]
     * pre: String childUid, boolean perfectSession
     * post: N/A
     */
    private void updateTechniqueStats(String childUid, boolean perfectSession) {
        //children document
        DocumentReference childRef = db.collection("children").document(childUid);

        // transaction so no one else modifies the data at the same time (b/c streak)
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(childRef);
            int currentStreak = 1;
            int totalPerfect = 0;
            int totalCompleted = 0;
            String lastDateStr = null;
            String today = getToday();

            //get technique map if it exists
            if (snap.exists()) {
                Map<String, Object> techniqueStats = (Map<String, Object>) snap.get("techniqueStats");
                if (techniqueStats != null) {
                    currentStreak = techniqueStats.get("currentStreak") != null ?
                            ((Number) techniqueStats.get("currentStreak")).intValue() : 1;
                    totalPerfect = techniqueStats.get("totalPerfectSessions") != null ?
                            ((Number) techniqueStats.get("totalPerfectSessions")).intValue() : 0;
                    totalCompleted = techniqueStats.get("totalCompletedSessions") != null ?
                            ((Number) techniqueStats.get("totalCompletedSessions")).intValue() : 0;
                    lastDateStr = (String) techniqueStats.get("lastSessionDate");
                }
            }

            //update the current streak
            if (lastDateStr == null || lastDateStr.isEmpty()) currentStreak = 1; //no last date; new streak
            else if (today.equals(lastDateStr)) { } //no change if today equals the last day
            else if (getYesterday().equals(lastDateStr)) { currentStreak++; } //add to streak if yesterday equals the last day
            else { currentStreak = 1; } //reset streak for some other case

            if (perfectSession) { totalPerfect++; } //if today is a perfect session, we add to total perfects
            totalCompleted++; //add 1 to completed technique sessions

            //create the map again
            Map<String, Object> techniqueStatsMap = new HashMap<>();
            techniqueStatsMap.put("currentStreak", currentStreak);
            techniqueStatsMap.put("totalPerfectSessions", totalPerfect);
            techniqueStatsMap.put("totalCompletedSessions", totalCompleted);
            techniqueStatsMap.put("lastSessionDate", today);

            Map<String, Object> data = new HashMap<>();
            data.put("techniqueStats", techniqueStatsMap);
            transaction.set(childRef, data, SetOptions.merge()); //merge; preserves other fields
            return null;

        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getActivity(), "Saved your technique session!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e("TechniqueStats", "Failed to update your technique stats.", e);
            Toast.makeText(getActivity(), "Could not save session :( ", Toast.LENGTH_SHORT).show();
        });
    }
//    private void updateTechniqueStats(String childUid, boolean perfectSession) {
//
//        //get document, or generate one if it doesn't exist
//        DocumentReference statsRef = db.collection("techniqueStats").document(childUid);
//
//        //use transaction to ensure no one else modifies the data at the same time
//        db.runTransaction(transaction -> {
//            DocumentSnapshot snap = transaction.get(statsRef);
//            int currentStreak = 1;
//            int totalPerfect = 0;
//            int totalCompleted = 0;
//            String lastDateStr = null;
//            String today = getToday();
//
//            //get the snap if it exists. if not, then we will use the null default values above
//            if (snap.exists()) {
//                currentStreak = snap.getLong("currentStreak") != null ? snap.getLong("currentStreak").intValue() : 1;
//                totalPerfect = snap.getLong("totalPerfectSessions") != null ? snap.getLong("totalPerfectSessions").intValue() : 0;
//                totalCompleted = snap.getLong("totalCompletedSessions") != null ? snap.getLong("totalCompletedSessions").intValue() : 0;
//                lastDateStr = snap.getString("lastSessionDate");
//            }
//
//            //UPDATE CURRENT STREAK
//            if (lastDateStr == null) currentStreak = 1; //no last date - new streak
//            else if (today.equals(lastDateStr)) { } //no change if today equals the last day
//            else if (getYesterday().equals(lastDateStr)) { currentStreak++; } //add to streak if yesterday equals the last day
//            else { currentStreak = 1; } //reset streak for some other case
//            if (perfectSession) { totalPerfect++; } //if today is a perfect session, we add to total perfects
//            totalCompleted++;                       //add 1 to compelted technique sessions
//            Map<String, Object> data = new HashMap<>();
//            data.put("currentStreak", currentStreak);
//            data.put("totalPerfectSessions", totalPerfect);
//            data.put("totalCompletedSessions", totalCompleted); //add 1 to completed, as we completed session
//            data.put("lastSessionDate", today); //keeps it in the YYYY-MM-DD format
//
//            transaction.set(statsRef, data); //set data
//            return null;
//
//        }).addOnSuccessListener(aVoid -> {
//            Toast.makeText(getActivity(), "Saved your technique session!", Toast.LENGTH_SHORT).show();
//        }).addOnFailureListener(e -> {
//            Log.e("TechniqueStats", "Failed to update technique stats", e);
//            Toast.makeText(getActivity(), "Could not save session. Progress will be retried.", Toast.LENGTH_SHORT).show();
//        });
//    }

    /*
    * pre: N/A
    * post: returns string of yesterday's date in YYYY-MM-DD format
    * */
    private String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }
    /*
     * pre: N/A
     * post: returns string of today's date in YYYY-MM-DD format
     * */
    private String getToday() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return fmt.format(new Date()); // today's date string
    }

    /*
     * a method called when an error occurs, goes back the main activity
     */
    public void destroyFragment() {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment instanceof TechniqueHelperFragment) {
            fm.beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }
}
