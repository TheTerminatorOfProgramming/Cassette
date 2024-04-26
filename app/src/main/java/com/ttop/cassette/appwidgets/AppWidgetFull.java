package com.ttop.cassette.appwidgets;

import android.widget.RemoteViews;

import com.ttop.cassette.R;
import com.ttop.cassette.appwidgets.base.BaseAppWidget;
import com.ttop.cassette.service.MusicService;
import com.ttop.cassette.util.PreferenceUtil;

public class AppWidgetFull extends BaseAppWidget {
    public static final String NAME = "app_widget_full";

    private static AppWidgetFull mInstance;

    public static synchronized AppWidgetFull getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetFull();
        }
        return mInstance;
    }

    /**
     * Update all active widget instances by pushing changes
     */
    public void performUpdate(final MusicService service, final int[] appWidgetIds) {
        appWidgetView = new RemoteViews(service.getPackageName(), getLayout());

        // Set the titles and artwork
        setTitlesArtwork(service);

        // Link actions buttons to intents
        linkButtons(service);

        // Load the album cover async and push the update on completion
        loadAlbumCover(service, appWidgetIds, 1);

        appWidgetView.setTextColor(R.id.title, service.getResources().getColor(PreferenceUtil.getWidgetTextColorFromPrefValue(PreferenceUtil.getInstance().getAppTheme())));
        appWidgetView.setTextColor(R.id.text, service.getResources().getColor(PreferenceUtil.getWidgetTextColorFromPrefValue(PreferenceUtil.getInstance().getAppTheme())));
    }


    public int getLayout() {
        return R.layout.app_widget_full;
    }

    public int getId() {
        return R.id.app_widget_full;
    }

    public int getImageSize(final MusicService service) {
        return service.getResources().getDimensionPixelSize(R.dimen.app_widget_full_image_size);
    }

    public float getCardRadius(final MusicService service) {
        return service.getResources().getDimension(R.dimen.app_widget_card_radius);
    }
}

