package b.my.audioplayer.video_player.activity;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import b.my.audioplayer.R;
import b.my.audioplayer.video_player.adapter.FolderAdapter;
import b.my.audioplayer.video_player.model.FolderModel;
import b.my.audioplayer.video_player.adapter.VideoAdapter;
import b.my.audioplayer.video_player.model.VideoModel;

public class VideoLibraryActivity extends AppCompatActivity implements
        VideoAdapter.OnVideoClickListener,
        FolderAdapter.OnFolderClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String PREF_NAME = "video_library_prefs";
    private static final String PREF_VIEW_TYPE = "view_type";
    private static final int VIEW_TYPE_GRID = 0;
    private static final int VIEW_TYPE_LIST = 1;

    private RecyclerView recyclerView, folderRecyclerView;
    private VideoAdapter videoAdapter;
    private FolderAdapter folderAdapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout loadingLayout, emptyLayout, permissionLayout;
    private TextView tvVideoCount, tvCurrentFolder;
    private MaterialButton btnGrantPermission;
    private ImageButton btnViewToggle, btnBack;
    private TabLayout tabLayout;

    private ExecutorService executorService;
    private List<VideoModel> allVideoList = new ArrayList<>();
    private List<VideoModel> currentVideoList = new ArrayList<>();
    private Map<String, List<VideoModel>> folderMap = new HashMap<>();
    private List<FolderModel> folderList = new ArrayList<>();

    private int currentViewType = VIEW_TYPE_GRID;
    private String currentFolder = null; // null means showing all or folders
    private boolean showingFolders = true;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_library);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentViewType = prefs.getInt(PREF_VIEW_TYPE, VIEW_TYPE_GRID);

        initViews();
        setupRecyclerView();
        setupListeners();

        executorService = Executors.newSingleThreadExecutor();

        checkPermissionAndLoad();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        folderRecyclerView = findViewById(R.id.folderRecyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        loadingLayout = findViewById(R.id.loadingLayout);
        emptyLayout = findViewById(R.id.emptyLayout);
        permissionLayout = findViewById(R.id.permissionLayout);
        tvVideoCount = findViewById(R.id.tvVideoCount);
        tvCurrentFolder = findViewById(R.id.tvCurrentFolder);
        btnGrantPermission = findViewById(R.id.btnGrantPermission);
        btnViewToggle = findViewById(R.id.btnViewToggle);
        btnBack = findViewById(R.id.btnBack);
        tabLayout = findViewById(R.id.tabLayout);

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.background_card);

        updateViewToggleIcon();
        btnBack.setVisibility(View.GONE);
        tvCurrentFolder.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        // Video adapter
        videoAdapter = new VideoAdapter(this, this, currentViewType);
        updateLayoutManager();
        recyclerView.setAdapter(videoAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        // Folder adapter
        folderAdapter = new FolderAdapter(this, this);
        folderRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        folderRecyclerView.setAdapter(folderAdapter);
        folderRecyclerView.setHasFixedSize(true);
    }

    private void updateLayoutManager() {
        if (currentViewType == VIEW_TYPE_GRID) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            recyclerView.setLayoutManager(gridLayoutManager);
        } else {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(linearLayoutManager);
        }
        videoAdapter.setViewType(currentViewType);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadVideos);
        btnGrantPermission.setOnClickListener(v -> requestPermission());

        btnViewToggle.setOnClickListener(v -> {
            currentViewType = (currentViewType == VIEW_TYPE_GRID) ? VIEW_TYPE_LIST : VIEW_TYPE_GRID;
            prefs.edit().putInt(PREF_VIEW_TYPE, currentViewType).apply();
            updateViewToggleIcon();
            updateLayoutManager();
        });

        btnBack.setOnClickListener(v -> {
            showFolders();
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Folders tab
                    showFolders();
                } else {
                    // All Videos tab
                    showAllVideos();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void updateViewToggleIcon() {
        if (currentViewType == VIEW_TYPE_GRID) {
            btnViewToggle.setImageResource(R.drawable.ic_view_list);
        } else {
            btnViewToggle.setImageResource(R.drawable.ic_view_grid);
        }
    }

    private void showFolders() {
        showingFolders = true;
        currentFolder = null;
        btnBack.setVisibility(View.GONE);
        tvCurrentFolder.setVisibility(View.GONE);
        folderRecyclerView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvVideoCount.setText(folderList.size() + " Folders");
    }

    private void showAllVideos() {
        showingFolders = false;
        currentFolder = null;
        btnBack.setVisibility(View.GONE);
        tvCurrentFolder.setVisibility(View.GONE);
        folderRecyclerView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        videoAdapter.setVideoList(allVideoList);
        tvVideoCount.setText(allVideoList.size() + " Videos");
    }

    private void showFolderVideos(String folderName) {
        showingFolders = false;
        currentFolder = folderName;
        btnBack.setVisibility(View.VISIBLE);
        tvCurrentFolder.setVisibility(View.VISIBLE);
        tvCurrentFolder.setText(folderName);
        folderRecyclerView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        List<VideoModel> videos = folderMap.get(folderName);
        if (videos != null) {
            videoAdapter.setVideoList(videos);
            tvVideoCount.setText(videos.size() + " Videos");
        }
    }

    private void checkPermissionAndLoad() {
        if (hasPermission()) {
            loadVideos();
        } else {
            showPermissionLayout();
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadVideos();
            } else {
                showPermissionLayout();
            }
        }
    }

    private void loadVideos() {
        showLoading();

        executorService.execute(() -> {
            List<VideoModel> videos = fetchVideosFromDevice();
            Map<String, List<VideoModel>> folders = organizeFolders(videos);
            List<FolderModel> folderModels = createFolderModels(folders);

            runOnUiThread(() -> {
                allVideoList = videos;
                folderMap = folders;
                folderList = folderModels;

                folderAdapter.setFolderList(folderModels);
                swipeRefresh.setRefreshing(false);

                if (videos.isEmpty()) {
                    showEmpty();
                } else {
                    showContent();
                    if (tabLayout.getSelectedTabPosition() == 0) {
                        showFolders();
                    } else {
                        showAllVideos();
                    }
                }
            });
        });
    }

    private Map<String, List<VideoModel>> organizeFolders(List<VideoModel> videos) {
        Map<String, List<VideoModel>> folders = new HashMap<>();
        for (VideoModel video : videos) {
            String folder = video.getFolderName();
            if (!folders.containsKey(folder)) {
                folders.put(folder, new ArrayList<>());
            }
            folders.get(folder).add(video);
        }
        return folders;
    }

    private List<FolderModel> createFolderModels(Map<String, List<VideoModel>> folders) {
        List<FolderModel> models = new ArrayList<>();
        for (Map.Entry<String, List<VideoModel>> entry : folders.entrySet()) {
            String name = entry.getKey();
            List<VideoModel> videos = entry.getValue();
            Uri thumbnail = videos.get(0).getUri();
            models.add(new FolderModel(name, videos.size(), thumbnail));
        }
        return models;
    }

    private List<VideoModel> fetchVideosFromDevice() {
        List<VideoModel> videos = new ArrayList<>();

        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        };

        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(
                collection, projection, null, null, sortOrder)) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
                int heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);
                int folderColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String path = cursor.getString(pathColumn);
                    long duration = cursor.getLong(durationColumn);
                    long size = cursor.getLong(sizeColumn);
                    int width = cursor.getInt(widthColumn);
                    int height = cursor.getInt(heightColumn);
                    String folder = cursor.getString(folderColumn);

                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                    if (title != null && title.contains(".")) {
                        title = title.substring(0, title.lastIndexOf('.'));
                    }

                    if (duration <= 0) continue;

                    VideoModel video = new VideoModel(
                            id, title, path, duration, size,
                            folder != null ? folder : "Unknown",
                            width, height, contentUri);

                    videos.add(video);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return videos;
    }

    private void showLoading() {
        loadingLayout.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);
        permissionLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        folderRecyclerView.setVisibility(View.GONE);
    }

    private void showEmpty() {
        loadingLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
        permissionLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        folderRecyclerView.setVisibility(View.GONE);
    }

    private void showPermissionLayout() {
        loadingLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);
        permissionLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        folderRecyclerView.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);
        permissionLayout.setVisibility(View.GONE);
    }

    @Override
    public void onVideoClick(VideoModel video, int position) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("video_path", video.getPath());
        intent.putExtra("video_title", video.getTitle());
        intent.putExtra("video_uri", video.getUri().toString());
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onFolderClick(FolderModel folder) {
        showFolderVideos(folder.getName());
    }

    @Override
    public void onBackPressed() {
        if (currentFolder != null) {
            showFolders();
            tabLayout.selectTab(tabLayout.getTabAt(0));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}