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
import b.my.audioplayer.video_player.model.FolderModel;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private Context context;
    private List<FolderModel> folderList;
    private OnFolderClickListener listener;
    private int lastPosition = -1;

    public interface OnFolderClickListener {
        void onFolderClick(FolderModel folder);
    }

    public FolderAdapter(Context context, OnFolderClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.folderList = new ArrayList<>();
    }

    public void setFolderList(List<FolderModel> folders) {
        this.folderList = folders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        FolderModel folder = folderList.get(position);
        holder.bind(folder);

        if (position > lastPosition) {
            holder.itemView.startAnimation(
                    AnimationUtils.loadAnimation(context, R.anim.scale_up));
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull FolderViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    class FolderViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView imgThumbnail, imgFolder;
        TextView tvFolderName, tvVideoCount;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            imgFolder = itemView.findViewById(R.id.imgFolder);
            tvFolderName = itemView.findViewById(R.id.tvFolderName);
            tvVideoCount = itemView.findViewById(R.id.tvVideoCount);
        }

        public void bind(FolderModel folder) {
            tvFolderName.setText(folder.getName());
            tvVideoCount.setText(folder.getVideoCountText());

            Glide.with(context)
                    .load(folder.getThumbnailUri())
                    .transform(new CenterCrop(), new RoundedCorners(24))
                    .placeholder(R.color.surface)
                    .into(imgThumbnail);

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFolderClick(folder);
                }
            });
        }
    }
}