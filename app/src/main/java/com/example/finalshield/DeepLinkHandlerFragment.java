package com.example.finalshield;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class DeepLinkHandlerFragment extends Fragment {

    @Override
    public void onResume() {
        super.onResume();
        String token = null;

        if (getArguments() != null) {
            token = getArguments().getString("security_token");
        }

        Uri data = requireActivity().getIntent().getData();
        if (token == null && data != null) {
            String path = data.getPath();

            if (path != null && path.contains("/api/enlaces/")) {
                List<String> segments = data.getPathSegments();
                int index = segments.indexOf("validar");
                if (index > 0) {
                    token = segments.get(index - 1);
                }
            } else {
                token = data.getQueryParameter("security_token");
            }

            requireActivity().getIntent().setData(null);
        }

        if (token != null) {
            requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE)
                    .edit().putString("pending_token", token).apply();

            SharedPreferences prefsAuth = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
            boolean logged = prefsAuth.getBoolean("logged", false);

            NavController nav = NavHostFragment.findNavController(this);

            if (!logged) {
                nav.navigate(R.id.inicioSesion);
            } else {
                Bundle args = new Bundle();
                args.putString("security_token", token);

                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.deepLinkHandlerFragment, true)
                        .build();

                nav.navigate(R.id.verClave, args, options);
            }
        } else {
            NavHostFragment.findNavController(this).navigate(R.id.inicio);
        }
    }

}