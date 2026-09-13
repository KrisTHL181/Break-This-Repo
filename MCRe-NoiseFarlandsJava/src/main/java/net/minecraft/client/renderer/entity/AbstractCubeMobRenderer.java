package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractCubeMobRenderer<T extends AbstractCubeMob, S extends SlimeRenderState, M extends EntityModel<? super S>>
    extends MobRenderer<T, S, M> {
    public AbstractCubeMobRenderer(final EntityRendererProvider.Context context, final M model) {
        super(context, model, 0.25F);
    }

    protected float getShadowRadius(final SlimeRenderState state) {
        return state.size * 0.25F;
    }

    protected void scale(final S state, final PoseStack poseStack) {
        super.scale(state, poseStack);
        this.applySizeAndSquish(state, poseStack);
    }

    protected void downscaleSlightly(final PoseStack poseStack) {
        float s = 0.999F;
        poseStack.scale(0.999F, 0.999F, 0.999F);
        poseStack.translate(0.0F, 0.001F, 0.0F);
    }

    protected void applySizeAndSquish(final S state, final PoseStack poseStack) {
        float size = state.size;
        float ss = state.squish / (size * 0.5F + 1.0F);
        float w = 1.0F / (ss + 1.0F);
        poseStack.scale(w * size, 1.0F / w * size, w * size);
    }

    public void extractRenderState(final T entity, final S state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.squish = Mth.lerp(partialTicks, entity.oSquish, entity.squish);
        state.size = entity.getSize();
    }
}