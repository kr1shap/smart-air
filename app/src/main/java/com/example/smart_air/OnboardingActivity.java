package com.example.smart_air;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.smart_air.Repository.AuthRepository;
import com.example.smart_air.adapter.OnboardingAdapter;
import com.example.smart_air.modelClasses.OnboardingItem;
import com.example.smart_air.modelClasses.User;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;
    private LinearLayout dotsLayout;
    private OnboardingAdapter adapter;
    private List<OnboardingItem> onboardingItems;
    private User user;
    AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        authRepository = new AuthRepository();

//        //user passed from intent
//        String userJson = getIntent().getStringExtra("user");
//        user = new Gson().fromJson(userJson, User.class);

        //get all views
        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.nextBtn);
        btnSkip = findViewById(R.id.skipBtn);
        dotsLayout = findViewById(R.id.dotsLayout);
        //to see if auth and not null
        checkCurrentUser( () -> {
            setupOnboarding();
        });
    }

    private void setupOnboarding() {
        //choose setup based on role (different pages shown)
        setupOnboardingPages();

        //onboarding adapter
        adapter = new OnboardingAdapter(onboardingItems);
        viewPager.setAdapter(adapter);

        //dot pager
        setupDotsIndicator(0);

        //listen to page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setupDotsIndicator(position);

                // Tell adapter to animate this position
                adapter.setCurrentPosition(position);
                adapter.notifyItemChanged(position);


                //on last page, go to get started (home page)
                if (position == onboardingItems.size() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });

        //for next button
        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < onboardingItems.size() - 1) {
                viewPager.setCurrentItem(current + 1, true); //set next item +1
            } else {
                finishOnboarding(); //else we finished on boarding
            }
        });

        //skips everything and goes to onboarding
        btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupDotsIndicator(int position) {
        dotsLayout.removeAllViews(); //remove all dots
        ImageView[] dots = new ImageView[onboardingItems.size()];

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this); //new dot
            dots[i].setImageDrawable(getDrawable(R.drawable.dot_inactive)); //set as inactive
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ); //linear layout for dot
            params.setMargins(8, 0, 8, 0);
            dotsLayout.addView(dots[i], params); //add it
        }

        if (dots.length > 0) {
            dots[position].setImageDrawable(getDrawable(R.drawable.dot_active));
        }
    }

    //completed onboarding!
    private void finishOnboarding() {
        if (user == null) return; // in case
        //mark onboarding for device
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_completed_" + user.getUid(), true).apply();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); //finish current activity
    }

    //checks to see if auth
    private void checkCurrentUser(Runnable onReady) {
        //if current user invalid
        if(authRepository.getCurrentUser() == null) { redirectSignout(); }
        else {
            authRepository.getUserDoc(authRepository.getCurrentUser().getUid())
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            user = doc.toObject(User.class);
                            onReady.run(); //only continue when user loaded (async fxn)
                        }
                        else redirectSignout();
                    })
                    .addOnFailureListener(e -> redirectSignout());
        }
    }
    //redirect if unauth, not found
    private void redirectSignout() {
        Intent intent = new Intent(this, LandingPageActivity.class);
        startActivity(intent);
        finish(); //finish current activity
    }

    private void setupOnboardingPages() {
        onboardingItems = new ArrayList<>();

        //PAGES - FOR ALL ROLES (PARENT, PROVIDER, CHILD)
        onboardingItems.add(new OnboardingItem(
                R.drawable.transparentlog,
                "Welcome to " + getString(R.string.app_name),
                "A simple, kid-focused tool for learning asthma basics, logging symptoms, and sending parent-approved reports to healthcare providers.",
                "#f2f7fc"
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.onboarding_privacy,
                "Your Privacy Matters",
                "We respect your privacy and keep your data secure. Your information is encrypted and never shared without permission.",
                "#f2f7fc"
        ));

        //ROLE-SPECFIC
        String role = user.getRole().toLowerCase(); //precautionary should alr be lowercase
        switch (role) {
            case "parent":
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_pcinvite,
                        "Keep track of your children's asthma symptoms",
                        "Access powerful analytics and manage/add children from your centralized dashboard. Monitor their activity in real-time.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_report,
                        "Track your child's progress",
                        "Check your child's daily usage, history, and track your inventory.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_notif,
                        "Reminders and Alerts",
                        "Receive alerts for low inventory, triage escalation and much more.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_ppinvite,
                        "Connect with your provider",
                        "Allow providers to view child data (read-only) based on your choice, and export child summary/reports.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_privacy,
                        "Provider Access Defaults",
                        "By default, your provider has read-only access to all your children's data. You are able to toggle what can and can't be shared through the dashboard.",
                        "#f2f7fc"
                ));
                break;

            case "child":

                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_c,
                        "Controller Inhaler",
                        "A controller inhaler is your “everyday helper” that keeps your lungs calm so asthma doesn’t bother you as much.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_r,
                        "Rescue Inhaler",
                        "Your rescue inhaler acts like a speedy superhero that helps you breathe easier when symptoms pop up.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_streak,
                        "Streaks and Badges",
                        "Follow simple steps to use your inhaler like a pro! Complete streaks to earn badges and more.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_child,
                        "Track your progress",
                        "Tell us how you feel each day, your symptoms, and medication use through our dashboard.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_privacy,
                        "Parent Controls",
                        "Helps your grown-up keep an eye on your asthma info so you stay safe and supported.",
                        "#f2f7fc"
                ));

                break;

            case "provider":
            default:
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_ppinvite,
                        "Connect to a parent",
                        "Connect your account to a parent to view their children's data and progress, all in real-time.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_report,
                        "Reports & Insights",
                        "Review child-reported data with easy-to-read summaries and trends, exportable in a PDF.",
                        "#f2f7fc"
                ));
                onboardingItems.add(new OnboardingItem(
                        R.drawable.onboarding_privacy,
                        "Provider Controls",
                        "By default, you only have access to child data which the parent allows.",
                        "#f2f7fc"
                ));
                break;
        }
    }

}