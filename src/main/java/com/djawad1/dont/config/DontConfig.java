package com.djawad1.dont.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings saved to config/dont.json. */
public class DontConfig {
    public float horrorIntensity = 1.0f;
    public float jumpscareVolume = 1.0f;
    public float ambientVolume = 0.8f;
    public float entityAppearanceFrequency = 1.0f;
    public float whiteEyesFrequency = 1.0f;
    public float glitchEffectIntensity = 1.0f;
    public boolean flashEffects = true;
    public boolean postCrashEffects = true;
    public boolean simulatedCrash = true;
    public boolean debugMode = false;
    public int appearanceTicks = 30;
    public int daysBeforeHorror = 3;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static DontConfig INSTANCE = new DontConfig();

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("dont.json"); }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                try (Reader r = Files.newBufferedReader(f)) {
                    DontConfig c = GSON.fromJson(r, DontConfig.class);
                    if (c != null) INSTANCE = c;
                }
            }
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(INSTANCE, w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
