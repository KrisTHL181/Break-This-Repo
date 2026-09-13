package net.minecraft.util;

import java.io.Serializable;
import java.util.Deque;
import java.util.List;
import java.util.RandomAccess;
import org.jspecify.annotations.Nullable;

public interface ListAndDeque<T> extends List<T>, RandomAccess, Cloneable, Serializable, Deque<T> {
   ListAndDeque<T> reversed();

   @Override
   T getFirst();

   @Override
   T getLast();

   @Override
   void addFirst(T t);

   @Override
   void addLast(T t);

   @Override
   T removeFirst();

   @Override
   T removeLast();

   @Override
   default boolean offer(final T value) {
      return this.offerLast(value);
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
   default void push(final T value) {
      this.addFirst(value);
   }

   @Override
   default T pop() {
      return this.removeFirst();
   }
}
