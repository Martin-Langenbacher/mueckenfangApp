package de.androidnewcomer.mueckenfang;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class RadarView extends View {

    private static final int KAMERABREITE_AZIMUT = GameActivity.KAMERABREITE_AZIMUT;
    private Paint mueckenfarbe = new Paint();
    private Paint linienfarbe = new Paint();
    private float massstab;
    private float winkel=0;
    private FrameLayout container;

    /* Remarks:
        Der AttributeSet-Parameter enthält die Einstellungen, die Sie im Layout-Editor vornehmen. ... die View funktioniert nicht ohne diesen Konstruktor, der den zughörigen Elternkonstruktor aufruft.

        Der Radarschirm benötigt zwei Zeichenstifte: einen, um einen Kreis zu zeichnen, an dem sich der Spieler orientieren kann, und einen zweiten, der die Mücken in einer anderen Farbe malt.

        Um die beidne Zeichenstifte nicht in beiden Konstruktoren zusammenzubauen, schreiben wir eine gemeinsame init()-Methode, die jeweils aufgerufen wird: */
    public RadarView(Context context) {
        super(context);
        init();
    }


    public RadarView(Context context, AttributeSet attrs) {
        super(context,attrs);
        init();
    }


    private void init() {
        massstab = getResources().getDisplayMetrics().density;
        mueckenfarbe.setAntiAlias(true);
        mueckenfarbe.setColor(Color.RED);
        mueckenfarbe.setStyle(Paint.Style.STROKE);
        mueckenfarbe.setStrokeWidth(5*massstab);
        linienfarbe.setAntiAlias(true);
        linienfarbe.setColor(Color.WHITE);
        linienfarbe.setStrokeWidth(1*massstab);
        linienfarbe.setStyle(Paint.Style.STROKE);
    }


    // Die GameActivity wird später diese Atribute (von INIT) setzen müssen: Den Winkel ständig und den container einmal zu Beginn.
    // Setter:
    public void setContainer(FrameLayout container) {
        this.container = container;
    }

    public void setWinkel(float winkel) {
        this.winkel = winkel;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(getResources().getColor(R.color.transgrau));

        if(container==null) return;

        int breite = getWidth();
        int hoehe = getHeight();
        int radius = Math.min(breite, hoehe)/2;

        canvas.drawCircle(breite/2, hoehe/2, radius, linienfarbe);
        canvas.drawArc(new RectF(0,0,breite,hoehe),-90-KAMERABREITE_AZIMUT/2, KAMERABREITE_AZIMUT, true, linienfarbe);

        int nummer=0;
        while(nummer<container.getChildCount()) {

            ImageView muecke = (ImageView) container.getChildAt(nummer);
            float azimut = (Integer)muecke.getTag(R.id.azimut);
            canvas.drawArc(new RectF(breite*0.1f,hoehe*0.1f,breite*0.9f,hoehe*0.9f),azimut + winkel-90, 5, false, mueckenfarbe);

            nummer++;
        }
    }
}
