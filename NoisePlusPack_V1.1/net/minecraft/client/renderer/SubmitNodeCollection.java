package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
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
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SubmitNodeCollection implements OrderedSubmitNodeCollector {
   private final List<SubmitNodeStorage.ShadowSubmit> shadowSubmits = new ArrayList<>();
   private final List<SubmitNodeStorage.FlameSubmit> flameSubmits = new ArrayList<>();
   private final NameTagFeatureRenderer.Storage nameTagSubmits = new NameTagFeatureRenderer.Storage();
   private final List<SubmitNodeStorage.TextSubmit> textSubmits = new ArrayList<>();
   private final List<SubmitNodeStorage.LeashSubmit> leashSubmits = new ArrayList<>();
   private final List<SubmitNodeStorage.BlockSubmit> blockSubmits = new ArrayList<>();
   private final List<SubmitNodeStorage.MovingBlockSubmit> movingBlockSubmits = new ArrayList<>();
   private final List<SubmitNodeStorage.BlockModelSubmit> blockModelSubmits = new ArrayList<>();
   private final List<SubmitNodeStorage.ItemSubmit> itemSubmits = new ArrayList<>();
   private final List<SubmitNodeCollector.ParticleGroupRenderer> particleGroupRenderers = new ArrayList<>();
   private final ModelFeatureRenderer.Storage modelSubmits = new ModelFeatureRenderer.Storage();
   private final ModelPartFeatureRenderer.Storage modelPartSubmits = new ModelPartFeatureRenderer.Storage();
   private final CustomFeatureRenderer.Storage customGeometrySubmits = new CustomFeatureRenderer.Storage();
   private final SubmitNodeStorage submitNodeStorage;
   private boolean wasUsed = false;

   public SubmitNodeCollection(SubmitNodeStorage p_430945_) {
      this.submitNodeStorage = p_430945_;
   }

   @Override
   public void submitShadow(PoseStack p_429767_, float p_422683_, List<EntityRenderState.ShadowPiece> p_426531_) {
      this.wasUsed = true;
      PoseStack.Pose posestack$pose = p_429767_.last();
      this.shadowSubmits.add(new SubmitNodeStorage.ShadowSubmit(new Matrix4f(posestack$pose.pose()), p_422683_, p_426531_));
   }

   @Override
   public void submitNameTag(
      PoseStack p_424812_,
      @Nullable Vec3 p_430465_,
      int p_430931_,
      Component p_427694_,
      boolean p_429006_,
      int p_426243_,
      double p_426713_,
      CameraRenderState p_427678_
   ) {
      this.wasUsed = true;
      this.nameTagSubmits.add(p_424812_, p_430465_, p_430931_, p_427694_, p_429006_, p_426243_, p_426713_, p_427678_);
   }

   @Override
   public void submitText(
      PoseStack p_428541_,
      float p_428691_,
      float p_425752_,
      FormattedCharSequence p_430334_,
      boolean p_422404_,
      Font.DisplayMode p_424005_,
      int p_429126_,
      int p_426500_,
      int p_423149_,
      int p_427742_
   ) {
      this.wasUsed = true;
      this.textSubmits
         .add(
            new SubmitNodeStorage.TextSubmit(
               new Matrix4f(p_428541_.last().pose()), p_428691_, p_425752_, p_430334_, p_422404_, p_424005_, p_429126_, p_426500_, p_423149_, p_427742_
            )
         );
   }

   @Override
   public void submitFlame(PoseStack p_426622_, EntityRenderState p_428524_, Quaternionf p_429669_) {
      this.wasUsed = true;
      this.flameSubmits.add(new SubmitNodeStorage.FlameSubmit(p_426622_.last().copy(), p_428524_, p_429669_));
   }

   @Override
   public void submitLeash(PoseStack p_422891_, EntityRenderState.LeashState p_425552_) {
      this.wasUsed = true;
      this.leashSubmits.add(new SubmitNodeStorage.LeashSubmit(new Matrix4f(p_422891_.last().pose()), p_425552_));
   }

   @Override
   public <S> void submitModel(
      Model<? super S> p_427484_,
      S p_422625_,
      PoseStack p_424450_,
      RenderType p_452018_,
      int p_426738_,
      int p_424372_,
      int p_423869_,
      @Nullable TextureAtlasSprite p_423183_,
      int p_426567_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_424580_
   ) {
      this.wasUsed = true;
      SubmitNodeStorage.ModelSubmit<S> modelsubmit = new SubmitNodeStorage.ModelSubmit<>(
         p_424450_.last().copy(), p_427484_, p_422625_, p_426738_, p_424372_, p_423869_, p_423183_, p_426567_, p_424580_
      );
      this.modelSubmits.add(p_452018_, modelsubmit);
   }

   @Override
   public void submitModelPart(
      ModelPart p_424958_,
      PoseStack p_427414_,
      RenderType p_460361_,
      int p_428290_,
      int p_426659_,
      @Nullable TextureAtlasSprite p_430957_,
      boolean p_423376_,
      boolean p_422462_,
      int p_425817_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_430912_,
      int p_431872_
   ) {
      this.wasUsed = true;
      this.modelPartSubmits
         .add(
            p_460361_,
            new SubmitNodeStorage.ModelPartSubmit(
               p_427414_.last().copy(), p_424958_, p_428290_, p_426659_, p_430957_, p_423376_, p_422462_, p_425817_, p_430912_, p_431872_
            )
         );
   }

   @Override
   public void submitBlock(PoseStack p_431098_, BlockState p_427916_, int p_424531_, int p_423181_, int p_423712_) {
      this.wasUsed = true;
      this.blockSubmits.add(new SubmitNodeStorage.BlockSubmit(p_431098_.last().copy(), p_427916_, p_424531_, p_423181_, p_423712_));
      Minecraft.getInstance()
         .getModelManager()
         .specialBlockModelRenderer()
         .renderByBlock(p_427916_.getBlock(), ItemDisplayContext.NONE, p_431098_, this.submitNodeStorage, p_424531_, p_423181_, p_423712_);
   }

   @Override
   public void submitMovingBlock(PoseStack p_426301_, MovingBlockRenderState p_423997_) {
      this.wasUsed = true;
      this.movingBlockSubmits.add(new SubmitNodeStorage.MovingBlockSubmit(new Matrix4f(p_426301_.last().pose()), p_423997_));
   }

   @Override
   public void submitBlockModel(
      PoseStack p_426724_,
      RenderType p_457003_,
      BlockStateModel p_430042_,
      float p_426641_,
      float p_429457_,
      float p_423466_,
      int p_422478_,
      int p_423902_,
      int p_424810_
   ) {
      this.wasUsed = true;
      this.blockModelSubmits
         .add(
            new SubmitNodeStorage.BlockModelSubmit(
               p_426724_.last().copy(), p_457003_, p_430042_, p_426641_, p_429457_, p_423466_, p_422478_, p_423902_, p_424810_
            )
         );
   }

   @Override
   public void submitItem(
      PoseStack p_427089_,
      ItemDisplayContext p_422921_,
      int p_425776_,
      int p_428806_,
      int p_431657_,
      int[] p_426729_,
      List<BakedQuad> p_425892_,
      RenderType p_452253_,
      ItemStackRenderState.FoilType p_428652_
   ) {
      this.wasUsed = true;
      this.itemSubmits
         .add(new SubmitNodeStorage.ItemSubmit(p_427089_.last().copy(), p_422921_, p_425776_, p_428806_, p_431657_, p_426729_, p_425892_, p_452253_, p_428652_));
   }

   @Override
   public void submitCustomGeometry(PoseStack p_431589_, RenderType p_450823_, SubmitNodeCollector.CustomGeometryRenderer p_431193_) {
      this.wasUsed = true;
      this.customGeometrySubmits.add(p_431589_, p_450823_, p_431193_);
   }

   @Override
   public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer p_430700_) {
      this.wasUsed = true;
      this.particleGroupRenderers.add(p_430700_);
   }

   public List<SubmitNodeStorage.ShadowSubmit> getShadowSubmits() {
      return this.shadowSubmits;
   }

   public List<SubmitNodeStorage.FlameSubmit> getFlameSubmits() {
      return this.flameSubmits;
   }

   public NameTagFeatureRenderer.Storage getNameTagSubmits() {
      return this.nameTagSubmits;
   }

   public List<SubmitNodeStorage.TextSubmit> getTextSubmits() {
      return this.textSubmits;
   }

   public List<SubmitNodeStorage.LeashSubmit> getLeashSubmits() {
      return this.leashSubmits;
   }

   public List<SubmitNodeStorage.BlockSubmit> getBlockSubmits() {
      return this.blockSubmits;
   }

   public List<SubmitNodeStorage.MovingBlockSubmit> getMovingBlockSubmits() {
      return this.movingBlockSubmits;
   }

   public List<SubmitNodeStorage.BlockModelSubmit> getBlockModelSubmits() {
      return this.blockModelSubmits;
   }

   public ModelPartFeatureRenderer.Storage getModelPartSubmits() {
      return this.modelPartSubmits;
   }

   public List<SubmitNodeStorage.ItemSubmit> getItemSubmits() {
      return this.itemSubmits;
   }

   public List<SubmitNodeCollector.ParticleGroupRenderer> getParticleGroupRenderers() {
      return this.particleGroupRenderers;
   }

   public ModelFeatureRenderer.Storage getModelSubmits() {
      return this.modelSubmits;
   }

   public CustomFeatureRenderer.Storage getCustomGeometrySubmits() {
      return this.customGeometrySubmits;
   }

   public boolean wasUsed() {
      return this.wasUsed;
   }

   public void clear() {
      this.shadowSubmits.clear();
      this.flameSubmits.clear();
      this.nameTagSubmits.clear();
      this.textSubmits.clear();
      this.leashSubmits.clear();
      this.blockSubmits.clear();
      this.movingBlockSubmits.clear();
      this.blockModelSubmits.clear();
      this.itemSubmits.clear();
      this.particleGroupRenderers.clear();
      this.modelSubmits.clear();
      this.customGeometrySubmits.clear();
      this.modelPartSubmits.clear();
   }

   public void endFrame() {
      this.modelSubmits.endFrame();
      this.modelPartSubmits.endFrame();
      this.customGeometrySubmits.endFrame();
      this.wasUsed = false;
   }
}
