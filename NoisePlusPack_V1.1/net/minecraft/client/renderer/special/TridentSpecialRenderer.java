package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class TridentSpecialRenderer implements NoDataSpecialModelRenderer {
   private final TridentModel model;

   public TridentSpecialRenderer(TridentModel p_458874_) {
      this.model = p_458874_;
   }

   @Override
   public void submit(
      ItemDisplayContext p_431154_, PoseStack p_427229_, SubmitNodeCollector p_422945_, int p_429484_, int p_426094_, boolean p_428494_, int p_431877_
   ) {
      p_427229_.pushPose();
      p_427229_.scale(1.0F, -1.0F, -1.0F);
      p_422945_.submitModelPart(
         this.model.root(), p_427229_, this.model.renderType(TridentModel.TEXTURE), p_429484_, p_426094_, null, false, p_428494_, -1, null, p_431877_
      );
      p_427229_.popPose();
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_454785_) {
      PoseStack posestack = new PoseStack();
      posestack.scale(1.0F, -1.0F, -1.0F);
      this.model.root().getExtentsForGui(posestack, p_454785_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked() implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<TridentSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new TridentSpecialRenderer.Unbaked());

      @Override
      public MapCodec<TridentSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_422967_) {
         return new TridentSpecialRenderer(new TridentModel(p_422967_.entityModelSet().bakeLayer(ModelLayers.TRIDENT)));
      }
   }
}
