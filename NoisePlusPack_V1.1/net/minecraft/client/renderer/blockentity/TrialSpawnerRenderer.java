package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
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

   public TrialSpawnerRenderer(BlockEntityRendererProvider.Context p_311333_) {
      this.entityRenderer = p_311333_.entityRenderer();
   }

   public SpawnerRenderState createRenderState() {
      return new SpawnerRenderState();
   }

   public void extractRenderState(
      TrialSpawnerBlockEntity p_424929_,
      SpawnerRenderState p_430977_,
      float p_424952_,
      Vec3 p_426405_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_423619_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_424929_, p_430977_, p_424952_, p_426405_, p_423619_);
      if (p_424929_.getLevel() != null) {
         TrialSpawner trialspawner = p_424929_.getTrialSpawner();
         TrialSpawnerStateData trialspawnerstatedata = trialspawner.getStateData();
         Entity entity = trialspawnerstatedata.getOrCreateDisplayEntity(trialspawner, p_424929_.getLevel(), trialspawner.getState());
         extractSpawnerData(p_430977_, p_424952_, entity, this.entityRenderer, trialspawnerstatedata.getOSpin(), trialspawnerstatedata.getSpin());
      }
   }

   static void extractSpawnerData(
      SpawnerRenderState p_430658_, float p_423837_, @Nullable Entity p_429987_, EntityRenderDispatcher p_426080_, double p_424568_, double p_423202_
   ) {
      if (p_429987_ != null) {
         p_430658_.displayEntity = p_426080_.extractEntity(p_429987_, p_423837_);
         p_430658_.displayEntity.lightCoords = p_430658_.lightCoords;
         p_430658_.spin = (float)Mth.lerp(p_423837_, p_424568_, p_423202_) * 10.0F;
         p_430658_.scale = 0.53125F;
         float f = Math.max(p_429987_.getBbWidth(), p_429987_.getBbHeight());
         if (f > 1.0) {
            p_430658_.scale /= f;
         }
      }
   }

   public void submit(SpawnerRenderState p_428741_, PoseStack p_424539_, SubmitNodeCollector p_427723_, CameraRenderState p_430794_) {
      if (p_428741_.displayEntity != null) {
         SpawnerRenderer.submitEntityInSpawner(p_424539_, p_427723_, p_428741_.displayEntity, this.entityRenderer, p_428741_.spin, p_428741_.scale, p_430794_);
      }
   }
}
