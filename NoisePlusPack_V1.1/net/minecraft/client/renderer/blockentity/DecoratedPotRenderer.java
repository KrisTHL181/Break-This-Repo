package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.DecoratedPotRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DecoratedPotRenderer implements BlockEntityRenderer<DecoratedPotBlockEntity, DecoratedPotRenderState> {
   private final MaterialSet materials;
   private static final String NECK = "neck";
   private static final String FRONT = "front";
   private static final String BACK = "back";
   private static final String LEFT = "left";
   private static final String RIGHT = "right";
   private static final String TOP = "top";
   private static final String BOTTOM = "bottom";
   private final ModelPart neck;
   private final ModelPart frontSide;
   private final ModelPart backSide;
   private final ModelPart leftSide;
   private final ModelPart rightSide;
   private final ModelPart top;
   private final ModelPart bottom;
   private static final float WOBBLE_AMPLITUDE = 0.125F;

   public DecoratedPotRenderer(BlockEntityRendererProvider.Context p_272872_) {
      this(p_272872_.entityModelSet(), p_272872_.materials());
   }

   public DecoratedPotRenderer(SpecialModelRenderer.BakingContext p_423024_) {
      this(p_423024_.entityModelSet(), p_423024_.materials());
   }

   public DecoratedPotRenderer(EntityModelSet p_376368_, MaterialSet p_428238_) {
      this.materials = p_428238_;
      ModelPart modelpart = p_376368_.bakeLayer(ModelLayers.DECORATED_POT_BASE);
      this.neck = modelpart.getChild("neck");
      this.top = modelpart.getChild("top");
      this.bottom = modelpart.getChild("bottom");
      ModelPart modelpart1 = p_376368_.bakeLayer(ModelLayers.DECORATED_POT_SIDES);
      this.frontSide = modelpart1.getChild("front");
      this.backSide = modelpart1.getChild("back");
      this.leftSide = modelpart1.getChild("left");
      this.rightSide = modelpart1.getChild("right");
   }

   public static LayerDefinition createBaseLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      CubeDeformation cubedeformation = new CubeDeformation(0.2F);
      CubeDeformation cubedeformation1 = new CubeDeformation(-0.1F);
      partdefinition.addOrReplaceChild(
         "neck",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(4.0F, 17.0F, 4.0F, 8.0F, 3.0F, 8.0F, cubedeformation1)
            .texOffs(0, 5)
            .addBox(5.0F, 20.0F, 5.0F, 6.0F, 1.0F, 6.0F, cubedeformation),
         PartPose.offsetAndRotation(0.0F, 37.0F, 16.0F, (float) Math.PI, 0.0F, 0.0F)
      );
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(-14, 13).addBox(0.0F, 0.0F, 0.0F, 14.0F, 0.0F, 14.0F);
      partdefinition.addOrReplaceChild("top", cubelistbuilder, PartPose.offsetAndRotation(1.0F, 16.0F, 1.0F, 0.0F, 0.0F, 0.0F));
      partdefinition.addOrReplaceChild("bottom", cubelistbuilder, PartPose.offsetAndRotation(1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F));
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public static LayerDefinition createSidesLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(1, 0).addBox(0.0F, 0.0F, 0.0F, 14.0F, 16.0F, 0.0F, EnumSet.of(Direction.NORTH));
      partdefinition.addOrReplaceChild("back", cubelistbuilder, PartPose.offsetAndRotation(15.0F, 16.0F, 1.0F, 0.0F, 0.0F, (float) Math.PI));
      partdefinition.addOrReplaceChild("left", cubelistbuilder, PartPose.offsetAndRotation(1.0F, 16.0F, 1.0F, 0.0F, (float) (-Math.PI / 2), (float) Math.PI));
      partdefinition.addOrReplaceChild("right", cubelistbuilder, PartPose.offsetAndRotation(15.0F, 16.0F, 15.0F, 0.0F, (float) (Math.PI / 2), (float) Math.PI));
      partdefinition.addOrReplaceChild("front", cubelistbuilder, PartPose.offsetAndRotation(1.0F, 16.0F, 15.0F, (float) Math.PI, 0.0F, 0.0F));
      return LayerDefinition.create(meshdefinition, 16, 16);
   }

   private static Material getSideMaterial(Optional<Item> p_344130_) {
      if (p_344130_.isPresent()) {
         Material material = Sheets.getDecoratedPotMaterial(DecoratedPotPatterns.getPatternFromItem(p_344130_.get()));
         if (material != null) {
            return material;
         }
      }

      return Sheets.DECORATED_POT_SIDE;
   }

   public DecoratedPotRenderState createRenderState() {
      return new DecoratedPotRenderState();
   }

   public void extractRenderState(
      DecoratedPotBlockEntity p_423969_,
      DecoratedPotRenderState p_423279_,
      float p_428199_,
      Vec3 p_431555_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_431516_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_423969_, p_423279_, p_428199_, p_431555_, p_431516_);
      p_423279_.decorations = p_423969_.getDecorations();
      p_423279_.direction = p_423969_.getDirection();
      DecoratedPotBlockEntity.WobbleStyle decoratedpotblockentity$wobblestyle = p_423969_.lastWobbleStyle;
      if (decoratedpotblockentity$wobblestyle != null && p_423969_.getLevel() != null) {
         p_423279_.wobbleProgress = ((float)(p_423969_.getLevel().getGameTime() - p_423969_.wobbleStartedAtTick) + p_428199_)
            / decoratedpotblockentity$wobblestyle.duration;
      } else {
         p_423279_.wobbleProgress = 0.0F;
      }
   }

   public void submit(DecoratedPotRenderState p_427323_, PoseStack p_430827_, SubmitNodeCollector p_431381_, CameraRenderState p_429895_) {
      p_430827_.pushPose();
      Direction direction = p_427323_.direction;
      p_430827_.translate(0.5, 0.0, 0.5);
      p_430827_.mulPose(Axis.YP.rotationDegrees(180.0F - direction.toYRot()));
      p_430827_.translate(-0.5, 0.0, -0.5);
      if (p_427323_.wobbleProgress >= 0.0F && p_427323_.wobbleProgress <= 1.0F) {
         if (p_427323_.wobbleStyle == DecoratedPotBlockEntity.WobbleStyle.POSITIVE) {
            float f = 0.015625F;
            float f1 = p_427323_.wobbleProgress * (float) (Math.PI * 2);
            float f2 = -1.5F * (Mth.cos(f1) + 0.5F) * Mth.sin(f1 / 2.0F);
            p_430827_.rotateAround(Axis.XP.rotation(f2 * 0.015625F), 0.5F, 0.0F, 0.5F);
            float f3 = Mth.sin(f1);
            p_430827_.rotateAround(Axis.ZP.rotation(f3 * 0.015625F), 0.5F, 0.0F, 0.5F);
         } else {
            float f4 = Mth.sin(-p_427323_.wobbleProgress * 3.0F * (float) Math.PI) * 0.125F;
            float f5 = 1.0F - p_427323_.wobbleProgress;
            p_430827_.rotateAround(Axis.YP.rotation(f4 * f5), 0.5F, 0.0F, 0.5F);
         }
      }

      this.submit(p_430827_, p_431381_, p_427323_.lightCoords, OverlayTexture.NO_OVERLAY, p_427323_.decorations, 0);
      p_430827_.popPose();
   }

   public void submit(PoseStack p_427529_, SubmitNodeCollector p_427279_, int p_431694_, int p_427202_, PotDecorations p_431420_, int p_431873_) {
      RenderType rendertype = Sheets.DECORATED_POT_BASE.renderType(RenderTypes::entitySolid);
      TextureAtlasSprite textureatlassprite = this.materials.get(Sheets.DECORATED_POT_BASE);
      p_427279_.submitModelPart(this.neck, p_427529_, rendertype, p_431694_, p_427202_, textureatlassprite, false, false, -1, null, p_431873_);
      p_427279_.submitModelPart(this.top, p_427529_, rendertype, p_431694_, p_427202_, textureatlassprite, false, false, -1, null, p_431873_);
      p_427279_.submitModelPart(this.bottom, p_427529_, rendertype, p_431694_, p_427202_, textureatlassprite, false, false, -1, null, p_431873_);
      Material material = getSideMaterial(p_431420_.front());
      p_427279_.submitModelPart(
         this.frontSide,
         p_427529_,
         material.renderType(RenderTypes::entitySolid),
         p_431694_,
         p_427202_,
         this.materials.get(material),
         false,
         false,
         -1,
         null,
         p_431873_
      );
      Material material1 = getSideMaterial(p_431420_.back());
      p_427279_.submitModelPart(
         this.backSide,
         p_427529_,
         material1.renderType(RenderTypes::entitySolid),
         p_431694_,
         p_427202_,
         this.materials.get(material1),
         false,
         false,
         -1,
         null,
         p_431873_
      );
      Material material2 = getSideMaterial(p_431420_.left());
      p_427279_.submitModelPart(
         this.leftSide,
         p_427529_,
         material2.renderType(RenderTypes::entitySolid),
         p_431694_,
         p_427202_,
         this.materials.get(material2),
         false,
         false,
         -1,
         null,
         p_431873_
      );
      Material material3 = getSideMaterial(p_431420_.right());
      p_427279_.submitModelPart(
         this.rightSide,
         p_427529_,
         material3.renderType(RenderTypes::entitySolid),
         p_431694_,
         p_427202_,
         this.materials.get(material3),
         false,
         false,
         -1,
         null,
         p_431873_
      );
   }

   public void getExtents(Consumer<Vector3fc> p_454995_) {
      PoseStack posestack = new PoseStack();
      this.neck.getExtentsForGui(posestack, p_454995_);
      this.top.getExtentsForGui(posestack, p_454995_);
      this.bottom.getExtentsForGui(posestack, p_454995_);
   }
}
