package com.example.kitty;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class UsageMonitorService extends Service {

    private static final String CHANNEL_ID = "KittyMonitorChannel";
    private static final int NOTIFICATION_ID = 1;
    private Handler handler = new Handler();
    private ArrayList<String> targetPackages;
    private long timeLimitMs = 10 * 1000; // 10 seconds for quick testing

    private Runnable monitorRunnable = new Runnable() {
        @Override
        public void run() {
            checkUsage();
            handler.postDelayed(this, 3000); // Check every 3 seconds
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetPackages = intent.getStringArrayListExtra("TARGET_PACKAGES");
            int minutes = intent.getIntExtra("TIME_LIMIT_MINUTES", 1);
            timeLimitMs = (long) minutes * 60 * 1000;
        }
        
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kitty is watching...")
                .setContentText("Monitoring your app usage")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        
        startForeground(NOTIFICATION_ID, notification);
        handler.post(monitorRunnable);
        
        return START_STICKY;
    }

    private void checkUsage() {
        String foregroundApp = getForegroundApp();
        if (foregroundApp != null && targetPackages != null && targetPackages.contains(foregroundApp)) {
            long usageTime = getTodayUsageTime(foregroundApp);
            if (usageTime > timeLimitMs) {
                showOverlay();
            } else {
                hideOverlay();
            }
        } else {
            hideOverlay();
        }
    }

    private void hideOverlay() {
        if (isOverlayShowing) {
            isOverlayShowing = false;
            stopService(new Intent(this, OverlayService.class));
        }
    }

    private String getForegroundApp() {
        String currentApp = null;
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }
        return currentApp;
    }

    private long getTodayUsageTime(String packageName) {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long startTime = getStartOfDay();
        long endTime = System.currentTimeMillis();
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        for (UsageStats usageStats : stats) {
            if (usageStats.getPackageName().equals(packageName)) {
                return usageStats.getTotalTimeInForeground();
            }
        }
        return 0;
    }

    private long getStartOfDay() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void showOverlay() {
        if (!isOverlayShowing) {
            isOverlayShowing = true;
            Intent overlayIntent = new Intent(this, OverlayService.class);
            overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(overlayIntent);
        }
    }

    public static void setOverlayShowing(boolean showing) {
        isOverlayShowing = showing;
    }

    private static boolean isOverlayShowing = false;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Usage Monitor Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(monitorRunnable);
    }
}
