package com.big.chit.activities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.big.chit.R;
import com.big.chit.models.Contact;
import com.big.chit.models.Group;
import com.big.chit.models.LogCall;
import com.big.chit.models.Status;
import com.big.chit.models.User;
import com.big.chit.utils.AudioPlayer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Zego Calling SDK Added by Ussama Iftikhar on 03-April-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */

public class IncomingCallScreenActivity extends BaseActivity {
    private static final int REQUEST_PERMISSION_CALL = 951;
    private static final String CHANNEL_ID_USER_MISSCALL = "my_channel_04";
    ValueEventListener valueEventListener;
    DatabaseReference reference;
    boolean isVideo = false;
    private String[] recordPermissions = {Manifest.permission.VIBRATE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private String mCallerId, mRoomId;
    private AudioPlayer mAudioPlayer;
    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.answerButton:
                    if (recordPermissionsAvailable()) {
                        answerClicked();
                    } else {
                        ActivityCompat.requestPermissions(IncomingCallScreenActivity.this, recordPermissions, REQUEST_PERMISSION_CALL);
                    }
                    break;
                case R.id.declineButton:
                    declineClicked();
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null)
            reference.removeEventListener(valueEventListener);
    }

    //TODO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_incoming_call_screen);

        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        mAudioPlayer = new AudioPlayer(this);
        mAudioPlayer.playRingtone();

        Intent intent = getIntent();
        mCallerId = intent.getStringExtra("caller_id");
        mRoomId = intent.getStringExtra("room_id");
        isVideo = intent.getBooleanExtra("is_video", false);


        findViewById(R.id.answerButton).setOnClickListener(mClickListener);
        findViewById(R.id.declineButton).setOnClickListener(mClickListener);
        onZegoConnected();


    }

    void onZegoConnected() {


        HashMap<String, User> myUsers = helper.getCacheMyUsers();
        if (myUsers != null && myUsers.containsKey(mCallerId)) {
            user = myUsers.get(mCallerId);
        }

        TextView remoteUser = (TextView) findViewById(R.id.remoteUser);
        ImageView userImage1 = findViewById(R.id.userImage1);
        ImageView userImage2 = findViewById(R.id.userImage2);
        remoteUser.setText(user != null ? user.getNameToDisplay() : mCallerId);
        if (user != null && !user.getImage().isEmpty()) {
//                Glide.with(this).load(user.getImage()).apply(new RequestOptions().placeholder(R.drawable.ic_placeholder)).into(userImage1);
//                Glide.with(this).load(user.getImage()).apply(RequestOptions.circleCropTransform().placeholder(R.drawable.ic_placeholder)).into(userImage2);
            Picasso.get()
                    .load(user.getImage())
                    .tag(this)
                    .placeholder(R.drawable.ic_avatar)
                    .into(userImage2);
        } else {
            userImage2.setBackgroundResource(R.drawable.ic_avatar);
        }
        TextView callingType = findViewById(R.id.txt_calling);
        callingType.setText(getResources().getString(R.string.app_name) + (isVideo ? " Incoming Video Calling" : " Incoming Voice Calling"));


        reference = FirebaseDatabase.getInstance().getReference().child("data");


        String phone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        //ph="+923104772882";
        try {
            valueEventListener = reference.child("call_zego").child(phone).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.getChildrenCount() > 0) {
                        if (dataSnapshot.getKey().equals(phone.toString())) {
                            boolean value = (boolean) dataSnapshot.child("answered").getValue();
                            String callerId = dataSnapshot.child("uId").getValue().toString();
                            if (!value) {
                                mAudioPlayer.stopRingtone();

                                LogCall logCall = null;
                                if (user == null) {
                                    user = new User(mCallerId, mCallerId, getString(R.string.app_name), "");
                                }

                                rChatDb.beginTransaction();
                                logCall = new LogCall(user, System.currentTimeMillis(), 0, false, "cause.toString()", userMe.getId(), user.getId());
                                rChatDb.copyToRealm(logCall);
                                rChatDb.commitTransaction();

                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
                                reference.child(userMe.getId()).removeValue();

                                Toast.makeText(IncomingCallScreenActivity.this, "Canceled by caller", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                        }
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } catch (Exception e) {

        }


    }

    private void answerClicked() {
        mAudioPlayer.stopRingtone();
        Log.d("clima 22", user.getId());
        Log.d("clima 21", mCallerId);
        try {
            startActivity(CallScreenActivity.newIntent(this, user, "IN",isVideo, mRoomId));
            finish();
        } catch (Exception e) {
            Log.e("CHECK", e.getMessage());

        }

    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            answerClicked();
        } else {
            Toast.makeText(this, "This application needs permission to use your microphone to function properly.", Toast.LENGTH_LONG).show();
        }
    }

    private void declineClicked() {
        mAudioPlayer.stopRingtone();

        LogCall logCall = null;
        if (user == null) {
            user = new User(mCallerId, mCallerId, getString(R.string.app_name), "");
        }

        rChatDb.beginTransaction();
        logCall = new LogCall(user, System.currentTimeMillis(), 0, false, "cause.toString()", userMe.getId(), user.getId());
        rChatDb.copyToRealm(logCall);
        rChatDb.commitTransaction();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(userMe.getId());
        reference.child("canceled").setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                //    Toast.makeText(IncomingCallScreenActivity.this, "Canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    private boolean recordPermissionsAvailable() {
        boolean available = true;
        for (String permission : recordPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                available = false;
                break;
            }
        }
        return available;
    }

    private void notifyMisscall(LogCall logCall) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 56, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_USER_MISSCALL, "Dreams Chat misscall notification", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_USER_MISSCALL);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSmallIcon(R.drawable.ic_logo_)
                .setContentTitle(logCall.getUser().getNameToDisplay())
                .setContentText("Gave you a miss call")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        int msgId = 0;
        try {
            msgId = Integer.parseInt(logCall.getUser().getId());
        } catch (NumberFormatException ex) {
            msgId = Integer.parseInt(logCall.getUser().getId().substring(logCall.getUser().getId().length() / 2));
        }
        notificationManager.notify(msgId, notificationBuilder.build());
    }

    @Override
    void myUsersResult(ArrayList<User> myUsers) {

    }


    @Override
    void myContactsResult(ArrayList<Contact> myContacts) {

    }

    @Override
    void userAdded(User valueUser) {

    }

    @Override
    void groupAdded(Group valueGroup) {

    }

    @Override
    void userUpdated(User valueUser) {

    }

    @Override
    void groupUpdated(Group valueGroup) {

    }

    @Override
    void statusAdded(Status status) {

    }

    @Override
    void statusUpdated(Status status) {

    }
}
