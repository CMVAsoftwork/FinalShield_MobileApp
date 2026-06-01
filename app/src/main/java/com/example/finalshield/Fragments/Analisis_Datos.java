package com.example.finalshield.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalshield.DTO.EstadisticasResumenDTO;
import com.example.finalshield.R;
import com.example.finalshield.Service.EstadisticasService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Analisis_Datos extends Fragment {

    private TextView txtValor1;
    private TextView txtValor2;
    private TextView txtValor3;
    private TextView txtValor4;
    private LineChart chartRendimiento;

    public Analisis_Datos() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_analisis__datos,
                container,
                false
        );

        inicializarVistas(view);
        cargarEstadisticas();

        return view;
    }

    private void inicializarVistas(View view) {

        txtValor1 = view.findViewById(R.id.txtValor1);
        txtValor2 = view.findViewById(R.id.txtValor2);
        txtValor3 = view.findViewById(R.id.txtValor3);
        txtValor4 = view.findViewById(R.id.txtValor4);
        chartRendimiento = view.findViewById(R.id.chartRendimiento);

    }

    private void cargarGrafica(
            long cifrados,
            long correos,
            long descifrados
    ) {

        List<Entry> entries = new ArrayList<>();

        entries.add(new Entry(0f, (float) cifrados));
        entries.add(new Entry(1f, (float) correos));
        entries.add(new Entry(2f, (float) descifrados));

        LineDataSet dataSet =
                new LineDataSet(entries, "Actividad FinalShield");

        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);

        LineData lineData = new LineData(dataSet);

        chartRendimiento.setData(lineData);

        chartRendimiento.invalidate();
    }

    private void cargarEstadisticas() {

        EstadisticasService estadisticasService =
                new EstadisticasService(requireContext());

        estadisticasService
                .getAPI()
                .obtenerResumen()
                .enqueue(new Callback<EstadisticasResumenDTO>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<EstadisticasResumenDTO> call,
                            @NonNull Response<EstadisticasResumenDTO> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            EstadisticasResumenDTO stats =
                                    response.body();

                            txtValor1.setText(
                                    String.valueOf(
                                            stats.getArchivosCifrados()
                                    )
                            );

                            txtValor2.setText(
                                    String.valueOf(
                                            stats.getCorreosEnviados()
                                    )
                            );

                            txtValor3.setText(
                                    String.valueOf(
                                            stats.getArchivosDescifrados()
                                    )
                            );

                            // Puedes cambiar esto cuando agregues más métricas
                            txtValor4.setText("0");

                            cargarGrafica(
                                    stats.getArchivosCifrados(),
                                    stats.getCorreosEnviados(),
                                    stats.getArchivosDescifrados()
                            );

                        } else {

                            Log.e(
                                    "ESTADISTICAS",
                                    "Código HTTP: " + response.code()
                            );

                            Toast.makeText(
                                    requireContext(),
                                    "No se pudieron obtener las estadísticas",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<EstadisticasResumenDTO> call,
                            @NonNull Throwable t
                    ) {

                        Log.e(
                                "ESTADISTICAS",
                                "Error",
                                t
                        );

                        Toast.makeText(
                                requireContext(),
                                "Error de conexión",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}