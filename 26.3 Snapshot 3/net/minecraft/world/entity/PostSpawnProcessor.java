package net.minecraft.world.entity;

import java.util.Objects;

public interface PostSpawnProcessor<T extends Entity> {
   void apply(T target);

   default PostSpawnProcessor<T> andThen(final PostSpawnProcessor<? super T> after) {
      Objects.requireNonNull(after);
      return t -> {
         this.apply(t);
         after.apply(t);
      };
   }

   static <T extends Entity> PostSpawnProcessor<T> nop() {
      return var0 -> {};
   }
}
