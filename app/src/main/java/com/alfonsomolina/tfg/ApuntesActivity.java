package com.alfonsomolina.tfg;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;


/**
 * Muestra los apuntes desbloqueados.
 *
 * @author Alfonso Molina
 */
public class ApuntesActivity extends Activity {

    private TextView ayuda;
    private TextView titulo;
    private int pagina_actual;
    private int total_paginas;
    private ArrayList<String> titulos;
    private ArrayList<String> ayudas;
    ListView indice;
    SharedPreferences sharedPref;

    /**
     * Constructor. Crea la clase leyendo las preferencias compartidas y cargando las ayudas desbloqueadas.
     *
     * @param savedInstanceState bundle necesario.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apuntes);

        indice = (ListView) findViewById(R.id.indice);
        titulo = (TextView) findViewById(R.id.titulo);
        ayuda = (TextView) findViewById(R.id.mensajeAyuda);


        //Se cargan las páginas de las preferencias
        sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);
        total_paginas = sharedPref.getInt("ayuda_total", 0);
        titulos = new ArrayList<>();
        ayudas = new ArrayList<>();

        for (int i = 0; i < total_paginas; i++) {
            titulos.add(getString(sharedPref.getInt("ayuda_titulo_" + i, R.string.titulo)));
            ayudas.add(getString(sharedPref.getInt("ayuda_texto_" + i, R.string.mensaje_ayuda)));
        }

        //Si hay alguno nuevo, se muestra ese inicialmente
        String s = sharedPref.getString("apunte_nuevo_0", "");
        pagina_actual = titulos.indexOf(s);
        if (pagina_actual == -1) pagina_actual = 0;

        //Si no hay nada se muestra vacío. Si hay algo, se muestra la primera página o una no leída.
        if (total_paginas == 0) {
            titulo.setText("Libro de apuntes");
            ayuda.setText("Vacío.");
            findViewById(R.id.boton_izquierda).setVisibility(View.INVISIBLE);
            findViewById(R.id.boton_derecha).setVisibility(View.INVISIBLE);
        } else {
            mostrarPagina();
        }


        //Se configura el índice, para que muestre los índices.
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this, R.layout.simple_list_item_dark, titulos);
        indice.setAdapter(adaptador);
        ayuda.setMovementMethod(new ScrollingMovementMethod());

        //Si se clica un elemento del índice se va a él.
        indice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                titulo.setText(titulos.get(position));
                ayuda.setText(Html.fromHtml(ayudas.get(0), new ImageGetter(), null));
                pagina_actual = position;
                indice.setVisibility(View.GONE);
                mostrarPagina();

            }

        });

        //Si se pulsa en algún sitio fuera de la lista, se esconde el índice.
        findViewById(R.id.raiz).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                indice.setVisibility(View.GONE);
            }

        });

    }

    /**
     * Muesta u oculta el índice.
     *
     * @param view vista del boton pulsado
     */
    public void mostrarIndice(View view) {
        if (total_paginas > 0)
            indice.setVisibility(indice.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
    }

    public void pasarPagina(View view) {
        int i = 0;
        if (view.getId() == R.id.boton_derecha)
            i = 1;
        else if (view.getId() == R.id.boton_izquierda)
            i = -1;

        pagina_actual += i;

        mostrarPagina();

    }

    /**
     * Muestra la ayuda seleccionada.
     */
    public void mostrarPagina() {

        int i = 0;
        String s = sharedPref.getString("apunte_nuevo_0", "");

        findViewById(R.id.nuevo).setVisibility(View.INVISIBLE);

        //Si el título de la página es uno de los que antes estaban como no leído
        //se borra de las preferencias como no leído, en los dos sitios donde está.
        while (!s.equals("")) {
            if (s.equals(titulos.get(pagina_actual))) {
                SharedPreferences.Editor editor = sharedPref.edit();

                findViewById(R.id.nuevo).setVisibility(View.VISIBLE);
                editor.remove("apunte_nuevo_" + i);
                editor.remove(s);

                //A los siguinete se les baja un índice.
                s = sharedPref.getString("apunte_nuevo_" + (++i), "");
                while (!s.equals("")) {
                    editor.putString("apunte_nuevo_" + (i - 1), s);
                    editor.remove("apunte_nuevo_" + i);
                    s = sharedPref.getString("apunte_nuevo_" + (++i), "");
                }

                editor.apply();

            }
            s = sharedPref.getString("apunte_nuevo_" + ++i, "");
        }


        //Se ajusta la visibilidad de los botones.
        if (pagina_actual >= 0 && pagina_actual < total_paginas) {
            titulo.setText(titulos.get(pagina_actual));
            ayuda.setText(Html.fromHtml(ayudas.get(pagina_actual), new ImageGetter(), null));

            if (pagina_actual == total_paginas - 1) {
                findViewById(R.id.boton_derecha).setVisibility(View.INVISIBLE);
                findViewById(R.id.boton_izquierda).setVisibility(View.VISIBLE);
            } else if (pagina_actual == 0) {
                findViewById(R.id.boton_izquierda).setVisibility(View.INVISIBLE);
                findViewById(R.id.boton_derecha).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.boton_izquierda).setVisibility(View.VISIBLE);
                findViewById(R.id.boton_derecha).setVisibility(View.VISIBLE);
            }
        }

        findViewById(R.id.mensajeAyuda).scrollTo(0, 0);
    }

    /**
     * Al pulsar el boton de "atrás", oculta el índice si estaba visible; si no lo estaba,
     * vuelve a MapaActivity.
     */
    @Override
    public void onBackPressed() {
        if (indice.getVisibility() == View.VISIBLE)
            //Si está mostrándose el índice, se cierra.
            indice.setVisibility(View.GONE);
        else
            //Si no, se vuelve atrás
            super.onBackPressed();

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

}