package de.androidnewcomer.mueckenfang;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.os.Handler;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MueckenfangActivity extends AppCompatActivity implements View.OnClickListener, Html.ImageGetter {


    private Animation animationEinblenden;
    private Animation animationWackeln;
    private Button startButton;
    private Handler handler = new Handler();
    private Runnable wackelnRunnable = new WackleButton();
    private View namenseingabe;
    private Button speichern;
    private static final String HIGHSCORE_SERVER_BASE_URL = "https://myhighscoreserver.appspot.com/highscoreserver";
    private static final String HIGHSCORESERVER_GAME_ID = "muuuueckenfang";
    private String highscoresHtml;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button button = findViewById(R.id.button_to_start_the_game);
        button.setOnClickListener(this);
        animationEinblenden = AnimationUtils.loadAnimation(this,R.anim.einblenden);
        animationWackeln = AnimationUtils.loadAnimation(this,R.anim.wackeln);
        startButton = findViewById(R.id.button_to_start_the_game);

        namenseingabe = findViewById(R.id.namenseingabe);
        namenseingabe.setVisibility(View.INVISIBLE);
        speichern = findViewById(R.id.speichern_button);
        speichern.setOnClickListener(this);
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

        highscoreAnzeigen();

        internetHighscores("",0);
    }


    private void highscoreAnzeigen() {
        TextView tv = findViewById(R.id.highscore_number);
        int highscore = leseHighscore();
        if(highscore > 0) {
            tv.setText(Integer.toString(highscore) + " von " + leseHighscoreName());
        } else {
            tv.setText("-");
        }
    }


    private int leseHighscore() {
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        return pref.getInt("HIGHSCORE", 0);
    }


    private void schreibeHighscoreName() {
        EditText et = findViewById(R.id.spielername_MainXml);
        String name = et.getText().toString().trim();
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("HIGHSCORE_NAME", name);
            // HIGHSCORE_NAME ist der Schlüssel
        editor.apply();
    }

    private String leseHighscoreName() {
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        return pref.getString("HIGHSCORE_NAME", "");
    }


    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(wackelnRunnable);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_to_start_the_game) {
            // mit der folgenden Zeile machen wir, dass GameActivity einen Wert zurückgibt...
            startActivityForResult(new Intent(this, GameActivity.class), 1);
            // alt:
            //startActivity(new Intent(this, GameActivity.class));
        } else if (view.getId() == R.id.speichern_button) {
            schreibeHighscoreName();
            highscoreAnzeigen();
            namenseingabe.setVisibility(View.GONE);
            internetHighscores(leseHighscoreName(), leseHighscore());
        }
    }


    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1) {
            if(resultCode > leseHighscore()) {
                schreibeHighscore(resultCode);
                namenseingabe.setVisibility(View.VISIBLE);
            }
        }
    }


    private void schreibeHighscore(int highscore) {
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("HIGHSCORE", highscore);
        editor.apply();
        // im Buch: Endgültiges schreiben durch:
        // editor.commit();
    }


    @Override
    public Drawable getDrawable(String name) {
        int id = getResources().getIdentifier(name, "drawable", this.getPackageName());
        Drawable d = getResources().getDrawable(id);
        d.setBounds(0, 0, 30, 30);
        return d;
    }


    private void internetHighscores(final String name, final int points) {

            // Um Code im Hintergrund auszuführen, schreiben wir folgendes:
        (new Thread(() -> {
            try {
                    // --> hier setzen wir die komplette URL zusammen:
                URL url = new URL(HIGHSCORE_SERVER_BASE_URL
                        + "?game=" + HIGHSCORESERVER_GAME_ID
                        + "&name=" + URLEncoder.encode(name, "utf-8")
                        + "&points=" + Integer.toString(points)
                        + "&max=200");

                    // Hier wird die Verbindung zum Server geöffnet:
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    // wenn wir wissen, dass der InputStream Textdaten enthält, verwenden Sie einen InputStreamReader:
                InputStreamReader input = new InputStreamReader(conn.getInputStream(), "utf-8");

                    // --> Der InputStreamReader kann leider weder zeilenweise lesen noch nach und nach die gesamte Antwort sammeln und dann als Komplettresultat zur Verfügung stellen. Also brauchen Sie einen weiteren Reader, nämlich den BufferedReader:
                BufferedReader reader = new BufferedReader(input,2000);
                List<String> highscoreList = new ArrayList<String>();
                String line = reader.readLine();
                while (line != null) {
                    highscoreList.add(line);
                    line = reader.readLine();
                }

                    // Anschließend können wir aus der Liste mit einzelnen Highscores eine hübsche Ausgabe erzeugen - z.B. mit HTML-Code, denn den versteht eine TextView ganz gut
                highscoresHtml = "";
                for(String s : highscoreList) {
                    highscoresHtml += "<b>"
                            + s.replace(",", "</b> <font color='red'>")
                            + "</font><img src='muecke'><br>";
                }
                Log.d("highscores", highscoresHtml);

            } catch(IOException e) {
                highscoresHtml = "Fehler: " + e.getMessage();
            }
            runOnUiThread(() -> {
                TextView tv = (TextView) findViewById(R.id.highscores);
                    // --> Der Aufruf Html.fromHtml() ist notwendig, damit die TextView den HTML-Code versteht...
                tv.setText(Html.fromHtml(highscoresHtml, MueckenfangActivity.this, null));
            });
        })).start();

    }
}


