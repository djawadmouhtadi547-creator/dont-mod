package com.djawad1.dont.client;

import com.djawad1.dont.entity.BlackEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.CheckedRandom;
import org.joml.Matrix4f;

import java.util.Iterator;

/** Extra layer: two glowing white eyes on the head. Does not replace the original model. */
public class WhiteEyesFeature<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {
    private static final Identifier WHITE = new Identifier("dont", "textures/entity/white.png");
    private static final int FULL_LIGHT = 15728880;

    public WhiteEyesFeature(FeatureRendererContext<T, M> ctx) { super(ctx); }

    @Override
    public void render(MatrixStack m, VertexConsumerProvider vcp, int light, T e, float limbAngle, float limbDist,
                       float tickDelta, float anim, float headYaw, float headPitch) {
        boolean black = e instanceof BlackEntity;
        if (!black && (e.isBaby() || !DontClient.hasEyes(e.getId()))) return;

        M model = this.getContextModel();
        ModelPart head = null;
        if (model instanceof ModelWithHead mh) head = mh.getHead();
        else if (model instanceof AnimalModel<?> am) {
            Iterator<ModelPart> it = am.getHeadParts().iterator();
            if (it.hasNext()) head = it.next();
        }
        if (head == null || head.isEmpty()) return;

        ModelPart.Cuboid c = head.getRandomCuboid(new CheckedRandom(0L) {
            @Override public int nextInt(int bound) { return 0; }
        });

        m.push();
        head.rotate(m);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
        Matrix4f mat = m.peek().getPositionMatrix();
        float w = c.maxX - c.minX, h = c.maxY - c.minY;
        float cx = (c.minX + c.maxX) / 2f;
        float y = c.minY + h * 0.38f;
        float z = c.minZ - 0.03f;
        float s = Math.max(0.9f, w * 0.13f);
        quad(vc, mat, cx - w * 0.24f, y, z, s);
        quad(vc, mat, cx + w * 0.24f, y, z, s);
        m.pop();
    }

    private static void quad(VertexConsumer vc, Matrix4f mat, float px, float py, float pz, float s) {
        float x0 = (px - s) / 16f, x1 = (px + s) / 16f, y0 = (py - s) / 16f, y1 = (py + s) / 16f, z = pz / 16f;
        v(vc, mat, x0, y0, z, 0, 0); v(vc, mat, x0, y1, z, 0, 1); v(vc, mat, x1, y1, z, 1, 1); v(vc, mat, x1, y0, z, 1, 0);
        v(vc, mat, x1, y0, z, 1, 0); v(vc, mat, x1, y1, z, 1, 1); v(vc, mat, x0, y1, z, 0, 1); v(vc, mat, x0, y0, z, 0, 0);
    }

    private static void v(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float u, float vv) {
        vc.vertex(mat, x, y, z).color(255, 255, 255, 255).texture(u, vv)
                .overlay(OverlayTexture.DEFAULT_UV).light(FULL_LIGHT).normal(0f, 1f, 0f).next();
    }
}
