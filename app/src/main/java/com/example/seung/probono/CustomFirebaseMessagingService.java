package com.example.seung.probono;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Window;
import android.view.WindowManager;

import com.google.firebase.messaging.RemoteMessage;

import java.util.Timer;
import java.util.TimerTask;


public class CustomFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private static final String TAG = "FirebaseMsgService";
    static int count=0;

    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if(count==0) {
            System.out.println("푸쉬 알림 받음");
            sendNotification(remoteMessage.getData().get("message"));
            count++;
        }
    }

    private void sendNotification(String messageBody) {

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        /*
        intent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED) +
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD +
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON +
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                */
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("FCM Push Test")
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());

        //AlarmFragment.vibrator=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        //AlarmFragment.vibrator.vibrate(new long[]{1000,1000,100,1000,100,1000,100,1000}, 0);
        PushWakeLock.acquireCpuWakeLock(this);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                PushWakeLock.releaseCpuLock();
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 500);

        AlarmFragment.alarm=true;

    }
    class Background extends AsyncTask<Void,Void,Void>{
        Context context;
        @Override
        protected void onPostExecute(Void aVoid) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                context = new ContextThemeWrapper(getApplicationContext(), android.R.style.Theme_Holo_Light);
            }
            else {
                context = new ContextThemeWrapper(getApplicationContext(), android.R.style.Theme_Holo_Light);
            }

            new AlertDialog.Builder(context)
                    .setTitle("Alarm")
                    .setMessage("현 위치 화재 발생!\n대피하시길 바랍니다.")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            AlarmFragment.vibrator.cancel();
                        }
                    })
                    .show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }

}
class PushWakeLock {
    static PowerManager.WakeLock sCpuWakeLock;
    static KeyguardManager.KeyguardLock mKeyguardLock;
    static boolean isScreenLock;

    static void acquireCpuWakeLock(Context context) {
        Log.e("PushWakeLock", "Acquiring cpu wake lock");
        Log.e("PushWakeLock", "wake sCpuWakeLock = " + sCpuWakeLock);

        if (sCpuWakeLock != null) {
            return;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        sCpuWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "hello");

        sCpuWakeLock.acquire();
    }

    static void releaseCpuLock() {
        Log.e("PushWakeLock", "Releasing cpu wake lock");
        Log.e("PushWakeLock", "relase sCpuWakeLock = " + sCpuWakeLock);

        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }
}