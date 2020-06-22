package com.daddyno1.aptdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.daddyno1.router_annotation.Route;

@Route(path = "/group1/hello")
public class FourthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fourth);
    }
}
