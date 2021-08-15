package com.dasher.osugdx.Audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.dasher.osugdx.Framework.Audio.AudioHandler;

import org.jetbrains.annotations.NotNull;

public class AudioManager implements AudioHandler {
    private float currentMusicVolume = 1;
    private float currentSFXVolume = 1;

    public void playSound(@NotNull Sound sound) {
        sound.play(currentSFXVolume);
    }

    public void playMusic(@NotNull Music music) {
        music.setVolume(currentSFXVolume);
        music.play();
    }

    public void setSFXVolume(float sfxVolume) {
        currentSFXVolume = sfxVolume;
    }

    public float getCurrentMusicVolume() {
        return currentMusicVolume;
    }

    public void setCurrentMusicVolume(float currentMusicVolume) {
        this.currentMusicVolume = currentMusicVolume;
    }
}