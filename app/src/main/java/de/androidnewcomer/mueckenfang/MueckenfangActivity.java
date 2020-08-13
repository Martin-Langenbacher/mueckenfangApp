package de.androidnewcomer.mueckenfang;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

public class MueckenfangActivity extends AppCompatActivity implements View.OnClickListener {


    //private Animation animationEinblenden;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(this);
        //animationEinblenden = AnimationUtils.loadAnimation(this,R.anim.einblenden);
    }

    @Override
    public void onClick(View v) {
        startActivity(new Intent(this, GameActivity.class));

    }


    @Override
    protected void onResume() {
        super.onResume();
        //View v = findViewById(R.id.wurzel);
       // v.startAnimation(animationEinblenden);
    }
}