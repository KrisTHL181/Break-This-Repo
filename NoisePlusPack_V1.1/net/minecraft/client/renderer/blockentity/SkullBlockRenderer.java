package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.skull.DragonHeadModel;
import net.minecraft.client.model.object.skull.PiglinHeadModel;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SkullBlockRenderer implements BlockEntityRenderer<SkullBlockEntity, SkullBlockRenderState> {
   private final Function<SkullBlock.Type, SkullModelBase> modelByType;
   private static final Map<SkullBlock.Type, Identifier> SKIN_BY_TYPE = Util.make(Maps.newHashMap(), p_340906_ -> {
      p_340906_.put(SkullBlock.Types.SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png"));
      p_340906_.put(SkullBlock.Types.WITHER_SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png"));
      p_340906_.put(SkullBlock.Types.ZOMBIE, Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png"));
      p_340906_.put(SkullBlock.Types.CREEPER, Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png"));
      p_340906_.put(SkullBlock.Types.DRAGON, Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png"));
      p_340906_.put(SkullBlock.Types.PIGLIN, Identifier.withDefaultNamespace("textures/entity/piglin/piglin.png"));
      p_340906_.put(SkullBlock.Types.PLAYER, DefaultPlayerSkin.getDefaultTexture());
   });
   private final PlayerSkinRenderCache playerSkinRenderCache;

   public static @Nullable SkullModelBase createModel(EntityModelSet p_376421_, SkullBlock.Type p_377655_) {
      if (p_377655_ instanceof SkullBlock.Types skullblock$types) {
         return switch (skullblock$types) {
            case SKELETON -> new SkullModel(p_376421_.bakeLayer(ModelLayers.SKELETON_SKULL));
            case WITHER_SKELETON -> new SkullModel(p_376421_.bakeLayer(ModelLayers.WITHER_SKELETON_SKULL));
            case PLAYER -> new SkullModel(p_376421_.bakeLayer(ModelLayers.PLAYER_HEAD));
            case ZOMBIE -> new SkullModel(p_376421_.bakeLayer(ModelLayers.ZOMBIE_HEAD));
            case CREEPER -> new SkullModel(p_376421_.bakeLayer(ModelLayers.CREEPER_HEAD));
            case DRAGON -> new DragonHeadModel(p_376421_.bakeLayer(ModelLayers.DRAGON_SKULL));
            case PIGLIN -> new PiglinHeadModel(p_376421_.bakeLayer(ModelLayers.PIGLIN_HEAD));
         };
      } else {
         return null;
      }
   }

   public SkullBlockRenderer(BlockEntityRendererProvider.Context p_173660_) {
      EntityModelSet entitymodelset = p_173660_.entityModelSet();
      this.playerSkinRenderCache = p_173660_.playerSkinRenderCache();
      this.modelByType = Util.memoize(p_448220_ -> createModel(entitymodelset, p_448220_));
   }

   public SkullBlockRenderState createRenderState() {
      return new SkullBlockRenderState();
   }

   public void extractRenderState(
      SkullBlockEntity p_428948_, SkullBlockRenderState p_425051_, float p_428826_, Vec3 p_425640_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_428325_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_428948_, p_425051_, p_428826_, p_425640_, p_428325_);
      p_425051_.animationProgress = p_428948_.getAnimation(p_428826_);
      BlockState blockstate = p_428948_.getBlockState();
      boolean flag = blockstate.getBlock() instanceof WallSkullBlock;
      p_425051_.direction = flag ? blockstate.getValue(WallSkullBlock.FACING) : null;
      int i = flag ? RotationSegment.convertToSegment(p_425051_.direction.getOpposite()) : blockstate.getValue(SkullBlock.ROTATION);
      p_425051_.rotationDegrees = RotationSegment.convertToDegrees(i);
      p_425051_.skullType = ((AbstractSkullBlock)blockstate.getBlock()).getType();
      p_425051_.renderType = this.resolveSkullRenderType(p_425051_.skullType, p_428948_);
   }

   public void submit(SkullBlockRenderState p_427348_, PoseStack p_429151_, SubmitNodeCollector p_428428_, CameraRenderState p_428415_) {
      SkullModelBase skullmodelbase = this.modelByType.apply(p_427348_.skullType);
      submitSkull(
         p_427348_.direction,
         p_427348_.rotationDegrees,
         p_427348_.animationProgress,
         p_429151_,
         p_428428_,
         p_427348_.lightCoords,
         skullmodelbase,
         p_427348_.renderType,
         0,
         p_427348_.breakProgress
      );
   }

   public static void submitSkull(
      @Nullable Direction p_429199_,
      float p_431016_,
      float p_424944_,
      PoseStack p_424955_,
      SubmitNodeCollector p_427335_,
      int p_430325_,
      SkullModelBase p_454097_,
      RenderType p_456161_,
      int p_422297_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_431224_
   ) {
      p_424955_.pushPose();
      if (p_429199_ == null) {
         p_424955_.translate(0.5F, 0.0F, 0.5F);
      } else {
         float f = 0.25F;
         p_424955_.translate(0.5F - p_429199_.getStepX() * 0.25F, 0.25F, 0.5F - p_429199_.getStepZ() * 0.25F);
      }

      p_424955_.scale(-1.0F, -1.0F, 1.0F);
      SkullModelBase.State skullmodelbase$state = new SkullModelBase.State();
      skullmodelbase$state.animationPos = p_424944_;
      skullmodelbase$state.yRot = p_431016_;
      p_427335_.submitModel(p_454097_, skullmodelbase$state, p_424955_, p_456161_, p_430325_, OverlayTexture.NO_OVERLAY, p_422297_, p_431224_);
      p_424955_.popPose();
   }

   private RenderType resolveSkullRenderType(SkullBlock.Type p_427761_, SkullBlockEntity p_425433_) {
      if (p_427761_ == SkullBlock.Types.PLAYER) {
         ResolvableProfile resolvableprofile = p_425433_.getOwnerProfile();
         if (resolvableprofile != null) {
            return this.playerSkinRenderCache.getOrDefault(resolvableprofile).renderType();
         }
      }

      return getSkullRenderType(p_427761_, null);
   }

   public static RenderType getSkullRenderType(SkullBlock.Type p_408012_, @Nullable Identifier p_452477_) {
      return RenderTypes.entityCutoutNoCullZOffset(p_452477_ != null ? p_452477_ : SKIN_BY_TYPE.get(p_408012_));
   }

   public static RenderType getPlayerSkinRenderType(Identifier p_459382_) {
      return RenderTypes.entityTranslucent(p_459382_);
   }
}
