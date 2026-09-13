package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DecoratedPotSpecialRenderer implements SpecialModelRenderer<PotDecorations> {
   private final DecoratedPotRenderer decoratedPotRenderer;

   public DecoratedPotSpecialRenderer(DecoratedPotRenderer p_377806_) {
      this.decoratedPotRenderer = p_377806_;
   }

   public @Nullable PotDecorations extractArgument(ItemStack p_375578_) {
      return p_375578_.get(DataComponents.POT_DECORATIONS);
   }

   public void submit(
      @Nullable PotDecorations p_428984_,
      ItemDisplayContext p_432000_,
      PoseStack p_432001_,
      SubmitNodeCollector p_432002_,
      int p_432003_,
      int p_432004_,
      boolean p_432005_,
      int p_431867_
   ) {
      this.decoratedPotRenderer.submit(p_432001_, p_432002_, p_432003_, p_432004_, Objects.requireNonNullElse(p_428984_, PotDecorations.EMPTY), p_431867_);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_455215_) {
      this.decoratedPotRenderer.getExtents(p_455215_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked() implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<DecoratedPotSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new DecoratedPotSpecialRenderer.Unbaked());

      @Override
      public MapCodec<DecoratedPotSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_428106_) {
         return new DecoratedPotSpecialRenderer(new DecoratedPotRenderer(p_428106_));
      }
   }
}
