package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BeaconRenderer<T extends BlockEntity & BeaconBeamOwner> implements BlockEntityRenderer<T, BeaconRenderState> {
   public static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/beacon_beam.png");
   public static final int MAX_RENDER_Y = 2048;
   private static final float BEAM_SCALE_THRESHOLD = 96.0F;
   public static final float SOLID_BEAM_RADIUS = 0.2F;
   public static final float BEAM_GLOW_RADIUS = 0.25F;

   public BeaconRenderState createRenderState() {
      return new BeaconRenderState();
   }

   public void extractRenderState(
      T p_431678_, BeaconRenderState p_429388_, float p_429147_, Vec3 p_430767_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_425159_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_431678_, p_429388_, p_429147_, p_430767_, p_425159_);
      extract(p_431678_, p_429388_, p_429147_, p_430767_);
   }

   public static <T extends BlockEntity & BeaconBeamOwner> void extract(T p_423160_, BeaconRenderState p_427134_, float p_431151_, Vec3 p_422459_) {
      p_427134_.animationTime = p_423160_.getLevel() != null ? Math.floorMod(p_423160_.getLevel().getGameTime(), 40) + p_431151_ : 0.0F;
      p_427134_.sections = p_423160_.getBeamSections()
         .stream()
         .map(p_430850_ -> new BeaconRenderState.Section(p_430850_.getColor(), p_430850_.getHeight()))
         .toList();
      float f = (float)p_422459_.subtract(p_427134_.blockPos.getCenter()).horizontalDistance();
      LocalPlayer localplayer = Minecraft.getInstance().player;
      p_427134_.beamRadiusScale = localplayer != null && localplayer.isScoping() ? 1.0F : Math.max(1.0F, f / 96.0F);
   }

   public void submit(BeaconRenderState p_423237_, PoseStack p_430655_, SubmitNodeCollector p_426267_, CameraRenderState p_426445_) {
      int i = 0;

      for (int j = 0; j < p_423237_.sections.size(); j++) {
         BeaconRenderState.Section beaconrenderstate$section = p_423237_.sections.get(j);
         submitBeaconBeam(
            p_430655_,
            p_426267_,
            p_423237_.beamRadiusScale,
            p_423237_.animationTime,
            i,
            j == p_423237_.sections.size() - 1 ? 2048 : beaconrenderstate$section.height(),
            beaconrenderstate$section.color()
         );
         i += beaconrenderstate$section.height();
      }
   }

   private static void submitBeaconBeam(
      PoseStack p_430806_, SubmitNodeCollector p_424720_, float p_424646_, float p_425289_, int p_424919_, int p_426256_, int p_429752_
   ) {
      submitBeaconBeam(p_430806_, p_424720_, BEAM_LOCATION, 1.0F, p_425289_, p_424919_, p_426256_, p_429752_, 0.2F * p_424646_, 0.25F * p_424646_);
   }

   public static void submitBeaconBeam(
      PoseStack p_430215_,
      SubmitNodeCollector p_423842_,
      Identifier p_456864_,
      float p_430510_,
      float p_423924_,
      int p_427512_,
      int p_426161_,
      int p_427198_,
      float p_428444_,
      float p_424527_
   ) {
      int i = p_427512_ + p_426161_;
      p_430215_.pushPose();
      p_430215_.translate(0.5, 0.0, 0.5);
      float f = p_426161_ < 0 ? p_423924_ : -p_423924_;
      float f1 = Mth.frac(f * 0.2F - Mth.floor(f * 0.1F));
      p_430215_.pushPose();
      p_430215_.mulPose(Axis.YP.rotationDegrees(p_423924_ * 2.25F - 45.0F));
      float f5 = -p_428444_;
      float f8 = -p_428444_;
      float f11 = -1.0F + f1;
      float f12 = p_426161_ * p_430510_ * (0.5F / p_428444_) + f11;
      p_423842_.submitCustomGeometry(
         p_430215_,
         RenderTypes.beaconBeam(p_456864_, false),
         (p_425436_, p_430525_) -> renderPart(
            p_425436_, p_430525_, p_427198_, p_427512_, i, 0.0F, p_428444_, p_428444_, 0.0F, f5, 0.0F, 0.0F, f8, 0.0F, 1.0F, f12, f11
         )
      );
      p_430215_.popPose();
      float f11_f = -1.0F + f1;
      float f12_f = p_426161_ * p_430510_ + f11_f;
      p_423842_.submitCustomGeometry(
         p_430215_,
         RenderTypes.beaconBeam(p_456864_, true),
         (p_427615_, p_428983_) -> renderPart(
            p_427615_,
            p_428983_,
            ARGB.color(32, p_427198_),
            p_427512_,
            i,
            -p_424527_,
            -p_424527_,
            p_424527_,
            -p_424527_,
            -p_424527_,
            p_424527_,
            p_424527_,
            p_424527_,
            0.0F,
            1.0F,
            f12_f,
            f11_f
         )
      );
      p_430215_.popPose();
   }

   private static void renderPart(
      PoseStack.Pose p_428829_,
      VertexConsumer p_112157_,
      int p_112162_,
      int p_112163_,
      int p_345221_,
      float p_112158_,
      float p_112159_,
      float p_112160_,
      float p_112161_,
      float p_112164_,
      float p_112165_,
      float p_112166_,
      float p_112167_,
      float p_112168_,
      float p_112169_,
      float p_112170_,
      float p_112171_
   ) {
      renderQuad(p_428829_, p_112157_, p_112162_, p_112163_, p_345221_, p_112158_, p_112159_, p_112160_, p_112161_, p_112168_, p_112169_, p_112170_, p_112171_);
      renderQuad(p_428829_, p_112157_, p_112162_, p_112163_, p_345221_, p_112166_, p_112167_, p_112164_, p_112165_, p_112168_, p_112169_, p_112170_, p_112171_);
      renderQuad(p_428829_, p_112157_, p_112162_, p_112163_, p_345221_, p_112160_, p_112161_, p_112166_, p_112167_, p_112168_, p_112169_, p_112170_, p_112171_);
      renderQuad(p_428829_, p_112157_, p_112162_, p_112163_, p_345221_, p_112164_, p_112165_, p_112158_, p_112159_, p_112168_, p_112169_, p_112170_, p_112171_);
   }

   private static void renderQuad(
      PoseStack.Pose p_332343_,
      VertexConsumer p_112122_,
      int p_112127_,
      int p_112128_,
      int p_345385_,
      float p_112123_,
      float p_112124_,
      float p_112125_,
      float p_112126_,
      float p_112129_,
      float p_112130_,
      float p_112131_,
      float p_112132_
   ) {
      addVertex(p_332343_, p_112122_, p_112127_, p_345385_, p_112123_, p_112124_, p_112130_, p_112131_);
      addVertex(p_332343_, p_112122_, p_112127_, p_112128_, p_112123_, p_112124_, p_112130_, p_112132_);
      addVertex(p_332343_, p_112122_, p_112127_, p_112128_, p_112125_, p_112126_, p_112129_, p_112132_);
      addVertex(p_332343_, p_112122_, p_112127_, p_345385_, p_112125_, p_112126_, p_112129_, p_112131_);
   }

   private static void addVertex(
      PoseStack.Pose p_334631_, VertexConsumer p_253894_, int p_254357_, int p_343267_, float p_253871_, float p_253841_, float p_254568_, float p_254361_
   ) {
      p_253894_.addVertex(p_334631_, p_253871_, p_343267_, p_253841_)
         .setColor(p_254357_)
         .setUv(p_254568_, p_254361_)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(p_334631_, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public int getViewDistance() {
      return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
   }

   @Override
   public boolean shouldRender(T p_173534_, Vec3 p_173535_) {
      return Vec3.atCenterOf(p_173534_.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(p_173535_.multiply(1.0, 0.0, 1.0), this.getViewDistance());
   }
}
