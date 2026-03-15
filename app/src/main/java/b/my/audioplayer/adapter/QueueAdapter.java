package b.my.audioplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import b.my.audioplayer.R;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.utils.WaveView;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private Context context;
    private List<Song> songs = new ArrayList<>();
    private int currentPlayingIndex = -1;
    private boolean isPlaying = false;
    private OnQueueItemClickListener listener;

    public interface OnQueueItemClickListener {
        void onSongClick(int position);
        void onRemoveClick(int position);
    }

    public QueueAdapter(Context context) {
        this.context = context;
    }

    public void setOnQueueItemClickListener(OnQueueItemClickListener listener) {
        this.listener = listener;
    }

    public void setQueue(List<Song> songs, int currentIndex) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        this.currentPlayingIndex = currentIndex;
        notifyDataSetChanged();
    }

    public void setPlayingState(int index, boolean playing) {
        int oldIndex = currentPlayingIndex;
        this.currentPlayingIndex = index;
        this.isPlaying = playing;

        if (oldIndex != index && oldIndex >= 0 && oldIndex < songs.size()) {
            notifyItemChanged(oldIndex);
        }
        if (index >= 0 && index < songs.size()) {
            notifyItemChanged(index);
        }
    }

    public void updateCurrentIndex(int index) {
        setPlayingState(index, isPlaying);
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        Song song = songs.get(position);
        boolean isCurrentPlaying = (position == currentPlayingIndex);
        holder.bind(song, position, isCurrentPlaying, isPlaying);
    }

    @Override
    public void onViewRecycled(@NonNull QueueViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.waveView != null) {
            holder.waveView.stopAnimation();
            holder.waveView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class QueueViewHolder extends RecyclerView.ViewHolder {

        ImageView albumArt;
        TextView title;
        TextView artist;
        TextView duration;
        ImageButton menuButton;
        ImageView favoriteIcon;
        WaveView waveView;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            albumArt = itemView.findViewById(R.id.songAlbumArt);
            title = itemView.findViewById(R.id.songTitle);
            artist = itemView.findViewById(R.id.songArtist);
            duration = itemView.findViewById(R.id.songDuration);
            menuButton = itemView.findViewById(R.id.songMenuButton);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
            waveView = itemView.findViewById(R.id.wavePlayed);

            itemView.setClickable(true);
        }

        public void bind(Song song, int position, boolean isCurrentPlaying, boolean playing) {
            title.setText(song.getTitle());
            artist.setText(song.getArtist());
            duration.setText(formatDuration(song.getDuration()));

            // Marquee for current playing
            title.setSelected(isCurrentPlaying && playing);
            artist.setSelected(isCurrentPlaying && playing);

            // Album art
            if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                Glide.with(context)
                        .load(song.getAlbumArt())
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(albumArt);
            } else {
                albumArt.setImageResource(R.drawable.ic_music_note);
            }

            // Favorite icon
            favoriteIcon.setVisibility(song.isFavorite() ? View.VISIBLE : View.GONE);

            // Wave animation for current playing
            if (isCurrentPlaying) {
                waveView.setVisibility(View.VISIBLE);
                if (playing) {
                    waveView.startAnimation();
                } else {
                    waveView.stopAnimation();
                }
            } else {
                waveView.stopAnimation();
                waveView.setVisibility(View.GONE);
            }

            // Change menu button to remove button for queue
            menuButton.setImageResource(R.drawable.ic_close);

            // Hide remove button for currently playing song
            menuButton.setVisibility(isCurrentPlaying ? View.INVISIBLE : View.VISIBLE);

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSongClick(getAdapterPosition());
                }
            });

            menuButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveClick(getAdapterPosition());
                }
            });
        }

        private String formatDuration(long millis) {
            return String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
        }
    }

    // ✅ এই method add করুন class এর মধ্যে
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= songs.size() ||
                toPosition < 0 || toPosition >= songs.size()) {
            return;
        }

        Song movedSong = songs.remove(fromPosition);
        songs.add(toPosition, movedSong);

        // Update current playing index if needed
        if (currentPlayingIndex == fromPosition) {
            currentPlayingIndex = toPosition;
        } else if (fromPosition < currentPlayingIndex && toPosition >= currentPlayingIndex) {
            currentPlayingIndex--;
        } else if (fromPosition > currentPlayingIndex && toPosition <= currentPlayingIndex) {
            currentPlayingIndex++;
        }

        notifyItemMoved(fromPosition, toPosition);
    }
}