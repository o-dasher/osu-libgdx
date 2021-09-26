package com.dasher.osugdx.osu.Mods;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.dasher.osugdx.Framework.Interfaces.Listenable;
import com.dasher.osugdx.OsuGame;
import com.dasher.osugdx.osu.Beatmaps.BeatMapSet;


import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public class ModManager implements ModManagerListener, Listenable<ModManagerListener> {
    private final OsuGame game;
    private final Array<ModManagerListener> listeners = new Array<>();

    public ModManager(OsuGame game) {
        this.game = game;
    }

    public void calculateBeatmaps(Mods mods) {
        game.asyncExecutor.submit(() -> {
            Array<BeatMapSet> beatMapSets = game.beatMapStore.getBeatMapSets();
            for (int i = 0; i < beatMapSets.size; i++) {
                BeatMapSet beatMapSet = beatMapSets.get(i);
                for (int j = 0; j < beatMapSet.beatmaps.size; j++) {
                    Beatmap beatmap = beatMapSet.beatmaps.get(j);
                    if (beatmap.getHitObjects().isEmpty()) {
                        Beatmap calculated = game.beatmapUtils.createMap(
                                Gdx.files.external(beatmap.beatmapFilePath),
                                true, true,
                                true, true,
                                true, true
                        );
                        if (calculated.getHitObjects().notEmpty()) {
                            calculated.calculateBase(mods);
                            beatmap.setBaseStars(calculated.getBaseStars());
                            beatmap.setTimingPoints(calculated.getTimingPoints());
                            System.out.println(beatmap.getMetadata().getTitleRomanized() + " recalculated");
                        }
                    } else {
                        System.out.println(
                                "Ignoring beatmap calculation of: "
                                        + beatmap.getMetadata().getTitleRomanized() +
                                        ", it already has objects"
                        );
                        beatmap.calculateBase(mods);
                    }
                    onBeatmapCalculated(beatmap);
                }
            }
            // i hate lambdas....
            try {
                onCompleteCalculation();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public void onBeatmapCalculated(Beatmap beatmap) {
        for (int i = 0; i < listeners.size; i ++) {
            ModManagerListener listener = listeners.get(i);
            listener.onBeatmapCalculated(beatmap);
        }
    }

    @Override
    public void onCompleteCalculation() {
        for (int i = 0; i < listeners.size; i ++) {
            ModManagerListener listener = listeners.get(i);
            listener.onCompleteCalculation();
        }
    }

    @Override
    public Array<ModManagerListener> getListeners() {
        return listeners;
    }
}
