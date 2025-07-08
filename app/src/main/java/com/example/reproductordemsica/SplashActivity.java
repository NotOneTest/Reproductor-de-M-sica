package com.example.reproductordemsica;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1000;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler.postDelayed(() -> {

            Intent intent = new Intent(this, com.example.reproductordemsica.LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
