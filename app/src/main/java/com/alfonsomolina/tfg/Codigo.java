package com.alfonsomolina.tfg;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Guarda información del codigo escrito en una línea de código. Implementa
 * serializable para poder mandarlo en un intent.
 *
 * @author Alfonso Molina
 */
public class Codigo implements Serializable{

    public static final int FIN_LETRAS = 258 ;

    public static final int CTE = 300;

    public static final int ABAJO = 301;
    public static final int IZQUIERDA = 302;
    public static final int DERECHA = 303;
    public static final int ARRIBA = 304;
    public static final int TRUE = 305;
    public static final int FALSE = 306;

    public static final int CTE_EXCEPCION = 319;
    public static final int CTE_VACIO = 320;
    public static final int CTE_META = 321;
    public static final int CTE_ROCA = 322;
    public static final int CTE_FIJO = 323;
    public static final int CTE_BESTIA = 324;
    public static final int CTE_PERSONA = 325;

    public static final int FIN_CONSTANTES = 400;

    public static final int ANDAR = 401;
    public static final int HABLAR = 402;
    public static final int MIRAR = 403;
    public static final int GOLPEAR = 404;
    public static final int DESCANSAR = 405;
    public static final int COGER = 406;
    public static final int USAR = 407;
    public static final int OIR = 408;


    public static final int FIN_ACCIONES = 500;

    public static final int IF = 501;
    public static final int ELSE_IF = 502;
    public static final int ELSE = 503;
    public static final int WHILE = 504;
    public static final int DO = 505;
    public static final int FOR = 506;
    public static final int FOR_SEP = 507;
    public static final int SWITCH = 508;
    public static final int CASE = 509;
    public static final int BREAK = 510;
    public static final int TRY = 520;
    public static final int CATCH = 521;

    public static final int FIN_BUCLE_INICIO = 550;

    public static final int CIERRE = 599;

    public static final int FIN_BUCLE = 600;

    public static final int IGUAL = 601;
    public static final int DESIGUAL = 602;
    public static final int MENOR = 603;
    public static final int MAYOR = 604;
    public static final int MENOR_IGUAL = 605;
    public static final int MAYOR_IGUAL = 606;
    public static final int AND = 607;
    public static final int OR = 608;


    public static final int FIN_COMP = 700;

    public static final int NUMERO = 997;
    public static final int VARIABLE = 998;
    public static final int ASIGNACION = 999;
    public static final int DECLARAR = 1000;
    public static final int VAR_INT = 1000;
    public static final int VAR_BOOLEAN = 1001;


    private int codigo;                   //Código principal de la línea.
    private ArrayList<Integer> params;    //Array con todos los elementos de la línea (empieza con el principal y sigue con los parámetros).
    private int nTab;                     //Número de tabulaciones que tiene la línea.
    private ArrayList<String> cadena;     //Cada parámetro tiene una cadena asignada, aunque no siempre se use.
    private String lenguaje;


    /**
     * Constructor.
     *
     * @param lenguaje String con el lenguaje de programacóon elegido.
     * @param c entero con el óodigo principal de la linea.
     */
    public Codigo(String lenguaje, int c) {
        this.codigo = c;
        this.lenguaje = lenguaje;
        params = new ArrayList<>();
        cadena = new ArrayList<>();
        if (c != 0)
            addParam(c);
        nTab = 0;
    }

    /**
     * Devuelve la íinea de código en caracteres.
     *
      * @return devuelve un String con la línea de código.
     */
    public String escribirLinea(){

        String linea = "";
        int i = 0;

        while (i < nTab){
            linea = linea.concat("\t");
            i++;
        }

        i=0;

        while (i < params.size() ) {
            linea = linea.concat(" " + code2text(i));
            //Si es un método dentro de un if (u otro), hay que cerrar el paréntesis
            if (i > 0 && params.get(i) >= ANDAR && params.get(i) < FIN_ACCIONES) {
                if (i+1 < params.size() && params.get(i+1) >= ABAJO && params.get(i+1) <= ARRIBA)
                    linea = linea.concat(" " + code2text(++i));
                linea = linea.concat(")");
            }
            i++;
        }

        //Se escribe el final de la línea según el código que haya escrito.
        if (codigo >= ANDAR && codigo < FIN_ACCIONES )
            linea = linea.concat(" );");
        else if (codigo == ELSE || codigo == DO || codigo == TRY || codigo == CATCH)
            linea = linea.concat ("{");
        else if (codigo >= IF && codigo < FIN_BUCLE_INICIO)
            linea = linea.concat (" ) {");
        else if (codigo == CIERRE && params.get(1) == DO+50)
            linea = linea.concat (" );");
        else if (codigo >= ASIGNACION )
            linea = linea.concat(";");

        return linea;
    }

    /**
     * Cambia el código principal.
     *
     * @param codigo entero con el nuevo código a fijar.
     */
    public void setCodigo(int codigo){
        this.codigo = codigo;
        params = new ArrayList<>();
        cadena = new ArrayList<>();
        if (codigo != 0)
            addParam(codigo);
    }

    /**
     * Añade un nuevo parametro.
     *
     * @param param entero con el código del parámetro a incluir.
     */
    public void addParam(int param){
        params.add(param);
        cadena.add("");
        if(params.size() == 1)
            codigo = param;
    }

    /**
     * Devuelve el código principal de la línea.
     *
     * @return devuelve un entero con el código principal de la línea.
     */
    public int getCodigo() {
        return codigo;
    }


    /**
     * Borra una condición. Si tras borrarla no queda ninguna, pone el código principal a cero.
     * Si ha borrado algo devuelve true.
     *
     * @return devuelve un boolean que vale "true" si ha borrado un parámetro, "false" en caso contrario
     */
    public boolean eliminarParam(){
        boolean salida = false;

        if (params.size() != 0) {

            cadena.remove(cadena.size() - 1);
            params.remove(params.size() - 1);
            salida = true;
        }

        if (params.size() == 0)
            codigo = 0;

        return salida;
    }

    /**
     * Devuelve el último parámetro de la línea.
     *
     * @return devuelve un entero con el código del íltimo parámetro de la línea.
     */
    public int getParam() {
        if (params.size() > 0)
            return params.get(params.size() - 1);
        else
            return -1;
    }

    /**
     * Devuelve el parámetro de la posición escogida.
     * @param i entero con la posición del parámetro en la línea.
     * @return devuelve un entero con el código del parámetro.
     */
    public int getParam(int i){
        return params.get(i);
    }

    /**
     * Devuelve el número de paáametros.
     * @return devuelve un entero con el número de parámetros.
     */
    public int getNumParam() {
        return params.size();
    }

    /**
     * Introduce un nuevo parámetro con una cadena complementaria.
     *
     * @param parametro entero con el código del parámetro.
     * @param cadena String con la cadena complementaria.
     */
    public void addCadena(int parametro, String cadena){
        params.add(parametro);
        this.cadena.add(cadena);
    }

    /**
     * Cambia la cadena del último parámetro.
     *
     * @param s String con la nueva cadena.
     */
    public void setCadena(String s) {
        if(cadena.size() > 0)
            cadena.set(cadena.size()-1, s);
    }

    /**
     * Cambia la cadena del parámetro elegido.
     *
     * @param s String con la nueva cadena.
     * @param i entero con la posición del parámetro en la línea.
     */
    public void setCadena(String s, int i){
        if (i < cadena.size())
            cadena.set(i,s);
    }

    /**
     * Devuelve la cadena delúultimo parametro de la línea.
     *
     * @return devuelve un String con la última cadena de la lista.
     */
    public String getCadena() {
        if (cadena.size() > 0)
            return cadena.get(cadena.size()-1);
        else
            return "";
    }

    /**
     * Devuelve la cadena de un parámetro en una posición determinada.
     *
     * @param i entero con la posición del parámetro en la línea.
     * @return devuelve un String con la cadena elegida.
     */
    public String getCadena(int i){
        return cadena.get(i);
    }

    /**
     * Devuelve el número de tabulaciones de la línea.
     * @return devuelve un entero con el número de tabulaciones.
     */
    public int getTab() {
        return nTab;
    }

    /**
     * Cambia el número de tabulaciones.
     * @param nTab entero con el nuevo número de tabulaciones.
     */
    public void setTab( int nTab) {
        this.nTab = nTab;
    }

    /**
     * Convierte un codigo determinado en una cadena de caracteres.
     *
     * @param i entero con la posición del parámetro en la línea.
     * @return devuelve un String con el parámetro escrito en texto.
     */
    private String code2text (int i){
        String s = "";

        //Primero buscamos en los demás idiomas, si se requiere.
        if(lenguaje.equals("C")){
            switch(params.get(i)){
                case(ANDAR):
                    s = "andar(";
                    break;
            }
        }

        //Si el lenguaje es java u otro y no se ha encontrado, se busca aquí.
        //Solo es necesario escribir en otros lenguajes las líneas que cambien.
        if(s.equals("")) {
            switch (params.get(i)) {
                case (ABAJO):
                    s = "ABAJO";
                    break;
                case (IZQUIERDA):
                    s = "IZQUIERDA";
                    break;
                case (DERECHA):
                    s = "DERECHA";
                    break;
                case (ARRIBA):
                    s = "ARRIBA";
                    break;
                case (TRUE):
                    s = "true";
                    break;
                case (FALSE):
                    s = "false";
                    break;
                case (ANDAR):
                    s = "ada.andar(";
                    break;
                case (HABLAR):
                    s = "ada.hablar(";
                    break;
                case (GOLPEAR):
                    s = "ada.golpear(";
                    break;
                case (MIRAR):
                    s = "ada.mirar(";
                    break;
                case (DESCANSAR):
                    s = "ada.descansar(";
                    break;
                case (COGER):
                    s = "ada.coger(";
                    break;
                case (USAR):
                    s = "ada.usar(";
                    break;
                case (OIR):
                    s = "ada.oir(";
                    break;
                case (VAR_INT):
                    s = "int";
                    break;
                case (VAR_BOOLEAN):
                    s = "boolean";
                    break;
                case (ASIGNACION):
                    s = cadena.get(0) + " = ";
                    break;
                case (NUMERO):
                case (VARIABLE):
                    s = cadena.get(i);
                    break;
                case (IF):
                    s = "if ( ";
                    break;
                case (ELSE_IF):
                    s = "else if ( ";
                    break;
                case (ELSE):
                    s = "else ";
                    break;
                case (WHILE):
                    s = "while ( ";
                    break;
                case (DO):
                    s = "do ";
                    break;
                case (TRY):
                    s = "try ";
                    break;
                case (CATCH):
                    s = "catch (Exception e) ";
                    break;
                case (CIERRE):
                    s = "}";
                    break;
                case (IGUAL):
                    s = " == ";
                    break;
                case (DESIGUAL):
                    s = " != ";
                    break;
                case (CTE_VACIO):
                    s = "Cte.VACIO ";
                    break;
                case (CTE_ROCA):
                    s = "Cte.OBSTACULO";
                    break;
                case (CTE_FIJO):
                    s = "Cte.OBST_IRROMPIBLE";
                    break;
                case (CTE_BESTIA):
                    s = "Cte.BESTIA";
                    break;
                case (CTE_META):
                    s = "Cte.META";
                    break;
                case (CTE_PERSONA):
                    s = "Cte.PERSONA";
                    break;

            }
        }
        return s;
    }

}