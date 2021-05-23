package com.alfonsomolina.tfg;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 *  Permite recorrer el mapa y elegir a qué nivel acceder.
 *  Tiene botones para ir a los apuntes y la configuración.
 *
 *  @author Alfonso Molina
 */
public class MapaActivity extends Activity {

    private ParserJSON parserJSON;
    private SharedPreferences sharedPref;
    private ListView lista;

    private String nivel;           //Identificador del nivel al que se quiere acceder
    private String[] l_niveles;
    private int ultima_etapa;       //Ultima etapa visitada en un nivel

    private static String leng;

    private boolean control_primera_vez = true;

    /**
     * Constructor. Carga el mapa con la configuracion adecuada. Muestra la última ciudad visitada
     * y los niveles disponibles del jugador, según su experiencia.
     *
     * @param savedInstanceState Bundle necesario.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);

        //Se coge de las preferencias la última ciudad visitada y otros datos.
        String ciudad = sharedPref.getString("ciudad", "pueblo_inicio");
        nivel = "";
        leng = sharedPref.getString("lenguaje","Java");

        //Se muestra si hay apuntes nuevos.
        SharedPreferences.Editor editor = sharedPref.edit();
        int i = 0;
        int duracion = 0;

        //apunte_nuevo_i guarda el título de cada nuevo apunte.
        String s = sharedPref.getString("apunte_nuevo_0", "");

        //Si hay algún apunte nuevo, se coloca el ícono con una exclamación
        if (!s.equals("")) {
            ((ImageButton) findViewById(R.id.boton_apuntes)).setImageResource(R.drawable.boton_apuntes_nuevo);
        }

        //Se muestra un Toast por cada nuevo apunte que se haya incluído
        while (!s.equals("")) {
            //Para que no se muestre cada vez que se entra en la actividad, tras mostrarlo
            //se guarda una preferencia booleana con el nombre del apunte.
            if (!sharedPref.getBoolean(s,false)) {
                //Se crea el Toast
                Toast toast = Toast.makeText(this, s + " se ha añadido a tu libro de apuntes.", Toast.LENGTH_SHORT);
                toast.getView().setBackgroundResource(R.drawable.toast);
                ((TextView) toast.getView().findViewById(android.R.id.message)).setTextColor(Color.BLACK);
                toast.show();

                //Para no volver a mostrarlo se crea una nueva preferencia
                editor.putBoolean(s, true);

                duracion += 2000;
            }

            s = sharedPref.getString("apunte_nuevo_" + ++i, "");
        }

        //Se habilita el scroll del TextView
        ((TextView) findViewById(R.id.info_texto)).setMovementMethod(new ScrollingMovementMethod());

        //Si hay apuntes nuevos se muestra una flecha encima del botón (solo la primera vez)
        //Se pone cuando hayan desaparecido los Toast.
        if(duracion != 0 && !sharedPref.getBoolean("control_nuevos_apuntes",false)){

            new Handler().postDelayed(new Runnable() {

                public void run() {
                    findViewById(R.id.flecha).setVisibility(View.VISIBLE);
                }

            }, duracion);

            //Para que no se muestre otra vez
            editor.putBoolean("control_nuevos_apuntes",true);
        }

        editor.apply();

        //Se añaden los lenguajes al spinner.
        final Spinner spinner = (Spinner) findViewById(R.id.spinner_lenguaje);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                //El spinner selecciona el primer item la primera vez que se inicia la actividad
                //Para que no cambie el idioma cada vez, la primera vez la ignoro:
                if(control_primera_vez)
                    control_primera_vez = false;
                else {
                    leng = parent.getItemAtPosition(pos).toString();
                    //Si ha cambiado:
                    if(!leng.equals(sharedPref.getString("lenguaje",""))) {
                        leng = parent.getItemAtPosition(pos).toString();
                        new AvisoCambiarIdioma().show(getFragmentManager(), "confirmacion");

                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        lista = (ListView) findViewById(R.id.lista_niveles);

        //Cuando se pulse un elemento de la lista, se mostrará información del nivel
        //en el LinearLayout "info_nivel".
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Se muestra toda la información del nivel en el layout de la derecha
                String nuevo_nivel = parserJSON.getNivel(l_niveles[position], "id");
                if(nuevo_nivel.equals(nivel)){
                    findViewById(R.id.info_nivel).setVisibility(View.GONE);
                    nivel = "";
                } else {
                    nivel = nuevo_nivel;
                    findViewById(R.id.info_texto).scrollTo(0, 0);
                    findViewById(R.id.info_nivel).setVisibility(View.VISIBLE);
                    findViewById(R.id.opciones).setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.info_texto)).setText(parserJSON.getNivel(l_niveles[position], "descripcion"));
                    ultima_etapa = sharedPref.getInt(nivel + "_max", 0);
                    findViewById(R.id.boton_etapa).setVisibility(ultima_etapa == 0 ? View.GONE : View.VISIBLE);
                }
            }

        });

        //Se inicializa la ciudad
        iniciarCiudad(ciudad);


    }

    /**
     * Muestra los datos de la ciudad y los niveles disponibles.
     *
     * @param ciudad con el identificador de la ciudad.
     */
    public void iniciarCiudad(String ciudad) {

        parserJSON = new ParserJSON(this, ciudad);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("ciudad", ciudad);
        editor.apply();

        //Se dibuja la minniatura de la ciudad
        ((ImageView) findViewById(R.id.ciudad_miniatura)).setImageResource(parserJSON.getCiudadMiniatura());
        ((TextView) findViewById(R.id.nombre_ciudad)).setText(parserJSON.getNombreCiudad());

        //Se dibujan las flechas si son necesarias
        findViewById(R.id.irNorte).setVisibility(parserJSON.getCiudad("norte").equals("") ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.irSur).setVisibility(parserJSON.getCiudad("sur").equals("") ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.irOeste).setVisibility(parserJSON.getCiudad("oeste").equals("") ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.irEste).setVisibility(parserJSON.getCiudad("este").equals("") ? View.INVISIBLE : View.VISIBLE);

        //Se cogen los nombres de los niveles disponibles en este mapa, según el nivel del jugador.
        l_niveles = parserJSON.getListaNiveles();

        ListaModificadaAdapter adapter = new ListaModificadaAdapter(this, R.layout.lista_misiones);

        for(int i = 0; i < l_niveles.length; i++){
            String s = parserJSON.getNivel(l_niveles[i], "id");
            adapter.add(l_niveles[i]);

            //Si la configuración de la primera etapa de este nivel aún no se ha hecho significa que es nuevo
            if (!sharedPref.getBoolean(s + "_0", false))
                adapter.setFlag(i, true);


        }

        lista.setAdapter(adapter);

        findViewById(R.id.info_nivel).setVisibility(View.INVISIBLE);
    }

    /**
     * Carga la ciudad existente en la direccion pulsada.
     *
     * @param view vista del botón pulsado.
     */
     public void irCiudad (View view){
        String sig_ciudad = "";

        switch(view.getId()) {
            case( R.id.irNorte):
                sig_ciudad = parserJSON.getCiudad("norte");
                break;
            case( R.id.irSur):
                sig_ciudad = parserJSON.getCiudad("sur");
                break;
            case(R.id.irEste):
                sig_ciudad = parserJSON.getCiudad("este");
                break;
            case(R.id.irOeste):
                sig_ciudad = parserJSON.getCiudad("oeste");
                break;
        }




        if(!sig_ciudad.equals("")) {
            iniciarCiudad(sig_ciudad);
        }

    }

    /**
     * Inicia IntroduccionActivity con el nivel elegido. Si se ha decidido
     * ir a la última etapa visitada, se mostrara esa.
     * @param view vista del botón pulsado.
     */
    public void irNivel(View view) {

        if(!nivel.equals("")) {
            Intent intent = new Intent(this, IntroduccionActivity.class);
            intent.putExtra("NIVEL", nivel);

            //Si el botón que se ha pulsado es el de ir a la última etapa visitada
            //Si no se pone, el valor por defecto al extraerlo del intent es 0.
            if(view.getId() == R.id.boton_etapa)
                intent.putExtra("ETAPA",ultima_etapa);

            startActivity(intent);
        }
    }

    /**
     * Inicia ApuntesActivity.
     *
     * @param view vista del botón pulsado.
     */
    public void irApuntes(View view) {
        Intent intent = new Intent(this, ApuntesActivity.class);
        startActivity(intent);
    }

    /**
     * Muestra las opciones del juego: reiniciar y cambiar idioma.
     *
     * @param view del botón pulsado.
     */
    public void mostrarOpciones(View view) {
        //Se oculta la info del nivel y se muestran/ocultan las opciones
        findViewById(R.id.info_nivel).setVisibility(View.GONE);
        findViewById(R.id.opciones).setVisibility(findViewById(R.id.opciones).getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        nivel = "";
    }

    /**
     * Reinicia el juego.
     *
     * @param view vista del botón pulsado.
     */
    public void reiniciar(View view) {
        new AvisoReinicio().show(getFragmentManager(), "confirmacion");
    }

    /**
     * Muestra el spinner con los idiomas disponibles.
     *
     * @param view del botón pulsado.
     */
    public void mostrarSpinner(View view) {
        findViewById(R.id.spinner_lenguaje).performClick();


    }

    /**
     * Sale de la aplicacion.
     */
    @Override
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    /**
     * Oculta la flecha al botón de apuntes, en caso
     * de que se vuelve desde ApuntesActivity.
     */
    @Override
    public void onResume() {
        super.onResume();

        //Se quita la ! si se pulsa el botón de Back desde ApuntesActivity y se vuelve a la actividad.
        ((ImageButton) findViewById(R.id.boton_apuntes)).setImageResource(sharedPref.getString("apunte_nuevo_0", "").equals("") ? R.drawable.boton_apuntes : R.drawable.boton_apuntes_nuevo);
        //Y la flecha si está visible
        findViewById(R.id.flecha).setVisibility (View.INVISIBLE);
        //Y se oculta el layout de la info
        findViewById(R.id.info_nivel).setVisibility(View.GONE);
        findViewById(R.id.opciones).setVisibility(View.GONE);


    }

    /**
     * Controla el Dialog al reiniciar el juego. Pide una confirmación para hacerlo.
     */
    public static class AvisoReinicio extends DialogFragment {

        /**
         * Crea el mensaje de aviso.
         * @param savedInstanceState Bundle necesario.
         * @return devuelve el Dialog
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(R.string.aviso_reinicio).setTitle(R.string.aviso_reinicio_titulo);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getActivity().getSharedPreferences("PREF",Context.MODE_PRIVATE).edit().clear().apply();
                    startActivity(new Intent(getActivity(), MainActivity.class));
                }
            });

            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //No se hace nada. Se cierra el diálogo.
                }
            });

            return builder.create();
        }
    }

    /**
     * Controla el Dialog al cambiar el idioma. Pide una confirmación para hacerlo.
     */
    public static class AvisoCambiarIdioma extends DialogFragment {

        /**
         * Crea el mensaje de aviso.
         * @param savedInstanceState Bundle necesario.
         * @return devuelve el Dialog
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(R.string.aviso_lenguaje).setTitle(R.string.aviso_lenguaje_titulo);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getActivity().getSharedPreferences("PREF",Context.MODE_PRIVATE).edit().clear().putString("lenguaje",leng).apply();
                    startActivity(new Intent(getActivity(), MainActivity.class));
                }
            });

            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //No se hace nada. Se cierra el diálogo.
                }
            });

            return builder.create();
        }
    }
}
