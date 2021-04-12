package com.big.chit.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.big.chit.R;
import com.big.chit.utils.Helper;
import com.daasuu.ahp.AnimateHorizontalProgressBar;

public class SplashActivity extends AppCompatActivity {

    boolean calledScreen = false;
    boolean isCall = false;
    int request_Code = 901;
    public static int CALL_STATUS=0;

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("clima","onst");
        if(CALL_STATUS==2 &&calledScreen){
            Log.d("clima" ,"calllling");
            calledScreen=false;
            CALL_STATUS=0;
            final Helper helper = new Helper(this);
            startActivity(new Intent(SplashActivity.this, helper.getLoggedInUser() != null ? MainActivity.class : SignInActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("clima","pase");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("clima", "resume");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Log.d("clima", "onk");
        CALL_STATUS=0;

        if (getIntent().getExtras() != null) {
            isCall = Boolean.parseBoolean(getIntent().getStringExtra("is_call"));
            Log.d("clima", "logged1");
            Log.d("clima", "logged1" + isCall);

            boolean isMissedCall = Boolean.parseBoolean(getIntent().getStringExtra("missed_call"));
            Log.d("clima", "logged2");
            Log.d("clima", "logged2" + isMissedCall);
            boolean isVideo =Boolean.parseBoolean(getIntent().getStringExtra("is_video"));
            Log.d("clima", "logged3");
            if (new Helper(this).isLoggedIn()) {
                Log.d("clima", "logged");
                if (isCall && !isMissedCall) {
                    Log.d("clima", "loggeds");
                    Intent intent = new Intent(this, IncomingCallScreenActivity.class);
                    intent.putExtra("is_video", isVideo);
                    intent.putExtra("stream_id", getIntent().getStringExtra("room_id").toString());
                    intent.putExtra("room_id", getIntent().getStringExtra("room_id").toString());
                    intent.putExtra("caller_id", getIntent().getStringExtra("caller_id").toString());
//                    intent.putExtra("caller_id", "+923104772882");
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(intent, request_Code);
                    //startActivity();
                    Log.d("clima", "splash");
                    isCall = true;
                    calledScreen = true;
                    return;
                }

            }

        }
        if (!isCall) {
            AnimateHorizontalProgressBar progressBar = (AnimateHorizontalProgressBar) findViewById(R.id.animate_progress_bar);
            progressBar.setMax(1500);
            progressBar.setProgressWithAnim(1500);
            final Helper helper = new Helper(this);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(SplashActivity.this, helper.getLoggedInUser() != null ? MainActivity.class : SignInActivity.class));
                    finish();
                }
            }, 1500);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("clima", "ack3");
        if (requestCode == request_Code) {
            Log.d("clima", "ack2");
            if (resultCode == RESULT_OK) {
                if(calledScreen) {
                    calledScreen=false;
                    isCall=false;

                    final Helper helper = new Helper(this);
                    startActivity(new Intent(SplashActivity.this, helper.getLoggedInUser() != null ? MainActivity.class : SignInActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();

                } else {
                    finishAffinity();
                }

                Log.d("clima", "ack");
                //finishAffinity();
                // OR
                // String returnedResult = data.getDataString();
            }
        }
    }
}
