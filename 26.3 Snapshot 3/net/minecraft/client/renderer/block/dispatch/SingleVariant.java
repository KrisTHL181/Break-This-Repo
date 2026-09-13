package net.minecraft.client.renderer.block.dispatch;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;

public class SingleVariant implements BlockStateModel {
   private final BlockStateModelPart model;

   public SingleVariant(final BlockStateModelPart model) {
      this.model = model;
   }

   @Override
   public void collectParts(final RandomSource random, final List<BlockStateModelPart> output) {
      output.add(this.model);
   }

   @Override
   public Material.Baked particleMaterial() {
      return this.model.particleMaterial();
   }

   @Override
   public @BakedQuad.MaterialFlags int materialFlags() {
      return this.model.materialFlags();
   }

   public record Unbaked(Variant variant) implements BlockStateModel.Unbaked {
      public static final Codec<SingleVariant.Unbaked> CODEC = Variant.CODEC.xmap(SingleVariant.Unbaked::new, SingleVariant.Unbaked::variant);

      @Override
      public BlockStateModel bake(final ModelBaker modelBakery) {
         return new SingleVariant(this.variant.bake(modelBakery));
      }

      @Override
      public void resolveDependencies(final ResolvableModel.Resolver resolver) {
         this.variant.resolveDependencies(resolver);
      }
   }
}
