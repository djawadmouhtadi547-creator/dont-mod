package com.djawad1.dont;

import com.djawad1.dont.config.DontConfig;
import com.djawad1.dont.entity.BlackEntity;
import com.djawad1.dont.horror.HorrorDirector;
import com.djawad1.dont.horror.HorrorStage;
import com.djawad1.dont.maze.MazeManager;
import com.djawad1.dont.state.DontState;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class DontMod implements ModInitializer {
    public static final String ID = "dont";

    public static final EntityType<BlackEntity> BLACK_ENTITY = Registry.register(
            Registries.ENTITY_TYPE, new Identifier(ID, "black_entity"),
            FabricEntityTypeBuilder.<BlackEntity>create(SpawnGroup.MISC, BlackEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .trackRangeBlocks(96)
                    .build());

    public static final SoundEvent FINAL_JUMPSCARE = register("final_jumpscare");
    public static final SoundEvent GLITCH = register("glitch");

    private static SoundEvent register(String name) {
        Identifier id = new Identifier(ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    // ---- tiny tick scheduler ----
    private record Task(long due, Runnable run) { }
    private static final List<Task> TASKS = new ArrayList<>();
    private static long clock = 0;

    public static void schedule(int ticks, Runnable r) {
        TASKS.add(new Task(clock + ticks, r));
    }

    @Override
    public void onInitialize() {
        DontConfig.load();
        FabricDefaultAttributeRegistry.register(BLACK_ENTITY, BlackEntity.createAttributes());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            clock++;
            if (!TASKS.isEmpty()) {
                List<Task> due = new ArrayList<>();
                TASKS.removeIf(t -> { if (t.due() <= clock) { due.add(t); return true; } return false; });
                for (Task t : due) {
                    try { t.run().run(); } catch (Exception e) { e.printStackTrace(); }
                }
            }
            HorrorDirector.tick(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> TASKS.clear());

        CommandRegistrationCallback.EVENT.register((d, access, env) -> d.register(
                CommandManager.literal("dont").requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("appear").executes(c -> run(c.getSource(), HorrorDirector::appearFlash)))
                        .then(CommandManager.literal("steps").executes(c -> run(c.getSource(), HorrorDirector::footsteps)))
                        .then(CommandManager.literal("glitch").executes(c -> run(c.getSource(), HorrorDirector::glitch)))
                        .then(CommandManager.literal("animals").executes(c -> run(c.getSource(), p -> HorrorDirector.animalsLook(p, false))))
                        .then(CommandManager.literal("swarm").executes(c -> run(c.getSource(), p -> HorrorDirector.animalsLook(p, true))))
                        .then(CommandManager.literal("breach").executes(c -> run(c.getSource(), HorrorDirector::breach)))
                        .then(CommandManager.literal("exit").executes(c -> run(c.getSource(), p -> {
                            if (MazeManager.inMaze(p)) p.teleport(MazeManager.mazeWorld(p.getServer()),
                                    MazeManager.EXIT.getX() - 2.5, MazeManager.EXIT.getY(), MazeManager.EXIT.getZ() + 0.5, 0f, 0f);
                        })))
                        .then(CommandManager.literal("hunt").executes(c -> run(c.getSource(), MazeManager::startFinalHuntCommand)))
                        .then(CommandManager.literal("day").then(CommandManager.argument("n", IntegerArgumentType.integer(0, 100)).executes(c -> {
                            int n = IntegerArgumentType.getInteger(c, "n");
                            c.getSource().getServer().getOverworld().setTimeOfDay(n * 24000L + 1000L);
                            return 1;
                        })))
                        .then(CommandManager.literal("status").executes(c -> {
                            var s = c.getSource().getServer();
                            DontState st = DontState.get(s);
                            long day = s.getOverworld().getTimeOfDay() / 24000L;
                            c.getSource().sendFeedback(() -> Text.literal("day=" + day + " stage="
                                    + HorrorStage.forDay(day, DontConfig.INSTANCE.daysBeforeHorror)
                                    + " phase=" + st.phase + " finished=" + st.finished + " events=" + st.eventsSeen), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("reset").executes(c -> {
                            ServerCommandSource src = c.getSource();
                            ServerPlayerEntity p = src.getPlayer();
                            MazeManager.resetRun(src.getServer(), p);
                            src.sendFeedback(() -> Text.literal("DON'T state reset."), false);
                            return 1;
                        }))));
    }

    private interface PlayerAction { void run(ServerPlayerEntity p); }

    private static int run(ServerCommandSource src, PlayerAction a) {
        try {
            ServerPlayerEntity p = src.getPlayerOrThrow();
            a.run(p);
            return 1;
        } catch (Exception e) {
            src.sendError(Text.literal("Player only: " + e.getMessage()));
            return 0;
        }
    }
}
