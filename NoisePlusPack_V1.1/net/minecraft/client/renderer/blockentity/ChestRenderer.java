package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import net.minecraft.util.SpecialDates;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChestRenderer<T extends BlockEntity & LidBlockEntity> implements BlockEntityRenderer<T, ChestRenderState> {
   private final MaterialSet materials;
   private final ChestModel singleModel;
   private final ChestModel doubleLeftModel;
   private final ChestModel doubleRightModel;
   private final boolean xmasTextures;

   public ChestRenderer(BlockEntityRendererProvider.Context p_173607_) {
      this.materials = p_173607_.materials();
      this.xmasTextures = xmasTextures();
      this.singleModel = new ChestModel(p_173607_.bakeLayer(ModelLayers.CHEST));
      this.doubleLeftModel = new ChestModel(p_173607_.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
      this.doubleRightModel = new ChestModel(p_173607_.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
   }

   public static boolean xmasTextures() {
      return SpecialDates.isExtendedChristmas();
   }

   public ChestRenderState createRenderState() {
      return new ChestRenderState();
   }

   public void extractRenderState(
      T p_428274_, ChestRenderState p_426323_, float p_427971_, Vec3 p_424703_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_427162_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_428274_, p_426323_, p_427971_, p_424703_, p_427162_);
      boolean flag = p_428274_.getLevel() != null;
      BlockState blockstate = flag ? p_428274_.getBlockState() : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
      p_426323_.type = blockstate.hasProperty(ChestBlock.TYPE) ? blockstate.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
      p_426323_.angle = blockstate.getValue(ChestBlock.FACING).toYRot();
      p_426323_.material = this.getChestMaterial(p_428274_, this.xmasTextures);
      DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> neighborcombineresult;
      if (flag && blockstate.getBlock() instanceof ChestBlock chestblock) {
         neighborcombineresult = chestblock.combine(blockstate, p_428274_.getLevel(), p_428274_.getBlockPos(), true);
      } else {
         neighborcombineresult = DoubleBlockCombiner.Combiner::acceptNone;
      }

      p_426323_.open = neighborcombineresult.apply(ChestBlock.opennessCombiner(p_428274_)).get(p_427971_);
      if (p_426323_.type != ChestType.SINGLE) {
         p_426323_.lightCoords = neighborcombineresult.apply(new BrightnessCombiner<>()).applyAsInt(p_426323_.lightCoords);
      }
   }

   public void submit(ChestRenderState p_422365_, PoseStack p_425196_, SubmitNodeCollector p_426678_, CameraRenderState p_425989_) {
      p_425196_.pushPose();
      p_425196_.translate(0.5F, 0.5F, 0.5F);
      p_425196_.mulPose(Axis.YP.rotationDegrees(-p_422365_.angle));
      p_425196_.translate(-0.5F, -0.5F, -0.5F);
      float f = p_422365_.open;
      f = 1.0F - f;
      f = 1.0F - f * f * f;
      Material material = Sheets.chooseMaterial(p_422365_.material, p_422365_.type);
      RenderType rendertype = material.renderType(RenderTypes::entityCutout);
      TextureAtlasSprite textureatlassprite = this.materials.get(material);
      if (p_422365_.type != ChestType.SINGLE) {
         if (p_422365_.type == ChestType.LEFT) {
            p_426678_.submitModel(
               this.doubleLeftModel,
               f,
               p_425196_,
               rendertype,
               p_422365_.lightCoords,
               OverlayTexture.NO_OVERLAY,
               -1,
               textureatlassprite,
               0,
               p_422365_.breakProgress
            );
         } else {
            p_426678_.submitModel(
               this.doubleRightModel,
               f,
               p_425196_,
               rendertype,
               p_422365_.lightCoords,
               OverlayTexture.NO_OVERLAY,
               -1,
               textureatlassprite,
               0,
               p_422365_.breakProgress
            );
         }
      } else {
         p_426678_.submitModel(
            this.singleModel, f, p_425196_, rendertype, p_422365_.lightCoords, OverlayTexture.NO_OVERLAY, -1, textureatlassprite, 0, p_422365_.breakProgress
         );
      }

      p_425196_.popPose();
   }

   private ChestRenderState.ChestMaterialType getChestMaterial(BlockEntity p_427262_, boolean p_429703_) {
      if (p_427262_ instanceof EnderChestBlockEntity) {
         return ChestRenderState.ChestMaterialType.ENDER_CHEST;
      }

      if (p_429703_) {
         return ChestRenderState.ChestMaterialType.CHRISTMAS;
      }

      if (p_427262_ instanceof TrappedChestBlockEntity) {
         return ChestRenderState.ChestMaterialType.TRAPPED;
      }

      if (p_427262_.getBlockState().getBlock() instanceof CopperChestBlock copperchestblock) {
         return switch (copperchestblock.getState()) {
            case UNAFFECTED -> ChestRenderState.ChestMaterialType.COPPER_UNAFFECTED;
            case EXPOSED -> ChestRenderState.ChestMaterialType.COPPER_EXPOSED;
            case WEATHERED -> ChestRenderState.ChestMaterialType.COPPER_WEATHERED;
            case OXIDIZED -> ChestRenderState.ChestMaterialType.COPPER_OXIDIZED;
         };
      } else {
         return ChestRenderState.ChestMaterialType.REGULAR;
      }
   }
}
