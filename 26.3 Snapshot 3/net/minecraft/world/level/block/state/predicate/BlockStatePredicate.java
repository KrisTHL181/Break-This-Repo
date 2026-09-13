package net.minecraft.world.level.block.state.predicate;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class BlockStatePredicate implements Predicate<BlockState> {
   public static final Predicate<BlockState> ANY = input -> true;
   private final StateDefinition<Block, BlockState> definition;
   private final Map<Property<?>, Predicate<Object>> properties = Maps.newHashMap();

   private BlockStatePredicate(final StateDefinition<Block, BlockState> definition) {
      this.definition = definition;
   }

   public static BlockStatePredicate forBlock(final Block block) {
      return new BlockStatePredicate(block.getStateDefinition());
   }

   public boolean test(final @Nullable BlockState input) {
      if (input != null && input.getBlock().equals(this.definition.getOwner())) {
         if (this.properties.isEmpty()) {
            return true;
         }

         for (Entry<Property<?>, Predicate<Object>> entry : this.properties.entrySet()) {
            if (!this.applies(input, entry.getKey(), entry.getValue())) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   protected <T extends Comparable<T>> boolean applies(final BlockState input, final Property<T> key, final Predicate<Object> predicate) {
      T value = input.getValue(key);
      return predicate.test(value);
   }

   public <V extends Comparable<V>> BlockStatePredicate where(final Property<V> property, final Predicate<Object> predicate) {
      if (!this.definition.getProperties().contains(property)) {
         throw new IllegalArgumentException(this.definition + " cannot support property " + property);
      }

      this.properties.put(property, predicate);
      return this;
   }
}
