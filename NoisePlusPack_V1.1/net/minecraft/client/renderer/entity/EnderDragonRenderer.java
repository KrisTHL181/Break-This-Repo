package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class EnderDragonRenderer extends EntityRenderer<EnderDragon, EnderDragonRenderState> {
   public static final Identifier CRYSTAL_BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal_beam.png");
   private static final Identifier DRAGON_EXPLODING_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_exploding.png");
   private static final Identifier DRAGON_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
   private static final Identifier DRAGON_EYES_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");
   private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(DRAGON_LOCATION);
   private static final RenderType DECAL = RenderTypes.entityDecal(DRAGON_LOCATION);
   private static final RenderType EYES = RenderTypes.eyes(DRAGON_EYES_LOCATION);
   private static final RenderType BEAM = RenderTypes.entitySmoothCutout(CRYSTAL_BEAM_LOCATION);
   private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);
   private final EnderDragonModel model;

   public EnderDragonRenderer(EntityRendererProvider.Context p_173973_) {
      super(p_173973_);
      this.shadowRadius = 0.5F;
      this.model = new EnderDragonModel(p_173973_.bakeLayer(ModelLayers.ENDER_DRAGON));
   }

   public void submit(EnderDragonRenderState p_430659_, PoseStack p_425524_, SubmitNodeCollector p_423449_, CameraRenderState p_424851_) {
      p_425524_.pushPose();
      float f = p_430659_.getHistoricalPos(7).yRot();
      float f1 = (float)(p_430659_.getHistoricalPos(5).y() - p_430659_.getHistoricalPos(10).y());
      p_425524_.mulPose(Axis.YP.rotationDegrees(-f));
      p_425524_.mulPose(Axis.XP.rotationDegrees(f1 * 10.0F));
      p_425524_.translate(0.0F, 0.0F, 1.0F);
      p_425524_.scale(-1.0F, -1.0F, 1.0F);
      p_425524_.translate(0.0F, -1.501F, 0.0F);
      int i = OverlayTexture.pack(0.0F, p_430659_.hasRedOverlay);
      if (p_430659_.deathTime > 0.0F) {
         int j = ARGB.white(p_430659_.deathTime / 200.0F);
         p_423449_.order(0)
            .submitModel(
               this.model,
               p_430659_,
               p_425524_,
               RenderTypes.dragonExplosionAlpha(DRAGON_EXPLODING_LOCATION),
               p_430659_.lightCoords,
               OverlayTexture.NO_OVERLAY,
               j,
               null,
               p_430659_.outlineColor,
               null
            );
         p_423449_.order(1).submitModel(this.model, p_430659_, p_425524_, DECAL, p_430659_.lightCoords, i, -1, null, p_430659_.outlineColor, null);
      } else {
         p_423449_.order(0).submitModel(this.model, p_430659_, p_425524_, RENDER_TYPE, p_430659_.lightCoords, i, -1, null, p_430659_.outlineColor, null);
      }

      p_423449_.submitModel(this.model, p_430659_, p_425524_, EYES, p_430659_.lightCoords, OverlayTexture.NO_OVERLAY, p_430659_.outlineColor, null);
      if (p_430659_.deathTime > 0.0F) {
         float f2 = p_430659_.deathTime / 200.0F;
         p_425524_.pushPose();
         p_425524_.translate(0.0F, -1.0F, -2.0F);
         submitRays(p_425524_, f2, p_423449_, RenderTypes.dragonRays());
         submitRays(p_425524_, f2, p_423449_, RenderTypes.dragonRaysDepth());
         p_425524_.popPose();
      }

      p_425524_.popPose();
      if (p_430659_.beamOffset != null) {
         submitCrystalBeams(
            (float)p_430659_.beamOffset.x,
            (float)p_430659_.beamOffset.y,
            (float)p_430659_.beamOffset.z,
            p_430659_.ageInTicks,
            p_425524_,
            p_423449_,
            p_430659_.lightCoords
         );
      }

      super.submit(p_430659_, p_425524_, p_423449_, p_424851_);
   }

   private static void submitRays(PoseStack p_431183_, float p_423513_, SubmitNodeCollector p_425353_, RenderType p_458914_) {
      p_425353_.submitCustomGeometry(
         p_431183_,
         p_458914_,
         (p_426942_, p_423628_) -> {
            float f = Math.min(p_423513_ > 0.8F ? (p_423513_ - 0.8F) / 0.2F : 0.0F, 1.0F);
            int i = ARGB.colorFromFloat(1.0F - f, 1.0F, 1.0F, 1.0F);
            int j = 16711935;
            RandomSource randomsource = RandomSource.create(432L);
            Vector3f vector3f = new Vector3f();
            Vector3f vector3f1 = new Vector3f();
            Vector3f vector3f2 = new Vector3f();
            Vector3f vector3f3 = new Vector3f();
            Quaternionf quaternionf = new Quaternionf();
            int k = Mth.floor((p_423513_ + p_423513_ * p_423513_) / 2.0F * 60.0F);

            for (int l = 0; l < k; l++) {
               quaternionf.rotationXYZ(
                     randomsource.nextFloat() * (float) (Math.PI * 2),
                     randomsource.nextFloat() * (float) (Math.PI * 2),
                     randomsource.nextFloat() * (float) (Math.PI * 2)
                  )
                  .rotateXYZ(
                     randomsource.nextFloat() * (float) (Math.PI * 2),
                     randomsource.nextFloat() * (float) (Math.PI * 2),
                     randomsource.nextFloat() * (float) (Math.PI * 2) + p_423513_ * (float) (Math.PI / 2)
                  );
               p_426942_.rotate(quaternionf);
               float f1 = randomsource.nextFloat() * 20.0F + 5.0F + f * 10.0F;
               float f2 = randomsource.nextFloat() * 2.0F + 1.0F + f * 2.0F;
               vector3f1.set(-HALF_SQRT_3 * f2, f1, -0.5F * f2);
               vector3f2.set(HALF_SQRT_3 * f2, f1, -0.5F * f2);
               vector3f3.set(0.0F, f1, f2);
               p_423628_.addVertex(p_426942_, vector3f).setColor(i);
               p_423628_.addVertex(p_426942_, vector3f1).setColor(16711935);
               p_423628_.addVertex(p_426942_, vector3f2).setColor(16711935);
               p_423628_.addVertex(p_426942_, vector3f).setColor(i);
               p_423628_.addVertex(p_426942_, vector3f2).setColor(16711935);
               p_423628_.addVertex(p_426942_, vector3f3).setColor(16711935);
               p_423628_.addVertex(p_426942_, vector3f).setColor(i);
               p_423628_.addVertex(p_426942_, vector3f3).setColor(16711935);
               p_423628_.addVertex(p_426942_, vector3f1).setColor(16711935);
            }
         }
      );
   }

   public static void submitCrystalBeams(
      float p_427097_, float p_422335_, float p_431166_, float p_425647_, PoseStack p_425351_, SubmitNodeCollector p_429648_, int p_425816_
   ) {
      float f = Mth.sqrt(p_427097_ * p_427097_ + p_431166_ * p_431166_);
      float f1 = Mth.sqrt(p_427097_ * p_427097_ + p_422335_ * p_422335_ + p_431166_ * p_431166_);
      p_425351_.pushPose();
      p_425351_.translate(0.0F, 2.0F, 0.0F);
      p_425351_.mulPose(Axis.YP.rotation((float)(-Math.atan2(p_431166_, p_427097_)) - (float) (Math.PI / 2)));
      p_425351_.mulPose(Axis.XP.rotation((float)(-Math.atan2(f, p_422335_)) - (float) (Math.PI / 2)));
      float f2 = 0.0F - p_425647_ * 0.01F;
      float f3 = f1 / 32.0F - p_425647_ * 0.01F;
      p_429648_.submitCustomGeometry(
         p_425351_,
         BEAM,
         (p_423101_, p_428371_) -> {
            int i = 8;
            float f4 = 0.0F;
            float f5 = 0.75F;
            float f6 = 0.0F;

            for (int j = 1; j <= 8; j++) {
               float f7 = Mth.sin(j * (float) (Math.PI * 2) / 8.0F) * 0.75F;
               float f8 = Mth.cos(j * (float) (Math.PI * 2) / 8.0F) * 0.75F;
               float f9 = j / 8.0F;
               p_428371_.addVertex(p_423101_, f4 * 0.2F, f5 * 0.2F, 0.0F)
                  .setColor(-16777216)
                  .setUv(f6, f2)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(p_425816_)
                  .setNormal(p_423101_, 0.0F, -1.0F, 0.0F);
               p_428371_.addVertex(p_423101_, f4, f5, f1)
                  .setColor(-1)
                  .setUv(f6, f3)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(p_425816_)
                  .setNormal(p_423101_, 0.0F, -1.0F, 0.0F);
               p_428371_.addVertex(p_423101_, f7, f8, f1)
                  .setColor(-1)
                  .setUv(f9, f3)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(p_425816_)
                  .setNormal(p_423101_, 0.0F, -1.0F, 0.0F);
               p_428371_.addVertex(p_423101_, f7 * 0.2F, f8 * 0.2F, 0.0F)
                  .setColor(-16777216)
                  .setUv(f9, f2)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(p_425816_)
                  .setNormal(p_423101_, 0.0F, -1.0F, 0.0F);
               f4 = f7;
               f5 = f8;
               f6 = f9;
            }
         }
      );
      p_425351_.popPose();
   }

   public EnderDragonRenderState createRenderState() {
      return new EnderDragonRenderState();
   }

   public void extractRenderState(EnderDragon p_367718_, EnderDragonRenderState p_360720_, float p_367927_) {
      super.extractRenderState(p_367718_, p_360720_, p_367927_);
      p_360720_.flapTime = Mth.lerp(p_367927_, p_367718_.oFlapTime, p_367718_.flapTime);
      p_360720_.deathTime = p_367718_.dragonDeathTime > 0 ? p_367718_.dragonDeathTime + p_367927_ : 0.0F;
      p_360720_.hasRedOverlay = p_367718_.hurtTime > 0;
      EndCrystal endcrystal = p_367718_.nearestCrystal;
      if (endcrystal != null) {
         Vec3 vec3 = endcrystal.getPosition(p_367927_).add(0.0, EndCrystalRenderer.getY(endcrystal.time + p_367927_), 0.0);
         p_360720_.beamOffset = vec3.subtract(p_367718_.getPosition(p_367927_));
      } else {
         p_360720_.beamOffset = null;
      }

      DragonPhaseInstance dragonphaseinstance = p_367718_.getPhaseManager().getCurrentPhase();
      p_360720_.isLandingOrTakingOff = dragonphaseinstance == EnderDragonPhase.LANDING || dragonphaseinstance == EnderDragonPhase.TAKEOFF;
      p_360720_.isSitting = dragonphaseinstance.isSitting();
      BlockPos blockpos = p_367718_.level()
         .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(p_367718_.getFightOrigin()));
      p_360720_.distanceToEgg = blockpos.distToCenterSqr(p_367718_.position());
      p_360720_.partialTicks = p_367718_.isDeadOrDying() ? 0.0F : p_367927_;
      p_360720_.flightHistory.copyFrom(p_367718_.flightHistory);
   }

   protected boolean affectedByCulling(EnderDragon p_362111_) {
      return false;
   }
}
