package com.djawad1.dont.horror;

import com.djawad1.dont.DontMod;
import com.djawad1.dont.config.DontConfig;
import com.djawad1.dont.entity.BlackEntity;
import com.djawad1.dont.maze.MazeManager;
import com.djawad1.dont.net.DontNet;
import com.djawad1.dont.state.DontState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/** Central brain: day counting, stage, cooldowns, event choice, escalation, the breach. */
public class HorrorDirector {
    private static final Random RNG = Random.create();

    public static void tick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) return;
        DontState st = DontState.get(server);
        ServerWorld ow = server.getOverworld();
        DontConfig cfg = DontConfig.INSTANCE;

        // players already in the maze
        ServerWorld mw = MazeManager.mazeWorld(server);
        for (ServerPlayerEntity p : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
            if (MazeManager.inMaze(p)) MazeManager.tickPlayer(p, mw);
        }

        if (st.finished) { postEnding(server, st); return; }
        if (st.phase != 0) return;

        long day = ow.getTimeOfDay() / 24000L;
        HorrorStage stage = HorrorStage.forDay(day, cfg.daysBeforeHorror);
        if (stage == HorrorStage.NORMALITY) return;

        long now = ow.getTime();
        if (now < st.nextEvent) return;

        List<ServerPlayerEntity> players = new ArrayList<>();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
            if (p.getWorld() == ow && !p.isSpectator()) players.add(p);
        if (players.isEmpty()) return;
        ServerPlayerEntity target = players.get(RNG.nextInt(players.size()));

        // The Breach: late stage, enough events witnessed, then a random chance each check.
        if (stage == HorrorStage.MANIFESTATION && st.eventsSeen >= 8 && (RNG.nextFloat() < 0.04f || day >= cfg.daysBeforeHorror + 12)) {
            breach(target);
            return;
        }

        runRandomEvent(target, stage, st);
        st.eventsSeen++;
        float intensity = Math.max(0.25f, cfg.horrorIntensity);
        int gap = 1200 + RNG.nextInt(4800);
        if (RNG.nextInt(6) == 0) gap += 6000;           // sometimes a long silence
        st.nextEvent = now + (long) (gap / intensity);
        st.markDirty();
    }

    public static void runRandomEvent(ServerPlayerEntity p, HorrorStage stage, DontState st) {
        int max = switch (stage) { case SUSPICION -> 3; case DISTURBANCE -> 5; case PRESENCE -> 7; default -> 9; };
        int pick = RNG.nextInt(max);
        DontConfig cfg = DontConfig.INSTANCE;
        switch (pick) {
            case 0 -> footsteps(p);
            case 1 -> distantBreak(p);
            case 2 -> door(p);
            case 3 -> glitch(p);
            case 4 -> animalsLook(p, false);
            case 5 -> farFigure(p);
            case 6 -> { if (RNG.nextFloat() < cfg.entityAppearanceFrequency) appearFlash(p); else footsteps(p); }
            case 7 -> animalsLook(p, true);
            default -> { if (RNG.nextFloat() < cfg.entityAppearanceFrequency) appearFlash(p); else glitch(p); }
        }
    }

    // ---------- events ----------
    public static void footsteps(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        Vec3d back = p.getRotationVec(1f).multiply(1, 0, 1).normalize().multiply(-3.5);
        float vol = 0.9f * DontConfig.INSTANCE.ambientVolume;
        for (int i = 0; i < 6; i++) {
            final int n = i;
            DontMod.schedule(i * 9, () -> {
                Vec3d pos = p.getPos().add(back.multiply(1.0 + n * 0.05));
                BlockPos bp = BlockPos.ofFloored(pos).down();
                BlockState bs = w.getBlockState(bp);
                if (bs.isAir()) return;
                w.playSound(null, pos.x, pos.y, pos.z, bs.getSoundGroup().getStepSound(), SoundCategory.AMBIENT, vol, 0.85f);
            });
        }
    }

    public static void distantBreak(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        double a = RNG.nextDouble() * Math.PI * 2;
        double d = 18 + RNG.nextInt(14);
        BlockPos bp = BlockPos.ofFloored(p.getX() + Math.cos(a) * d, p.getY(), p.getZ() + Math.sin(a) * d);
        BlockState bs = w.getBlockState(bp.down());
        if (bs.isAir()) bs = w.getBlockState(bp);
        w.playSound(null, bp.getX(), bp.getY(), bp.getZ(), bs.getSoundGroup().getBreakSound(), SoundCategory.AMBIENT,
                2.0f * DontConfig.INSTANCE.ambientVolume, 0.8f);
    }

    public static void door(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        double a = RNG.nextDouble() * Math.PI * 2;
        double x = p.getX() + Math.cos(a) * 9, z = p.getZ() + Math.sin(a) * 9;
        float vol = 1.0f * DontConfig.INSTANCE.ambientVolume;
        w.playSound(null, x, p.getY(), z, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.AMBIENT, vol, 0.9f);
        DontMod.schedule(45, () -> w.playSound(null, x, p.getY(), z, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.AMBIENT, vol, 0.9f));
    }

    public static void glitch(ServerPlayerEntity p) {
        DontNet.glitch(p, 700, 0.35f * DontConfig.INSTANCE.ambientVolume);
    }

    public static void appearFlash(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        Vec3d look = p.getRotationVec(1f).multiply(1, 0, 1).normalize();
        for (int i = 0; i < 12; i++) {
            double ang = (RNG.nextDouble() - 0.5) * 0.9;
            double d = 8 + RNG.nextInt(8);
            double c = Math.cos(ang), s = Math.sin(ang);
            Vec3d dir = new Vec3d(look.x * c - look.z * s, 0, look.x * s + look.z * c);
            BlockPos base = BlockPos.ofFloored(p.getX() + dir.x * d, p.getY(), p.getZ() + dir.z * d);
            BlockPos pos = findStand(w, base, 0, 2, 3);
            if (pos == null || Math.abs(pos.getY() - p.getY()) > 3) continue;
            if (spawn(w, pos, p, BlackEntity.WATCH, Math.max(10, DontConfig.INSTANCE.appearanceTicks)) != null) {
                DontNet.glitch(p, 400, 0.45f * DontConfig.INSTANCE.jumpscareVolume);
            }
            return;
        }
    }

    public static void farFigure(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        if (!w.isSkyVisible(p.getBlockPos())) { footsteps(p); return; }
        double a = RNG.nextDouble() * Math.PI * 2;
        double d = 45 + RNG.nextInt(15);
        int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
        if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) return;
        int y = w.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        spawn(w, new BlockPos(x, y, z), p, BlackEntity.WATCH, 100);
    }

    public static void animalsLook(ServerPlayerEntity p, boolean approach) {
        ServerWorld w = p.getServerWorld();
        List<AnimalEntity> animals = w.getEntitiesByClass(AnimalEntity.class, p.getBoundingBox().expand(24), a -> true);
        if (animals.isEmpty()) return;
        int cap = Math.max(2, (int) (8 * DontConfig.INSTANCE.whiteEyesFrequency));
        List<AnimalEntity> chosen = animals.size() > cap ? animals.subList(0, cap) : animals;
        List<Integer> ids = new ArrayList<>();
        for (AnimalEntity a : chosen) ids.add(a.getId());
        DontNet.eyes(p, ids, approach ? 260 : 200);
        for (int t = 0; t < (approach ? 24 : 18); t++) {
            DontMod.schedule(t * 10, () -> {
                for (AnimalEntity a : chosen) {
                    if (!a.isAlive() || a.squaredDistanceTo(p) > 40 * 40) continue;
                    if (approach && a.squaredDistanceTo(p) > 36) a.getNavigation().startMovingTo(p, 1.6);
                    else a.getNavigation().stop();
                    a.getLookControl().lookAt(p, 60f, 60f);
                    if (a instanceof CatEntity c) c.setSitting(false);
                }
            });
        }
    }

    // ---------- the breach ----------
    public static void breach(ServerPlayerEntity p) {
        DontState st = DontState.get(p.getServer());
        st.phase = 1;               // prevents re-entry while in progress
        st.nextEvent = Long.MAX_VALUE;
        st.markDirty();
        ServerWorld w = p.getServerWorld();
        Vec3d look = p.getRotationVec(1f).multiply(1, 0, 1).normalize();
        BlockPos base = BlockPos.ofFloored(p.getPos().add(look.multiply(12)));
        BlockPos pos = findStand(w, base, 0, 3, 8);
        if (pos == null) pos = p.getBlockPos().add((int) (look.x * 6), 0, (int) (look.z * 6));
        BlackEntity e = spawn(w, pos, p, BlackEntity.WATCH, 100);
        DontNet.glitch(p, 1500, 1.0f * DontConfig.INSTANCE.jumpscareVolume);
        if (e != null) {
            e.setNoGravity(false);
            for (int i = 1; i <= 14; i++) {
                DontMod.schedule(i, () -> {
                    if (!e.isAlive()) return;
                    Vec3d dir = p.getPos().subtract(e.getPos()).multiply(1, 0, 1);
                    if (dir.lengthSquared() < 4) return;
                    Vec3d np = e.getPos().add(dir.normalize().multiply(0.8));
                    e.refreshPositionAndAngles(np.x, e.getY(), np.z, e.getYaw(), 0);
                });
            }
        }
        DontMod.schedule(15, () -> {
            Vec3d away = p.getPos().subtract(e != null ? e.getPos() : p.getPos().add(look)).multiply(1, 0, 1);
            if (away.lengthSquared() < 0.01) away = look;
            Vec3d kb = away.normalize().multiply(1.6);
            p.setVelocity(kb.x, 0.45, kb.z);
            p.velocityModified = true;
            DontNet.blackout(p, 1100, 1500, 1200);
        });
        DontMod.schedule(40, () -> {
            if (e != null) e.discard();
            MazeManager.enter(p);
        });
    }

    // ---------- maze ambience ----------
    public static void mazeAmbient(ServerPlayerEntity p, ServerWorld mw) {
        DontConfig cfg = DontConfig.INSTANCE;
        float r = RNG.nextFloat();
        if (r < 0.05f) DontNet.glitch(p, 500, 0.3f * cfg.ambientVolume);
        else if (r < 0.10f) {
            Vec3d pos = p.getPos().add(RNG.nextInt(9) - 4, 0, RNG.nextInt(9) - 4);
            mw.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_DEEPSLATE_BRICKS_STEP, SoundCategory.AMBIENT, 0.9f * cfg.ambientVolume, 0.8f);
        } else if (r < 0.14f) {
            BlockPos pos = findStand(mw, p.getBlockPos(), 9, 16, 20);
            if (pos != null && mw.getEntitiesByType(DontMod.BLACK_ENTITY, e -> true).size() < 6) spawn(mw, pos, p, BlackEntity.WATCH, 80);
        } else if (r < 0.16f) {
            BlockPos pos = findStand(mw, p.getBlockPos(), 12, 20, 20);
            if (pos != null && mw.getEntitiesByType(DontMod.BLACK_ENTITY, e -> true).size() < 6) {
                BlackEntity e = spawn(mw, pos, p, BlackEntity.HUNT, 200);
                if (e != null) e.setHuntSpeed(1.1);
            }
        }
    }

    // ---------- after the crash ----------
    private static final java.util.Set<java.util.UUID> POST_SEEN = new java.util.HashSet<>();

    private static void postEnding(MinecraftServer server, DontState st) {
        if (st.postDone || !DontConfig.INSTANCE.postCrashEffects) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (MazeManager.inMaze(p) || !POST_SEEN.add(p.getUuid())) continue;
            ServerWorld w = p.getServerWorld();
            DontMod.schedule(200, () -> {
                // a black door where there was none
                for (int i = 0; i < 20; i++) {
                    double a = RNG.nextDouble() * Math.PI * 2;
                    int x = (int) (p.getX() + Math.cos(a) * 28), z = (int) (p.getZ() + Math.sin(a) * 28);
                    if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) continue;
                    int y = w.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos b = new BlockPos(x, y, z);
                    if (!w.getBlockState(b.down()).isSolid()) continue;
                    var black = net.minecraft.block.Blocks.BLACK_CONCRETE.getDefaultState();
                    for (int dy = 0; dy < 3; dy++) {
                        w.setBlockState(b.up(dy), black, 3);
                        w.setBlockState(b.up(dy).east(), black, 3);
                    }
                    w.setBlockState(b.up(3), black, 3);
                    w.setBlockState(b.up(3).east(), black, 3);
                    p.sendMessage(net.minecraft.text.Text.literal("YOU BROUGHT IT BACK").styled(s -> s.withColor(0xAA0000)), true);
                    st.postDone = true; st.markDirty();
                    return;
                }
            });
        }
    }

    // ---------- helpers ----------
    public static BlockPos findStand(ServerWorld w, BlockPos center, int minD, int maxD, int tries) {
        for (int i = 0; i < tries; i++) {
            int dx = RNG.nextInt(maxD * 2 + 1) - maxD, dz = RNG.nextInt(maxD * 2 + 1) - maxD;
            if (dx * dx + dz * dz < minD * minD) continue;
            int x = center.getX() + dx, z = center.getZ() + dz;
            if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) continue;
            for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                BlockPos pos = new BlockPos(x, center.getY() + dy, z);
                if (w.getBlockState(pos).isAir() && w.getBlockState(pos.up()).isAir()
                        && w.getBlockState(pos.down()).isSolidBlock(w, pos.down())) return pos;
            }
        }
        return null;
    }

    public static BlackEntity spawn(ServerWorld w, BlockPos pos, ServerPlayerEntity face, int mode, int life) {
        if (w.getEntitiesByType(DontMod.BLACK_ENTITY, e -> true).size() >= 24) return null;
        BlackEntity e = DontMod.BLACK_ENTITY.create(w);
        if (e == null) return null;
        double dx = face.getX() - (pos.getX() + 0.5), dz = face.getZ() - (pos.getZ() + 0.5);
        float yaw = (float) (MathHelper.atan2(dz, dx) * 57.29578) - 90f;
        e.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
        e.setHeadYaw(yaw);
        e.setBodyYaw(yaw);
        e.setMode(mode);
        e.setLife(life);
        e.setPersistent();
        w.spawnEntity(e);
        return e;
    }
}
