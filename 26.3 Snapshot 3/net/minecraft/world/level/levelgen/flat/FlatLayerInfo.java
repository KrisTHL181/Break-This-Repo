package net.minecraft.world.level.levelgen.flat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

public class FlatLayerInfo {
   public static final Codec<FlatLayerInfo> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter(FlatLayerInfo::getHeight),
            BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(l -> l.block)
         )
         .apply(i, FlatLayerInfo::new)
   );
   private final Holder<Block> block;
   private final int height;

   public FlatLayerInfo(final int height, final Block block) {
      this(height, block.builtInRegistryHolder());
   }

   public FlatLayerInfo(final int height, final Holder<Block> block) {
      this.height = height;
      this.block = block;
   }

   public int getHeight() {
      return this.height;
   }

   public BlockState getBlockState() {
      return this.block.value().defaultBlockState();
   }

   public FlatLayerInfo heightLimited(final int maxHeight) {
      return this.height > maxHeight ? new FlatLayerInfo(maxHeight, this.block) : this;
   }

   @Override
   public String toString() {
      return (this.height != 1 ? this.height + "*" : "") + this.block.getRegisteredName();
   }
}
