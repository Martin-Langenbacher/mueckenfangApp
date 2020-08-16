package de.androidnewcomer.mueckenfang;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.os.Handler;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
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

    private static final int REQUESTCODE_PERMISSIONS = 665;
    private Animation animationEinblenden;
    private Animation animationWackeln;
    private Button startButton;
    private Handler handler = new Handler();
    private Runnable wackelnRunnable = new WackleButton();
    private View namenseingabe;
    private Button speichern;
    private static final String HIGHSCORE_SERVER_BASE_URL = "https://myhighscoreserver.appspot.com/highscoreserver";
    private static final String HIGHSCORESERVER_GAME_ID = "muuuueckenfang";
    private ListView listView;
    private ToplistAdapter adapter;
    private List<String> highscoreList = new ArrayList<>();
    // private String highscoresHtml;
    private Spinner schwierigkeitsgrad;
    private ArrayAdapter<String> schwierigkeitsgradAdapter;


        // inner class: TopListAdapter...
    private class ToplistAdapter extends ArrayAdapter<String> {

            public ToplistAdapter(Context context, int resource) {
                super(context, resource);
            }

                // teilt der zuständigen ListView die Anzahl der verfügbaren Listeneinträge mit
            @Override
            public int getCount() {
                return highscoreList.size();
            }

                // Die Methode getView() erzeugt oder füllt die View, die eine Zeile in der Liste darstellt:
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.toplist_element,null);
                }
                TextView tvPlatz = (TextView) convertView.findViewById(R.id.platz);
                tvPlatz.setText(Integer.toString(position + 1) + ".");
                TextUtils.SimpleStringSplitter sss = new TextUtils.SimpleStringSplitter(',');
                sss.setString(highscoreList.get(position));
                TextView tvName = (TextView) convertView.findViewById(R.id.name);
                tvName.setText(sss.next());

                TextView tvPunkte = (TextView) convertView.findViewById(R.id.points);
                tvPunkte.setText("Fehler");
                //tvPunkte.setText(sss.next());
                // ggf. Speicher löschen ??????????????????????????????????????????????????????????????????????????????????????
                Log.d("Problem: ", "-------------------------------------------------------------------------------------------------------------------------------->" +sss.toString());

                return  convertView;
            }
        }


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

        listView = (ListView) findViewById(R.id.listView);
            // wir deklarieren den Adapter als Attribut der Activity, und initialisieren ihn in der onCreate-Methode
        adapter = new ToplistAdapter(this,0);
        listView.setAdapter(adapter);
        schwierigkeitsgrad = (Spinner) findViewById(R.id.schwierigkeitsgrad);
        schwierigkeitsgradAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[] {"leicht","mittel","schwer"});
            // wenn wir später den Spinner antippen, öffnet sich eine Liste mit den wählbaren Einträgen (dafür gibt es ein eigenes Layout, das wir dem Adapter mitteilen...
        schwierigkeitsgradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Schließlich müssen wir noch den Spinner mit dem Adapter verheiraten:
        schwierigkeitsgrad.setAdapter(schwierigkeitsgradAdapter);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)  {
            startButton.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA}, REQUESTCODE_PERMISSIONS);
        }
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
            int s = schwierigkeitsgrad.getSelectedItemPosition();
            // mit der folgenden Zeile machen wir, dass GameActivity einen Wert zurückgibt...
            // alt (ohne Intent): startActivityForResult(new Intent(this, GameActivity.class), 1);
            // alt:
            //startActivity(new Intent(this, GameActivity.class));

            Intent intent = new Intent(this, GameActivity.class);
                // Extra an den intent anhängen: Extras bestehen immer aus einem Namen und einem Wert, in diesem Fall einem int. Sie können auch Strings oder beinahe beliebige andere Objekte anhängen. Auf diese Weise können Sie alle relevanten Daten an eine andere Activity übergeben.
            intent.putExtra("schwierigkeitsgrad", s);
            startActivityForResult(intent, 1);


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
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // --> hier setzen wir die komplette URL zusammen:
                    URL url = new URL(HIGHSCORE_SERVER_BASE_URL
                            + "?game=" + HIGHSCORESERVER_GAME_ID
                            + "&name=" + URLEncoder.encode(name, "utf-8")
                            + "&points=" + Integer.toString(points)
                            + "&max=100");

                    // Hier wird die Verbindung zum Server geöffnet:
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    // wenn wir wissen, dass der InputStream Textdaten enthält, verwenden Sie einen InputStreamReader:
                    InputStreamReader input = new InputStreamReader(conn.getInputStream(), "utf-8");

                    // --> Der InputStreamReader kann leider weder zeilenweise lesen noch nach und nach die gesamte Antwort sammeln und dann als Komplettresultat zur Verfügung stellen. Also brauchen Sie einen weiteren Reader, nämlich den BufferedReader:
                    BufferedReader reader = new BufferedReader(input, 2000);
                    highscoreList.clear();
                    String line = reader.readLine();
                    while (line != null) {
                        highscoreList.add(line);
                        line = reader.readLine();
                    }

                } catch (IOException e) {
                    ;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Anstatt die nicht mehr vorhandene highscores-TextView zu füllen, müssen wir nach dem Herunterladen der Highscores nur noch dem Adapter mitteilen, dass sich der Inhalt des Arrays geändert hat:
                        adapter.notifyDataSetInvalidated();
                    }
                });
            }
        })).start();
    }


/* alt ...
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
        })).start();   */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode==REQUESTCODE_PERMISSIONS && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
            startButton.setEnabled(true);
        }
    }


}


