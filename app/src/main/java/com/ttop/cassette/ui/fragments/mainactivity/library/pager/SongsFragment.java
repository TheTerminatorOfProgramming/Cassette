package com.ttop.cassette.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;

import com.ttop.cassette.R;
import com.ttop.cassette.adapter.song.ShuffleButtonSongAdapter;
import com.ttop.cassette.adapter.song.SongAdapter;
import com.ttop.cassette.interfaces.LoaderIds;
import com.ttop.cassette.loader.SongLoader;
import com.ttop.cassette.misc.WrappedAsyncTaskLoader;
import com.ttop.cassette.model.Song;
import com.ttop.cassette.util.PreferenceUtil;

import java.util.ArrayList;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongsFragment extends AbsLibraryPagerRecyclerViewCustomGridSizeFragment<SongAdapter, GridLayoutManager> implements LoaderManager.LoaderCallbacks<ArrayList<Song>> {

    private static final int LOADER_ID = LoaderIds.SONGS_FRAGMENT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @NonNull
    @Override
    protected GridLayoutManager createLayoutManager() {
        return new GridLayoutManager(getActivity(), getGridSize());
    }

    @NonNull
    @Override
    protected SongAdapter createAdapter() {
        int itemLayoutRes = getItemLayoutRes();
        notifyLayoutResChanged(itemLayoutRes);
        boolean usePalette = loadUsePalette();
        ArrayList<Song> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();

        if (getGridSize() <= getMaxGridSizeForList()) {
            return new ShuffleButtonSongAdapter(
                    getLibraryFragment().getMainActivity(),
                    dataSet,
                    itemLayoutRes,
                    usePalette,
                    getLibraryFragment());
        }
        return new SongAdapter(
                getLibraryFragment().getMainActivity(),
                dataSet,
                itemLayoutRes,
                usePalette,
                getLibraryFragment());
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_songs;
    }

    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();

        // give the adapter a chance to update the decoration
        getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onPlayStateChanged() {
        super.onPlayStateChanged();

        // give the adapter a chance to update the decoration
        getAdapter().notifyDataSetChanged();
    }

    @Override
    protected String loadSortOrder() {
        return PreferenceUtil.getInstance().getSongSortOrder();
    }

    @Override
    protected void saveSortOrder(String sortOrder) {
        PreferenceUtil.getInstance().setSongSortOrder(sortOrder);
    }

    @Override
    protected int loadGridSize() {
        return PreferenceUtil.getInstance().getSongGridSize(getActivity());
    }

    @Override
    protected void saveGridSize(int gridSize) {
        PreferenceUtil.getInstance().setSongGridSize(gridSize);
    }

    @Override
    protected int loadGridSizeLand() {
        return PreferenceUtil.getInstance().getSongGridSizeLand(getActivity());
    }

    @Override
    protected void saveGridSizeLand(int gridSize) {
        PreferenceUtil.getInstance().setSongGridSizeLand(gridSize);
    }

    @Override
    protected int loadGridSizeTablet() {
        return PreferenceUtil.getInstance().getSongGridSizeTablet(getActivity());
    }

    @Override
    protected void saveGridSizeTablet(int gridSize) {
        PreferenceUtil.getInstance().setSongGridSizeTablet(gridSize);
    }

    @Override
    protected int loadGridSizeTabletLand() {
        return PreferenceUtil.getInstance().getSongGridSizeTabletLand(getActivity());
    }

    @Override
    protected void saveGridSizeTabletLand(int gridSize) {
        PreferenceUtil.getInstance().setSongGridSizeTabletLand(gridSize);
    }

    @Override
    public void saveUsePalette(boolean usePalette) {
        PreferenceUtil.getInstance().setSongColoredFooters(usePalette);
    }

    @Override
    public boolean loadUsePalette() {
        return PreferenceUtil.getInstance().songColoredFooters();
    }

    @Override
    public void setUsePalette(boolean usePalette) {
        getAdapter().usePalette(usePalette);
    }

    @Override
    protected void setGridSize(int gridSize) {
        getLayoutManager().setSpanCount(gridSize);
        getAdapter().notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Loader<ArrayList<Song>> onCreateLoader(int id, Bundle args) {
        return new AsyncSongLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<Song>> loader, ArrayList<Song> data) {
        getAdapter().swapDataSet(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<Song>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<ArrayList<Song>> {
        public AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public ArrayList<Song> loadInBackground() {
            return SongLoader.getAllSongs();
        }
    }

    @Override
    public void reload() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }
}
