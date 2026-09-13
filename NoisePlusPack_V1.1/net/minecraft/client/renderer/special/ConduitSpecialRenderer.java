package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class ConduitSpecialRenderer implements NoDataSpecialModelRenderer {
   private final MaterialSet materials;
   private final ModelPart model;

   public ConduitSpecialRenderer(MaterialSet p_422883_, ModelPart p_377637_) {
      this.materials = p_422883_;
      this.model = p_377637_;
   }

   @Override
   public void submit(
      ItemDisplayContext p_430270_, PoseStack p_426882_, SubmitNodeCollector p_426178_, int p_429474_, int p_423720_, boolean p_426720_, int p_431906_
   ) {
      p_426882_.pushPose();
      p_426882_.translate(0.5F, 0.5F, 0.5F);
      p_426178_.submitModelPart(
         this.model,
         p_426882_,
         ConduitRenderer.SHELL_TEXTURE.renderType(RenderTypes::entitySolid),
         p_429474_,
         p_423720_,
         this.materials.get(ConduitRenderer.SHELL_TEXTURE),
         false,
         false,
         -1,
         null,
         p_431906_
      );
      p_426882_.popPose();
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_460569_) {
      PoseStack posestack = new PoseStack();
      posestack.translate(0.5F, 0.5F, 0.5F);
      this.model.getExtentsForGui(posestack, p_460569_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked() implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<ConduitSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new ConduitSpecialRenderer.Unbaked());

      @Override
      public MapCodec<ConduitSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_428162_) {
         return new ConduitSpecialRenderer(p_428162_.materials(), p_428162_.entityModelSet().bakeLayer(ModelLayers.CONDUIT_SHELL));
      }
   }
}
