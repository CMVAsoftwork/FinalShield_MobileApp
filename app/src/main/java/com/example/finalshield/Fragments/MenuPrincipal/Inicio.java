package com.example.finalshield.Fragments.MenuPrincipal;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.AdaptadorArchivos;
import com.example.finalshield.Adaptadores.AdaptadorUltimos;
import com.example.finalshield.Model.Archivo;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class Inicio extends Fragment implements View.OnClickListener {

    private ListView listViewUltimos;
    private AdaptadorUltimos adaptador; // Usamos el nuevo adaptador
    private List<ArchivoMetadata> listaUltimos = new ArrayList<>();
    private ArchivoService archivoService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        archivoService = new ArchivoService(getContext());
        listViewUltimos = v.findViewById(R.id.listauarchivos);

        // Inicializamos el nuevo adaptador
        adaptador = new AdaptadorUltimos(getContext(), listaUltimos);
        listViewUltimos.setAdapter(adaptador);

        // Configuración de botones (mantengo tu lógica original)
        setupButtons(v);

        fetchUltimosArchivos();
    }

    private void setupButtons(View v) {
        int[] ids = {R.id.btnperfil, R.id.house, R.id.archivo, R.id.candadoclose,
                R.id.carpeta, R.id.mail, R.id.candadopen, R.id.btnseleccarpeta, R.id.btnenvcorreo};
        for (int id : ids) {
            View view = v.findViewById(id);
            if (view != null) view.setOnClickListener(this);
        }
    }

    private void fetchUltimosArchivos() {
        if (archivoService == null) return;

        archivoService.getAPI().getAllArchivos().enqueue(new Callback<List<Archivo>>() {
            @Override
            public void onResponse(Call<List<Archivo>> call, Response<List<Archivo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Archivo> todos = response.body();
                    Collections.reverse(todos); // Más nuevos arriba

                    listaUltimos.clear();
                    int limite = Math.min(todos.size(), 5); // Mostramos solo 5
                    for (int i = 0; i < limite; i++) {
                        listaUltimos.add(new ArchivoMetadata(todos.get(i)));
                    }
                    adaptador.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Archivo>> call, Throwable t) {
                Log.e("Inicio", "Error: " + t.getMessage());
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.carpeta) Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        else if (id == R.id.candadoclose) Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        else if (id == R.id.candadopen) Navigation.findNavController(v).navigate(R.id.archivosDesifrados);
        else if (id == R.id.btnseleccarpeta || id == R.id.archivo) Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        else if (id == R.id.btnenvcorreo || id == R.id.mail) Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        else if (id == R.id.btnperfil) Navigation.findNavController(v).navigate(R.id.perfil2);
    }
}
