package b.my.audioplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;

public class AudioVisualizerView extends View {

    private static final int MAX_BARS = 32;
    private byte[] waveformData;
    private byte[] fftData;
    private Paint paint;
    private int visualizerMode = MODE_BARS;

    public static final int MODE_BARS = 0;
    public static final int MODE_WAVEFORM = 1;
    public static final int MODE_CIRCULAR = 2;

    private Visualizer visualizer;

    public AudioVisualizerView(Context context) {
        super(context);
        init();
    }

    public AudioVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#6200EE"));
        paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setAudioSessionId(int audioSessionId) {
        releaseVisualizer();

        try {
            visualizer = new Visualizer(audioSessionId);
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    waveformData = waveform;
                    invalidate();
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    fftData = fft;
                    invalidate();
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true);

            visualizer.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setVisualizerMode(int mode) {
        this.visualizerMode = mode;
        invalidate();
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        switch (visualizerMode) {
            case MODE_BARS:
                drawBars(canvas);
                break;
            case MODE_WAVEFORM:
                drawWaveform(canvas);
                break;
            case MODE_CIRCULAR:
                drawCircular(canvas);
                break;
        }
    }

    private void drawBars(Canvas canvas) {
        if (fftData == null) return;

        int width = getWidth();
        int height = getHeight();
        int barWidth = width / MAX_BARS;
        int gap = 2;

        for (int i = 0; i < MAX_BARS; i++) {
            int index = (i * 2) + 2;
            if (index >= fftData.length) break;

            int magnitude = (int) Math.hypot(fftData[index], fftData[index + 1]);
            int barHeight = (int) (magnitude * height / 128f);
            barHeight = Math.min(barHeight, height);

            int left = i * barWidth + gap;
            int top = height - barHeight;
            int right = (i + 1) * barWidth - gap;
            int bottom = height;

            canvas.drawRect(left, top, right, bottom, paint);
        }
    }

    private void drawWaveform(Canvas canvas) {
        if (waveformData == null) return;

        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        paint.setStyle(Paint.Style.STROKE);

        float prevX = 0;
        float prevY = centerY;

        for (int i = 0; i < waveformData.length; i++) {
            float x = (float) i / waveformData.length * width;
            float y = centerY + (waveformData[i] / 128f) * centerY;

            canvas.drawLine(prevX, prevY, x, y, paint);

            prevX = x;
            prevY = y;
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCircular(Canvas canvas) {
        if (fftData == null) return;

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 4;

        int bars = 64;
        double angleStep = 2 * Math.PI / bars;

        for (int i = 0; i < bars; i++) {
            int index = (i * 2) + 2;
            if (index >= fftData.length) break;

            int magnitude = (int) Math.hypot(fftData[index], fftData[index + 1]);
            int barHeight = (int) (magnitude * radius / 128f);

            double angle = i * angleStep - Math.PI / 2;
            float startX = (float) (centerX + radius * Math.cos(angle));
            float startY = (float) (centerY + radius * Math.sin(angle));
            float endX = (float) (centerX + (radius + barHeight) * Math.cos(angle));
            float endY = (float) (centerY + (radius + barHeight) * Math.sin(angle));

            canvas.drawLine(startX, startY, endX, endY, paint);
        }
    }

    public void releaseVisualizer() {
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
                visualizer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            visualizer = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseVisualizer();
    }
}