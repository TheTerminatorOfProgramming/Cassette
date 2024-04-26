package com.ttop.cassette.ui.fragments.player.flat;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.ttop.cassette.R;
import com.ttop.cassette.databinding.FragmentFlatPlayerPlaybackControlsBinding;
import com.ttop.cassette.helper.MusicPlayerRemote;
import com.ttop.cassette.helper.MusicProgressViewUpdateHelper;
import com.ttop.cassette.helper.PlayPauseButtonOnClickHandler;
import com.ttop.cassette.misc.LongTouchActionListener;
import com.ttop.cassette.misc.SimpleOnSeekbarChangeListener;
import com.ttop.cassette.service.MusicService;
import com.ttop.cassette.ui.fragments.AbsMusicServiceFragment;
import com.ttop.cassette.util.MusicUtil;
import com.ttop.cassette.util.PreferenceUtil;
import com.ttop.cassette.views.PlayPauseDrawable;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class FlatPlayerPlaybackControlsFragment extends AbsMusicServiceFragment implements MusicProgressViewUpdateHelper.Callback {
    ImageButton playPauseButton;
    ImageButton prevButton;
    ImageButton nextButton;
    ImageButton repeatButton;
    ImageButton shuffleButton;
    ImageButton repeatButton1;
    ImageButton shuffleButton1;
    SeekBar progressSlider;
    TextView songTotalTime;
    TextView songCurrentProgress;

    private PlayPauseDrawable playPauseDrawable;

    private int lastPlaybackControlsColor;
    private int lastDisabledPlaybackControlsColor;

    private MusicProgressViewUpdateHelper progressViewUpdateHelper;

    private AnimatorSet musicControllerAnimationSet;

    private boolean hidden = false;

    int seekTime = 1000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentFlatPlayerPlaybackControlsBinding binding = FragmentFlatPlayerPlaybackControlsBinding.inflate(inflater, container, false);
        playPauseButton = binding.playerPlayPauseButton;
        prevButton = binding.playerPrevButton;
        nextButton = binding.playerNextButton;
        repeatButton = binding.playerRepeatButton;
        shuffleButton = binding.playerShuffleButton;
        repeatButton1 = binding.playerRepeatButton1;
        shuffleButton1 = binding.playerShuffleButton1;
        progressSlider = binding.playerProgressSlider;
        songTotalTime = binding.playerSongTotalTime;
        songCurrentProgress = binding.playerSongCurrentProgress;

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpMusicControllers();
        updateProgressTextColor();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        progressViewUpdateHelper.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressViewUpdateHelper.stop();
    }

    @Override
    public void onServiceConnected() {
        updatePlayPauseDrawableState(false);
        updateRepeatState();
        updateShuffleState();
    }

    @Override
    public void onPlayStateChanged() {
        updatePlayPauseDrawableState(true);
    }

    @Override
    public void onRepeatModeChanged() {
        updateRepeatState();
    }

    @Override
    public void onShuffleModeChanged() {
        updateShuffleState();
    }

    public void setDark(boolean dark) {
        if (dark) {
            lastPlaybackControlsColor = MaterialValueHelper.getSecondaryTextColor(getActivity(), true);
            lastDisabledPlaybackControlsColor = MaterialValueHelper.getSecondaryDisabledTextColor(getActivity(), true);
        } else {
            lastPlaybackControlsColor = MaterialValueHelper.getPrimaryTextColor(getActivity(), false);
            lastDisabledPlaybackControlsColor = MaterialValueHelper.getPrimaryDisabledTextColor(getActivity(), false);
        }

        updateRepeatState();
        updateShuffleState();
        updatePrevNextColor();
        updatePlayPauseColor();
        updateProgressTextColor();
    }

    private void setUpPlayPauseButton() {
        playPauseDrawable = new PlayPauseDrawable(getActivity());
        playPauseButton.setImageDrawable(playPauseDrawable);
        updatePlayPauseColor();
        playPauseButton.setOnClickListener(new PlayPauseButtonOnClickHandler());
        playPauseButton.post(() -> {
            if (playPauseButton != null) {
                playPauseButton.setPivotX(playPauseButton.getWidth() / 2.0f);
                playPauseButton.setPivotY(playPauseButton.getHeight() / 2.0f);
            }
        });
    }

    protected void updatePlayPauseDrawableState(boolean animate) {
        if (MusicPlayerRemote.isPlaying()) {
            playPauseDrawable.setPause(animate);
        } else {
            playPauseDrawable.setPlay(animate);
        }
    }

    private void setUpMusicControllers() {
        setUpPlayPauseButton();

        if (PreferenceUtil.getInstance().getExtraPlayerControls()) {
            shuffleButton1.setVisibility(View.GONE);
            repeatButton1.setVisibility(View.GONE);

            nextButton.setVisibility(View.VISIBLE);
            prevButton.setVisibility(View.VISIBLE);
            repeatButton.setVisibility(View.VISIBLE);
            shuffleButton.setVisibility(View.VISIBLE);

            setUpPrevNext();
        } else {
            shuffleButton1.setVisibility(View.VISIBLE);
            repeatButton1.setVisibility(View.VISIBLE);

            nextButton.setVisibility(View.GONE);
            prevButton.setVisibility(View.GONE);
            repeatButton.setVisibility(View.GONE);
            shuffleButton.setVisibility(View.GONE);
        }

        setUpRepeatButton();
        setUpShuffleButton();
        setUpProgressSlider();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpPrevNext() {
        updatePrevNextColor();

        nextButton.setOnTouchListener(new com.ttop.cassette.misc.LongTouchActionListener(requireContext()) {
            @Override
            public void onClick(View v) {
                if (PreferenceUtil.getInstance().getCurrentState())
                {
                    if (MusicPlayerRemote.isPlaying()){
                        MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() + 1, true);
                    }else{
                        MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() + 1, false);
                    }
                }else{
                    MusicPlayerRemote.playNextSong();
                }
            }

            @Override
            public void onLongTouchAction(View v) {
                // get current song position
                int currentPosition = MusicPlayerRemote.getSongProgressMillis();
                // check if seekForward time is lesser than song duration
                if (currentPosition + seekTime <= MusicPlayerRemote.getSongDurationMillis()) {
                    // forward song
                    MusicPlayerRemote.seekTo(currentPosition + seekTime);
                } else {
                    // forward to end position
                    MusicPlayerRemote.seekTo(MusicPlayerRemote.getSongDurationMillis());
                }
            }
        });

        prevButton.setOnTouchListener(new com.ttop.cassette.misc.LongTouchActionListener(requireContext()) {
            @Override
            public void onClick(View v) {
                if (PreferenceUtil.getInstance().getCurrentState())
                {
                    if (MusicPlayerRemote.getPosition() > 0) {
                        if (MusicPlayerRemote.isPlaying()) {
                            MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() - 1, true);
                        } else {
                            MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() - 1, false);
                        }
                    }else{
                        MusicPlayerRemote.seekTo(0);
                    }
                }else{
                    MusicPlayerRemote.playPreviousSong();
                }
            }

            @Override
            public void onLongTouchAction(View v) {
                // get current song position
                int currentPosition = MusicPlayerRemote.getSongProgressMillis();
                // check if seekRewind time is more than 0
                if (currentPosition - seekTime >= 0) {
                    // rewind song
                    MusicPlayerRemote.seekTo(currentPosition - seekTime);
                } else {
                    // rewind to start position
                    MusicPlayerRemote.seekTo(0);
                }
            }
        });
    }

    private void updateProgressTextColor() {
        int color = MaterialValueHelper.getPrimaryTextColor(getContext(), false);
        songTotalTime.setTextColor(color);
        songCurrentProgress.setTextColor(color);
    }

    private void updatePrevNextColor() {
        nextButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        prevButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
    }

    private void updatePlayPauseColor() {
        playPauseButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
    }

    private void setUpShuffleButton() {
        shuffleButton.setOnClickListener(v -> MusicPlayerRemote.toggleShuffleMode());
        shuffleButton1.setOnClickListener(v -> MusicPlayerRemote.toggleShuffleMode());
    }

    private void updateShuffleState() {
        if (MusicPlayerRemote.getShuffleMode() == MusicService.SHUFFLE_MODE_SHUFFLE) {
            shuffleButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
            shuffleButton1.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        } else {
            shuffleButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
            shuffleButton1.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private void setUpRepeatButton() {
        repeatButton.setOnClickListener(v -> MusicPlayerRemote.cycleRepeatMode());
        repeatButton1.setOnClickListener(v -> MusicPlayerRemote.cycleRepeatMode());
    }

    private void updateRepeatState() {
        switch (MusicPlayerRemote.getRepeatMode()) {
            case MusicService.REPEAT_MODE_NONE:
                repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp);
                repeatButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);

                repeatButton1.setImageResource(R.drawable.ic_repeat_white_24dp);
                repeatButton1.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
            case MusicService.REPEAT_MODE_ALL:
                repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp);
                repeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);

                repeatButton1.setImageResource(R.drawable.ic_repeat_white_24dp);
                repeatButton1.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
            case MusicService.REPEAT_MODE_THIS:
                repeatButton.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                repeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);

                repeatButton1.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                repeatButton1.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
        }
    }

    public void show() {
        if (hidden) {
            if (musicControllerAnimationSet == null) {
                TimeInterpolator interpolator = new FastOutSlowInInterpolator();
                final int duration = 300;

                LinkedList<Animator> animators = new LinkedList<>();

                addAnimation(animators, playPauseButton, interpolator, duration, 0);
                addAnimation(animators, nextButton, interpolator, duration, 100);
                addAnimation(animators, prevButton, interpolator, duration, 100);
                addAnimation(animators, shuffleButton, interpolator, duration, 200);
                addAnimation(animators, repeatButton, interpolator, duration, 200);
                addAnimation(animators, shuffleButton1, interpolator, duration, 200);
                addAnimation(animators, repeatButton1, interpolator, duration, 200);

                musicControllerAnimationSet = new AnimatorSet();
                musicControllerAnimationSet.playTogether(animators);
            } else {
                musicControllerAnimationSet.cancel();
            }
            musicControllerAnimationSet.start();
        }

        hidden = false;
    }

    public void hide() {
        if (musicControllerAnimationSet != null) {
            musicControllerAnimationSet.cancel();
        }
        prepareForAnimation(playPauseButton);
        prepareForAnimation(nextButton);
        prepareForAnimation(prevButton);
        prepareForAnimation(shuffleButton);
        prepareForAnimation(repeatButton);
        prepareForAnimation(shuffleButton1);
        prepareForAnimation(repeatButton1);

        hidden = true;
    }

    private static void addAnimation(Collection<Animator> animators, View view, TimeInterpolator interpolator, int duration, int delay) {
        Animator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f);
        scaleX.setInterpolator(interpolator);
        scaleX.setDuration(duration);
        scaleX.setStartDelay(delay);
        animators.add(scaleX);

        Animator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f);
        scaleY.setInterpolator(interpolator);
        scaleY.setDuration(duration);
        scaleY.setStartDelay(delay);
        animators.add(scaleY);
    }

    private static void prepareForAnimation(View view) {
        if (view != null) {
            view.setScaleX(0f);
            view.setScaleY(0f);
        }
    }

    private void setUpProgressSlider() {
        int color = MaterialValueHelper.getPrimaryTextColor(getContext(), false);
        progressSlider.getThumb().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        progressSlider.getProgressDrawable().mutate().setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);

        progressSlider.setOnSeekBarChangeListener(new SimpleOnSeekbarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MusicPlayerRemote.seekTo(progress);
                    onUpdateProgressViews(MusicPlayerRemote.getSongProgressMillis(), MusicPlayerRemote.getSongDurationMillis());
                }
            }
        });
    }

    @Override
    public void onUpdateProgressViews(int progress, int total) {
        progressSlider.setMax(total);
        progressSlider.setProgress(progress);
        songTotalTime.setText(MusicUtil.getReadableDurationString(total));
        songCurrentProgress.setText(MusicUtil.getReadableDurationString(progress));
    }
}