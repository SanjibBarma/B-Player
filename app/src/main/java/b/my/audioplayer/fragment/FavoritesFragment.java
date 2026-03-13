package b.my.audioplayer.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private TextView emptyText;
    private ImageView ivFavorite;

    private MusicPlaybackService musicService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
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
        emptyText = view.findViewById(R.id.emptyText);
        ivFavorite = view.findViewById(R.id.ivFavorite);
    }

    private void setupRecyclerView() {
        adapter = new SongAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnSongClickListener((song, position) -> {
            if (isBound) {
                musicService.getMusicPlayer().setPlaylist(adapter.getSongs(), position);
                musicService.play();
                startActivity(new Intent(requireContext(), NowPlayingActivity.class));
            }
        });

        adapter.setOnSongMenuClickListener(new SongAdapter.OnSongMenuClickListener() {
            @Override
            public void onPlayNext(Song song) {
                if (isBound) {
                    musicService.getMusicPlayer().addToQueueNext(song);
                }
            }

            @Override
            public void onAddToPlaylist(Song song) {
                // Show playlist dialog
            }

            @Override
            public void onToggleFavorite(Song song) {
                viewModel.toggleFavorite(song);
            }

            @Override
            public void onDelete(Song song) {
                // Show delete confirmation
            }

            @Override
            public void onShare(Song song) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("audio/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM,
                        android.net.Uri.parse("file://" + song.getPath()));
                startActivity(Intent.createChooser(shareIntent, "Share song"));
            }
        });
    }

    private void observeData() {
        viewModel.getFavoriteSongs().observe(getViewLifecycleOwner(), songs -> {
            List<Song> favoriteSongs = songs != null ? songs : new ArrayList<>();
            adapter.setSongs(favoriteSongs);

            if (favoriteSongs.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                ivFavorite.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.colorFavorite)));
            }
        });
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
        }
    }
}