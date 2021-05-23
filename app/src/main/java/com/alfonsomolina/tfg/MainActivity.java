package com.alfonsomolina.tfg;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

/**
 *  "Splash Activity" que se muestra al iniciar la aplicacion.
 *   Muestra una animación de bienvenida, con el logo del juego, y
 *   al cabo de unos instantes redirige al mapa.
 *   Realiza la configuración inicial la primera vez que se ejecute.
 *
 *   @author Alfonso Molina
 */
public class MainActivity extends Activity {

    /**
     * Constructor. Si es la primera vez ejecuta inicializarSharedPreferences,
     * y, un segundo y medio mas tarde, irMapa.
     *
     * @param savedInstanceState Bundle necesario.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Si es la primera vez que se inicia, se configuran algunas cosas.
        SharedPreferences sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);

        if (!sharedPref.getBoolean("control_inicializado", false))
            inicializarSharedPreferences(sharedPref);

        //Después de 1.5 segundos se pasa a la actividad del mapa.
        new Handler().postDelayed(new Runnable() {

            public void run() {
                irMapa();
            }

        }, 1500);
    }

    /**
     * Inicia MapaActivity. Si es la primera vez, inicia el tutorial en IntroduccionActivity.
     */
    public void irMapa() {
        Intent intent = new Intent(this, MapaActivity.class);

        SharedPreferences sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);

        //Si es la primera vez que se entra, se lanza el tutorial
        if(!sharedPref.getBoolean("control_tutorial", false)) {
            intent = new Intent(this, IntroduccionActivity.class);
            intent.putExtra("NIVEL", "tutorial");
            intent.putExtra("ETAPA", 0);

            //Se guarda para que no vuelva a salir
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("control_tutorial",true);
            editor.apply();
        }

        startActivity(intent);
    }

    /**
     * Realiza la configuración inicial del juego.
     *
     * @param sharedPref con el fichero de preferencias compartidas.
     */
    private void inicializarSharedPreferences(SharedPreferences sharedPref) {

        SharedPreferences.Editor editor = sharedPref.edit();

        //Se añaden las teclas que estan desbloqueadas inicialmente.
        //Estas son: cambiar teclado, enter, borrar, direcciones y los numeros.

        //Teclado 1
        int [] teclas = {0, 1, 2, 3, 4, 7, 8, 9, 11, 16};
        for(int i : teclas)
            editor.putBoolean("tecla_1_"+i,true);

        //Teclado 2
        teclas = new int[]{0, 1, 2, 3, 11, 15};
        for(int i : teclas)
            editor.putBoolean("tecla_2_"+i,true);

        //Teclado 3
        teclas = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15, 18, 24};
        for(int i : teclas)
            editor.putBoolean("tecla_3_"+i,true);


        //Se inicializa el valor de la ciudad en la que está el personaje.
        editor.putString("ciudad", "pueblo_inicio");

        //Se inicializan valores del jugador:
        editor.putInt("experiencia", 1);    //Se usa para ver qué niveles están disponibles.
        editor.putInt("salud", 5);          //Puntos de vida.
        editor.putInt("fuerza", 5);         //Fuerza.
        editor.putInt("constantes",0);      //Número de constantes desbloqueadas.
        editor.putInt("variables",0);       //Número de tipo de variables desbloqueadas.

        //Lenguaje (por defecto, Java).
        if(sharedPref.getString("lenguaje","").equals(""))
            editor.putString("lenguaje", "Java");

        //Para no inicializarlo cada vez
        editor.putBoolean("control_inicializado", true);

        editor.apply();
    }
}