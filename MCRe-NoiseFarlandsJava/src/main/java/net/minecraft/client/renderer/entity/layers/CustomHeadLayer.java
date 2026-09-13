package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomHeadLayer<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> extends RenderLayer<S, M> {
    private static final float ITEM_SCALE = 0.625F;
    private static final float SKULL_SCALE = 1.1875F;
    private final CustomHeadLayer.Transforms transforms;
    private final Function<SkullBlock.Type, SkullModelBase> skullModels;
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public CustomHeadLayer(final RenderLayerParent<S, M> renderer, final EntityModelSet modelSet, final PlayerSkinRenderCache playerSkinRenderCache) {
        this(renderer, modelSet, playerSkinRenderCache, CustomHeadLayer.Transforms.DEFAULT);
    }

    public CustomHeadLayer(
        final RenderLayerParent<S, M> renderer,
        final EntityModelSet modelSet,
        final PlayerSkinRenderCache playerSkinRenderCache,
        final CustomHeadLayer.Transforms transforms
    ) {
        super(renderer);
        this.transforms = transforms;
        this.skullModels = Util.memoize(type -> SkullBlockRenderer.createModel(modelSet, type));
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    public void submit(
        final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final S state, final float yRot, final float xRot
    ) {
        if (!state.headItem.isEmpty() || state.wornHeadType != null) {
            poseStack.pushPose();
            poseStack.scale(this.transforms.horizontalScale(), 1.0F, this.transforms.horizontalScale());
            M parentModel = this.getParentModel();
            parentModel.root().translateAndRotate(poseStack);
            parentModel.translateToHead(poseStack);
            if (state.wornHeadType != null) {
                poseStack.translate(0.0F, this.transforms.skullYOffset(), 0.0F);
                poseStack.scale(1.1875F, 1.1875F, 1.1875F);
                SkullBlock.Type type = state.wornHeadType;
                SkullModelBase skullModel = this.skullModels.apply(type);
                RenderType renderType = this.resolveSkullRenderType(state, type);
                SkullBlockRenderer.submitSkull(
                    state.wornHeadAnimationPos, poseStack, submitNodeCollector, lightCoords, skullModel, renderType, state.outlineColor, null
                );
            } else {
                translateToHead(poseStack, this.transforms);
                state.headItem.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            }

            poseStack.popPose();
        }
    }

    private RenderType resolveSkullRenderType(final LivingEntityRenderState state, final SkullBlock.Type type) {
        if (type == SkullBlock.Types.PLAYER) {
            ResolvableProfile profile = state.wornHeadProfile;
            if (profile != null) {
                return this.playerSkinRenderCache.getOrDefault(profile).renderType();
            }
        }

        return SkullBlockRenderer.getSkullRenderType(type, null);
    }

    public static void translateToHead(final PoseStack poseStack, final CustomHeadLayer.Transforms transforms) {
        poseStack.translate(0.0F, -0.25F + transforms.yOffset(), 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.625F, -0.625F, -0.625F);
    }

    @OnlyIn(Dist.CLIENT)
    public record Transforms(float yOffset, float skullYOffset, float horizontalScale) {
        public static final CustomHeadLayer.Transforms DEFAULT = new CustomHeadLayer.Transforms(0.0F, 0.0F, 1.0F);
    }
}