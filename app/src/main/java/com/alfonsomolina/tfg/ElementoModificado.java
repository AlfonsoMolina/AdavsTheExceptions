package com.alfonsomolina.tfg;

/**
 * Clase con el objeto contenido en las diferentes listas a usar.
 * Tiene dos atributos, "texto" y "flag", que varían según la lista utilizada.
 * @author Alfonso Molina
 */
public class ElementoModificado {

    private String texto;
    private boolean flag;

    /**
     * Constructor. Construye la clase con el texto a mostrar y la bandera desactivada.
     *
     * @param texto String con el texto.
     */
    public ElementoModificado(String texto){
        this.texto = texto;
        this.flag = false;
    }

    /**
     * Construye la clase con el texto a mostrar y la bandera elegida.
     *
     * @param flag boolean con la bandera.
     * @param texto String con el texto.
     */
    public ElementoModificado(boolean flag, String texto){
        this.texto = texto;
        this.flag = flag;
    }

    /**
     * Devuelve el texto.
     *
     * @return devuelve un String con el texto.
     */
    public String getTexto() {
        return texto;
    }

    /**
     * Modifica el texto por uno nuevo.
     * @param texto String con el texto
     */
    public void setTexto(String texto) {
        this.texto = texto;
    }

    /**
     * Devuelve la bandera.
     * @return boolean con la bandera.
     */
    public boolean isFlagged() {
        return flag;
    }

    /**
     * Modifica la bandera por una nueva.
     * @param flag boolean con la bandera.
     */
    public void setFlag(boolean flag){
        this.flag = flag;
    }
}
