package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TrialSpawnerRenderer implements BlockEntityRenderer<TrialSpawnerBlockEntity, SpawnerRenderState> {
    private final EntityRenderDispatcher entityRenderer;

    public TrialSpawnerRenderer(final BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    public SpawnerRenderState createRenderState() {
        return new SpawnerRenderState();
    }

    public void extractRenderState(
        final TrialSpawnerBlockEntity blockEntity,
        final SpawnerRenderState state,
        final float partialTicks,
        final Vec3 cameraPosition,
        final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        if (blockEntity.getLevel() != null) {
            TrialSpawner spawner = blockEntity.getTrialSpawner();
            TrialSpawnerStateData data = spawner.getStateData();
            Entity displayEntity = data.getOrCreateDisplayEntity(spawner, blockEntity.getLevel(), spawner.getState());
            extractSpawnerData(state, partialTicks, displayEntity, this.entityRenderer, data.getOSpin(), data.getSpin());
        }
    }

    public static void extractSpawnerData(
        final SpawnerRenderState state,
        final float partialTicks,
        final @Nullable Entity displayEntity,
        final EntityRenderDispatcher entityRenderer,
        final double oSpin,
        final double spin
    ) {
        if (displayEntity != null) {
            state.displayEntity = entityRenderer.extractEntity(displayEntity, partialTicks);
            state.displayEntity.lightCoords = state.lightCoords;
            state.spin = (float)Mth.lerp(partialTicks, oSpin, spin) * 10.0F;
            state.scale = 0.53125F;
            float maxLength = Math.max(displayEntity.getBbWidth(), displayEntity.getBbHeight());
            if (maxLength > 1.0) {
                state.scale /= maxLength;
            }
        }
    }

    public void submit(final SpawnerRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        if (state.displayEntity != null) {
            SpawnerRenderer.submitEntityInSpawner(poseStack, submitNodeCollector, state.displayEntity, this.entityRenderer, state.spin, state.scale, camera);
        }
    }
}