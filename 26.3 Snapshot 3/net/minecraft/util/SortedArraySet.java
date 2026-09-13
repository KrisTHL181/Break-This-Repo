package net.minecraft.util;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;

public class SortedArraySet<T> extends AbstractSet<T> {
   private static final int DEFAULT_INITIAL_CAPACITY = 10;
   private final Comparator<T> comparator;
   private T[] contents;
   private int size;

   private SortedArraySet(final int initialCapacity, final Comparator<T> comparator) {
      this.comparator = comparator;
      if (initialCapacity < 0) {
         throw new IllegalArgumentException("Initial capacity (" + initialCapacity + ") is negative");
      }

      this.contents = (T[])castRawArray(new Object[initialCapacity]);
   }

   public static <T extends Comparable<T>> SortedArraySet<T> create() {
      return create(10);
   }

   public static <T extends Comparable<T>> SortedArraySet<T> create(final int initialCapacity) {
      return new SortedArraySet<>(initialCapacity, Comparator.naturalOrder());
   }

   public static <T> SortedArraySet<T> create(final Comparator<T> comparator) {
      return create(comparator, 10);
   }

   public static <T> SortedArraySet<T> create(final Comparator<T> comparator, final int initialCapacity) {
      return new SortedArraySet<>(initialCapacity, comparator);
   }

   private static <T> T[] castRawArray(final Object[] array) {
      return (T[])array;
   }

   private int findIndex(final T t) {
      return Arrays.binarySearch(this.contents, 0, this.size, t, this.comparator);
   }

   private static int getInsertionPosition(final int position) {
      return -position - 1;
   }

   @Override
   public boolean add(final T t) {
      int position = this.findIndex(t);
      if (position >= 0) {
         return false;
      }

      int pos = getInsertionPosition(position);
      this.addInternal(t, pos);
      return true;
   }

   private void grow(int capacity) {
      if (capacity > this.contents.length) {
         if (this.contents != ObjectArrays.DEFAULT_EMPTY_ARRAY) {
            capacity = Util.growByHalf(this.contents.length, capacity);
         } else if (capacity < 10) {
            capacity = 10;
         }

         Object[] t = new Object[capacity];
         System.arraycopy(this.contents, 0, t, 0, this.size);
         this.contents = (T[])castRawArray(t);
      }
   }

   private void addInternal(final T t, final int pos) {
      this.grow(this.size + 1);
      if (pos != this.size) {
         System.arraycopy(this.contents, pos, this.contents, pos + 1, this.size - pos);
      }

      this.contents[pos] = t;
      this.size++;
   }

   private void removeInternal(final int position) {
      this.size--;
      if (position != this.size) {
         System.arraycopy(this.contents, position + 1, this.contents, position, this.size - position);
      }

      this.contents[this.size] = null;
   }

   private T getInternal(final int position) {
      return this.contents[position];
   }

   public T addOrGet(final T t) {
      int position = this.findIndex(t);
      if (position >= 0) {
         return this.getInternal(position);
      }

      this.addInternal(t, getInsertionPosition(position));
      return t;
   }

   @Override
   public boolean remove(final Object o) {
      int position = this.findIndex((T)o);
      if (position >= 0) {
         this.removeInternal(position);
         return true;
      } else {
         return false;
      }
   }

   public @Nullable T get(final T t) {
      int position = this.findIndex(t);
      return position >= 0 ? this.getInternal(position) : null;
   }

   public T first() {
      return this.getInternal(0);
   }

   public T last() {
      return this.getInternal(this.size - 1);
   }

   @Override
   public boolean contains(final Object o) {
      int result = this.findIndex((T)o);
      return result >= 0;
   }

   @Override
   public Iterator<T> iterator() {
      return new SortedArraySet.ArrayIterator();
   }

   @Override
   public int size() {
      return this.size;
   }

   @Override
   public Object[] toArray() {
      return Arrays.copyOf(this.contents, this.size, Object[].class);
   }

   @Override
   public <U> U[] toArray(final U[] a) {
      if (a.length < this.size) {
         return (U[])Arrays.copyOf(this.contents, this.size, (Class<? extends T[]>)a.getClass());
      }

      System.arraycopy(this.contents, 0, a, 0, this.size);
      if (a.length > this.size) {
         a[this.size] = null;
      }

      return a;
   }

   @Override
   public void clear() {
      Arrays.fill(this.contents, 0, this.size, null);
      this.size = 0;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else {
         return o instanceof SortedArraySet<?> that && this.comparator.equals(that.comparator)
            ? this.size == that.size && Arrays.equals(this.contents, that.contents)
            : super.equals(o);
      }
   }

   private class ArrayIterator implements Iterator<T> {
      private int index;
      private int last = -1;

      @Override
      public boolean hasNext() {
         return this.index < SortedArraySet.this.size;
      }

      @Override
      public T next() {
         if (this.index >= SortedArraySet.this.size) {
            throw new NoSuchElementException();
         }

         this.last = this.index++;
         return SortedArraySet.this.contents[this.last];
      }

      @Override
      public void remove() {
         if (this.last == -1) {
            throw new IllegalStateException();
         }

         SortedArraySet.this.removeInternal(this.last);
         this.index--;
         this.last = -1;
      }
   }
}
