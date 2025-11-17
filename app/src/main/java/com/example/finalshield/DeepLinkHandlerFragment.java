package com.example.finalshield;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DeepLinkHandlerFragment extends Fragment {

    @Override
    public void onResume() {
        super.onResume();

        Uri data = requireActivity().getIntent().getData();
        String token = null;

        if (data != null) {
            token = data.getQueryParameter("security_token");
        }

        requireActivity().getIntent().setData(null);

        NavController nav = NavHostFragment.findNavController(this);

        if (token == null) {
            nav.navigate(R.id.inicio);
            return;
        }

        SharedPreferences prefsDL = requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE);
        prefsDL.edit().putString("pending_token", token).apply();

        SharedPreferences prefsAuth = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        boolean logged = prefsAuth.getBoolean("logged", false);

        Bundle args = new Bundle();
        args.putString("security_token", token);

        if (!logged) {
            nav.navigate(R.id.inicioSesion, args);
        } else {
            nav.navigate(R.id.verClave2, args);
        }
    }

}