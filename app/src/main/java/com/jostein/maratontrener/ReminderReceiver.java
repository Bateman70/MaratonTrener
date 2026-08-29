package com.jostein.maratontrener;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "training_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Reschedule alarm for tomorrow to chain exact alarms
        scheduleDailyReminder(context);

        Executors.newSingleThreadExecutor().execute(() -> {
            WorkoutDao dao = WorkoutDatabase.getDatabase(context).workoutDao();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long todayStart = cal.getTimeInMillis();
            long todayEnd = todayStart + 86400000;

            List<WorkoutEntity> allWorkouts = dao.getAllWorkoutsSync();
            WorkoutEntity todaysWorkout = null;

            for (WorkoutEntity w : allWorkouts) {
                if (w.getScheduledDate() >= todayStart && w.getScheduledDate() < todayEnd && !w.isCompleted()) {
                    todaysWorkout = w;
                    break;
                }
            }

            if (todaysWorkout != null) {
                showNotification(context, todaysWorkout);
                sendIphonePush(context, todaysWorkout);
            }
        });
    }

    public static void scheduleDailyReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        android.util.Log.d("ReminderReceiver", "Daily alarm successfully scheduled for: " + calendar.getTime().toString());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (SecurityException e) {
            // Fallback to inexact alarm if exact alarm permission is missing/revoked
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void sendIphonePush(Context context, WorkoutEntity workout) {
        SharedPreferences buddyPrefs = SecurityUtils.getEncryptedPrefs(context, "BuddyPrefs");
        String myId = buddyPrefs.getString("my_id", "CH020721");
        
        String topic = "maratontrener-alerts-" + myId.toLowerCase();
        String title = "Dagens treningsøkt! 🏃‍♂️";
        String message = String.format("Du har planlagt en økt i dag: %s på %.1f km. God tur!", 
                workout.getWorkoutType(), workout.getDistance());
                
        java.net.HttpURLConnection conn = null;
        try {
            java.net.URL url = new java.net.URL("https://ntfy.sh/" + topic);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            conn.setRequestProperty("Title", "=?UTF-8?B?" + android.util.Base64.encodeToString(title.getBytes("UTF-8"), android.util.Base64.NO_WRAP) + "?=");
            conn.setRequestProperty("Tags", "runner,bell");
            
            byte[] outputBytes = message.getBytes("UTF-8");
            java.io.OutputStream os = conn.getOutputStream();
            os.write(outputBytes);
            os.flush();
            os.close();
            
            int responseCode = conn.getResponseCode();
            android.util.Log.d("ReminderReceiver", "ntfy.sh push sent. Response: " + responseCode);
        } catch (Exception e) {
            android.util.Log.e("ReminderReceiver", "Error sending ntfy.sh push: ", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void showNotification(Context context, WorkoutEntity workout) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Training Reminders", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String message = String.format("You have a %s scheduled for today! Don't forget your rum afterwards!", workout.getWorkoutType());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home) // Use existing icon
                .setContentTitle("Time to Train!")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}
