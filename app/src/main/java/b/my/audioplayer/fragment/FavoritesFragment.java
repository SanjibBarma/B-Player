package b.my.audioplayer.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import b.my.audioplayer.R;
import b.my.audioplayer.activity.NowPlayingActivity;
import b.my.audioplayer.adapter.SongAdapter;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.service.MusicPlaybackService;
import b.my.audioplayer.viewmodel.MainViewModel;
import es.dmoral.toasty.Toasty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private View emptyState;

    private MusicPlaybackService musicService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.MusicBinder binder = (MusicPlaybackService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
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
        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        emptyState = view.findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        adapter = new SongAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnSongClickListener((song, position) -> {
            if (isBound && musicService != null) {
                musicService.setPlaylistAndPlay(adapter.getSongs(), position);
                startActivity(new Intent(requireContext(), NowPlayingActivity.class));
            }
        });

        adapter.setOnSongMenuClickListener(new SongAdapter.OnSongMenuClickListener() {
            @Override
            public void onPlayNext(Song song) {
                if (isBound && musicService != null) {
                    musicService.getMusicPlayer().addToQueueNext(song);
                    Toasty.info(requireContext(), "Will play next", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAddToPlaylist(Song song) {
                // Should be implemented or use common dialog
            }

            @Override
            public void onToggleFavorite(Song song) {
                viewModel.toggleFavorite(song);
            }

            @Override
            public void onDelete(Song song) {
                // Implementation for delete
            }

            @Override
            public void onShare(Song song) {
                shareSong(song);
            }
        });
    }

    private void observeData() {
        viewModel.getFavoriteSongs().observe(getViewLifecycleOwner(), songs -> {
            List<Song> favoriteSongs = songs != null ? songs : new ArrayList<>();
            adapter.setSongs(favoriteSongs);

            if (favoriteSongs.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
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
}