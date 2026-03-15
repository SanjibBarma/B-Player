package b.my.audioplayer.video_player.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import b.my.audioplayer.R;
import b.my.audioplayer.video_player.adapter.SpeedAdapter;

public class VideoPlayerActivity extends AppCompatActivity {

    // Views
    private PlayerView playerView;
    private ExoPlayer player;
    private FrameLayout rootLayout;
    private LinearLayout topControls, centerControls, bottomControls;
    private LinearLayout brightnessLayout, volumeLayout, seekIndicator, lockIndicator;
    private LinearLayout horizontalSeekLayout;
    private ImageButton btnBack, btnPlayPause, btnRewind, btnForward;
    private ImageButton btnLock, btnSpeed, btnRotate, btnScale;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime, tvVideoTitle, tvSpeed;
    private TextView tvBrightnessPercent, tvVolumePercent, tvSeekTime;
    private TextView tvHorizontalSeekTime, tvHorizontalSeekPosition;
    private ProgressBar progressBrightness, progressVolume, loadingProgress;
    private ImageView imgBrightness, imgVolume, imgSeekDirection, imgHorizontalSeekDirection;

    // Speed Popup
    private PopupWindow speedPopupWindow;

    // Variables
    private String videoPath;
    private String videoTitle;
    private Uri videoUri;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarRunnable;
    private Runnable hideControlsRunnable;
    private Runnable hideLockIndicatorRunnable;

    private boolean isControlsVisible = true;
    private boolean isLocked = false;
    private float currentSpeed = 1.0f;
    private float[] speedOptions = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
    private int currentSpeedIndex = 3;

    // Scale/Resize modes
    private int currentScaleIndex = 0;
    private int[] scaleModes = {
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    };
    private String[] scaleNames = {"Fit", "Fill", "Zoom", "100%", "Stretch"};
    private int[] scaleIcons = {
            R.drawable.ic_fit_screen,
            R.drawable.ic_crop_free,
            R.drawable.ic_zoom_out_map,
            R.drawable.ic_aspect_ratio,
            R.drawable.ic_aspect_ratio
    };

    // Gesture
    private GestureDetectorCompat gestureDetector;
    private AudioManager audioManager;
    private int maxVolume;
    private int currentVolume;
    private float currentBrightness;
    private boolean isSwipingBrightness = false;
    private boolean isSwipingVolume = false;
    private boolean isSwipingHorizontal = false;
    private int screenWidth, screenHeight;

    // Horizontal Swipe
    private float swipeStartX;
    private long seekStartPosition;
    private long seekChangeAmount;
    private static final long MAX_SEEK_CHANGE = 120000;

    // Constants
    private static final long SEEK_TIME = 10000;
    private static final int CONTROLS_HIDE_DELAY = 3000;
    private static final int ANIMATION_DURATION = 250;
    private static final int ANIMATION_DURATION_FAST = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        hideSystemUI();

        videoPath = getIntent().getStringExtra("video_path");
        videoTitle = getIntent().getStringExtra("video_title");
        String uriString = getIntent().getStringExtra("video_uri");
        if (uriString != null) {
            videoUri = Uri.parse(uriString);
        }

        initViews();
        initAudioManager();
        initPlayer();
        setupGestureDetector();
        setupListeners();
        setupVideo();
    }

    private void hideSystemUI() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    private void initViews() {
        rootLayout = findViewById(R.id.rootLayout);
        playerView = findViewById(R.id.playerView);

        topControls = findViewById(R.id.topControls);
        centerControls = findViewById(R.id.centerControls);
        bottomControls = findViewById(R.id.bottomControls);

        brightnessLayout = findViewById(R.id.brightnessLayout);
        volumeLayout = findViewById(R.id.volumeLayout);
        seekIndicator = findViewById(R.id.seekIndicator);
        lockIndicator = findViewById(R.id.lockIndicator);
        horizontalSeekLayout = findViewById(R.id.horizontalSeekLayout);

        btnBack = findViewById(R.id.btnBack);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnRewind = findViewById(R.id.btnRewind);
        btnForward = findViewById(R.id.btnForward);
        btnLock = findViewById(R.id.btnLock);
        btnSpeed = findViewById(R.id.btnSpeed);
        btnRotate = findViewById(R.id.btnRotate);
        btnScale = findViewById(R.id.btnScale);

        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvVideoTitle = findViewById(R.id.tvVideoTitle);
        tvSpeed = findViewById(R.id.tvSpeed);

        tvBrightnessPercent = findViewById(R.id.tvBrightnessPercent);
        tvVolumePercent = findViewById(R.id.tvVolumePercent);
        tvSeekTime = findViewById(R.id.tvSeekTime);

        tvHorizontalSeekTime = findViewById(R.id.tvHorizontalSeekTime);
        tvHorizontalSeekPosition = findViewById(R.id.tvHorizontalSeekPosition);
        imgHorizontalSeekDirection = findViewById(R.id.imgHorizontalSeekDirection);

        progressBrightness = findViewById(R.id.progressBrightness);
        progressVolume = findViewById(R.id.progressVolume);
        loadingProgress = findViewById(R.id.loadingProgress);

        imgBrightness = findViewById(R.id.imgBrightness);
        imgVolume = findViewById(R.id.imgVolume);
        imgSeekDirection = findViewById(R.id.imgSeekDirection);

        if (videoTitle != null) {
            tvVideoTitle.setText(videoTitle);
        }

        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
    }

    private void initAudioManager() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        try {
            currentBrightness = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS) / 255f;
        } catch (Settings.SettingNotFoundException e) {
            currentBrightness = 0.5f;
        }
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        loadingProgress.setVisibility(View.VISIBLE);
                        break;
                    case Player.STATE_READY:
                        loadingProgress.setVisibility(View.GONE);
                        long duration = player.getDuration();
                        seekBar.setMax((int) duration);
                        tvTotalTime.setText(formatTime(duration));
                        updatePlayPauseButton();
                        startSeekBarUpdate();
                        scheduleHideControls();
                        break;
                    case Player.STATE_ENDED:
                        // Video ended - show replay icon and controls
                        btnPlayPause.setImageResource(R.drawable.ic_replay);
                        showControls();
                        handler.removeCallbacks(hideControlsRunnable); // Don't auto-hide
                        break;
                    case Player.STATE_IDLE:
                        break;
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseButton();
            }
        });
    }


    private void togglePlayPause() {
        if (player == null) return;

        // Check if video ended - replay from start
        if (player.getPlaybackState() == Player.STATE_ENDED) {
            player.seekTo(0);
            player.play();
            updatePlayPauseButton();
            scheduleHideControls();
            return;
        }

        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        updatePlayPauseButton();
        scheduleHideControls();
    }

    private void updatePlayPauseButton() {
        if (player == null) return;

        int iconRes;

        // Check if video ended
        if (player.getPlaybackState() == Player.STATE_ENDED) {
            iconRes = R.drawable.ic_replay;
        } else {
            iconRes = player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play;
        }

        btnPlayPause.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .alpha(0.5f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    btnPlayPause.setImageResource(iconRes);
                    btnPlayPause.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator(1.5f))
                            .start();
                })
                .start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(this, new GestureListener());

        rootLayout.setOnTouchListener((v, event) -> {
            if (isLocked) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showLockIndicator();
                }
                return true;
            }

            gestureDetector.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    swipeStartX = event.getX();
                    if (player != null) {
                        seekStartPosition = player.getCurrentPosition();
                    }
                    seekChangeAmount = 0;
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isSwipingHorizontal) {
                        applyHorizontalSeek();
                    }
                    hideAllIndicators();
                    isSwipingBrightness = false;
                    isSwipingVolume = false;
                    isSwipingHorizontal = false;
                    break;
            }

            return true;
        });
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 50;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            dismissSpeedPopup();
            toggleControls();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float x = e.getX();
            if (x < screenWidth / 3f) {
                seekBackward();
                showSeekIndicator(false);
            } else if (x > screenWidth * 2f / 3f) {
                seekForward();
                showSeekIndicator(true);
            } else {
                togglePlayPause();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null) return false;

            float deltaY = e1.getY() - e2.getY();
            float deltaX = e2.getX() - e1.getX();
            float absDeltaX = Math.abs(deltaX);
            float absDeltaY = Math.abs(deltaY);

            if (!isSwipingBrightness && !isSwipingVolume && !isSwipingHorizontal) {
                if (absDeltaX > absDeltaY && absDeltaX > SWIPE_THRESHOLD) {
                    isSwipingHorizontal = true;
                    hideControls();
                } else if (absDeltaY > absDeltaX && absDeltaY > SWIPE_THRESHOLD) {
                    if (e1.getX() < screenWidth / 2f) {
                        isSwipingBrightness = true;
                    } else {
                        isSwipingVolume = true;
                    }
                }
            }

            if (isSwipingHorizontal) {
                handleHorizontalSwipe(deltaX);
            } else if (isSwipingBrightness) {
                adjustBrightness(deltaY);
            } else if (isSwipingVolume) {
                adjustVolume(deltaY);
            }

            return true;
        }
    }

    private void handleHorizontalSwipe(float deltaX) {
        if (player == null) return;

        long duration = player.getDuration();
        if (duration <= 0) return;

        float seekRatio = deltaX / screenWidth;
        seekChangeAmount = (long) (seekRatio * MAX_SEEK_CHANGE);

        long newPosition = seekStartPosition + seekChangeAmount;
        newPosition = Math.max(0, Math.min(duration, newPosition));
        seekChangeAmount = newPosition - seekStartPosition;

        showHorizontalSeekIndicator();
    }

    private void showHorizontalSeekIndicator() {
        if (player == null) return;

        long newPosition = seekStartPosition + seekChangeAmount;
        long duration = player.getDuration();
        newPosition = Math.max(0, Math.min(duration, newPosition));

        if (seekChangeAmount >= 0) {
            imgHorizontalSeekDirection.setImageResource(R.drawable.ic_forward);
        } else {
            imgHorizontalSeekDirection.setImageResource(R.drawable.ic_rewind);
        }

        long absChange = Math.abs(seekChangeAmount);
        String changeText = (seekChangeAmount >= 0 ? "+" : "-") + formatTime(absChange);
        tvHorizontalSeekTime.setText(changeText);

        tvHorizontalSeekPosition.setText(formatTime(newPosition) + " / " + formatTime(duration));

        if (horizontalSeekLayout.getVisibility() != View.VISIBLE) {
            horizontalSeekLayout.setVisibility(View.VISIBLE);
            horizontalSeekLayout.setAlpha(0f);
            horizontalSeekLayout.setScaleX(0.8f);
            horizontalSeekLayout.setScaleY(0.8f);
            horizontalSeekLayout.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIMATION_DURATION_FAST)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .setListener(null)
                    .start();
        }
    }

    private void applyHorizontalSeek() {
        if (player == null || seekChangeAmount == 0) return;

        long newPosition = seekStartPosition + seekChangeAmount;
        long duration = player.getDuration();
        newPosition = Math.max(0, Math.min(duration, newPosition));

        player.seekTo(newPosition);
        seekBar.setProgress((int) newPosition);
        tvCurrentTime.setText(formatTime(newPosition));
    }

    private void hideAllIndicators() {
        hideIndicatorWithAnimation(brightnessLayout);
        hideIndicatorWithAnimation(volumeLayout);
        hideIndicatorWithAnimation(horizontalSeekLayout);
    }

    private void hideIndicatorWithAnimation(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(ANIMATION_DURATION_FAST)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            view.setVisibility(View.GONE);
                            view.setAlpha(1f);
                            view.setScaleX(1f);
                            view.setScaleY(1f);
                        }
                    })
                    .start();
        }
    }

    private void adjustBrightness(float deltaY) {
        float change = deltaY / (screenHeight * 0.8f);
        currentBrightness = Math.max(0.01f, Math.min(1f, currentBrightness + change));

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = currentBrightness;
        getWindow().setAttributes(layoutParams);

        int percent = (int) (currentBrightness * 100);
        animateProgressBar(progressBrightness, percent);
        tvBrightnessPercent.setText(percent + "%");

        if (percent < 30) {
            imgBrightness.setImageResource(R.drawable.ic_brightness_low);
        } else if (percent < 70) {
            imgBrightness.setImageResource(R.drawable.ic_brightness);
        } else {
            imgBrightness.setImageResource(R.drawable.ic_brightness_high);
        }

        showIndicatorWithAnimation(brightnessLayout);
    }

    private void adjustVolume(float deltaY) {
        // Always sync from system to avoid jumps
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float change = deltaY / (screenHeight * 0.8f) * maxVolume;
        currentVolume = Math.max(0, Math.min(maxVolume, (int) (currentVolume + change)));

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);

        int percent = (int) ((float) currentVolume / maxVolume * 100);
        animateProgressBar(progressVolume, percent);
        tvVolumePercent.setText(percent + "%");

        if (currentVolume == 0) {
            imgVolume.setImageResource(R.drawable.ic_volume_off);
        } else if (percent < 50) {
            imgVolume.setImageResource(R.drawable.ic_volume_low);
        } else {
            imgVolume.setImageResource(R.drawable.ic_volume);
        }

        showIndicatorWithAnimation(volumeLayout);
    }

    private void animateProgressBar(ProgressBar progressBar, int toValue) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), toValue);
        animator.setDuration(100);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void showIndicatorWithAnimation(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
            view.setAlpha(0f);
            view.setScaleX(0.8f);
            view.setScaleY(0.8f);
            view.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIMATION_DURATION_FAST)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .setListener(null)
                    .start();
        }
    }

    private void showSeekIndicator(boolean isForward) {
        imgSeekDirection.setImageResource(isForward ? R.drawable.ic_forward : R.drawable.ic_rewind);
        tvSeekTime.setText("10s");

        seekIndicator.setVisibility(View.VISIBLE);
        seekIndicator.setAlpha(0f);
        seekIndicator.setScaleX(0.5f);
        seekIndicator.setScaleY(0.5f);

        seekIndicator.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ANIMATION_DURATION_FAST)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        handler.postDelayed(() -> {
                            seekIndicator.animate()
                                    .alpha(0f)
                                    .scaleX(0.5f)
                                    .scaleY(0.5f)
                                    .setDuration(ANIMATION_DURATION_FAST)
                                    .setInterpolator(new FastOutSlowInInterpolator())
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            seekIndicator.setVisibility(View.GONE);
                                        }
                                    })
                                    .start();
                        }, 400);
                    }
                })
                .start();
    }

    private void showLockIndicator() {
        handler.removeCallbacks(hideLockIndicatorRunnable);

        lockIndicator.setVisibility(View.VISIBLE);
        lockIndicator.setAlpha(0f);
        lockIndicator.setScaleX(0.8f);
        lockIndicator.setScaleY(0.8f);

        lockIndicator.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideLockIndicatorRunnable = () -> hideLockIndicator();
                        handler.postDelayed(hideLockIndicatorRunnable, 2000);
                    }
                })
                .start();
    }

    private void hideLockIndicator() {
        lockIndicator.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        lockIndicator.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void unlockControls() {
        isLocked = false;
        handler.removeCallbacks(hideLockIndicatorRunnable);

        lockIndicator.animate()
                .alpha(0f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(ANIMATION_DURATION_FAST)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        lockIndicator.setVisibility(View.GONE);
                        lockIndicator.setScaleX(1f);
                        lockIndicator.setScaleY(1f);
                    }
                })
                .start();

        btnLock.setImageResource(R.drawable.ic_lock_open);
        showControls();
        Toast.makeText(this, "Controls Unlocked", Toast.LENGTH_SHORT).show();
    }

    // ==================== SCALE/RESIZE ====================

    private void cycleScale() {
        currentScaleIndex = (currentScaleIndex + 1) % scaleModes.length;

        // Apply resize mode
        playerView.setResizeMode(scaleModes[currentScaleIndex]);

        // Update button icon
        btnScale.setImageResource(scaleIcons[currentScaleIndex]);

        // Show toast
        Toast.makeText(this, scaleNames[currentScaleIndex], Toast.LENGTH_SHORT).show();

        scheduleHideControls();
    }

    // ==================================================

    // ==================== SPEED POPUP ====================

    private void showSpeedPopup() {
        handler.removeCallbacks(hideControlsRunnable);

        View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_speed_selector, null);
        RecyclerView rvSpeedOptions = popupView.findViewById(R.id.rvSpeedOptions);

        rvSpeedOptions.setLayoutManager(new LinearLayoutManager(this));
        SpeedAdapter adapter = new SpeedAdapter(this, speedOptions, currentSpeedIndex, (speed, index) -> {
            setPlaybackSpeed(speed, index);
            dismissSpeedPopup();
        });
        rvSpeedOptions.setAdapter(adapter);

        speedPopupWindow = new PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        speedPopupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        speedPopupWindow.setElevation(16f);
        speedPopupWindow.setAnimationStyle(R.style.PopupAnimation);

        speedPopupWindow.setOnDismissListener(this::scheduleHideControls);

        int[] location = new int[2];
        btnSpeed.getLocationOnScreen(location);

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();

        int xOffset = location[0] + (btnSpeed.getWidth() / 2) - (popupWidth / 2);
        int yOffset = location[1] + btnSpeed.getHeight() + 8;

        speedPopupWindow.showAtLocation(rootLayout, Gravity.NO_GRAVITY, xOffset, yOffset);
    }

    private void dismissSpeedPopup() {
        if (speedPopupWindow != null && speedPopupWindow.isShowing()) {
            speedPopupWindow.dismiss();
            speedPopupWindow = null;
        }
    }

    private void setPlaybackSpeed(float speed, int index) {
        currentSpeed = speed;
        currentSpeedIndex = index;

        if (player != null) {
            player.setPlaybackParameters(new PlaybackParameters(speed));
        }

        if (currentSpeed == 1.0f) {
            tvSpeed.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .setDuration(ANIMATION_DURATION_FAST)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            tvSpeed.setVisibility(View.GONE);
                        }
                    })
                    .start();
        } else {
            String speedText = (speed == (int) speed)
                    ? String.format("%.0fx", speed)
                    : String.format("%.2fx", speed).replaceAll("0+x$", "x");
            tvSpeed.setText(speedText);
            if (tvSpeed.getVisibility() != View.VISIBLE) {
                tvSpeed.setVisibility(View.VISIBLE);
                tvSpeed.setAlpha(0f);
                tvSpeed.setScaleX(0.8f);
            }
            tvSpeed.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .setDuration(ANIMATION_DURATION_FAST)
                    .setListener(null)
                    .start();
        }

        scheduleHideControls();
    }

    // ==================================================

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            animateButtonClick(v);
            finish();
        });

        btnPlayPause.setOnClickListener(v -> {
            animateButtonClick(v);
            togglePlayPause();
        });

        btnRewind.setOnClickListener(v -> {
            animateButtonClick(v);
            seekBackward();
            showSeekIndicator(false);
        });

        btnForward.setOnClickListener(v -> {
            animateButtonClick(v);
            seekForward();
            showSeekIndicator(true);
        });

        btnLock.setOnClickListener(v -> {
            animateButtonClick(v);
            toggleLock();
        });

        btnSpeed.setOnClickListener(v -> {
            animateButtonClick(v);
            showSpeedPopup();
        });

        btnRotate.setOnClickListener(v -> {
            animateButtonClick(v);
            toggleOrientation();
        });

        // Scale button listener
        btnScale.setOnClickListener(v -> {
            animateButtonClick(v);
            cycleScale();
        });

        lockIndicator.setOnClickListener(v -> unlockControls());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(hideControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleHideControls();
            }
        });
    }

    private void animateButtonClick(View view) {
        view.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(80)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .setInterpolator(new OvershootInterpolator(2f))
                            .start();
                })
                .start();
    }

    private void setupVideo() {
        loadingProgress.setVisibility(View.VISIBLE);

        Uri playUri = videoUri;
        if (playUri == null && videoPath != null && !videoPath.isEmpty()) {
            playUri = Uri.parse(videoPath);
        }

        if (playUri == null) {
            Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MediaItem mediaItem = MediaItem.fromUri(playUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void seekForward() {
        if (player == null) return;
        long newPosition = Math.min(player.getCurrentPosition() + SEEK_TIME, player.getDuration());
        player.seekTo(newPosition);
        seekBar.setProgress((int) newPosition);
    }

    private void seekBackward() {
        if (player == null) return;
        long newPosition = Math.max(player.getCurrentPosition() - SEEK_TIME, 0);
        player.seekTo(newPosition);
        seekBar.setProgress((int) newPosition);
    }

    private void toggleLock() {
        isLocked = !isLocked;

        if (isLocked) {
            btnLock.setImageResource(R.drawable.ic_lock);
            hideControlsSmooth();
            Toast.makeText(this, "Controls Locked", Toast.LENGTH_SHORT).show();
        } else {
            btnLock.setImageResource(R.drawable.ic_lock_open);
            showControls();
            Toast.makeText(this, "Controls Unlocked", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;

        if (currentOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    private void toggleControls() {
        if (isControlsVisible) {
            hideControlsSmooth();
        } else {
            showControls();
        }
    }

    private void showControls() {
        isControlsVisible = true;

        topControls.setVisibility(View.VISIBLE);
        topControls.setAlpha(0f);
        topControls.setTranslationY(-60);
        topControls.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(null)
                .start();

        centerControls.setVisibility(View.VISIBLE);
        centerControls.setAlpha(0f);
        centerControls.setScaleX(0.7f);
        centerControls.setScaleY(0.7f);
        centerControls.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .setListener(null)
                .start();

        bottomControls.setVisibility(View.VISIBLE);
        bottomControls.setAlpha(0f);
        bottomControls.setTranslationY(60);
        bottomControls.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(null)
                .start();

        scheduleHideControls();
    }

    private void hideControls() {
        isControlsVisible = false;
        handler.removeCallbacks(hideControlsRunnable);

        topControls.setVisibility(View.GONE);
        centerControls.setVisibility(View.GONE);
        bottomControls.setVisibility(View.GONE);
    }

    private void hideControlsSmooth() {
        isControlsVisible = false;
        handler.removeCallbacks(hideControlsRunnable);

        topControls.animate()
                .alpha(0f)
                .translationY(-60)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        topControls.setVisibility(View.GONE);
                    }
                })
                .start();

        centerControls.animate()
                .alpha(0f)
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        centerControls.setVisibility(View.GONE);
                    }
                })
                .start();

        bottomControls.animate()
                .alpha(0f)
                .translationY(60)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        bottomControls.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable);

        hideControlsRunnable = () -> {
            if (player != null && player.isPlaying() && !isLocked) {
                hideControlsSmooth();
            }
        };

        handler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_DELAY);
    }

    private void startSeekBarUpdate() {
        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null) {
                    long currentPosition = player.getCurrentPosition();
                    seekBar.setProgress((int) currentPosition);
                    tvCurrentTime.setText(formatTime(currentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateSeekBarRunnable);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public void onBackPressed() {
        dismissSpeedPopup();
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissSpeedPopup();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissSpeedPopup();
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}