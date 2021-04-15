package com.big.chit.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.big.chit.R;
import com.big.chit.Utils;
import com.big.chit.models.Contact;
import com.big.chit.models.Group;
import com.big.chit.models.LogCall;
import com.big.chit.models.Status;
import com.big.chit.models.User;
import com.big.chit.utils.AudioPlayer;
import com.big.chit.utils.OnDragTouchListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

/**
 * Agora Calling SDK Added by Ussama Iftikhar on 12-April-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */

public class CallScreenActivity extends BaseActivity implements SensorEventListener {
    static final String TAG = CallScreenActivity.class.getSimpleName();
    static final String ADDED_LISTENER = "addedListener";

    private static final String EXTRA_DATA_USER = "extradatauser";
    private static final String EXTRA_DATA_IN_OR_OUT = "extradatainorout";
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSION_REQ_ID = 22;

    String callRoomId = "room";
    PowerManager.WakeLock wlOff = null, wlOn = null;
    boolean isConnected = false;
    String streamID;
    boolean isSpeaker = false;
    ValueEventListener valueEventListener;
    boolean isLoggedIn = false;
    DatabaseReference referenceDb;
    boolean isVideoCall;
    String receiverToken = "";
    String inComingCallerID = "";
    String inComingRoomID = "";
    FrameLayout mLocalContainer, mRemoteContainer;
    SurfaceView mLocalView, mRemoteView;
    private Chronometer mCallDuration;
    private AudioPlayer mAudioPlayer;
    private String inOrOut;
    private boolean mAddedListener, isMute, alphaInvisible, logSaved;
    private RelativeLayout myCallScreenRootRLY;
    private TextView mCallState, mCallerName, myTxtCalling;
    private ImageView userImage1, userImage2, switchVideo, switchMic, switchVolume;
    private View tintBlue, bottomButtons;
    private RelativeLayout localVideo, remoteVideo;
    private LinearLayout mySwitchCameraLLY;
    private SensorManager mSensorManager;
    private Sensor mProximity;

    private RtcEngine mRtcEngine;
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onRtcStats(RtcStats stats) {
            super.onRtcStats(stats);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CallScreenActivity.this, "users " + stats.users, Toast.LENGTH_SHORT).show();
                }
            });
        }



        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    isLoggedIn = true;
//                    if (!isVideoCall)
//                        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
//                    else
//                        setVolumeControlStream(AudioManager.STREAM_SYSTEM);

                    if (!inOrOut.equals("IN")) {
                        mAudioPlayer.playProgressTone();
                        pushNotification(false);
                    }
                    Toast.makeText(CallScreenActivity.this, "onJOinChannel", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAudioPlayer.stopProgressTone();
                    if (!isVideoCall)
                        if (!mRtcEngine.isSpeakerphoneEnabled())
                            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

                    Toast.makeText(CallScreenActivity.this, "onUersJoined", Toast.LENGTH_SHORT).show();

                    //Toast.makeText(CallScreenActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    myTxtCalling.setText(getResources().getString(R.string.app_name) + " Call Connected");

                    mCallDuration.setVisibility(View.VISIBLE);
                    mCallDuration.setFormat("%02d:%02d");
                    mCallDuration.setBase(SystemClock.elapsedRealtime());
                    //mCallDuration.setText("Connected");
                    mCallDuration.start();
                    isConnected = true;

                    if (isVideoCall) {
                        remoteVideo.removeAllViews();
                        remoteVideo.setVisibility(View.VISIBLE);
                        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
                        mRemoteContainer.addView(mRemoteView);
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                        remoteVideo.addView(mRemoteContainer);

                        localVideo.removeAllViews();
                        localVideo.addView(mLocalContainer);
                        localVideo.setVisibility(View.VISIBLE);
                    }
                }
            });
        }


        @Override
        public void onUserOffline(final int uid, final int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    endCall();
                }
            });
        }

        // Listen for the onUserMuterAudio callback.
        // This callback occurs when a remote user stops sending the audio stream.
        @Override
        public void onUserMuteAudio(final int uid, final boolean muted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //onRemoteUserVoiceMuted(uid, muted);
                }
            });
        }
    };

    public static Intent newIntent(Context context, User user, String inOrOut, boolean callIsVideo, String token, String roomToken) {
        Intent intent = new Intent(context, CallScreenActivity.class);
        intent.putExtra(EXTRA_DATA_USER, user);
        intent.putExtra(EXTRA_DATA_IN_OR_OUT, inOrOut);
        intent.putExtra("callIsVideo", callIsVideo);
        intent.putExtra("token", token);
        intent.putExtra("room_token", roomToken);
        return intent;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float distance = sensorEvent.values[0];
        if (!isVideoCall && !isSpeaker) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (distance < 4) {
                if (wlOn != null && wlOn.isHeld()) {
                    wlOn.release();
                }
                if (pm != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        if (wlOff == null)
                            wlOff = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "tag");
                        if (!wlOff.isHeld()) wlOff.acquire();
                    }
                }
            } else {
                if (wlOff != null && wlOff.isHeld()) {
                    wlOff.release();
                }
                if (pm != null) {
                    if (wlOn == null)
                        wlOn = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
                    if (!wlOn.isHeld()) wlOn.acquire();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(ADDED_LISTENER, mAddedListener);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mAddedListener = savedInstanceState.getBoolean(ADDED_LISTENER);
    }

    String roomToken="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_screen);

        mLocalContainer = new FrameLayout(this);
        mRemoteContainer = new FrameLayout(this);
        mRemoteView = new SurfaceView(this);
        mLocalView = new SurfaceView(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        Intent intent = getIntent();
        user = intent.getParcelableExtra(EXTRA_DATA_USER);

        inOrOut = intent.getStringExtra(EXTRA_DATA_IN_OR_OUT);
        receiverToken = intent.getStringExtra("token");
        //todo check
        roomToken = intent.getStringExtra("room_token");
        isVideoCall = intent.getBooleanExtra("callIsVideo", false);

//        if (inOrOut.equals("IN")) {
//            inComingRoomID= in
//        }


        mCallDuration = findViewById(R.id.callDuration);
        mCallerName = findViewById(R.id.remoteUser);
        mCallState = findViewById(R.id.callState);
        userImage1 = findViewById(R.id.userImage1);
        userImage2 = findViewById(R.id.userImage2);
        myTxtCalling = findViewById(R.id.txt_calling);
        tintBlue = findViewById(R.id.tintBlue);
        localVideo = findViewById(R.id.localVideo);
        remoteVideo = findViewById(R.id.remoteVideo);
        switchVideo = findViewById(R.id.switchVideo);
        switchMic = findViewById(R.id.switchMic);
        switchVolume = findViewById(R.id.switchVolume);
        bottomButtons = findViewById(R.id.layout_btns);
        mySwitchCameraLLY = findViewById(R.id.switchVideo_LLY);
        myCallScreenRootRLY = findViewById(R.id.layout_call_screen_root_RLY);

        mAudioPlayer = new AudioPlayer(this);
        onZegoCreated();


        findViewById(R.id.hangupButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });


        remoteVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAlphaAnimation();
            }
        });
        switchMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isMute = !isMute;
                isMute();
            }
        });
        switchVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSpeaker = !isSpeaker;
                enableSpeaker(isSpeaker);
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        updateUI();
    }

    @Override
    public void onBackPressed() {
        // User should exit activity by ending call, not by going back.
    }

    @Override
    protected void onDestroy() {
        SplashActivity.CALL_STATUS = 2;
        mAudioPlayer.stopProgressTone();
        mAudioPlayer.stopRingtone();

        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        if (isLoggedIn) {
            RtcEngine.destroy();
            mRtcEngine = null;
//            zegoExpressEngine.stopPlayingStream(streamID);
//            zegoExpressEngine.stopPublishingStream();
//            zegoExpressEngine.stopPreview();
//            zegoExpressEngine.logoutRoom(callRoomId);
        }

        //ZegoExpressEngine.destroyEngine(null);

        try {
            if (wlOff != null && wlOff.isHeld()) {
                wlOff.release();
            } else if (wlOn != null && wlOn.isHeld()) {
                wlOn.release();
            }
        } catch (RuntimeException ex) {
        }


        if (valueEventListener != null)
            referenceDb.removeEventListener(valueEventListener);
        super.onDestroy();
    }

    private void endCall() {
        SplashActivity.CALL_STATUS = 2;
        mRtcEngine.leaveChannel();

        mAudioPlayer.stopProgressTone();
        mAudioPlayer.stopRingtone();
        saveLog();

        try {
            if (!inOrOut.equals("IN")) {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(user.getId());
                reference.removeValue();
                if (!isConnected) {
                    //todo miss call message
                }
            }

        } catch (Exception e) {
        }
        finish();
    }

    private void saveLog() {
        if (!logSaved) {
            rChatDb.beginTransaction();
            rChatDb.copyToRealm(new LogCall(user, System.currentTimeMillis(), 0,
                    isVideoCall, inOrOut, userMe.getId(), user.getId()));
            rChatDb.commitTransaction();
            logSaved = true;
        }
    }
    String accessToken ="";
    void onZegoCreated() {
        if (isVideoCall && !checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else if (!checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), Utils.appID2, mRtcEventHandler);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
        } catch (Exception e) {
            Log.e("clima e", Log.getStackTraceString(e));
            Toast.makeText(this, "NEED TO check rtc sdk init fatal error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

         accessToken =  Utils.token;
        mRtcEngine.joinChannel(accessToken, "testChannel", "Extra Optional Data", 0);
        if (isVideoCall) {
            setVolumeControlStream(AudioManager.STREAM_SYSTEM);
            isSpeaker = true;
            mRtcEngine.enableVideo();
            // Create a SurfaceView object.
            mLocalView = RtcEngine.CreateRendererView(getBaseContext());
            mLocalView.setZOrderMediaOverlay(true);
            mLocalContainer.addView(mLocalView);
            if (!inOrOut.equals("IN")) {
                remoteVideo.removeAllViews();
                remoteVideo.addView(mLocalContainer);
            } else {
                localVideo.setVisibility(View.VISIBLE);
                localVideo.removeAllViews();
                localVideo.addView(mLocalContainer);
            }
            VideoCanvas localVideoCanvas = new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0);
            mRtcEngine.setupLocalVideo(localVideoCanvas);
        } else {
            mRtcEngine.disableVideo();
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }

        isMute();
        enableSpeaker(isSpeaker);

        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));

        if (!inOrOut.equals("IN"))
            callRoomId = callRoomId + userMe.getId() + randomSuffix;
        else {
            callRoomId = receiverToken;
        }


        if (!isVideoCall && !inOrOut.equals("IN"))
            streamID = userMe.getId();
        else if (!isVideoCall)
            streamID = user.getId();

        if (isVideoCall)
            streamID = userMe.getId();


        if (!inOrOut.equals("IN")) {
//            if (!isVideoCall)
            //zegoExpressEngine.startPublishingStream(streamID);

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
            HashMap<String, Object> datamap = new HashMap<>();
            datamap.put("name", userMe.getName());
            datamap.put("id", streamID);
            datamap.put("callerId", user.getId());
            datamap.put("answered", true);
            datamap.put("streamId", streamID);
            datamap.put("video", isVideoCall);
            datamap.put("canceled", false);
            datamap.put("uId", FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
            datamap.put("room", callRoomId);
            datamap.put("isGroup", false);
            reference.child(user.getId()).setValue(datamap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    //Toast.makeText(CallScreenActivity.this, "Calling", Toast.LENGTH_SHORT).show();

                }
            });


            referenceDb = FirebaseDatabase.getInstance().getReference().child("data");
            try {
                valueEventListener = referenceDb.child("call_zego").child(user.getId()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getChildrenCount() > 0) {

                            if (dataSnapshot.getKey().equals(user.getId())) {

                                boolean value = (Boolean) dataSnapshot.child("canceled").getValue();
                                if (value) {
                                    endCall();
                                    Toast.makeText(CallScreenActivity.this, "Call cancelled by user", Toast.LENGTH_SHORT).show();
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


        if (callRoomId != null) {

            mCallerName.setText(user != null ? user.getNameToDisplay() : "call.getRemoteUserId()");
            mCallState.setText("call.getState().toString()");
            if (user != null) {
//                Glide.with(this).load(user.getImage()).apply(new RequestOptions().placeholder(R.drawable.ic_logo_)).into(userImage1);
//                Glide.with(this).load(user.getImage()).apply(RequestOptions.circleCropTransform().placeholder(R.drawable.ic_logo_)).into(userImage2);
                if (user.getImage() != null && !user.getImage().isEmpty()) {
                    if (user.getBlockedUsersIds() != null
                            && !user.getBlockedUsersIds().contains(userMe.getId()))
                        Picasso.get()
                                .load(user.getImage())
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage1);
                    else
                        Picasso.get()
                                .load(R.drawable.ic_avatar)
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage1);

                    Picasso.get()
                            .load(user.getImage())
                            .tag(this)
                            .error(R.drawable.ic_avatar)
                            .placeholder(R.drawable.ic_avatar)
                            .into(userImage2);
                } else {
                    userImage1.setBackgroundResource(R.drawable.ic_avatar);
                    userImage2.setBackgroundResource(R.drawable.ic_avatar);
                }
//                tintBlue.setVisibility(View.INVISIBLE);
//                remoteVideo.setVisibility(View.INVISIBLE);
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }

        updateUI();

    }

    private void pushNotification(boolean isMissedCall) {
        try {
            String token = FirebaseInstanceId.getInstance().getToken();
            Log.d("clima", token);
            RequestQueue queue = Volley.newRequestQueue(this);

            String url = "https://fcm.googleapis.com/fcm/send";

            JSONObject notificationObject = new JSONObject();
            notificationObject.put("title", isVideoCall ? "Video Call" : "Voice Call");
            notificationObject.put("body", userMe.getId() + " is calling you");

            JSONObject dataObj = new JSONObject();
            dataObj.put("is_video", isVideoCall);
            dataObj.put("is_group", false);
            dataObj.put("is_call", true);
            dataObj.put("room_id", callRoomId);
            dataObj.put("room_token", accessToken);
            dataObj.put("missed_call", isMissedCall);
            dataObj.put("caller_id", userMe.getId());
            dataObj.put("caller_name", userMe.getName());

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("notification", notificationObject);
            jsonObject.put("data", dataObj);
            jsonObject.put("to", receiverToken);

            JsonObjectRequest request = new JsonObjectRequest(url, jsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("clima", response.toString());
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("clima", error.getMessage());
                }
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    String api_key_header_value = "key=AAAAn4Y4Ciw:APA91bHAgDKs1SakEKc-cdMI4LYz7G8O3IZ6odbpKU8h5tu0SmyICpeOhMFeBnwdOsccZVmUDuwZ245PWt_kk09E2fnS78VelY_JbaJ1XtJVNl4Na6QCioeXSoFS4kMvlDHzJ9EJvX1Q";
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", api_key_header_value);
                    return headers;
                }
            };

            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i("clima", "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    private void updateUI() {

        if (callRoomId != null) {
            mCallerName.setText(user != null ? user.getNameToDisplay() : "call.getRemoteUserId()");
            myTxtCalling.setText(getResources().getString(R.string.app_name) + (isVideoCall ? " Video Calling" : " Voice Calling"));

            tintBlue.setVisibility(isVideoCall ? View.GONE : View.VISIBLE);
            localVideo.setVisibility(!isVideoCall ? View.GONE : View.VISIBLE);
        }

        switchVideo.setClickable(true);

        mySwitchCameraLLY.setVisibility(isVideoCall ? View.VISIBLE : View.GONE);
        switchVideo.setAlpha(isVideoCall ? 1f : 0.4f);
        userImage1.setVisibility(isVideoCall ? View.GONE : View.VISIBLE);

        localVideo.setOnTouchListener(new OnDragTouchListener(localVideo, remoteVideo));
        switchVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRtcEngine.switchCamera();
            }
        });
    }


    private void startAlphaAnimation() {
        AlphaAnimation animation1 = new AlphaAnimation(alphaInvisible ? 0.0f : 1.0f, alphaInvisible ? 1.0f : 0.0f);
        animation1.setDuration(500);
        animation1.setStartOffset(25);
        animation1.setFillAfter(true);

        myTxtCalling.startAnimation(animation1);
        userImage2.startAnimation(animation1);
        mCallerName.startAnimation(animation1);
        mCallState.startAnimation(animation1);
        mCallDuration.startAnimation(animation1);
        bottomButtons.startAnimation(animation1);

        alphaInvisible = !alphaInvisible;
    }

    private void enableSpeaker(boolean enable) {
        mRtcEngine.setEnableSpeakerphone(enable);
        if (isVideoCall)
            switchVolume.setImageDrawable(ContextCompat.getDrawable(this, enable ? R.drawable.ic_speaker : R.drawable.ic_speaker_off));
        else
            switchVolume.setImageDrawable(ContextCompat.getDrawable(this, !enable ? R.drawable.ic_speaker : R.drawable.ic_speaker_off));
    }

    private void isMute() {
        switchMic.setImageDrawable(ContextCompat.getDrawable(this, isMute ? R.drawable.ic_mic_off : R.drawable.ic_mic_on));
        mRtcEngine.muteLocalAudioStream(isMute);
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