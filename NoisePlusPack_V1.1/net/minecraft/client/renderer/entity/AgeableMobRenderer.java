package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@Deprecated
@OnlyIn(Dist.CLIENT)
public abstract class AgeableMobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends MobRenderer<T, S, M> {
   private final M adultModel;
   private final M babyModel;

   public AgeableMobRenderer(EntityRendererProvider.Context p_367262_, M p_369985_, M p_363518_, float p_363526_) {
      super(p_367262_, p_369985_, p_363526_);
      this.adultModel = p_369985_;
      this.babyModel = p_363518_;
   }

   @Override
   public void submit(S p_429008_, PoseStack p_429473_, SubmitNodeCollector p_423480_, CameraRenderState p_425502_) {
      this.model = p_429008_.isBaby ? this.babyModel : this.adultModel;
      super.submit(p_429008_, p_429473_, p_423480_, p_425502_);
   }
}
