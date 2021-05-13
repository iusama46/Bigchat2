package com.big.chit.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.big.chit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(getString(R.string.privacy_policy_url));

        findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PrivacyPolicyActivity.this, MainActivity.class));
                finish();
            }
        });

        try {
            String token = FirebaseInstanceId.getInstance().getToken();
            String uId = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("data").child("users").child(uId);
            reference.child("deviceToken").setValue(token);
            reference.child("osType").setValue("android");
        } catch (Exception e) {
            Log.d("clima e", e.getMessage());
        }
    }


    public class WebViewClient extends android.webkit.WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            String token = FirebaseInstanceId.getInstance().getToken();
            String uId = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("data").child("users").child(uId);
            reference.child("deviceToken").setValue(token);
            reference.child("osType").setValue("android");
        } catch (Exception e) {
            Log.d("clima e", e.getMessage());
        }
        super.onDestroy();
    }
}