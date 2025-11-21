package com.example.finalshield.Adaptadores;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.finalshield.R;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageFragment extends Fragment {

    private static final String ARG_URI = "image_uri";

    // crear el fragmento y pasarle el URI
    public static ImageFragment newInstance(String uriString) {
        ImageFragment fragment = new ImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URI, uriString);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // usa el nuevo layout fragment_image.xml
        return inflater.inflate(R.layout.activity_vista_imagen_fragment_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PhotoView imagenCompleta = view.findViewById(R.id.imagenCompleta);

        if (getArguments() != null) {
            String uriString = getArguments().getString(ARG_URI);
            if (uriString != null) {
                // Cargar la imagen con Glide
                Glide.with(this)
                        .load(Uri.parse(uriString))
                        .into(imagenCompleta);
            }
        }
    }
}