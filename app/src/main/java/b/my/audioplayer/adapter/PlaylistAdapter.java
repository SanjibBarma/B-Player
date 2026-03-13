package b.my.audioplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import b.my.audioplayer.R;
import b.my.audioplayer.model.Playlist;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private Context context;
    private List<Playlist> playlists = new ArrayList<>();
    private OnPlaylistClickListener listener;
    private OnPlaylistMenuClickListener menuListener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public interface OnPlaylistMenuClickListener {
        void onRename(Playlist playlist);
        void onDelete(Playlist playlist);
    }

    public PlaylistAdapter(Context context) {
        this.context = context;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setOnPlaylistMenuClickListener(OnPlaylistMenuClickListener menuListener) {
        this.menuListener = menuListener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {

        ImageView playlistIcon;
        TextView playlistName;
        TextView songCount;
        ImageButton menuButton;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistIcon = itemView.findViewById(R.id.playlistIcon);
            playlistName = itemView.findViewById(R.id.playlistName);
            songCount = itemView.findViewById(R.id.playlistSongCount);
            menuButton = itemView.findViewById(R.id.playlistMenuButton);
        }

        public void bind(Playlist playlist) {
            playlistName.setText(playlist.getName());
            songCount.setText(playlist.getSongCount() + " songs");

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });

            menuButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, v);
                popup.inflate(R.menu.menu_playlist_options);
                popup.setOnMenuItemClickListener(item -> {
                    if (menuListener == null) return false;

                    int itemId = item.getItemId();
                    if (itemId == R.id.action_rename_playlist) {
                        menuListener.onRename(playlist);
                        return true;
                    } else if (itemId == R.id.action_delete_playlist) {
                        menuListener.onDelete(playlist);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}