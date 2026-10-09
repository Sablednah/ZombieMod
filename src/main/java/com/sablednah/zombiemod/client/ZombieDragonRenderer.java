package com.sablednah.zombiemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * Vanilla's dragon renderer, plus a rotting skin for the Zombie Dragon.
 *
 * <p>Every dragon that is not one of ours - and ours while it is dying, dissolve and light rays and
 * all - goes straight to {@code super}. Only a living Zombie Dragon is drawn here: vanilla's own
 * body texture, our rot over it translucent, and our eyes in place of the purple ones. The pose is
 * vanilla's, line for line, so the two passes sit exactly on the vanilla model.
 *
 * <p><b>Differs per version</b>, like the rest of {@code client/}: the {@code submitModel} overloads
 * and the name-tag call changed between 1.21.11 and 26.x.
 */
public class ZombieDragonRenderer extends EnderDragonRenderer {

    private static final Identifier DRAGON = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");

    private final EnderDragonModel model;

    public ZombieDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new EnderDragonModel(context.bakeLayer(ModelLayers.ENDER_DRAGON));
    }

    @Override
    public EnderDragonRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EnderDragon entity, EnderDragonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof State ours) {
            ours.zombie = ZombieDragons.isZombie(entity.getId());
            // Lying dead before it rises: let vanilla draw its own beams and dissolve, which it
            // only does for a dragon with a death time - and this one's health never reached zero.
            float fake = ZombieDragons.fakeDeathTime(entity.getId(), partialTicks);
            if (fake > 0.0F) {
                state.deathTime = fake;
            }
        }
    }

    @Override
    public void submit(EnderDragonRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (!(state instanceof State ours) || !ours.zombie || state.deathTime > 0.0F) {
            super.submit(state, poseStack, collector, camera);
            return;
        }

        poseStack.pushPose();
        float yRot = state.getHistoricalPos(7).yRot();
        float pitch = (float) (state.getHistoricalPos(5).y() - state.getHistoricalPos(10).y());
        poseStack.rotateDegrees(Axis.YP, -yRot);
        poseStack.rotateDegrees(Axis.XP, pitch * 10.0F);
        poseStack.translate(0.0F, 0.0F, 1.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        int overlay = OverlayTexture.pack(0.0F, state.hasRedOverlay);

        collector.submitModel(this.model, state, poseStack, DRAGON, state.lightCoords, overlay, state.outlineColor);
        collector.submitModel(this.model, state, poseStack, RenderTypes.entityTranslucent(ZombieDragons.rot()),
                state.lightCoords, overlay, -1, null, state.outlineColor);
        collector.submitModel(this.model, state, poseStack, RenderTypes.eyes(ZombieDragons.eyes()),
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();

        if (state.beamOffset != null) {
            submitCrystalBeams((float) state.beamOffset.x, (float) state.beamOffset.y, (float) state.beamOffset.z,
                    state.ageInTicks, poseStack, collector, state.lightCoords);
        }
        submitNameDisplay(state, poseStack, collector, camera);
    }

    /** Vanilla's state, plus the one thing it cannot know. */
    static final class State extends EnderDragonRenderState {
        boolean zombie;
    }
}
