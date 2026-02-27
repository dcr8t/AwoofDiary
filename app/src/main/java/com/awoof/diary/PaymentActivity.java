package com.awoof.diary;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.awoof.diary.R;

public class PaymentActivity extends Activity {

    private WebView webView;
    private static final String PAYSTACK_URL = "https://paystack.shop/pay/mocg9hti27";
    private static final String REDIRECT_SIGNAL = "bonuscodes.vercel.app/xyzucx";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        webView = (WebView) findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        // --- FIX 1: FIT TO SCREEN LOGIC ---
        // This ensures the webpage shrinks to fit the phone width
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        // --- FIX 2: FORCE ZOOM OUT ---
        // We force the page to render at 60% scale. 
        // This makes the text/buttons smaller so everything fits at a glance.
        webView.setInitialScale(60);

        // --- FIX 3: REMOVED DESKTOP USER AGENT ---
        // We removed the custom UserAgent string. 
        // This allows Paystack to detect a mobile phone and serve the Compact Mobile Interface automatically.

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains(REDIRECT_SIGNAL)) {
                    SharedPreferences prefs = getSharedPreferences("AwoofPrefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("isPaid", true).apply();
                    Toast.makeText(PaymentActivity.this, "Premium Unlocked Successfully!", Toast.LENGTH_LONG).show();
                    finish();
                    return true;
                }
                return false;
            }
        });

        webView.loadUrl(PAYSTACK_URL);
    }
}
