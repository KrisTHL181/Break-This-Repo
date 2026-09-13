package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

public record DebugStickState(Map<Holder<Block>, Property<?>> properties) {
   public static final DebugStickState EMPTY = new DebugStickState(Map.of());
   public static final Codec<DebugStickState> CODEC = Codec.dispatchedMap(
         BuiltInRegistries.BLOCK.holderByNameCodec(), block -> Codec.STRING.comapFlatMap(name -> {
            Property<?> property = ((Block)block.value()).getStateDefinition().getProperty(name);
            return property != null
               ? DataResult.success(property)
               : DataResult.error(() -> "No property on " + block.getRegisteredName() + " with name: " + name);
         }, Property::getName)
      )
      .xmap(DebugStickState::new, DebugStickState::properties);

   public DebugStickState withProperty(final Holder<Block> block, final Property<?> property) {
      return new DebugStickState(Util.copyAndPut(this.properties, block, property));
   }
}
