package b.my.audioplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import b.my.audioplayer.R;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.utils.WaveView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private Context context;
    private List<Song> songs = new ArrayList<>();
    private OnSongClickListener listener;
    private OnSongMenuClickListener menuListener;

    private long playingSongId = -1;
    private boolean isPlaying = false;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    public interface OnSongMenuClickListener {
        void onPlayNext(Song song);
        void onAddToPlaylist(Song song);
        void onToggleFavorite(Song song);
        void onDelete(Song song);
        void onShare(Song song);
    }

    public SongAdapter(Context context) {
        this.context = context;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs != null ? songs : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setPlayingState(long songId, boolean playing) {
        long oldId = this.playingSongId;
        this.playingSongId = songId;
        this.isPlaying = playing;

        if (oldId != songId) {
            notifyItemChangedById(oldId);
        }
        notifyItemChangedById(songId);
    }

    public int getPlayingPosition() {
        if (playingSongId == -1) return -1;
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId() == playingSongId) return i;
        }
        return -1;
    }

    private void notifyItemChangedById(long id) {
        if (id == -1) return;
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId() == id) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setOnSongMenuClickListener(OnSongMenuClickListener menuListener) {
        this.menuListener = menuListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        boolean isThisPlaying = (song.getId() == playingSongId);
        holder.bind(song, position, isThisPlaying, isPlaying);
    }

    @Override
    public void onViewRecycled(@NonNull SongViewHolder holder) {
        super.onViewRecycled(holder);
        holder.wavePlayed.stopAnimation();
        holder.wavePlayed.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {

        ImageView albumArt;
        TextView title;
        TextView artist;
        TextView duration;
        ImageButton menuButton;
        ImageView favoriteIcon;
        WaveView wavePlayed;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            albumArt = itemView.findViewById(R.id.songAlbumArt);
            title = itemView.findViewById(R.id.songTitle);
            artist = itemView.findViewById(R.id.songArtist);
            duration = itemView.findViewById(R.id.songDuration);
            menuButton = itemView.findViewById(R.id.songMenuButton);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
            wavePlayed = itemView.findViewById(R.id.wavePlayed);
        }

        public void bind(Song song, int position, boolean isThisPlaying, boolean playing) {
            title.setText(song.getTitle());
            artist.setText(song.getArtist());
            duration.setText(formatDuration(song.getDuration()));

            // Start Marquee animation only if the song is playing
            title.setSelected(isThisPlaying && playing);
            artist.setSelected(isThisPlaying && playing);

            if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                Glide.with(context)
                        .load(song.getAlbumArt())
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(albumArt);
            } else {
                albumArt.setImageResource(R.drawable.ic_music_note);
            }

            favoriteIcon.setVisibility(song.isFavorite() ? View.VISIBLE : View.GONE);

            if (isThisPlaying) {
                wavePlayed.setVisibility(View.VISIBLE);
                if (playing) {
                    wavePlayed.startAnimation();
                } else {
                    wavePlayed.stopAnimation();
                }
            } else {
                wavePlayed.stopAnimation();
                wavePlayed.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSongClick(song, position);
            });

            menuButton.setOnClickListener(v -> showPopupMenu(v, song, position));
        }

        private void showPopupMenu(View view, Song song, int position) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.inflate(R.menu.menu_song_options);
            
            MenuItem favoriteItem = popup.getMenu().findItem(R.id.action_toggle_favorite);
            if (favoriteItem != null) {
                favoriteItem.setTitle(song.isFavorite() ? "Remove from Favorites" : "Add to Favorites");
            }

            popup.setOnMenuItemClickListener(item -> {
                if (menuListener == null) return false;
                int itemId = item.getItemId();
                if (itemId == R.id.action_add_to_playlist) {
                    menuListener.onAddToPlaylist(song);
                    return true;
                } else if (itemId == R.id.action_toggle_favorite) {
                    menuListener.onToggleFavorite(song);
                    notifyItemChanged(position); // Immediate UI feedback
                    return true;
                } else if (itemId == R.id.action_delete) {
                    menuListener.onDelete(song);
                    return true;
                } else if (itemId == R.id.action_share) {
                    menuListener.onShare(song);
                    return true;
                }
                return false;
            });
            popup.show();
        }

        private String formatDuration(long millis) {
            return String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
        }
    }
}