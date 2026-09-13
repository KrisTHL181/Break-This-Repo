package net.minecraft.client.data.models;

import java.util.List;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record MultiVariant(WeightedList<Variant> variants) {
   public MultiVariant {
      if (variants.isEmpty()) {
         throw new IllegalArgumentException("Variant list must contain at least one element");
      }
   }

   public MultiVariant with(VariantMutator p_395610_) {
      return new MultiVariant(this.variants.map(p_395610_));
   }

   public BlockStateModel.Unbaked toUnbaked() {
      List<Weighted<Variant>> list = this.variants.unwrap();
      return (BlockStateModel.Unbaked)(
         list.size() == 1 ? new SingleVariant.Unbaked(list.getFirst().value()) : new WeightedVariants.Unbaked(this.variants.map(SingleVariant.Unbaked::new))
      );
   }
}
