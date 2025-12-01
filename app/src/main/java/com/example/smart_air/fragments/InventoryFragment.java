package com.example.smart_air.fragments;

import static android.content.Context.ALARM_SERVICE;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import android.widget.DatePicker;
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
    int threshold=300;
    double lessthan20=threshold*0.2;
    private SharedChildViewModel sharedModel;
    private String selectedChildUid = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.inventorypage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        controllerContainer = view.findViewById(R.id.controller_card_container);
        rescueContainer    = view.findViewById(R.id.rescue_card_container);
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        // set checks for expiring medication
        Context appContext = requireContext().getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences("expiry_prefs", Context.MODE_PRIVATE);
        boolean alreadyScheduled = prefs.getBoolean("expiry_alarm_scheduled", false);
        if (alreadyScheduled==false) {
            scheduleexpirycheck(appContext, 0, 0); // set 24h time
            prefs.edit().putBoolean("expiry_alarm_scheduled", true).apply();
            Log.d("Alarm", "First time scheduling expiry alarm from InventoryFragment");
        }
        else {
            Log.d("Alarm", "Expiry alarm already scheduled, initial scheduled time will not change");
        }
        // back button functionality
        Button backButton   = view.findViewById(R.id.btn_back_meds);
        backButton.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
        // checks if user is parent, if not don't allow access, if so show inventory screen
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            denyAccess();
            return;
        }
        sharedModel.getCurrentRole().observe(getViewLifecycleOwner(), role -> {
            if (role == null){
                return;
            }
            if (!"parent".equals(role)) {
                denyAccess();
                return;
            }
            setupchildinventory();
        });
    }

    /*
    Message to tell any non-parent user they cannot access this page
     */
    public void denyAccess() {
        if (isAdded()) {
            Toast.makeText(requireContext(), "You do not have access to the inventory page.", Toast.LENGTH_SHORT).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }

    /*
    loads selected childs inventory page UI
     */
    public void setupchildinventory() {
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedChildViewModel.class);
        // cache children list loading
        sharedModel.getAllChildren().observe(getViewLifecycleOwner(), children -> {
            Log.d("InventoryFragment", "Children list size = " + (children == null ? 0 : children.size()));
        });
        // load childs inventory based on selected child
        sharedModel.getCurrentChild().observe(getViewLifecycleOwner(), currentIndex -> {
            List<Child> children = sharedModel.getAllChildren().getValue();
            if (children != null && !children.isEmpty() && currentIndex != null) {
                selectedChildUid = children.get(currentIndex).getChildUid();
                Log.d("InventoryFragment", "Selected child = " + selectedChildUid);
                loadinventory(selectedChildUid);
            } else {
                selectedChildUid = null;
                controllerContainer.removeAllViews();
                rescueContainer.removeAllViews();
            }
        });
    }
    /*
    loads the rescue and controller inventory of selected child
     */
    public void loadinventory(String childUid) {
        // clears existing view of inventory
        controllerContainer.removeAllViews();
        rescueContainer.removeAllViews();
        // if kid doesnt exist dont load
        if (childUid == null){ return; }
        Log.d("InventoryFragment", "Loading inventory for childUid = " + childUid);
        // load corresponding document
        loadinventoryDoc(childUid, "controller", controllerContainer);
        loadinventoryDoc(childUid, "rescue", rescueContainer);
    }
    /*
     load rescue/controller document from Firebase
     */
    public void loadinventoryDoc(String childUid, String docId, LinearLayout targetContainer) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("children")
                .document(childUid)
                .collection("inventory")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()==false) {
                        // if document does not exist, tell user
                        TextView tv = new TextView(requireContext());
                        tv.setText("No " + docId + " inventory added yet.");
                        tv.setTextSize(14f);
                        targetContainer.addView(tv);
                        return;
                    }
                    // inflate document into viewcard
                    LayoutInflater inflater = LayoutInflater.from(requireContext());
                    View card = inflater.inflate(R.layout.inventory_card, targetContainer, false);
                    EditText nameEt = card.findViewById(R.id.edit_controller_name);
                    TextView amountTv = card.findViewById(R.id.text_controller_today);
                    TextView purchaseTv = card.findViewById(R.id.text_controller_purchase_date);
                    TextView expiryTv = card.findViewById(R.id.text_controller_expiry_date);
                    Button editBtn = card.findViewById(R.id.btn_save_controller_name);
                    // read Firebase values
                    String name = doc.getString("name");
                    Long amount = doc.getLong("amount");
                    com.google.firebase.Timestamp purchaseTs = doc.getTimestamp("purchaseDate");
                    com.google.firebase.Timestamp expiryTs   = doc.getTimestamp("expiryDate");
                    java.text.DateFormat df = new java.text.SimpleDateFormat("MMM d yyyy", java.util.Locale.getDefault());
                    // load and format values
                    if (name != null){
                        nameEt.setText(name);
                    }
                    if (amount != null){
                        amountTv.setText("Amount: " + amount);
                    }
                    if (purchaseTs != null){
                        purchaseTv.setText("Purchase Date: " + df.format(purchaseTs.toDate()));
                    }
                    if (expiryTs != null){
                        expiryTv.setText("Expiry Date: " + df.format(expiryTs.toDate()));
                    }
                    // show edit dialog on click
                    editBtn.setOnClickListener(v ->
                            editdialog(childUid, docId, nameEt, amountTv, purchaseTv, expiryTv)
                    );
                    // add card to UI
                    targetContainer.addView(card);
                })
                .addOnFailureListener(e ->
                        Log.e("InventoryFragment", "Error loading inventory doc " + docId, e));
    }
    /*
        runs edit dialog to edit values in inventory
     */
    public void editdialog(String childUid, String docId, EditText nameEtOriginal, TextView amountTv, TextView purchaseTv, TextView expiryTv) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_inventory_edit, null);
        // fields in the dialog
        EditText etName   = dialogView.findViewById(R.id.et_child_name);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        Button btnPurchase = dialogView.findViewById(R.id.datepickerpur);
        Button btnExpiry   = dialogView.findViewById(R.id.datepickerexp);
        Button btnSave     = dialogView.findViewById(R.id.btn_save_edits);
        // read existing values from the card
        String currentNameText     = nameEtOriginal.getText().toString().trim();
        String currentAmountText   = amountTv.getText().toString().replace("Amount:", "").trim();
        String currentPurchaseText = purchaseTv.getText().toString().replace("Purchase Date:", "").trim();
        String currentExpiryText   = expiryTv.getText().toString().replace("Expiry Date:", "").trim();
        // prefill name + amount
        etName.setText(currentNameText);
        etAmount.setText(currentAmountText);
        if (currentPurchaseText.isEmpty()==false) btnPurchase.setText(currentPurchaseText);
        if (currentExpiryText.isEmpty()==false) btnExpiry.setText(currentExpiryText);
        btnPurchase.setOnClickListener(v -> opendatepick(btnPurchase));
        btnExpiry.setOnClickListener(v -> opendatepick(btnExpiry));
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        btnSave.setOnClickListener(v -> {
            saveedits(childUid, docId, etName, etAmount, btnPurchase, btnExpiry, nameEtOriginal, amountTv, purchaseTv, expiryTv, dialog);
        });
        dialog.show();
    }


    /*
    check validity and save edited inventory into Firebase
 */
    public void saveedits(String childUid, String docId, EditText etName, EditText etAmount, Button btnPurchase, Button btnExpiry, EditText nameEtOriginal, TextView amountTv, TextView purchaseTv, TextView expiryTv, AlertDialog dialog) {
        Integer newAmountval = null;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            dialog.dismiss();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        String nameStr = etName.getText().toString().trim();
        if (nameStr.isEmpty()==false) {
            updates.put("name", nameStr);
        }
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()==false) {
            try {
                int newAmount=Integer.parseInt(amountStr);
                // make sure amount isnt more then threshold
                if (newAmount > threshold) {
                    Toast.makeText(requireContext(), "Amount cannot exceed 300.", Toast.LENGTH_SHORT).show();
                    return;
                }
                newAmountval=newAmount;
                updates.put("amount", newAmount);
            }
            catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid number.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        java.text.DateFormat df = new java.text.SimpleDateFormat("MMM d yyyy", java.util.Locale.getDefault());
        String purchaseStr = btnPurchase.getText().toString().trim();
        if (purchaseStr.isEmpty()==false && !purchaseStr.equalsIgnoreCase("Select purchase date")) {
            try {
                Date p = df.parse(purchaseStr);
                updates.put("purchaseDate", new com.google.firebase.Timestamp(p));
            }
            catch (Exception e) {
                Toast.makeText(requireContext(), "Purchase date format should be like: November 29 2025", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String expiryStr = btnExpiry.getText().toString().trim();
        if (expiryStr.isEmpty()==false && !expiryStr.equalsIgnoreCase("Select expiry date")) {
            try {
                Date d = df.parse(expiryStr);
                updates.put("expiryDate", new com.google.firebase.Timestamp(d));
            }
            catch (Exception ex) {
                Toast.makeText(requireContext(), "Expiry date format should be like: November 29 2025", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        final boolean invalertsend=(newAmountval!=null&&newAmountval<=lessthan20); // decide if we should send inventory alert
        db.collection("children")
                .document(childUid)
                .collection("inventory")
                .document(docId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (invalertsend==true) {
                        Toast.makeText(requireContext(), "Sent low inventory alert!", Toast.LENGTH_SHORT).show();
                        sendinventoryAlert(childUid);
                    }
                    // update UI fields on the card
                    if (updates.containsKey("name")) {
                        nameEtOriginal.setText(nameStr);
                    }
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
      send inventory-related notifications to all parents of this child
     */
    public void sendinventoryAlert(String childUid) {
        Log.d("Inventory", "sendinventoryAlert for childUid = " + childUid);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(childUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()==false) {
                            Log.e("Inventory", "Child user document missing in users collection");
                            return;
                        }
                        @SuppressWarnings("unchecked") // surpresses warnings
                        List<String> parentUids = (List<String>) doc.get("parentUid");
                        if (parentUids == null || parentUids.isEmpty()) {
                            Log.e("Inventory", "No parentUid array found on child user doc");
                            return;
                        }
                        NotificationRepository notifRepo = new NotificationRepository();
                        for (String pUid : parentUids) {
                            if (pUid == null){
                                continue;
                            }
                            Notification notif = new Notification(childUid, false, Timestamp.now(), NotifType.INVENTORY);
                            notifRepo.createNotification(pUid, notif)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d("NotificationRepo", "Inventory notification created for parent " + pUid))
                                    .addOnFailureListener(e ->
                                            Log.e("NotificationRepo", "Failed to notify parent " + pUid, e));
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e("Inventory", "Failed to load child user doc", e));
    }
    /*
    schedule when to check if medications are expired
     */
    public void scheduleexpirycheck(Context context, int hour, int minute) {
        Intent intent = new Intent(context, ExpiryCheck.class);
        intent.setAction("CHECK_EXPIRY");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, flags
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager==null) {
            Log.e("Alarm", "AlarmManager null!");
            return;
        }
        // compute trigger time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // if time already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        // schedule repeating every day
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        Log.d("Alarm", "Expiry alarm set for: " + calendar.getTime());
    }
    /*
    to format date into string
     */
    public String datestring(int day, int month, int year)
    {
        return formatmonth(month) + " " + day + " " + year;
    }
    /*
    converts month number to string
     */
    public String formatmonth(int month)
    {
        if(month == 1) return "Jan";
        if(month == 2) return "Feb";
        if(month == 3) return "Mar";
        if(month == 4) return "Apr";
        if(month == 5) return "May";
        if(month == 6) return "Jun";
        if(month == 7) return "Jul";
        if(month == 8) return "Aug";
        if(month == 9) return "Sep";
        if(month == 10) return "Oct";
        if(month == 11) return "Nov";
        if(month == 12) return "Dec";
        return "Jan"; // default, not reached
    }
    /*
    calendar dialog to choose expiry/purchase date
     */
    public void opendatepick(Button targetBtn) {
        // gets today's date to start at it
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        // creates calendar dialog
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                R.style.SmartAirDatePicker,
                // handle user selection
                (view, y, m, d) -> {
                    m++; // due to index of months starting from 0
                    String formatted = datestring(d, m, y);
                    targetBtn.setText(formatted);
                },
                year, month, day
        );
        dialog.show(); // show dialog
    }
}
