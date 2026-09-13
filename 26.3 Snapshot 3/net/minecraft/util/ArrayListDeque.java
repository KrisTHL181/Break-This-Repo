package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

public class ArrayListDeque<T> extends AbstractList<T> implements ListAndDeque<T> {
   private static final int MIN_GROWTH = 1;
   private @Nullable Object[] contents;
   private int head;
   private int size;

   public ArrayListDeque() {
      this(1);
   }

   public ArrayListDeque(final int capacity) {
      this.contents = new Object[capacity];
      this.head = 0;
      this.size = 0;
   }

   @Override
   public int size() {
      return this.size;
   }

   @VisibleForTesting
   public int capacity() {
      return this.contents.length;
   }

   private int getIndex(final int index) {
      return (index + this.head) % this.contents.length;
   }

   @Override
   public T get(final int index) {
      this.verifyIndexInRange(index);
      return this.getInner(this.getIndex(index));
   }

   private static void verifyIndexInRange(final int index, final int size) {
      if (index < 0 || index >= size) {
         throw new IndexOutOfBoundsException(index);
      }
   }

   private void verifyIndexInRange(final int index) {
      verifyIndexInRange(index, this.size);
   }

   private T getInner(final int innerIndex) {
      return (T)this.contents[innerIndex];
   }

   @Override
   public T set(final int index, final T element) {
      this.verifyIndexInRange(index);
      Objects.requireNonNull(element);
      int innerIndex = this.getIndex(index);
      T current = this.getInner(innerIndex);
      this.contents[innerIndex] = element;
      return current;
   }

   @Override
   public void add(final int index, final T element) {
      verifyIndexInRange(index, this.size + 1);
      Objects.requireNonNull(element);
      if (this.size == this.contents.length) {
         this.grow();
      }

      int internalIndex = this.getIndex(index);
      if (index == this.size) {
         this.contents[internalIndex] = element;
      } else if (index == 0) {
         this.head--;
         if (this.head < 0) {
            this.head = this.head + this.contents.length;
         }

         this.contents[this.getIndex(0)] = element;
      } else {
         for (int i = this.size - 1; i >= index; i--) {
            this.contents[this.getIndex(i + 1)] = this.contents[this.getIndex(i)];
         }

         this.contents[internalIndex] = element;
      }

      this.modCount++;
      this.size++;
   }

   private void grow() {
      int newLength = this.contents.length + Math.max(this.contents.length >> 1, 1);
      Object[] newContents = new Object[newLength];
      this.copyCount(newContents, this.size);
      this.head = 0;
      this.contents = newContents;
   }

   @Override
   public T remove(final int index) {
      this.verifyIndexInRange(index);
      int innerIndex = this.getIndex(index);
      T value = this.getInner(innerIndex);
      if (index == 0) {
         this.contents[innerIndex] = null;
         this.head++;
      } else if (index == this.size - 1) {
         this.contents[innerIndex] = null;
      } else {
         for (int i = index + 1; i < this.size; i++) {
            this.contents[this.getIndex(i - 1)] = this.get(i);
         }

         this.contents[this.getIndex(this.size - 1)] = null;
      }

      this.modCount++;
      this.size--;
      return value;
   }

   @Override
   public boolean removeIf(final Predicate<? super T> filter) {
      int removed = 0;

      for (int i = 0; i < this.size; i++) {
         T value = this.get(i);
         if (filter.test(value)) {
            removed++;
         } else if (removed != 0) {
            this.contents[this.getIndex(i - removed)] = value;
            this.contents[this.getIndex(i)] = null;
         }
      }

      this.modCount += removed;
      this.size -= removed;
      return removed != 0;
   }

   private void copyCount(final Object[] newContents, final int count) {
      for (int i = 0; i < count; i++) {
         newContents[i] = this.get(i);
      }
   }

   @Override
   public void replaceAll(final UnaryOperator<T> operator) {
      for (int i = 0; i < this.size; i++) {
         int index = this.getIndex(i);
         this.contents[index] = Objects.requireNonNull(operator.apply(this.getInner(i)));
      }
   }

   @Override
   public void forEach(final Consumer<? super T> action) {
      for (int i = 0; i < this.size; i++) {
         action.accept(this.get(i));
      }
   }

   @Override
   public void addFirst(final T value) {
      this.add(0, value);
   }

   @Override
   public void addLast(final T value) {
      this.add(this.size, value);
   }

   @Override
   public boolean offerFirst(final T value) {
      this.addFirst(value);
      return true;
   }

   @Override
   public boolean offerLast(final T value) {
      this.addLast(value);
      return true;
   }

   @Override
   public T removeFirst() {
      if (this.size == 0) {
         throw new NoSuchElementException();
      } else {
         return this.remove(0);
      }
   }

   @Override
   public T removeLast() {
      if (this.size == 0) {
         throw new NoSuchElementException();
      } else {
         return this.remove(this.size - 1);
      }
   }

   @Override
   public ListAndDeque<T> reversed() {
      return new ArrayListDeque.ReversedView(this);
   }

   @Override
   public @Nullable T pollFirst() {
      return this.size == 0 ? null : this.removeFirst();
   }

   @Override
   public @Nullable T pollLast() {
      return this.size == 0 ? null : this.removeLast();
   }

   @Override
   public T getFirst() {
      if (this.size == 0) {
         throw new NoSuchElementException();
      } else {
         return this.get(0);
      }
   }

   @Override
   public T getLast() {
      if (this.size == 0) {
         throw new NoSuchElementException();
      } else {
         return this.get(this.size - 1);
      }
   }

   @Override
   public @Nullable T peekFirst() {
      return this.size == 0 ? null : this.getFirst();
   }

   @Override
   public @Nullable T peekLast() {
      return this.size == 0 ? null : this.getLast();
   }

   @Override
   public boolean removeFirstOccurrence(final Object o) {
      for (int i = 0; i < this.size; i++) {
         T value = this.get(i);
         if (Objects.equals(o, value)) {
            this.remove(i);
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean removeLastOccurrence(final Object o) {
      for (int i = this.size - 1; i >= 0; i--) {
         T value = this.get(i);
         if (Objects.equals(o, value)) {
            this.remove(i);
            return true;
         }
      }

      return false;
   }

   @Override
   public Iterator<T> descendingIterator() {
      return new ArrayListDeque.DescendingIterator();
   }

   private class DescendingIterator implements Iterator<T> {
      private int index = ArrayListDeque.this.size() - 1;

      public DescendingIterator() {
      }

      @Override
      public boolean hasNext() {
         return this.index >= 0;
      }

      @Override
      public T next() {
         return ArrayListDeque.this.get(this.index--);
      }

      @Override
      public void remove() {
         ArrayListDeque.this.remove(this.index + 1);
      }
   }

   private class ReversedView extends AbstractList<T> implements ListAndDeque<T> {
      private final ArrayListDeque<T> source;

      public ReversedView(final ArrayListDeque<T> source) {
         this.source = source;
      }

      @Override
      public ListAndDeque<T> reversed() {
         return this.source;
      }

      @Override
      public T getFirst() {
         return this.source.getLast();
      }

      @Override
      public T getLast() {
         return this.source.getFirst();
      }

      @Override
      public void addFirst(final T t) {
         this.source.addLast(t);
      }

      @Override
      public void addLast(final T t) {
         this.source.addFirst(t);
      }

      @Override
      public boolean offerFirst(final T t) {
         return this.source.offerLast(t);
      }

      @Override
      public boolean offerLast(final T t) {
         return this.source.offerFirst(t);
      }

      @Override
      public @Nullable T pollFirst() {
         return this.source.pollLast();
      }

      @Override
      public @Nullable T pollLast() {
         return this.source.pollFirst();
      }

      @Override
      public @Nullable T peekFirst() {
         return this.source.peekLast();
      }

      @Override
      public @Nullable T peekLast() {
         return this.source.peekFirst();
      }

      @Override
      public T removeFirst() {
         return this.source.removeLast();
      }

      @Override
      public T removeLast() {
         return this.source.removeFirst();
      }

      @Override
      public boolean removeFirstOccurrence(final Object o) {
         return this.source.removeLastOccurrence(o);
      }

      @Override
      public boolean removeLastOccurrence(final Object o) {
         return this.source.removeFirstOccurrence(o);
      }

      @Override
      public Iterator<T> descendingIterator() {
         return this.source.iterator();
      }

      @Override
      public int size() {
         return this.source.size();
      }

      @Override
      public boolean isEmpty() {
         return this.source.isEmpty();
      }

      @Override
      public boolean contains(final Object o) {
         return this.source.contains(o);
      }

      @Override
      public T get(final int index) {
         return this.source.get(this.reverseIndex(index));
      }

      @Override
      public T set(final int index, final T element) {
         return this.source.set(this.reverseIndex(index), element);
      }

      @Override
      public void add(final int index, final T element) {
         this.source.add(this.reverseIndex(index) + 1, element);
      }

      @Override
      public T remove(final int index) {
         return this.source.remove(this.reverseIndex(index));
      }

      @Override
      public int indexOf(final Object o) {
         return this.reverseIndex(this.source.lastIndexOf(o));
      }

      @Override
      public int lastIndexOf(final Object o) {
         return this.reverseIndex(this.source.indexOf(o));
      }

      @Override
      public List<T> subList(final int fromIndex, final int toIndex) {
         return this.source.subList(this.reverseIndex(toIndex) + 1, this.reverseIndex(fromIndex) + 1).reversed();
      }

      @Override
      public Iterator<T> iterator() {
         return this.source.descendingIterator();
      }

      @Override
      public void clear() {
         this.source.clear();
      }

      private int reverseIndex(final int index) {
         return index == -1 ? -1 : this.source.size() - 1 - index;
      }
   }
}
