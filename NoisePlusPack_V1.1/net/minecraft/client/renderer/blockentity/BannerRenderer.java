package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity, BannerRenderState> {
   private static final int MAX_PATTERNS = 16;
   private static final float SIZE = 0.6666667F;
   private final MaterialSet materials;
   private final BannerModel standingModel;
   private final BannerModel wallModel;
   private final BannerFlagModel standingFlagModel;
   private final BannerFlagModel wallFlagModel;

   public BannerRenderer(BlockEntityRendererProvider.Context p_173521_) {
      this(p_173521_.entityModelSet(), p_173521_.materials());
   }

   public BannerRenderer(SpecialModelRenderer.BakingContext p_427206_) {
      this(p_427206_.entityModelSet(), p_427206_.materials());
   }

   public BannerRenderer(EntityModelSet p_375660_, MaterialSet p_427582_) {
      this.materials = p_427582_;
      this.standingModel = new BannerModel(p_375660_.bakeLayer(ModelLayers.STANDING_BANNER));
      this.wallModel = new BannerModel(p_375660_.bakeLayer(ModelLayers.WALL_BANNER));
      this.standingFlagModel = new BannerFlagModel(p_375660_.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
      this.wallFlagModel = new BannerFlagModel(p_375660_.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
   }

   public BannerRenderState createRenderState() {
      return new BannerRenderState();
   }

   public void extractRenderState(
      BannerBlockEntity p_422407_, BannerRenderState p_427240_, float p_422439_, Vec3 p_429187_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_428098_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_422407_, p_427240_, p_422439_, p_429187_, p_428098_);
      p_427240_.baseColor = p_422407_.getBaseColor();
      p_427240_.patterns = p_422407_.getPatterns();
      BlockState blockstate = p_422407_.getBlockState();
      if (blockstate.getBlock() instanceof BannerBlock) {
         p_427240_.angle = -RotationSegment.convertToDegrees(blockstate.getValue(BannerBlock.ROTATION));
         p_427240_.standing = true;
      } else {
         p_427240_.angle = -blockstate.getValue(WallBannerBlock.FACING).toYRot();
         p_427240_.standing = false;
      }

      long i = p_422407_.getLevel() != null ? p_422407_.getLevel().getGameTime() : 0L;
      BlockPos blockpos = p_422407_.getBlockPos();
      p_427240_.phase = ((float)Math.floorMod(blockpos.getX() * 7 + blockpos.getY() * 9 + blockpos.getZ() * 13 + i, 100L) + p_422439_) / 100.0F;
   }

   public void submit(BannerRenderState p_426731_, PoseStack p_424811_, SubmitNodeCollector p_423263_, CameraRenderState p_428572_) {
      BannerModel bannermodel;
      BannerFlagModel bannerflagmodel;
      if (p_426731_.standing) {
         bannermodel = this.standingModel;
         bannerflagmodel = this.standingFlagModel;
      } else {
         bannermodel = this.wallModel;
         bannerflagmodel = this.wallFlagModel;
      }

      submitBanner(
         this.materials,
         p_424811_,
         p_423263_,
         p_426731_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         p_426731_.angle,
         bannermodel,
         bannerflagmodel,
         p_426731_.phase,
         p_426731_.baseColor,
         p_426731_.patterns,
         p_426731_.breakProgress,
         0
      );
   }

   public void submitSpecial(
      PoseStack p_431179_, SubmitNodeCollector p_427768_, int p_427044_, int p_428051_, DyeColor p_425000_, BannerPatternLayers p_430457_, int p_431899_
   ) {
      submitBanner(
         this.materials,
         p_431179_,
         p_427768_,
         p_427044_,
         p_428051_,
         0.0F,
         this.standingModel,
         this.standingFlagModel,
         0.0F,
         p_425000_,
         p_430457_,
         null,
         p_431899_
      );
   }

   private static void submitBanner(
      MaterialSet p_431017_,
      PoseStack p_423757_,
      SubmitNodeCollector p_431636_,
      int p_428310_,
      int p_424909_,
      float p_426030_,
      BannerModel p_454643_,
      BannerFlagModel p_450434_,
      float p_426784_,
      DyeColor p_431756_,
      BannerPatternLayers p_424340_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_423910_,
      int p_431875_
   ) {
      p_423757_.pushPose();
      p_423757_.translate(0.5F, 0.0F, 0.5F);
      p_423757_.mulPose(Axis.YP.rotationDegrees(p_426030_));
      p_423757_.scale(0.6666667F, -0.6666667F, -0.6666667F);
      Material material = ModelBakery.BANNER_BASE;
      p_431636_.submitModel(
         p_454643_,
         Unit.INSTANCE,
         p_423757_,
         material.renderType(RenderTypes::entitySolid),
         p_428310_,
         p_424909_,
         -1,
         p_431017_.get(material),
         p_431875_,
         p_423910_
      );
      submitPatterns(
         p_431017_, p_423757_, p_431636_, p_428310_, p_424909_, p_450434_, p_426784_, material, true, p_431756_, p_424340_, false, p_423910_, p_431875_
      );
      p_423757_.popPose();
   }

   public static <S> void submitPatterns(
      MaterialSet p_425635_,
      PoseStack p_428360_,
      SubmitNodeCollector p_431763_,
      int p_424189_,
      int p_429785_,
      Model<S> p_426098_,
      S p_426980_,
      Material p_424999_,
      boolean p_430414_,
      DyeColor p_426913_,
      BannerPatternLayers p_424024_,
      boolean p_431878_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_426522_,
      int p_431888_
   ) {
      p_431763_.submitModel(
         p_426098_,
         p_426980_,
         p_428360_,
         p_424999_.renderType(RenderTypes::entitySolid),
         p_424189_,
         p_429785_,
         -1,
         p_425635_.get(p_424999_),
         p_431888_,
         p_426522_
      );
      if (p_431878_) {
         p_431763_.submitModel(p_426098_, p_426980_, p_428360_, RenderTypes.entityGlint(), p_424189_, p_429785_, -1, p_425635_.get(p_424999_), 0, p_426522_);
      }

      submitPatternLayer(
         p_425635_, p_428360_, p_431763_, p_424189_, p_429785_, p_426098_, p_426980_, p_430414_ ? Sheets.BANNER_BASE : Sheets.SHIELD_BASE, p_426913_, p_426522_
      );

      for (int i = 0; i < 16 && i < p_424024_.layers().size(); i++) {
         BannerPatternLayers.Layer bannerpatternlayers$layer = p_424024_.layers().get(i);
         Material material = p_430414_
            ? Sheets.getBannerMaterial(bannerpatternlayers$layer.pattern())
            : Sheets.getShieldMaterial(bannerpatternlayers$layer.pattern());
         submitPatternLayer(p_425635_, p_428360_, p_431763_, p_424189_, p_429785_, p_426098_, p_426980_, material, bannerpatternlayers$layer.color(), null);
      }
   }

   private static <S> void submitPatternLayer(
      MaterialSet p_422964_,
      PoseStack p_425210_,
      SubmitNodeCollector p_427076_,
      int p_425594_,
      int p_426983_,
      Model<S> p_424492_,
      S p_426017_,
      Material p_430214_,
      DyeColor p_423269_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_427090_
   ) {
      int i = p_423269_.getTextureDiffuseColor();
      p_427076_.submitModel(
         p_424492_, p_426017_, p_425210_, p_430214_.renderType(RenderTypes::entityNoOutline), p_425594_, p_426983_, i, p_422964_.get(p_430214_), 0, p_427090_
      );
   }

   public void getExtents(Consumer<Vector3fc> p_457764_) {
      PoseStack posestack = new PoseStack();
      posestack.translate(0.5F, 0.0F, 0.5F);
      posestack.scale(0.6666667F, -0.6666667F, -0.6666667F);
      this.standingModel.root().getExtentsForGui(posestack, p_457764_);
      this.standingFlagModel.setupAnim(0.0F);
      this.standingFlagModel.root().getExtentsForGui(posestack, p_457764_);
   }
}
