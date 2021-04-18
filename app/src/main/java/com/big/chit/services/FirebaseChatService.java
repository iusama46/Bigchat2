package com.big.chit.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.big.chit.BaseApplication;
import com.big.chit.R;
import com.big.chit.activities.ChatActivity;
import com.big.chit.activities.GroupIncomingActivity;
import com.big.chit.activities.IncomingCallScreenActivity;
import com.big.chit.activities.MainActivity;
import com.big.chit.models.Attachment;
import com.big.chit.models.AttachmentList;
import com.big.chit.models.AttachmentTypes;
import com.big.chit.models.Chat;
import com.big.chit.models.Group;
import com.big.chit.models.LogCall;
import com.big.chit.models.Message;
import com.big.chit.models.MessageNewArrayList;
import com.big.chit.models.MyString;
import com.big.chit.models.Status;
import com.big.chit.models.StatusImage;
import com.big.chit.models.StatusImageList;
import com.big.chit.models.StatusImageNew;
import com.big.chit.models.StatusNew;
import com.big.chit.models.User;
import com.big.chit.models.solochat;
import com.big.chit.utils.FirebaseUploader;
import com.big.chit.utils.Helper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * BackGround Calling Support Added by Ussama Iftikhar on 12-April-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */

public class FirebaseChatService extends Service {
    private static final String CHANNEL_ID_MAIN = "my_channel_01";
    private static final String CHANNEL_ID_GROUP = "my_channel_02";
    private static final String CHANNEL_ID_USER = "my_channel_03";
    String replyId = "0";
    private Helper helper;
    private String myId, msgUserID = "";
    private Realm rChatDb;
    private HashMap<String, User> userHashMap = new HashMap<>();
    private HashMap<String, Group> groupHashMap = new HashMap<>();
    private HashMap<String, Message> statusHashMap = new HashMap<>();
    private User userMe;
    private StatusImage statusImage;
    private Status status;
    private ArrayList<MessageNewArrayList> messageArrayList = new ArrayList<>();
    //private RealmList<MessageNew> messageArrayList = new RealmList<>();
    private int count = 0;
    private int i = 0;
    private BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopForeground(true);
            stopSelf();
        }
    };
    private BroadcastReceiver uploadAndSendReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Helper.UPLOAD_AND_SEND)) {
                Group group = null;
                Attachment attachment = intent.getParcelableExtra("attachment");
                if (intent.getParcelableExtra("chatDataGroup") != null) {
                    group = intent.getParcelableExtra("chatDataGroup");
                }
                int type = intent.getIntExtra("attachment_type", -1);
                String attachmentFilePath = intent.getStringExtra("attachment_file_path");
                String attachmentChatChild = intent.getStringExtra("attachment_chat_child");
                String attachmentRecipientId = intent.getStringExtra("attachment_recipient_id");
                replyId = intent.getStringExtra("attachment_reply_id");
                msgUserID = intent.getStringExtra("new_msg_id");
//                uploadAndSend(new File(attachmentFilePath), attachment, type, attachmentChatChild,
//                        attachmentRecipientId, group, msgUserID);
                uploadAndSend(new File(attachmentFilePath), attachment, type, attachmentChatChild,
                        attachmentRecipientId, group, msgUserID, intent.getStringExtra("statusUrl"));
            }
        }
    };


    private ChildEventListener chatUpdateListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            //  if (!dataSnapshot.getKey().equalsIgnoreCase("chatDelete")) {
            Message message = dataSnapshot.getValue(Message.class);

           /* if (message.isDelivered() || (message.getRecipientId().startsWith(Helper.GROUP_PREFIX) && !groupHashMap.containsKey(message.getRecipientId())))
                return;  */
            if (message != null && message.getId() != null) {
                Message result = rChatDb.where(Message.class).equalTo("id", message.getId()).findFirst();
                if (result == null && !TextUtils.isEmpty(myId) && helper.isLoggedIn()) {

                    if (!message.getRecipientId().startsWith(Helper.GROUP_PREFIX) && message.isBlocked()
                            && message.getSenderId().equalsIgnoreCase(userMe.getId()))
                        saveMessage(message);
                    else if (!message.getRecipientId().startsWith(Helper.GROUP_PREFIX) && !message.isBlocked())
                        saveMessage(message);
                    else if (message.getRecipientId().startsWith(Helper.GROUP_PREFIX))
                        saveMessage(message);

                    if (!message.getRecipientId().startsWith(Helper.GROUP_PREFIX) &&
                            !message.getSenderId().equals(myId) && !message.isDelivered() && !message.isBlocked())
                        BaseApplication.getChatRef().child(dataSnapshot.getRef().getParent().getKey())
                                .child(message.getId()).child("delivered").setValue(true);
                    /*saveMessage(message);
                    if (!message.getRecipientId().startsWith(Helper.GROUP_PREFIX) && !message.getSenderId().equals(myId) && !message.isDelivered())
                        BaseApplication.getChatRef().child(dataSnapshot.getRef().getParent().getKey()).child(message.getId()).child("delivered").setValue(true);*/
                }
            }
            // }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            //  if (!dataSnapshot.getKey().equalsIgnoreCase("chatDelete")) {
            final Message message = dataSnapshot.getValue(Message.class);
            if (message != null && message.getId() != null) {
                final Message result = rChatDb.where(Message.class).equalTo("id", message.getId()).findFirst();
                if (result != null) {
                    //  rChatDb.beginTransaction();
                    //Log.d("clima","hello 2chnaged");
                    //startActivity(new Intent(FirebaseChatService.this, DelMainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    //Toast.makeText(FirebaseChatService.this, "chnaged", Toast.LENGTH_SHORT).show();
                    rChatDb.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            result.setReadMsg(message.isReadMsg());
                            result.setDelivered(message.isDelivered());
                            result.setDelete(message.getDelete());
                            if (message.getUserIds() != null) {
                                ArrayList<String> userIds = new ArrayList<>();
                                userIds.addAll(message.getUserIds());
                                result.setUserIds(userIds);
                            }
                        }
                    });

                    // rChatDb.commitTransaction();

                }
            }
            //}
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            // if (!dataSnapshot.getKey().equalsIgnoreCase("chatDelete")) {
            Message message = dataSnapshot.getValue(Message.class);
            if (message != null && message.getId() != null) {
                Helper.deleteMessageFromRealm(rChatDb, message.getId());

                String userOrGroupId = myId.equals(message.getSenderId()) ? message.getRecipientId() : message.getSenderId();
                final Chat chat = Helper.getChat(rChatDb, myId, userOrGroupId).findFirst();
                if (chat != null) {
                    //rChatDb.beginTransaction();
                    rChatDb.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RealmList<Message> realmList = chat.getMessages();
                            if (realmList.size() == 0)
                                RealmObject.deleteFromRealm(chat);
                            else {
                                chat.setLastMessage(realmList.get(realmList.size() - 1).getBody());
                                chat.setTimeUpdated(realmList.get(realmList.size() - 1).getDate());
                            }
                        }
                    });

                    // rChatDb.commitTransaction();
                }
            }
            ///  }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };
    private ChildEventListener chatStatusListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            final MessageNewArrayList messageNewArrayList = dataSnapshot.getValue(MessageNewArrayList.class);

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    public FirebaseChatService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_MAIN, "Dreams chat service", NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            try {
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_MAIN)
                .setSmallIcon(R.drawable.ic_logo_)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_name))
                .setSound(null)
                .build();
        startForeground(1, notification);
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadAndSendReceiver, new IntentFilter(Helper.UPLOAD_AND_SEND));
        LocalBroadcastManager.getInstance(
                this).registerReceiver(logoutReceiver, new IntentFilter(Helper.BROADCAST_LOGOUT));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerCallUpdates() {
        BaseApplication.getCallRef().child(userMe.getId()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Log.d("clima", dataSnapshot.getKey());

                if (dataSnapshot.getChildrenCount() == 8) {
                    if (dataSnapshot.child("call_status").getValue() != null) {
                        int callStatus = Integer.parseInt(String.valueOf(dataSnapshot.child("call_status").getValue()));

                        if (callStatus == 0) {
                            handleCallNotifications(dataSnapshot, false);
                        } else if (callStatus == 1) {
                            Log.d("clima", "missed call");
                            handleCallNotifications(dataSnapshot, true);
                            dataSnapshot.getRef().child("call_status").setValue(5);
                        }
                    }
                } else {
                    dataSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d("clima", "changed");
                Log.d("clima", dataSnapshot.getKey());

                if (dataSnapshot.getChildrenCount() == 8) {
                    if (dataSnapshot.child("call_status").getValue() != null) {
                        int callStatus = Integer.parseInt(String.valueOf(dataSnapshot.child("call_status").getValue()));

                        if (callStatus == 0) {
                            //showCallNotifications(dataSnapshot, false);
                            dataSnapshot.getRef().child("call_status").setValue(5);
                        } else if (callStatus == 1) {
                            Log.d("clima", "missed call");
                            handleCallNotifications(dataSnapshot, true);
                            dataSnapshot.getRef().child("call_status").setValue(5);
                        }
                    }
                } else {
                    dataSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void handleCallNotifications(DataSnapshot dataSnapshot, boolean isMissedCall) {

        boolean isGroup = (boolean) dataSnapshot.child("is_group").getValue();
        String callerId = (String) dataSnapshot.child("caller_id").getValue();
        String contactName = "PakOne";

        boolean isVideo = (boolean) dataSnapshot.child("is_video").getValue();
        String callType = isVideo ? "Video" : "Voice";

        String roomId = (String) dataSnapshot.child("channel_id").getValue();
        String roomToken = (String) dataSnapshot.child("channel_token").getValue();
        String callerName = (String) dataSnapshot.child("caller_name").getValue();

        User userCaller = null;
        HashMap<String, User> myUsers = helper.getCacheMyUsers();
        if (myUsers != null && myUsers.containsKey(callerId)) {
            userCaller = myUsers.get(callerId);
            contactName = userCaller.getNameToDisplay();
        } else {
            contactName = callerId;
        }

        if (isGroup && !isMissedCall) {
            Intent intent = new Intent(FirebaseChatService.this, GroupIncomingActivity.class);
            Log.d("clima id", callerId);
            intent.putExtra("is_video", isVideo);
            intent.putExtra("room_token", roomToken);
            intent.putExtra("room_id", roomId);
            intent.putExtra("caller_id", callerId);
            intent.putExtra("caller_name", callerName);
            intent.putExtra("key", dataSnapshot.getKey());
            createNotificationForCall(intent, callerName, "Incoming Group " + callType + " Call");
        } else if (!isGroup && !isMissedCall) {
            Intent intent = new Intent(FirebaseChatService.this, IncomingCallScreenActivity.class);

            Log.d("clima id", callerId);

            intent.putExtra("is_video", isVideo);
            intent.putExtra("room_token", roomToken);
            intent.putExtra("room_id", roomId);
            intent.putExtra("caller_id", callerId);
            intent.putExtra("key", dataSnapshot.getKey());

            createNotificationForCall(intent, contactName, "Incoming " + callType + " Call");
        }


        if (isMissedCall) {

            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 56, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            Uri ringUri = Settings.System.DEFAULT_RINGTONE_URI;

            String missedCallName = isGroup ? callerName : contactName;
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(FirebaseChatService.this, BaseApplication.CALL)
                    .setContentTitle(missedCallName)
                    .setContentText("Gave You " + callType + "  Missed Call")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setDefaults(Notification.DEFAULT_ALL)
                    //.addAction(R.drawable.camera_icon, getString(R.string.app_name), pendingIntent)
//                            .addAction(R.drawable.ic_call_accept, getString(R.string.answer_call), receiveCallPendingIntent)
                    .setAutoCancel(true)
                    .setSound(ringUri)
                    .setFullScreenIntent(pendingIntent, true);
            Notification incomingCallNotification = notificationBuilder.build();
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify(89, incomingCallNotification);

            LogCall logCall = null;
            if (userCaller != null && !isGroup) {
                //    userCaller = new User(callerId, callerId, getString(R.string.app_name), "");
                rChatDb.beginTransaction();
                logCall = new LogCall(userCaller, System.currentTimeMillis(), 0, false, "cause.toString()", userMe.getId(), userCaller.getId());
                rChatDb.copyToRealm(logCall);
                rChatDb.commitTransaction();
            }

        }
    }

    private void createNotificationForCall(Intent intent, String callerName, String message) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addNextIntentWithParentStack(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(92, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri ringUri = Settings.System.DEFAULT_RINGTONE_URI;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(FirebaseChatService.this, BaseApplication.CALL)
                .setContentTitle(callerName)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                //.setSound(ringUri)
                .setFullScreenIntent(pendingIntent, true);
        Notification incomingCallNotification = notificationBuilder.build();
        //incomingCallNotification.notify();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(89, incomingCallNotification);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!User.validate(userMe)) {
            initVars();
            if (User.validate(userMe)) {
                myId = userMe.getId();
                rChatDb = Helper.getRealmInstance();
                registerUserUpdates();
                registerGroupUpdates();
                registerStatusUpdates();
                registerCallUpdates();
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        == PackageManager.PERMISSION_GRANTED) {
                    if (!FetchMyUsersService.STARTED) {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            firebaseUser.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                @Override
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        String idToken = task.getResult().getToken();
                                        FetchMyUsersService.startMyUsersService(FirebaseChatService.this, userMe.getId(), idToken);
                                    }
                                }
                            });
                        }
                    }
                }
            } else {
                stopForeground(true);
                stopSelf();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initVars() {
        helper = new Helper(this);


        Realm.init(this);

        userMe = helper.getLoggedInUser();

    }

    private void restartService() {
        if (new Helper(this).isLoggedIn()) {
            Intent intent = new Intent(this, FirebaseChatService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pendingIntent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadAndSendReceiver);
        restartService();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        restartService();
        super.onTaskRemoved(rootIntent);
    }

    private void uploadAndSend(final File fileToUpload, final Attachment attachment, final int attachmentType,
                               final String chatChild, final String recipientId, final Group group,
                               final String new_msg_id, final String statusUrl) {
        if (!fileToUpload.exists())
            return;
        final String fileName = Uri.fromFile(fileToUpload).getLastPathSegment();
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child(getString(R.string.app_name)).child(AttachmentTypes.getTypeName(attachmentType)).child(fileName);
        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //If file is already uploaded
                Attachment attachment1 = attachment;
                if (attachment1 == null) attachment1 = new Attachment();
                attachment1.setName(fileName);
                attachment1.setUrl(uri.toString());
                attachment1.setBytesCount(fileToUpload.length());
                sendMessage(null, attachmentType, attachment1, chatChild, recipientId, group, new_msg_id, statusUrl);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                //Elase upload and then send message
                FirebaseUploader firebaseUploader = new FirebaseUploader(new FirebaseUploader.UploadListener() {
                    @Override
                    public void onUploadFail(String message) {
                        Log.e("DatabaseException", message);
                    }

                    @Override
                    public void onUploadSuccess(String downloadUrl) {
                        Attachment attachment1 = attachment;
                        if (attachment1 == null) attachment1 = new Attachment();
                        attachment1.setName(fileToUpload.getName());
                        attachment1.setUrl(downloadUrl);
                        attachment1.setBytesCount(fileToUpload.length());
                        sendMessage(null, attachmentType, attachment1, chatChild, recipientId, group, new_msg_id, statusUrl);
                    }

                    @Override
                    public void onUploadProgress(int progress) {

                    }

                    @Override
                    public void onUploadCancelled() {

                    }
                }, storageReference);
                firebaseUploader.uploadOthers(getApplicationContext(), fileToUpload);
            }
        });
    }

    private void sendMessage(String messageBody, @AttachmentTypes.AttachmentType int attachmentType,
                             Attachment attachment, String chatChild, String userOrGroupId, Group group,
                             String new_msg_id, String statusUrl) {
        //Create message object
        Helper.deleteMessageFromRealm(rChatDb, new_msg_id);
        Message message = new Message();
        message.setAttachmentType(attachmentType);
        if (attachmentType != AttachmentTypes.NONE_TEXT)
            message.setAttachment(attachment);
        message.setBody(messageBody);
        message.setDate(System.currentTimeMillis());
        message.setSenderId(userMe.getId());
        message.setSenderName(userMe.getName());
        message.setSent(true);
        message.setDelivered(false);
        message.setRecipientId(userOrGroupId);
        message.setId(BaseApplication.getChatRef().child(chatChild).push().getKey());
        message.setReplyId(replyId);
        message.setStatusUrl(statusUrl);
        if (group != null && group.getUserIds() != null) {
            ArrayList<String> userIds = new ArrayList<>();
            for (String user : group.getUserIds()) {
                if (group.getGrpExitUserIds() == null) {
                    userIds.add(user);
                } else if (group.getGrpExitUserIds() != null && !group.getGrpExitUserIds().contains(user))
                    userIds.add(user);
            }
            message.setUserIds(userIds);
        }
        if (!userOrGroupId.startsWith(Helper.GROUP_PREFIX) && userHashMap.get(userOrGroupId).getBlockedUsersIds() != null
                && userHashMap.get(userOrGroupId).getBlockedUsersIds().contains(userMe.getId())) {
            message.setBlocked(true);
        }

        //Add messages in chat child
        BaseApplication.getChatRef().child(chatChild).child(message.getId()).setValue(message);
    }

    private void registerGroupUpdates() {
        BaseApplication.getGroupRef().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                try {
                    Group group = dataSnapshot.getValue(Group.class);
                    if (Group.validate(group) && group.getUserIds().contains(myId)) {
                        if (!groupHashMap.containsKey(group.getId())) {
                            groupHashMap.put(group.getId(), group);
                            broadcastGroup("added", group);
                            checkAndNotify(group);
                            registerChatUpdates(true, group.getId());
                        }
                    }
                } catch (Exception ex) {
                    Log.e("GROUP", "invalid group");
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                try {
                    Group group = dataSnapshot.getValue(Group.class);
                    if (Group.validate(group)) {
                        if (group.getUserIds().contains(myId)) {
                            if (!groupHashMap.containsKey(group.getId()))
                                groupHashMap.put(group.getId(), group);
                            registerChatUpdates(true, group.getId());
                            broadcastGroup("changed", group);
                            updateGroupInDb(group);
                        } else if (groupHashMap.containsKey(group.getId())) {
                            registerChatUpdates(false, group.getId());
                            groupHashMap.remove(group.getId());
                            broadcastGroup("changed", group);
                            updateGroupInDb(group);
                        }
                    }
                } catch (Exception ex) {
                    Log.e("GROUP", "invalid group");
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.e("GROUP", "GROUPRemoved");
                try {
                    Group group = dataSnapshot.getValue(Group.class);
                    if (groupHashMap.containsKey(group.getId())) {
                        registerChatUpdates(false, group.getId());
                        groupHashMap.remove(group.getId());
                        broadcastGroup("changed", group);
                        if (group != null && group.getId() != null) {
                            Helper.deleteGroupFromRealm(rChatDb, group.getId());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void checkAndNotify(final Group group) {
        rChatDb.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Chat thisGroupChat = rChatDb.where(Chat.class)
                        .equalTo("myId", myId)
                        .equalTo("groupId", group.getId()).findFirst();
                if (thisGroupChat == null) {
                    if (!group.getUserIds().get(0).equals(new MyString(myId)) && !helper.getSharedPreferenceHelper().getBooleanPreference(Helper.GROUP_NOTIFIED, false)) { //if i am not admin and have'nt notified yet
                        notifyNewGroup(group);
                        helper.getSharedPreferenceHelper().setBooleanPreference(Helper.GROUP_NOTIFIED, true);
                    }
                    thisGroupChat = rChatDb.createObject(Chat.class);
                    thisGroupChat.setGroup(rChatDb.copyToRealm(groupHashMap.get(group.getId())));
                    thisGroupChat.setGroupId(group.getId());
                    thisGroupChat.setMessages(new RealmList<Message>());
                    thisGroupChat.setMyId(myId);
                    thisGroupChat.setRead(false);
                    long millis = System.currentTimeMillis();
                    thisGroupChat.setLastMessage("Created on " + Helper.getDateTime(millis));
                    thisGroupChat.setTimeUpdated(millis);
                }
            }
        });
    }

    private void notifyNewGroup(Group group) {
        // Construct the Intent you want to end up at
        Intent chatActivity = ChatActivity.newIntent(this, null, group);
        // Construct the PendingIntent for your Notification
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // This uses android:parentActivityName and
        // android.support.PARENT_ACTIVITY meta-data by default
        stackBuilder.addNextIntentWithParentStack(chatActivity);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(99, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_GROUP, "new group notification", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_GROUP);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSmallIcon(R.drawable.ic_logo_)
                .setContentTitle("Group: " + group.getName())
                .setContentText("You have been added to new group called " + group.getName())
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        int msgId = Integer.parseInt(group.getId().substring(group.getId().length() - 4, group.getId().length() - 1));
        notificationManager.notify(msgId, notificationBuilder.build());
    }

    private void registerUserUpdates() {
        BaseApplication.getUserRef().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                try {
                    User user = dataSnapshot.getValue(User.class);
                    if (User.validate(user)) {
                        if (!userHashMap.containsKey(user.getId())) {
                            userHashMap.put(user.getId(), user);
                            broadcastUser("added", user);
                            registerChatUpdates(true, user.getId());
                        }
                    }
                } catch (Exception ex) {
                    Log.e("USER", "invalid user");
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                try {
                    User user = dataSnapshot.getValue(User.class);
                    if (User.validate(user)) {
                        if (userHashMap.containsKey(user.getId()))
                            userHashMap.put(user.getId(), user);
                        broadcastUser("changed", user);
                        updateUserInDb(user);
                    }
                } catch (Exception ex) {
                    Log.e("USER", "invalid user");
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void registerStatusUpdates() {
        // messageArrayList.clear();
        BaseApplication.getStatusRef().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                try {
                    Log.e("start", "start");
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        final MessageNewArrayList messageNewArrayList = d.getValue(MessageNewArrayList.class);

                        DatabaseReference databaseReference = BaseApplication.getUserRef().child(messageNewArrayList.getSenderId());
                        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                final User user = dataSnapshot.getValue(User.class);

                                rChatDb.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        MessageNewArrayList message = messageNewArrayList;
                                        StatusNew statusQuery =
                                                rChatDb.where(StatusNew.class).equalTo("userId", message.getSenderId()).findFirst();
                                        if (statusQuery == null) {
                                            StatusImageNew statusImage = rChatDb.createObject(StatusImageNew.class);
                                            statusImage.setAttachmentType(message.getAttachmentType());
                                            AttachmentList attachment1 = rChatDb.createObject(AttachmentList.class);
                                            attachment1.setBytesCount(message.getAttachment().getBytesCount());
                                            attachment1.setData(message.getAttachment().getData());
                                            attachment1.setName(message.getAttachment().getName());

                                            RealmList<StatusImageList> realmList = new RealmList<>();
                                            for (int i = 0; i < message.getAttachment().getUrlList().size(); i++) {
                                                StatusImageList statusImageList = rChatDb.createObject(StatusImageList.class);
                                                statusImageList.setUrl(message.getAttachment().getUrlList().get(i).getUrl());
                                                statusImageList.setExpiry(message.getAttachment().getUrlList().get(i).isExpiry());
                                                statusImageList.setUploadTime(message.getAttachment().getUrlList().get(i).getUploadTime());
                                                realmList.add(statusImageList);
                                            }

                                            attachment1.setUrlList(realmList);
                                            statusImage.setAttachment(attachment1);
                                            statusImage.setBody(message.getBody());
                                            statusImage.setDate(message.getDate());
                                            statusImage.setSenderId(message.getSenderId());
                                            statusImage.setSenderName(message.getSenderName());
                                            statusImage.setSent(false);
                                            statusImage.setDelivered(false);
                                            statusImage.setId(message.getId());

                                            StatusNew status = rChatDb.createObject(StatusNew.class);
                                            status.getStatusImages().add(statusImage);
                                            status.setLastMessage(message.getBody());
                                            status.setMyId(message.getId());
                                            status.setUser(rChatDb.copyToRealm(user));
                                            status.setUserId(message.getSenderId());
                                            status.setTimeUpdated(message.getDate());
                                            status.setLastMessage(message.getBody());
                                        } else {
                                            AttachmentList attachment1 = statusQuery.getStatusImages().get(0).getAttachment();

                                            RealmList<StatusImageList> realmList = new RealmList<>();
                                            for (int i = 0; i < message.getAttachment().getUrlList().size(); i++) {
                                                StatusImageList statusImageList = rChatDb.createObject(StatusImageList.class);
                                                statusImageList.setUrl(message.getAttachment().getUrlList().get(i).getUrl());
                                                statusImageList.setExpiry(message.getAttachment().getUrlList().get(i).isExpiry());
                                                statusImageList.setUploadTime(message.getAttachment().getUrlList().get(i).getUploadTime());
                                                realmList.add(statusImageList);
                                            }
                                            attachment1.setUrlList(realmList);
                                            attachment1.setName(message.getAttachment().getName());
                                            statusQuery.getStatusImages().get(0).setAttachment(attachment1);
                                            statusQuery.setTimeUpdated(message.getDate());
                                            statusQuery.setStatusImages(statusQuery.getStatusImages());

                                        }
                                        //    Log.e("i==>>", "" + i);
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.i("TAG_ERROR", databaseError.getMessage());
                            }
                        });
                        // }
                    }
                    Log.e("end", "end");
                } catch (Exception ex) {
                    Log.e("Status==>>", ex.getMessage());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d("onChildChanged=>>", "onChildChanged");
                try {
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        final MessageNewArrayList message = d.getValue(MessageNewArrayList.class);

                        DatabaseReference databaseReference = BaseApplication.getUserRef().child(message.getSenderId());
                        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                final User user = dataSnapshot.getValue(User.class);

                                rChatDb.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        StatusNew statusQuery = rChatDb.where(StatusNew.class).equalTo("userId", message.getSenderId()).findFirst();
                                        if (statusQuery == null) {
                                            StatusImageNew statusImage = rChatDb.createObject(StatusImageNew.class);
                                            statusImage.setAttachmentType(message.getAttachmentType());
                                            AttachmentList attachment1 = rChatDb.createObject(AttachmentList.class);
                                            attachment1.setBytesCount(message.getAttachment().getBytesCount());
                                            attachment1.setData(message.getAttachment().getData());
                                            attachment1.setName(message.getAttachment().getName());

                                            RealmList<StatusImageList> realmList = new RealmList<>();
                                            for (int i = 0; i < message.getAttachment().getUrlList().size(); i++) {
                                                StatusImageList statusImageList = rChatDb.createObject(StatusImageList.class);
                                                statusImageList.setUrl(message.getAttachment().getUrlList().get(i).getUrl());
                                                statusImageList.setExpiry(message.getAttachment().getUrlList().get(i).isExpiry());
                                                statusImageList.setUploadTime(message.getAttachment().getUrlList().get(i).getUploadTime());
                                                realmList.add(statusImageList);
                                            }
                                            attachment1.setUrlList(realmList);
                                            statusImage.setAttachment(attachment1);
                                            statusImage.setBody(message.getBody());
                                            statusImage.setDate(message.getDate());
                                            statusImage.setSenderId(message.getSenderId());
                                            statusImage.setSenderName(message.getSenderName());
                                            statusImage.setSent(false);
                                            statusImage.setDelivered(false);
                                            statusImage.setId(message.getId());

                                            StatusNew status = rChatDb.createObject(StatusNew.class);
                                            status.getStatusImages().add(statusImage);
                                            status.setLastMessage(message.getBody());
                                            status.setMyId(message.getId());
                                            status.setUser(rChatDb.copyToRealm(user));
                                            status.setUserId(message.getSenderId());
                                            status.setTimeUpdated(message.getDate());
                                            status.setLastMessage(message.getBody());
                                        } else {
                                            AttachmentList attachment1 = statusQuery.getStatusImages().get(0).getAttachment();

                                            RealmList<StatusImageList> realmList = new RealmList<>();
                                            Log.e("size", "" + message.getAttachment().getUrlList().size());
                                            for (int i = 0; i < message.getAttachment().getUrlList().size(); i++) {
                                                StatusImageList statusImageList = rChatDb.createObject(StatusImageList.class);
                                                statusImageList.setUrl(message.getAttachment().getUrlList().get(i).getUrl());
                                                statusImageList.setExpiry(message.getAttachment().getUrlList().get(i).isExpiry());
                                                statusImageList.setUploadTime(message.getAttachment().getUrlList().get(i).getUploadTime());
                                                realmList.add(statusImageList);
                                            }
                                            attachment1.setUrlList(realmList);
                                            attachment1.setName(message.getAttachment().getName());
                                            statusQuery.getStatusImages().get(0).setAttachment(attachment1);
                                            statusQuery.setTimeUpdated(message.getDate());
                                            statusQuery.setStatusImages(statusQuery.getStatusImages());
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.i("TAG_ERROR", databaseError.getMessage());
                            }
                        });

                    }
                } catch (Exception ex) {
                    Log.e("Status==>>", ex.getMessage());
                }

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateUserInDb(final User value) {
//update in database
        if (!TextUtils.isEmpty(myId)) {
            if (myId.equalsIgnoreCase(value.getId())) {
                final Chat chat = rChatDb.where(Chat.class).equalTo("myId", myId).findFirst();
                if (chat.getUser() != null) {
                    rChatDb.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            ArrayList<solochat> solochats = new ArrayList<>();
                            solochats.addAll(value.getSolochat());
                            chat.getUser().setSolochat(solochats);
// helper.setLoggedInUser(chat.getUser());
                        }
                    });
                }
// User updated = rChatDb.copyToRealm(value);
// chat.setUser(updated);
            } else {
                final Chat chat = rChatDb.where(Chat.class).equalTo("myId", myId).equalTo("userId", value.getId()).findFirst();
                if (chat != null) {
// rChatDb.beginTransaction();
                    rChatDb.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            User updated = rChatDb.copyToRealm(value);
                            updated.setNameInPhone(chat.getUser().getNameInPhone());
                            chat.setUser(updated);
                        }
                    });

//rChatDb.commitTransaction();
                }
                final StatusNew statusQuery =
                        rChatDb.where(StatusNew.class).equalTo("userId", value.getId()).findFirst();
                if (statusQuery != null) {
                    rChatDb.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            User updated = rChatDb.copyToRealm(value);
                            updated.setNameInPhone(statusQuery.getUser().getNameInPhone());
                            statusQuery.setUser(updated);
                        }
                    });
                }
            }

            final Chat chat = rChatDb.where(Chat.class).equalTo("myId", myId).findFirst();
            if (chat.getUser() != null) {
                rChatDb.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {

                        ArrayList<String> blockedUsers = new ArrayList<>();
                        blockedUsers.addAll(value.getBlockedUsersIds());
                        chat.getUser().setBlockedUsersIds(blockedUsers);
                    }
                });
            }

        }
    }

    private void updateGroupInDb(final Group group) {
        if (!TextUtils.isEmpty(myId)) {
            final Chat chat = rChatDb.where(Chat.class).equalTo("myId", myId).equalTo("groupId", group.getId()).findFirst();
            if (chat != null) {
                //rChatDb.beginTransaction();
                rChatDb.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        if (group.getUserIds() != null && group.getUserIds().contains(userMe.getId())) {
                            chat.setGroup(rChatDb.copyToRealm(group));
                        } else {
                            chat.setGroup(null);
                        }
                    }
                });

                // rChatDb.commitTransaction();
            }
        }
    }

    //Status

    private void registerChatUpdates(boolean register, String id) {
        if (!TextUtils.isEmpty(myId) && !TextUtils.isEmpty(id)) {
            // DatabaseReference idChatRef = BaseApplication.getChatRef().child(id.startsWith(Helper.GROUP_PREFIX) ? id : Helper.getChatChild(myId, id));

            DatabaseReference idChatRef = BaseApplication.getChatRef().child(id.startsWith(Helper.GROUP_PREFIX) ? id : myId + "-" + id);
            DatabaseReference idChatRef1 = BaseApplication.getChatRef().child(id.startsWith(Helper.GROUP_PREFIX) ? id : id + "-" + myId);
            if (register) {
                idChatRef.addChildEventListener(chatUpdateListener);
                idChatRef1.addChildEventListener(chatUpdateListener);
            } else {
                idChatRef.removeEventListener(chatUpdateListener);
                idChatRef1.removeEventListener(chatUpdateListener);
            }
        }
    }

//    private void registerCallUpdates(boolean register) {
//
//        // DatabaseReference idChatRef = BaseApplication.getChatRef().child(id.startsWith(Helper.GROUP_PREFIX) ? id : Helper.getChatChild(myId, id));
//
//        DatabaseReference idChatRef = BaseApplication.getCallRef().child(userMe.getId());
//        //DatabaseReference idChatRef1 = BaseApplication.getChatRef().child(id.startsWith(Helper.GROUP_PREFIX) ? id : id + "-" + myId);
//        if (register) {
//            idChatRef.addChildEventListener(callUpdateListner);
//            //idChatRef1.addChildEventListener(chatUpdateListener);
//        } else {
//            idChatRef.removeEventListener(callUpdateListner);
//            //idChatRef1.removeEventListener(chatUpdateListener);
//        }
//
//    }

    private void registerStatusUpdates(boolean register, String id) {
        if (!TextUtils.isEmpty(myId) && !TextUtils.isEmpty(id)) {
            DatabaseReference idStatusRef = BaseApplication.getStatusRef().child(id);
            if (register) {
                idStatusRef.addChildEventListener(chatStatusListener);
            } else {
                idStatusRef.removeEventListener(chatStatusListener);
            }
        }
    }

    private void updateMessage(Message message) {
        String userOrGroupId = message.getRecipientId().startsWith(Helper.GROUP_PREFIX) ? message.getRecipientId() : myId.equals(message.getSenderId()) ? message.getRecipientId() : message.getSenderId();
        Chat chat = Helper.getChat(rChatDb, myId, userOrGroupId).findFirst();
        if (chat == null) {
            chat = rChatDb.createObject(Chat.class);
            if (userOrGroupId.startsWith(Helper.GROUP_PREFIX)) {
                chat.setGroup(rChatDb.copyToRealm(groupHashMap.get(userOrGroupId)));
                chat.setGroupId(userOrGroupId);
                chat.setUser(null);
                chat.setUserId(null);
            } else {
                chat.setUser(rChatDb.copyToRealm(userHashMap.get(userOrGroupId)));
                chat.setUserId(userOrGroupId);
                chat.setGroup(null);
                chat.setGroupId(null);
            }
            RealmList<Message> msgList = new RealmList<>();
            msgList.add(message);
            chat.setMessages(msgList);
            chat.setLastMessage(message.getBody());
            chat.setMyId(myId);
            chat.setTimeUpdated(message.getDate());
        }
    }

    private void saveMessage(final Message message) {
        if (message.getAttachment() != null && !TextUtils.isEmpty(message.getAttachment().getUrl())
                && !TextUtils.isEmpty(message.getAttachment().getName())) {
            String idToCompare = "loading" + message.getAttachment().getBytesCount() + message.getAttachment().getName();
//            String idToCompare = msgUserID;
            Helper.deleteMessageFromRealm(rChatDb, idToCompare);
        }

        final String userOrGroupId = message.getRecipientId()
                .startsWith(Helper.GROUP_PREFIX) ? message.getRecipientId() : myId.equals(message.getSenderId()) ? message.getRecipientId() : message.getSenderId();
        final Chat[] chat = {Helper.getChat(rChatDb, myId, userOrGroupId).findFirst()};
        if (userMe != null && userMe.getSolochat().size() > 0 && !userOrGroupId.startsWith(Helper.GROUP_PREFIX)) {
            ArrayList<String> chatSoloIds = new ArrayList<>();
            for (int i = 0; i < userMe.getSolochat().size(); i++) {
                chatSoloIds.add(userMe.getSolochat().get(i).getPhoneNo());
            }
            for (solochat soloChatIds : userMe.getSolochat()) {
                if (userOrGroupId.contains(soloChatIds.getPhoneNo()) &&
                        message.getDate() > soloChatIds.getTimeStamp()) {
                    storeInLocalDB(message, userOrGroupId, chat);
                }
            }
            if (!chatSoloIds.contains(userOrGroupId)) {
                storeInLocalDB(message, userOrGroupId, chat);
            }
        } else {
            storeInLocalDB(message, userOrGroupId, chat);
        }
    }

    private void storeInLocalDB(final Message message, final String userOrGroupId, final Chat[] chat) {
        rChatDb.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (chat[0] == null) {
                    chat[0] = rChatDb.createObject(Chat.class);
                    if (userOrGroupId.startsWith(Helper.GROUP_PREFIX)) {
                        chat[0].setGroup(rChatDb.copyToRealm(groupHashMap.get(userOrGroupId)));
                        chat[0].setGroupId(userOrGroupId);
                        chat[0].setUser(null);
                        chat[0].setUserId(null);
                        //!message.getGrpDeletedMsgIds().contains(myId) &&
                        if (message.getUserIds() != null
                                && message.getUserIds().contains(userMe.getId())) {
                            chat[0].setLastMessage(message.getBody());
                        }
                    } else {
                        chat[0].setUser(rChatDb.copyToRealm(userHashMap.get(userOrGroupId)));
                        chat[0].setUserId(userOrGroupId);
                        chat[0].setGroup(null);
                        chat[0].setGroupId(null);
                        if (!message.getDelete().contains(myId)) {
                            chat[0].setLastMessage(message.getBody());
                        }
                    }
                    chat[0].setMessages(new RealmList<Message>());
                    chat[0].setMyId(myId);
                    chat[0].setTimeUpdated(message.getDate());
                }

                if (!message.getSenderId().equals(myId))
                    chat[0].setRead(false);
                chat[0].setTimeUpdated(message.getDate());
                chat[0].getMessages().add(message);
                if (userOrGroupId.startsWith(Helper.GROUP_PREFIX)) {
                    //message.getGrpDeletedMsgIds() == null &&
                    if (message.getUserIds() != null
                            && message.getUserIds().contains(userMe.getId())) {
                        chat[0].setLastMessage(message.getBody());
                    } else if (message.getUserIds() != null
                            && message.getUserIds().contains(userMe.getId())) {
                        //message.getGrpDeletedMsgIds() != null && !message.getGrpDeletedMsgIds().contains(myId) &&
                        chat[0].setLastMessage(message.getBody());
                    }
                } else if (!message.getDelete().contains(myId)) {
                    chat[0].setLastMessage(message.getBody());
                }
//                chat[0].setLastMessage(message.getBody());
            }
        });

        if (userOrGroupId.startsWith(Helper.GROUP_PREFIX) && message.getUserIds() != null && message.getUserIds().contains(userMe.getId())) {
            showNotifications(message, userOrGroupId, chat);
        } else if (!userOrGroupId.startsWith(Helper.GROUP_PREFIX) && message.getUserIds() == null && !message.isBlocked()) {
            showNotifications(message, userOrGroupId, chat);
        }
    }

    private void showNotifications(Message message, String userOrGroupId, Chat[] chat) {
        if (!message.isDelivered() && !message.getSenderId().equals(myId)
                && !helper.isUserMute(message.getSenderId())
                && (Helper.CURRENT_CHAT_ID == null
                || !Helper.CURRENT_CHAT_ID.equals(userOrGroupId))) {
            // Construct the Intent you want to end up at
            Intent chatActivity = null;// = ChatActivity.newIntent(this, null,  ? chat.getGroup() : chat.getUser());
            if (userOrGroupId.startsWith(Helper.GROUP_PREFIX))
                chatActivity = ChatActivity.newIntent(this, null, chat[0].getGroup());
            else
                chatActivity = ChatActivity.newIntent(this, null, chat[0].getUser());
            // Construct the PendingIntent for your Notification
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // This uses android:parentActivityName and
            // android.support.PARENT_ACTIVITY meta-data by default
            stackBuilder.addNextIntentWithParentStack(chatActivity);
            PendingIntent pendingIntent = stackBuilder.getPendingIntent(99, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder notificationBuilder = null;
            String channelId = userOrGroupId.startsWith(Helper.GROUP_PREFIX) ? CHANNEL_ID_GROUP : CHANNEL_ID_USER;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId, "New message notification", NotificationManager.IMPORTANCE_DEFAULT);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
                notificationBuilder = new NotificationCompat.Builder(this, channelId);
            } else {
                notificationBuilder = new NotificationCompat.Builder(this);
            }
            String contactname = "";
            if (helper.getCacheMyUsers() != null) {
                if (!userOrGroupId.startsWith(Helper.GROUP_PREFIX) && helper.getCacheMyUsers().containsKey(chat[0].getUser().getName())) {
                    contactname = helper.getCacheMyUsers().get(chat[0].getUser().getName()).getNameToDisplay();
                }
            }
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilder.setSmallIcon(R.drawable.ic_logo_)
                    .setContentTitle(userOrGroupId.startsWith(Helper.GROUP_PREFIX) ? chat[0].getGroup().getName()
                            : contactname.isEmpty() ? chat[0].getUser().getName() : contactname)
                    .setContentText(getContent(message))
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);
            int msgId = 0;
            try {
                msgId = Integer.parseInt(message.getSenderId());
            } catch (NumberFormatException ex) {
                msgId = Integer.parseInt(message.getSenderId().substring(message.getSenderId().length() / 2));
            }
            notificationManager.notify(msgId, notificationBuilder.build());
        }
    }


    private String getContent(Message message) {
        if (message.getAttachmentType() == AttachmentTypes.AUDIO) {
            return getString(R.string.audio);
        } else if (message.getAttachmentType() == AttachmentTypes.RECORDING) {
            return getString(R.string.recording);
        } else if (message.getAttachmentType() == AttachmentTypes.VIDEO) {
            return getString(R.string.video);
        } else if (message.getAttachmentType() == AttachmentTypes.IMAGE) {
            return getString(R.string.image);
        } else if (message.getAttachmentType() == AttachmentTypes.CONTACT) {
            return getString(R.string.contact);
        } else if (message.getAttachmentType() == AttachmentTypes.LOCATION) {
            return getString(R.string.location);
        } else if (message.getAttachmentType() == AttachmentTypes.DOCUMENT) {
            return getString(R.string.document);
        } else if (message.getAttachmentType() == AttachmentTypes.NONE_TEXT) {
            return message.getBody();
        }
        return " ";
    }

    private void broadcastUser(String what, User value) {
        Intent intent = new Intent(Helper.BROADCAST_USER);
        intent.putExtra("data", value);
        intent.putExtra("what", what);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastGroup(String what, Group value) {
        Intent intent = new Intent(Helper.BROADCAST_GROUP);
        intent.putExtra("data", value);
        intent.putExtra("what", what);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);
    }


    private void broadcastStatus(String what, Message value) {
        Intent intent = new Intent(Helper.BROADCAST_STATUS);
        intent.putExtra("data", value);
        intent.putExtra("what", what);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);
    }

}
