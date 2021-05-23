package com.alfonsomolina.tfg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/**
 * Muesta un teclado para escribir código y las líneas que ya han sido escritas.
 *
 * @author Alfonso Molina
 */
public class CrearCodigoActivity extends Activity {

    private KeyboardView teclado;
    private String nivel = "";
    private int etapa = 0;

    private ArrayList<String> titulos;              //Títulos de las ayudas
    private ArrayList<String> ayudas;               //Texto de las ayudas
    private int num_ayuda = 0;                      //Número de la lista de ayudas actual

    private int linea_actual;
    private ArrayList<Codigo> codigo;               //Cada elemento es una línea de código
    private ArrayList<String> variables;            //Lista con los nombres de las variables creadas
    private String[] constantes;                    //Constantes
    private int num_constantes;                     //Número de constantes desblqueadas
    private String[] tipos_variables;               //Tipo de variables
    private int num_variables;                      //Número de tipos desbloqueados
    private String lenguaje;                        //Lenguaje del código

    private ListView lista;
    private ListaModificadaAdapter adapter;
    private ListView popup;                         //Popup que se mostrará para ofrecer opciones (como variables creadas, constantes)
    private ArrayList<String> popup_texto;
    private ArrayAdapter<String> popup_adapter;


    /**
     * Constructor. Crea la actividad obteniendo el mensaje de ayuda y las líneas de muestra.
     *
     * @param savedInstanceState bundle necesario
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_crear_codigo);

        nivel = getIntent().getStringExtra("NIVEL");
        etapa = getIntent().getIntExtra("ETAPA", 0);

        final SharedPreferences sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);
        ParserJSON parserJSON = new ParserJSON(this, nivel, etapa);
        num_ayuda = 0;
        lenguaje = sharedPref.getString("lenguaje", "java");

        //MENSAJE DE AYUDA
        String[] lista_ayudas = parserJSON.getAyuda("ayuda");
        titulos = new ArrayList<>();
        ayudas = new ArrayList<>();
        int titulo_id;
        String lenguaje = sharedPref.getString("lenguaje", "Java");

        for (String nombre : lista_ayudas) {
            titulo_id = getResources().getIdentifier(
                    "ayuda_" + nombre + "_titulo" + "_" + lenguaje, "string", getPackageName());

            //Si hay una string particular para este lenguaje:
            if (titulo_id > 0) {
                titulos.add(getString(titulo_id));
                ayudas.add(getString(getResources().getIdentifier(
                        "ayuda_" + nombre + "_" + lenguaje, "string", getPackageName())));
                //Si no lo hay se coge el por defecto
            } else {

                titulo_id = getResources().getIdentifier(
                        "ayuda_" + nombre + "_titulo", "string", getPackageName());

                //Si hay uno genérico, se pone
                if (titulo_id > 0) {
                    titulos.add(getString(titulo_id));
                    ayudas.add(getString(getResources().getIdentifier(
                            "ayuda_" + nombre, "string", getPackageName())));
                }
                //Y si esa ayuda sólo está disponible para un mensaje concreto no se hace nada.
            }
        }

        if (titulos.size() > 0)
            mostrarAyuda(findViewById(R.id.mensajeAyuda));
        else
            cerrar(findViewById(R.id.boton_x));

        //Se habilita el scroll del TextView
        ((TextView) findViewById(R.id.mensajeAyuda)).setMovementMethod(new ScrollingMovementMethod());

        //CABECERA
        ((TextView) findViewById(R.id.cabecera)).setText(getString(getResources().getIdentifier(
                "cabecera_" + lenguaje, "string", getPackageName())));
        ((TextView) findViewById(R.id.pie)).setText(getString(getResources().getIdentifier(
                "pie_" + lenguaje, "string", getPackageName())));


        //TECLADO
        final Keyboard mKeyboard = new Keyboard(this, R.xml.teclado1);
        teclado = (KeyboardView) findViewById(R.id.teclado);
        teclado.setKeyboard(mKeyboard);
        teclado.setPreviewEnabled(false);
        teclado.setOnKeyboardActionListener(tecladoActionListener);
        ocultarTeclas(1);


        //POPUP
        popup = (ListView) findViewById(R.id.popup);
        popup_texto = new ArrayList<>();
        popup_adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_dark, popup_texto);
        popup.setAdapter(popup_adapter);
        variables = new ArrayList<>();
        constantes = new String[]{"NADA", "META", "OBSTACULO", "OBSTACULO (irrompible)", "BESTIA", "PERSONA"};
        num_constantes = parserJSON.getNumeroConstantes();
        tipos_variables = new String[]{"int", "boolean", "float", "double", "string"};
        num_variables = parserJSON.getNumeroVariables();


        //LISTA
        lista = (ListView) findViewById(R.id.lista_codigo);
        adapter = new ListaModificadaAdapter(this, R.layout.lista_linea_de_codigo);

        //Se cogen las líneas de código iniciales y se escriben
        codigo = parserJSON.getLineasCodigo();
        for (int i = 0; i < codigo.size(); i++) {
            adapter.add(codigo.get(i).escribirLinea());
            if (codigo.get(i).getCodigo() >= Codigo.DECLARAR) {

                variables.add(codigo.get(i).getCadena(1));
            }
        }

        linea_actual = parserJSON.getPosicionInicial();
        adapter.setFlag(linea_actual, true);        //Se resalta la línea actual


        lista.setAdapter(adapter);
        lista.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);

        //Cuando se clica en una línea, se pasa el cursor a esa y se muestra el teclado.
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                //Se oculta el popup
                popup.setVisibility(View.GONE);

                //Si la línea actual es 0 y la posición también, se añade una nueva línea en 0.
                if (position == 0 && linea_actual == 0) {
                    adapter.setFlag(0, false);
                    adapter.add(linea_actual, " ");
                    adapter.setFlag(0, true);
                    codigo.add(linea_actual, new Codigo(sharedPref.getString("lenguaje", "java"), 0));
                    if (teclado.getVisibility() != View.VISIBLE) {
                        teclado.setKeyboard(new Keyboard(getApplicationContext(), R.xml.teclado1));
                        ocultarTeclas(1);
                        teclado.setVisibility(View.VISIBLE);
                    }

                    lista.invalidateViews();

                } else {
                    //Si se pulsa en una nueva línea desde la asignación de una variable, se guarda.
                    if (codigo.get(linea_actual).getCodigo() >= Codigo.DECLARAR) {
                        variables.add(codigo.get(linea_actual).getCadena());

                    }

                    adapter.setFlag(linea_actual, false);
                    linea_actual = position;
                    if (teclado.getVisibility() != View.VISIBLE) {
                        teclado.setKeyboard(new Keyboard(getApplicationContext(), R.xml.teclado1));
                        ocultarTeclas(1);
                        teclado.setVisibility(View.VISIBLE);
                    }
                    adapter.setFlag(linea_actual, true);

                    //Se rehace la tabulación
                    if (linea_actual != 0 && codigo.get(linea_actual - 1).getCodigo() >= Codigo.IF && codigo.get(linea_actual - 1).getCodigo() <= Codigo.FIN_BUCLE_INICIO) {
                        if (codigo.get(linea_actual).getCodigo() != Codigo.CIERRE) {
                            codigo.get(linea_actual).setTab(codigo.get(linea_actual - 1).getTab() + 1);
                        }
                    } else if (linea_actual != 0 && codigo.get(linea_actual).getCodigo() == Codigo.CIERRE) {
                        codigo.get(linea_actual).setTab(codigo.get(linea_actual - 1).getTab() - 1);
                    } else if (linea_actual != 0) {
                        codigo.get(linea_actual).setTab(codigo.get(linea_actual - 1).getTab());
                    }
                    adapter.setText(linea_actual, codigo.get(linea_actual).escribirLinea());
                    lista.invalidateViews();

                }

            }

        });


    }


    //MÉTODOS PARA MOSTRAR EL TEXTO DE AYUDA

    /**
     * Muestra la ayuda.
     *
     * @param view vista del botón pulsado.
     */
    public void mostrarAyuda(View view) {

        num_ayuda = 0;

        if (titulos.size() > 0) {
            ((TextView) findViewById(R.id.titulo)).setText(titulos.get(num_ayuda));
            ((TextView) findViewById(R.id.mensajeAyuda)).setText(Html.fromHtml(ayudas.get(num_ayuda), new ImageGetter(), null));
            findViewById(R.id.mensajeAyuda).scrollTo(0, 0);
            findViewById(R.id.boton_izquierda).setVisibility(View.INVISIBLE);
            findViewById(R.id.boton_derecha).setVisibility(num_ayuda == titulos.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            findViewById(R.id.mensajeAyuda).setVisibility(View.VISIBLE);
            findViewById(R.id.boton_x).setVisibility(View.VISIBLE);
            findViewById(R.id.titulo).setVisibility(View.VISIBLE);
        }
    }

    /**
     * Muestra la siguiente ayuda de las disponibles.
     *
     * @param view vista del botón pulsado.
     */
    public void pasarDerecha(View view) {

        if (num_ayuda < titulos.size() - 1) {
            num_ayuda++;
            ((TextView) findViewById(R.id.titulo)).setText(titulos.get(num_ayuda));
            ((TextView) findViewById(R.id.mensajeAyuda)).setText(Html.fromHtml(ayudas.get(num_ayuda), new ImageGetter(), null));
            findViewById(R.id.mensajeAyuda).scrollTo(0, 0);

            //Si es la última no se musetra la flecha para la derecha
            if (num_ayuda == titulos.size() - 1) {
                findViewById(R.id.boton_derecha).setVisibility(View.INVISIBLE);
            }
        }
        findViewById(R.id.boton_izquierda).setVisibility(View.VISIBLE);

    }

    /**
     * Muestra la ayuda anterior de las disponibles.
     *
     * @param view vista del botón pulsado.
     */
    public void pasarIzquierda(View view) {

        if (num_ayuda > 0) {
            num_ayuda--;
            ((TextView) findViewById(R.id.titulo)).setText(titulos.get(num_ayuda));
            ((TextView) findViewById(R.id.mensajeAyuda)).setText(Html.fromHtml(ayudas.get(num_ayuda), new ImageGetter(), null));
            findViewById(R.id.mensajeAyuda).scrollTo(0, 0);

            //Si es la primera no se muestra la fecla para la izquierda
            if (num_ayuda == 0) {
                findViewById(R.id.boton_izquierda).setVisibility(View.INVISIBLE);
            }
        }
        findViewById(R.id.boton_derecha).setVisibility(View.VISIBLE);
    }

    /**
     * Cierra las ayudas.
     *
     * @param view vista del botón pulsado.
     */
    public void cerrar(View view) {
        findViewById(R.id.mensajeAyuda).setVisibility(View.GONE);
        findViewById(R.id.boton_x).setVisibility(View.GONE);
        findViewById(R.id.boton_derecha).setVisibility(View.GONE);
        findViewById(R.id.boton_izquierda).setVisibility(View.GONE);
        findViewById(R.id.teclado).setVisibility(View.VISIBLE);
        findViewById(R.id.titulo).setVisibility(View.GONE);
    }


    /**
     * Oculta las teclas que no hayan sido desbloqueadas.
     *
     * @param teclado entero con el número del teclado activo.
     */
    public void ocultarTeclas(int teclado) {
        Keyboard mKeyboard = this.teclado.getKeyboard();

        //Ponemos las teclas desbloqueadas en este nivel:
        SharedPreferences sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);

        int maxTeclas = 0;

        switch (teclado) {
            case (1):
                maxTeclas = 20;
                break;
            case (2):
                maxTeclas = 16;
                break;
            case (3):
                maxTeclas = 29;
                break;
        }

        //Si la tecla no está desbloqueada (no está esta preferencia) se
        //cambia la altura de la tecla a 0 y se elimina el nombre
        for (int i = 0; i <= maxTeclas; i++)
            if (!sharedPref.getBoolean("tecla_" + teclado + "_" + i, false)) {
                mKeyboard.getKeys().get(i).label = "";
                mKeyboard.getKeys().get(i).height = 0;
            }


    }

    /**
     * Lanza de nuevo la actividad con el resumen de la misión.
     *
     * @param view vista del botón pulsado.
     */
    public void mostrarResumen(View view) {
        Intent intent = new Intent(this, ResumenMisionActivity.class);
        intent.putExtra("NIVEL", nivel);
        intent.putExtra("ETAPA", etapa);
        intent.putExtra("DESDE_CODIGO", true);
        startActivity(intent);
    }

    /**
     * Muestra el código escrito dentro de una clase.
     *
     * @param view vista del botón pulsado.
     */
    public void mostrarCabecera(View view) {
        Button b = (Button) view;
        if (b.getText().equals("+")) {
            b.setText("-");
            findViewById(R.id.cabecera).setVisibility(View.VISIBLE);
            findViewById(R.id.pie).setVisibility(View.VISIBLE);
            findViewById(R.id.teclado).setVisibility(View.GONE);
        } else {
            b.setText("+");

            findViewById(R.id.cabecera).setVisibility(View.GONE);
            findViewById(R.id.pie).setVisibility(View.GONE);
        }

    }

    //Al pulsar el botón para ir a la actividad de resolver el código

    /**
     * Inicia ResolverCodigoActivity para ejecutar las líneas escritas.
     *
     * @param view vista del botón pulsado.
     */
    public void resolver(View view) {
        if (codigo != null && findViewById(R.id.mensajeAyuda).getVisibility() == View.GONE) {
            Intent intent = new Intent(this, ResolverCodigoActivity.class);
            intent.putExtra("CODIGO", codigo);
            intent.putExtra("NIVEL", nivel);
            intent.putExtra("ETAPA", etapa);
            startActivity(intent);
        }
    }

    /**
     * Al pulsar el boton de "atrás", oculta las ayudas y el popup y, si no estaban
     * visibles, muestra un aviso solicitando una confirmación para salir.
     */
    @Override
    public void onBackPressed() {
        if (findViewById(R.id.mensajeAyuda).getVisibility() == View.VISIBLE)
            //Si está mostrándose la ayuda, se cierra.
            cerrar(findViewById(R.id.mensajeAyuda));
        else if (findViewById(R.id.popup).getVisibility() == View.VISIBLE)
            popup.setVisibility(View.GONE);
        else
            //Si no, se muestra un dialog para confirmar que se quiere salir al mapa
            new Aviso().show(getFragmentManager(), "confirmacion");

    }

    /**
     * Clase necesaria para mostrar imágenes en los TextView.
     */
    private class ImageGetter implements Html.ImageGetter {

        public Drawable getDrawable(String source) {

            Drawable d = getResources().getDrawable(getResources().getIdentifier(source, "drawable", getPackageName()));
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            return d;
        }
    }


    /**
     * Listener que escribe el código cuando se pulsa una tecla.
     */
    private KeyboardView.OnKeyboardActionListener tecladoActionListener =
            new KeyboardView.OnKeyboardActionListener() {

                @Override
                public void onKey(int primaryCode, final int[] keyCodes) {
                    Codigo c = codigo.get(linea_actual);
                    //Ocultar teclado
                    if (primaryCode == -3) {
                        teclado.setVisibility(View.GONE);

                        //Tres teclas cambian el teclado visible
                    } else if (primaryCode == -10) {
                        teclado.setKeyboard(new Keyboard(getApplicationContext(), R.xml.teclado1));
                        ocultarTeclas(1);
                    } else if (primaryCode == -11) {
                        teclado.setKeyboard(new Keyboard(getApplicationContext(), R.xml.teclado2));
                        ocultarTeclas(2);
                    } else if (primaryCode == -12) {
                        teclado.setKeyboard(new Keyboard(getApplicationContext(), R.xml.teclado3));
                        ocultarTeclas(3);
                    } else if (primaryCode == 0) {
                        Toast.makeText(CrearCodigoActivity.this, "Esa tecla aún no está implementada.\n ¡Lo siento!", Toast.LENGTH_SHORT).show();

                        //Enter
                    } else if (primaryCode == -1) {

                        adapter.setFlag(linea_actual, false);

                        linea_actual++;

                        adapter.add(linea_actual, " ");
                        adapter.setFlag(linea_actual, true);

                        codigo.add(linea_actual, new Codigo(lenguaje, 0));

                        //Si la línea anterior era el inicio de un bucle, se aumenta la tabulación de la siguiente
                        if (c.getCodigo() >= Codigo.IF && c.getCodigo() <= Codigo.FIN_BUCLE_INICIO)
                            codigo.get(linea_actual).setTab(c.getTab() + 1);
                        else
                            codigo.get(linea_actual).setTab(c.getTab());
                        //Borrar
                    } else if (primaryCode == -5) {

                        //Si la línea está vacía, se elimina (a no ser que sea la única)
                        if (c.getCodigo() == 0) {
                            if (codigo.size() > 1) {
                                adapter.remove(linea_actual);
                                codigo.remove(linea_actual);
                                if (linea_actual != 0) {
                                    linea_actual--;
                                }
                                adapter.setFlag(linea_actual, true);
                            }

                            //Si es la creación de una variable, se elimina la variable.
                        } else if (c.getCodigo() >= Codigo.DECLARAR) {
                            int indice = variables.indexOf(c.getCadena());
                            if (indice >= 0)
                                variables.remove(indice);
                            codigo.set(linea_actual, new Codigo(lenguaje, 0));

                            //Si es un cierre, se borra dos veces al final, por el parámetro oculto.
                        } else if (c.getCodigo() == Codigo.CIERRE) {
                            c.eliminarParam();
                            if (c.getNumParam() == 1)
                                c.eliminarParam();
                            //Y en todos los demás caso se borra un parámetro.
                        } else
                            c.eliminarParam();

                        //Es un número
                    } else if (primaryCode < 10) {

                        //Si ya hay un número se añade el nuevo, sino se crea de cero.
                        if (c.getParam() == Codigo.NUMERO)
                            c.setCadena(c.getCadena() + primaryCode);
                        else {
                            if (c.getCodigo() == 0)
                                c.setCodigo(Codigo.NUMERO);
                            c.addCadena(Codigo.NUMERO, "" + primaryCode);

                        }

                        //Es una letra == Estamos declarando una variable
                    } else if (primaryCode < Codigo.FIN_LETRAS) {

                        String s = c.getCadena();

                        //Borrar letra
                        if (primaryCode == 256) {
                            if (!s.isEmpty()) {
                                s = s.substring(0, s.length() - 1);
                                c.setCadena(s);
                            } else {
                                c.setCodigo(0);
                                teclado.setKeyboard(new Keyboard(getApplicationContext(), R.xml.teclado1));
                                ocultarTeclas(1);
                            }


                            //Acabar
                        } else if (primaryCode == 257) {
                            boolean bien = true;

                            //Se comprueba si está vacío
                            if (s.equals(""))
                                bien = false;

                            if (bien) {
                                variables.add(s);

                                //Y se hace enter
                                adapter.setFlag(linea_actual, false);
                                linea_actual++;
                                adapter.add(linea_actual, " ");
                                adapter.setFlag(linea_actual, true);
                                codigo.add(linea_actual, new Codigo(lenguaje, 0));
                                codigo.get(linea_actual).setTab(c.getTab());

                            } else {
                                Toast.makeText(CrearCodigoActivity.this, "Nombre de variable inválido", Toast.LENGTH_SHORT).show();
                                codigo.get(linea_actual).setCodigo(0);
                            }
                            teclado.setKeyboard(new Keyboard(getApplicationContext(), R.xml.teclado1));
                            ocultarTeclas(1);

                            //Se añade una letra
                        } else if (primaryCode < 255) {
                            //Si el CAPS LOCK está activado y es una letra
                            if (teclado.getKeyboard().getKeys().get(31).on && primaryCode > 95)
                                s = s + Character.toString((char) (primaryCode - 32));
                            else
                                s = s + Character.toString((char) primaryCode);
                            c.setCadena(s);
                        }

                        //Añadir una constante (al asignar o como parámetro de una condición)
                    } else if (primaryCode == Codigo.CTE) {
                        popup_texto.clear();
                        for (int i = 0; i < num_constantes; i++)
                            popup_texto.add(constantes[i]);
                        popup_texto.add("Salir");
                        popup_adapter.notifyDataSetChanged();
                        popup.setVisibility(View.VISIBLE);
                        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view,
                                                    int position, long id) {

                                //Según la posición se sabe qué constante añadir
                                //"Vacio" es la primera y después van en orden
                                if (position != popup_adapter.getCount() - 1)
                                    codigo.get(linea_actual).addParam(Codigo.CTE_VACIO + position);
                                popup.setVisibility(View.GONE);
                                adapter.setText(linea_actual, codigo.get(linea_actual).escribirLinea());
                                lista.invalidateViews();

                            }

                        });

                        //Crear una variable
                    } else if (primaryCode == Codigo.DECLARAR) {
                        popup_texto.clear();
                        for (int i = 0; i < num_variables; i++)
                            popup_texto.add(tipos_variables[i]);
                        popup_texto.add("Salir");
                        popup_adapter.notifyDataSetChanged();
                        popup.setVisibility(View.VISIBLE);
                        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view,
                                                    int position, long id) {

                                //INT es la primera, y después en orden.
                                if (position != popup_adapter.getCount() - 1) {
                                    teclado.setKeyboard(new Keyboard(getApplicationContext(), R.xml.teclado_letras));
                                    codigo.get(linea_actual).setCodigo(Codigo.VAR_INT + position);
                                    codigo.get(linea_actual).addParam(Codigo.VARIABLE);
                                }
                                popup.setVisibility(View.GONE);
                                adapter.setText(linea_actual, codigo.get(linea_actual).escribirLinea());
                                lista.invalidateViews();

                            }

                        });

                        //Escribir una variable, para usar su valor o darle uno
                    } else if (primaryCode == Codigo.VARIABLE) {

                        popup_texto.clear();
                        popup_texto.add("Crear");
                        popup_texto.addAll(variables);
                        popup_texto.add("Salir");
                        popup_adapter.notifyDataSetChanged();
                        popup.setVisibility(View.VISIBLE);
                        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view,
                                                    int position, long id) {
                                if (position == 0) {
                                    onKey(Codigo.DECLARAR, keyCodes);
                                } else if (position != popup_adapter.getCount() - 1) {

                                    //Se guarda como parámetro el nombre de la variable
                                    if (codigo.get(linea_actual).getCodigo() == 0) {
                                        codigo.get(linea_actual).setCodigo(Codigo.ASIGNACION);
                                        codigo.get(linea_actual).setCadena(popup_texto.get(position));

                                    } else {
                                        codigo.get(linea_actual).addCadena(Codigo.VARIABLE,
                                                popup_texto.get(position));
                                    }
                                }

                                if (position != 0) {
                                    popup.setVisibility(View.GONE);
                                    adapter.setText(linea_actual, codigo.get(linea_actual).escribirLinea());
                                    lista.invalidateViews();
                                }
                            }


                        });

                        //Se ha pulsado una constante desde fuera del popup (direcciones).
                    } else if (primaryCode < Codigo.FIN_CONSTANTES) {

                        //Las constantes no se pueden escribir si no hay nada
                        if (c.getCodigo() != 0) {
                            c.addParam(primaryCode);
                        } else {
                            Toast.makeText(CrearCodigoActivity.this, "¡No puedes escribir ahí!", Toast.LENGTH_SHORT).show();
                        }

                        //Acciones: andar, mirar, hablar, golpear
                    } else if (primaryCode < Codigo.FIN_ACCIONES) {
                        if (c.getCodigo() == 0)
                            c.setCodigo(primaryCode);

                        else if (c.getCodigo() < Codigo.FIN_ACCIONES) {
                            adapter.setFlag(linea_actual, false);
                            linea_actual++;
                            adapter.add(linea_actual, " ");
                            adapter.setFlag(linea_actual, true);
                            codigo.add(linea_actual, new Codigo(lenguaje, primaryCode));
                            codigo.get(linea_actual).setTab(c.getTab());
                        } else if (c.getCodigo() != Codigo.CIERRE || c.getParam(1) == Codigo.DO + 50)
                            c.addParam(primaryCode);
                        else
                            Toast.makeText(CrearCodigoActivity.this, "¡No puedes escribir ahí!", Toast.LENGTH_SHORT).show();

                        //Iniciar un bucle: if, else if, else, while.
                    } else if (primaryCode < Codigo.FIN_BUCLE_INICIO) {
                        if (c.getCodigo() == Codigo.CIERRE) {
                            adapter.setFlag(linea_actual, false);
                            linea_actual++;
                            adapter.add(linea_actual, " ");
                            adapter.setFlag(linea_actual, true);
                            codigo.add(linea_actual, new Codigo(lenguaje, 0));
                            c = codigo.get(linea_actual);
                            c.setTab(codigo.get(linea_actual - 1).getTab());
                        }
                        c.setCodigo(primaryCode);

                        codigo.add(linea_actual + 1, new Codigo(lenguaje, Codigo.CIERRE));
                        codigo.get(linea_actual + 1).addParam(primaryCode + 50);
                        codigo.get(linea_actual + 1).setTab(c.getTab());
                        adapter.add(linea_actual + 1, codigo.get(linea_actual + 1).escribirLinea());


                        //Si es un cierre, se busca en los anteriores para poner el tipo correcto
                    } else if (primaryCode == Codigo.CIERRE) {

                        //Busco el origen del cierre primero
                        int i = linea_actual;
                        Codigo cc;
                        int cont = 0;
                        do {
                            i--;
                            cc = codigo.get(i);

                            //Si en esta línea empieza un cierre de bucle
                            if (cc.getCodigo() == Codigo.CIERRE) {
                                cont++;
                                //Si he encontrado el inicio
                            } else if (cc.getCodigo() >= Codigo.IF && cc.getCodigo() < Codigo.FIN_BUCLE_INICIO) {
                                cont--;
                            }
                        } while (cont >= 0 && i > 0);

                        //Si se ha encontrado:
                        if (cont < 0) {
                            c.setCodigo(Codigo.CIERRE);
                            c.addParam(codigo.get(i).getCodigo() + 50);
                            if (c.getParam(1) == Codigo.DO + 50) {
                                c.addParam(Codigo.WHILE);
                            }
                        } else {
                            Toast.makeText(CrearCodigoActivity.this, "No se encuentra el inicio del bloque.", Toast.LENGTH_SHORT).show();
                        }


                        //Si es un comparador: ==, !=
                        //Solo se puede escribir en un condicional o bucle
                    } else if (primaryCode < Codigo.FIN_COMP) {
                        if (c.getCodigo() >= Codigo.IF && c.getCodigo() < Codigo.FIN_BUCLE)
                            c.addParam(primaryCode);
                        else
                            Toast.makeText(CrearCodigoActivity.this, "¡No puedes escribir ahí!", Toast.LENGTH_SHORT).show();

                    }

                    adapter.setText(linea_actual, codigo.get(linea_actual).escribirLinea());
                    lista.invalidateViews();

                }

                @Override
                public void onPress(int arg0) {
                }

                @Override
                public void onRelease(int primaryCode) {
                }

                @Override
                public void onText(CharSequence text) {
                }

                @Override
                public void swipeDown() {
                }

                @Override
                public void swipeLeft() {
                }

                @Override
                public void swipeRight() {
                }

                @Override
                public void swipeUp() {
                }
            };
}