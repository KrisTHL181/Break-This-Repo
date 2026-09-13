package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SubmitNodeStorage implements SubmitNodeCollector {
   private final Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder = new Int2ObjectAVLTreeMap();

   public SubmitNodeCollection order(int p_430976_) {
      return (SubmitNodeCollection)this.submitsPerOrder.computeIfAbsent(p_430976_, p_430484_ -> new SubmitNodeCollection(this));
   }

   @Override
   public void submitShadow(PoseStack p_429483_, float p_426162_, List<EntityRenderState.ShadowPiece> p_428363_) {
      this.order(0).submitShadow(p_429483_, p_426162_, p_428363_);
   }

   @Override
   public void submitNameTag(
      PoseStack p_429725_,
      @Nullable Vec3 p_425387_,
      int p_431263_,
      Component p_426988_,
      boolean p_430194_,
      int p_429999_,
      double p_423666_,
      CameraRenderState p_426718_
   ) {
      this.order(0).submitNameTag(p_429725_, p_425387_, p_431263_, p_426988_, p_430194_, p_429999_, p_423666_, p_426718_);
   }

   @Override
   public void submitText(
      PoseStack p_428309_,
      float p_430474_,
      float p_427923_,
      FormattedCharSequence p_429347_,
      boolean p_427914_,
      Font.DisplayMode p_423431_,
      int p_429868_,
      int p_423689_,
      int p_424896_,
      int p_430829_
   ) {
      this.order(0).submitText(p_428309_, p_430474_, p_427923_, p_429347_, p_427914_, p_423431_, p_429868_, p_423689_, p_424896_, p_430829_);
   }

   @Override
   public void submitFlame(PoseStack p_429574_, EntityRenderState p_425014_, Quaternionf p_427721_) {
      this.order(0).submitFlame(p_429574_, p_425014_, p_427721_);
   }

   @Override
   public void submitLeash(PoseStack p_425730_, EntityRenderState.LeashState p_429134_) {
      this.order(0).submitLeash(p_425730_, p_429134_);
   }

   @Override
   public <S> void submitModel(
      Model<? super S> p_430836_,
      S p_431574_,
      PoseStack p_429440_,
      RenderType p_459985_,
      int p_424805_,
      int p_425107_,
      int p_423377_,
      @Nullable TextureAtlasSprite p_426883_,
      int p_426628_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_431344_
   ) {
      this.order(0).submitModel(p_430836_, p_431574_, p_429440_, p_459985_, p_424805_, p_425107_, p_423377_, p_426883_, p_426628_, p_431344_);
   }

   @Override
   public void submitModelPart(
      ModelPart p_429685_,
      PoseStack p_430296_,
      RenderType p_455127_,
      int p_422930_,
      int p_429845_,
      @Nullable TextureAtlasSprite p_428170_,
      boolean p_426549_,
      boolean p_426000_,
      int p_422790_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_428539_,
      int p_431905_
   ) {
      this.order(0).submitModelPart(p_429685_, p_430296_, p_455127_, p_422930_, p_429845_, p_428170_, p_426549_, p_426000_, p_422790_, p_428539_, p_431905_);
   }

   @Override
   public void submitBlock(PoseStack p_430235_, BlockState p_426291_, int p_422415_, int p_425164_, int p_425333_) {
      this.order(0).submitBlock(p_430235_, p_426291_, p_422415_, p_425164_, p_425333_);
   }

   @Override
   public void submitMovingBlock(PoseStack p_429296_, MovingBlockRenderState p_422884_) {
      this.order(0).submitMovingBlock(p_429296_, p_422884_);
   }

   @Override
   public void submitBlockModel(
      PoseStack p_431019_,
      RenderType p_458331_,
      BlockStateModel p_423693_,
      float p_425572_,
      float p_426359_,
      float p_429142_,
      int p_429693_,
      int p_427830_,
      int p_429931_
   ) {
      this.order(0).submitBlockModel(p_431019_, p_458331_, p_423693_, p_425572_, p_426359_, p_429142_, p_429693_, p_427830_, p_429931_);
   }

   @Override
   public void submitItem(
      PoseStack p_427166_,
      ItemDisplayContext p_426551_,
      int p_431446_,
      int p_427702_,
      int p_427528_,
      int[] p_424406_,
      List<BakedQuad> p_431271_,
      RenderType p_451048_,
      ItemStackRenderState.FoilType p_427510_
   ) {
      this.order(0).submitItem(p_427166_, p_426551_, p_431446_, p_427702_, p_427528_, p_424406_, p_431271_, p_451048_, p_427510_);
   }

   @Override
   public void submitCustomGeometry(PoseStack p_423586_, RenderType p_454393_, SubmitNodeCollector.CustomGeometryRenderer p_423841_) {
      this.order(0).submitCustomGeometry(p_423586_, p_454393_, p_423841_);
   }

   @Override
   public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer p_429053_) {
      this.order(0).submitParticleGroup(p_429053_);
   }

   public void clear() {
      this.submitsPerOrder.values().forEach(SubmitNodeCollection::clear);
   }

   public void endFrame() {
      this.submitsPerOrder.values().removeIf(p_423805_ -> !p_423805_.wasUsed());
      this.submitsPerOrder.values().forEach(SubmitNodeCollection::endFrame);
   }

   public Int2ObjectAVLTreeMap<SubmitNodeCollection> getSubmitsPerOrder() {
      return this.submitsPerOrder;
   }

   @OnlyIn(Dist.CLIENT)
   public record BlockModelSubmit(
      PoseStack.Pose pose, RenderType renderType, BlockStateModel model, float r, float g, float b, int lightCoords, int overlayCoords, int outlineColor
   ) {
   }

   @OnlyIn(Dist.CLIENT)
   public record BlockSubmit(PoseStack.Pose pose, BlockState state, int lightCoords, int overlayCoords, int outlineColor) {
   }

   @OnlyIn(Dist.CLIENT)
   public record CustomGeometrySubmit(PoseStack.Pose pose, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
   }

   @OnlyIn(Dist.CLIENT)
   public record FlameSubmit(PoseStack.Pose pose, EntityRenderState entityRenderState, Quaternionf rotation) {
   }

   @OnlyIn(Dist.CLIENT)
   public record ItemSubmit(
      PoseStack.Pose pose,
      ItemDisplayContext displayContext,
      int lightCoords,
      int overlayCoords,
      int outlineColor,
      int[] tintLayers,
      List<BakedQuad> quads,
      RenderType renderType,
      ItemStackRenderState.FoilType foilType
   ) {
   }

   @OnlyIn(Dist.CLIENT)
   public record LeashSubmit(Matrix4f pose, EntityRenderState.LeashState leashState) {
   }

   @OnlyIn(Dist.CLIENT)
   public record ModelPartSubmit(
      PoseStack.Pose pose,
      ModelPart modelPart,
      int lightCoords,
      int overlayCoords,
      @Nullable TextureAtlasSprite sprite,
      boolean sheeted,
      boolean hasFoil,
      int tintedColor,
      ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
      int outlineColor
   ) {
   }

   @OnlyIn(Dist.CLIENT)
   public record ModelSubmit<S>(
      PoseStack.Pose pose,
      Model<? super S> model,
      S state,
      int lightCoords,
      int overlayCoords,
      int tintedColor,
      @Nullable TextureAtlasSprite sprite,
      int outlineColor,
      ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
   ) {
   }

   @OnlyIn(Dist.CLIENT)
   public record MovingBlockSubmit(Matrix4f pose, MovingBlockRenderState movingBlockRenderState) {
   }

   @OnlyIn(Dist.CLIENT)
   public record NameTagSubmit(Matrix4f pose, float x, float y, Component text, int lightCoords, int color, int backgroundColor, double distanceToCameraSq) {
   }

   @OnlyIn(Dist.CLIENT)
   public record ShadowSubmit(Matrix4f pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {
   }

   @OnlyIn(Dist.CLIENT)
   public record TextSubmit(
      Matrix4f pose,
      float x,
      float y,
      FormattedCharSequence string,
      boolean dropShadow,
      Font.DisplayMode displayMode,
      int lightCoords,
      int color,
      int backgroundColor,
      int outlineColor
   ) {
   }

   @OnlyIn(Dist.CLIENT)
   public record TranslucentModelSubmit<S>(SubmitNodeStorage.ModelSubmit<S> modelSubmit, RenderType renderType, Vector3f position) {
   }
}
