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
         BuiltInRegistries.BLOCK.holderByNameCodec(),
         p_329333_ -> Codec.STRING
            .comapFlatMap(
               p_332541_ -> {
                  Property<?> property = ((Block)p_329333_.value()).getStateDefinition().getProperty(p_332541_);
                  return property != null
                     ? DataResult.success(property)
                     : DataResult.error(() -> "No property on " + p_329333_.getRegisteredName() + " with name: " + p_332541_);
               },
               Property::getName
            )
      )
      .xmap(DebugStickState::new, DebugStickState::properties);

   public DebugStickState withProperty(Holder<Block> p_330343_, Property<?> p_334645_) {
      return new DebugStickState(Util.copyAndPut(this.properties, p_330343_, p_334645_));
   }
}
