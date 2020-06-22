package com.daddyno1.test;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.daddyno1.router_annotation.Route;

@Route(path = "/group3/login")
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }
}
