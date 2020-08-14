package de.androidnewcomer.mueckenfang;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
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

public class GameActivity extends AppCompatActivity implements View.OnClickListener, Runnable {

    private static final long HOECHSTALTER_MS = 2000;
    public static final int DELAY_MILLIS = 100;
    public static final int ZEITSCHEIBEN = 600;
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
    private ViewGroup spielbereich;
    private boolean spielLaeuft;
    private Handler handler = new Handler();
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        massstab = getResources().getDisplayMetrics().density;
        spielbereich = (ViewGroup) findViewById(R.id.spielbereich);
        mediaPlayer = MediaPlayer.create(this, R.raw.summen);
        spielStarten();
    }

    private void spielStarten() {
        spielLaeuft = true;
        runde = 0;
        punkte = 0;
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
        muecken = runde *10;
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
        punkte += 100;
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
        super.onPause();
        handler.removeCallbacks(this);
    }
    // --> Da in der GameActivity keine eigene Runnable-Klasse gebaut wurde, sondern die Activity selbst dazu verwendet wurde, lautet der richtige Parameter beim Aufruf der Methode removeCallbacks() einfach this.



    /*
    1) Die Methode onAnimationEnd() erfährt leider nicht, bei welcher Mücke die Animation beendet ist.
    2) Auch die Animaton selbst lüftet dieses Geheimnis nicht...

    3) Wir müssen also wohl oder über der Klasse MueckeAnimationListener eine Referenz auf die Mücke mitgeben. Eine Instanz der inneren Klasse merkt sich dann die Referenz und kann die richtige Mücke entfernen.

    4) Lösung: Wir fügen der inneren Klasse ein Attribut muecke hinzu, und wir schreiben der Einfachheit einen Konstruktor, der dieses Attribut als Parameter mitbekommt:

     */
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







