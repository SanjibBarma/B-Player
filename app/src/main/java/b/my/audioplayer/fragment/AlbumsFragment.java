package b.my.audioplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import b.my.audioplayer.R;
import b.my.audioplayer.activity.AlbumDetailActivity;
import b.my.audioplayer.adapter.AlbumAdapter;
import b.my.audioplayer.model.Album;
import b.my.audioplayer.viewmodel.MainViewModel;
import b.my.audioplayer.utils.Constants;
import java.util.ArrayList;
import java.util.List;

public class AlbumsFragment extends Fragment {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private AlbumAdapter adapter;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        initViews(view);
        setupRecyclerView();
        observeData();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewAlbums);
        emptyText = view.findViewById(R.id.emptyText);
    }

    private void setupRecyclerView() {
        adapter = new AlbumAdapter(requireContext());
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnAlbumClickListener(album -> {
            Intent intent = new Intent(requireContext(), AlbumDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ALBUM_ID, album.getId());
            intent.putExtra(Constants.EXTRA_ALBUM_NAME, album.getName());
            startActivity(intent);
        });
    }

    private void observeData() {
        viewModel.getAllAlbums().observe(getViewLifecycleOwner(), albums -> {
            if (albums != null && !albums.isEmpty()) {
                adapter.setAlbums(albums);
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }
}