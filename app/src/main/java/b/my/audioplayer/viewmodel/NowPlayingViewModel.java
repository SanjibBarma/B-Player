package b.my.audioplayer.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.Player;
import b.my.audioplayer.model.Song;

public class NowPlayingViewModel extends AndroidViewModel {

    private MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private MutableLiveData<Long> currentPosition = new MutableLiveData<>(0L);
    private MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private MutableLiveData<Boolean> isShuffleEnabled = new MutableLiveData<>(false);
    private MutableLiveData<Integer> repeatMode = new MutableLiveData<>(Player.REPEAT_MODE_OFF);

    public NowPlayingViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Song> getCurrentSong() {
        return currentSong;
    }

    public void setCurrentSong(Song song) {
        currentSong.setValue(song);
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean playing) {
        isPlaying.setValue(playing);
    }

    public LiveData<Long> getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(long position) {
        currentPosition.setValue(position);
    }

    public LiveData<Long> getDuration() {
        return duration;
    }

    public void setDuration(long dur) {
        duration.setValue(dur);
    }

    public LiveData<Boolean> getIsShuffleEnabled() {
        return isShuffleEnabled;
    }

    public void toggleShuffle() {
        Boolean current = isShuffleEnabled.getValue();
        isShuffleEnabled.setValue(current != null ? !current : true);
    }

    public LiveData<Integer> getRepeatMode() {
        return repeatMode;
    }

    public void cycleRepeatMode() {
        Integer current = repeatMode.getValue();
        if (current == null) current = Player.REPEAT_MODE_OFF;

        int nextMode;
        switch (current) {
            case Player.REPEAT_MODE_OFF:
                nextMode = Player.REPEAT_MODE_ALL;
                break;
            case Player.REPEAT_MODE_ALL:
                nextMode = Player.REPEAT_MODE_ONE;
                break;
            case Player.REPEAT_MODE_ONE:
                nextMode = Player.REPEAT_MODE_OFF;
                break;
            default:
                nextMode = Player.REPEAT_MODE_OFF;
        }
        repeatMode.setValue(nextMode);
    }
}