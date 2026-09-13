package net.minecraft.world.level.block.state;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockState extends BlockBehaviour.BlockStateBase {
   public static final Codec<BlockState> CODEC = codec(BuiltInRegistries.BLOCK.byNameCodec(), Block::defaultBlockState, Block::getStateDefinition).stable();

   public BlockState(final Block owner, final Property<?>[] propertyKeys, final Comparable<?>[] propertyValues) {
      super(owner, propertyKeys, propertyValues);
   }

   @Override
   protected BlockState asState() {
      return this;
   }
}
