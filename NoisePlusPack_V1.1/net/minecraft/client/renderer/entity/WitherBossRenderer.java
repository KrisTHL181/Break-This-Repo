package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherBossRenderer extends MobRenderer<WitherBoss, WitherRenderState, WitherBossModel> {
   private static final Identifier WITHER_INVULNERABLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
   private static final Identifier WITHER_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither.png");

   public WitherBossRenderer(EntityRendererProvider.Context p_174445_) {
      super(p_174445_, new WitherBossModel(p_174445_.bakeLayer(ModelLayers.WITHER)), 1.0F);
      this.addLayer(new WitherArmorLayer(this, p_174445_.getModelSet()));
   }

   protected int getBlockLightLevel(WitherBoss p_116443_, BlockPos p_116444_) {
      return 15;
   }

   public Identifier getTextureLocation(WitherRenderState p_368277_) {
      int i = Mth.floor(p_368277_.invulnerableTicks);
      return i <= 0 || i <= 80 && i / 5 % 2 == 1 ? WITHER_LOCATION : WITHER_INVULNERABLE_LOCATION;
   }

   public WitherRenderState createRenderState() {
      return new WitherRenderState();
   }

   protected void scale(WitherRenderState p_370233_, PoseStack p_116434_) {
      float f = 2.0F;
      if (p_370233_.invulnerableTicks > 0.0F) {
         f -= p_370233_.invulnerableTicks / 220.0F * 0.5F;
      }

      p_116434_.scale(f, f, f);
   }

   public void extractRenderState(WitherBoss p_369559_, WitherRenderState p_363159_, float p_363731_) {
      super.extractRenderState(p_369559_, p_363159_, p_363731_);
      int i = p_369559_.getInvulnerableTicks();
      p_363159_.invulnerableTicks = i > 0 ? i - p_363731_ : 0.0F;
      System.arraycopy(p_369559_.getHeadXRots(), 0, p_363159_.xHeadRots, 0, p_363159_.xHeadRots.length);
      System.arraycopy(p_369559_.getHeadYRots(), 0, p_363159_.yHeadRots, 0, p_363159_.yHeadRots.length);
      p_363159_.isPowered = p_369559_.isPowered();
   }
}
