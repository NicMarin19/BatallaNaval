package com.batallanaval.ui;

import com.batallanaval.audio.SoundManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Ajustes compartidos por la pantalla de inicio y el menu de pausa.
 * Mantiene los valores y notifica a la escena cuando cambian.
 */
public class GameSettings {

    private boolean soundEnabled = true;
    private boolean highQualityWater = true;
    private boolean showHints = true;

    private final List<Runnable> listeners = new ArrayList<>();

    public GameSettings() {
        soundEnabled = SoundManager.getInstance().isSoundEnabled();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyChanged() {
        for (Runnable r : listeners) {
            r.run();
        }
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        SoundManager.getInstance().setSoundEnabled(enabled);
        notifyChanged();
    }

    public void toggleSound() {
        setSoundEnabled(!soundEnabled);
    }

    public boolean isHighQualityWater() {
        return highQualityWater;
    }

    public void setHighQualityWater(boolean high) {
        this.highQualityWater = high;
        notifyChanged();
    }

    public void toggleWaterQuality() {
        setHighQualityWater(!highQualityWater);
    }

    public boolean isShowHints() {
        return showHints;
    }

    public void setShowHints(boolean show) {
        this.showHints = show;
        notifyChanged();
    }

    public void toggleShowHints() {
        setShowHints(!showHints);
    }
}
