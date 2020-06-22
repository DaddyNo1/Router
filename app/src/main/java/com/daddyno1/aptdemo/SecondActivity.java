package com.daddyno1.aptdemo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.daddyno1.router.Router;
import com.daddyno1.router_annotation.Route;

@Route(path = "/group2/second")
public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }

    public void onClick(View v){
        Router.navigation("/group3/login");
    }
}
