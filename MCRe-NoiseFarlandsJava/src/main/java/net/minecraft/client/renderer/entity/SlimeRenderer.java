package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SlimeRenderer extends AbstractCubeMobRenderer<Slime, SlimeRenderState, SlimeModel> {
    public static final Identifier SLIME_LOCATION = Identifier.withDefaultNamespace("textures/entity/slime/slime.png");

    public SlimeRenderer(final EntityRendererProvider.Context context) {
        super(context, new SlimeModel(context.bakeLayer(ModelLayers.SLIME)));
        this.addLayer(new SlimeOuterLayer(this, context.getModelSet()));
    }

    @Override
    protected void scale(final SlimeRenderState state, final PoseStack poseStack) {
        this.downscaleSlightly(poseStack);
        super.scale(state, poseStack);
    }

    public Identifier getTextureLocation(final SlimeRenderState state) {
        return SLIME_LOCATION;
    }

    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }
}