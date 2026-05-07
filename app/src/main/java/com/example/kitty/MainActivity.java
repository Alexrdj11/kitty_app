package com.example.kitty;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.kitty.databinding.ActivityMainBinding;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();

        binding.startButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                startMonitoring();
            }
        });

        // Check permissions on start
        if (!hasUsageStatsPermission() || !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permissions required for monitoring", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> filteredApps = new ArrayList<>();
        
        for (ApplicationInfo app : packages) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                filteredApps.add(app);
            }
        }

        adapter = new AppAdapter(filteredApps, pm);
        binding.appRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.appRecyclerView.setAdapter(adapter);
    }

    private boolean checkPermissions() {
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Please allow Usage Access", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return false;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow Display over other apps", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return false;
        }
        return true;
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void startMonitoring() {
        if (adapter.getSelectedPackages().isEmpty()) {
            Toast.makeText(this, "Select at least one app", Toast.LENGTH_SHORT).show();
            // Optional: Stop the service if no apps are selected
            stopService(new Intent(this, UsageMonitorService.class));
            return;
        }

        String timeStr = binding.timeLimitInput.getText().toString();
        int minutes = 1;
        if (!timeStr.isEmpty()) {
            try {
                minutes = Integer.parseInt(timeStr);
            } catch (NumberFormatException e) {
                minutes = 1;
            }
        }
        
        Intent serviceIntent = new Intent(this, UsageMonitorService.class);
        serviceIntent.putStringArrayListExtra("TARGET_PACKAGES", new ArrayList<>(adapter.getSelectedPackages()));
        serviceIntent.putExtra("TIME_LIMIT_MINUTES", minutes);
        
        ContextCompat.startForegroundService(this, serviceIntent);

        Toast.makeText(this, "Monitoring updated!", Toast.LENGTH_SHORT).show();
    }
}
