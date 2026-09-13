package net.minecraft.world.level.block.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockState extends BlockBehaviour.BlockStateBase {
   public static final Codec<BlockState> CODEC = codec(BuiltInRegistries.BLOCK.byNameCodec(), Block::defaultBlockState).stable();

   public BlockState(Block p_61042_, Reference2ObjectArrayMap<Property<?>, Comparable<?>> p_328252_, MapCodec<BlockState> p_61044_) {
      super(p_61042_, p_328252_, p_61044_);
   }

   @Override
   protected BlockState asState() {
      return this;
   }
}
