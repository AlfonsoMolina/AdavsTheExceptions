package com.alfonsomolina.tfg;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Lee los archivos JSON para obtener información acerca del nivel.
 *
 * @author Alfonso Molina
 */
public class ParserJSON {

    private int etapa = 0;            //La etapa dentro de cada nivel (empieza en 0 y hasta num_etapas)
    private int num_etapas;
    private int[] aleatorio;          //Tabla con los valores donde puede ir el elemento aleatorio del tablero.
    private int semilla;              //La semilla se usa para dibujar todas las posibilidades
    private Context ctx;
    private JSONObject jsonObj;
    private SharedPreferences sharedPref;


    /**
     * Constructor. Crea un objeto JSONObject a partir del archivo JSON de un nivel.
     *
     * @param ctx contexto de la aplicación.
     * @param nombre string con el identificador del nivel.
     * @param etapa entero con el número de etapa.
     */
    public ParserJSON(Context ctx, String nombre, int etapa){
        this.ctx = ctx;
        this.etapa = etapa;
        this.semilla = -1;

        String jsonStr = leerFichero("res/raw/"+nombre+".json");
        if (jsonStr != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonStr);

                num_etapas = jsonArray.length();

                jsonObj = jsonArray.getJSONObject(etapa);



            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("ServiceHandler", "No se encontró el nivel");
        }

    }

    /**
     * Crea un objeto JSONObject a partir del archivo JSON de una ciudad.
     *
     * @param ctx contexto de la aplicación.
     * @param ciudad string con el identificador de la ciudad.
     */
    public ParserJSON(Context ctx, String ciudad){
        this.ctx = ctx;
        String jsonStr = leerFichero("res/raw/"+ciudad+".json");
        if (jsonStr != null) {
            try {
                jsonObj = new JSONObject(jsonStr);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("ServiceHandler", "No se encontró la ciudad");
        }
    }

    /**
     * Convierte el fichero en una String.
     * @param res cadena con el identificador del fichero.
     * @return devuelve un String.
     */
    public String leerFichero(String res)
    {
        InputStream entrada = this.getClass().getClassLoader().getResourceAsStream(res);
        InputStreamReader lector = new InputStreamReader(entrada);
        BufferedReader buffer = new BufferedReader(lector);

        String linea;
        StringBuilder textoFinal = new StringBuilder();

        try {
            while (( linea = buffer.readLine()) != null) {
                textoFinal.append(linea);
                textoFinal.append('\n');
            }
        } catch (IOException e) {
            return null;
        }

        sharedPref = ctx.getSharedPreferences("PREF",Context.MODE_PRIVATE);

        return textoFinal.toString();
    }

    /**
     * Configura una semilla para los elementos aleatorios.
     *
     * @param semilla una semilla, entre 0 y el número de elementos aleatorios.
     * @return devuelve "true" si se ha configurado una semilla y "false" si ya se han probado todas las combinaciones
     */
    public boolean setSemilla(int semilla){
        this.semilla = semilla;

        //Cuando ya se hayan hecho todas las posibilidades
        if (aleatorio != null && semilla >= aleatorio.length){
            aleatorio = null;
            return false;
        } else if (aleatorio != null){
            aleatorio = null;
            return true;
        }


        //Si es mayor que 1 y aleatorio sigue siendo null es que
        // no hay ningun item aleatorio y no se ha inicializado
        if(aleatorio == null && semilla > 0)
            return false;

        return true;
    }

    /*MÉTODOS PARA DIBUJAR EL MAPA*/

    /**
     * Devuelve el identificador de la miniatura del mapa.
     * @return devuelve un entero con el identificador de la miniatura del mapa.
     */
    public int getCiudadMiniatura(){
        try {
            return ctx.getResources().getIdentifier(jsonObj.getString("miniatura"), "drawable", ctx.getPackageName());
        } catch (JSONException e){
            return R.drawable.miniatura_pueblo_inicio;
        }
    }

    /**
     * Devuelve el nombre de la ciudad.
     * @return devuelve un String con el nombre de la ciudad.
     */
    public String getNombreCiudad() {
        try {
            return jsonObj.getString("nombre");
        } catch (JSONException e){
            return "No encontrado.";
        }
    }

    /**
     * Devuelve el identificador de la ciudad que está esa dirección en el mapa.
     * Devuelve "" si no hay ninguna.
     *
     * @param s String con la dirección cardinal elegida.
     * @return devuelve un String con el identificador de la ciudad o "" si no hay ninguna.
     */
    public String getCiudad(String s){
        try {
            if(jsonObj.getInt("exp_"+s) <= sharedPref.getInt("experiencia",1))
                return jsonObj.getString("ciudad_"+s);
            else
                return "";
        } catch (JSONException e){
            return "";
        }
    }

    /**
     * Muestra los niveles disponibles que hay en cada ciudad.
     * @return devuelve un array con los nombres de los niveles.
     */
    public String[] getListaNiveles() {
        try {


            JSONArray lista = jsonObj.getJSONArray("niveles");
            JSONArray lenguajes;
            String[] s = new String[lista.length()];
            int cont = 0;


            //Lenguaje y nivel del juegador.
            String lenguaje = sharedPref.getString("lenguaje","Java");
            int experiencia = sharedPref.getInt("experiencia",1);
            for (int i = 0; i < lista.length(); i++)
                if (lista.getJSONObject(i).getInt("experiencia") < experiencia) {

                    //Se coge la lista de lenguajes adimitidos para esta mision
                    try{
                        lenguajes = lista.getJSONObject(i).getJSONArray("lenguajes");
                    } catch (JSONException e){
                        lenguajes = null;
                    }

                    //Si no hay ninguno es que es java
                    if(lenguajes == null) {
                        if (lenguaje.equals("Java"))
                            //Si el lenguaje elegido es java se carga a la lista de niveles
                            s[cont++] = lista.getJSONObject(i).getString("nombre");
                    } else {
                        //Si tiene una lista de lenguajes se comprueban uno a uno.
                        //Si el elegido está en ella, se carga el nivel.
                        for (int j = 0; j < lenguajes.length(); j++)
                            if(lenguaje.equals(lenguajes.getString(j))){
                                s[cont++] = lista.getJSONObject(i).getString("nombre");
                                j = lenguajes.length();
                            }
                    }

                }

            return Arrays.copyOfRange(s,0,cont);

        } catch (JSONException e){
            return new String[0];
        }
    }

    /**
     * Devuelve, del nivel elegido, la descripción o el id.
     * @param nombre nombre de la mision.
     * @param s dato a obtener ("descripcion" o "id")
     * @return devuelve un String con la descripción o el identificador del nivel.
     */
    public String getNivel(String nombre, String s){
        try {
            //Como el numero i no tiene por qué corresponder a la lista de niveles
            //(puede haberse saltado uno por experiencia o lenguaje)
            //Tengo que recorrer toda la lista y encontrar a uno con este nombre.
            JSONArray lista = jsonObj.getJSONArray("niveles");

            for (int i = 0; i < lista.length(); i++)
                if(lista.getJSONObject(i).getString("nombre").equals(nombre))
                    return lista.getJSONObject(i).getString(s);

            return "";

        } catch (JSONException e){
            return "";
        }
    }


    /*MÉTODOS PARA MOSTRAR LA CONVERSACIÓN PRE-NIVEL*/

    /**
     * Devuelve el identificador del fondo de la etapa. Se mostrará cuando los personajes hablen,
     * en IntroduccionActivity.
     * @return devuelve un entero con el identificador del fondo de la etapa.
     */
    public int getFondo(){
        try {
            return ctx.getResources().getIdentifier(jsonObj.getString("fondo"), "drawable", ctx.getPackageName());
        } catch (JSONException e){
            return R.drawable.fondo_torre;
        }
    }

    /**
     * Devuelve el identificador de la animación de la persona que dice un mensaje determinado.
     * Si es la misma animación que el mensaje anterior de esa persona, devuelve 0. Devuelve -1
     * si no hay mas mensajes.
     *
     * @param i entero con el número del mensaje.
     * @return devuelve un entero con el idenficador, 0 o -1.
     */
    public int getPersona(int i){
        try{
            String s = jsonObj.getJSONArray("mensajes").getJSONObject(i).getString("imagen");
            if (s.equals("0")){
                return 0;
            } else{
                return ctx.getResources().getIdentifier(s, "drawable", ctx.getPackageName());
            }
        } catch (JSONException e){
            return -1;
        }

    }

    /**
     * Devuelve la posición del personaje que dice el mensaje.
     *
     * @param i entero con el número del mensaje.
     * @return devuelve "true" si está a la izquierda y "false" si está a la derecha.
     */
    public boolean getPosicion(int i){
        try{
            String s = jsonObj.getJSONArray("mensajes").getJSONObject(i).getString("posicion");
            switch(s){
                case("izquierda"):
                    return true;
                default:
                    return false;
            }
        } catch (JSONException e){
            return false;
        }
    }

    /**
     * Devuelve un mensaje concreto.
     *
     * @param i entero con el número del mensaje.
     * @return devuelve un string con el texto que se dice.
     */
    public String getMensaje(int i){
        try{
            return jsonObj.getJSONArray("mensajes").getJSONObject(i).getString("texto");
        } catch (JSONException e){
            return "";
        }
    }

    /**
     * Indica si el siguiente mensaje debe monstrarse automaticamente.
     *
     * @param i entero con el número del mensaje.
     * @return devuelve "true" si hay que mostrar el siguiente mensaje.
     */
    public boolean getSig(int i) {
        try{
            return jsonObj.getJSONArray("mensajes").getJSONObject(i).getBoolean("siguiente");
        } catch (JSONException e){
            return false;
        }
    }

    /**
     * Devuelve "true" si la etapa es solo un dialogo, sin escritura de codigo.
     * @return devuelve "true" si la etapa es un dialogo.
     */
    public boolean dialogo(){
        try{
            return jsonObj.getBoolean("dialogo");
        } catch (JSONException e){
            return false;
        }
    }

    /**
     * Devuelve la experienca ganada en el nivel.
     * @return devuelve un entero con la experiencia ganada.
     */
    public int getExperiencia(){
        try{
            return jsonObj.getInt("experiencia");
        } catch (JSONException e) {
            return 0;
        }
    }




    /*MÉTODOS PARA MOSTRAR EL RESUMEN DEL NIVEL*/

    /**
     * Devuelve el avatar del personaje que plantea el nivel.
     *
     * @return devuelve un entero con una imagen para el avatar.
     */
    public int getAvatar(){
        try{
            return ctx.getResources().getIdentifier(jsonObj.getString("avatar"), "drawable", ctx.getPackageName());
        } catch (JSONException e){
            return R.drawable.avatar;
        }
    }

    /**
     * Devuelve la descripción del nivel y sus objetivos.
     *
     * @param s string con la información a obtener ("resumen", "objetivo" u objetivo "secundario")
     * @return devuelve una cadena con la información requerida.
     */
    public String getInfoNivel(String s){
        try{
            return jsonObj.getString(s);
        } catch (JSONException e){
            return "";
        }
    }

    /*MÉTODOS PARA DIBUJAR EL TABLERO DEL NIVEL*/

    /**
     * Devuelve el tipo de victoria que le corresponde al nivel. Esta puede ser llegar a la meta,
     * eliminar a todos los enemigos, usar unas determinadas líneas de código o una combinación de
     * varias.
     *
     * @return devuelve un entero que identifica el tipo de victoria.
     */
    public int getVictoria(){
        try{
        String s = jsonObj.getString("victoria");
        switch (s) {
            case ("meta"):
                return 1;
            case ("hablar"):
                return 4;
            case ("limpiar"):
                return 2;
            case ("usar"):
                return 8;
            case ("meta y limpiar"):
                return 3;
            default:
                return 0;
        }
        }catch (JSONException e) {
            return -1;
        }
    }

    /**
     * Devuelve las medidas del tablero.
     *
     * @param s String con la medida a obtener ("filas" o "columnas").
     * @return devuelve un entero con la información requerida.
     */
    public int getInfoTablero(String s) {
        try {
            return jsonObj.getJSONObject("mapa").getInt(s);

        } catch (JSONException e) {
            return 0;
        }
    }


    /**
     * Devuelve la baldosa base del suelo. Esta será la baldosa mas común.
     *
     * @return devuelve un entero con el identificador al drawable de la baldosa.
     */
    public int getBaldosaBase(){
        try{
            return ctx.getResources().getIdentifier(jsonObj.getJSONObject("mapa").getString("baldosaBase"), "drawable", ctx.getPackageName());
        } catch (JSONException e){
            return R.drawable.baldosa_hierba;
        }
    }

    /**
     * Devuelve la posicion de una baldosa que es diferente a la baldosa base.
     *
     * @param i entero con el número de la baldosa.
     * @return devuelve un entero con la posición la baldosa.
     */
    public int getBaldosaPosicion(int i){
        try{
            return jsonObj.getJSONObject("mapa").getJSONArray("baldosaSuelo").getJSONObject(i).getInt("posicion");
        } catch (JSONException e){
            return -1;
        }
    }

    /**
     * Devuelve el identificador de la imagen de una baldosa que es diferente a la baldosa base.
     *
     * @param i entero con el número de la baldosa.
     * @return devuelve un entero con el identificador del drawable de la baldosa.
     */
    public int getBaldosaBmp(int i){
        try{
            return ctx.getResources().getIdentifier(jsonObj.getJSONObject("mapa").getJSONArray("baldosaSuelo").getJSONObject(i).getString("bitmap"),"drawable",ctx.getPackageName());
        } catch (JSONException e){
            return -1;
        }
    }

    /**
     * Devuelve el identificador de la imagen del sprite.
     *
     * @param i entero con el número del sprite en la lista de elementos en el tablero.
     * @return devuelve un entero con el identificador del drawable del sprite.
     */
    public int getItemBmp(int i){
        try{
            return ctx.getResources().getIdentifier(jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i).getString("bitmap"), "drawable", ctx.getPackageName());
        } catch (JSONException e){
            return R.drawable.sprite_ada;
        }
    }


    /**
     * Devuelve la posición del sprite en el tablero. Puede ser un valor aleatorio de entre una
     * serie de posiciones válidas.
     *
     * @param i entero con el número del sprite en la lista de elementos en el tablero.
     * @return devuelve un entero con la posición en el tablero.
     */
    public int getItemPosicion(int i){
        try{
            if (esAleatorio(i)){
                JSONObject mob = jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i);
                int tam = mob.getJSONArray("posicion").length();
                if (aleatorio == null) {
                    aleatorio = new int[tam];
                    for(int j = 0; j< aleatorio.length; j++ ){
                        aleatorio[j]=mob.getJSONArray("posicion").getInt(j);
                    }
                }

                int num ;
                int j = 0;

                while(j<20){
                    if (semilla == -1)
                        num = (int) Math.floor(Math.random()*tam);
                    else
                        num = semilla++ % tam;
                    if(aleatorio[num] != -1){
                        aleatorio[num] = -1;
                        return mob.getJSONArray("posicion").getInt(num);
                    }
                    j++;
                }
                return -1;
            } else {
                return jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i).getInt("posicion");
            }
        } catch (JSONException e){
            return -1;
        }
    }

    /**
     * Devuelve el tipo del sprite. Puede ser objeto, bestia, npc, roca, etc.
     *
     * @param i entero con el número del sprite en la lista de elementos en el tablero.
     * @return devuelve un entero que identifica el tipo de sprite.
     */
    public int getItemTipo(int i) {
        try {
            String s = jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i).getString("tipo");
            switch(s) {
                case("cocodrilo"):
                    return Sprite.COCODRILO;
                case("meta"):
                    return Sprite.META;
                case("ada"):
                    return Sprite.ADA;
                case("roca"):
                    return Sprite.ROCA;
                case("fijo"):
                    return Sprite.FIJO;
                case("diana"):
                    return Sprite.DIANA;
                case("bestia"):
                    return Sprite.BESTIA;
                case("objeto"):
                    return Sprite.OBJETO;
                case("NPC"):
                    return Sprite.NPC;
                case("excepcion"):
                    return Sprite.EXCEPCION;
                default:
                    return Sprite.ROCA;
            }
        } catch (JSONException e) {
            return Sprite.ROCA;
        }
    }

    /**
     * Devuelve el identificador del sprite. Permite programar diferentes comportamiento
     * en un mismo tipo de sprite.
     *
     * @param i entero con el numero del sprite en la lista de elementos en el tablero.
     * @return devuelve un String con el id particular.
     */
    public String getID(int i){
        try {
            return jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i).getString("ID");

        } catch (JSONException e) {
            try {
                return jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i).getString("bitmap");
            } catch (JSONException ee){
                return "";
            }
        }
    }

    /**
     * Devuelve la dirección a la que está mirando inicialmente el sprite.
     *
     * @param i entero con el número del sprite en la lista de elementos en el tablero.
     * @return devuelve un entero con la dirección a la que mira el sprite.
     */
    public int getItemDireccion(int i) {
        try{
            String s = jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i).getString("direccion");
            switch (s){
                case("arriba"):
                    return Sprite.ARRIBA;
                case("abajo"):
                    return Sprite.ABAJO;
                case("derecha"):
                    return Sprite.DERECHA;
                case("izquierda"):
                    return Sprite.IZQUIERDA;
                default:
                    return Sprite.ABAJO;
            }
        }catch (JSONException e){
            return Sprite.ABAJO;
        }
    }

    /**
     * Devuelve la salud máxima del sprite.
     *
     * @param i entero con el número del sprite en la lista de elementos en el tablero.
     * @return devuelve un entero con la salud.
     */
    public int getItemSalud(int i) {
        try {
            int fuerza = jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i).getInt("salud");

            //Si es ADA significa que se actualiza el valor de la salud máxima.
            if(getItemTipo(i) == Sprite.ADA)
                sharedPref.edit().putInt("salud",fuerza).apply();


            return fuerza;
        } catch (JSONException e) {
            if(getItemTipo(i) == Sprite.ADA)
                return sharedPref.getInt("salud",5);
            else
                return 100;
        }


    }

    /**
     * Devuelve la fuerza del sprite.
     *
     * @param i entero con el número del sprite en la lista de elementos en el tablero.
     * @return devuelve un entero con la fuerza.
     */
    public int getItemFuerza(int i) {
        try {
            int fuerza = jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i).getInt("fuerza");

            //Si es ADA significa que se actualiza el valor de fuerza.
            if(getItemTipo(i) == Sprite.ADA)
                sharedPref.edit().putInt("fuerza",fuerza).apply();


            return fuerza;
        } catch (JSONException e) {
            if(getItemTipo(i) == Sprite.ADA)
                return sharedPref.getInt("fuerza",2);
            else
                return 0;
        }

    }

    /**
     * Indica si el sprite tiene una posición aleatoria.
     *
     * @param i entero con el número del sprite en la lista de elementos en el tablero.
     * @return  devuelve "true" si el sprite tiene una posición aleatoria.
     */
    public boolean esAleatorio(int i) {
        try{
            return jsonObj.getJSONObject("mapa").getJSONArray("item").getJSONObject(i).getBoolean("aleatorio");
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Devuelve las posciones en las que puede aparecer un sprite con carácter aleatorio.
     *
     * @return devuelve un array de enteros con las posiciones que pueden tener sprites aleatorios.
     */
    public int [] getAleatorio(){
        if(aleatorio != null)
            return aleatorio;
        else
            return new int[0];
    }

    public String getExtra(){
        try{
            return jsonObj.getString("extra");
        } catch (JSONException e) {
            return "";
        }
    }


    /*MÉTODOS PARA OBTENER LOS MENSAJES DEL MENÚ DE AYUDA*/

    /**
     * Devuelve la lista de ayudas del nivel. Devuelve los nombres, a partir de los cuales
     * se podra acceder a las ayudas de la carpeta /res/strings.
     *
     * @param s string con el tipo de ayuda ("ayuda" o "ayuda_extra").
     * @return devuelve un array de string con los nombres de las ayudas.
     */
    public String[] getAyuda(String s){
        try{
            JSONArray jsonArray = jsonObj.getJSONArray(s);
            String [] l_s = new String[jsonArray.length()];
            for (int i = 0; i<jsonArray.length(); i++)
                l_s[i] = jsonArray.getString(i);
            return l_s;
        }catch (JSONException e){
            return new String[0];
        }
    }

    /**
     * Indica si hay más etapas o es la última.
     *
     * @return devuelve "true" si hay más etapas y "false" si es la última.
     */
    public boolean haySiguienteNivel() {
        return (etapa < (num_etapas-1));
    }

    /**
     * Devuelve las teclas a desbloquear en el nivel.
     *
     * @return devuelve un array bidimensional de enteros con las teclas a desbloquear.
     * La primera columna indica el teclado, la segunda la tecla.
     */
    public int [][] getTeclas() {
        try {
            int num_teclas = 0;
            JSONArray array = jsonObj.getJSONArray("teclas");
            for (int i = 0; i < array.length(); i++) {
                num_teclas += array.getJSONObject(i).getJSONArray("teclas").length();
            }

            int[][] teclas = new int[num_teclas][2];

            int i = 0;

            for (int j = 0; j < array.length(); j++) {
                JSONObject teclado = array.getJSONObject(j);
                for (int k = 0; k < teclado.getJSONArray("teclas").length(); k++) {
                    teclas[i][0] = teclado.getInt("teclado");
                    teclas[i][1] = teclado.getJSONArray("teclas").getInt(k);
                    i++;
                }
            }

            return teclas;

        } catch (JSONException e ) {
            return new int[0][0];
        }

    }

    /**
     * Devuelve las líneas de código de muestra del nivel.
     *
     * @return devuelve una lista inteligente de Codigo, con las líneas de muestra.
     */
    public ArrayList<Codigo> getLineasCodigo() {
        String lenguaje = sharedPref.getString("lenguaje", "Java");

        try {
            ArrayList<Codigo> codigo = new ArrayList<>();
            JSONArray jsonArray = jsonObj.getJSONArray("lineas");
            JSONArray jsonArray2;
            Codigo c;
            int j = 1;
            int num;
            for (int i = 0; i < jsonArray.length(); i++){
                jsonArray2 = jsonArray.getJSONArray(i);
                num = jsonArray2.getInt(0);
                c = new Codigo(lenguaje,0);
                j = 0;
                if(num < 10 && num > 0){
                    c.setTab(num);
                    j++;
                }

                for(; j < jsonArray2.length(); j++){
                    c.addParam(jsonArray2.getInt(j));
                    if (jsonArray2.getInt(j) == Codigo.NUMERO){
                        c.setCadena(""+ jsonArray2.getInt(j+1),j);
                        j++;
                    } else if (jsonArray2.getInt(j) == Codigo.VARIABLE || jsonArray2.getInt(j) == Codigo.ASIGNACION){
                        c.setCadena(jsonArray2.getString(j+1),j);
                        j++;
                    }

                }
                codigo.add(c);

            }

            return codigo;
        }catch (JSONException e) {
            ArrayList<Codigo> c = new ArrayList<>();
            c.add(new Codigo(lenguaje, 0));
            c.add(new Codigo(lenguaje, 0));
            c.add(new Codigo(lenguaje, 0));
            return c;
        }
    }

    /**
     * Devuelve la linea de código inicial para escribir.
     *
     * @return devuelve un entero con la línea de código inicial.
     */
    public int getPosicionInicial(){
        try{
            return jsonObj.getInt("posicion_inicial");
        }catch (JSONException e) {
            return 0;
        }
    }

    /**
     * Devuelve las líneas de código que es obligatorio escribir.
     *
     * @return devuelve una lista inteligente con las líneas de código que hay que escribir,
     * cada una de ellas compuesta por un arrays de enteros, con los códigos que hay en cada línea.
     */
    public ArrayList<int[]> getObligatorio(){
        try {
            ArrayList<int[]> lista = new ArrayList<>();
            JSONArray jsonArray = jsonObj.getJSONArray("obligatorio");
            JSONArray jsonArray2;
            for(int i = 0; i < jsonArray.length(); i++){
                jsonArray2 = jsonArray.getJSONArray(i);
                lista.add(new int [jsonArray2.length()]);

                for(int j = 0; j < jsonArray2.length(); j++)
                    lista.get(i)[j] = jsonArray2.getInt(j);

            }
            return lista;
        }catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Devuelve el número de constantes desbloqueadas.
     *
     * @return devuelve un entero con las constantes desbloqueadas.
     */
    public int getNumeroConstantes(){
        try{
            int i = jsonObj.getInt("constantes");

            sharedPref.edit().putInt("constantes",i).apply();

            return i;
        }catch (JSONException e) {
            return sharedPref.getInt("constantes",0);
        }
    }

    /**
     * Devuelve el número de tipos de variables desbloqueados.
     *
     * @return devuelve un entero con los tipos de variables desbloqueados.
     */
    public int getNumeroVariables(){
        try{
            int i = jsonObj.getInt("variables");

            sharedPref.edit().putInt("variables",i).apply();

            return i;
        }catch (JSONException e) {
            return sharedPref.getInt("variables",0);
        }
    }
}
