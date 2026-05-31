package me.lingci.lib.dm.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.HashMap;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.BiliDanmakuLoader;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;
import me.lingci.lib.base.util.CodeUtil;
import me.lingci.lib.base.util.Log;
import me.lingci.lib.base.util.ScreenUtil;
import me.lingci.lib.base.util.ToastUtil;
import me.lingci.lib.dm.view.parser.BiliDanmakuParser;
import me.lingci.lib.dm.view.entity.DmStyleExtend;
import me.lingci.lib.dm.view.widget.GradientViewCacheStuffer;

public class DmFlowView extends DanmakuView {

    private final DanmakuContext mContext;
    private final BiliDanmakuLoader mDmLoader;

    public DmFlowView(@NonNull Context context) {
        super(context);
    }

    public DmFlowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DmFlowView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
        // 滚动弹幕最大显示5行
        //maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 5);
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        boolean isTablet = ScreenUtil.isTablet(Resources.getSystem().getConfiguration());
        Log.d(TAG, "instance initializer: " + metrics.density + " " + isTablet);
        mContext = DanmakuContext.create();
        mDmLoader = BiliDanmakuLoader.instance();
        mContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 6)
                .setDuplicateMergingEnabled(true)
                .setScrollSpeedFactor(1f)
                .setScaleTextSize((isTablet ? 2 : 1.2f))
                .setDanmakuTransparency(0.75f)
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair)
                .setCacheStuffer(new GradientViewCacheStuffer(), null)
                .setDanmakuMargin(32);
        setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                start();
            }

            @Override
            public void updateTimer(DanmakuTimer timer) {

            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {

            }

            @Override
            public void drawingFinished() {

            }
        });
        showFPS(BuildConfig.DEBUG);
        enableDanmakuDrawingCache(true);
    }

    @Override
    public View getView() {
        return this;
    }

    public void loadDm(String url) {
        Log.d(TAG, "loadDm: " + url);
        if (TextUtils.isEmpty(url)) {
            return;
        }
        File file = new File(url);
        if (!file.exists()) {
            return;
        }
        try {
            mDmLoader.load(url);
            BiliDanmakuParser parser = new BiliDanmakuParser(true);
            parser.load(mDmLoader.getDataSource());
            prepare(parser, mContext);
            mContext.addUserHashBlackList();
        } catch (IllegalDataException e) {
            ToastUtil.INSTANCE.showToast(this.getContext(), "弹幕加载失败");
        }
    }

    public void loadDefault() {
        try {
            prepare(new BaseDanmakuParser() {
                @Override
                protected IDanmakus parse() {
                    return new Danmakus();
                }
            }, mContext);
            setCallback(new DrawHandler.Callback() {
                @Override
                public void prepared() {
                    start();
                }

                @Override
                public void updateTimer(DanmakuTimer timer) {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {

                }

                @Override
                public void drawingFinished() {
                    seekTo(0L);
                }
            });
            mContext.addUserHashBlackList();
        } catch (Exception ignored) {

        }
    }

    public void setLines(int lines) {
        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
        // 滚动弹幕最大显示行
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, lines);
        mContext.setMaximumLines(maxLinesPair);
        mContext.addUserHashBlackList();
    }

    /**
     * 发送文字弹幕
     *
     * @param text   弹幕文字
     * @param styleExtend 样式扩展
     */
    public void addDanmaku(String text, long time, Integer color, DmStyleExtend styleExtend) {
        BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null) {
            return;
        }
        danmaku.text = text;
        danmaku.priority = 0;  // 优先级 0可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = false;
        danmaku.setTime(getCurrentTime() + time);
        danmaku.textSize = CodeUtil.INSTANCE.dp2px(24);
        danmaku.textColor = color == null ? Color.WHITE : color;
        danmaku.textShadowColor = Color.BLACK;
        danmaku.borderColor = Color.TRANSPARENT;
        danmaku.tag = styleExtend;
        addDanmaku(danmaku);
    }

    public void setDanMuSize(int progress) {
        mContext.setScaleTextSize(1.2f * (progress / 35f));
        mContext.addUserHashBlackList();
    }

    public void setDanMuOpacity(int progress) {
        mContext.setDanmakuTransparency(progress/ 100f);
        mContext.addUserHashBlackList();
    }

    public void setDanMuStroke(int progress) {
        mContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, progress);
        mContext.addUserHashBlackList();
    }

}
