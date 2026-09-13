package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.statue.CopperGolemStatueModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class CopperGolemStatueSpecialRenderer implements NoDataSpecialModelRenderer {
   private static final Direction MODEL_STATE = Direction.SOUTH;
   private final CopperGolemStatueModel model;
   private final Identifier texture;

   public CopperGolemStatueSpecialRenderer(CopperGolemStatueModel p_453012_, Identifier p_457828_) {
      this.model = p_453012_;
      this.texture = p_457828_;
   }

   @Override
   public void submit(
      ItemDisplayContext p_429891_, PoseStack p_422495_, SubmitNodeCollector p_422708_, int p_426547_, int p_423523_, boolean p_427797_, int p_431896_
   ) {
      positionModel(p_422495_);
      p_422708_.submitModel(
         this.model, Direction.SOUTH, p_422495_, RenderTypes.entityCutoutNoCull(this.texture), p_426547_, p_423523_, -1, null, p_431896_, null
      );
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_458316_) {
      PoseStack posestack = new PoseStack();
      positionModel(posestack);
      this.model.setupAnim(MODEL_STATE);
      this.model.root().getExtentsForGui(posestack, p_458316_);
   }

   private static void positionModel(PoseStack p_430903_) {
      p_430903_.translate(0.5F, 1.5F, 0.5F);
      p_430903_.scale(-1.0F, -1.0F, 1.0F);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(Identifier texture, CopperGolemStatueBlock.Pose pose) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<CopperGolemStatueSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448368_ -> p_448368_.group(
               Identifier.CODEC.fieldOf("texture").forGetter(CopperGolemStatueSpecialRenderer.Unbaked::texture),
               CopperGolemStatueBlock.Pose.CODEC.fieldOf("pose").forGetter(CopperGolemStatueSpecialRenderer.Unbaked::pose)
            )
            .apply(p_448368_, CopperGolemStatueSpecialRenderer.Unbaked::new)
      );

      public Unbaked(WeatheringCopper.WeatherState p_424329_, CopperGolemStatueBlock.Pose p_426378_) {
         this(CopperGolemOxidationLevels.getOxidationLevel(p_424329_).texture(), p_426378_);
      }

      @Override
      public MapCodec<CopperGolemStatueSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_431217_) {
         CopperGolemStatueModel coppergolemstatuemodel = new CopperGolemStatueModel(p_431217_.entityModelSet().bakeLayer(getModel(this.pose)));
         return new CopperGolemStatueSpecialRenderer(coppergolemstatuemodel, this.texture);
      }

      private static ModelLayerLocation getModel(CopperGolemStatueBlock.Pose p_456564_) {
         return switch (p_456564_) {
            case STANDING -> ModelLayers.COPPER_GOLEM;
            case SITTING -> ModelLayers.COPPER_GOLEM_SITTING;
            case STAR -> ModelLayers.COPPER_GOLEM_STAR;
            case RUNNING -> ModelLayers.COPPER_GOLEM_RUNNING;
         };
      }
   }
}
