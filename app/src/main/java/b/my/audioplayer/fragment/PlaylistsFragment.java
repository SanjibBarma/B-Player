package b.my.audioplayer.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import b.my.audioplayer.R;
import b.my.audioplayer.activity.PlaylistDetailActivity;
import b.my.audioplayer.adapter.PlaylistAdapter;
import b.my.audioplayer.model.Playlist;
import b.my.audioplayer.utils.GradientTextView;
import b.my.audioplayer.viewmodel.PlaylistViewModel;
import b.my.audioplayer.utils.Constants;
import es.dmoral.toasty.Toasty;

public class PlaylistsFragment extends Fragment {

    private PlaylistViewModel viewModel;
    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private TextView emptyText;

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
        FloatingActionButton fabCreatePlaylist = view.findViewById(R.id.fabCreatePlaylist);

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

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextInputEditText editText = dialogView.findViewById(R.id.editPlaylistName);
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvTitle.setText("Create Playlist");
        btnCreate.setText("Create");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCreate.setOnClickListener(v -> {
            if (editText.getText() != null) {
                String name = editText.getText().toString().trim();
                if (!name.isEmpty()) {
                    viewModel.createPlaylist(name);
                    Toasty.success(requireContext(), "Playlist created", Toasty.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toasty.warning(requireContext(), "Enter a valid name...", Toasty.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showRenameDialog(Playlist playlist) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_playlist, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextInputEditText editText = dialogView.findViewById(R.id.editPlaylistName);
        Button btnRename = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvTitle.setText("Rename Playlist");
        btnRename.setText("Rename");
        editText.setText(playlist.getName());
        editText.selectAll();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnRename.setOnClickListener(v -> {
            if (editText.getText() != null) {
                String newName = editText.getText().toString().trim();
                if (!newName.isEmpty()) {
                    viewModel.renamePlaylist(playlist.getId(), newName);
                    Toasty.success(requireContext(), "Playlist renamed", Toasty.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toasty.warning(requireContext(), "Enter a valid name...", Toasty.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showDeleteConfirmation(Playlist playlist) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_playlist, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        CardView layout = dialogView.findViewById(R.id.layoutPlaylistName);
        Button btnDelete = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvTitle.setText("Delete Playlist");
        layout.setVisibility(View.GONE); // Hide input for delete dialog

        // Add a message text view
        GradientTextView messageView = new GradientTextView(requireContext());
        messageView.setText("Are you sure you want to delete \"" + playlist.getName() + "\"?");
//        messageView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary));
        messageView.setTextSize(16);
        messageView.setPadding(0, 24, 0, 24);

        ViewGroup parent = (ViewGroup) layout.getParent();
        int index = parent.indexOfChild(layout);
        parent.addView(messageView, index);

        btnDelete.setText("Delete");
        btnDelete.setTextColor(Color.RED);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnDelete.setOnClickListener(v -> {
            viewModel.deletePlaylist(playlist);
            Toasty.success(requireContext(), "Playlist deleted", Toasty.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }
}
