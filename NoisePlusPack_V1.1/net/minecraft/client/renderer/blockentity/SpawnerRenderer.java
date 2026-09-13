package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SpawnerRenderer implements BlockEntityRenderer<SpawnerBlockEntity, SpawnerRenderState> {
   private final EntityRenderDispatcher entityRenderer;

   public SpawnerRenderer(BlockEntityRendererProvider.Context p_173673_) {
      this.entityRenderer = p_173673_.entityRenderer();
   }

   public SpawnerRenderState createRenderState() {
      return new SpawnerRenderState();
   }

   public void extractRenderState(
      SpawnerBlockEntity p_428697_, SpawnerRenderState p_425461_, float p_431520_, Vec3 p_424506_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_427855_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_428697_, p_425461_, p_431520_, p_424506_, p_427855_);
      if (p_428697_.getLevel() != null) {
         BaseSpawner basespawner = p_428697_.getSpawner();
         Entity entity = basespawner.getOrCreateDisplayEntity(p_428697_.getLevel(), p_428697_.getBlockPos());
         TrialSpawnerRenderer.extractSpawnerData(p_425461_, p_431520_, entity, this.entityRenderer, basespawner.getOSpin(), basespawner.getSpin());
      }
   }

   public void submit(SpawnerRenderState p_431426_, PoseStack p_426807_, SubmitNodeCollector p_425864_, CameraRenderState p_427406_) {
      if (p_431426_.displayEntity != null) {
         submitEntityInSpawner(p_426807_, p_425864_, p_431426_.displayEntity, this.entityRenderer, p_431426_.spin, p_431426_.scale, p_427406_);
      }
   }

   public static void submitEntityInSpawner(
      PoseStack p_423723_,
      SubmitNodeCollector p_425111_,
      EntityRenderState p_428211_,
      EntityRenderDispatcher p_425589_,
      float p_430598_,
      float p_430747_,
      CameraRenderState p_429467_
   ) {
      p_423723_.pushPose();
      p_423723_.translate(0.5F, 0.4F, 0.5F);
      p_423723_.mulPose(Axis.YP.rotationDegrees(p_430598_));
      p_423723_.translate(0.0F, -0.2F, 0.0F);
      p_423723_.mulPose(Axis.XP.rotationDegrees(-30.0F));
      p_423723_.scale(p_430747_, p_430747_, p_430747_);
      p_425589_.submit(p_428211_, p_429467_, 0.0, 0.0, 0.0, p_423723_, p_425111_);
      p_423723_.popPose();
   }
}
