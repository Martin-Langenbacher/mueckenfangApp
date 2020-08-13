package de.androidnewcomer.mueckenfang;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.os.Handler;

public class MueckenfangActivity extends AppCompatActivity implements View.OnClickListener {


    private Animation animationEinblenden;
    private Animation animationWackeln;
    private Button startButton;
    private Handler handler = new Handler();
    private Runnable wackelnRunnable = new WackleButton();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
        animationEinblenden = AnimationUtils.loadAnimation(this,R.anim.einblenden);
        animationWackeln = AnimationUtils.loadAnimation(this,R.anim.wackeln);
        startButton = (Button) findViewById(R.id.button);
    }


    private class WackleButton implements Runnable {
        @Override
        public void run() {
            startButton.startAnimation(animationWackeln);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!  <<<====================================================
        // Achtung: die wurzel ist die ID von main.xml --> "android:id="@+id/wurzel""
        View v = findViewById(R.id.wurzel);
        v.startAnimation(animationEinblenden);
        handler.postDelayed(wackelnRunnable, 1000*10);
    }



    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(wackelnRunnable);
    }




    @Override
    public void onClick(View v) {
        startActivity(new Intent(this, GameActivity.class));

    }

}


