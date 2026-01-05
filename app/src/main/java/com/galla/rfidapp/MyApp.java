package com.galla.rfidapp;

import android.app.Application;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MyApp extends Application {


    private static MyApp mApplication;
    @Override
    public void onCreate() {
        super.onCreate();
        mApplication=this;
    }



    public static MyApp getInstance(){
        return mApplication;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

//    public static class ScannerActivity extends AppCompatActivity {
//
//        @Override
//        protected void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            EdgeToEdge.enable(this);
//            setContentView(R.layout.activity_scanner);
//            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//                return insets;
//            });
//        }
//    }
}
