package b.my.audioplayer.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import b.my.audioplayer.R;
import b.my.audioplayer.activity.PlaylistDetailActivity;
import b.my.audioplayer.adapter.PlaylistAdapter;
import b.my.audioplayer.model.Playlist;
import b.my.audioplayer.viewmodel.PlaylistViewModel;
import b.my.audioplayer.utils.Constants;
import es.dmoral.toasty.Toasty;

public class PlaylistsFragment extends Fragment {

    private PlaylistViewModel viewModel;
    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private TextView emptyText;
    private FloatingActionButton fabCreatePlaylist;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PlaylistViewModel.class);

        initViews(view);
        setupRecyclerView();
        observeData();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewPlaylists);
        emptyText = view.findViewById(R.id.emptyText);
        fabCreatePlaylist = view.findViewById(R.id.fabCreatePlaylist);

        fabCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
    }

    private void setupRecyclerView() {
        adapter = new PlaylistAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnPlaylistClickListener(playlist -> {
            Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PLAYLIST_ID, playlist.getId());
            startActivity(intent);
        });

        adapter.setOnPlaylistMenuClickListener(new PlaylistAdapter.OnPlaylistMenuClickListener() {
            @Override
            public void onRename(Playlist playlist) {
                showRenameDialog(playlist);
            }

            @Override
            public void onDelete(Playlist playlist) {
                showDeleteConfirmation(playlist);
            }
        });
    }

    private void observeData() {
        viewModel.getAllPlaylists().observe(getViewLifecycleOwner(), playlists -> {
            if (playlists != null && !playlists.isEmpty()) {
                adapter.setPlaylists(playlists);
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_playlist, null);

        TextInputEditText editText = dialogView.findViewById(R.id.editPlaylistName);
        TextInputLayout layout = dialogView.findViewById(R.id.layoutPlaylistName);
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Make dialog background fully transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();

        // Create Button Action
        btnCreate.setOnClickListener(v -> {
            String name = editText.getText().toString().trim();
            if (!name.isEmpty()) {
                viewModel.createPlaylist(name);
                Toasty.success(requireContext(), "Playlist created", Toasty.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toasty.warning(requireContext(), "Enter a valid name...", Toasty.LENGTH_SHORT).show();
            }
        });

        // Cancel Button Action
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }
    private void showRenameDialog(Playlist playlist) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_playlist, null);
        TextInputEditText editText = dialogView.findViewById(R.id.editPlaylistName);
        editText.setText(playlist.getName());
        editText.selectAll();

        new AlertDialog.Builder(requireContext())
                .setTitle("Rename Playlist")
                .setView(dialogView)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        viewModel.renamePlaylist(playlist.getId(), newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmation(Playlist playlist) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete \"" + playlist.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deletePlaylist(playlist);
                    Toast.makeText(requireContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}