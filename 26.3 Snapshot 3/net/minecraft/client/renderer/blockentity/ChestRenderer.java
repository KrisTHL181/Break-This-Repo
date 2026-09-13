package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.util.SpecialDates;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public class ChestRenderer<T extends BlockEntity & LidBlockEntity> implements BlockEntityRenderer<T, ChestRenderState> {
   public static final MultiblockChestResources<ModelLayerLocation> LAYERS = new MultiblockChestResources<>(
      ModelLayers.CHEST, ModelLayers.DOUBLE_CHEST_LEFT, ModelLayers.DOUBLE_CHEST_RIGHT
   );
   private static final Map<Direction, Transformation> TRANSFORMATIONS = Util.makeEnumMap(Direction.class, ChestRenderer::createModelTransformation);
   private final SpriteGetter sprites;
   private final MultiblockChestResources<ChestModel> models;
   private final boolean xmasTextures;

   public ChestRenderer(final BlockEntityRendererProvider.Context context) {
      this.sprites = context.sprites();
      this.xmasTextures = xmasTextures();
      this.models = LAYERS.map(layer -> new ChestModel(context.bakeLayer(layer)));
   }

   public static boolean xmasTextures() {
      return SpecialDates.isExtendedChristmas();
   }

   public ChestRenderState createRenderState() {
      return new ChestRenderState();
   }

   public void extractRenderState(
      final T blockEntity,
      final ChestRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      boolean hasLevel = blockEntity.getLevel() != null;
      BlockState blockState = hasLevel ? blockEntity.getBlockState() : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
      state.type = blockState.hasProperty(ChestBlock.TYPE) ? blockState.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
      state.facing = blockState.getValue(ChestBlock.FACING);
      state.material = getChestMaterial(blockEntity, this.xmasTextures);
      DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combineResult;
      if (hasLevel && blockState.getBlock() instanceof ChestBlock chestBlock) {
         combineResult = chestBlock.combine(blockState, blockEntity.getLevel(), blockEntity.getBlockPos(), true);
      } else {
         combineResult = DoubleBlockCombiner.Combiner::acceptNone;
      }

      state.open = combineResult.apply(ChestBlock.opennessCombiner(blockEntity)).get(partialTicks);
      if (state.type != ChestType.SINGLE) {
         state.lightCoords = combineResult.apply(new BrightnessCombiner<>()).applyAsInt(state.lightCoords);
      }
   }

   public void submit(final ChestRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      poseStack.pushPose();
      poseStack.mulPose(modelTransformation(state.facing));
      float open = state.open;
      open = 1.0F - open;
      open = 1.0F - open * open * open;
      SpriteId spriteId = Sheets.chooseSprite(state.material, state.type);
      ChestModel model = this.models.select(state.type);
      submitNodeCollector.submitModel(model, open, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, spriteId, this.sprites, 0);
      if (state.breakProgress != null) {
         submitNodeCollector.order(1)
            .submitCrumblingOverlay(
               model, open, poseStack, spriteId.renderType(model.renderType()), state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress
            );
      }

      poseStack.popPose();
   }

   private static ChestRenderState.ChestMaterialType getChestMaterial(final BlockEntity entity, final boolean xmasTextures) {
      if (entity.getBlockState().getBlock() instanceof CopperChestBlock copperChestBlock) {
         return switch (copperChestBlock.getState()) {
            case UNAFFECTED -> ChestRenderState.ChestMaterialType.COPPER_UNAFFECTED;
            case EXPOSED -> ChestRenderState.ChestMaterialType.COPPER_EXPOSED;
            case WEATHERED -> ChestRenderState.ChestMaterialType.COPPER_WEATHERED;
            case OXIDIZED -> ChestRenderState.ChestMaterialType.COPPER_OXIDIZED;
         };
      } else if (entity instanceof EnderChestBlockEntity) {
         return ChestRenderState.ChestMaterialType.ENDER_CHEST;
      } else if (xmasTextures) {
         return ChestRenderState.ChestMaterialType.CHRISTMAS;
      } else {
         return entity instanceof TrappedChestBlockEntity ? ChestRenderState.ChestMaterialType.TRAPPED : ChestRenderState.ChestMaterialType.REGULAR;
      }
   }

   public static Transformation modelTransformation(final Direction facing) {
      return TRANSFORMATIONS.get(facing);
   }

   private static Transformation createModelTransformation(final Direction facing) {
      return new Transformation(new Matrix4f().rotationAround(Axis.YP.rotationDegrees(-facing.toYRot()), 0.5F, 0.0F, 0.5F));
   }
}
