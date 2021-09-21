package com.dasher.osugdx.IO.Beatmaps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.dasher.osugdx.Audio.AudioManager;
import com.dasher.osugdx.Framework.Interfaces.Listenable;
import com.dasher.osugdx.GameScenes.Intro.IntroScreen;
import com.dasher.osugdx.GameScenes.SoundSelect.SoundSelectScreen;
import com.dasher.osugdx.GameScenes.UIScreen;
import com.dasher.osugdx.OsuGame;
import com.dasher.osugdx.PlatformSpecific.Toast.PlatformToast;

import org.jetbrains.annotations.NotNull;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;

public class BeatmapManager implements Listenable<BeatmapManagerListener>, BeatmapManagerListener {
    private final BeatMapStore beatMapStore;
    private BeatMapSet currentBeatmapSet;
    private Beatmap currentMap;
    private Music currentMusic;
    private long timeLastMap;
    private final OsuGame game;
    private final PlatformToast toast;
    private final BeatmapUtils beatmapUtils;
    private final AudioManager audioManager;
    private boolean isFirstBeatmapLoaded = false;
    private final Array<BeatmapManagerListener> beatmapManagerListeners = new Array<>();

    public BeatmapManager(OsuGame game, BeatMapStore beatMapStore, PlatformToast toast, BeatmapUtils beatmapUtils, AudioManager audioManager) {
        this.game = game;
        this.beatMapStore = beatMapStore;
        this.toast = toast;
        this.beatmapUtils = beatmapUtils;
        this.audioManager = audioManager;
    }

    public BeatMapSet getCurrentBeatmapSet() {
        return currentBeatmapSet;
    }

    public void randomizeCurrentBeatmapSet() {
        setCurrentBeatmapSet(beatMapStore.getBeatMapSets().random());
    }

    public void setCurrentBeatmapSet(BeatMapSet newBeatmapSet) {
        if (newBeatmapSet == null) {
            randomizeCurrentBeatmapSet();
            return;
        }

        if (currentBeatmapSet != null) {
            for (Beatmap beatmap: currentBeatmapSet.beatmaps) {
                beatmap.freeResources();
            }
        }

        Beatmap beatmapSetFirstMap = newBeatmapSet.beatmaps.first();
        if (beatmapSetFirstMap == null) {
            newBeatmapSet.getFolder().delete();
            beatMapStore.getBeatMapSets().removeValue(newBeatmapSet, true);
            randomizeCurrentBeatmapSet();
        } else {
            System.out.println("Selected mapSet: " + newBeatmapSet.toString());
            reInitBeatmapSet(newBeatmapSet);
            currentBeatmapSet = newBeatmapSet;
            this.onNewBeatmapSet(currentBeatmapSet);
            if (currentBeatmapSet.beatmaps.isEmpty()) {
                handleEmptyBeatmapSet();
            } else {
                setCurrentMap(currentBeatmapSet.beatmaps.first());
            }
        }
    }

    private void reInitBeatmapSet(@NotNull BeatMapSet beatMapSet) {
        Array<Beatmap> loadedBeatmaps = new Array<>();
        for (Beatmap beatmap: beatMapSet.beatmaps) {
            FileHandle beatmapFile = Gdx.files.external(beatmap.beatmapFilePath);
            loadedBeatmaps.add(beatmapUtils.createMap(beatmapFile));
        }
        beatMapSet.beatmaps.clear();
        beatMapSet.beatmaps.addAll(loadedBeatmaps);
    }

    private void setupMusic(Beatmap newMap) {
        if (currentMusic != null && currentMusic.isPlaying() && !newMap.equals(currentMap)) {
            currentMusic.dispose();
        }

        String folder = currentBeatmapSet.beatmapSetFolderPath + "/";
        if (newMap.getGenerals() == null) {
            beatMapStore.deleteBeatmapFile(null, Gdx.files.external(newMap.beatmapFilePath));
            if (game.getScreen() instanceof SoundSelectScreen) {
                game.getScreen().show();
            }
            return;
        }

        String newMusicPath = folder + newMap.getGenerals().getAudioFileName();
        String currentMusicPath = currentMap != null && currentMap.getGenerals() != null?
                folder + currentMap.getGenerals().getAudioFileName() : "";

        // WE DON'T WANT TO RELOAD THE MUSIC IF IT'S THE SAME MUSIC REPLAYING
        if (!newMusicPath.equals(currentMusicPath)) {
            FileHandle musicFile = Gdx.files.external(newMusicPath);
            try {
                currentMusic = Gdx.audio.newMusic(musicFile);
                currentMusic.setOnCompletionListener((music) -> {
                    System.out.println("Beatmap music finished!");
                    if (currentMusic == music) {
                        randomizeCurrentBeatmapSet();
                    }
                });
            } catch (Exception e) {
                toast.log("Failed to create map music for: " + musicFile.name());
                e.printStackTrace();
            }
            System.out.println("New music: " + newMusicPath);
        } else {
            System.out.println("Replaying beatmap music: " + newMap.toString());
            if (game.getScreen() instanceof UIScreen && currentMusic.isPlaying()) {
                return;
            }
        }

        Screen gameScreen = game.getScreen();
        if (currentMusic != null) {
            if (gameScreen instanceof UIScreen) {
                try {
                    currentMusic.setPosition(newMap.getGenerals().getPreviewTime());
                } catch (GdxRuntimeException e) {
                    toast.log("Unexpected error while loading beatmap music!");
                    randomizeCurrentBeatmapSet();
                    return;
                }
            }
            if (gameScreen != null && !(gameScreen instanceof IntroScreen)) {
                audioManager.playMusic(currentMusic);
            }
        }
    }

    public Beatmap getCurrentMap() {
        return currentMap;
    }

    private void handleEmptyBeatmapSet() {
        beatMapStore.deleteBeatmapFile(currentBeatmapSet, null);
        randomizeCurrentBeatmapSet();
        if (game.getScreen() instanceof SoundSelectScreen) {
            game.getScreen().show();
        }
    }

    public void setCurrentMap(Beatmap newMap) {
        if (!currentBeatmapSet.beatmaps.contains(newMap, true)) {
            toast.log("Abnormal beatmap selected!");
            beatMapStore.deleteBeatmapFile(currentBeatmapSet, null);
            if (currentBeatmapSet.beatmaps.isEmpty()) {
                handleEmptyBeatmapSet();
            } else {
                setCurrentMap(currentBeatmapSet.beatmaps.first());
            }
        }
        setupMusic(newMap);
        currentMap = newMap;
        timeLastMap = System.nanoTime();
        onNewBeatmap(currentMap);
        System.out.println("Selected map: " + currentMap.toString());
        isFirstBeatmapLoaded = true;
    }

    public void startMusicPlaying() {
      if (currentMusic != null) {
            audioManager.playMusic(currentMusic);
        }
    }

    public boolean isFirstBeatmapLoaded() {
        return isFirstBeatmapLoaded;
    }

    public long getTimeLastMap() {
        return timeLastMap;
    }

    public Music getCurrentMusic() {
        return currentMusic;
    }

    @Override
    public Array<BeatmapManagerListener> getListeners() {
        return beatmapManagerListeners;
    }

    @Override
    public void onNewBeatmap(Beatmap beatmap) {
        for (BeatmapManagerListener listener: beatmapManagerListeners) {
            listener.onNewBeatmap(beatmap);
        }
    }

    @Override
    public void onNewBeatmapSet(BeatMapSet beatMapSet) {
        for (BeatmapManagerListener listener: beatmapManagerListeners) {
            listener.onNewBeatmapSet(beatMapSet);
        }
    }
}
