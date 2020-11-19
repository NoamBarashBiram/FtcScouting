package com.noam.ftcscouting.ui.myMatches;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.noam.ftcscouting.R;
import com.noam.ftcscouting.utils.StaticSync;


public class MyMatchesFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_my_matches, container, false);
        root.findViewById(R.id.addMatch).setOnClickListener(this::addMatch);
        return root;
    }

    private void addMatch(View view) {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void updateUI() {

    }

    private void runOnUiThread(Runnable action){
        getActivity().runOnUiThread(action);
    }
}