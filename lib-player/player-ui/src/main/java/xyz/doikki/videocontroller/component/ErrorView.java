package xyz.doikki.videocontroller.component;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.lingci.lib.player.ui.databinding.DkplayerLayoutErrorViewBinding;
import xyz.doikki.videoplayer.controller.ControlWrapper;
import xyz.doikki.videoplayer.controller.IControlComponent;
import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.util.PlayerUtils;

/**
 * 播放出错提示界面
 * Created by Doikki on 2017/4/13.
 */
public class ErrorView extends LinearLayout implements IControlComponent {

    private float mDownX;
    private float mDownY;

    private ControlWrapper mControlWrapper;

    @SuppressWarnings("FieldCanBeLocal")
    private final DkplayerLayoutErrorViewBinding mBinding;

    public ErrorView(Context context) {
        this(context, null);
    }

    public ErrorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ErrorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        setVisibility(GONE);
        mBinding = DkplayerLayoutErrorViewBinding.inflate(LayoutInflater.from(getContext()), this, true);
        //LayoutInflater.from(getContext()).inflate(R.layout.dkplayer_layout_error_view, this, true);
        mBinding.stopFullscreen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = PlayerUtils.scanForActivity(getContext());
                if (activity != null && !activity.isFinishing()) {
                    activity.finish();
                }
            }
        });
        mBinding.statusBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setVisibility(GONE);
                mControlWrapper.replay(false);
            }
        });
        setClickable(true);
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
    public void onPlayStateChanged(int playState) {
        if (playState == VideoView.STATE_ERROR) {
            bringToFront();
            setVisibility(VISIBLE);
        } else if (playState == VideoView.STATE_IDLE) {
            setVisibility(GONE);
        }
    }

    @Override
    public void onPlayerStateChanged(int playerState) {

    }

    @Override
    public void setProgress(int duration, int position) {

    }

    @Override
    public void onLockStateChanged(boolean isLock) {

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = ev.getX();
                mDownY = ev.getY();
                // True if the child does not want the parent to intercept touch events.
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float absDeltaX = Math.abs(ev.getX() - mDownX);
                float absDeltaY = Math.abs(ev.getY() - mDownY);
                if (absDeltaX > ViewConfiguration.get(getContext()).getScaledTouchSlop() ||
                        absDeltaY > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
            case MotionEvent.ACTION_UP:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
