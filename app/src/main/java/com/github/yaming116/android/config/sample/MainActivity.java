package com.github.yaming116.android.config.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.github.yaming116.module.config.annotation.AutoConfig;
import com.github.yaming116.module.config.api.AutoInitializer;

public class MainActivity extends AppCompatActivity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AutoInitializer.install(getApplication());

        webView = (WebView) findViewById(R.id.webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl("http://rubik.app.ucmed.cn/appointment/ysyy;jsessionid=2AF7478D030D21A7295A1A69D9D4D094?execution=e1s1");
    }
}
