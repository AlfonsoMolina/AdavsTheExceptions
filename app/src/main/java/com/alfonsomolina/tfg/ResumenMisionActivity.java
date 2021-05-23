package com.alfonsomolina.tfg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Muestra un resumen de la misiso, los objetivos de ella y el tablero de juego.
 * El tablero será de muestra, los elementos aleatorios no estarán decididos y serán
 * semitransparentes.
 *
 * @author Alfonso Molina
 */
public class ResumenMisionActivity extends Activity {

    private String nivel;
    private int etapa;

    /**
     * Constructor. Muestra la información del nivel y dibuja un tablero de muestra, con los elementos
     * aleatorios aún no decididos.
     *
     * @param savedInstanceState Bundle necesario
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ciclodevida", "En onCreate.");

        setContentView(R.layout.activity_resumen_mision);

        nivel = getIntent().getStringExtra("NIVEL");
        etapa = getIntent().getIntExtra("ETAPA",0);


        ParserJSON parserJSON = new ParserJSON(this, nivel, etapa);

        //Se crea el tablero en una Surface View
        Tablero tablero = new Tablero(this, parserJSON,true);
        //Y se añade al layout.
        LinearLayout surface = (LinearLayout) findViewById(R.id.tablero);
        surface.addView(tablero);


        //Se cargan los datos de este nivel en concreto
        ((ImageView) findViewById(R.id.avatar)).setImageResource(parserJSON.getAvatar());
        ((TextView) findViewById(R.id.descripcion)).setText(parserJSON.getInfoNivel("resumen"));

        //Se muestra el objetivo principal
        String s = parserJSON.getInfoNivel("objetivo");
        if (!s.equals(""))
            ((TextView) findViewById(R.id.retoPrimario)).setText(s);
        else {
            findViewById(R.id.punto_principal).setVisibility(View.GONE);
            findViewById(R.id.retoPrimario).setVisibility(View.GONE);
        }

        //Se muestra el objetivo secundario (si hay)
        s = parserJSON.getInfoNivel("secundario");
        if (!s.equals(""))
            ((TextView) findViewById(R.id.retoSecundario)).setText(s);
        else {
            findViewById(R.id.punto_secundario).setVisibility(View.GONE);
            findViewById(R.id.retoSecundario).setVisibility(View.GONE);

        }

        //Si se muestra desde la actividad de escribir código, no se pone el botón que lleva ahí.
        if(getIntent().getBooleanExtra("DESDE_CODIGO",false))
            ((Button) findViewById(R.id.boton_mision)).setText("Volver al código.");
    }

    /**
     * Inicia CrearCodigoActivity.
     *
     * @param view vista del botón pulsado
     */
    public void irNivel(View view) {
        if(getIntent().getBooleanExtra("DESDE_CODIGO",false))
            super.onBackPressed();
        else {
            Intent intent = new Intent(this, CrearCodigoActivity.class);
            intent.putExtra("NIVEL", nivel);
            intent.putExtra("ETAPA", etapa);
            startActivity(intent);
        }
    }


    /**
     * Al pulsar el botón de "atrás" vuelve a CrearCodigoActivity, si la actividad
     * anterior fue esa, o solicita una confirmación para salir al mapa.
     */
    @Override
    public void onBackPressed() {
        if(getIntent().getBooleanExtra("DESDE_CODIGO",false))
            super.onBackPressed();
        else
            new Aviso().show(getFragmentManager(), "confirmacion");
    }

}
