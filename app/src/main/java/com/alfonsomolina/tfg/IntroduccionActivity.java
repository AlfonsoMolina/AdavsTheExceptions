package com.alfonsomolina.tfg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * Muestra a dos personajes hablando, introduciendo el nivel y avanzando en la trama
 * del videojuego.
 */
 public class IntroduccionActivity extends Activity  {

    private ParserJSON parserJSON;
    private String nivel;
    private int etapa;

    private int numMensaje = 0;                         //Contador de los mensajes mostrados

    private ListaModificadaAdapter mensajeAdapter;

    private ImageView imagenPersDcha;
    private AnimationDrawable animacionPersDcha;
    private ImageView imagenPersIzq;
    private AnimationDrawable animacionPersIzq;

    /**
     * Constructor. Realiza la inicializacion de la etapa.
     * @param savedInstanceState Bundle necesario.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduccion);

        nivel = getIntent().getStringExtra("NIVEL");
        etapa = getIntent().getIntExtra("ETAPA", 0);

        parserJSON = new ParserJSON(this, nivel, etapa);

        //Se guardan las views de las imágenes
        ImageView imagenFondo = (ImageView) findViewById(R.id.imagen_fondo);

        imagenFondo.setImageResource(parserJSON.getFondo());

        imagenPersDcha = (ImageView) findViewById(R.id.persDcha);
        imagenPersIzq = (ImageView) findViewById(R.id.persIzq);

        //Se inicializa la lista.
        ListView lista = (ListView) findViewById(R.id.dialogo);
        mensajeAdapter = new ListaModificadaAdapter(getApplicationContext(), R.layout.lista_chat);
        lista.setAdapter(mensajeAdapter);
        lista.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);     //Para que haga autoscroll al final

        //Listener llamado pulsador para que, al pulsar, se muestre un nuevo mensaje.
        final View.OnClickListener pulsador = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMensaje();
            }

        };

        SharedPreferences sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();


        //Si la configuración de este nivel aún no se ha hecho
        if(!sharedPref.getBoolean(nivel+"_"+etapa,false)) {

            //Se esconde el botón de "Saltar". La primera vez debe leerse el chat.
            findViewById(R.id.boton_saltar).setVisibility(View.GONE);

            //Se debloquean las teclas de este nivel:
            int i = 0;
            int[][] teclas = parserJSON.getTeclas();
            while (i < teclas.length) {
                editor.putBoolean("tecla_" + teclas[i][0] + "_" + teclas[i][1], true);
                i++;
            }


            //Se añaden los mensajes de ayuda del nivel al libro de apuntes.
            int total = sharedPref.getInt("ayuda_total",0);
            String [] s = parserJSON.getAyuda("ayuda");
            String lenguaje = sharedPref.getString("lenguaje","Java");
            int titulo_id;

            for (i = 0; i < s.length; i++) {
                titulo_id = getResources().getIdentifier("ayuda_"+s[i]+"_titulo"+"_"+lenguaje,"string",getPackageName());

                //Si hay una string particular para este lenguaje:
                if(titulo_id > 0) {
                    editor.putInt("ayuda_titulo_"+total,titulo_id);
                    editor.putInt("ayuda_texto_"+total,getResources().getIdentifier("ayuda_" + s[i]+"_"+lenguaje, "string", getPackageName()));
                    total++;
                //Si no lo hay se mira si hay uno por defecto
                }else{
                    titulo_id = getResources().getIdentifier("ayuda_" + s[i] + "_titulo", "string", getPackageName());

                    //Si hay uno genérico, se pone
                    if(titulo_id > 0) {
                        editor.putInt("ayuda_titulo_" + total, titulo_id);
                        editor.putInt("ayuda_texto_" + total, getResources().getIdentifier("ayuda_" + s[i], "string", getPackageName()));
                        total++;
                    }
                    //Y si esa ayuda sólo está disponible para un mensaje concreto no se hace nada.
                }
            }

            editor.putInt("ayuda_total", total);

            //Se guarda como nivel visitado y configurado
            editor.putBoolean(nivel + "_" + etapa, true);

        }


        //Si es la primera vez que se entra, se pone la imagen de ayuda
        if(!sharedPref.getBoolean("control_pulsar_chat", false)){
            //Se guarda para que no vuelva a salir
            editor.putBoolean("control_pulsar_chat",true);
            //Se pone la imagen de fondo y
            findViewById(R.id.clicks).setBackgroundResource(R.drawable.pulsa);
            //un listener. Al pulsar se quita la imagen, se muestra un mensaje y se pone el listener normal
            findViewById(R.id.clicks).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    findViewById(R.id.clicks).setBackgroundResource(0);
                    findViewById(R.id.clicks).setOnClickListener(pulsador);
                    getMensaje();
                }
            });
        }
        else {

            //Se activa el listener para mostrar nuevos mensajes
            findViewById(R.id.clicks).setOnClickListener(pulsador);
        }

        editor.apply();


    }

    /**
     * Muestra un mensaje más cuando se pulsa la pantalla. Se encarga de mestrar la animación y el texto.
     */
    public void getMensaje(){
        int resID = parserJSON.getPersona(numMensaje); //el resId.
        boolean posicion = parserJSON.getPosicion(numMensaje);


        //Ya no hay más mensajes. Se muestra el botón para ir a la misión y se oculta el de saltar.
        if (resID == -1){
            findViewById(R.id.boton_siguiente).setVisibility(View.VISIBLE);
            findViewById(R.id.boton_saltar).setVisibility(View.GONE);

        //Hay que reactivar la animación
        } else if (resID == 0) {
            AnimationDrawable animacion = posicion ? animacionPersIzq : animacionPersDcha;

            //Se reinicializa la animación
            animacion.setVisible(false, true);
            animacion.start();


        //Animación nueva.
        } else {
            ImageView imagen = posicion ? imagenPersIzq : imagenPersDcha;

            //Se carga el nuevo dibujo.
            imagen.setBackgroundResource(resID);
            AnimationDrawable animacion = (AnimationDrawable) imagen.getBackground();
            animacion.start();

            if (posicion)
                animacionPersIzq = animacion;
            else
                animacionPersDcha = animacion;


        }

        //El primer elemento: false = derecha, true = izquierda
        if (!parserJSON.getMensaje(numMensaje).equals("")) {
            //Se pone el mensaje. Si es igual a 0 significa que va a la izquierda, así que se pone true en el adapter.
            //si es 1 es que está a la derecha, y se pone false.
            mensajeAdapter.add(new ElementoModificado(posicion, parserJSON.getMensaje(numMensaje)));
            mensajeAdapter.notifyDataSetChanged();
        }
        numMensaje++;

        //Si hay que dibujar el siguiente inmediatamente.
        if(parserJSON.getSig(numMensaje -1))
            getMensaje();

    }


    /**
     * Lanza la siguiente actividad del nivel. Suele ser ResumenMisionActivity, pero si el nivel
     * es únicamente un diálogo se hace la configuracion final de la etapa y se va a la siguiente.
     * Puede darse el caso de que la etapa final sea un diálogo, por lo que hay que hacer
     * la configuración final del nivel (aumentar la experiencia y las ayudas extra).
     * @param view vista del boton pulsado.
     */
    public void irNivel(View view) {
        Intent intent;

        //Si la etapa era sólo de introducción, se va a la siguiente
        if (parserJSON.dialogo() && parserJSON.haySiguienteNivel()) {
            intent = new Intent(this, IntroduccionActivity.class);
            intent.putExtra("NIVEL", nivel);
            intent.putExtra("ETAPA", etapa + 1);

        //Si era sólo de diálogo es el último nivel, se vuelve al mapa.
        } else if (parserJSON.dialogo()){       //Si es el último
            intent = new Intent(this, MapaActivity.class);

            //Se añaden los apuntes que se desbloquean al completar la etapa.
            SharedPreferences sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();


            if (!sharedPref.getBoolean(nivel+"_extra",false)) {
                String [] s = parserJSON.getAyuda("ayuda_extra");
                if(s.length > 0) {
                    int total = sharedPref.getInt("ayuda_total", 0);

                    String lenguaje = sharedPref.getString("lenguaje", "Java");
                    int titulo_id;
                    int cont = 0;

                    for (int i = 0; i < s.length; i++) {
                        titulo_id = getResources().getIdentifier("ayuda_" + s[i] + "_titulo" + "_" + lenguaje, "string", getPackageName());

                        //Si hay una string particular para este lenguaje:
                        if (titulo_id > 0) {
                            editor.putInt("ayuda_titulo_" + total, titulo_id);
                            editor.putInt("ayuda_texto_" + total, getResources().getIdentifier("ayuda_" + s[i] + "_" + lenguaje, "string", getPackageName()));
                            editor.putString("apunte_nuevo_" + cont++, getString(titulo_id));
                            total++;

                            //Si no lo hay se coge el por defecto
                        } else {
                            titulo_id = getResources().getIdentifier("ayuda_" + s[i] + "_titulo", "string", getPackageName());

                            //Si hay uno genérico, se pone
                            if (titulo_id > 0) {
                                editor.putInt("ayuda_titulo_" + total, titulo_id);
                                editor.putInt("ayuda_texto_" + total, getResources().getIdentifier("ayuda_" + s[i], "string", getPackageName()));
                                editor.putString("apunte_nuevo_" + cont++, getString(titulo_id));
                                total++;
                            }
                            //Y si esa ayuda sólo está disponible para un mensaje concreto no se hace nada.
                        }
                    }

                    editor.putInt("ayuda_total", total);
                }
                //Para no volver a añadir las ayudas extra cada vez que se realiza el nivel
                editor.putBoolean(nivel+"_extra",true);
                //Se actualiza la experiencia.
                editor.putInt("experiencia",sharedPref.getInt("experiencia",1)+parserJSON.getExperiencia());
            }

            //El nivel máximo visitado vuelve a ser el cero
            editor.putInt(nivel+"_max",0);

            editor.apply();

        //A la siguiente fase de la etapa.
        } else {
            intent = new Intent(this, ResumenMisionActivity.class);
            intent.putExtra("NIVEL", nivel);
            intent.putExtra("ETAPA", etapa);
        }

        startActivity(intent);
    }

    /**
     * Al pulsar el botón de "atrás" se solicita una confirmación para salir.
     */
    @Override
    public void onBackPressed() {
        new Aviso().show(getFragmentManager(), "confirmacion");
    }
}
