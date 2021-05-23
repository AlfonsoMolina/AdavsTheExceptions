package com.alfonsomolina.tfg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import android.os.Handler;
import android.widget.Toast;

/**
 * Muestra la ejecución del código de forma gráfica en una SurfaceView con el tablero.
 *
 * @author Alfonso Molina
 */
public class ResolverCodigoActivity extends Activity {

    //Las diferentes formas que hay de ganar
    public static final int META = 1;          //Llegar a la meta
    public static final int LIMPIAR = 2;       //Eliminar todos los mobs
    public static final int METAYLIMPIAR = 3;
    public static final int HABLAR = 4;        //Decir algo en concreto
    public static final int USAR = 8;          //Usar las lineas de código obligatorias

    private ArrayList<Codigo> codigo;           //El código escrito, recibido por un intent
    private ArrayList<Variable> variables;      //Variables que se han ido declarando
    private ArrayList<int []> obligatorio;      //Array con los códigos que debe utilizar.

    private ParserJSON parserJSON;
    private SharedPreferences sharedPref;
    private Handler handler = new Handler();

    private Tablero tablero;
    private TextView[] texto;                   //Se mostrarán tres líneas del código
    private TextView log;

    private String nivel = "";
    private int etapa = 0;
    private int linea_actual = 0;
    private int contador = 0;                   //Si se ejecutan lineas_max y no se gana, fin. Para evitar bucles infinitos.
    private int lineas_max;
    private boolean esperar;                    //¿En esta línea hay que esperar a que se dibuje?
    private boolean enEjecucion;                //¿Está ejecutando una línea? (== dibujando)
    private boolean fin;                        //¿Se ha terminado ya el nivel?
    private boolean victoria;                   //Si está terminado, ¿se ha ganado?
    private boolean ejecucion_prev;             //True si la ejecución es la previa del onCreate.

    private boolean enTryCatch;

    /**
     * Constructor. Prueba en diferentes distribuciones del tablero para
     * comprobar si el código cumple en todas.
     *
     * @param savedInstanceState bundle necesario.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resolver_codigo);

        codigo = (ArrayList<Codigo>) getIntent().getSerializableExtra("CODIGO");
        nivel = getIntent().getStringExtra("NIVEL");
        etapa = getIntent().getIntExtra("ETAPA", 0);
        sharedPref = getSharedPreferences("PREF", Context.MODE_PRIVATE);
        String lenguaje = sharedPref.getString("lenguaje","Java");


        //Quitamos las lineas en blanco
        int i = 0;
        while(i < codigo.size()){
            if(codigo.get(i).getCodigo() == 0)
                codigo.remove(i--);
            i++;
        }

        //Si hay menos de tres líneas, las añadimos
        while(codigo.size() < 3) codigo.add(new Codigo(lenguaje, 0));

        log = (TextView) findViewById(R.id.log);
        texto = new TextView[3];
        texto[0] = (TextView) findViewById(R.id.mensaje_1);
        texto[1] = (TextView) findViewById(R.id.mensaje_2);
        texto[2] = (TextView) findViewById(R.id.mensaje_3);

        lineas_max = 2000;

        parserJSON = new ParserJSON(this, nivel, etapa);

        //Ahora se prueba en todas las combinaciones hasta que una salga error
        boolean flag = true; //false si se ha fallado en alguna
        ejecucion_prev = true;
        i = 0;
        log.setText("Construyendo el tablero. Espere...");
        while(flag && parserJSON.setSemilla(i++)){
            Log.d("bucle", "probando con semilla " + (i-1));
            contador = 0;
            linea_actual = 0;
            tablero = new Tablero(this,parserJSON,false);
            tablero.setVelocidadRapida();
            obligatorio = parserJSON.getObligatorio();
            variables = new ArrayList<>();
            while (victoria_rapido() == 0){
                ejecutar();
                tablero.actualizar();
                if(esperar){
                    tablero.actualizarIA();
                    esperar=false;
                }
                tablero.centrarDibujo();
                tablero.actualizar();
            }
            if (victoria_rapido() == -1)
                flag = false;
            Log.d("bucle", "victoria es " + victoria_rapido() + " y la flag es "+ flag + "");

        }
        ejecucion_prev = false;
        log.setText("");

        //Se han probado todas. Si uno está mal, flag será false y la semilla será i.
        //Si todos se han pasado, se dibuja uno al azar (semilla -1)
        Log.d("bucle", "se configura el tablero con la semmila " + (flag? -1 : (i-1)));
        parserJSON.setSemilla(flag ? -1 : i-1);

        //Y se dibuja el tablero, ya listo para la ejecución.
        tablero = new Tablero(this, parserJSON, false); //poner mundo y cosas.

        variables = new ArrayList<>();
        obligatorio = parserJSON.getObligatorio();
        contador = 0;
        linea_actual = 0;
        enEjecucion = false;
        fin = false;
        victoria = false;
        enTryCatch = false;

        escribir();

        LinearLayout surface = (LinearLayout)findViewById(R.id.tablero);
        surface.addView(tablero);



        //Listener para los clicks. Cuando se pulsa la pantalla, se ejecuta una línea más (si se puede).
        final View.OnClickListener pulsador = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!enEjecucion && !fin)
                    ejecutar();
            }

        };


        //Si es la primera vez que se entra, se pone el boton de ayuda
        if(!sharedPref.getBoolean("control_pulsar_resolver", false)){

            //Se guarda para que no vuelva a salir
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("control_pulsar_resolver",true);
            editor.apply();

            //Se activa un listener para que, cuando se pulse, se quite la imagen y se cargue el listener normal.
            findViewById(R.id.clicks).setBackgroundResource(R.drawable.pulsa);
            findViewById(R.id.clicks).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    findViewById(R.id.clicks).setBackgroundResource(0);
                    findViewById(R.id.clicks).setOnClickListener(pulsador);
                    ejecutar();
                }
            });

        } else {
            findViewById(R.id.clicks).setOnClickListener(pulsador);
        }

    }

    /**
     * Controla la ejecución de la linea. Se asegura de no haber llegado al final, comprueba si
     * se ha ganado o perdido y obliga al jugador a esperar un tiempo entre ejecuciones.
     */
    public void ejecutar(){
        Log.d("ejecucion", "Se ejecuta la linea "+ linea_actual);
        enEjecucion = true;

        //Si aun no se han ejecutado todas
         if (linea_actual < codigo.size() && contador++ < lineas_max) {

            //Coloco try y catch para corregir fácilmente errores en el código.
            //Si se ha escrito algo más y al ejecutarlo da un error, se poneun Toast
            //y se lleva al jugador de vuelta para corregirlo.
            try {
                leerLinea();
            } catch (IndexOutOfBoundsException e){
                if(!ejecucion_prev)
                    Toast.makeText(this, "Error - indices", Toast.LENGTH_LONG).show();
                fin = true;
                e.printStackTrace();
            }

        }else{
            if(!ejecucion_prev)
                Toast.makeText(this,contador < lineas_max ? "No lo has conseguido..." : "Fin del tiempo",Toast.LENGTH_LONG).show();
            fin = true;
        }



        if(!ejecucion_prev) {
            int tEspera;

            //Si se ha realizado una acción que requiera dibujarse, se espera un tiempo.
            //También se reduce el contador de la IA. Harán un movimiento cada vez que Ada se mueva,
            //no haciéndolo si es un bucle o algo similar.
            if (esperar) {
                tEspera = 750;
                if(codigo.get(linea_actual).getCodigo() == Codigo.DESCANSAR)
                    tEspera = 50;
                else if (codigo.get(linea_actual).getCodigo() == Codigo.GOLPEAR)
                    tEspera = 500;

            } else
                tEspera = 50;

            //Esperamos a que se complete la animación, escribo las nuevas líneas y ya se puede activar.
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (esperar)
                        tablero.actualizarIA();

                    victoria();
                    if(!victoria)
                        escribir();
                    enEjecucion = false;
                    esperar = false;
                    log.setText(tablero.getLog());
                    if (victoria)
                        log.setText(tablero.getLog() + "Has ganado.\n");
                }
            }, tEspera);
        }

        linea_actual++;


    }

    /**
     * Escribe la línea que se esta ejecutando y las dos adyacentes.
     */
    public void escribir(){
        if (linea_actual > 0 && linea_actual < (codigo.size()-1) ){
            texto[0].setText(codigo.get(linea_actual -1 ).escribirLinea());
            texto[1].setText(codigo.get(linea_actual).escribirLinea());
            texto[2].setText(codigo.get(linea_actual + 1).escribirLinea());
        } else if (linea_actual == 0){
            texto[0].setText("");
            texto[1].setText(codigo.get(linea_actual).escribirLinea());
            texto[2].setText(codigo.get(linea_actual + 1).escribirLinea());
        } else if (linea_actual == codigo.size()-1){
            texto[0].setText(codigo.get(linea_actual -1 ).escribirLinea());
            texto[1].setText(codigo.get(linea_actual).escribirLinea());
            texto[2].setText("");
        }
    }

    /**
     * Comprueba la victoria e indica si se ha ganado, perdido o se sigue jugando.
     * @return devuelve un entero con 1 si se ha ganado, -1 si se ha perdido y 0 si se sigue jugando.
     */
    private int victoria_rapido() {
        int b = 0;

        //Condiciones de derrota.
        //Si no tiene puntos de salud pierde
        if (tablero.getAda().getSalud() <= 0) {
            b = -1;
        }

        if (contador > 500 || linea_actual >= codigo.size()) {
            b = -1;
        }

        //Condiciones de victoria (solo si ha usado todas las líneas obligatorias)
        if (obligatorio.size() == 0) {
            //¿Ha llegado a la meta?
            if (parserJSON.getVictoria() == META && tablero.getMeta()) {
                b = 1;
            } else if (parserJSON.getVictoria() == LIMPIAR && !tablero.getBestias()) {
                b = 1;
            } else if (parserJSON.getVictoria() == USAR) {
                b = 1;
            } else if (parserJSON.getVictoria() == METAYLIMPIAR && !tablero.getBestias() && tablero.getMeta()) {
                b = 1;
            }
        }
        return b;
    }

    /**
     * Comprueba la victoria y actua en consecuencia. Si se ha finalizado la ejecución
     * para el juego y le da la opción al jugador de continuar o repetir el nivel, si ha ganado
     * o perdido respectivamente.
     */
    private void victoria(){

        //Condiciones de derrota.
        int vic = victoria_rapido();
        if(vic == -1){
            fin = true;
            victoria = false;
            if(tablero.getAda().getSalud() <= 0)
            Toast.makeText(this,"Te has debilitado...",Toast.LENGTH_LONG).show();
        } else if (vic == 1) {
            fin = true;
            victoria = true;
            Toast.makeText(this, "VICTORIA", Toast.LENGTH_LONG).show();
            ((Button) findViewById(R.id.boton_salir)).setText("Continuar");
            log.setText(tablero.getLog() + "¡Has ganado!\n");

        }

        if(fin && !victoria && !sharedPref.getBoolean("control_derrota",false)){
            findViewById(R.id.flecha).setVisibility(View.VISIBLE);
            sharedPref.edit().putBoolean("control_derrota",true).apply();
        }

    }

    /**
     * Lee la línea actual y la ejecuta.
     */
    public void leerLinea() {
        Codigo c = codigo.get(linea_actual);

        //Si había que usar este comando, lo quito.
        for(int i = 0; i < obligatorio.size(); i++){
            int[] linea = obligatorio.get(i);

            //Si sólo había que ejecutar el comando
            if(linea.length == 1){
                if(linea[0] == c.getCodigo()) {
                    obligatorio.remove(i);
                    i = obligatorio.size(); //Salimos.
                }
            //Si había que ejecutarlo con estas opciones en concreto
            } else if(linea.length == c.getNumParam()){
                boolean f = true;
                for(int j = 0; j < linea.length && f; j++){
                    if(c.getParam(j) != linea[j])
                        f = false;
                }

                //Si al final de la iteración la linea es identica a la que había que escribir:
                if(f){
                    obligatorio.remove(i);
                    i = obligatorio.size();
                }
            }
        }

        //Andar
        if (c.getCodigo() == Codigo.ANDAR) {
            esperar = true;

            //Si tiene un parámetro y es una dirección
            if (c.getNumParam() == 2 &&
                    (c.getParam(1) >= Codigo.ABAJO && c.getParam(1) <= Codigo.ARRIBA)) {
                tablero.getAda().setAccion(Sprite.ANDAR, c.getParam(1) - Codigo.ABAJO);

            //O si no tiene parámetros
            } else if (c.getNumParam() == 1) {
                tablero.getAda().setAccion(Sprite.ANDAR);

            } else {
                if (!ejecucion_prev)
                    Toast.makeText(this, "Error - Los parametros de \"andar\" están mal.",
                            Toast.LENGTH_LONG).show();
                fin = true;
            }

        //Mirar
        }else if (c.getCodigo() == Codigo.MIRAR) {
            if (c.getNumParam() == 1) {

                mirarTablero(tablero.getAda().getDireccion());
                tablero.getAda().orientar(tablero.getAda().getDireccion());


            } else if (c.getNumParam() == 2 && c.getParam(1) >= Codigo.ABAJO && c.getParam(1) <= Codigo.ARRIBA) {

                mirarTablero(c.getParam(1) - Codigo.ABAJO);
                tablero.getAda().orientar(c.getParam(1) - Codigo.ABAJO);

            } else {
                if(!ejecucion_prev)
                    Toast.makeText(this, "Error - parametros incorrectos en Mirar", Toast.LENGTH_LONG).show();
                fin = true;
            }
        } else if (c.getCodigo() == Codigo.OIR) {
            tablero.escuchar();
        //Golpear
        }else if (c.getCodigo() == Codigo.GOLPEAR) {
            esperar = true;
            if (c.getNumParam() == 2 && (c.getParam(1) >= Codigo.ABAJO && c.getParam(1) <= Codigo.ARRIBA)) {
                tablero.getAda().setAccion(Sprite.ATACAR, c.getParam(1) - Codigo.ABAJO);
            } else if (c.getNumParam() == 1) {
                tablero.getAda().setAccion(Sprite.ATACAR);
            } else {
                if(!ejecucion_prev)
                    Toast.makeText(this, "Error - Mal los parametros en golpear", Toast.LENGTH_LONG).show();
                fin = true;
            }
        //Descansar
        }else if (c.getCodigo() == Codigo.DESCANSAR) {
            esperar = true;
            if(c.getNumParam() == 1)
                tablero.getAda().setSalud(tablero.getAda().getSalud()+2 < sharedPref.getInt("salud",0) ? tablero.getAda().getSalud()+2 : sharedPref.getInt("salud",0));
            else {
                if (!ejecucion_prev)
                    Toast.makeText(this, "Error - Mal los parametros en descansar", Toast.LENGTH_LONG).show();
                fin = true;
            }

        //Coger
        }else if (c.getCodigo() == Codigo.COGER) {
            if (c.getNumParam() == 2 && (c.getParam(1) >= Codigo.ABAJO && c.getParam(1) <= Codigo.ARRIBA)) {
                tablero.coger(c.getParam(1) - Codigo.ABAJO);
            } else if (c.getNumParam() == 1) {
                tablero.coger();
            } else {
                if(!ejecucion_prev)
                    Toast.makeText(this, "Error - Mal los parametros en coger", Toast.LENGTH_LONG).show();
                fin = true;
            }
        //Usar
        }else if (c.getCodigo() == Codigo.USAR) {
            if (c.getNumParam() == 2 && (c.getParam(1) >= Codigo.ABAJO && c.getParam(1) <= Codigo.ARRIBA)) {
                tablero.usar(c.getParam(1) - Codigo.ABAJO);
            } else if (c.getNumParam() == 1) {
                tablero.usar();
            } else {
                if(!ejecucion_prev)
                    Toast.makeText(this, "Error - Mal los parametros en coger", Toast.LENGTH_LONG).show();
                fin = true;
            }

        //Creación de una variable
        }else if (c.getCodigo() >= Codigo.DECLARAR ) {
            //Se crea una variable del tipo guardado en el parámetro 0 y el nombre de la cadena escrita
            Variable v = buscarVariable(c.getCadena());
            if(v != null)
                variables.remove(variables.indexOf(v));
            variables.add(new Variable(c.getParam(0), c.getCadena()));

        //Asignación
        } else if (c.getCodigo() == Codigo.ASIGNACION){

            Variable v = buscarVariable(c.getCadena(0));
            if (v!= null) {

                if(v.getTipo() == Codigo.VAR_INT) {
                    //Si es del formato 'var = valor'
                    if (c.getNumParam() == 2 && (c.getParam(1) == Codigo.VARIABLE || c.getParam(1) == Codigo.NUMERO)) {
                        if (c.getParam(1) == Codigo.NUMERO)
                            v.setInt(Integer.parseInt(c.getCadena(1)));

                        else if (c.getParam(1) == Codigo.VARIABLE) {
                            Variable v2 = buscarVariable(c.getCadena(1));
                            if (v2 != null && v2.getTipo() == Codigo.VAR_INT)
                                v.setInt(v2.getInt());
                            else{
                                if(!ejecucion_prev)
                                    Toast.makeText(this, "Error en la asignación", Toast.LENGTH_LONG).show();
                                fin = true;
                            }

                        }
                        //Si es del formato 'var = mirar ( param )' o 'var = mirar () '
                    } else if (c.getParam(1) == Codigo.MIRAR) {
                        //Miramos lo que hay en la baldosa concreta del tablero
                        if (c.getNumParam() == 2) {

                            v.setInt(mirarTablero(tablero.getAda().getDireccion()));
                            tablero.getAda().orientar(tablero.getAda().getDireccion());


                        } else if (c.getNumParam() == 3 && c.getParam(2) >= Codigo.ABAJO && c.getParam(2) <= Codigo.ARRIBA) {

                            v.setInt(mirarTablero(c.getParam(2) - Codigo.ABAJO));
                            tablero.getAda().orientar(c.getParam(2) - Codigo.ABAJO);

                        } else {
                            if(!ejecucion_prev)
                            Toast.makeText(this, "Error - parametros incorrectos en Mirar", Toast.LENGTH_LONG).show();
                            fin = true;
                        }

                        //Si es escuchar
                    } else if (c.getParam(1) == Codigo.OIR) {

                        v.setInt(tablero.escuchar());

                    //Si es una constante
                    } else if (c.getParam(1) >= Codigo.CTE_VACIO && c.getParam(1) < Codigo.FIN_CONSTANTES) {
                        if(c.getParam(1) != Codigo.TRUE && c.getParam(1) != Codigo.FALSE)
                            v.setInt(c.getParam(1));
                        else{
                            if(!ejecucion_prev)
                            Toast.makeText(this, "Error en la asignación", Toast.LENGTH_LONG).show();
                            fin = true;
                        }
                    }
                } else if (v.getTipo() == Codigo.VAR_BOOLEAN){
                    if (c.getNumParam() == 2 && (c.getParam(1) == Codigo.VARIABLE)) {
                        Variable v2 = buscarVariable(c.getCadena(1));
                        if (v2 != null && v2.getTipo() == Codigo.VAR_BOOLEAN)
                            v.setBoolean(v2.getBoolean());
                        else {
                            if(!ejecucion_prev)
                            Toast.makeText(this, "Error en la asignación", Toast.LENGTH_LONG).show();
                            fin = true;
                        }

                    } else if (c.getParam(1) == Codigo.TRUE){
                        v.setBoolean(true);
                    } else if (c.getParam(1) == Codigo.FALSE){
                        v.setBoolean(false);
                    } else{
                        if(!ejecucion_prev)
                        Toast.makeText(this, "Error en la asignación", Toast.LENGTH_LONG).show();
                        fin = true;
                    }
                }
            }else {
                fin = true;
                if(!ejecucion_prev)
                Toast.makeText(this, "Error - Asignación incorrecta", Toast.LENGTH_LONG).show();
            }

        //Si el código es un IF o ELSE IF o WHILE
        //Hacen todos lo mismo: si la condición es cierta, se lee la siguiente línea
        // y si es falsa se va hacia el cierre del bucle
        } else if (c.getCodigo() == Codigo.IF || c.getCodigo() == Codigo.ELSE_IF || c.getCodigo() == Codigo.WHILE){

            //else if siempre tienen que ir después de un if o else if.
            if ( c.getCodigo() == Codigo.ELSE_IF){
                Codigo ca = codigo.get(linea_actual-1);

                if (ca.getCodigo() != Codigo.CIERRE || ca.getParam() != (Codigo.IF+50) && ca.getParam() != Codigo.ELSE_IF+50) {
                    if(!ejecucion_prev)
                        Toast.makeText(this, "Error - ElseIf sin un if delante", Toast.LENGTH_LONG).show();
                    fin = true;
                }
            }

            boolean resultado = esCorrecta(1);

            //Si la condición es cierta no se hace nada, porque se suma una línea automáticamente.
            // Si no se cumple, se salta al cierre y una línea más.
            if (!resultado){
                irCierre(c.getCodigo()+50);
            }

        //Si es un ELSE no se hace nada y se ejecuta la siguiente
        } else if (c.getCodigo() == Codigo.ELSE) {

            //else  siempre tienen que ir después de un if.
            Codigo ca = codigo.get(linea_actual-1);

            if (ca.getCodigo() != Codigo.CIERRE || (ca.getParam() != Codigo.IF + 50 && ca.getParam() != Codigo.ELSE_IF + 50)) {
                if(!ejecucion_prev)
                    Toast.makeText(this, "Error - Else no está detrás de un if " + ca.getParam(), Toast.LENGTH_LONG).show();
                fin = true;
            }

        } else if (c.getCodigo() == Codigo.DO){
            Log.d("ejec", "No se hace nada...");

        //Es un cierre. Esto quiere decir que se ha reccorido el bucle y hay que saltarse los ELSE IF y ELSE siguientes
        } else if (c.getCodigo() == Codigo.CIERRE){

            //Es un cierre de un if o else if. Hay que ir hasta el final saltándo los otros bucles.
            //Tengo que encontrar la siguiente línea en que se ejecute algo en este nivel
            if(c.getParam(1) == Codigo.IF+50 || c.getParam(1) == Codigo.ELSE_IF+50) {
                int cont = 0;               //Número de bloques de la "pila"
                boolean flag = false;
                int i = linea_actual;
                Codigo cc;

                do {
                    i++;
                    cc = codigo.get(i);

                    //Si se entra en otro bloque hay que asegurarse de seguir buscando hasta salir de él
                    if (cc.getCodigo() == Codigo.ELSE_IF || cc.getCodigo() == Codigo.ELSE) {
                        cont++;
                        flag = false;
                    } else if (cc.getCodigo() == Codigo.CIERRE) {
                        if (cc.getParam() >= Codigo.ELSE_IF + 50 || cc.getParam() <= Codigo.ELSE + 50)
                            cont--;
                    } else if (cc.getCodigo() != 0) {
                        flag = true;
                    }

                } while (cont > 0 || !flag);
                //Para salir del bucle tiene que encontrarse una línea que no sea ni if ni else if y,
                //además, que sea en el mismo nivel de ejecución, que no sea dentro de otro bloque.

                linea_actual = i-1;

            //En los while, hay que volver al principio si se llega la cierre
            }else if (c.getParam(1) == Codigo.WHILE +50 || c.getParam(1) == Codigo.DO + 50){

                if(c.getParam(1) == Codigo.WHILE +50 ||(c.getParam(1) == Codigo.DO+50 && esCorrecta(3)) )

                    irInicioBloque(c.getParam(1)-50);

            //Si es el cierre de un TRY, se salta el catch.
            } else if (c.getParam(1) == Codigo.TRY + 50) {

                if(codigo.get(linea_actual+1).getCodigo() != Codigo.CATCH) {
                    fin = true;
                    if(!ejecucion_prev)
                        Toast.makeText(this, "Error - Try sin Catch", Toast.LENGTH_LONG).show();
                }else {
                    linea_actual++;
                    irCierre(Codigo.CATCH+50);

                    enTryCatch = false;
                }
            }


        } else if (c.getCodigo() == Codigo.TRY){
            enTryCatch = true;
        }

    }

    /**
     * Cambia la línea actual para que la siguiente a ejecutar sea el cierre del bloque.
     * @param cierre_objetivo entero con el tipo de cierre que se busca.
     */
    private void irCierre(int cierre_objetivo){
        int cont = 1;               //Número de bloques de la "pila"
        int i = linea_actual;
        Codigo c = codigo.get(linea_actual);
        Codigo cc;
        //Hay que encontrar la próxima llave de cierre, teniendo cuidado de no
        //confundirla con la de un bloque IF interior (para eso se usa cont).
        do {
            i++;
            cc = codigo.get(i);

            //Si en esta línea empieza un bucle igual
            if(cc.getCodigo() == c.getCodigo()){
                cont++;
                //Si he encontrado un cierre del tipo correcto
            } else if (cc.getCodigo() == Codigo.CIERRE && cc.getParam() == cierre_objetivo){
                cont--;
            }

        }while (cont > 0 );

        linea_actual = i;
    }

    /**
     * Cambia la línea actual para que la siguiente a ejecutar sea el inicio del bucle.
     * @param objetivo entero con el tipo de bucle elegido.
     */
    private void irInicioBloque(int objetivo){
        int cont = 1;               //Número de bloques de la "pila"
        int i = linea_actual;
        Codigo cc;

        do {

            i--;
            cc = codigo.get(i);

            //Si se entra en otro bloque hay que seguir buscando hasta salir de él
            if (cc.getCodigo() == Codigo.CIERRE && cc.getParam() == objetivo+50)
                cont++;
            else if (cc.getCodigo() == objetivo)
                cont--;


        } while (cont > 0 );

        linea_actual = i-1;

    }

    /**
     * Busca una variable en la lista de variables.
     * @param s String con el nombre de la variable.
     * @return devuelve un objeto Variable, si se ha encontrado.
     */
    private Variable buscarVariable(String s){
        Variable v = null;

        int i = 0;

        while (i < variables.size()){
            if (variables.get(i).getNombre().equals(s)){
                v = variables.get(i);
                i = variables.size(); //Salgo del bucle
            }
            i++;
        }

        //if(v == null) {
           // if(!ejecucion_prev)
          //  Toast.makeText(this, "Error - No se ha encontrado la variable", Toast.LENGTH_LONG).show();
          //  fin = true;
        //}

        return v;
    }

    /**
     * Devuelve lo que haya en una posicion concreta del tablero.
     *
     * @param direccion entero con la dirección en la que se está mirando.
     * @return devuelve un entero que identifica lo que haya en la dirección seleccionada.
     */
    private int mirarTablero (int direccion){
        int x = tablero.getAda().getColumna();
        int y = tablero.getAda().getFila();

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

        if(tablero.mirar(x,y,false) == Codigo.CTE_EXCEPCION){
            if (enTryCatch) {
                //Se salta al inicio del catch.
                int cont = 1;               //Número de bloques de la "pila"
                int i = linea_actual;
                Codigo cc;

                do {
                    i++;
                    cc = codigo.get(i);

                    //Si en esta línea empieza un bucle igual
                    if (cc.getCodigo() == Codigo.TRY) {
                        cont++;
                        //Si he encontrado un cierre del tipo correcto
                    } else if (cc.getCodigo() == Codigo.CATCH) {
                        cont--;
                    }
                } while (cont > 0);
                linea_actual = i;
                enTryCatch = false;
                tablero.eliminar(x,y);
            } else {
                tablero.getAda().setSalud(0);
            }
        }

        return tablero.mirar(x,y,true);

    }

    /**
     * Comprueba si una condición es cierta o no.
     *
     * @param paramInicio entero con la posición de la línea en que empieza la condición.
     * @return devuelve un boolean con el resultado de la condición.
     */
    private boolean esCorrecta(int paramInicio) {

        boolean resultado = false;
        Codigo c = codigo.get(linea_actual);
        int j = paramInicio;

        int[] cond = new int[2];
        int indice = 0;
        int comparador = 0;

        //Si está vacío, error
        if(j == c.getNumParam()){
            Log.d("cond", "if vacío");
            fin = true;
            if(!ejecucion_prev)
                Toast.makeText(this, "Error - IF vacío", Toast.LENGTH_LONG).show();
            resultado = false;
        }

        //Hay que ir leyendo los parámetros de la condición. Siempre serán de tres en tres: 'cond1 == cond2'
        //Se buscan las condiciones y se guardan en cond[2]. Cuanda haya 2 y el comparador no sea 0,
        //se comparan. De momento sólo habrá una comparación, no hay && o ||.
        while (j < c.getNumParam()) {
            Log.d("cond", "ejecución " + j);

            if (c.getParam(j) == Codigo.VARIABLE) {
                Log.d("cond", " un elemento es una variable ");

                Variable v = buscarVariable(c.getCadena(j));

                if (v != null) {
                    if(v.getTipo() == Codigo.VAR_INT)
                        cond[indice] = v.getInt();
                    else if (v.getTipo() == Codigo.VAR_BOOLEAN && v.getBoolean())
                        cond[indice] = Codigo.TRUE;
                    else if (v.getTipo() == Codigo.VAR_BOOLEAN && !v.getBoolean())
                        cond[indice] = Codigo.FALSE;

                }

                indice++;
            } else if (c.getParam(j) == Codigo.NUMERO) {
                cond[indice] = Integer.parseInt(c.getCadena(j));
                Log.d("cond", "un elemento es un numero\": " + cond[indice]);

                indice++;

            } else if (c.getParam(j) >= Codigo.CTE_VACIO && c.getParam(j) < Codigo.FIN_CONSTANTES) {
                Log.d("cond", " es una constante  " + c.getParam(j));
                cond[indice] = c.getParam(j);
                indice++;

            } else if (c.getParam(j) == Codigo.MIRAR) {
                Log.d("cond", " un elemento es mirar");

                //Si tiene un parámetro más y es una dirección
                if (j + 1 < c.getNumParam() && c.getParam(j + 1) >= Codigo.ABAJO && c.getParam(j + 1) <= Codigo.ARRIBA) {
                    cond[indice] = mirarTablero(c.getParam(++j) - Codigo.ABAJO);
                    tablero.getAda().orientar(c.getParam(j) - Codigo.ABAJO);
                //Si no
                } else
                    cond[indice] = mirarTablero(tablero.getAda().getDireccion());

                Log.d("cond", "Se pone el valor de mirar: " + cond[indice]);

                //Si se consiguen más de dos y no se comparan el índice no se reinicia, y da
                //un nullPointerException que se captura.
                indice++;

            } else if (c.getParam(j) == Codigo.OIR) {
                cond[indice++] = tablero.escuchar();
                Log.d("cond", "se esucha el tablero " + cond[indice - 1]);
            } else if (c.getParam(j) >= Codigo.IGUAL && c.getParam(j) <= Codigo.DESIGUAL) {
                Log.d("cond", " un elemento es una condicion");
                comparador = c.getParam(j);
            } else if (c.getParam(j) == Codigo.TRUE) {
                Log.d("cond", "true");
                cond[indice++] = Codigo.TRUE;
            } else if (c.getParam(j) == Codigo.FALSE) {
                Log.d("cond","false");
                cond[indice++] = Codigo.FALSE;
            } else {
                Log.d("cond", "parámetro incorrecto");
                fin = true;
                if(!ejecucion_prev)
                    Toast.makeText(this, "Error - en los parámetros del IF", Toast.LENGTH_LONG).show();
                resultado = false;
            }


            //Si ya se tiene un juego completo
            if (indice == 2 && comparador != 0) {
                switch (comparador) {
                    case (Codigo.IGUAL):
                        resultado = (cond[0] == cond[1]);
                        break;
                    case (Codigo.DESIGUAL):
                        resultado = (cond[0] != cond[1]);
                        break;
                }

                indice = 0;
                comparador = 0;
                Log.d("cond", j + " se han cargado tres elementos y sale " + resultado);

            }


            j++;
        }

        //Si se han leído parámetros pero no se comparan:
        if (indice == 1 && cond[0] == Codigo.TRUE)
            resultado = true;
        else if (indice == 1 && cond[0] == Codigo.FALSE)
            resultado = false;
        else if (indice != 0){
            fin = true;
            if(!ejecucion_prev)
            Toast.makeText(this, "Error - en los parámetros del IF", Toast.LENGTH_LONG).show();
            resultado = false;
        }



        return resultado;
    }

    /**
     * Sale de la actividad. Va a una u otra distinta según el estado de la ejecución. Si se ha
     * ganado va a la siguiente, si se ha perdido o se sigue ejecutando vuelve al mapa
     * tras mostrar un aviso.
     * @param view vista del boton pulsado.
     */
    public void salir(View view) {
        Intent intent;

        //Se ha ganado y hay más niveles --> Ir al siguiente nivel
        if (victoria && parserJSON.haySiguienteNivel()) {
            intent = new Intent(this, IntroduccionActivity.class);
            intent.putExtra("NIVEL", nivel);
            intent.putExtra("ETAPA", etapa + 1);

            SharedPreferences sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            //Se actualiza el nivel máximo alcanzado
            editor.putInt(nivel+"_max",etapa+1);
            editor.apply();

        //Se ha ganado y no hay más niveles --> Al mapa
        }else if (victoria){
            intent = new Intent(this, MapaActivity.class);


            SharedPreferences sharedPref = this.getSharedPreferences("PREF", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();



            if (!sharedPref.getBoolean(nivel+"_extra",false)) {

                //Se añaden las ayudas extra y se suma la experiencia.
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


                editor.putInt("experiencia",sharedPref.getInt("experiencia",1)+parserJSON.getExperiencia());

                //Para no volver a añadir las ayudas extra cada vez que se realiza el nivel
                editor.putBoolean(nivel+"_extra",true);
            }

            editor.putInt(nivel+"_max",0);
            editor.apply();

        }else {
            //Si aun no se ha superado, se muestra un aviso por si se quiere volver atrás.
            new Aviso().show(getFragmentManager(), "confirmacion");
            intent = null;
        }

        if(intent != null)
            startActivity(intent);
    }

    /**
     * Vuelve a EscribirCodigoActivity sin perder las líneas escritas.
     * @param view vista del botón pulsado.
     */
    public void atras (View view){
        onBackPressed();
    }
}
