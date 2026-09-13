package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SquidRenderer<T extends Squid> extends AgeableMobRenderer<T, SquidRenderState, SquidModel> {
   private static final Identifier SQUID_LOCATION = Identifier.withDefaultNamespace("textures/entity/squid/squid.png");

   public SquidRenderer(EntityRendererProvider.Context p_174406_, SquidModel p_457590_, SquidModel p_460762_) {
      super(p_174406_, p_457590_, p_460762_, 0.7F);
   }

   public Identifier getTextureLocation(SquidRenderState p_460605_) {
      return SQUID_LOCATION;
   }

   public SquidRenderState createRenderState() {
      return new SquidRenderState();
   }

   public void extractRenderState(T p_459853_, SquidRenderState p_361362_, float p_367215_) {
      super.extractRenderState(p_459853_, p_361362_, p_367215_);
      p_361362_.tentacleAngle = Mth.lerp(p_367215_, p_459853_.oldTentacleAngle, p_459853_.tentacleAngle);
      p_361362_.xBodyRot = Mth.lerp(p_367215_, p_459853_.xBodyRotO, p_459853_.xBodyRot);
      p_361362_.zBodyRot = Mth.lerp(p_367215_, p_459853_.zBodyRotO, p_459853_.zBodyRot);
   }

   protected void setupRotations(SquidRenderState p_361221_, PoseStack p_116025_, float p_116026_, float p_116027_) {
      p_116025_.translate(0.0F, p_361221_.isBaby ? 0.25F : 0.5F, 0.0F);
      p_116025_.mulPose(Axis.YP.rotationDegrees(180.0F - p_116026_));
      p_116025_.mulPose(Axis.XP.rotationDegrees(p_361221_.xBodyRot));
      p_116025_.mulPose(Axis.YP.rotationDegrees(p_361221_.zBodyRot));
      p_116025_.translate(0.0F, p_361221_.isBaby ? -0.6F : -1.2F, 0.0F);
   }
}
