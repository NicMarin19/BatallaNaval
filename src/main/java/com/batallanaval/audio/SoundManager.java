package com.batallanaval.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SoundManager {
    public enum SoundType {
        SONAR_PING,
        CANNON_FIRE,
        WATER_SPLASH,
        EXPLOSION,
        HIT_ALERT,
        VICTORY,
        DEFEAT,
        BUTTON_CLICK
    }

    private static SoundManager instance;
    private final Map<SoundType, byte[]> audioSamples;
    private final AudioFormat audioFormat;
    private boolean soundEnabled = true;

    private SoundManager() {
        this.audioFormat = new AudioFormat(44100f, 16, 1, true, false);
        this.audioSamples = new HashMap<>();
        pregenerateSounds();
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public void toggleSound() {
        this.soundEnabled = !this.soundEnabled;
    }

    public void play(SoundType type) {
        if (!soundEnabled) return;
        byte[] pcmData = audioSamples.get(type);
        if (pcmData == null) return;

        new Thread(() -> {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
                AudioInputStream ais = new AudioInputStream(bais, audioFormat, pcmData.length / 2);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (Exception ignored) {
            }
        }, "Audio-" + type.name()).start();
    }

    private void pregenerateSounds() {
        audioSamples.put(SoundType.SONAR_PING, generateSonarPing());
        audioSamples.put(SoundType.CANNON_FIRE, generateCannonFire());
        audioSamples.put(SoundType.WATER_SPLASH, generateWaterSplash());
        audioSamples.put(SoundType.EXPLOSION, generateExplosion());
        audioSamples.put(SoundType.HIT_ALERT, generateHitAlert());
        audioSamples.put(SoundType.VICTORY, generateVictoryFanfare());
        audioSamples.put(SoundType.DEFEAT, generateDefeatAlarm());
        audioSamples.put(SoundType.BUTTON_CLICK, generateButtonClick());
    }

    private byte[] generateSonarPing() {
        int sampleRate = 44100;
        double duration = 1.2;
        int totalSamples = (int) (sampleRate * duration);
        byte[] buffer = new byte[totalSamples * 2];

        double freq = 920.0;
        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            double envelope = Math.exp(-3.5 * t);
            double sample = Math.sin(2.0 * Math.PI * freq * t) * envelope;
            // Add subtle high harmonic
            sample += 0.25 * Math.sin(2.0 * Math.PI * (freq * 2) * t) * envelope;
            sample = Math.max(-1.0, Math.min(1.0, sample * 0.6));
            short val = (short) (sample * 32767);
            buffer[i * 2] = (byte) (val & 0xFF);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buffer;
    }

    private byte[] generateCannonFire() {
        int sampleRate = 44100;
        double duration = 0.9;
        int totalSamples = (int) (sampleRate * duration);
        byte[] buffer = new byte[totalSamples * 2];
        Random rnd = new Random(42);

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            double env = Math.exp(-6.0 * t);
            // Low boom dropping from 120Hz to 35Hz
            double currentFreq = 120.0 * Math.exp(-8.0 * t) + 35.0;
            double boom = Math.sin(2.0 * Math.PI * currentFreq * t);
            double noise = (rnd.nextDouble() * 2.0 - 1.0) * Math.exp(-12.0 * t);
            double sample = (boom * 0.7 + noise * 0.5) * env;
            sample = Math.max(-1.0, Math.min(1.0, sample * 0.85));
            short val = (short) (sample * 32767);
            buffer[i * 2] = (byte) (val & 0xFF);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buffer;
    }

    private byte[] generateWaterSplash() {
        int sampleRate = 44100;
        double duration = 0.8;
        int totalSamples = (int) (sampleRate * duration);
        byte[] buffer = new byte[totalSamples * 2];
        Random rnd = new Random(101);

        double filter = 0.0;
        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            double env = Math.sin(Math.min(Math.PI, t * Math.PI / 0.15)) * Math.exp(-4.5 * t);
            double noise = rnd.nextDouble() * 2.0 - 1.0;
            // Low-pass filter to sound like liquid splash
            filter += 0.25 * (noise - filter);
            // Add bubbling sine warble
            double bubble = Math.sin(2.0 * Math.PI * (400.0 + 300.0 * Math.sin(30.0 * t)) * t) * 0.3;
            double sample = (filter * 0.7 + bubble) * env;
            sample = Math.max(-1.0, Math.min(1.0, sample * 0.7));
            short val = (short) (sample * 32767);
            buffer[i * 2] = (byte) (val & 0xFF);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buffer;
    }

    private byte[] generateExplosion() {
        int sampleRate = 44100;
        double duration = 1.4;
        int totalSamples = (int) (sampleRate * duration);
        byte[] buffer = new byte[totalSamples * 2];
        Random rnd = new Random(555);

        double prevNoise = 0.0;
        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            double env = Math.exp(-3.2 * t);
            double rawNoise = rnd.nextDouble() * 2.0 - 1.0;
            prevNoise += 0.15 * (rawNoise - prevNoise); // low rumble
            double subBoom = Math.sin(2.0 * Math.PI * (55.0 * Math.exp(-3.0 * t)) * t);
            double sample = (prevNoise * 0.8 + subBoom * 0.4 + rawNoise * Math.exp(-15.0 * t) * 0.6) * env;
            // Soft clipping
            sample = Math.tanh(sample * 1.5);
            sample = Math.max(-1.0, Math.min(1.0, sample * 0.85));
            short val = (short) (sample * 32767);
            buffer[i * 2] = (byte) (val & 0xFF);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buffer;
    }

    private byte[] generateHitAlert() {
        int sampleRate = 44100;
        double duration = 0.35;
        int totalSamples = (int) (sampleRate * duration);
        byte[] buffer = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            double env = Math.exp(-8.0 * t);
            double sample = (Math.sin(2.0 * Math.PI * 1200.0 * t) + 0.5 * Math.sin(2.0 * Math.PI * 2400.0 * t)) * env;
            sample = Math.max(-1.0, Math.min(1.0, sample * 0.5));
            short val = (short) (sample * 32767);
            buffer[i * 2] = (byte) (val & 0xFF);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buffer;
    }

    private byte[] generateVictoryFanfare() {
        int sampleRate = 44100;
        double duration = 1.8;
        int totalSamples = (int) (sampleRate * duration);
        byte[] buffer = new byte[totalSamples * 2];

        // Notes: C5 (523.25), E5 (659.25), G5 (783.99), C6 (1046.50)
        double[] freqs = {523.25, 659.25, 783.99, 1046.50};
        double noteLen = 0.35;

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            int noteIdx = Math.min(freqs.length - 1, (int) (t / noteLen));
            double noteTime = t - (noteIdx * noteLen);
            double env = Math.exp(-2.5 * noteTime);
            double f = freqs[noteIdx];
            double sample = (Math.sin(2.0 * Math.PI * f * t) + 0.3 * Math.sin(2.0 * Math.PI * f * 2 * t)) * env;
            sample = Math.max(-1.0, Math.min(1.0, sample * 0.55));
            short val = (short) (sample * 32767);
            buffer[i * 2] = (byte) (val & 0xFF);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buffer;
    }

    private byte[] generateDefeatAlarm() {
        int sampleRate = 44100;
        double duration = 1.5;
        int totalSamples = (int) (sampleRate * duration);
        byte[] buffer = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            double freq = 220.0 - 60.0 * (t / duration);
            double env = 0.5 * (1.0 + Math.sin(2.0 * Math.PI * 4.0 * t)) * Math.exp(-1.0 * t);
            double sample = Math.sin(2.0 * Math.PI * freq * t) * env;
            sample = Math.max(-1.0, Math.min(1.0, sample * 0.6));
            short val = (short) (sample * 32767);
            buffer[i * 2] = (byte) (val & 0xFF);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buffer;
    }

    private byte[] generateButtonClick() {
        int sampleRate = 44100;
        double duration = 0.05;
        int totalSamples = (int) (sampleRate * duration);
        byte[] buffer = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            double env = Math.exp(-60.0 * t);
            double sample = Math.sin(2.0 * Math.PI * 1400.0 * t) * env;
            sample = Math.max(-1.0, Math.min(1.0, sample * 0.4));
            short val = (short) (sample * 32767);
            buffer[i * 2] = (byte) (val & 0xFF);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buffer;
    }
}
