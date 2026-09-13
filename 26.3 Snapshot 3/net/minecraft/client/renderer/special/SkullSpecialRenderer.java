package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SkullBlock;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class SkullSpecialRenderer implements NoDataSpecialModelRenderer {
   private final SkullModelBase model;
   private final float animation;
   private final RenderType renderType;

   public SkullSpecialRenderer(final SkullModelBase model, final float animation, final RenderType renderType) {
      this.model = model;
      this.animation = animation;
      this.renderType = renderType;
   }

   @Override
   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final int overlayCoords,
      final boolean hasFoil,
      final int outlineColor
   ) {
      SkullBlockRenderer.submitSkull(this.animation, poseStack, submitNodeCollector, lightCoords, this.model, this.renderType, outlineColor, null);
   }

   @Override
   public void getExtents(final Consumer<Vector3fc> output) {
      PoseStack poseStack = new PoseStack();
      SkullModelBase.State modelState = new SkullModelBase.State();
      modelState.animationPos = this.animation;
      this.model.setupAnim(modelState);
      this.model.root().getExtentsForGui(poseStack, output);
   }

   public record Unbaked(SkullBlock.Type kind, Optional<Identifier> textureOverride, float animation) implements NoDataSpecialModelRenderer.Unbaked {
      public static final MapCodec<SkullSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               SkullBlock.Type.CODEC.fieldOf("kind").forGetter(SkullSpecialRenderer.Unbaked::kind),
               Identifier.CODEC.optionalFieldOf("texture").forGetter(SkullSpecialRenderer.Unbaked::textureOverride),
               Codec.FLOAT.optionalFieldOf("animation", 0.0F).forGetter(SkullSpecialRenderer.Unbaked::animation)
            )
            .apply(i, SkullSpecialRenderer.Unbaked::new)
      );

      public Unbaked(final SkullBlock.Type kind) {
         this(kind, Optional.empty(), 0.0F);
      }

      @Override
      public MapCodec<SkullSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      public @Nullable SkullSpecialRenderer bake(final SpecialModelRenderer.BakingContext context) {
         SkullModelBase model = SkullBlockRenderer.createModel(context.entityModelSet(), this.kind);
         Identifier textureOverride = this.textureOverride.<Identifier>map(t -> t.withPath(p -> "textures/entity/" + p + ".png")).orElse(null);
         if (model == null) {
            return null;
         }

         RenderType renderType = SkullBlockRenderer.getSkullRenderType(this.kind, textureOverride);
         return new SkullSpecialRenderer(model, this.animation, renderType);
      }
   }
}
