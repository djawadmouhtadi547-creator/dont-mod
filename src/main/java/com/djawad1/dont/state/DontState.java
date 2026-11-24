package com.djawad1.dont.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** Persistent state stored in the world's data folder (never touches region data). */
public class DontState extends PersistentState {
    public int phase = 0; // 0 none, 1 maze, 2 final hunt, 3 finished
    public boolean finished = false;
    public boolean postDone = false;
    public boolean mazePlaced = false;
    public int eventsSeen = 0;
    public long nextEvent = 0;
    public double retX, retY, retZ;
    public float retYaw, retPitch;
    public boolean hasReturn = false;

    public static DontState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(DontState::fromNbt, DontState::new, "dont");
    }

    public static DontState fromNbt(NbtCompound n) {
        DontState s = new DontState();
        s.phase = n.getInt("phase");
        s.finished = n.getBoolean("finished");
        s.postDone = n.getBoolean("postDone");
        s.mazePlaced = n.getBoolean("mazePlaced");
        s.eventsSeen = n.getInt("eventsSeen");
        s.nextEvent = n.getLong("nextEvent");
        s.retX = n.getDouble("retX");
        s.retY = n.getDouble("retY");
        s.retZ = n.getDouble("retZ");
        s.retYaw = n.getFloat("retYaw");
        s.retPitch = n.getFloat("retPitch");
        s.hasReturn = n.getBoolean("hasReturn");
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound n) {
        n.putInt("phase", phase);
        n.putBoolean("finished", finished);
        n.putBoolean("postDone", postDone);
        n.putBoolean("mazePlaced", mazePlaced);
        n.putInt("eventsSeen", eventsSeen);
        n.putLong("nextEvent", nextEvent);
        n.putDouble("retX", retX);
        n.putDouble("retY", retY);
        n.putDouble("retZ", retZ);
        n.putFloat("retYaw", retYaw);
        n.putFloat("retPitch", retPitch);
        n.putBoolean("hasReturn", hasReturn);
        return n;
    }

    public void reset() {
        phase = 0; finished = false; postDone = false; eventsSeen = 0; nextEvent = 0; hasReturn = false;
        markDirty();
    }
}
