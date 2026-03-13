package b.my.audioplayer.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import b.my.audioplayer.R;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.utils.Constants;
import b.my.audioplayer.viewmodel.MainViewModel;
import b.my.audioplayer.viewmodel.PlaylistViewModel;
import java.util.ArrayList;
import java.util.List;

public class SongPickerActivity extends AppCompatActivity {

    private MainViewModel mainViewModel;
    private PlaylistViewModel playlistViewModel;
    private RecyclerView recyclerView;
    private SongPickerAdapter adapter;
    private MaterialButton btnAdd;
    private TextView selectedCount;
    private ImageButton btnBack;

    private long playlistId;
    private List<Song> allSongs = new ArrayList<>();
    private List<Song> selectedSongs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_picker);

        playlistId = getIntent().getLongExtra(Constants.EXTRA_PLAYLIST_ID, -1);
        if (playlistId == -1) {
            finish();
            return;
        }

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        playlistViewModel = new ViewModelProvider(this).get(PlaylistViewModel.class);

        initViews();
        setupRecyclerView();
        observeData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewSongPicker);
        btnAdd = findViewById(R.id.btnAddToPlaylist);
        selectedCount = findViewById(R.id.selectedCount);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> onBackPressed());

        btnAdd.setOnClickListener(v -> {
            if (selectedSongs.isEmpty()) {
                Toast.makeText(this, "Select at least one song", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Long> songIds = new ArrayList<>();
            for (Song song : selectedSongs) {
                songIds.add(song.getId());
            }
            playlistViewModel.addSongsToPlaylist(playlistId, songIds);
            Toast.makeText(this, selectedSongs.size() + " songs added", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupRecyclerView() {
        adapter = new SongPickerAdapter(this, song -> {
            if (selectedSongs.contains(song)) {
                selectedSongs.remove(song);
            } else {
                selectedSongs.add(song);
            }
            updateSelectedCount();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void observeData() {
        mainViewModel.getAllSongs().observe(this, songs -> {
            allSongs = songs != null ? songs : new ArrayList<>();
            adapter.setSongs(allSongs);
        });
    }

    private void updateSelectedCount() {
        selectedCount.setText(selectedSongs.size() + " selected");
        btnAdd.setEnabled(!selectedSongs.isEmpty());
    }

    interface OnSongSelectedListener {
        void onSongSelected(Song song);
    }

    // Inner adapter class
    class SongPickerAdapter extends RecyclerView.Adapter<SongPickerAdapter.ViewHolder> {

        private android.content.Context context;
        private List<Song> songs = new ArrayList<>();
        private OnSongSelectedListener listener;


        SongPickerAdapter(android.content.Context context, OnSongSelectedListener listener) {
            this.context = context;
            this.listener = listener;
        }

        void setSongs(List<Song> songs) {
            this.songs = songs;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(context)
                    .inflate(R.layout.item_song_picker, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Song song = songs.get(position);
            holder.title.setText(song.getTitle());
            holder.artist.setText(song.getArtist());
            holder.checkBox.setChecked(selectedSongs.contains(song));

            holder.itemView.setOnClickListener(v -> {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
                listener.onSongSelected(song);
            });

            holder.checkBox.setOnClickListener(v -> {
                listener.onSongSelected(song);
            });
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, artist;
            CheckBox checkBox;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.songTitle);
                artist = itemView.findViewById(R.id.songArtist);
                checkBox = itemView.findViewById(R.id.checkBox);
            }
        }
    }
}