package com.alfonsomolina.tfg;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.alfonsomolina.tfg.MapaActivity;
import com.alfonsomolina.tfg.R;

/**
 * Dialog que solicita una confirmación cuando se pulsa el boton "atrás" dentro de una misión.
 *
 * @author Alfonso Molina
 */
public class Aviso extends DialogFragment {

    /**
     * Crea el mensaje de aviso.
     * @param savedInstanceState Bundle necesario.
     * @return devuelve el Dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.aviso_mensaje).setTitle(R.string.aviso_titulo);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Se vuelve al mapa.
                startActivity(new Intent(getActivity(), MapaActivity.class));
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //No se hace nada. Se cierra el diálogo.
            }
        });
        return builder.create();
    }
}

