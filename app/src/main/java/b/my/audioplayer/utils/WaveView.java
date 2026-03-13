package b.my.audioplayer.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class WaveView extends View {

    private Paint paint = new Paint();
    private int barCount = 10;
    private float[] barHeights;
    private boolean isAnimating = false;
    private Random random = new Random();

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setStyle(Paint.Style.FILL);

        LinearGradient gradient = new LinearGradient(
                0, 0, 0, 600,
                new int[]{
                        Color.parseColor("#26FFFFFF"),
                        Color.parseColor("#26FFFF00"),
                        Color.parseColor("#26D0964B")
                },
                new float[]{0f, 0.7f, 1f},
                Shader.TileMode.CLAMP
        );

        paint.setShader(gradient);
        barHeights = new float[barCount];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float barWidth = width / (float) barCount;

        for (int i = 0; i < barCount; i++) {
            if (isAnimating) {
                barHeights[i] = random.nextFloat() * height;
            }

            float left = i * barWidth;
            float top = height - barHeights[i];
            float right = left + barWidth - 10;
            float bottom = height;

            canvas.drawRect(left, top, right, bottom, paint);
        }

        if (isAnimating) {
            postInvalidateDelayed(120);
        }
    }

    public void startAnimation() {
        isAnimating = true;
        invalidate();
    }

    public void stopAnimation() {
        isAnimating = false;
        // FIX: clear all bar heights so no stale frame is left painted
        for (int i = 0; i < barCount; i++) {
            barHeights[i] = 0f;
        }
        invalidate();
    }
}