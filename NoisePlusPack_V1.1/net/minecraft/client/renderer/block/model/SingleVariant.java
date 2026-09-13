package net.minecraft.client.renderer.block.model;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SingleVariant implements BlockStateModel {
   private final BlockModelPart model;

   public SingleVariant(BlockModelPart p_394592_) {
      this.model = p_394592_;
   }

   @Override
   public void collectParts(RandomSource p_397567_, List<BlockModelPart> p_396765_) {
      p_396765_.add(this.model);
   }

   @Override
   public TextureAtlasSprite particleIcon() {
      return this.model.particleIcon();
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(Variant variant) implements BlockStateModel.Unbaked {
      public static final Codec<SingleVariant.Unbaked> CODEC = Variant.CODEC.xmap(SingleVariant.Unbaked::new, SingleVariant.Unbaked::variant);

      @Override
      public BlockStateModel bake(ModelBaker p_397283_) {
         return new SingleVariant(this.variant.bake(p_397283_));
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_395676_) {
         this.variant.resolveDependencies(p_395676_);
      }
   }
}
