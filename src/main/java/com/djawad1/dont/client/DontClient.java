package com.djawad1.dont.client;

import com.djawad1.dont.DontMod;
import com.djawad1.dont.config.DontConfig;
import com.djawad1.dont.entity.BlackEntity;
import com.djawad1.dont.maze.MazeManager;
import com.djawad1.dont.net.DontNet;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DontClient implements ClientModInitializer {
    private static final Identifier JUMP_TEX = new Identifier("dont", "textures/gui/jumpscare.png");
    private static final Identifier GLITCH_TEX = new Identifier("dont", "textures/gui/glitch.png");

    private static long jumpStart = 0, jumpDur = 0;
    private static long blackStart = 0, fadeIn = 0, hold = 0, fadeOut = 0;
    private static long glitchStart = 0, glitchDur = 0;
    private static final Map<Integer, Long> EYES = new HashMap<>();

    public static boolean hasEyes(int id) {
        Long until = EYES.get(id);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) { EYES.remove(id); return false; }
        return true;
    }

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(DontMod.BLACK_ENTITY, BlackRenderer::new);

        Set<EntityType<?>> eyed = new HashSet<>(Set.of(EntityType.COW, EntityType.SHEEP, EntityType.PIG,
                EntityType.CHICKEN, EntityType.WOLF, EntityType.CAT, EntityType.HORSE, EntityType.FOX, DontMod.BLACK_ENTITY));
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (eyed.contains(type)) helper.register(new WhiteEyesFeature(renderer));
        });

        ClientPlayNetworking.registerGlobalReceiver(DontNet.JUMPSCARE, (mc, h, buf, snd) -> {
            int ms = buf.readInt();
            mc.execute(() -> {
                jumpStart = System.currentTimeMillis();
                jumpDur = ms;
                mc.getSoundManager().play(PositionedSoundInstance.master(DontMod.FINAL_JUMPSCARE, 1.0f,
                        DontConfig.INSTANCE.jumpscareVolume));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DontNet.BLACKOUT, (mc, h, buf, snd) -> {
            long a = buf.readInt(), b = buf.readInt(), c = buf.readInt();
            mc.execute(() -> { blackStart = System.currentTimeMillis(); fadeIn = a; hold = b; fadeOut = c; });
        });
        ClientPlayNetworking.registerGlobalReceiver(DontNet.GLITCH, (mc, h, buf, snd) -> {
            int ms = buf.readInt();
            float vol = buf.readFloat();
            mc.execute(() -> {
                glitchStart = System.currentTimeMillis();
                glitchDur = ms;
                mc.getSoundManager().play(PositionedSoundInstance.master(DontMod.GLITCH, 1.0f, vol));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DontNet.EYES, (mc, h, buf, snd) -> {
            int ms = buf.readInt(), n = buf.readInt();
            int[] ids = new int[n];
            for (int i = 0; i < n; i++) ids[i] = buf.readInt();
            mc.execute(() -> {
                long until = System.currentTimeMillis() + ms;
                for (int id : ids) EYES.put(id, until);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DontNet.CRASH, (mc, h, buf, snd) ->
                mc.execute(() -> mc.setScreen(new CrashScreen())));

        HudRenderCallback.EVENT.register(DontClient::hud);
    }

    private static void hud(DrawContext ctx, float tickDelta) {
        long now = System.currentTimeMillis();
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        DontConfig cfg = DontConfig.INSTANCE;

        // short glitch overlay (smooth fade, no strobing)
        if (now < glitchStart + glitchDur) {
            float t = (now - glitchStart) / (float) glitchDur;
            float a = (float) Math.sin(t * Math.PI) * 0.35f * cfg.glitchEffectIntensity;
            int off = cfg.flashEffects ? (int) ((now / 90) % 7) * 6 : 0;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, Math.min(0.6f, a));
            ctx.drawTexture(GLITCH_TEX, -off, 0, w + 40, h, 0f, 0f, 680, 451, 680, 451);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }

        // blackout (fade in / hold / fade out)
        long bt = now - blackStart;
        if (bt >= 0 && bt < fadeIn + hold + fadeOut) {
            float a = bt < fadeIn ? bt / (float) fadeIn : bt < fadeIn + hold ? 1f : 1f - (bt - fadeIn - hold) / (float) fadeOut;
            ctx.fill(0, 0, w, h, ((int) (Math.max(0f, Math.min(1f, a)) * 255) << 24));
        }

        // final jumpscare: black, big face rushing at the camera, shake, then cut to black
        long jt = now - jumpStart;
        if (jt >= 0 && jt < jumpDur) {
            ctx.fill(0, 0, w, h, 0xFF000000);
            float p = jt / (float) jumpDur;
            float scale = 0.9f + 0.9f * Math.min(1f, p * 2.2f);
            int size = (int) (h * scale);
            java.util.Random r = new java.util.Random(jt / 40);
            int sx = cfg.flashEffects ? (int) ((r.nextFloat() - 0.5f) * 30 * cfg.glitchEffectIntensity) : 0;
            int sy = cfg.flashEffects ? (int) ((r.nextFloat() - 0.5f) * 30 * cfg.glitchEffectIntensity) : 0;
            float fade = p > 0.8f ? (1f - p) / 0.2f : 1f;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, fade);
            ctx.drawTexture(JUMP_TEX, (w - size) / 2 + sx, (h - size) / 2 + sy, size, size, 0f, 0f, 240, 240, 240, 240);
            if (p > 0.35f) {
                RenderSystem.setShaderColor(1f, 1f, 1f, 0.22f * cfg.glitchEffectIntensity * fade);
                ctx.drawTexture(GLITCH_TEX, 0, 0, w, h, 0f, 0f, 680, 451, 680, 451);
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
    }

    // ---- world-list marker (worlds that finished the ending show as "dont") ----
    private static Set<String> ended = new HashSet<>();
    private static long lastRead = 0;

    public static boolean isEnded(String dir) {
        long now = System.currentTimeMillis();
        if (now - lastRead > 2000) {
            lastRead = now;
            try {
                var f = MazeManager.markerFile();
                ended = Files.exists(f) ? new HashSet<>(Files.readAllLines(f)) : new HashSet<>();
            } catch (Exception e) { ended = new HashSet<>(); }
        }
        return DontConfig.INSTANCE.postCrashEffects && ended.contains(dir);
    }

    public static class BlackRenderer extends BipedEntityRenderer<BlackEntity, BipedEntityModel<BlackEntity>> {
        private static final Identifier TEX = new Identifier("dont", "textures/entity/black_entity.png");

        public BlackRenderer(EntityRendererFactory.Context ctx) {
            super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.0f);
        }

        @Override
        public Identifier getTexture(BlackEntity e) { return TEX; }
    }
}
