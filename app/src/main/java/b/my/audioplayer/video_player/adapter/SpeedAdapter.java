package b.my.audioplayer.video_player.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import b.my.audioplayer.R;

public class SpeedAdapter extends RecyclerView.Adapter<SpeedAdapter.SpeedViewHolder> {

    private Context context;
    private float[] speedOptions;
    private int selectedIndex;
    private OnSpeedSelectedListener listener;

    public interface OnSpeedSelectedListener {
        void onSpeedSelected(float speed, int index);
    }

    public SpeedAdapter(Context context, float[] speedOptions, int selectedIndex, OnSpeedSelectedListener listener) {
        this.context = context;
        this.speedOptions = speedOptions;
        this.selectedIndex = selectedIndex;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SpeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_speed_option, parent, false);
        return new SpeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpeedViewHolder holder, int position) {
        float speed = speedOptions[position];
        holder.bind(speed, position);
    }

    @Override
    public int getItemCount() {
        return speedOptions.length;
    }

    class SpeedViewHolder extends RecyclerView.ViewHolder {
        LinearLayout speedItemLayout;
        TextView tvSpeedValue;
        ImageView imgSelected;

        public SpeedViewHolder(@NonNull View itemView) {
            super(itemView);
            speedItemLayout = itemView.findViewById(R.id.speedItemLayout);
            tvSpeedValue = itemView.findViewById(R.id.tvSpeedValue);
            imgSelected = itemView.findViewById(R.id.imgSelected);
        }

        public void bind(float speed, int position) {
            // Format speed text
            String speedText;
            if (speed == 1.0f) {
                speedText = "Normal";
            } else if (speed == (int) speed) {
                speedText = String.format("%.0fx", speed);
            } else {
                speedText = String.format("%.2fx", speed);
            }
            tvSpeedValue.setText(speedText);

            // Show/hide selected indicator
            if (position == selectedIndex) {
                imgSelected.setVisibility(View.VISIBLE);
                tvSpeedValue.setTextColor(context.getResources().getColor(R.color.primary));
            } else {
                imgSelected.setVisibility(View.GONE);
                tvSpeedValue.setTextColor(context.getResources().getColor(R.color.white));
            }

            // Click listener
            speedItemLayout.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSpeedSelected(speed, position);
                }
            });
        }
    }
}