package b.my.audioplayer.adapter;

import static b.my.audioplayer.utils.Constants.textGradient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import b.my.audioplayer.R;
import b.my.audioplayer.model.Album;
import java.util.ArrayList;
import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    private Context context;
    private List<Album> albums = new ArrayList<>();
    private OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public AlbumAdapter(Context context) {
        this.context = context;
    }

    public void setAlbums(List<Album> albums) {
        this.albums = albums;
        notifyDataSetChanged();
    }

    public void setOnAlbumClickListener(OnAlbumClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.bind(album);
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    class AlbumViewHolder extends RecyclerView.ViewHolder {

        ImageView albumArt;
        TextView albumName;
        TextView artistName;
        TextView songCount;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumArt = itemView.findViewById(R.id.albumArt);
            albumName = itemView.findViewById(R.id.albumName);
            artistName = itemView.findViewById(R.id.albumArtist);
            songCount = itemView.findViewById(R.id.albumSongCount);
        }

        public void bind(Album album) {
            textGradient(albumName);
            textGradient(artistName);
            textGradient(songCount);
            albumName.setText(album.getName());
            artistName.setText(album.getArtist());
            songCount.setText(album.getSongCount() + " songs");

            if (album.getAlbumArt() != null && !album.getAlbumArt().isEmpty()) {
                Glide.with(context)
                        .load(album.getAlbumArt())
                        .placeholder(R.drawable.ic_album)
                        .error(R.drawable.ic_album)
                        .into(albumArt);
            } else {
                albumArt.setImageResource(R.drawable.ic_album);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlbumClick(album);
                }
            });
        }
    }
}