package b.my.audioplayer.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import b.my.audioplayer.R;
import b.my.audioplayer.activity.NowPlayingActivity;
import b.my.audioplayer.adapter.SongAdapter;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.service.MusicPlaybackService;
import b.my.audioplayer.viewmodel.MainViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class SongsFragment extends Fragment {

    private static final String TAG = "SongsFragment";

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private TextView emptyText;
    private View progressBar;
    private MusicPlaybackService musicService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            MusicPlaybackService.MusicBinder binder = (MusicPlaybackService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Attach ExoPlayer listener for state updates
            attachPlayerListener();

            // Immediately reflect current playing state
            syncPlayingState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_songs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        initViews(view);
        setupRecyclerView();
        observeData();
        bindService();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewSongs);
        emptyText = view.findViewById(R.id.emptyText);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new SongAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnSongClickListener((song, position) -> {
            if (!isBound || musicService == null) {
                Log.e(TAG, "Service not bound!");
                return;
            }

            Song currentSong = musicService.getCurrentSong();
            boolean isAlreadyPlaying = currentSong != null && currentSong.getId() == song.getId();

            if (isAlreadyPlaying) {
                // Same song — just open NowPlaying
                startActivity(new Intent(requireContext(), NowPlayingActivity.class));
            } else {
                // FIX: Use service method instead of direct MusicPlayer access
                Log.d(TAG, "Playing song: " + song.getTitle() + " at position " + position);
                musicService.setPlaylistAndPlay(adapter.getSongs(), position);
                startActivity(new Intent(requireContext(), NowPlayingActivity.class));
            }
        });

        adapter.setOnSongMenuClickListener(new SongAdapter.OnSongMenuClickListener() {
            @Override
            public void onPlayNext(Song song) {
                if (isBound && musicService != null) {
                    musicService.getMusicPlayer().addToQueueNext(song);
                }
            }

            @Override
            public void onAddToPlaylist(Song song) {
                showAddToPlaylistDialog(song);
            }

            @Override
            public void onToggleFavorite(Song song) {
                viewModel.toggleFavorite(song);
            }

            @Override
            public void onDelete(Song song) {
                showDeleteConfirmation(song);
            }

            @Override
            public void onShare(Song song) {
                shareSong(song);
            }
        });
    }

    private void observeData() {
        viewModel.getIsScanning().observe(getViewLifecycleOwner(), isScanning ->
                progressBar.setVisibility(View.GONE));

        viewModel.getAllSongs().observe(getViewLifecycleOwner(), songs -> {
            List<Song> songList = songs != null ? songs : new ArrayList<>();
            adapter.setSongs(songList);

            emptyText.setVisibility(songList.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(songList.isEmpty() ? View.GONE : View.VISIBLE);

            syncPlayingState();
            recyclerView.post(() -> scrollToPlaying(false));
        });
    }

    private void attachPlayerListener() {
        if (!isBound || musicService == null) return;

        musicService.getMusicPlayer().getExoPlayer().addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Log.d(TAG, "onIsPlayingChanged: " + isPlaying);
                    syncPlayingState();
                });
            }

            @Override
            public void onMediaItemTransition(androidx.media3.common.MediaItem mediaItem, int reason) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Log.d(TAG, "onMediaItemTransition");
                    syncPlayingState();
                    scrollToPlaying(true);
                });
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void syncPlayingState() {
        if (!isBound || musicService == null) return;

        Song currentSong = musicService.getCurrentSong();
        boolean playing = musicService.isPlaying();

        long id = (currentSong != null) ? currentSong.getId() : -1;

        Log.d(TAG, "syncPlayingState: id=" + id + ", playing=" + playing +
                ", song=" + (currentSong != null ? currentSong.getTitle() : "null"));

        adapter.setPlayingState(id, playing);
    }

    public void scrollToPlayingIfNeeded() {
        scrollToPlaying(true);
    }

    private void scrollToPlaying(boolean smooth) {
        int pos = adapter.getPlayingPosition();
        if (pos == -1) return;

        if (smooth) {
            recyclerView.smoothScrollToPosition(pos);
        } else {
            ((LinearLayoutManager) recyclerView.getLayoutManager())
                    .scrollToPositionWithOffset(pos, 0);
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
        if (isBound) {
            syncPlayingState();
            scrollToPlaying(false);
        }
    }

    private void bindService() {
        Intent intent = new Intent(requireContext(), MusicPlaybackService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------

    private void showAddToPlaylistDialog(Song song) {
        // Implementation
    }

    private void showDeleteConfirmation(Song song) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Song")
                .setMessage("Are you sure you want to delete \"" + song.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteSong(song))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareSong(Song song) {
        if (song == null || song.getPath() == null) {
            Toasty.error(requireContext(), "Cannot share this song", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = new File(song.getPath());

            if (!file.exists()) {
                Toasty.error(requireContext(), "File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    "b.my.audioplayer.fileprovider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share \"" + song.getTitle() + "\""));

        } catch (IllegalArgumentException e) {
            Toasty.error(requireContext(), "Cannot share this file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toasty.error(requireContext(), "Error sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}