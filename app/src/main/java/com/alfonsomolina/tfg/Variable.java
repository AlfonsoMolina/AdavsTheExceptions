package com.alfonsomolina.tfg;

/**
 * Clase para guarda las variables que se hayan creado en la ejecución.
 * Tiene diferentes valores para cada tipo de variable diferente.
 *
 * @author Alfonso Molina
 */
public class Variable {

    int valorInt;
    boolean valorBoolean;
    int tipo;
    String nombre;

    /**
     * Constructor. Crea una variable con un tipo y nombre concretos.
     * @param tipo entero que indica el tipo de la variable.
     * @param nombre String con el nombre de la variable.
     */
    public Variable(int tipo, String nombre){
        this.tipo = tipo;
        this.nombre = nombre;
    }

    /**
     * Devuelve el valor entero de la variable.
     * @return devuelve un entero con el valor de la variable.
     */
    public int getInt() {
        return valorInt;
    }

    /**
     * Fija el valor entero de la variable.
     * @param valorInt entero con el valor de la variable.
     */
    public void setInt(int valorInt) {
        this.valorInt = valorInt;
    }

    /**
     * Devuelve el valor boolean de la variable.
     * @return devuelve un boolean con el valor de la variable.
     */
    public boolean getBoolean () {
        return valorBoolean;
    }

    /**
     * Fija el valor boolean de la variable.
     * @param valorBoolean boolean con el valor de la variable.
     */
    public void setBoolean(boolean valorBoolean) {
        this.valorBoolean = valorBoolean;
    }

    /**
     * Devuelve el tipo de la variable.
     * @return devuelve un entero con el tipo de la variable.
     */
    public int getTipo() {
        return tipo;
    }

    /**
     * Devuelve el nombre de la variable.
     * @return devuelve un String con el nombre de la variable.
     */
    public String getNombre() {
        return nombre;
    }
}
