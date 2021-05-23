package com.alfonsomolina.tfg;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Almacena toda la información referente a un Sprite y lo dibuja en el tablero.
 */
public class Sprite {

    //Constantes
    //Hacia dónde está mirando o hacia donde se mueve
    public static final int ABAJO = 0;
    public static final int IZQUIERDA = 1;
    public static final int DERECHA = 2;
    public static final int ARRIBA = 3;

    //Acciones
    public static final int QUIETO = 0;
    public static final int ANDAR = 1;
    public static final int ATACAR = 2;
    public static final int TEMP = 3;
    public static final int CHOCAR = 4;


    //Tipo de Sprite. Modela su comportamiento
    public static final int ROCA = 0;           //Se queda quieto. No ataca ni puede pisarse. Puede romperse.
    public static final int DIANA = 2;          //Hay que romperlo para ganar (si la victoria es por limpiar). No ataca ni se mueve.
    public static final int COCODRILO = 3;      //No se mueve. Si lo pisas te ataca.
    public static final int META = 4;           //La meta. Si la pisas ganas el juego (si la victoria es llegando a la meta).
    public static final int ADA = 5;           //El protagonista. Más acciones.
    public static final int BESTIA = 6;        //Se mueve y ataca a Ada. Con IA.
    public static final int FIJO = 7;           //Se queda quieto y no ataca. Irrompible.
    public static final int OBJETO = 8;         //Un objeto. No se puede romper, pero sí coger.
    public static final int NPC = 9;            //Personaje amistoso con IA.
    public static final int EXCEPCION = 10;     //Excepcion. Si las mira, toca o ataca, muere.

    private Tablero tablero;
    private Paint paint;                        //Para hacerlo transparente si hace falta.

    //Numero de filas, columnas y acciones diferentes que tiene el drawable.
    private Bitmap bmp_full;        //Bitmap completo
    private Bitmap bmp;             //Bitmap de la acción que está realizando
    private int bmp_filas;          //Filas del bitmap (una por direccion)
    private int bmp_columnas;       //Fotogramas por animación (una o tres)
    private int bmp_acciones;       //Acciones que hace el Sprite (quieto o quieto, andar y atacar)
    private int ancho;              //Ancho y
    private int alto;               //alto de cada fotograma.
    private int fotogramaActual;    //Por qué fotograma va.
    private int fotogramas;         //Numero de fotogramas
    private int direccion;        //Direccion en la que mira
    private int accion;           //Accion que está realizando
    private int vida;               //Vida de la acción. Cuándo dejará de moverse.
    private Rect src;               //Zona del bmp que se va a dibujar
    private Rect dest;               //Y dónde dibujarlo
    private int x = 0;              //Posicion del canvas en la que se encuentra (x,y)
    private int y = 0;
    private int columna;            //Columna en la que se encuentra
    private int fila;               //Fila en la que se encuentra
    private int velocidad ;         //Velocidad a la que se mueve

    private int tipo;               //Su tipo general (NPC, mob, objeto...)
    private String iD = "";         //Más particular.
    private int salud;            //Salud del personaje
    private int fuerza;           //Fuerza del personaje (daño que hace en cada golpe)

    private int x_tablero_offset = 0;           //Para centrarlo en la SurfaceView
    private int y_tablero_offset = 0;

    private boolean ultimo = false;
    private boolean velocidad_rapida = false;

    private String equipo;                      //objeto que lleva equipado

    /**
     * Constructor.
     * @param tablero tablero en el que se juega.
     * @param bmp Bitmap con los fotogramas del Sprite.
     * @param x entero con la coordenada X inicial.
     * @param y entero con la coordenada Y inicial.
     * @param dir entero con la dirección inicial en la que se orienta.
     * @param tipo entero con el tipo de Sprite.
     */
    public Sprite(Tablero tablero, Bitmap bmp, int x, int y, int dir, int tipo) {

        if (tipo == ADA || tipo == BESTIA || tipo == NPC) {
            bmp_acciones = 3;   //Quieto, andar y atacar.
            bmp_filas = 4;
            bmp_columnas = 3;

        } else if (tipo == COCODRILO ||tipo == EXCEPCION) {
            bmp_acciones = 1;   //Quieto.
            bmp_filas = 1;
            bmp_columnas = 3;

        } else if (tipo == META || tipo == OBJETO || tipo == FIJO || tipo == ROCA || tipo == DIANA) {
            bmp_acciones = 1;   //Quieto.
            bmp_columnas = 1;
            bmp_filas = 1;      //Sin animación
        }

        this.tipo = tipo;
        this.tablero = tablero;
        this.bmp_full = bmp;
        this.ancho = bmp.getWidth() / (bmp_columnas * bmp_acciones);
        this.alto = bmp.getHeight() / bmp_filas;
        this.bmp = Bitmap.createBitmap(bmp_full, 0, 0, ancho * bmp_columnas, alto * bmp_filas);
        this.salud = 1000;      //~ Irrompible
        this.fuerza = 0;
        this.columna = x;
        this.fila = y;
        this.vida = 0;
        this.direccion = dir;
        this.accion = QUIETO;
        this.velocidad = 10;
        this.paint = new Paint();
        this.ultimo = false;
        this.fotogramaActual = 0;
        this.fotogramas = bmp_columnas;

        x_tablero_offset = 0;
        y_tablero_offset = 0;


        //Donde está en el tablero. Centrado en la baldosa y teniendo en cuenta
        //también que las primeras filas empiezan algo más a la derecha que las siguientes.
        this.x = x * tablero.getAnchuraBaldosa() + (tablero.getFilas() - 1 - y) * tablero.getBaldosa_offset() + x_tablero_offset;
        this.y = (y + 1) * tablero.getAlturaBaldosa() - alto + y_tablero_offset;          //Fila + 1 para tener la base


        this.src = new Rect(0, 0, ancho, alto);
        this.dest = new Rect(this.x, this.y, this.x + ancho, this.y + alto);


    }

    /**
     * Dibuja el Sprite en un canvas.
      * @param canvas canvas en el que dibujar.
     */
    public void dibujar(Canvas canvas) {
        if (salud > 0) {
            canvas.drawBitmap(bmp, src, dest, paint);

        }
    }

    /**
     * Cambia el fotograma en el que está el Sprite. Actualiza su movimiento si es necesario.
     */
    public void actualizar() {
        int srcX = fotogramaActual * ancho;
        int srcY = direccion * alto;
        src.set(srcX, srcY, srcX + ancho, srcY + alto);

        //Si está andando, muevo el lugar donde se encuentra en el canvas.
        //Cambio la posición en que se encuentra cuando esté más o menos a mitad de camino.
        if (accion == ANDAR || accion == CHOCAR) {
            switch (accion == ANDAR ? direccion : ARRIBA - direccion) {
                case ARRIBA:
                    dest.offset(velocidad / 2, -velocidad);
                    y -= velocidad;
                    x += velocidad / 2;
                    if(!velocidad_rapida && accion == ANDAR && vida == tablero.getAlturaBaldosa() / (2* velocidad))
                        fila--;
                    break;
                case ABAJO:
                    dest.offset(-velocidad / 2, velocidad);
                    y += velocidad;
                    x -= velocidad / 2;
                    if(!velocidad_rapida && accion == ANDAR && vida == tablero.getAlturaBaldosa() / (2* velocidad))
                        fila++;
                    break;
                case DERECHA:
                    dest.offset(velocidad, 0);
                    x += velocidad;
                    if(!velocidad_rapida && accion == ANDAR && vida == tablero.getAnchuraBaldosa() / (2* velocidad))
                        columna++;
                    break;
                case IZQUIERDA:
                    dest.offset(-velocidad, 0);
                    x -= velocidad;
                    if(!velocidad_rapida && accion == ANDAR && vida == tablero.getAnchuraBaldosa() / (2* velocidad))
                        columna--;
                    break;
            }
            if (tipo == ADA)
                tablero.actualizarOffset();
        }

        if (y_tablero_offset != tablero.getYoffset()) {
            dest.offset(0, tablero.getYoffset() - y_tablero_offset);
            y_tablero_offset = tablero.getYoffset();
        }

        if (x_tablero_offset != tablero.getXoffset()) {
            dest.offset(tablero.getXoffset() - x_tablero_offset, 0);
            x_tablero_offset = tablero.getXoffset();
        }

        //Resto uno a la vida del Sprite. Si es 0, lo pongo quieto. Si es negativo, pues sigue quieto.
        if (accion != QUIETO && --vida < 0) {
            if(ultimo)
                setSalud(0);
            else if (accion == TEMP) {
                fotogramas = bmp_columnas;
                this.ancho = bmp.getWidth() / (bmp_columnas * bmp_acciones);
                this.alto = bmp.getHeight() / bmp_filas;
                this.bmp = Bitmap.createBitmap(bmp_full, 0, 0, ancho * bmp_columnas, alto * bmp_filas);
                setAccion(QUIETO);
                this.centrarDibujo();
            }
            else{
                setAccion(QUIETO, direccion);
                centrarDibujo();
            }
        }

        fotogramaActual = ++fotogramaActual % fotogramas;

    }

    /**
     * Cambia la acción que está realizando el sprite.
     * @param acc entero que identifica la nueva acción.
     */
    public void setAccion(int acc) {
        this.setAccion(acc, direccion);
    }

    /**
     * Cambia la acción que está realizando el Sprite y la dirección en la que la realizará.
     * @param acc entero que identifica la nueva acción.
     * @param dir entero con la dirección en la que se orienta.
     */
    public void setAccion(int acc, int dir) {
        //Si es una acción nueva, cargo el nuevo bitmap.
        if (accion != acc) {
            accion = acc;
            int a = accion;
            if (accion == CHOCAR)
                a = ANDAR;
            bmp = Bitmap.createBitmap(bmp_full, ancho * bmp_columnas * a, 0, ancho * bmp_columnas, alto * bmp_filas);
        }

        fotogramaActual = 0;
        direccion = dir;

        //Se configuran las opciones determinadas de cada acción, como la vida.
        switch (acc) {
            case ATACAR:
                vida = 3;
                break;
            //Va para atrás lo que lleva andando.
            case CHOCAR:
                switch (dir) {
                    case DERECHA:
                        columna--;
                        vida = tablero.getAnchuraBaldosa() / velocidad - vida;
                        break;
                    case IZQUIERDA:
                        columna++;
                        vida = tablero.getAnchuraBaldosa() / velocidad - vida;
                        break;
                    case ARRIBA:
                        fila++;
                        vida = tablero.getAlturaBaldosa() / velocidad - vida;
                        break;
                    case ABAJO:
                        fila--;
                        vida = tablero.getAlturaBaldosa() / velocidad - vida;
                        break;
                }
                break;
            case ANDAR:
                //Se fija la vida del movimiento (si es posible).
                //No se cambia la posición automáticamente, sino cuando llegue.
                switch (dir) {
                    case DERECHA:
                        if (columna + 1 < tablero.getColumnas()) {
                            if(velocidad_rapida)
                              columna++;
                            vida = tablero.getAnchuraBaldosa() / velocidad;

                        } else {
                            vida = 0;
                            accion = QUIETO;
                        }
                        break;
                    case IZQUIERDA:
                        if (columna - 1 >= 0) {
                            vida = tablero.getAnchuraBaldosa() / velocidad;
                            if(velocidad_rapida)
                                columna--;
                        } else {
                            vida = 0;
                            accion = QUIETO;
                        }
                        break;
                    case ARRIBA:
                        if (fila - 1 >= 0) {
                            vida = tablero.getAlturaBaldosa() / velocidad;
                            if(velocidad_rapida)
                                fila--;
                        } else {
                            vida = 0;
                            accion = QUIETO;
                        }
                        break;
                    case ABAJO:
                        if (fila + 1 < tablero.getFilas()) {
                            vida = tablero.getAlturaBaldosa() / velocidad;
                            if(velocidad_rapida)
                                fila++;
                        } else {
                            vida = 0;
                            accion = QUIETO;
                        }
                        break;
                }
                break;
        }

        if(velocidad_rapida)
            vida = 0;
    }

    /**
     * Cambia la animación del Sprite. Utiliza otra imagen para dibujar el Sprite
     * durante algunos fotogramas. Después de esto vuelve al original o se elimina.
     *
     * @param bmp Bitmap con la nueva animación.
     * @param filas entero con el número de filas de la imagen.
     * @param columnas entero con el número de columnas de la imagen.
     * @param life entero con el número de fotogramas que se usará esta imagen.
     * @param ultimo boolean que vale "true" si tras terminar esta animación hay que eliminar el Sprite.
     */
    public void addTempSprite(Bitmap bmp, int filas, int columnas, int life, boolean ultimo) {
        this.ancho = bmp.getWidth() / filas;
        this.alto = bmp.getHeight() / columnas;
        if (columnas == 1)
            this.direccion = 0;
        this.fotogramas = filas;
        this.bmp = bmp;
        this.centrarDibujo();
        this.vida = life;
        accion = TEMP;
        this.ultimo = ultimo;
    }

    /**
     * Centra el dibujo en el centro de la baldosa.
     */
    public void centrarDibujo() {
        this.x = columna * tablero.getAnchuraBaldosa()
                + (tablero.getFilas() - 1 - fila) * tablero.getBaldosa_offset();
        this.y = (fila + 1) * tablero.getAlturaBaldosa() - alto;

        dest = new Rect(x + x_tablero_offset, y + y_tablero_offset,
                x + ancho + x_tablero_offset, y + alto + y_tablero_offset);
    }

    /**
     * Comprueba si un Sprite choca con otro en el tablero.
     * @param s Sprite con el que se comprueba si choca.
     * @return devuelve "true" si chocan y "false" en caso contrario.
     */
    public boolean choca(Sprite s) {
        return salud > 0 && this.columna == s.getColumna() && this.fila == s.getFila() && Rect.intersects(dest, s.dest);
    }

    /**
     * Cambia la dirección en la que se orienta el Sprite.
     *
     * @param dir entero con la nueva dirección.
     */
    public void orientar(int dir) {
        direccion = dir;
    }

    /**
     * Quita algo de vida al Sprite. Si no queda salud, devuelve true.
     *
     * @param puntos entero con los puntos de vida a quitar.
     * @return devuelve "true" si, tras quitar vida, la salud es igual o inferior a cero.
     */
    public boolean quitarVida(int puntos) {
        salud -= puntos;
        return salud <= 0;
    }

    /**
     * Fija la transparencia del Sprite.
     * @param alpha entero de 0 a 255 con la transparencia.
     */
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    /**
     * Fija la fuerza del Sprite.
     * @param fuerza entero con el valor de fuerza.
     */
    public void setFuerza(int fuerza) {
        this.fuerza = fuerza;
    }

    /**
     * Fija la salud del Sprite.
     * @param salud entero con la salud.
     */
    public void setSalud(int salud) {
        if(tipo == ADA && salud > this.salud)
            tablero.setLog("Ada descansa y recupera salud.\n");
        this.salud = salud;
    }

    /**
     * Fija el identificador propio del Sprite.
     * @param iD String con el id.
     */
    public void setID(String iD) {
        this.iD = iD;
    }

    /**
     * Devuelve el identificador propio del Sprite.
     * @return devuelve un String con el identificador del Sprite.
     */
    public String getID() {
        return iD;
    }

    /**
     * Devuelve los fotogramas que le quedan a la animacion del movimiento.
     * @return devuelve un entero con los fotogramas que le quedan a la animacion del movimiento.
     */
    public int getVida() {
        return vida;
    }

    /**
     * Devuelve la columna en la que se encuentra el Sprite.
     * @return devuelve un entero con la columna en que se encuentra el Sprite.
     */
    public int getColumna() {
        return columna;
    }

    /**
     * Devuelve la fila en la que se encuentra el Sprite.
     * @return devuelve un entero con la fila en que se encuentra el Sprite.
     */
    public int getFila() {
        return fila;
    }

    /**
     * Devuelve la posición del canvas del eje X en que se encuentra el Sprite.
     * @return devuelve un entero con la posición del canvas del eje X en que se encuentra el Sprite.
     */
    public int getX() {
        return x;
    }

    /**
     * Devuelve la posición del canvas del eje X en que se encuentra el Sprite.
     * @return devuelve un entero con la posición del canvas del eje X en que se encuentra el Sprite.
     */
    public int getY() {
        return y;
    }

    /**
     * Devuelve el tipo de Sprite.
     * @return devuelve un entero que identifica el tipo de Sprite.
     */
    public int getTipo() {
        return tipo;
    }

    /**
     * Devuelve la salud del Sprite.
     * @return devuelve un entero con la salud del Sprite.
     */
    public int getSalud() {
        return salud;
    }

    /**
     * Fija el equipo del Sprite.
     * @param equipo String que identifica el objeto a equipar.
     */
    public void setEquipo(String equipo) {
        this.equipo = equipo;
    }

    /**
     * Devuelve el objeto equipado.
     *
     * @return devuelve un String que identifica el objeto equipado.
     */
    public String getEquipo() {
        return equipo;
    }

    /**
     * Devuelve la fuerza del Sprite.
     *
     * @return devuelve un entero con la fuerza del Sprite.
     */
    public int getFuerza() {
        return fuerza;
    }

    /**
     * Devuelve la dirección que orienta el Sprite.
     * @return devuelve un entero que identifica la dirección que orienta el Sprite.
     */
    public int getDireccion() {
        return direccion;
    }

    /**
     * Devuelve la acción que esta realizando el Sprite.
     * @return devuelve un entero que identifica la acción que esta realizando el Sprite.
     */
    public int getAccion() {
        return accion;
    }

    /**
     * Acelera la animacioó, haciendo que los sprites no tarden en realizar acciones. Este metodo
     * se utiliza para probar las combinaciones antes de ejecutar cada nivel.
     */
    public void setVelocidadRapida() {
        velocidad_rapida = true;
    }

    /**
     * Fija las medidas del bitmap. Se utiliza para aquellos que no siguen la estructura tipica
     * de su tipo de Sprite.
     *
     * @param bmp_columnas entero con las columnas de la imagen (fotogramas por accion).
     * @param bmp_filas entero con las filas de la imagen (una o cuatro, una por cada direccion).
     * @param bmp_acciones entero con las acciones de la imagen.
     */
    public void setBmp_medidas(int bmp_columnas, int bmp_filas, int bmp_acciones) {
        this.bmp_columnas = bmp_columnas == 0? this.bmp_columnas : bmp_columnas;
        this.bmp_filas = bmp_filas == 0 ? this.bmp_filas : bmp_filas;
        this.bmp_acciones = bmp_acciones == 0 ? this.bmp_acciones : bmp_acciones;
        this.ancho = bmp.getWidth() / (this.bmp_columnas * this.bmp_acciones);
        this.alto = bmp.getHeight() / this.bmp_filas;
        this.bmp = Bitmap.createBitmap(bmp_full, 0, 0, ancho * this.bmp_columnas, alto * this.bmp_filas);
        this.fotogramas = this.bmp_columnas;
        centrarDibujo();
    }


}