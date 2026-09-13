package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface OrderedSubmitNodeCollector {
   void submitShadow(PoseStack var1, float var2, List<EntityRenderState.ShadowPiece> var3);

   void submitNameTag(PoseStack var1, @Nullable Vec3 var2, int var3, Component var4, boolean var5, int var6, double var7, CameraRenderState var9);

   void submitText(
      PoseStack var1, float var2, float var3, FormattedCharSequence var4, boolean var5, Font.DisplayMode var6, int var7, int var8, int var9, int var10
   );

   void submitFlame(PoseStack var1, EntityRenderState var2, Quaternionf var3);

   void submitLeash(PoseStack var1, EntityRenderState.LeashState var2);

   <S> void submitModel(
      Model<? super S> var1,
      S var2,
      PoseStack var3,
      RenderType var4,
      int var5,
      int var6,
      int var7,
      @Nullable TextureAtlasSprite var8,
      int var9,
      ModelFeatureRenderer.@Nullable CrumblingOverlay var10
   );

   default <S> void submitModel(
      Model<? super S> p_423531_,
      S p_423172_,
      PoseStack p_430909_,
      RenderType p_452425_,
      int p_429653_,
      int p_427844_,
      int p_429198_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_428470_
   ) {
      this.submitModel(p_423531_, p_423172_, p_430909_, p_452425_, p_429653_, p_427844_, -1, null, p_429198_, p_428470_);
   }

   default void submitModelPart(
      ModelPart p_428756_, PoseStack p_425252_, RenderType p_460672_, int p_427164_, int p_422744_, @Nullable TextureAtlasSprite p_425811_
   ) {
      this.submitModelPart(p_428756_, p_425252_, p_460672_, p_427164_, p_422744_, p_425811_, false, false, -1, null, 0);
   }

   default void submitModelPart(
      ModelPart p_422542_,
      PoseStack p_423411_,
      RenderType p_457471_,
      int p_430083_,
      int p_423063_,
      @Nullable TextureAtlasSprite p_430651_,
      int p_453977_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_453259_
   ) {
      this.submitModelPart(p_422542_, p_423411_, p_457471_, p_430083_, p_423063_, p_430651_, false, false, p_453977_, p_453259_, 0);
   }

   default void submitModelPart(
      ModelPart p_428070_,
      PoseStack p_431622_,
      RenderType p_460296_,
      int p_424143_,
      int p_431591_,
      @Nullable TextureAtlasSprite p_425071_,
      boolean p_454543_,
      boolean p_450949_
   ) {
      this.submitModelPart(p_428070_, p_431622_, p_460296_, p_424143_, p_431591_, p_425071_, p_454543_, p_450949_, -1, null, 0);
   }

   void submitModelPart(
      ModelPart var1,
      PoseStack var2,
      RenderType var3,
      int var4,
      int var5,
      @Nullable TextureAtlasSprite var6,
      boolean var7,
      boolean var8,
      int var9,
      ModelFeatureRenderer.@Nullable CrumblingOverlay var10,
      int var11
   );

   void submitBlock(PoseStack var1, BlockState var2, int var3, int var4, int var5);

   void submitMovingBlock(PoseStack var1, MovingBlockRenderState var2);

   void submitBlockModel(PoseStack var1, RenderType var2, BlockStateModel var3, float var4, float var5, float var6, int var7, int var8, int var9);

   void submitItem(
      PoseStack var1,
      ItemDisplayContext var2,
      int var3,
      int var4,
      int var5,
      int[] var6,
      List<BakedQuad> var7,
      RenderType var8,
      ItemStackRenderState.FoilType var9
   );

   void submitCustomGeometry(PoseStack var1, RenderType var2, SubmitNodeCollector.CustomGeometryRenderer var3);

   void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer var1);
}
