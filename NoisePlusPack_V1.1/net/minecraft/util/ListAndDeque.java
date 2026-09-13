package net.minecraft.util;

import java.io.Serializable;
import java.util.Deque;
import java.util.List;
import java.util.RandomAccess;
import org.jspecify.annotations.Nullable;

public interface ListAndDeque<T> extends Serializable, Cloneable, Deque<T>, List<T>, RandomAccess {
   ListAndDeque<T> reversed();

   @Override
   T getFirst();

   @Override
   T getLast();

   @Override
   void addFirst(T var1);

   @Override
   void addLast(T var1);

   @Override
   T removeFirst();

   @Override
   T removeLast();

   @Override
   default boolean offer(T p_331775_) {
      return this.offerLast(p_331775_);
   }

   @Override
   default T remove() {
      return this.removeFirst();
   }

   @Override
   default @Nullable T poll() {
      return this.pollFirst();
   }

   @Override
   default T element() {
      return this.getFirst();
   }

   @Override
   default @Nullable T peek() {
      return this.peekFirst();
   }

   @Override
   default void push(T p_327786_) {
      this.addFirst(p_327786_);
   }

   @Override
   default T pop() {
      return this.removeFirst();
   }
}
