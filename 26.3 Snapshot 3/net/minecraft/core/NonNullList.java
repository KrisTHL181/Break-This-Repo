package net.minecraft.core;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class NonNullList<E> extends AbstractList<E> {
   private final List<E> list;
   private final @Nullable E defaultValue;

   public static <E> NonNullList<E> create() {
      return new NonNullList<>(Lists.newArrayList(), null);
   }

   public static <E> NonNullList<E> createWithCapacity(final int capacity) {
      return new NonNullList<>(Lists.newArrayListWithCapacity(capacity), null);
   }

   public static <E> NonNullList<E> withSize(final int size, final E defaultValue) {
      Objects.requireNonNull(defaultValue);
      Object[] objects = new Object[size];
      Arrays.fill(objects, defaultValue);
      return new NonNullList<>(Arrays.asList((E[])objects), defaultValue);
   }

   @SafeVarargs
   public static <E> NonNullList<E> of(final E defaultValue, final E... values) {
      return new NonNullList<>(Arrays.asList(values), defaultValue);
   }

   protected NonNullList(final List<E> list, final @Nullable E defaultValue) {
      this.list = list;
      this.defaultValue = defaultValue;
   }

   @Override
   public E get(final int index) {
      return this.list.get(index);
   }

   @Override
   public E set(final int index, final E element) {
      Objects.requireNonNull(element);
      return this.list.set(index, element);
   }

   @Override
   public void add(final int index, final E element) {
      Objects.requireNonNull(element);
      this.list.add(index, element);
   }

   @Override
   public E remove(final int index) {
      return this.list.remove(index);
   }

   @Override
   public int size() {
      return this.list.size();
   }

   @Override
   public void clear() {
      if (this.defaultValue == null) {
         super.clear();
      } else {
         for (int i = 0; i < this.size(); i++) {
            this.set(i, this.defaultValue);
         }
      }
   }
}
