package com.example.smart_air.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.R;
import com.example.smart_air.modelClasses.OnboardingItem;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    //array list of all onboarding items to inflate to
    private final List<OnboardingItem> items;
    private int currentPos = 0; //to fix issue of no animation

    public OnboardingAdapter(List<OnboardingItem> items) {
        this.items = items;
    }

    //creates a new view
    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()) //inflate view
                .inflate(R.layout.item_onboarding, parent, false); //inflate onboarding layout
        return new OnboardingViewHolder(view);
    }

    //binds current item to holder, based on position (i.e. card 3 out of 5 slides)
    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        OnboardingItem item = items.get(position);
        boolean shldAnimate = (position == currentPos); //check if we should animate it or not
        holder.bind(item, shldAnimate); //get the right info based on position
    }

    //get #of cards
    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setCurrentPosition(int position) {
        this.currentPos = position;
    }

    //custom view holder for onboarding, using recycler view (extend view holder class)
    public static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView title;
        private final TextView description;
        private final CardView cardView;

        //Get from onboarding layout
        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            //instantiate all items in onboarding layout
            image = itemView.findViewById(R.id.onboardingImage);
            title = itemView.findViewById(R.id.onboardingTitle);
            description = itemView.findViewById(R.id.onboardingDescription);
            cardView = itemView.findViewById(R.id.cardView);
        }

        //on appearance of item, do the following
        public void bind(OnboardingItem item, boolean shldAnimate) {
            image.setImageResource(item.getImageRes());
            title.setText(item.getTitle());
            description.setText(item.getDescription());

            //bkgnd colour based on what's passed in
            cardView.setCardBackgroundColor(Color.parseColor(item.getBackgroundColor()));

            //clear old animation (to fix issue of not animating)
            image.clearAnimation();
            title.clearAnimation();
            description.clearAnimation();
            description.animate().cancel();
            //reset alpha
            description.setAlpha(0f);

            //only on animation, do such
            if (shldAnimate) {
                //animate
                Animation fadeIn = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.fade_in);
                Animation slideUp = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.slide_up);
                Animation scaleUp = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.scale_up);

                image.startAnimation(scaleUp);
                title.startAnimation(slideUp);

                //delay desc animation by a bit
                description.setAlpha(0f);
                description.animate()
                        .alpha(1f)
                        .setDuration(800)
                        .setStartDelay(300)
                        .start();
            } else {
                description.setAlpha(1f); //no animation
            }
        }
    }
}