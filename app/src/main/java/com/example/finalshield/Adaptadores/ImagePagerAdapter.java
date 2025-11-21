package com.example.finalshield.Adaptadores;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class ImagePagerAdapter extends FragmentStateAdapter {

    private final List<String> uriList; // Lista de todos los URIs

    public ImagePagerAdapter(@NonNull FragmentActivity fragmentActivity, List<String> uriList) {
        super(fragmentActivity);
        this.uriList = uriList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Crea un ImageFragment para el URI en esa posición
        String uriString = uriList.get(position);
        return ImageFragment.newInstance(uriString);
    }

    @Override
    public int getItemCount() {
        return uriList.size(); // Total de imágenes
    }
}