package xyz.doikki.videocontroller.component;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.lingci.lib.base.util.Log;
import me.lingci.lib.player.danmaku.PlayerInitializer;
import me.lingci.lib.player.ui.R;
import me.lingci.lib.player.ui.databinding.DkplayerLayoutGestureControlViewBinding;
import xyz.doikki.videoplayer.controller.ControlWrapper;
import xyz.doikki.videoplayer.controller.IGestureComponent;
import xyz.doikki.videoplayer.player.VideoView;

import static xyz.doikki.videoplayer.util.PlayerUtils.stringForTime;

/**
 * 手势控制
 * <a href="https://developer.android.google.cn/develop/ui/views/touch-and-input/gestures/scroll?hl=zh-cn">...</a>
 * <a href="https://developer.android.google.cn/develop/ui/views/touch-and-input/gestures/scale?hl=zh-cn">...</a>
 */
public class GestureView extends FrameLayout implements IGestureComponent {

    public GestureView(@NonNull Context context) {
        super(context);
    }

    public GestureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GestureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private ControlWrapper mControlWrapper;

    private final DkplayerLayoutGestureControlViewBinding mBinding;


    {
        setVisibility(GONE);
        mBinding = DkplayerLayoutGestureControlViewBinding.inflate(LayoutInflater.from(getContext()), this, true);
        //LayoutInflater.from(getContext()).inflate(R.layout.dkplayer_layout_gesture_control_view, this, true);
        // 延迟100ms启动（确保视图完全加载）
        mBinding.ivSpeed.postDelayed(() -> {
            Drawable drawable = mBinding.ivSpeed.getDrawable();
            if (drawable instanceof Animatable && !((Animatable)drawable).isRunning()) {
                ((Animatable)drawable).start();
            }
        }, 100);
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

    }

    @Override
    public void onPlayerStateChanged(int playerState) {

    }

    @Override
    public void onStartSlide() {
        mControlWrapper.hide();
        mBinding.centerContainer.setVisibility(VISIBLE);
        mBinding.centerContainer.setAlpha(1f);
    }

    @Override
    public void onStopSlide() {
        mBinding.centerContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mBinding.centerContainer.setVisibility(GONE);
                    }
                })
                .start();
    }

    @Override
    public void onPositionChange(int slidePosition, int currentPosition, int duration) {
        mBinding.proPercent.setVisibility(GONE);
        if (slidePosition > currentPosition) {
            mBinding.ivIcon.setImageResource(R.drawable.ic_fast_forward_24);
        } else {
            mBinding.ivIcon.setImageResource(R.drawable.ic_fast_rewind_24);
        }
        mBinding.tvPercent.setText(String.format("%s/%s", stringForTime(slidePosition), stringForTime(duration)));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBrightnessChange(int percent) {
        mBinding.proPercent.setVisibility(VISIBLE);
        mBinding.ivIcon.setImageResource(R.drawable.ic_brightness_medium_24);
        mBinding.tvPercent.setText(percent + "%");
        mBinding.proPercent.setProgress(percent);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onVolumeChange(int percent) {
        mBinding.proPercent.setVisibility(VISIBLE);
        if (percent <= 0) {
            mBinding.ivIcon.setImageResource(R.drawable.ic_volume_off_24);
        } else {
            mBinding.ivIcon.setImageResource(R.drawable.ic_volume_up_24);
        }
        mBinding.tvPercent.setText(percent + "%");
        mBinding.proPercent.setProgress(percent);
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onStartAccelerate() {
        Log.d(this, PlayerInitializer.Player.INSTANCE.getVideoSpeed(), PlayerInitializer.Player.INSTANCE.getPressVideoSpeed());
        if (PlayerInitializer.Player.INSTANCE.getVideoSpeed() == PlayerInitializer.Player.INSTANCE.getPressVideoSpeed()) {
            return;
        }
        mBinding.centerContainer.setVisibility(GONE);
        mBinding.accelerateLl.setVisibility(VISIBLE);
        mBinding.accelerateLl.setAlpha(1f);


        mControlWrapper.setSpeed(PlayerInitializer.Player.INSTANCE.getPressVideoSpeed());

        mBinding.accelerateTv.setText(mControlWrapper.getSpeed() + "倍速播放中");
    }

    @Override
    public void onStopAccelerate() {
        mBinding.accelerateLl.animate()
                .alpha(0f)
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mBinding.accelerateLl.setVisibility(GONE);
                    }
                })
                .start();
    }

    @Override
    public void onPlayStateChanged(int playState) {
        if (playState == VideoView.STATE_IDLE
                || playState == VideoView.STATE_START_ABORT
                || playState == VideoView.STATE_PREPARING
                || playState == VideoView.STATE_PREPARED
                || playState == VideoView.STATE_ERROR
                || playState == VideoView.STATE_PLAYBACK_COMPLETED) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    @Override
    public void setProgress(int duration, int position) {

    }

    @Override
    public void onLockStateChanged(boolean isLock) {

    }

}