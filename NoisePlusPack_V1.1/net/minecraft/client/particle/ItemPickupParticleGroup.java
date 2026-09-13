package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemPickupParticleGroup extends ParticleGroup<ItemPickupParticle> {
   public ItemPickupParticleGroup(ParticleEngine p_426758_) {
      super(p_426758_);
   }

   @Override
   public ParticleGroupRenderState extractRenderState(Frustum p_425972_, Camera p_425384_, float p_431552_) {
      return new ItemPickupParticleGroup.State(
         this.particles.stream().map(p_429820_ -> ItemPickupParticleGroup.ParticleInstance.fromParticle(p_429820_, p_425384_, p_431552_)).toList()
      );
   }

   @OnlyIn(Dist.CLIENT)
   record ParticleInstance(EntityRenderState itemRenderState, double xOffset, double yOffset, double zOffset) {
      public static ItemPickupParticleGroup.ParticleInstance fromParticle(ItemPickupParticle p_428662_, Camera p_423054_, float p_430102_) {
         float f = (p_428662_.life + p_430102_) / 3.0F;
         f *= f;
         double d0 = Mth.lerp(p_430102_, p_428662_.targetXOld, p_428662_.targetX);
         double d1 = Mth.lerp(p_430102_, p_428662_.targetYOld, p_428662_.targetY);
         double d2 = Mth.lerp(p_430102_, p_428662_.targetZOld, p_428662_.targetZ);
         double d3 = Mth.lerp(f, p_428662_.itemRenderState.x, d0);
         double d4 = Mth.lerp(f, p_428662_.itemRenderState.y, d1);
         double d5 = Mth.lerp(f, p_428662_.itemRenderState.z, d2);
         Vec3 vec3 = p_423054_.position();
         return new ItemPickupParticleGroup.ParticleInstance(p_428662_.itemRenderState, d3 - vec3.x(), d4 - vec3.y(), d5 - vec3.z());
      }
   }

   @OnlyIn(Dist.CLIENT)
   record State(List<ItemPickupParticleGroup.ParticleInstance> instances) implements ParticleGroupRenderState {
      @Override
      public void submit(SubmitNodeCollector p_427784_, CameraRenderState p_431324_) {
         PoseStack posestack = new PoseStack();
         EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

         for (ItemPickupParticleGroup.ParticleInstance itempickupparticlegroup$particleinstance : this.instances) {
            entityrenderdispatcher.submit(
               itempickupparticlegroup$particleinstance.itemRenderState,
               p_431324_,
               itempickupparticlegroup$particleinstance.xOffset,
               itempickupparticlegroup$particleinstance.yOffset,
               itempickupparticlegroup$particleinstance.zOffset,
               posestack,
               p_427784_
            );
         }
      }
   }
}
