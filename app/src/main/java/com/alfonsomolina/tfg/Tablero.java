package com.alfonsomolina.tfg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;

/**
 * Dibuja el tablero y los personajes y controla las interacciones entre ellos.
 *
 * @author Alfonso Molina
 */
public class Tablero extends SurfaceView {

    private Tablero tablero = this;
    private Hilo hilo;
    private ParserJSON parserJSON;

    private ArrayList<Sprite> items;              //Lista con los elementos interactuables (criaturas, meta) que hay.
    private Sprite ada;                           //El Sprite de Ada (la protagonista).
    private Bitmap baldosaBase;
    private ArrayList<Bitmap> baldosas;           //Desde 0 hasta el máximo, se van dibjando las baldosas.
    private int filas;
    private int columnas;
    private int alturaBaldosa;
    private int anchuraBaldosa;
    private boolean confMedidas;                    //boolean para obtener las medidas solo una vez.
    private int alturaCanvas;
    private int anchuraCanvas;
    private int baldosa_offset;                     //Cuanto se mueve cada una en cada fila, al ir en diagonal.
    private int xoffset;                            //xoffset e
    private int yoffset;                            //yoffset, para centrar el tablero en el espacio.
    private boolean meta;                         //True si ada está encima de la meta o ha cumplido un objetivo.
    private boolean bestias;                      //True si aun hay enemigos en el tablero.
    private boolean victoria;                     //True si se ha cumplido la condición especial para ganar.
    private String log;                             //El texto de lo que ocurre.
    private int turno;                              //El número de turnos que se han jugado.

    private SurfaceHolder holder;

    public Tablero(Context context){
        this(context,new ParserJSON(context,"tutorial",0),false);
    }


    /**
     * Constructor. Crea el tablero a partir de los datos en el JSON.
     *
     * @param context contexto de la actividad
     * @param parserJSON parser para leer los datos del archivo JSON.
     * @param muestra boolean que vale "true" si es un tablero de muestra y los sprites aleatorios deben ser transparentes.
     */
    public Tablero(Context context, ParserJSON parserJSON, boolean muestra) {
        super(context);
        baldosas = new ArrayList<>();
        items = new ArrayList<>();
        log = "¡Empezamos!\n";
        this.parserJSON = parserJSON;
        confMedidas = true;
        meta = false;
        victoria = false;
        //Estos dos offset se utilizan para centrar el tablero en la superficie
        yoffset = 0;
        xoffset = 0;
        turno = 0;

        //Se dibuja el tablero.
        filas = parserJSON.getInfoTablero("filas");
        columnas = parserJSON.getInfoTablero("columnas");

        //Se añaden las baldosas diferentes. Estas son estéticas para el suelo, no son interactuables.
        baldosaBase = BitmapFactory.decodeResource(getResources(), parserJSON.getBaldosaBase());
        alturaBaldosa = baldosaBase.getHeight();
        anchuraBaldosa = 2 * baldosaBase.getWidth() / 3 ;                   //Solo la base, sin la parte inclinada.
        baldosa_offset = baldosaBase.getWidth() - anchuraBaldosa;      //La parte inclinada.

        int i = 0;
        int b;
        //Se cargan en el array el suelo con todas las baldosas básicas
        while (i < filas * columnas) {
            baldosas.add(null);
            i++;
        }

        i = 0;
        int pos;
        while ((b = parserJSON.getBaldosaBmp(i)) != -1) {
            pos = parserJSON.getBaldosaPosicion(i);
            baldosas.set(pos, BitmapFactory.decodeResource(getResources(), b));

            //Si es agua, añado un item invisible para que no se pueda andar por el agua.
            if (b == R.drawable.baldosa_agua) {
                items.add(new Sprite(tablero, BitmapFactory.decodeResource(getResources(), R.drawable.sprite_invisible), pos % columnas, pos / columnas, 0, Sprite.COCODRILO));
                items.get(items.size() - 1).setBmp_medidas(1, 1, 1);
                items.get(items.size() - 1).setID("agua");
            }

            i++;
        }

        //Se añaden los items que haya (la meta también está incluída aquí).
        i = 0;
        int posicion = parserJSON.getItemPosicion(i);
        while (posicion != -1) {
            Sprite s = new Sprite(this, BitmapFactory.decodeResource(getResources(), parserJSON.getItemBmp(i)), posicion % columnas, posicion / columnas, parserJSON.getItemDireccion(i), parserJSON.getItemTipo(i));

            //Si está en ResumenMisionActivity y el elemento es aleatorio se pone invisible
            if (muestra && parserJSON.esAleatorio(i))
                s.setAlpha(125);

            //Según el tipo de item hay que añadirle datos
            switch (parserJSON.getItemTipo(i)) {
                case (Sprite.ADA):
                    ada = s;
                case (Sprite.BESTIA):
                    s.setFuerza(parserJSON.getItemFuerza(i));
                case (Sprite.ROCA):
                case (Sprite.DIANA):
                    s.setSalud(parserJSON.getItemSalud(i));
                case (Sprite.NPC):
                case (Sprite.OBJETO):
                default:
                    s.setID(parserJSON.getID(i));
                    break;
            }

            if(s.getTipo() == Sprite.COCODRILO && s.getID().equals("sprite_meta"))
                s.setBmp_medidas(1,1,1);


            //Si es Ada, no se añade a la lista de items.
            //Si es una excepción y no es el mapa definitivo, tampoco.
            if(s.getTipo() == Sprite.ADA)
                ada = s;
            else if((s.getTipo() != Sprite.EXCEPCION || !muestra) && posicion >= 0 )
                items.add(s);

            posicion = parserJSON.getItemPosicion(++i);


        }

        //Si está en ResumenMisionActivity, se ponen los elementos aleatorios restantes.
        if (muestra) {
            int[] aleatorio = parserJSON.getAleatorio();
            for (i = 0; i < aleatorio.length; i++)
                if (aleatorio[i] != -1) {
                    items.add(new Sprite(this, BitmapFactory.decodeResource(getResources(), R.drawable.aleatorio), aleatorio[i] % columnas, aleatorio[i] / columnas, 0, Sprite.FIJO));
                    items.get(items.size() - 1).setAlpha(125);
                }
        }

        //Se calcula el offset para centrar el mapa.
        actualizarOffset();


        //Se crea el hilo dentro de este callback. Cada vez que se cree la SurfaceView (cuando
        //se inicie la actividad por primera y vez y cada vez que se vuelva) se creará un hilo.
        //Cuando se destruya (al salir de la actividad, de la forma que sea) cancelará el hilo.
        holder = getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                hilo.cancel(true);
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                hilo = new Hilo(tablero);
                hilo.execute();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {

            }
        });
    }


    //METODOS PARA DIBUJAR EL TABLERO

    /**
     * Dibuja el tablero en el canvas. Primero actualiza todos los Sprites y después los dibuja.
     * @param canvas Canvas en el que dibujar.
     */
    protected void dibujar(Canvas canvas) {

        if (confMedidas) {
            alturaCanvas = canvas.getHeight();
            anchuraCanvas = canvas.getWidth();
            actualizarOffset();
        }


        actualizar();

        canvas.drawColor(Color.parseColor("#EEEEEE"));  //Se pinta de blanco, para borrar lo anterior

        dibujarTablero(canvas);                         //Se dibuja el tablero
        int i = 0;
        while (i < items.size()) {
            items.get(i).dibujar(canvas);                 //Se dibujan los items
            if(items.get(i).getSalud() < 0)
                items.remove(i--);
            i++;
        }
        i = 0;

        ada.dibujar(canvas);                             //Y se dibuja a Ada.


    }

    /**
     * Actualiza la posición de  todos los sprites y comprueba si interactúan entre ellos.
     */
    public void actualizar() {

        int i = 0;
        bestias = false;        //No hay bestias
        Sprite item;

        //Antes de nada se pone que Ada no está pisando la meta, por si la pisa y después sale.
        //meta se utiliza en dos tipos de victorias: pisando la meta y cumpliendo algo especial.
        //Cuando se cumple eso especial victoria es true y meta no se reinicia.
        if (!victoria)
            meta = false;

        int ada_x = -1;
        int ada_y = -1;
        boolean objetivo = false;

        if (ada.getAccion() == Sprite.ATACAR && ada.getVida() == 0) {
            ada_x = ada.getColumna();
            ada_y = ada.getFila();

            if (ada.getDireccion() == Sprite.ARRIBA)
                ada_y--;
            else if (ada.getDireccion() == Sprite.ABAJO)
                ada_y++;
            else if (ada.getDireccion() == Sprite.DERECHA)
                ada_x++;
            else
                ada_x--;
        }


        while (i < items.size()) {
            item = items.get(i);

            if (item.getTipo() == Sprite.COCODRILO || item.getTipo() == Sprite.BESTIA || item.getTipo() == Sprite.DIANA)
                bestias = true; //Hemos encontrado una bestia --> sí hay

            if (ada.choca(item)) {
                if (item.getTipo() == Sprite.META) {
                    meta = true;

                    //Si es un cocodrilo, delibitamos a Ada y añadimos una animación.
                } else if (item.getTipo() == Sprite.COCODRILO) {
                    ada.setSalud(0);
                    if (!item.getID().equals("meta"))
                        item.addTempSprite(BitmapFactory.decodeResource(getResources(), R.drawable.sprite_agua), 3, 1, 6, true);
                } else if (item.getTipo() == Sprite.EXCEPCION) {
                    ada.setSalud(0);
                    log = log.concat("Ada ha chocado contra ???.\n");
                } else {
                    ada.setAccion(Sprite.CHOCAR);
                }


                //Si el item está atacando y ada está a su lado, le hace daño.
            }

            if (ada.getAccion() == Sprite.ATACAR && !objetivo) {
                if (item.getColumna() == ada_x && item.getFila() == ada_y) {
                    objetivo = true;

                    if (item.getTipo() == Sprite.BESTIA)
                        log = log.concat("Ada ataca a " + item.getID() + " (" + ada.getFuerza() + ").\n");
                    else if (item.getTipo() == Sprite.ROCA || item.getTipo() == Sprite.DIANA)
                        log = log.concat("Ada golpea a " + item.getID() + " (" + ada.getFuerza() + ").\n");
                    else if (item.getTipo() == Sprite.EXCEPCION) {
                        log = log.concat("Ada golpea a ???.\n");
                        ada.setSalud(0);
                    } else {
                        log = log.concat("¡No puedes golpear eso!\nAda se hace daño a sí misma (" + ada.getFuerza() + ")\n");
                    }
                    if (item.quitarVida(ada.getFuerza()))
                        items.remove(i--);
                }
            }

            if (item.getSalud()> 0 && item.getAccion() == Sprite.ATACAR) {
                int x = item.getColumna();
                int y = item.getFila();
                if (item.getDireccion() == Sprite.ARRIBA)
                    y--;
                else if (item.getDireccion() == Sprite.ABAJO)
                    y++;
                else if (item.getDireccion() == Sprite.DERECHA)
                    x++;
                else
                    x--;

                //Sólo se hace daño en el primer fotograma (vida == 3)
                if (ada.getColumna() == x && ada.getFila() == y && item.getVida() == 0) {
                    if (ada.quitarVida(item.getFuerza()))
                        log = log.concat("Te has debilitado...\n");
                }
            } else if (item.getSalud()> 0 && item.getAccion() == Sprite.ANDAR){
                int k = 0;
                Sprite item2;
                while (k < items.size()) {
                    item2 = items.get(k);
                    if (item2 != item && item2.choca(item)) {
                        if (item2.getTipo() == Sprite.EXCEPCION) {
                            item.setSalud(0);
                            items.remove(items.indexOf(item));
                            k = items.size();
                            //Si es un cocodrilo, delibitamos a Ada y añadimos una animación.
                        } else if (item2.getTipo() == Sprite.COCODRILO) {
                            item.setSalud(0);
                            items.remove(items.indexOf(item));
                            k = items.size();
                            if (!item2.getID().equals("meta"))
                                item2.addTempSprite(BitmapFactory.decodeResource(getResources(), R.drawable.sprite_agua), 3, 1, 6, true);

                        } else {
                            item.setAccion(Sprite.CHOCAR);
                        }
                    }
                    k++;
                }

            }
            i++;
        }


        //Si no había nadie Ada se daña a sí misma. Así se evita que se escriba sin pensar.
        if (ada.getAccion() == Sprite.ATACAR && ada.getVida() == 0 && !objetivo) {
            log = log.concat("No hay nada que golpear. Ada se hace daño a sí misma (" + ada.getFuerza() + ").\n");
            ada.quitarVida(ada.getFuerza());
        }

        //Se actualizan todos los sprites. Se hace desde aquí para que después se dibuje sin
        //micropausas para actualizar.
        //Y se hace ahora para que todos tenga el comportamiento ya realizado.
        ada.actualizar();
        i = 0;
        while (i < items.size()) {
            items.get(i++).actualizar();
        }


    }

    /**
     * Actualiza el comportamiento de los personajes no controlados por el jugador.
     */
    public void actualizarIA() {
        int i = 0;
        Sprite item;
        turno++;

        while (i < items.size()) {
            item = items.get(i);
            if(item.getTipo() == Sprite.BESTIA){
                int x = item.getColumna();
                int y = item.getFila();

                boolean objetivo = false;
                //Si tiene a Ada al lado, le pega. Y si no, se acerca (primero filas luego columnas).
                if(x == ada.getColumna()){
                    if(y + 1 == ada.getFila()) {
                        item.setAccion(Sprite.ATACAR, Sprite.ABAJO);
                        objetivo = true;
                    }
                    else if (y - 1 == ada.getFila()) {
                        item.setAccion(Sprite.ATACAR, Sprite.ARRIBA);
                        objetivo = true;
                    }
                    else
                        item.setAccion(Sprite.ANDAR, y > ada.getFila() ? Sprite.ARRIBA : Sprite.ABAJO);

                } else if (y == ada.getFila()){
                    if(x + 1 == ada.getColumna()) {
                        item.setAccion(Sprite.ATACAR, Sprite.DERECHA);
                        objetivo = true;
                    }else if (x - 1 == ada.getColumna()) {
                        item.setAccion(Sprite.ATACAR, Sprite.IZQUIERDA);
                        objetivo = true;
                    } else {
                        item.setAccion(Sprite.ANDAR, x > ada.getColumna() ? Sprite.IZQUIERDA : Sprite.DERECHA);
                    }
                } else {
                    if(Math.abs(x - ada.getColumna()) > Math.abs(y - ada.getFila()))
                        item.setAccion(Sprite.ANDAR, x > ada.getColumna() ? Sprite.IZQUIERDA : Sprite.DERECHA);
                    else
                        item.setAccion(Sprite.ANDAR, y > ada.getFila() ? Sprite.ARRIBA : Sprite.ABAJO);

                }
                if (objetivo)
                    log = log.concat("¡Han atacado a Ada! (" + item.getFuerza() + ").\n");
                else if(parserJSON.getExtra().equals("boole")){
                    if(turno == 1)
                        item.setAccion(Sprite.ANDAR, Sprite.IZQUIERDA);
                    else if(turno < 4)
                        item.setAccion(Sprite.ANDAR, Sprite.ABAJO);
                    else
                        item.setAccion(Sprite.ANDAR, Sprite.IZQUIERDA);
                }


            //Comportamiento de los NPC (Non-Player Character).
            }else if (item.getTipo() == Sprite.NPC){
                switch(item.getID()){

                    //Comportamiento del herrero: si enfrente tiene un yunque, le pega. Sino, no hace nada.
                    case("herrero"):
                        int x = item.getColumna();
                        int y = item.getFila();

                        switch (item.getDireccion()) {
                            case (Sprite.DERECHA):
                                x++;
                                break;
                            case (Sprite.IZQUIERDA):
                                x--;
                                break;
                            case (Sprite.ABAJO):
                                y++;
                                break;
                            case (Sprite.ARRIBA):
                                y--;
                                break;
                        }

                        int j = 0;
                        boolean flag = true;
                        while (j < items.size() && flag){
                            if(items.get(j).getColumna() == x && items.get(j).getFila() == y) {
                                flag = false;
                                if(items.get(j).getID().equals("yunque"))
                                    item.setAccion(Sprite.ATACAR);
                            }
                            j++;
                        }
                        break;
                    case ("perro"):
                        if(parserJSON.getExtra().equals("posada")){
                            if(mirar(item.getColumna()+1,item.getFila(),false) == Codigo.CTE_VACIO){
                                meta = true;
                                victoria = true;
                            }
                        }
                    case("boole"):
                        if(parserJSON.getExtra().equals("boole")){
                            if(turno < 3)
                                item.setAccion(Sprite.ANDAR, Sprite.ABAJO);
                            else
                                item.setAccion(Sprite.ANDAR, Sprite.IZQUIERDA);
                        }
                }

            }
            i++;

        }
    }


    /**
     * Dibuja las baldosas del tablero.
     * @param canvas canvas donde dibujar.
     */
    protected void dibujarTablero(Canvas canvas){
        int fila = 0;
        int columna;
        int i = 0;

        while (fila < filas) {

            columna = 0;
            while (columna < columnas) {
                if (baldosas.get(i) == null)
                    canvas.drawBitmap(baldosaBase, xoffset + columna* anchuraBaldosa + (filas -1 -fila)* baldosa_offset, yoffset + fila* alturaBaldosa,null);
                else
                    canvas.drawBitmap(baldosas.get(i), xoffset + columna* anchuraBaldosa +(filas-1-fila)* baldosa_offset, yoffset + fila* alturaBaldosa, null);

                i++;
                columna++;
            }

            //Se coloca el borde de la fila
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baldosa_borde_lateral),xoffset + columnas* anchuraBaldosa +(filas-1-fila)* baldosa_offset,yoffset + fila* alturaBaldosa,null);



            fila++;
        }

        //Se coloca el borde inferior de las columnas
        columna = 0;
        while (columna < columnas)
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baldosa_borde_inferior),xoffset + columna++ * anchuraBaldosa, yoffset + filas* alturaBaldosa,null);

    }

    /**
     * Calcula cuanto se mueve el tablero para centrarlo en la pantalla.
     */
    public void actualizarOffset(){

        if(alturaCanvas > filas*alturaBaldosa)                      //Cabe
            yoffset = (alturaCanvas - filas * alturaBaldosa) / 2;
        else if ((alturaBaldosa+ada.getY()) < alturaCanvas /2)      //Se ve el suelo
            yoffset = alturaBaldosa;
        else if ((filas*alturaBaldosa-ada.getY()) < alturaCanvas/2) //Se ve el techo
            yoffset = alturaCanvas - filas * alturaBaldosa;
        else                                                        //Está en el centro
            yoffset = alturaCanvas/2 - ada.getY();

        if(anchuraCanvas > columnas*anchuraBaldosa + filas*baldosa_offset)                      //Cabe
            xoffset = (anchuraCanvas - columnas * anchuraBaldosa -filas*baldosa_offset) / 2;
        else if (ada.getX() < anchuraCanvas /2)                                                 //Se ve la parte izquierda
            xoffset = 0;
        else if ((columnas*(anchuraBaldosa)+filas*baldosa_offset-ada.getX()) < anchuraCanvas/2) //Se ve la parte derecha
            xoffset = anchuraCanvas- columnas*anchuraBaldosa - filas*baldosa_offset;
        else                                                                                    //Está en el centro
            xoffset = anchuraCanvas/2 - ada.getX();


    }

    //METODOS PARA ACTUAR EN EL JUEGO

    /**
     * Devuelve lo que hay en una posicion determinada en el tablero.
     * @param x entero con la coordenada x.
     * @param y entero con la coordenada y.
     * @param log boolean que vale "true" si hay que mostrar en el log lo que se ha visto.
     * @return devuelve un entero con un número que representa lo que había en la baldosa seleccionada.
     */
    public int mirar (int x, int y, boolean log){
        int i = 0;
        int m = 0;
        String s = "";
        boolean flag = true;
        if(x < columnas && y < filas ) {
            while (i < items.size() && flag) {
                if (items.get(i).getColumna() == x && items.get(i).getFila() == y) {
                    switch (items.get(i).getTipo()) {
                        case (Sprite.META):
                            m = Codigo.CTE_META;
                            s = "la meta.\n";
                            break;
                        case (Sprite.FIJO):
                            m = Codigo.CTE_FIJO;
                            s = "un objeto irrompible.\n";
                            break;
                        case (Sprite.ROCA):
                        case (Sprite.DIANA):
                            m = Codigo.CTE_ROCA;
                            s = "algo que puede romper.\n";
                            break;
                        case (Sprite.BESTIA):
                        case (Sprite.COCODRILO):
                            m = Codigo.CTE_BESTIA;
                            s = "un peligroso enemigo.\n";
                            break;
                        case (Sprite.EXCEPCION):
                            s = "???";
                            m = Codigo.CTE_EXCEPCION;
                            break;
                        case (Sprite.NPC):
                            m = Codigo.CTE_PERSONA;
                            s = "a un amigo.";
                            break;
                    }
                    flag = false;

                }
                i++;
            }
            if (flag){
                m = Codigo.CTE_VACIO;
                s = "una baldosa vacía.\n";
            }
        } else
            s = "la nada";


        if(log)
            this.log = this.log.concat("Ada ve " + s);

        return m;
    }

    /**
     * Elimina el Sprite que haya en la coordenada seleccionada.
     *
     * @param x entero con la coordenada x.
     * @param y entero con la coordenada y.
     */
    public void eliminar(int x, int y) {
        int i = 0;
        boolean flag = true;
        while (i < items.size() && flag) {
            if (items.get(i).getColumna() == x && items.get(i).getFila() == y) {
                items.remove(i);
                flag = false;
            }
            i++;
        }
    }

    /**
     * Coge el objeto en la dirección que esté mirando Ada y lo equipa.
     */
    public void coger(){
        coger(ada.getDireccion());
    }

    /**
     * Coge el objeto en la dirección seleccionada y lo equipa.
     * @param direccion entero con la dirección seleccionada
     */
    public void coger(int direccion){
        int x = ada.getColumna();
        int y = ada.getFila();

        switch (direccion) {
            case (Sprite.DERECHA):
                x++;
                break;
            case (Sprite.IZQUIERDA):
                x--;
                break;
            case (Sprite.ABAJO):
                y++;
                break;
            case (Sprite.ARRIBA):
                y--;
                break;
        }

        ada.orientar(direccion);

        int i = 0;
        String s = "";
        boolean flag = true;
        while (i < items.size() && flag){
            if(items.get(i).getColumna() == x && items.get(i).getFila() == y){
                if(items.get(i).getTipo() == Sprite.OBJETO) {
                    s = "Ada coge el objeto \"" + items.get(i).getID()+"\"\n";
                    ada.setEquipo(items.get(i).getID());
                    items.remove(i);
                }
                else
                    s = "¡No puedes coger eso!\n";
                flag = false;
            }
            i++;
        }
        if (flag){
            s = "¡No hay nada para coger!\n";
        }

        log = log.concat(s);
    }

    /**
     * Usa el objeto equipado.
     */
    public void usar(){
        usar(ada.getDireccion());
    }

    /**
     * Usa el objeto equipado en la dirección seleccionada.
     * @param direccion entero con la dirección seleccionada.
     */
    public void usar(int direccion){
        String objeto = ada.getEquipo();
        if(objeto != null){

            //Si son tenazas y el herrero está al lado, se las da.
            if(objeto.equals("tenazas")){
                int x = ada.getColumna();
                int y = ada.getFila();

                switch (direccion) {
                    case (Sprite.DERECHA):
                        x++;
                        break;
                    case (Sprite.IZQUIERDA):
                        x--;
                        break;
                    case (Sprite.ABAJO):
                        y++;
                        break;
                    case (Sprite.ARRIBA):
                        y--;
                        break;
                }
                ada.orientar(direccion);
                int i = 0;
                String s = "";
                boolean flag = true;
                while (i < items.size() && flag){
                    if(items.get(i).getColumna() == x && items.get(i).getFila() == y){
                        if(items.get(i).getTipo() == Sprite.NPC && items.get(i).getID().equals("herrero")) {
                            s = "Ada le da las tenazas al herrero\n";
                            ada.setEquipo("");

                            //Si el objetivo era darle las tenazas, fin del juego. (Desechado)
                            if(parserJSON.getExtra().equals("tenazas")) {
                                meta = true;
                                victoria = true;
                            }

                        }
                        else
                            s = "¡No puedes usar las tenazas ahí!\n";
                        flag = false;
                    }
                    i++;
                }
                if (flag){
                    s = "¡No puedes usar las tenazas ahí!\n";
                }

                log = log.concat(s);
            }

        }else
            log = log.concat("No tienes ningún\n objeto.");

    }

    /**
     * Devuelve un entero con lo que se ha escuchado. Cambia según el nivel.
     * @return devuelve un entero con información acerca del nivel.
     */
    public int escuchar() {
        int r = 0;
        int i = 0;
        String s = "";

        while (i < items.size() && r == 0) {
            Sprite item = items.get(i);
            if (item.getID().equals("perro") && parserJSON.getExtra().equals("posada")) {
                switch (item.getFila()) {
                    case (1):
                        r = 3;
                        s = "Ada oye ladrar en la habitación 3.\n";
                        break;
                    case (3):
                        r = 2;
                        s = "Ada oye ladrar en la habitación 2.\n";
                        break;
                    case (6):
                        r = 1;
                        s = "Ada oye ladrar en la habitación 1.\n";
                }
            } else if (parserJSON.getExtra().equals("variables") && item.getID().equals("meta")) {
                s = "El monje dice: \"La meta correcta es la " + item.getColumna() + "\"\n";
                r = item.getColumna();
                item.setID("metaTerminado");
            } else if (item.getID().equals("herrero")) {
                s = "El herrero dice: \"¡Vamos, Ada!\"";
                r = 0;
            }

            i++;
        }

        if (s.equals(""))
            s = "Ada no escucha nada...\n";

        log = log.concat(s);
        return r;
    }

    /**
     * Centra cada sprite en el centro de su baldosa.
     */
    public void centrarDibujo(){
        ada.centrarDibujo();
        int i = 0;
        while(i < items.size())
            items.get(i++).centrarDibujo();
    }


    /**
     * Devuelve el desfase de cada baldosa.
      * @return devuelve un entero con el desfase de cada baldosa.
     */
    public int getBaldosa_offset() {
        return baldosa_offset;
    }

    /**
     * Devuelve cuánto hay que mover el tablero en la cooredanad Y para centrarlo.
     * @return devuelve un entero con el desfase en la coordenada Y.
     */
    public int getYoffset() {
        return yoffset;
    }

    /**
     * Devuelve cuánno hay que mover el tablero en la cooredanad X para centrarlo.
     * @return devuelve un entero con el desfase en la coordenada X.
     */
    public int getXoffset() {
        return xoffset;
    }

    /**
     * Devuelve el número de filas del tablero.
     * @return devuelve un entero con el número de filas del tablero.
     */
    public int getFilas() {
        return filas;
    }

    /**
     * Devuelve el número de columnas del tablero.
     * @return devuelve un entero con el número de columnas del tablero.
     */
    public int getColumnas() {
        return columnas;
    }

    /**
     * Indica si se ha alcanzado la meta.
     * @return devuelve un boolean que vale "true" si se ha pisado la meta.
     */
    public boolean getMeta() {
        return meta;
    }

    /**
     * Devuelve la altura de cada baldosa.
     * @return devuelve un entero con la altura de cada baldosa.
     */
    public int getAlturaBaldosa() {
        return alturaBaldosa;
    }

    /**
     * Devuelve la anchura de cada baldosa.
     * @return devuelve un entero con la anchura de cada baldosa.
     */
    public int getAnchuraBaldosa() {
        return anchuraBaldosa;
    }

    /**
     * Devuelve el log que se ha generado.
     * @return devuelve un String con el log generado.
     */
    public String getLog(){
        return log;
    }

    /**
     * Modifica el log introduciendo una nueva entrada.
     * @param s String con la nueva entrada del log.
     */
    public void setLog(String s) {
        this.log = log.concat(s);
    }

    /**
     * Acelera la animacion, haciendo que los sprites no tarden en moverse. Este método
     * se utiliza para probar las combinaciones antes de ejecutar cada nivel.
     */
    public void setVelocidadRapida() {
        ada.setVelocidadRapida();
        int i = 0;
        while(i < items.size())
            items.get(i++).setVelocidadRapida();
    }

    /**
     * Devuelve el Sprite de Ada.
     * @return devuelve el Sprite de Ada.
     */
    public Sprite getAda(){
        return ada;
    }

    /**
     * Indica si se ha derrotado a todos los enemigos del tablero.
     * @return devuelve un boolean que vale "true" si se han derrotado a todos los enemigos.
     */
    public boolean getBestias(){
        return bestias;
    }
}