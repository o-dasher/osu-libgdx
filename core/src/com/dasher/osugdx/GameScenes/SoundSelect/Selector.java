package com.dasher.osugdx.GameScenes.SoundSelect;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Align;
import com.dasher.osugdx.IO.Beatmaps.BeatMapSet;
import com.dasher.osugdx.IO.Beatmaps.BeatmapManager;
import com.dasher.osugdx.IO.Beatmaps.BeatmapManagerListener;
import com.dasher.osugdx.Images.GameImage;
import com.dasher.osugdx.OsuGame;
import com.dasher.osugdx.Skins.Skin;

import org.jetbrains.annotations.NotNull;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;


public abstract class Selector extends Group implements BeatmapManagerListener {
    public final GameImage thumbnail;
    protected final OsuGame game;
    protected final GameImage menuBackground;
    protected final Skin skin;
    protected final BeatmapManager beatmapManager;
    protected final SoundSelectScreen soundSelectScreen;
    public boolean addedThumbnail = false;

    public Selector(
            OsuGame game, @NotNull Skin skin,
            BeatmapManager beatmapManager, SoundSelectScreen soundSelectScreen
    ) {
        super();
        this.game = game;
        Sprite menuButtonBG = skin.menuButtonBG.getSprite();
        float w = menuButtonBG.getWidth();
        float h = menuButtonBG.getHeight();
        this.menuBackground = new GameImage(game, menuButtonBG, false);
        this.thumbnail = new GameImage(game, new SpriteDrawable(new Sprite()), false);
        menuBackground.setSize(w, h);
        this.beatmapManager = beatmapManager;
        this.skin = skin;
        this.soundSelectScreen = soundSelectScreen;
        adjustColor();
        this.addActor(menuBackground);
        setSize(w, h);
        setOrigin(Align.right);
        thumbnail.setSize(115, 85);
        float thumbnailX = 18;
        if (skin.menuButtonBG.isHD()) {
            thumbnailX /= 2;
        }
        thumbnail.setPosition(thumbnailX, getHeight() / 2 - thumbnail.getHeight() / 2);
        menuBackground.setPosition(getWidth() / 2 - w / 2f, getHeight() / 2 - h / 2f);
        setScale(0.9f);
        addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (mapChangeCondition()) {
                    changeMap();
                    adjustColor();
                }
                return false;
            }
        });
    }

    public abstract boolean mapChangeCondition();
    public abstract void changeMap();
    public abstract boolean isThisMapSelected();

    public boolean isThumbnailTextureLoaded = false;
    public boolean isLazyLoadingThumbnail = false;

    @Override
    public void act(float delta) {
        super.act(delta);
    }

    public void adjustColor() {
        if (isThisMapSelected()) {
            changeSelectedSelector();
            menuBackground.setColor(skin.getSongSelectActiveTextColor());
        } else {
            menuBackground.setColor(skin.getSongSelectInactiveTextColor());
        }
    }

    public void disableSelector(Selector selector) {
        if (selector != null) {
            selector.menuBackground.setColor(skin.getSongSelectInactiveTextColor());
        }
    }

    public abstract void changeSelectedSelector();

    @Override
    public void onNewBeatmap(Beatmap beatmap) {
        adjustColor();
    }

    @Override
    public void onNewBeatmapSet(BeatMapSet beatMapSet) {
        adjustColor();
    }
}