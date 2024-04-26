package com.ttop.cassette.service.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.ttop.cassette.R;
import com.ttop.cassette.discog.tagging.MultiValuesTagUtil;
import com.ttop.cassette.glide.CassetteGlideExtension;
import com.ttop.cassette.glide.GlideApp;
import com.ttop.cassette.glide.CassetteSimpleTarget;
import com.ttop.cassette.glide.palette.BitmapPaletteWrapper;
import com.ttop.cassette.model.Song;
import com.ttop.cassette.service.MusicService;
import com.ttop.cassette.ui.activities.MainActivity;
import com.ttop.cassette.util.ImageUtil;
import com.ttop.cassette.util.MusicUtil;
import com.ttop.cassette.util.PreferenceUtil;
import com.ttop.cassette.util.CassetteColorUtil;

public class PlayingNotificationImpl extends PlayingNotification {

    private Target target;

    @Override
    public synchronized void update() {
        stopped = false;

        final Song song = service.getCurrentSong();

        final boolean isPlaying = service.isPlaying();

        final RemoteViews notificationLayout = new RemoteViews(service.getPackageName(), R.layout.notification);
        final RemoteViews notificationLayoutBig = new RemoteViews(service.getPackageName(), R.layout.notification_big);

        final String artistNames = MultiValuesTagUtil.infoString(song.artistNames);
        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(artistNames)) {
            notificationLayout.setViewVisibility(R.id.media_titles, View.INVISIBLE);
        } else {
            notificationLayout.setViewVisibility(R.id.media_titles, View.VISIBLE);
            notificationLayout.setTextViewText(R.id.title, song.title);
            notificationLayout.setTextViewText(R.id.text, artistNames);
        }

        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(artistNames) && TextUtils.isEmpty(song.albumName)) {
            notificationLayoutBig.setViewVisibility(R.id.media_titles, View.INVISIBLE);
        } else {
            notificationLayoutBig.setViewVisibility(R.id.media_titles, View.VISIBLE);
            notificationLayoutBig.setTextViewText(R.id.title, song.title);
            notificationLayoutBig.setTextViewText(R.id.text, artistNames);
            notificationLayoutBig.setTextViewText(R.id.text2, song.albumName);
        }

        linkButtons(notificationLayout, notificationLayoutBig);

        Intent action = new Intent(service, MainActivity.class);
        action.putExtra(MainActivity.EXPAND_PANEL,  PreferenceUtil.getInstance().getExpandPanel());
        action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final PendingIntent clickIntent = PendingIntent.getActivity(service, 0, action, PendingIntent.FLAG_UPDATE_CURRENT);
        final PendingIntent deleteIntent = buildPendingIntent(service, MusicService.ACTION_QUIT, null);

        final Notification notification = new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon_unfilled)
                .setContentIntent(clickIntent)
                .setDeleteIntent(deleteIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContent(notificationLayout)
                .setCustomBigContentView(notificationLayoutBig)
                .setOngoing(isPlaying)
                .build();

        final int bigNotificationImageSize = service.getResources().getDimensionPixelSize(R.dimen.notification_big_image_size);
        service.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (target != null) {
                    GlideApp.with(service).clear(target);
                }
                target = GlideApp.with(service)
                        .asBitmapPalette()
                        .load(CassetteGlideExtension.getSongModel(song))
                        .transition(CassetteGlideExtension.getDefaultTransition())
                        .songOptions(song)
                        .into(new CassetteSimpleTarget<BitmapPaletteWrapper>(bigNotificationImageSize, bigNotificationImageSize) {
                            @Override
                            public void onResourceReady(@NonNull BitmapPaletteWrapper resource, Transition<? super BitmapPaletteWrapper> glideAnimation) {
                                update(resource.getBitmap(), CassetteColorUtil.getColor(resource.getPalette(), Color.TRANSPARENT));
                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                super.onLoadFailed(errorDrawable);
                                update(null, Color.WHITE);
                            }

                            private void update(@Nullable Bitmap bitmap, int bgColor) {
                                if (bitmap != null) {
                                    notificationLayout.setImageViewBitmap(R.id.image, bitmap);
                                    notificationLayoutBig.setImageViewBitmap(R.id.image, bitmap);
                                } else {
                                    notificationLayout.setImageViewResource(R.id.image, R.drawable.default_album_art);
                                    notificationLayoutBig.setImageViewResource(R.id.image, R.drawable.default_album_art);
                                }

                                if (!PreferenceUtil.getInstance().coloredNotification()) {
                                    bgColor = Color.WHITE;
                                }
                                setBackgroundColor(bgColor);
                                setNotificationContent(ColorUtil.isColorLight(bgColor));

                                if (stopped)
                                    return; // notification has been stopped before loading was finished
                                updateNotifyModeAndPostNotification(notification);
                            }

                            private void setBackgroundColor(int color) {
                                notificationLayout.setInt(R.id.root, "setBackgroundColor", color);
                                notificationLayoutBig.setInt(R.id.root, "setBackgroundColor", color);
                            }

                            private void setNotificationContent(boolean dark) {
                                int primary = MaterialValueHelper.getPrimaryTextColor(service, dark);
                                int secondary = MaterialValueHelper.getSecondaryTextColor(service, dark);

                                Bitmap prev = ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(service, R.drawable.ic_skip_previous_white_24dp, primary), 1.5f);
                                Bitmap next = ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(service, R.drawable.ic_skip_next_white_24dp, primary), 1.5f);
                                Bitmap playPause = ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(service, isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp, primary), 1.5f);

                                notificationLayout.setTextColor(R.id.title, primary);
                                notificationLayout.setTextColor(R.id.text, secondary);
                                notificationLayout.setImageViewBitmap(R.id.action_prev, prev);
                                notificationLayout.setImageViewBitmap(R.id.action_next, next);
                                notificationLayout.setImageViewBitmap(R.id.action_play_pause, playPause);

                                notificationLayoutBig.setTextColor(R.id.title, primary);
                                notificationLayoutBig.setTextColor(R.id.text, secondary);
                                notificationLayoutBig.setTextColor(R.id.text2, secondary);
                                notificationLayoutBig.setImageViewBitmap(R.id.action_prev, prev);
                                notificationLayoutBig.setImageViewBitmap(R.id.action_next, next);
                                notificationLayoutBig.setImageViewBitmap(R.id.action_play_pause, playPause);
                            }
                        });
            }
        });
    }

    private void linkButtons(final RemoteViews notificationLayout, final RemoteViews notificationLayoutBig) {
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(service, MusicService.class);

        // Previous track
        pendingIntent = buildPendingIntent(service, MusicService.ACTION_REWIND, serviceName);
        notificationLayout.setOnClickPendingIntent(R.id.action_prev, pendingIntent);
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_prev, pendingIntent);

        // Play and pause
        pendingIntent = buildPendingIntent(service, MusicService.ACTION_TOGGLE_PAUSE, serviceName);
        notificationLayout.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent);
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent);

        // Next track
        pendingIntent = buildPendingIntent(service, MusicService.ACTION_SKIP, serviceName);
        notificationLayout.setOnClickPendingIntent(R.id.action_next, pendingIntent);
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_next, pendingIntent);
    }

    private PendingIntent buildPendingIntent(Context context, final String action, final ComponentName serviceName) {
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);
        return PendingIntent.getService(context, 0, intent, 0);
    }

}