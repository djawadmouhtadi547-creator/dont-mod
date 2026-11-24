package com.djawad1.dont.maze;

import com.djawad1.dont.DontMod;
import com.djawad1.dont.config.DontConfig;
import com.djawad1.dont.entity.BlackEntity;
import com.djawad1.dont.horror.HorrorDirector;
import com.djawad1.dont.net.DontNet;
import com.djawad1.dont.state.DontState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Maze dimension, false exit, white-eyed army, final hunt and the safe fake crash. */
public class MazeManager {
    public static final RegistryKey<World> MAZE_KEY =
            RegistryKey.of(RegistryKeys.WORLD, new Identifier("dont", "maze"));

    // Placement origin and points found by tools/points.py on the converted litematic.
    public static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    public static final BlockPos START = ORIGIN.add(2, 8, 2);
    public static final BlockPos EXIT = ORIGIN.add(100, 9, 14);

    private static boolean sequenceBusy = false;

    public static boolean inMaze(ServerPlayerEntity p) {
        return p.getWorld().getRegistryKey() == MAZE_KEY;
    }

    public static ServerWorld mazeWorld(MinecraftServer s) { return s.getWorld(MAZE_KEY); }

    public static void ensureBuilt(MinecraftServer server) {
        DontState st = DontState.get(server);
        ServerWorld mw = mazeWorld(server);
        if (mw == null || st.mazePlaced) return;
        StructureTemplate t = mw.getStructureTemplateManager().getTemplate(new Identifier("dont", "maze")).orElse(null);
        if (t == null) { System.err.println("[DON'T] maze.nbt missing"); return; }
        t.place(mw, ORIGIN, ORIGIN, new StructurePlacementData().setIgnoreEntities(true),
                mw.getRandom(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        // The false exit: a bright door with a light behind it.
        mw.setBlockState(EXIT, Blocks.IRON_DOOR.getDefaultState(), 2);
        mw.setBlockState(EXIT.up(2), Blocks.LIGHT.getDefaultState(), 2);
        st.mazePlaced = true;
        st.markDirty();
    }

    /** Called by HorrorDirector: send the player into the maze, remembering where they were. */
    public static void enter(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        DontState st = DontState.get(server);
        ensureBuilt(server);
        ServerWorld mw = mazeWorld(server);
        if (mw == null) return;
        st.retX = p.getX(); st.retY = p.getY(); st.retZ = p.getZ();
        st.retYaw = p.getYaw(); st.retPitch = p.getPitch();
        st.hasReturn = true;
        st.phase = 1;
        st.markDirty();
        p.teleport(mw, START.getX() + 0.5, START.getY(), START.getZ() + 0.5, 0f, 0f);
        p.sendMessage(Text.literal(""), true);
    }

    public static void returnToWorld(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        DontState st = DontState.get(server);
        ServerWorld ow = server.getOverworld();
        if (st.hasReturn) p.teleport(ow, st.retX, st.retY, st.retZ, st.retYaw, st.retPitch);
        else p.teleport(ow, ow.getSpawnPos().getX() + 0.5, ow.getSpawnPos().getY(), ow.getSpawnPos().getZ() + 0.5, 0f, 0f);
        p.removeStatusEffect(StatusEffects.DARKNESS);
    }

    /** Per-second maze logic. */
    public static void tickPlayer(ServerPlayerEntity p, ServerWorld mw) {
        DontState st = DontState.get(p.getServer());
        if (st.phase == 1) {
            if (p.getBlockPos().getSquaredDistance(EXIT) < 9.0 && !sequenceBusy) {
                falseExit(p, mw);
                return;
            }
            HorrorDirector.mazeAmbient(p, mw);
        }
    }

    private static void falseExit(ServerPlayerEntity p, ServerWorld mw) {
        sequenceBusy = true;
        DontNet.glitch(p, 1200, 0.7f);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 220, 0, false, false));
        // open the "door": a wide dark void filled with watchers
        List<BlackEntity> watchers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            double a = i * (Math.PI * 2 / 10);
            BlockPos pos = HorrorDirector.findStand(mw, p.getBlockPos().add((int) (Math.cos(a) * 6), 0, (int) (Math.sin(a) * 6)), 0, 4, 20);
            if (pos == null) continue;
            BlackEntity e = HorrorDirector.spawn(mw, pos, p, BlackEntity.WATCH, 260);
            if (e != null) { e.setMain(i == 0); watchers.add(e); }
        }
        DontMod.schedule(100, () -> DontNet.blackout(p, 600, 800, 800));
        DontMod.schedule(125, () -> {
            for (BlackEntity e : watchers) e.discard();
            p.teleport(mw, START.getX() + 0.5, START.getY(), START.getZ() + 0.5, 0f, 0f);
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 60, 20));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("")));
            p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("YOU WERE NEVER SUPPOSED TO LEAVE.").styled(s -> s.withColor(0xAA0000))));
            populateArmy(p, mw);
        });
        DontMod.schedule(125 + 160, () -> startFinalHunt(p, mw));
    }

    private static void populateArmy(ServerPlayerEntity p, ServerWorld mw) {
        DontState st = DontState.get(p.getServer());
        st.phase = 2; st.markDirty();
        int placed = 0;
        for (int i = 0; i < 9 && placed < 8; i++) {
            BlockPos pos = HorrorDirector.findStand(mw, p.getBlockPos(), i < 3 ? 3 : 8, i < 3 ? 10 : 24, 30);
            if (pos == null) continue;
            BlackEntity e = HorrorDirector.spawn(mw, pos, p, BlackEntity.WATCH, -1);
            if (e != null) { e.setMain(placed == 0); placed++; }
        }
    }

    private static void startFinalHunt(ServerPlayerEntity p, ServerWorld mw) {
        // brief silence, then everything rushes.
        for (BlackEntity e : mw.getEntitiesByType(DontMod.BLACK_ENTITY, x -> true)) {
            e.setMode(BlackEntity.HUNT);
            e.setHuntSpeed(e.isMain() ? 2.4 : 2.0);
        }
    }

    /** A hunter reached the player. */
    public static void onCaught(BlackEntity e, ServerPlayerEntity p) {
        DontState st = DontState.get(p.getServer());
        if (st.phase != 2 || st.finished) return;
        st.phase = 3; st.markDirty();
        finalSequence(p);
    }

    private static void finalSequence(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        ServerWorld mw = mazeWorld(server);
        for (BlackEntity e : mw.getEntitiesByType(DontMod.BLACK_ENTITY, x -> true)) e.discard();
        DontNet.jumpscare(p, 2600);
        // while the screen is covered: go home, mark the world as ended, save, then fake the crash.
        DontMod.schedule(60, () -> {
            returnToWorld(p);
            DontState st = DontState.get(server);
            st.finished = true; st.phase = 3; st.markDirty();
            markEnded(server, true);
            sequenceBusy = false;
        });
        DontMod.schedule(70, () -> server.save(false, true, true));
        DontMod.schedule(80, () -> {
            if (DontConfig.INSTANCE.simulatedCrash) DontNet.crash(p);
        });
    }

    public static void startFinalHuntCommand(ServerPlayerEntity p) {
        ServerWorld mw = mazeWorld(p.getServer());
        if (mw == null || !inMaze(p)) return;
        sequenceBusy = true;
        populateArmy(p, mw);
        DontMod.schedule(60, () -> startFinalHunt(p, mw));
    }

    // ---- "dont" world-name marker, read by the client mixin (config/dont_ended_worlds.txt) ----
    public static Path markerFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("dont_ended_worlds.txt");
    }

    public static void markEnded(MinecraftServer server, boolean ended) {
        try {
            Path root = server.getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
            String dir = root.getFileName().toString();
            Path f = markerFile();
            List<String> lines = new ArrayList<>(Files.exists(f) ? Files.readAllLines(f) : List.of());
            lines.remove(dir);
            if (ended) lines.add(dir);
            Files.write(f, lines);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void resetRun(MinecraftServer server, ServerPlayerEntity p) {
        sequenceBusy = false;
        DontState st = DontState.get(server);
        if (p != null && inMaze(p)) returnToWorld(p);
        ServerWorld mw = mazeWorld(server);
        if (mw != null) for (BlackEntity e : mw.getEntitiesByType(DontMod.BLACK_ENTITY, x -> true)) e.discard();
        st.reset();
        markEnded(server, false);
    }
}
