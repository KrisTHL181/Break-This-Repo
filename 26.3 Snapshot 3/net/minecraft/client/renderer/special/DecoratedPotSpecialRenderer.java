package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class DecoratedPotSpecialRenderer implements SpecialModelRenderer<PotDecorations> {
   private final DecoratedPotRenderer decoratedPotRenderer;

   public DecoratedPotSpecialRenderer(final DecoratedPotRenderer decoratedPotRenderer) {
      this.decoratedPotRenderer = decoratedPotRenderer;
   }

   public @Nullable PotDecorations extractArgument(final ItemStack stack) {
      return stack.get(DataComponents.POT_DECORATIONS);
   }

   public void submit(
      final @Nullable PotDecorations decorations,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final int overlayCoords,
      final boolean hasFoil,
      final int outlineColor
   ) {
      this.decoratedPotRenderer
         .submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, Objects.requireNonNullElse(decorations, PotDecorations.EMPTY), outlineColor);
   }

   @Override
   public void getExtents(final Consumer<Vector3fc> output) {
      this.decoratedPotRenderer.getExtents(output);
   }

   public record Unbaked() implements SpecialModelRenderer.Unbaked<PotDecorations> {
      public static final MapCodec<DecoratedPotSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new DecoratedPotSpecialRenderer.Unbaked());

      @Override
      public MapCodec<DecoratedPotSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      public DecoratedPotSpecialRenderer bake(final SpecialModelRenderer.BakingContext context) {
         return new DecoratedPotSpecialRenderer(new DecoratedPotRenderer(context));
      }
   }
}
