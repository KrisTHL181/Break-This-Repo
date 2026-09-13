package net.minecraft.client.renderer;

import java.util.function.Function;
import net.minecraft.world.level.block.state.properties.ChestType;

public record MultiblockChestResources<T>(T single, T left, T right) {
   public T select(final ChestType chestType) {
      return (T)(switch (chestType) {
         case SINGLE -> this.single;
         case LEFT -> this.left;
         case RIGHT -> this.right;
      });
   }

   public <S> MultiblockChestResources<S> map(final Function<T, S> mapper) {
      return new MultiblockChestResources<>(mapper.apply(this.single), mapper.apply(this.left), mapper.apply(this.right));
   }
}
