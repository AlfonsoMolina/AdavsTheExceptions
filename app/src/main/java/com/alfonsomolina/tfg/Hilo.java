package com.alfonsomolina.tfg;

import android.graphics.Canvas;
import android.os.AsyncTask;

/**
 * Crea un hilo que actualiza el tablero, recordándole cada pocos milisegundos que debe
 * actualizarse y redibujar su contenido.
 *
 * @author Alfonso Molina
 */
public class Hilo extends AsyncTask<Void, Float, Void> {

    static final long FPS = 10;
    private Tablero tablero;

    /**
     * Constructor. Construye el hilo para el tablero ya creado.
     * @param tablero Tablero que se debe actualizar.
     */
    public Hilo(Tablero tablero){
        this.tablero = tablero;
    }

    /**
     * Un bucle se ejecuta hasta ser cancelado, actualizando el tablero cada
     * poco tiempo.
     *
     * @param sinusar No necesita parámetros.
     * @return No devuelve nada.
     */
    @Override
    protected Void doInBackground(Void... sinusar) {

        long tEspera = 1000 / FPS;  //Cada tEspera se redibujará, para hacer 10 fotogramas por segundo.
        long inicio;
        long fin;

        //Este bucle se ejecutará contiuamente, hasta que el hilo sea cancelado desde el tablero.
        while (true) {

            if(this.isCancelled())
                return null;

            inicio = System.currentTimeMillis();
            Canvas c = tablero.getHolder().lockCanvas();
            tablero.dibujar(c);
            tablero.getHolder().unlockCanvasAndPost(c);

            //Para que se dibuje cada tiempo fijo, y no cada tEspera + lo que tarde en dibujar.
            fin = tEspera - (System.currentTimeMillis() - inicio);
            try {
                if (fin > 0)
                    Thread.sleep(fin);
                else
                    Thread.sleep(10);
            } catch (Exception e) {
            }

        }
    }

}
