package com.ttop.cassette.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.ttop.cassette.BuildConfig;
import com.ttop.cassette.helper.StackBlur;
import com.ttop.cassette.util.ImageUtil;

import java.security.MessageDigest;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class BlurTransformation extends BitmapTransformation {

    private static final String ID = "com.ttop.cassette.glide.BlurTransformation";

    public static final float DEFAULT_BLUR_RADIUS = 5f;

    private Context context;
    private float blurRadius;
    private int sampling;

    private void init(Builder builder) {
        this.context = builder.context;
        this.blurRadius = builder.blurRadius;
        this.sampling = builder.sampling;
    }

    private BlurTransformation(Builder builder) {
        super();
        init(builder);
    }

    private BlurTransformation(Builder builder, BitmapPool bitmapPool) {
        super();
        init(builder);
    }

    public static class Builder {
        private final Context context;
        private BitmapPool bitmapPool;
        private float blurRadius = DEFAULT_BLUR_RADIUS;
        private int sampling;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        /**
         * @param blurRadius The radius to use. Must be between 0 and 25. Default is 5.
         * @return the same Builder
         */
        public Builder blurRadius(@FloatRange(from = 0.0f, to = 25.0f) float blurRadius) {
            this.blurRadius = blurRadius;
            return this;
        }

        /**
         * @param sampling The inSampleSize to use. Must be a power of 2, or 1 for no down sampling or 0 for auto detect sampling. Default is 0.
         * @return the same Builder
         */
        public Builder sampling(int sampling) {
            this.sampling = sampling;
            return this;
        }

        /**
         * @param bitmapPool The BitmapPool to use.
         * @return the same Builder
         */
        public Builder bitmapPool(BitmapPool bitmapPool) {
            this.bitmapPool = bitmapPool;
            return this;
        }

        public BlurTransformation build() {
            if (bitmapPool != null) {
                return new BlurTransformation(this, bitmapPool);
            }
            return new BlurTransformation(this);
        }
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        int sampling;
        if (this.sampling == 0) {
            sampling = ImageUtil.calculateInSampleSize(toTransform.getWidth(), toTransform.getHeight(), 100);
        } else {
            sampling = this.sampling;
        }

        int width = toTransform.getWidth();
        int height = toTransform.getHeight();
        int scaledWidth = width / sampling;
        int scaledHeight = height / sampling;

        Bitmap out = pool.get(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.scale(1 / (float) sampling, 1 / (float) sampling);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(toTransform, 0, 0, paint);

        if (Build.VERSION.SDK_INT >= 17) {
            try {
                final RenderScript rs = RenderScript.create(context.getApplicationContext());
                final Allocation input = Allocation.createFromBitmap(rs, out, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
                final Allocation output = Allocation.createTyped(rs, input.getType());
                final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

                script.setRadius(blurRadius);
                script.setInput(input);
                script.forEach(output);

                output.copyTo(out);

                rs.destroy();

                return out;

            } catch (RSRuntimeException e) {
                // on some devices RenderScript.create() throws: android.support.v8.renderscript.RSRuntimeException: Error loading libRSSupport library
                if (BuildConfig.DEBUG) e.printStackTrace();
            }
        }

        return StackBlur.blur(out, blurRadius);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlurTransformation;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(("BlurTransformation(radius=" + blurRadius + ", sampling=" + sampling + ")").getBytes(CHARSET));
    }
}
