package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HangingSignRenderer extends AbstractSignRenderer {
   private static final String PLANK = "plank";
   private static final String V_CHAINS = "vChains";
   private static final String NORMAL_CHAINS = "normalChains";
   private static final String CHAIN_L_1 = "chainL1";
   private static final String CHAIN_L_2 = "chainL2";
   private static final String CHAIN_R_1 = "chainR1";
   private static final String CHAIN_R_2 = "chainR2";
   private static final String BOARD = "board";
   public static final float MODEL_RENDER_SCALE = 1.0F;
   private static final float TEXT_RENDER_SCALE = 0.9F;
   private static final Vec3 TEXT_OFFSET = new Vec3(0.0, -0.32F, 0.073F);
   private final Map<HangingSignRenderer.ModelKey, Model.Simple> hangingSignModels;

   public HangingSignRenderer(BlockEntityRendererProvider.Context p_248772_) {
      super(p_248772_);
      Stream<HangingSignRenderer.ModelKey> stream = WoodType.values()
         .flatMap(
            p_374638_ -> Arrays.stream(HangingSignRenderer.AttachmentType.values()).map(p_374642_ -> new HangingSignRenderer.ModelKey(p_374638_, p_374642_))
         );
      this.hangingSignModels = stream.collect(
         ImmutableMap.toImmutableMap(
            p_376765_ -> p_376765_, p_420923_ -> createSignModel(p_248772_.entityModelSet(), p_420923_.woodType, p_420923_.attachmentType)
         )
      );
   }

   public static Model.Simple createSignModel(EntityModelSet p_378530_, WoodType p_378120_, HangingSignRenderer.AttachmentType p_377170_) {
      return new Model.Simple(p_378530_.bakeLayer(ModelLayers.createHangingSignModelName(p_378120_, p_377170_)), RenderTypes::entityCutoutNoCull);
   }

   @Override
   protected float getSignModelRenderScale() {
      return 1.0F;
   }

   @Override
   protected float getSignTextRenderScale() {
      return 0.9F;
   }

   public static void translateBase(PoseStack p_376581_, float p_378078_) {
      p_376581_.translate(0.5, 0.9375, 0.5);
      p_376581_.mulPose(Axis.YP.rotationDegrees(p_378078_));
      p_376581_.translate(0.0F, -0.3125F, 0.0F);
   }

   @Override
   protected void translateSign(PoseStack p_277807_, float p_277917_, BlockState p_277638_) {
      translateBase(p_277807_, p_277917_);
   }

   @Override
   protected Model.Simple getSignModel(BlockState p_376853_, WoodType p_378510_) {
      HangingSignRenderer.AttachmentType hangingsignrenderer$attachmenttype = HangingSignRenderer.AttachmentType.byBlockState(p_376853_);
      return this.hangingSignModels.get(new HangingSignRenderer.ModelKey(p_378510_, hangingsignrenderer$attachmenttype));
   }

   @Override
   protected Material getSignMaterial(WoodType p_251791_) {
      return Sheets.getHangingSignMaterial(p_251791_);
   }

   @Override
   protected Vec3 getTextOffset() {
      return TEXT_OFFSET;
   }

   public static void submitSpecial(
      MaterialSet p_430573_, PoseStack p_423478_, SubmitNodeCollector p_425840_, int p_429367_, int p_427642_, Model.Simple p_429076_, Material p_424765_
   ) {
      p_423478_.pushPose();
      translateBase(p_423478_, 0.0F);
      p_423478_.scale(1.0F, -1.0F, -1.0F);
      p_425840_.submitModel(
         p_429076_,
         Unit.INSTANCE,
         p_423478_,
         p_424765_.renderType(p_429076_::renderType),
         p_429367_,
         p_427642_,
         -1,
         p_430573_.get(p_424765_),
         OverlayTexture.NO_OVERLAY,
         null
      );
      p_423478_.popPose();
   }

   public static LayerDefinition createHangingSignLayer(HangingSignRenderer.AttachmentType p_375619_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("board", CubeListBuilder.create().texOffs(0, 12).addBox(-7.0F, 0.0F, -1.0F, 14.0F, 10.0F, 2.0F), PartPose.ZERO);
      if (p_375619_ == HangingSignRenderer.AttachmentType.WALL) {
         partdefinition.addOrReplaceChild("plank", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -6.0F, -2.0F, 16.0F, 2.0F, 4.0F), PartPose.ZERO);
      }

      if (p_375619_ == HangingSignRenderer.AttachmentType.WALL || p_375619_ == HangingSignRenderer.AttachmentType.CEILING) {
         PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("normalChains", CubeListBuilder.create(), PartPose.ZERO);
         partdefinition1.addOrReplaceChild(
            "chainL1",
            CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
            PartPose.offsetAndRotation(-5.0F, -6.0F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
         );
         partdefinition1.addOrReplaceChild(
            "chainL2",
            CubeListBuilder.create().texOffs(6, 6).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
            PartPose.offsetAndRotation(-5.0F, -6.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
         );
         partdefinition1.addOrReplaceChild(
            "chainR1",
            CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
            PartPose.offsetAndRotation(5.0F, -6.0F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
         );
         partdefinition1.addOrReplaceChild(
            "chainR2",
            CubeListBuilder.create().texOffs(6, 6).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
            PartPose.offsetAndRotation(5.0F, -6.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
         );
      }

      if (p_375619_ == HangingSignRenderer.AttachmentType.CEILING_MIDDLE) {
         partdefinition.addOrReplaceChild("vChains", CubeListBuilder.create().texOffs(14, 6).addBox(-6.0F, -6.0F, 0.0F, 12.0F, 6.0F, 0.0F), PartPose.ZERO);
      }

      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   @OnlyIn(Dist.CLIENT)
   public enum AttachmentType implements StringRepresentable {
      WALL("wall"),
      CEILING("ceiling"),
      CEILING_MIDDLE("ceiling_middle");

      private final String name;

      AttachmentType(final String p_375617_) {
         this.name = p_375617_;
      }

      public static HangingSignRenderer.AttachmentType byBlockState(BlockState p_376849_) {
         if (p_376849_.getBlock() instanceof CeilingHangingSignBlock) {
            return p_376849_.getValue(BlockStateProperties.ATTACHED) ? CEILING_MIDDLE : CEILING;
         } else {
            return WALL;
         }
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record ModelKey(WoodType woodType, HangingSignRenderer.AttachmentType attachmentType) {
   }
}
