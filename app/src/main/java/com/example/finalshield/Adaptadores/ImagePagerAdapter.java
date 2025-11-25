package com.example.finalshield.Adaptadores;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class ImagePagerAdapter extends FragmentStateAdapter {

    // *** CAMBIO 1: Cambiar el tipo de la lista interna a List<Uri> ***
    private final List<Uri> uriList;

    // *** CAMBIO 2: Aceptar List<Uri> en el constructor ***
    public ImagePagerAdapter(@NonNull FragmentActivity fragmentActivity, List<Uri> uriList) {
        super(fragmentActivity);
        this.uriList = uriList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Crea un ImageFragment para el URI en esa posición
        Uri uri = uriList.get(position);

        // *** CAMBIO 3: Convertir Uri a String para pasarlo al Fragmento (Si ImageFragment lo espera) ***
        String uriString = uri.toString();

        return ImageFragment.newInstance(uriString);
    }

    @Override
    public int getItemCount() {
        return uriList.size();
    }
}
