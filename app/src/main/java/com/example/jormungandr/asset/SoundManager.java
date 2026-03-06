package com.example.jormungandr.asset;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles music playback (MediaPlayer) and sound effects (SoundPool).
 * Loads audio from the assets/sounds/ directory.
 */
public class SoundManager {
    private static SoundManager instance;

    private final Context context;
    private MediaPlayer musicPlayer;
    private SoundPool soundPool;
    private final Map<String, Integer> loadedSounds = new HashMap<>();
    private boolean musicEnabled = true;
    private boolean sfxEnabled = true;
    private float musicVolume = 0.5f;
    private float sfxVolume = 0.7f;

    private SoundManager(Context context) {
        this.context = context.getApplicationContext();
        this.soundPool = new SoundPool.Builder().setMaxStreams(8).build();
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    /**
     * Play background music from assets/sounds/music/
     */
    public void playMusic(String musicName) {
        if (!musicEnabled) return;
        stopMusic();

        try {
            AssetFileDescriptor afd = context.getAssets().openFd("sounds/music/" + musicName);
            musicPlayer = new MediaPlayer();
            musicPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            musicPlayer.setLooping(true);
            musicPlayer.setVolume(musicVolume, musicVolume);
            musicPlayer.prepare();
            musicPlayer.start();
            afd.close();
        } catch (IOException e) {
            // Music file not available yet
        }
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.release();
            musicPlayer = null;
        }
    }

    /**
     * Play a sound effect from assets/sounds/sfx/
     */
    public void playSfx(String sfxName) {
        if (!sfxEnabled) return;

        Integer soundId = loadedSounds.get(sfxName);
        if (soundId != null) {
            soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
            return;
        }

        // Load and play
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("sounds/sfx/" + sfxName);
            int id = soundPool.load(afd, 1);
            loadedSounds.put(sfxName, id);
            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                if (status == 0) {
                    pool.play(sampleId, sfxVolume, sfxVolume, 1, 0, 1.0f);
                }
            });
            afd.close();
        } catch (IOException e) {
            // SFX not available yet
        }
    }

    public void setMusicEnabled(boolean enabled) { this.musicEnabled = enabled; }
    public void setSfxEnabled(boolean enabled) { this.sfxEnabled = enabled; }
    public void setMusicVolume(float volume) { this.musicVolume = volume; }
    public void setSfxVolume(float volume) { this.sfxVolume = volume; }

    public void release() {
        stopMusic();
        soundPool.release();
        loadedSounds.clear();
    }
}
