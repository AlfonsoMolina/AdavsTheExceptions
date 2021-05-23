package com.alfonsomolina.tfg;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Adaptador creado para las diferentes listas. Tiene un comportamiento diferente
 * según la naturaleza de la lista.
 *
 * @author Alfonso Molina
 */
public class ListaModificadaAdapter extends ArrayAdapter{

    private ArrayList<ElementoModificado> lista = new ArrayList<>();  //Los elementos de la lista
    private int resourceID;                                           //El layout en que se va a mostrar

    /**
     * Constructor.
     * @param context contexto de la actividad.
     * @param resourceID recurso con el layout de cada fila.
     */
    public ListaModificadaAdapter(Context context, int resourceID) {
        super(context,resourceID);
        this.resourceID = resourceID;
    }

    /**
     * Devuelve el número de filas en la lista.
     * @return devuelve un entero con el número de elementos en la lista.
     */
    @Override
    public int getCount() {
        return this.lista.size();
    }

    /**
     * Devuelve el elemento de la lista de la posición elegida.
     * @param position entero con la posición del elemento en la lista.
     * @return devuelve el objeto en la fila elegida.
     */
    @Override
    public Object getItem(int position) {
        return this.lista.get(position);
    }

     /**
     * Crea la vista del elemento de la lista.
     *
     * @param position entero con la posición del elemento en la lista.
     * @param convertView vista de la fila.
     * @param parent vista de la lista padre.
     * @return devuelve la fila modificada.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View fila = convertView;

        if (convertView == null) {
            //Se añade una nueva view a la lista.
            LayoutInflater inflater = (LayoutInflater) this.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            fila = inflater.inflate(resourceID, parent, false);
        }

        ElementoModificado l = lista.get(position);

        ((TextView) fila.findViewById(R.id.texto)).setText(l.getTexto());

        if(resourceID == R.layout.lista_linea_de_codigo) {
            //Se coloca el número de línea
            ((TextView) fila.findViewById(R.id.numeroLinea)).setText(""+position);
            //Se resalta la linea si es la actual (flag==1)
            fila.findViewById(R.id.contenedor).setBackgroundColor(l.isFlagged() ? Color.YELLOW : Color.WHITE);

        } else if (resourceID == R.layout.lista_misiones) {
            //Si la misión es nueva se cambia el fondo
            fila.findViewById(R.id.contenedor).setBackgroundResource(l.isFlagged()? R.drawable.boton_mision_nueva : R.drawable.boton_mision);

        }else if (resourceID == R.layout.lista_chat){

            //Se elige el fondo y la posición según si está a la derecha o la izquierda
            fila.findViewById(R.id.texto).setBackgroundResource(l.isFlagged() ? R.drawable.globo_izq : R.drawable.globo_dcha);
            LinearLayout contenedor = (LinearLayout) fila.findViewById(R.id.contenedor);
            contenedor.setGravity(l.isFlagged()? Gravity.START : Gravity.END);
            contenedor.setPadding(l.isFlagged()? 0 : 40, 0, l.isFlagged() ? 40: 0,0);
        }

        return fila;
    }

    /**
     * Añade un nuevo elemento al final de la lista.
     *
     * @param texto String con el texto del nuevo elemento.
     */
    public void add(String texto){
        lista.add(new ElementoModificado(texto));
    }

    /**
     * Añade un nuevo elemento al final de la lista.
     *
     * @param e ElementoModificado con el nuevo elemento a introducir.
     */
    public void add(ElementoModificado e){
        lista.add(e);
    }

    /**
     * Añade un nuevo elemento en una posicion concreta de la lista.
     * @param posicion entero con la posición del nuevo elemento
     * @param s String con el texto del nuevo elemento.
     */
    public void add(int posicion, String s) {
        lista.add(posicion, new ElementoModificado(s));
    }

    /**
     * Elimina una fila de la lista.
     *
     * @param posicion entero con la posición del elemento a eliminar
     */
    public void remove(int posicion){
        lista.remove(posicion);
    }

    /**
     * Cambia la bandera de un elemento de la lista.
     *
     * @param position entero con la posición del elemento en la lista.
     * @param b boolean con el nuevo valor de la bandera.
     */
    public void setFlag (int position, boolean b){
        lista.get(position).setFlag(b);
    }

    /**
     * Cambia el texto de un elemento de la lista
     *
     * @param position entero con la posición del elemento en la lista.
     * @param s String con el nuevo texto.
     */
    public void setText(int position, String s){
        lista.get(position).setTexto(s);
    }


}
