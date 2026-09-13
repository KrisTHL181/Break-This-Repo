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

public class WitherBossRenderer extends MobRenderer<WitherBoss, WitherRenderState, WitherBossModel> {
   private static final Identifier WITHER_INVULNERABLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
   private static final Identifier WITHER_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither.png");

   public WitherBossRenderer(final EntityRendererProvider.Context context) {
      super(context, new WitherBossModel(context.bakeLayer(ModelLayers.WITHER)), 1.0F);
      this.addLayer(new WitherArmorLayer(this, context.getModelSet()));
   }

   protected int getBlockLightLevel(final WitherBoss entity, final BlockPos blockPos) {
      return 15;
   }

   public Identifier getTextureLocation(final WitherRenderState state) {
      int invulnerableTicks = Mth.floor(state.invulnerableTicks);
      return invulnerableTicks > 0 && (invulnerableTicks > 80 || invulnerableTicks / 5 % 2 != 1) ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
   }

   public WitherRenderState createRenderState() {
      return new WitherRenderState();
   }

   protected void scale(final WitherRenderState state, final PoseStack poseStack) {
      float scale = 2.0F;
      if (state.invulnerableTicks > 0.0F) {
         scale -= state.invulnerableTicks / 220.0F * 0.5F;
      }

      poseStack.scale(scale, scale, scale);
   }

   public void extractRenderState(final WitherBoss entity, final WitherRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      int invulnerableTicks = entity.getInvulnerableTicks();
      state.invulnerableTicks = invulnerableTicks > 0 ? invulnerableTicks - partialTicks : 0.0F;
      System.arraycopy(entity.getHeadXRots(), 0, state.xHeadRots, 0, state.xHeadRots.length);
      System.arraycopy(entity.getHeadYRots(), 0, state.yHeadRots, 0, state.yHeadRots.length);
      state.isPowered = entity.isPowered();
   }
}
