package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BedRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BedRenderer implements BlockEntityRenderer<BedBlockEntity, BedRenderState> {
   private final MaterialSet materials;
   private final Model.Simple headModel;
   private final Model.Simple footModel;

   public BedRenderer(BlockEntityRendererProvider.Context p_173540_) {
      this(p_173540_.materials(), p_173540_.entityModelSet());
   }

   public BedRenderer(SpecialModelRenderer.BakingContext p_429150_) {
      this(p_429150_.materials(), p_429150_.entityModelSet());
   }

   public BedRenderer(MaterialSet p_431659_, EntityModelSet p_429790_) {
      this.materials = p_431659_;
      this.headModel = new Model.Simple(p_429790_.bakeLayer(ModelLayers.BED_HEAD), RenderTypes::entitySolid);
      this.footModel = new Model.Simple(p_429790_.bakeLayer(ModelLayers.BED_FOOT), RenderTypes::entitySolid);
   }

   public static LayerDefinition createHeadLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(50, 6).addBox(0.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F),
         PartPose.rotation((float) (Math.PI / 2), 0.0F, (float) (Math.PI / 2))
      );
      partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(50, 18).addBox(-16.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F),
         PartPose.rotation((float) (Math.PI / 2), 0.0F, (float) Math.PI)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createFootLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 22).addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(50, 0).addBox(0.0F, 6.0F, -16.0F, 3.0F, 3.0F, 3.0F), PartPose.rotation((float) (Math.PI / 2), 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(50, 12).addBox(-16.0F, 6.0F, -16.0F, 3.0F, 3.0F, 3.0F),
         PartPose.rotation((float) (Math.PI / 2), 0.0F, (float) (Math.PI * 3.0 / 2.0))
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public BedRenderState createRenderState() {
      return new BedRenderState();
   }

   public void extractRenderState(
      BedBlockEntity p_422411_, BedRenderState p_427384_, float p_430865_, Vec3 p_431582_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_422474_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_422411_, p_427384_, p_430865_, p_431582_, p_422474_);
      p_427384_.color = p_422411_.getColor();
      p_427384_.facing = p_422411_.getBlockState().getValue(BedBlock.FACING);
      p_427384_.isHead = p_422411_.getBlockState().getValue(BedBlock.PART) == BedPart.HEAD;
      if (p_422411_.getLevel() != null) {
         DoubleBlockCombiner.NeighborCombineResult<? extends BedBlockEntity> neighborcombineresult = DoubleBlockCombiner.combineWithNeigbour(
            BlockEntityType.BED,
            BedBlock::getBlockType,
            BedBlock::getConnectedDirection,
            ChestBlock.FACING,
            p_422411_.getBlockState(),
            p_422411_.getLevel(),
            p_422411_.getBlockPos(),
            (p_112202_, p_112203_) -> false
         );
         p_427384_.lightCoords = neighborcombineresult.apply(new BrightnessCombiner<>()).get(p_427384_.lightCoords);
      }
   }

   public void submit(BedRenderState p_426981_, PoseStack p_422405_, SubmitNodeCollector p_426045_, CameraRenderState p_423065_) {
      Material material = Sheets.getBedMaterial(p_426981_.color);
      this.submitPiece(
         p_422405_,
         p_426045_,
         p_426981_.isHead ? this.headModel : this.footModel,
         p_426981_.facing,
         material,
         p_426981_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         false,
         p_426981_.breakProgress,
         0
      );
   }

   public void submitSpecial(PoseStack p_426034_, SubmitNodeCollector p_425219_, int p_423641_, int p_429478_, Material p_427672_, int p_431881_) {
      this.submitPiece(p_426034_, p_425219_, this.headModel, Direction.SOUTH, p_427672_, p_423641_, p_429478_, false, null, p_431881_);
      this.submitPiece(p_426034_, p_425219_, this.footModel, Direction.SOUTH, p_427672_, p_423641_, p_429478_, true, null, p_431881_);
   }

   private void submitPiece(
      PoseStack p_424863_,
      SubmitNodeCollector p_424084_,
      Model.Simple p_426581_,
      Direction p_424114_,
      Material p_425857_,
      int p_431434_,
      int p_425657_,
      boolean p_425559_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_426668_,
      int p_431910_
   ) {
      p_424863_.pushPose();
      preparePose(p_424863_, p_425559_, p_424114_);
      p_424084_.submitModel(
         p_426581_,
         Unit.INSTANCE,
         p_424863_,
         p_425857_.renderType(RenderTypes::entitySolid),
         p_431434_,
         p_425657_,
         -1,
         this.materials.get(p_425857_),
         p_431910_,
         p_426668_
      );
      p_424863_.popPose();
   }

   private static void preparePose(PoseStack p_406225_, boolean p_410142_, Direction p_408294_) {
      p_406225_.translate(0.0F, 0.5625F, p_410142_ ? -1.0F : 0.0F);
      p_406225_.mulPose(Axis.XP.rotationDegrees(90.0F));
      p_406225_.translate(0.5F, 0.5F, 0.5F);
      p_406225_.mulPose(Axis.ZP.rotationDegrees(180.0F + p_408294_.toYRot()));
      p_406225_.translate(-0.5F, -0.5F, -0.5F);
   }

   public void getExtents(Consumer<Vector3fc> p_453619_) {
      PoseStack posestack = new PoseStack();
      preparePose(posestack, false, Direction.SOUTH);
      this.headModel.root().getExtentsForGui(posestack, p_453619_);
      posestack.setIdentity();
      preparePose(posestack, true, Direction.SOUTH);
      this.footModel.root().getExtentsForGui(posestack, p_453619_);
   }
}
