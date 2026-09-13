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

   public CustomHeadLayer(RenderLayerParent<S, M> p_234822_, EntityModelSet p_234823_, PlayerSkinRenderCache p_422915_) {
      this(p_234822_, p_234823_, p_422915_, CustomHeadLayer.Transforms.DEFAULT);
   }

   public CustomHeadLayer(RenderLayerParent<S, M> p_234829_, EntityModelSet p_234830_, PlayerSkinRenderCache p_426986_, CustomHeadLayer.Transforms p_377766_) {
      super(p_234829_);
      this.transforms = p_377766_;
      this.skullModels = Util.memoize(p_448342_ -> SkullBlockRenderer.createModel(p_234830_, p_448342_));
      this.playerSkinRenderCache = p_426986_;
   }

   public void submit(PoseStack p_430594_, SubmitNodeCollector p_424787_, int p_428319_, S p_423337_, float p_425556_, float p_428582_) {
      if (!p_423337_.headItem.isEmpty() || p_423337_.wornHeadType != null) {
         p_430594_.pushPose();
         p_430594_.scale(this.transforms.horizontalScale(), 1.0F, this.transforms.horizontalScale());
         M m = this.getParentModel();
         m.root().translateAndRotate(p_430594_);
         m.translateToHead(p_430594_);
         if (p_423337_.wornHeadType != null) {
            p_430594_.translate(0.0F, this.transforms.skullYOffset(), 0.0F);
            p_430594_.scale(1.1875F, -1.1875F, -1.1875F);
            p_430594_.translate(-0.5, 0.0, -0.5);
            SkullBlock.Type skullblock$type = p_423337_.wornHeadType;
            SkullModelBase skullmodelbase = this.skullModels.apply(skullblock$type);
            RenderType rendertype = this.resolveSkullRenderType(p_423337_, skullblock$type);
            SkullBlockRenderer.submitSkull(
               null, 180.0F, p_423337_.wornHeadAnimationPos, p_430594_, p_424787_, p_428319_, skullmodelbase, rendertype, p_423337_.outlineColor, null
            );
         } else {
            translateToHead(p_430594_, this.transforms);
            p_423337_.headItem.submit(p_430594_, p_424787_, p_428319_, OverlayTexture.NO_OVERLAY, p_423337_.outlineColor);
         }

         p_430594_.popPose();
      }
   }

   private RenderType resolveSkullRenderType(LivingEntityRenderState p_431568_, SkullBlock.Type p_426369_) {
      if (p_426369_ == SkullBlock.Types.PLAYER) {
         ResolvableProfile resolvableprofile = p_431568_.wornHeadProfile;
         if (resolvableprofile != null) {
            return this.playerSkinRenderCache.getOrDefault(resolvableprofile).renderType();
         }
      }

      return SkullBlockRenderer.getSkullRenderType(p_426369_, null);
   }

   public static void translateToHead(PoseStack p_174484_, CustomHeadLayer.Transforms p_366424_) {
      p_174484_.translate(0.0F, -0.25F + p_366424_.yOffset(), 0.0F);
      p_174484_.mulPose(Axis.YP.rotationDegrees(180.0F));
      p_174484_.scale(0.625F, -0.625F, -0.625F);
   }

   @OnlyIn(Dist.CLIENT)
   public record Transforms(float yOffset, float skullYOffset, float horizontalScale) {
      public static final CustomHeadLayer.Transforms DEFAULT = new CustomHeadLayer.Transforms(0.0F, 0.0F, 1.0F);
   }
}
