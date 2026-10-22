package com.solji.mimicseedcheck;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class MainActivity extends Activity {
    private WebView web;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(16,25,34));
        getWindow().setNavigationBarColor(Color.rgb(16,25,34));
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(16,25,34));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if ("file".equals(url.getScheme())) return false;
                if ("https".equals(url.getScheme())) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, url);
                    startActivity(intent);
                }
                return true;
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    @Override protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
