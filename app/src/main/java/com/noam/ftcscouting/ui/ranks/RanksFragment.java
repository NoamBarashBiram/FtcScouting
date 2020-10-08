package com.noam.ftcscouting.ui.ranks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.noam.ftcscouting.R;
import com.noam.ftcscouting.utils.StaticSync;

public class RanksFragment extends Fragment implements StaticSync.Notifiable {

    private View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_ranks, container, false);
        return root;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StaticSync.register(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onNotified(Object message) {
    }

    private void updateUI() {
        getActivity().runOnUiThread(() -> {

        });
    }
}