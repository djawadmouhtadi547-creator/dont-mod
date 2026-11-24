package com.djawad1.dont.horror;

public enum HorrorStage {
    NORMALITY, SUSPICION, DISTURBANCE, PRESENCE, MANIFESTATION, BREACH, MAZE, FINAL_HUNT, AFTERMATH;

    public static HorrorStage forDay(long day, int daysBefore) {
        if (day < daysBefore) return NORMALITY;
        long d = day - daysBefore;
        if (d < 2) return SUSPICION;
        if (d < 4) return DISTURBANCE;
        if (d < 7) return PRESENCE;
        return MANIFESTATION;
    }
}
