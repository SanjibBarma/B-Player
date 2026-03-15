package b.my.audioplayer.video_player.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import b.my.audioplayer.R;
import b.my.audioplayer.video_player.model.VideoModel;

public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_GRID = 0;
    private static final int VIEW_TYPE_LIST = 1;

    private Context context;
    private List<VideoModel> videoList;
    private OnVideoClickListener listener;
    private int lastPosition = -1;
    private int viewType = VIEW_TYPE_GRID;

    public interface OnVideoClickListener {
        void onVideoClick(VideoModel video, int position);
    }

    public VideoAdapter(Context context, OnVideoClickListener listener, int viewType) {
        this.context = context;
        this.listener = listener;
        this.videoList = new ArrayList<>();
        this.viewType = viewType;
    }

    public void setVideoList(List<VideoModel> videos) {
        this.videoList = videos;
        lastPosition = -1;
        notifyDataSetChanged();
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
        lastPosition = -1;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LIST) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_video_list, parent, false);
            return new ListViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false);
            return new GridViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        VideoModel video = videoList.get(position);

        if (holder instanceof GridViewHolder) {
            ((GridViewHolder) holder).bind(video, position);
        } else if (holder instanceof ListViewHolder) {
            ((ListViewHolder) holder).bind(video, position);
        }

        if (position > lastPosition) {
            holder.itemView.startAnimation(
                    AnimationUtils.loadAnimation(context, R.anim.scale_up));
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    // Grid View Holder
    class GridViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView imgThumbnail;
        TextView tvTitle, tvDuration, tvFolder, tvSize, tvQuality;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvFolder = itemView.findViewById(R.id.tvFolder);
            tvSize = itemView.findViewById(R.id.tvSize);
            tvQuality = itemView.findViewById(R.id.tvQuality);
        }

        public void bind(VideoModel video, int position) {
            tvTitle.setText(video.getTitle());
            tvDuration.setText(video.getFormattedDuration());
            tvFolder.setText(video.getFolderName());
            tvSize.setText(video.getFormattedSize());

            String quality = video.getQualityTag();
            if (quality != null) {
                tvQuality.setText(quality);
                tvQuality.setVisibility(View.VISIBLE);
            } else {
                tvQuality.setVisibility(View.GONE);
            }

            Glide.with(context)
                    .load(video.getUri())
                    .transform(new CenterCrop(), new RoundedCorners(24))
                    .placeholder(R.color.surface)
                    .into(imgThumbnail);

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVideoClick(video, position);
                }
            });
        }
    }

    // List View Holder
    class ListViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView imgThumbnail;
        TextView tvTitle, tvDuration, tvFolder, tvSize, tvQuality, tvResolution;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvFolder = itemView.findViewById(R.id.tvFolder);
            tvSize = itemView.findViewById(R.id.tvSize);
            tvQuality = itemView.findViewById(R.id.tvQuality);
            tvResolution = itemView.findViewById(R.id.tvResolution);
        }

        public void bind(VideoModel video, int position) {
            tvTitle.setText(video.getTitle());
            tvDuration.setText(video.getFormattedDuration());
            tvFolder.setText(video.getFolderName());
            tvSize.setText(video.getFormattedSize());
            tvResolution.setText(video.getWidth() + "x" + video.getHeight());

            String quality = video.getQualityTag();
            if (quality != null) {
                tvQuality.setText(quality);
                tvQuality.setVisibility(View.VISIBLE);
            } else {
                tvQuality.setVisibility(View.GONE);
            }

            Glide.with(context)
                    .load(video.getUri())
                    .transform(new CenterCrop(), new RoundedCorners(16))
                    .placeholder(R.color.surface)
                    .into(imgThumbnail);

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVideoClick(video, position);
                }
            });
        }
    }
}