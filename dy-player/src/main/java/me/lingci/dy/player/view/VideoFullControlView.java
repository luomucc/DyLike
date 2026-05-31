package me.lingci.dy.player.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.lingci.dy.player.R;
import me.lingci.dy.player.databinding.LayoutVideoFullControlViewBinding;
import xyz.doikki.videoplayer.controller.ControlWrapper;
import xyz.doikki.videoplayer.controller.IControlComponent;
import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.util.L;
import xyz.doikki.videoplayer.util.PlayerUtils;

public class VideoFullControlView extends FrameLayout implements IControlComponent {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final LayoutVideoFullControlViewBinding mBinding;

    private ControlWrapper mControlWrapper;
    private final int mScaledTouchSlop;
    private int mStartX, mStartY;

    private boolean mIsDragging = false;
    private final Runnable hideView = () -> {
        onVisibilityChanged(false, null);
    };

    public VideoFullControlView(@NonNull Context context) {
        super(context);
    }

    public VideoFullControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoFullControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        mBinding = LayoutVideoFullControlViewBinding.inflate(LayoutInflater.from(getContext()), this, true);
        setVisibility(GONE);
        mBinding.ivBack.setOnClickListener(v -> {
            mControlWrapper.toggleFullScreen(PlayerUtils.scanForActivity(getContext()));
        });
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mBinding.ivFullExit.setOnClickListener(v -> {
            mControlWrapper.toggleFullScreen(PlayerUtils.scanForActivity(getContext()));
        });
        mBinding.playSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * progress) / seekBar.getMax();
                String progressText = PlayerUtils.stringForTime((int) newPosition) + " / " + PlayerUtils.stringForTime((int) duration);
                mBinding.tvProgress.setText(progressText);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragging = true;
                mControlWrapper.stopProgress();
                mHandler.removeCallbacks(hideView);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long duration = mControlWrapper.getDuration();
                long newPosition = (long) ((double) duration * mBinding.playSeekbar.getProgress() / mBinding.playSeekbar.getMax());
                mControlWrapper.seekTo(newPosition);
                mIsDragging = false;
                mControlWrapper.startProgress();
                mHandler.postDelayed(hideView, 3000);
            }
        });
    }

    public void setTitle(String name) {
        mBinding.tvTitle.setText(name);
    }

    @Override
    public void attach(@NonNull ControlWrapper controlWrapper) {
        mControlWrapper = controlWrapper;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onVisibilityChanged(boolean isVisible, Animation anim) {
        if (isVisible) {
            mBinding.topContainer.setVisibility(VISIBLE);
            mBinding.bottomContainer.setVisibility(VISIBLE);
            mHandler.postDelayed(hideView, 3000);
        } else {
            mBinding.topContainer.setVisibility(GONE);
            mBinding.bottomContainer.setVisibility(GONE);
            mHandler.removeCallbacks(hideView);
        }
        if (anim != null) {
            mBinding.topContainer.startAnimation(anim);
            mBinding.bottomContainer.startAnimation(anim);
        }
    }

    @Override
    public void onPlayStateChanged(int playState) {
        switch (playState) {
            case VideoView.STATE_IDLE:
            case VideoView.STATE_PLAYBACK_COMPLETED:
                setVisibility(GONE);
                break;
            case VideoView.STATE_PLAYING:
                L.e("STATE_PLAYING " + hashCode());
                mBinding.playBtn.setVisibility(GONE);
                mControlWrapper.startProgress();
                break;
            case VideoView.STATE_PAUSED:
                L.e("STATE_PAUSED " + hashCode());
                mBinding.playBtn.setVisibility(VISIBLE);
                break;
            case VideoView.STATE_PREPARED:
                L.e("STATE_PREPARED " + hashCode());
                break;
            case VideoView.STATE_ERROR:
                L.e("STATE_ERROR " + hashCode());
                Toast.makeText(getContext(), me.lingci.lib.player.ui.R.string.dkplayer_error_message, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        switch (playerState) {
            case VideoView.PLAYER_NORMAL:
                setVisibility(GONE);
                onVisibilityChanged(false, null);
                break;
            case VideoView.PLAYER_FULL_SCREEN:
                setVisibility(VISIBLE);
                onVisibilityChanged(false, null);
                break;
        }
    }

    @Override
    public void setProgress(int duration, int position) {
        if (mIsDragging) {
            return;
        }
        if (getVisibility() == GONE) {
            return;
        }
        if (duration > 0) {
            mBinding.playSeekbar.setEnabled(true);
            int pos = (int) (((double) position / duration) * mBinding.playSeekbar.getMax());
            mBinding.playSeekbar.setProgress(pos, true);
            String progressText = PlayerUtils.stringForTime(position) + " / " + PlayerUtils.stringForTime(duration);
            mBinding.tvProgress.setText(progressText);
        } else {
            mBinding.playSeekbar.setEnabled(false);
        }
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {

    }

}
