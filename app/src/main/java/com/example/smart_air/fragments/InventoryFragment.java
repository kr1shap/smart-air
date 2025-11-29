package com.example.smart_air.fragments;

import static android.content.Context.ALARM_SERVICE;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smart_air.ExpiryCheck;
import com.example.smart_air.MainActivity;
import com.example.smart_air.Repository.NotificationRepository;
import com.example.smart_air.modelClasses.Notification;
import com.example.smart_air.modelClasses.enums.NotifType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.SetOptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smart_air.R;
import com.example.smart_air.modelClasses.Child;
import com.example.smart_air.viewmodel.SharedChildViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryFragment extends Fragment {

    private LinearLayout controllerContainer;
    private LinearLayout rescueContainer;
    int lessthan20=60;
    int threshold=300;
    private SharedChildViewModel sharedModel;
    private String selectedChildUid = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.inventorypage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // set checks for expiring medication
        Context appContext = requireContext().getApplicationContext();
        scheduleDailyExpiryAlarm(appContext, 0, 0);

        controllerContainer = view.findViewById(R.id.controller_card_container);
        rescueContainer     = view.findViewById(R.id.rescue_card_container);
        Button backButton   = view.findViewById(R.id.btn_back_meds);

        backButton.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            denyAccess();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        denyAccess();
                        return;
                    }

                    String role = doc.getString("role");
                    if (!"parent".equals(role)) {
                        denyAccess();
                        return;
                    }
                    setupChildSelectionWithViewModel();
                })
                .addOnFailureListener(e -> {
                    Log.e("InventoryFragment", "Error loading user role", e);
                    denyAccess();
                });
    }


    private void denyAccess() {
        if (isAdded()) {
            Toast.makeText(requireContext(),
                    "You do not have access to the inventory page.",
                    Toast.LENGTH_SHORT).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }

    /**
     * Uses your existing SharedChildViewModel pattern.
     * When a child is selected, we load that child's inventory.
     */
    private void setupChildSelectionWithViewModel() {
        sharedModel = new ViewModelProvider(requireActivity())
                .get(SharedChildViewModel.class);

        // When children list loads, just cache it
        sharedModel.getAllChildren().observe(getViewLifecycleOwner(), children -> {
            Log.d("InventoryFragment", "Children list size = " +
                    (children == null ? 0 : children.size()));
        });

        // When current child index changes → load that child's inventory
        sharedModel.getCurrentChild().observe(getViewLifecycleOwner(), currentIndex -> {
            List<Child> children = sharedModel.getAllChildren().getValue();
            if (children != null && !children.isEmpty() && currentIndex != null) {
                selectedChildUid = children.get(currentIndex).getChildUid();
                Log.d("InventoryFragment", "Selected child = " + selectedChildUid);
                loadInventoryForChild(selectedChildUid);
            } else {
                selectedChildUid = null;
                controllerContainer.removeAllViews();
                rescueContainer.removeAllViews();
            }
        });
    }
    private void loadInventoryForChild(String childUid) {
        controllerContainer.removeAllViews();
        rescueContainer.removeAllViews();

        if (childUid == null) return;

        Log.d("InventoryFragment", "Loading inventory for childUid = " + childUid);

        // controller doc
        loadSingleInventoryDoc(childUid, "controller", controllerContainer);

        // rescue doc
        loadSingleInventoryDoc(childUid, "rescue", rescueContainer);
    }

    private void loadSingleInventoryDoc(String childUid,
                                        String docId,
                                        LinearLayout targetContainer) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("children")
                .document(childUid)
                .collection("inventory")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // If you later want an "Add" button here, you can add it.
                        TextView tv = new TextView(requireContext());
                        tv.setText("No " + docId + " inventory added yet.");
                        tv.setTextSize(14f);
                        targetContainer.addView(tv);
                        return;
                    }

                    LayoutInflater inflater = LayoutInflater.from(requireContext());
                    View card = inflater.inflate(R.layout.inventory_card, targetContainer, false);

                    EditText nameEt      = card.findViewById(R.id.edit_controller_name);
                    TextView amountTv    = card.findViewById(R.id.text_controller_today);
                    TextView purchaseTv  = card.findViewById(R.id.text_controller_purchase_date);
                    TextView expiryTv    = card.findViewById(R.id.text_controller_expiry_date);
                    Button   editBtn     = card.findViewById(R.id.btn_save_controller_name);

                    String name = doc.getString("name");
                    Long amount = doc.getLong("amount");
                    com.google.firebase.Timestamp purchaseTs = doc.getTimestamp("purchaseDate");
                    com.google.firebase.Timestamp expiryTs   = doc.getTimestamp("expiryDate");

                    java.text.DateFormat df =
                            new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault());

                    if (name != null)   nameEt.setText(name);
                    if (amount != null) amountTv.setText("Amount: " + amount);
                    if (purchaseTs != null)
                        purchaseTv.setText("Purchase Date: " + df.format(purchaseTs.toDate()));
                    if (expiryTs != null)
                        expiryTv.setText("Expiry Date: " + df.format(expiryTs.toDate()));

                    editBtn.setOnClickListener(v ->
                            showEditInventoryDialog(childUid, docId,
                                    amountTv, purchaseTv, expiryTv));

                    targetContainer.addView(card);
                })
                .addOnFailureListener(e ->
                        Log.e("InventoryFragment",
                                "Error loading inventory doc " + docId, e));
    }
    private void showEditInventoryDialog(String childUid,
                                         String docId,
                                         TextView amountTv,
                                         TextView purchaseTv,
                                         TextView expiryTv) {

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_inventory_edit, null);

        EditText etAmount   = dialogView.findViewById(R.id.et_child_name);
        EditText etPurchase = dialogView.findViewById(R.id.et_personal_best);
        EditText etExpiry   = dialogView.findViewById(R.id.et_badge_threshold_tech);
        Button   btnSave    = dialogView.findViewById(R.id.btn_save_edits);

        // Pre-fill from current text (optional, quick & dirty parsing)
        String currentAmountText   = amountTv.getText().toString().replace("Amount:", "").trim();
        String currentPurchaseText = purchaseTv.getText().toString().replace("Purchase Date:", "").trim();
        String currentExpiryText   = expiryTv.getText().toString().replace("Expiry Date:", "").trim();

        etAmount.setText(currentAmountText);
        etPurchase.setText(currentPurchaseText);
        etExpiry.setText(currentExpiryText);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnSave.setOnClickListener(v -> {
            saveInventoryEdits(childUid, docId, etAmount, etPurchase, etExpiry, amountTv, purchaseTv, expiryTv, dialog);
        });

        dialog.show();
    }
    private void saveInventoryEdits(String childUid, String docId, EditText etAmount, EditText etPurchase, EditText etExpiry, TextView amountTv, TextView purchaseTv, TextView expiryTv, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            dialog.dismiss();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        String amountStr=etAmount.getText().toString().trim();
        if (amountStr.isEmpty()==false) {
            try {
                int newAmount = Integer.parseInt(amountStr);
                if (newAmount>threshold) {
                    Toast.makeText(requireContext(), "Amount cannot exceed 300.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newAmount<=lessthan20)
                {
                    Toast.makeText(requireContext(), "Sent low inventory alert!.", Toast.LENGTH_SHORT).show();
                    sendinventoryAlert(childUid);
                }
                updates.put("amount", newAmount);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid number.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        java.text.DateFormat df = new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault());
        String purchaseStr = etPurchase.getText().toString().trim();
        if (!purchaseStr.isEmpty()) {
            try {
                Date p = df.parse(purchaseStr);
                if (p != null) {
                    updates.put("purchaseDate", new com.google.firebase.Timestamp(p));
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Purchase date format should be like: November 29, 2025", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String expiryStr = etExpiry.getText().toString().trim();
        if (expiryStr.isEmpty()==false) {
            try {
                Date e = df.parse(expiryStr);
                if (e!=null) {
                    updates.put("expiryDate", new com.google.firebase.Timestamp(e));
                }
            } catch (Exception ex) {
                Toast.makeText(requireContext(), "Expiry date format should be like: November 19, 2025", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        db.collection("children")
                .document(childUid)
                .collection("inventory")
                .document(docId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // update UI text from what user typed
                    if (updates.containsKey("amount")) {
                        amountTv.setText("Amount: " + updates.get("amount"));
                    }
                    if (updates.containsKey("purchaseDate")) {
                        purchaseTv.setText("Purchase Date: " + purchaseStr);
                    }
                    if (updates.containsKey("expiryDate")) {
                        expiryTv.setText("Expiry Date: " + expiryStr);
                    }
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e("InventoryFragment", "Error saving inventory", e);
                    Toast.makeText(requireContext(), "Failed to save. Please try again.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
    }
    /*
    get child's name
     */
    public void getchildname(String childUid, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String childName = doc.getString("name");
                        onSuccess.onSuccess(childName);
                    }
                    else {
                        onFailure.onFailure(new Exception("Child document not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /*
     * Send inventory low notification to all parents of this child
     */
    public void sendinventoryAlert(String childUid) {
        Log.d("Inventory", "sendinventoryAlert for childUid = " + childUid);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1) Get the child's name from children/{childUid}
        getchildname(childUid, childName -> {

            // 2) Get child's user doc from users/{childUid} to find parentUid array
            db.collection("users")
                    .document(childUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Log.e("Inventory", "Child user document missing in users collection");
                            return;
                        }

                        @SuppressWarnings("unchecked")
                        List<String> parentUids = (List<String>) doc.get("parentUid");
                        if (parentUids == null || parentUids.isEmpty()) {
                            Log.e("Inventory", "No parentUid array found on child user doc");
                            return;
                        }

                        NotificationRepository notifRepo = new NotificationRepository();
                        for (String pUid : parentUids) {
                            if (pUid == null) continue;

                            Notification notif = new Notification(
                                    childUid,                 // child id
                                    false,                    // read flag
                                    Timestamp.now(),          // time
                                    NotifType.INVENTORY,      // type
                                    childName                 // extra text/name
                            );

                            notifRepo.createNotification(pUid, notif)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d("NotificationRepo",
                                                    "Inventory notification created for parent " + pUid))
                                    .addOnFailureListener(e ->
                                            Log.e("NotificationRepo",
                                                    "Failed to notify parent " + pUid, e));
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e("Inventory", "Failed to load child user doc", e)
                    );

        }, error -> {
            Log.e("Inventory", "Failed to fetch child name in sendinventoryAlert", error);
        });
    }
    private void scheduleDailyExpiryAlarm(Context context, int hour, int minute) {

        Intent intent = new Intent(context, ExpiryCheck.class);
        intent.setAction("CHECK_EXPIRY");

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, flags
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            Log.e("Alarm", "AlarmManager null!");
            return;
        }

        // compute trigger time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // if time already passed today → schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // schedule repeating every day
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );

        Log.d("Alarm", "Expiry alarm set for: " + calendar.getTime());
    }

}
