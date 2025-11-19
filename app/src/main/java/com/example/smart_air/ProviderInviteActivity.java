package com.example.smart_air;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_air.Repository.InviteRepository;
import com.example.smart_air.adapter.InvitesAdapter;
import com.example.smart_air.modelClasses.Invite;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProviderInviteActivity extends AppCompatActivity {

    private Button btnGenerateInvite;
    private RecyclerView recyclerViewInvites;
    private ProgressBar progressBar;
    private TextView tvNoInvites;

    private InviteRepository inviteRepository;
    private InvitesAdapter invitesAdapter;
    private List<Invite> invitesList;
    private String parentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_invite);

        parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        inviteRepository = new InviteRepository();
        invitesList = new ArrayList<>();

        initViews();
        setupRecyclerView();
        loadInvites();

        btnGenerateInvite.setOnClickListener(v -> generateNewInvite());
    }

    private void initViews() {
        btnGenerateInvite = findViewById(R.id.btnGenerateInvite);
        recyclerViewInvites = findViewById(R.id.recyclerViewInvites);
        progressBar = findViewById(R.id.progressBar);
        tvNoInvites = findViewById(R.id.tvNoInvites);
    }

    private void setupRecyclerView() {
        invitesAdapter = new InvitesAdapter(invitesList,
                this::onCopyCode,
                this::onRevokeInvite,
                this::onRegenerateInvite);
        recyclerViewInvites.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewInvites.setAdapter(invitesAdapter);
    }

    private void loadInvites() {
        progressBar.setVisibility(View.VISIBLE);
        inviteRepository.getActiveProviderInvites(parentUid).observe(this, invites -> {
            progressBar.setVisibility(View.GONE);
            if (invites != null && !invites.isEmpty()) {
                invitesList.clear();
                invitesList.addAll(invites);
                invitesAdapter.notifyDataSetChanged();
                tvNoInvites.setVisibility(View.GONE);
                recyclerViewInvites.setVisibility(View.VISIBLE);
            } else {
                tvNoInvites.setVisibility(View.VISIBLE);
                recyclerViewInvites.setVisibility(View.GONE);
            }
        });
    }

    private void generateNewInvite() {
        progressBar.setVisibility(View.VISIBLE);
        btnGenerateInvite.setEnabled(false);

        inviteRepository.generateProviderInvite(parentUid, new InviteRepository.OnInviteGeneratedListener() {
            @Override
            public void onSuccess(Invite invite) {
                progressBar.setVisibility(View.GONE);
                btnGenerateInvite.setEnabled(true);
                Toast.makeText(ProviderInviteActivity.this,
                        "Invite code generated: " + invite.getCode(),
                        Toast.LENGTH_LONG).show();

                // Auto-copy to clipboard
                copyToClipboard(invite.getCode());
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnGenerateInvite.setEnabled(true);
                Toast.makeText(ProviderInviteActivity.this,
                        "Error: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCopyCode(Invite invite) {
        copyToClipboard(invite.getCode());
    }

    private void onRevokeInvite(Invite invite) {
        inviteRepository.revokeInviteByCode(invite.getCode(), new InviteRepository.OnInviteOperationListener() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(ProviderInviteActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ProviderInviteActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onRegenerateInvite(Invite invite) {
        progressBar.setVisibility(View.VISIBLE);

        inviteRepository.regenerateProviderInvite(parentUid, invite.getCode(),
                new InviteRepository.OnInviteGeneratedListener() {
                    @Override
                    public void onSuccess(Invite newInvite) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProviderInviteActivity.this,
                                "New code: " + newInvite.getCode(),
                                Toast.LENGTH_LONG).show();
                        copyToClipboard(newInvite.getCode());
                    }

                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProviderInviteActivity.this,
                                "Error: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void copyToClipboard(String code) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Invite Code", code);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}