package com.big.chit.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.big.chit.BaseApplication;
import com.big.chit.R;
import com.big.chit.activities.GroupIncomingActivity;
import com.big.chit.activities.IncomingCallScreenActivity;
import com.big.chit.activities.MainActivity;
import com.big.chit.models.LogCall;
import com.big.chit.models.User;
import com.big.chit.utils.Helper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.HashMap;


/**
 * Created by Ussama Iftikhar on 11-Apr-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e("clima", "onMessageReceived");
        Log.d("clima", remoteMessage.getData().toString());
        if (new Helper(this).isLoggedIn()) {
            if (remoteMessage.getData().size() > 0) {
                if (remoteMessage.getData().get("callerId") != null) {
                    if (remoteMessage.getData().get("callStatus") != null) {
                        String callStatus = remoteMessage.getData().get("callStatus");
                        if (callStatus.equals("0"))
                            handleCallNotifications(remoteMessage, false);
                        else if (callStatus.equals("1"))
                            handleCallNotifications(remoteMessage, true);
                        return;
                    }
                }
            }
        }

        if (new Helper(this).isLoggedIn()) {
            Intent intent = new Intent(this, FirebaseChatService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pendingIntent);
            Log.e("clima", "fcm scheduled");
        }
    }

    private void handleCallNotifications(RemoteMessage remoteMessage, boolean isMissedCall) {

        boolean isGroup = remoteMessage.getData().get("isGroupCall").equals("1");
        String callerId = remoteMessage.getData().get("callerId");
        String contactName = "PakOne";

        boolean isVideo = remoteMessage.getData().get("isVideoCall").equals("1");
        String callType = isVideo ? "Video" : "Voice";

        String roomId = remoteMessage.getData().get("channelName");
        String roomToken = remoteMessage.getData().get("token");

        String callerName = "PakOne";
        if (remoteMessage.getData().get("callerId") != null)
            callerName = remoteMessage.getData().get("callerId");

        User userCaller = null;
        HashMap<String, User> myUsers = new Helper(MyFirebaseMessagingService.this).getCacheMyUsers();
        if (myUsers != null && myUsers.containsKey(callerId)) {
            userCaller = myUsers.get(callerId);
            contactName = userCaller.getNameToDisplay();

        } else {
            contactName = callerId;
            userCaller = new User(callerId, callerId, "", "non");
            Log.d("clima user ", userCaller.getId());
        }

        if (isGroup && !isMissedCall) {
            Intent intent = new Intent(MyFirebaseMessagingService.this, GroupIncomingActivity.class);
            Log.d("clima id", callerId);
            callerName = remoteMessage.getData().get("callerName");
            intent.putExtra("is_video", isVideo);
            intent.putExtra("room_token", roomToken);
            intent.putExtra("room_id", roomId);
            intent.putExtra("caller_id", callerId);
            intent.putExtra("caller_name", callerName);
            //todo
            intent.putExtra("key", "dataSnapshot.getKey()");
            if (remoteMessage.getData().get("timeStamp") != null) {
                long date = Long.parseLong(remoteMessage.getData().get("timeStamp")) + 12 * 1000L;
                String text = remoteMessage.getData().get("timeStamp");
                Log.d("clima date", text);
                long currentDate = new Date().getTime();
                Log.d("clima current date", String.valueOf(currentDate));
                Log.d("clima current date 22", String.valueOf(date));
                if (currentDate > date) {
                    Log.d("clima", "return");
                    createMissedCallNotification(callerName, "Gave You " + callType + " Group  Missed Call");
                    return;
                }
            }

            createNotificationForCall(intent, callerName, "Incoming Group " + callType + " Call");
        } else if (!isGroup && !isMissedCall) {
            Intent intent = new Intent(MyFirebaseMessagingService.this, IncomingCallScreenActivity.class);

            Log.d("clima id", callerId);
            intent.putExtra("is_video", isVideo);
            intent.putExtra("room_token", roomToken);
            intent.putExtra("room_id", roomId);
            intent.putExtra("caller_id", callerId);
            //todo
            intent.putExtra("key", "dataSnapshot.getKey()");
            intent.putExtra("user", userCaller);

            boolean isAddedInCall = false;
            if (remoteMessage.getData().get("isAddedInCall") != null) {
                String text = remoteMessage.getData().get("isAddedInCall");
                isAddedInCall = text.equals("1");
            }

            if (remoteMessage.getData().get("timeStamp") != null) {
                long date = Long.parseLong(remoteMessage.getData().get("timeStamp")) + 12 * 1000L;
                String text = remoteMessage.getData().get("timeStamp");
                Log.d("clima date", text);
                long currentDate = new Date().getTime();
                Log.d("clima current date", String.valueOf(currentDate));
                Log.d("clima current date 22", String.valueOf(date));
                if (currentDate > date) {
                    Log.d("clima", "return");
                    if (isAddedInCall) {
                        createMissedCallNotification(contactName, "Was added you in a " + callType + " Call");
                    }
                    return;
                }
            }


            if (!isAddedInCall)
                createNotificationForCall(intent, contactName, "Incoming " + callType + " Call");
            else
                createNotificationForCall(intent, contactName, "Added  you in a " + callType + " Call");
        }


        if (isMissedCall && !isGroup)
            createMissedCallNotification(contactName, "Gave You " + callType + "  Missed Call");
//        else if(isMissedCall && isGroup)
//        createMissedCallNotification(callerName, "Gave You " + callType + " Group  Missed Call");
    }

    public void createMissedCallNotification(String contactName, String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 56, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManagerCompat.from(MyFirebaseMessagingService.this).cancel(89);
        //NotificationManagerCompat.from(MyFirebaseMessagingService.this).;
        Uri ringUri = Settings.System.DEFAULT_RINGTONE_URI;

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MyFirebaseMessagingService.this, BaseApplication.CALL)
                .setContentTitle(contactName)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_baseline_phone_missed_24)
                //.setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setDefaults(Notification.DEFAULT_SOUND)

                .setAutoCancel(true)
                //.setSound(ringUri)
                .setContentIntent(pendingIntent);
        //.setFullScreenIntent(pendingIntent, true);
        Notification incomingCallNotification = notificationBuilder.build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(29, incomingCallNotification);

        LogCall logCall = null;
        //  if (userCaller != null && !isGroup) {
//                rChatDb.beginTransaction();
//                logCall = new LogCall(userCaller, System.currentTimeMillis(), 0, false, "cause.toString()", userMe.getId(), userCaller.getId());
//                rChatDb.copyToRealm(logCall);
//                rChatDb.commitTransaction();
        //    }


    }

    private void createNotificationForCall(Intent intent, String callerName, String message) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addNextIntentWithParentStack(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(92, PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent pendingIntent = PendingIntent.getActivity(MyFirebaseMessagingService.this,92,intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri ringUri = Settings.System.DEFAULT_RINGTONE_URI;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MyFirebaseMessagingService.this, BaseApplication.CALL)
                .setContentTitle(callerName)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_logo_)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setBadgeIconType(R.drawable.ic_logo_)
                .setAutoCancel(true)
                .setTimeoutAfter(12000)
                .setFullScreenIntent(pendingIntent, false)
                //.setSound(ringUri)
                .setContentIntent(pendingIntent);
        //           .addAction(R.drawable.camera_icon,"Decline", pendingIntent)
        //         .addAction(R.drawable.camera_icon,"Answer", pendingIntent)
        //.setFullScreenIntent(pendingIntent, true);
        Notification incomingCallNotification = notificationBuilder.build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(89, incomingCallNotification);
    }
}
