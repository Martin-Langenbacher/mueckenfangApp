package de.androidnewcomer.mueckenfang;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.Random;

/**
 * Created by uwe on 20.05.15.
 */
public class GameActivity extends Activity implements View.OnClickListener, Runnable, SensorEventListener, Camera.PreviewCallback {

    private static final long HOECHSTALTER_MS = 60000;
    public static final int DELAY_MILLIS = 100;
    public static final int ZEITSCHEIBEN = 600;
    public static final int KAMERABREITE_AZIMUT = 10;
    public static final int KAMERABREITE_POLAR = 15;
    private int punkte;
    private int runde;
    private int gefangeneMuecken;
    private int zeit;
    private float massstab;
    private int muecken;
    private Random zufallsgenerator = new Random();
    private FrameLayout spielbereich;
    private boolean spielLaeuft;
    private Handler handler = new Handler();
    private MediaPlayer mp;
    private int schwierigkeitsgrad;
    private SensorManager sensorManager;
    private Sensor sensor;
    private RadarView radar;
    private CameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        cameraView = findViewById(R.id.camera);
        massstab = getResources().getDisplayMetrics().density;
        spielbereich = (FrameLayout) findViewById(R.id.spielbereich);
        radar = (RadarView) findViewById(R.id.radar);
        radar.setContainer(spielbereich);
        mp = MediaPlayer.create(this,R.raw.summen);
        schwierigkeitsgrad = getIntent().getIntExtra("schwierigkeitsgrad",0);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        spielStarten();
    }

    private void spielStarten() {
        spielLaeuft = true;
        runde = 0;
        punkte = 0;
        starteRunde();
        cameraView.setOneShotPreviewCallback(this);
    }

    private void bildschirmAktualisieren() {
        TextView tvPunkte = (TextView)findViewById(R.id.points);
        tvPunkte.setText(Integer.toString(punkte));
        TextView tvRunde = (TextView)findViewById(R.id.round);
        tvRunde.setText(Integer.toString(runde));
        TextView tvTreffer = (TextView) findViewById(R.id.hits);
        tvTreffer.setText(Integer.toString(gefangeneMuecken));
        TextView tvZeit = (TextView) findViewById(R.id.time);
        tvZeit.setText(Integer.toString(zeit/(1000/DELAY_MILLIS)));
        FrameLayout flTreffer = (FrameLayout)findViewById(R.id.bar_hits);
        FrameLayout flZeit = (FrameLayout)findViewById(R.id.bar_time);
        ViewGroup.LayoutParams lpTreffer = flTreffer.getLayoutParams();
        lpTreffer.width = Math.round( massstab * 300 *
                Math.min( gefangeneMuecken,muecken)/muecken);
        ViewGroup.LayoutParams lpZeit = flZeit.getLayoutParams();
        lpZeit.width = Math.round(massstab*zeit*300/ ZEITSCHEIBEN);

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void zeitHerunterzaehlen(){
        zeit = zeit-1;
        if(zeit % (1000/DELAY_MILLIS) ==0) {
            float zufallszahl = zufallsgenerator.nextFloat();
            double wahrscheinlichkeit = muecken * 1.5;
            if (wahrscheinlichkeit > 1) {
                eineMueckeAnzeigen();
                if (zufallszahl < wahrscheinlichkeit - 1) {
                    eineMueckeAnzeigen();
                }
            } else {
                if (zufallszahl < wahrscheinlichkeit) {
                    eineMueckeAnzeigen();
                }
            }
        }
        mueckenVerschwinden();
        bildschirmAktualisieren();
        if(!pruefeSpielende()) {
            if(!pruefeRundenende()) {
                handler.postDelayed(this, DELAY_MILLIS);
                cameraView.setOneShotPreviewCallback(this);
            }
        }
    }

    private boolean pruefeRundenende() {
        if(gefangeneMuecken >= muecken) {
            starteRunde();
            return true;
        }
        return false;
    }

    private void starteRunde() {
        runde = runde +1;
        muecken = runde * (10+schwierigkeitsgrad*10);
        gefangeneMuecken = 0;
        zeit = ZEITSCHEIBEN;
        bildschirmAktualisieren();
        handler.postDelayed(this, 1000);
    }

    private boolean pruefeSpielende() {
        if(zeit == 0 && gefangeneMuecken < muecken) {
            gameOver();
            return true;
        }
        return false;
    }

    private void gameOver() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.gameover);
        dialog.show();
        spielLaeuft = false;
        setResult(punkte);
    }


    private void mueckenVerschwinden() {
        int nummer=0;
        while(nummer < spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            Date geburtsdatum = (Date) muecke.getTag(R.id.geburtsdatum);
            long alter = (new Date()).getTime() - geburtsdatum.getTime();
            if(alter > HOECHSTALTER_MS) {
                spielbereich.removeView(muecke);
            } else {
                nummer++;
            }
        }
    }

    private void eineMueckeAnzeigen() {

        int breite = spielbereich.getWidth();
        int hoehe  = spielbereich.getHeight();
        int muecke_breite = (int) Math.round(massstab*50);
        int muecke_hoehe = (int) Math.round(massstab*42);
        int links = zufallsgenerator.nextInt(breite - muecke_breite );
        int oben = zufallsgenerator.nextInt(hoehe - muecke_hoehe);

        // Mücke erzeugen
        ImageView muecke = new ImageView(this);
        muecke.setOnClickListener(this);
        muecke.setTag(R.id.geburtsdatum, new Date());
        muecke.setVisibility(View.INVISIBLE);

        muecke.setImageResource(R.drawable.muecke);

        // Mücke positionieren
        int azimut = zufallsgenerator.nextInt(360);
        int polar  = zufallsgenerator.nextInt(61) - 30;
        muecke.setTag(R.id.azimut, new Integer(azimut));
        muecke.setTag(R.id.polar, new Integer(polar));

        // Mücke anzeigen
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(muecke_breite, muecke_hoehe);
        params.leftMargin = links;
        params.topMargin = oben;
        params.gravity = Gravity.TOP + Gravity.LEFT;
        spielbereich.addView(muecke, params);

        // Summen starten
        mp.seekTo(0);
        mp.start();
    }


    @Override
    public void onClick(View muecke) {
        gefangeneMuecken++;
        punkte += 100 + schwierigkeitsgrad*100;
        bildschirmAktualisieren();
        mp.pause();
        Animation animationTreffer = AnimationUtils.loadAnimation(this,R.anim.treffer);
        muecke.startAnimation(animationTreffer);
        animationTreffer.setAnimationListener(new MueckeAnimationListener(muecke));
        muecke.setOnClickListener(null);
    }

    private void mueckenPositionieren(float azimutKamera, float polarKamera) {
        int nummer=0;
        while(nummer<spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            int azimut = (Integer) muecke.getTag(R.id.azimut);
            int polar  = (Integer) muecke.getTag(R.id.polar);
            float azimutRelativ = azimut - azimutKamera;
            float polarRelativ = polar - polarKamera;
            if(istMueckeInKamera(azimutRelativ, polarRelativ)) {
                muecke.setVisibility(View.VISIBLE);
                FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) muecke.getLayoutParams();
                params.leftMargin = spielbereich.getWidth()/ 2 + Math.round(spielbereich.getWidth()
                        * azimutRelativ / KAMERABREITE_AZIMUT)-muecke.getWidth()/2;
                params.topMargin =spielbereich.getHeight()/2 - Math.round(spielbereich.getHeight()
                        *polarRelativ/KAMERABREITE_POLAR) - muecke.getHeight()/2;
                muecke.setLayoutParams(params);
            } else {
                muecke.setVisibility(View.GONE);
            }
            nummer++;
        }
    }

    private boolean istMueckeInKamera(float azimutRelativ, float polarRelativ) {
        return (Math.abs(azimutRelativ) <= KAMERABREITE_AZIMUT/2) && (Math.abs(polarRelativ)<= KAMERABREITE_POLAR/2);
    }

    @Override
    public void run() {
        zeitHerunterzaehlen();
    }

    @Override
    protected void onDestroy() {
        mp.release();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(this);
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float azimutKamera = sensorEvent.values[0];
        float polarKamera = -90- sensorEvent.values[1];
        mueckenPositionieren(azimutKamera, polarKamera);
        radar.setWinkel(-sensorEvent.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onPreviewFrame(byte[] bild, Camera camera) {
        int breite = camera.getParameters().getPreviewSize().width;
        int hoehe = camera.getParameters().getPreviewSize().height;
        if (camera.getParameters().getPreviewFormat() == ImageFormat.NV21 && spielbereich.getChildCount()>0) {
            mueckenAufTomatenPruefen(new NV21Image(bild, breite, hoehe));
        }
    }

    private void mueckenAufTomatenPruefen(NV21Image nv21) {
        int nummer=0;
        while(nummer<spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            if(mueckeBeruehrtTomate(muecke, nv21)) {
                mp.pause();
                gefangeneMuecken++;
                punkte += 100 + schwierigkeitsgrad*100;
                bildschirmAktualisieren();
                spielbereich.removeView(muecke);
            } else {
                nummer++;
            }
        }
    }

    private boolean mueckeBeruehrtTomate(ImageView muecke, NV21Image nv21) {
        float faktorHorizontal = nv21.getHoehe()*1.0f / getResources().getDisplayMetrics().widthPixels;
        float faktorVertikal = nv21.getBreite()*1.0f / getResources().getDisplayMetrics().heightPixels;
        Rect ausschnitt = new Rect();
        ausschnitt.bottom= Math.round(nv21.getHoehe() - faktorHorizontal * muecke.getLeft());
        ausschnitt.top   = Math.round(nv21.getHoehe() - faktorHorizontal * muecke.getRight());
        ausschnitt.right = Math.round(faktorVertikal * muecke.getBottom());
        ausschnitt.left  = Math.round(faktorVertikal * muecke.getTop());
        int rotePixel = nv21.zaehleRotePixel(ausschnitt);
        if(rotePixel > 10) {
            return true;
        }
        return false;
    }

    private class MueckeAnimationListener implements Animation.AnimationListener {
        private View muecke;

        public MueckeAnimationListener(View m) {
            muecke = m;
        }
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    spielbereich.removeView(muecke);
                }
            });
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }


}


/*




package de.androidnewcomer.mueckenfang;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.Random;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, Runnable, SensorEventListener, Camera.PreviewCallback {

    private static final long HOECHSTALTER_MS = 2000;
    public static final int DELAY_MILLIS = 100;
    public static final int ZEITSCHEIBEN = 600;
    public static final int KAMERABREITE_AZIMUT = 10;
    public static final int KAMERABREITE_POLAR = 15;
    private static final int MUECKEN_BILDER[][] = {
            {R.drawable.muecke_nw, R.drawable.muecke_n, R.drawable.muecke_no},
            {R.drawable.muecke_w,  R.drawable.muecke,   R.drawable.muecke_o},
            {R.drawable.muecke_sw, R.drawable.muecke_s, R.drawable.muecke_so}};
    private static final String HIMMELSRICHTUNGEN[][] = {
            {"nw", "n", "no"},
            {"w",  "", "o"},
            {"sw", "s", "so"}};
    private int punkte;
    private int runde;
    private int gefangeneMuecken;
    private int zeit;
    private float massstab;
    private int muecken;
    private Random zufallsgenerator = new Random();
    private FrameLayout spielbereich;
    private boolean spielLaeuft;
    private Handler handler = new Handler();
    private MediaPlayer mediaPlayer;
    private int schwierigkeitsgrad;
    private SensorManager sensorManager;
    private Sensor sensor;
    private RadarView radar;
    private CameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        cameraView = findViewById(R.id.camera);
        massstab = getResources().getDisplayMetrics().density;
        spielbereich = findViewById(R.id.spielbereich);
        radar = findViewById(R.id.radar);
        radar.setContainer(spielbereich);
        mediaPlayer = MediaPlayer.create(this, R.raw.summen);
            // ... nun muss die GameAcivity den Wert wieder aus dem Intent (von MueckenfangActivity.java) extrahieren:
        schwierigkeitsgrad = getIntent().getIntExtra("schwierigkeitsgrad",0);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        spielStarten();
    }

    private void spielStarten() {
        spielLaeuft = true;
        runde = 0;
        punkte = 0;
        cameraView.setOneShotPreviewCallback(this);
        starteRunde();
    }

    private void bildschirmAktualisieren() {
        TextView tvPunkte = (TextView)findViewById(R.id.points);
        tvPunkte.setText(Integer.toString(punkte));
        TextView tvRunde = (TextView)findViewById(R.id.round);
        tvRunde.setText(Integer.toString(runde));
        TextView tvTreffer = (TextView) findViewById(R.id.hits);
        tvTreffer.setText(Integer.toString(gefangeneMuecken));
        TextView tvZeit = (TextView) findViewById(R.id.time);
        tvZeit.setText(Integer.toString(zeit/(1000/DELAY_MILLIS)));
        FrameLayout flTreffer = (FrameLayout)findViewById(R.id.bar_hits);
        FrameLayout flZeit = (FrameLayout)findViewById(R.id.bar_time);
        ViewGroup.LayoutParams lpTreffer = flTreffer.getLayoutParams();
        lpTreffer.width = Math.round( massstab * 300 *
                Math.min( gefangeneMuecken,muecken)/muecken);
        ViewGroup.LayoutParams lpZeit = flZeit.getLayoutParams();
        lpZeit.width = Math.round(massstab*zeit*300/ ZEITSCHEIBEN);
    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener((SensorEventListener) this,sensor,SensorManager.SENSOR_DELAY_FASTEST);
    }


    private void zeitHerunterzaehlen(){
        zeit = zeit-1;
        if(zeit % (1000/DELAY_MILLIS) ==0) {
            float zufallszahl = zufallsgenerator.nextFloat();
            double wahrscheinlichkeit = muecken * 1.5;
            if (wahrscheinlichkeit > 1) {
                eineMueckeAnzeigen();
                if (zufallszahl < wahrscheinlichkeit - 1) {
                    eineMueckeAnzeigen();
                }
            } else {
                if (zufallszahl < wahrscheinlichkeit) {
                    eineMueckeAnzeigen();
                }
            }
        }
        mueckenVerschwinden();
        mueckenBewegen();
        bildschirmAktualisieren();
        if(!pruefeSpielende()) {
            if(!pruefeRundenende()) {
                handler.postDelayed(this, DELAY_MILLIS);
                    // ... somit fordern wir ein frisches Kamerabild an:
                cameraView.setOneShotPreviewCallback(this);
            }
        }
    }


    private boolean pruefeRundenende() {
        if(gefangeneMuecken >= muecken) {
            starteRunde();
            return true;
        }
        return false;
    }


    private void starteRunde() {
        runde = runde +1;
        muecken = runde * (10 + schwierigkeitsgrad * 10);
        // muecken = runde *10;
        gefangeneMuecken = 0;
        zeit = ZEITSCHEIBEN;
        bildschirmAktualisieren();
        handler.postDelayed(this, 1000);
    }


    private boolean pruefeSpielende() {
        if(zeit == 0 && gefangeneMuecken < muecken) {
            gameOver();
            return true;
        }
        return false;
    }


    private void gameOver() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.gameover);
        dialog.show();
        spielLaeuft = false;
        // --> mit der Methode setResult() übergeben wir die Punkte zurück an MueckenfangActivity: Auf diese Weise ist dafür gesorgt, dass onActivityResult() mit der Punktzahl aufgerufen wird.
        setResult(punkte);
    }


    private void mueckenBewegen() {
        int nummer=0;
        while(nummer < spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            int vx = (Integer) muecke.getTag(R.id.vx);
            int vy = (Integer) muecke.getTag(R.id.vy);
            // und nun bewegen...
            FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) muecke.getLayoutParams();
            params.leftMargin += vx*runde;
            params.topMargin += vy*runde;
            muecke.setLayoutParams(params);
            nummer++;
        }
    }


    private void mueckenVerschwinden() {
        int nummer=0;
        while(nummer < spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            Date geburtsdatum = (Date) muecke.getTag(R.id.geburtsdatum);
            long alter = (new Date()).getTime() - geburtsdatum.getTime();
            if(alter > HOECHSTALTER_MS) {
                spielbereich.removeView(muecke);
            } else {
                nummer++;
            }
        }
    }


    private void eineMueckeAnzeigen() {
        int breite = spielbereich.getWidth();
        int hoehe  = spielbereich.getHeight();
        int muecke_breite = Math.round(massstab*50);
        int muecke_hoehe = Math.round(massstab*42);
        int links = zufallsgenerator.nextInt(breite - muecke_breite );
        int oben = zufallsgenerator.nextInt(hoehe - muecke_hoehe);

        // Mücke erzeugen
        ImageView muecke = new ImageView(this);
        muecke.setOnClickListener(this);
        muecke.setTag(R.id.geburtsdatum, new Date());
        muecke.setVisibility(View.INVISIBLE);

        muecke.setImageResource(R.drawable.muecke);

        // Mücke positionieren
        int azimut = zufallsgenerator.nextInt(360);
        int polar  = zufallsgenerator.nextInt(61) - 30;
        muecke.setTag(R.id.azimut, new Integer(azimut));
        muecke.setTag(R.id.polar, new Integer(polar));

        // Mücke anzeigen
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(muecke_breite, muecke_hoehe);
        params.leftMargin = links;
        params.topMargin = oben;
        params.gravity = Gravity.TOP + Gravity.LEFT;
        spielbereich.addView(muecke, params);


/* -----------------------------------------------> Ausblenden
        // Bewegungsvektor erzeugen
        int vx;
        int vy;
        do {
            vx = zufallsgenerator.nextInt(3)-1;
            vy = zufallsgenerator.nextInt(3)-1;
        } while(vx==0 && vy==0);

        muecke.setTag(R.id.vx, new Integer(vx));
        muecke.setTag(R.id.vy, new Integer(vy));
        // wir hängen die Geschwindigkeit als Tag an die Mücke, genau wie das Geburtsdatum.

        setzeBild(muecke, vx, vy);

        // Geschwindigkeitskorrektur für schräge Mücken
        double faktor = 1.0;
        if(vx != 0 && vy != 0) {
            faktor = 0.70710678;
        }

        vx = (int) Math.round(massstab*faktor*vx);
        vy = (int) Math.round(massstab*faktor*vy);

        // Mücke anzeigen
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(muecke_breite, muecke_hoehe);
        params.leftMargin = links;
        params.topMargin = oben;
        params.gravity = Gravity.TOP + Gravity.LEFT;
        spielbereich.addView(muecke, params);
 -----------------------------------------------> Ausblenden  */






/*



        // Summen starten
        mediaPlayer.seekTo(0);
        mediaPlayer.start();
    }


    private void setzeBild(ImageView muecke, int vx, int vy) {
        // setzen des richtigen Bildes mit dem zweidimensionalen Array
        muecke.setImageResource(MUECKEN_BILDER[vy+1][vx+1]);

        // alternativ: setzen über Himmelsrichtungen und Bildname
        //muecke.setImageResource(getResources().getIdentifier("muecke_"+HIMMELSRICHTUNGEN[vy+1][vx+1], "drawable", getPackageName()));
    }


    @Override
    public void onClick(View muecke) {
        gefangeneMuecken++;
        punkte += 150 + schwierigkeitsgrad * 100;
        // punkte += 150;
        bildschirmAktualisieren();
        mediaPlayer.pause();
        Animation animationTreffer = AnimationUtils.loadAnimation(this,R.anim.treffer);
        muecke.startAnimation(animationTreffer);
            // Zeile löschen, da sonst die Mücke sofort gelöscht wird und die Animation nicht gezeigt wird
            // spielbereich.removeView(muecke);

            // wir benötigen einen AnimationListener um zu erfahren, wann die Animation beendet ist - dann können wir die Fliege löschen
        animationTreffer.setAnimationListener(new MueckeAnimationListener(muecke));
            // wir entfernen einfach am Ende von onClick() den Listener von der Mücke, indem wir ihn mit null überschreiben
        muecke.setOnClickListener(null);
    }


    private void mueckenPositionieren(float azimutKamera, float polarKamera) {
        int nummer=0;
        while(nummer<spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            int azimut = (Integer) muecke.getTag(R.id.azimut);
            int polar  = (Integer) muecke.getTag(R.id.polar);
            float azimutRelativ = azimut - azimutKamera;
            float polarRelativ = polar - polarKamera;
            if(istMueckeInKamera(azimutRelativ, polarRelativ)) {
                muecke.setVisibility(View.VISIBLE);
                FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) muecke.getLayoutParams();
                params.leftMargin = spielbereich.getWidth()/ 2 + Math.round(spielbereich.getWidth()
                        * azimutRelativ / KAMERABREITE_AZIMUT)-muecke.getWidth()/2;
                params.topMargin =spielbereich.getHeight()/2 - Math.round(spielbereich.getHeight()
                        *polarRelativ/KAMERABREITE_POLAR) - muecke.getHeight()/2;
                muecke.setLayoutParams(params);
            } else {
                muecke.setVisibility(View.GONE);
            }
            nummer++;
        }
    }


    private boolean istMueckeInKamera(float azimutRelativ, float polarRelativ) {
        return (Math.abs(azimutRelativ) <= KAMERABREITE_AZIMUT/2) && (Math.abs(polarRelativ)<= KAMERABREITE_POLAR/2);
    }


    @Override
    public void run() {
        zeitHerunterzaehlen();
    }


    @Override
    protected void onDestroy() {
        mediaPlayer.release();
        super.onDestroy();
    }


    // Warteschlange des Handlers wird geleert - hier falls es eine Pause gibt.
    @Override
    protected void onPause() {
        handler.removeCallbacks(this);
        sensorManager.unregisterListener((SensorListener) this);
        super.onPause();
    }
    // --> Da in der GameActivity keine eigene Runnable-Klasse gebaut wurde, sondern die Activity selbst dazu verwendet wurde, lautet der richtige Parameter beim Aufruf der Methode removeCallbacks() einfach this.


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float azimutKamera = sensorEvent.values[0];
        float polarKamera = -90- sensorEvent.values[1];
        mueckenPositionieren(azimutKamera, polarKamera);
        radar.setWinkel(-sensorEvent.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }





    @Override
    public void onPreviewFrame(byte[] bild, Camera camera) {
        int breite = camera.getParameters().getPreviewSize().width;
        int hoehe = camera.getParameters().getPreviewSize().height;
        if (camera.getParameters().getPreviewFormat() == ImageFormat.NV21 && spielbereich.getChildCount()>0) {
            mueckenAufTomatenPruefen(new NV21Image(bild, breite, hoehe));
        }
    }



    private void mueckenAufTomatenPruefen(NV21Image nv21) {
        int nummer=0;
        while(nummer<spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            if(mueckeBeruehrtTomate(muecke, nv21)) {
                mediaPlayer.pause();
                gefangeneMuecken++;
                punkte += 100 + schwierigkeitsgrad*100;
                bildschirmAktualisieren();
                spielbereich.removeView(muecke);
            } else {
                nummer++;
            }
        }
    }


    private boolean mueckeBeruehrtTomate(ImageView muecke, NV21Image nv21) {
        float faktorHorizontal = nv21.getHoehe()*1.0f / getResources().getDisplayMetrics().widthPixels;
        float faktorVertikal = nv21.getBreite()*1.0f / getResources().getDisplayMetrics().heightPixels;
        Rect ausschnitt = new Rect();
        ausschnitt.bottom= Math.round(nv21.getHoehe() - faktorHorizontal * muecke.getLeft());
        ausschnitt.top   = Math.round(nv21.getHoehe() - faktorHorizontal * muecke.getRight());
        ausschnitt.right = Math.round(faktorVertikal * muecke.getBottom());
        ausschnitt.left  = Math.round(faktorVertikal * muecke.getTop());
        int rotePixel = nv21.zaehleRotePixel(ausschnitt);
        if(rotePixel > 10) {
            return true;
        }
        return false;
    }


    /*
    1) Die Methode onAnimationEnd() erfährt leider nicht, bei welcher Mücke die Animation beendet ist.
    2) Auch die Animaton selbst lüftet dieses Geheimnis nicht...

    3) Wir müssen also wohl oder über der Klasse MueckeAnimationListener eine Referenz auf die Mücke mitgeben. Eine Instanz der inneren Klasse merkt sich dann die Referenz und kann die richtige Mücke entfernen.

    4) Lösung: Wir fügen der inneren Klasse ein Attribut muecke hinzu, und wir schreiben der Einfachheit einen Konstruktor, der dieses Attribut als Parameter mitbekommt:

     */




















/*

    private class MueckeAnimationListener implements Animation.AnimationListener {
        private View muecke;

        public MueckeAnimationListener(View m) {
            muecke = m;
        }
        @Override
        public void onAnimationStart(Animation animation) {

        }


        // 5) Wenn wir jetzt in der Methode onAnimationEnd() die muecke einfach aus dem Spielbereich entfernen, knallt es früher oder später: Animation und Bildschirmaufbau sind nämlich nicht synchronisiert.
        // --> Es kann passieren, dass Sie die Mücke entfernen, während Android gerade versucht, sie zu zeichnen. Das Resultat wäre ein Crash. Darum müssen wir über den Handler gehen.
        @Override
        public void onAnimationEnd(Animation animation) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    spielbereich.removeView(muecke);
                }
            });
        }
        // 6) Elegante Lösung: Wir übergeben hier der Methode handler.post einen sogenannte anonyme innere Klasse --> nach Runnable: ==>"() {... xxx() { spielbereich.xxxxxx(muecke); } }"<==
        // ... sie heißt anonym, weil stie keinen Namen erhält und mit new ein (ebenfalls namenloses) Objekt erzeugt wird.
        // ... die anonyme Klasse implementiert das Interface Runnable, und folglich müssen wir die Methode run() implementieren: In dieser Methoden können wir nun die Mücke unfallfrei entfernen...
        // 7) Vorteil: Das fragliche Objekt muecke ist der anonymen Klasse bekannt, weil sie einen innere Klasse von MueckeAnimationListener ist, die über die richtige Referenz verfügt.


        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

}


*/




