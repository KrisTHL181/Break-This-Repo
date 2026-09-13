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

   public ArrayListDeque(int p_299918_) {
      this.contents = new Object[p_299918_];
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

   private int getIndex(int p_299728_) {
      return (p_299728_ + this.head) % this.contents.length;
   }

   @Override
   public T get(int p_300499_) {
      this.verifyIndexInRange(p_300499_);
      return this.getInner(this.getIndex(p_300499_));
   }

   private static void verifyIndexInRange(int p_299791_, int p_299333_) {
      if (p_299791_ < 0 || p_299791_ >= p_299333_) {
         throw new IndexOutOfBoundsException(p_299791_);
      }
   }

   private void verifyIndexInRange(int p_298701_) {
      verifyIndexInRange(p_298701_, this.size);
   }

   private T getInner(int p_299306_) {
      return (T)this.contents[p_299306_];
   }

   @Override
   public T set(int p_300259_, T p_298094_) {
      this.verifyIndexInRange(p_300259_);
      Objects.requireNonNull(p_298094_);
      int i = this.getIndex(p_300259_);
      T t = this.getInner(i);
      this.contents[i] = p_298094_;
      return t;
   }

   @Override
   public void add(int p_301285_, T p_300734_) {
      verifyIndexInRange(p_301285_, this.size + 1);
      Objects.requireNonNull(p_300734_);
      if (this.size == this.contents.length) {
         this.grow();
      }

      int i = this.getIndex(p_301285_);
      if (p_301285_ == this.size) {
         this.contents[i] = p_300734_;
      } else if (p_301285_ == 0) {
         this.head--;
         if (this.head < 0) {
            this.head = this.head + this.contents.length;
         }

         this.contents[this.getIndex(0)] = p_300734_;
      } else {
         for (int j = this.size - 1; j >= p_301285_; j--) {
            this.contents[this.getIndex(j + 1)] = this.contents[this.getIndex(j)];
         }

         this.contents[i] = p_300734_;
      }

      this.modCount++;
      this.size++;
   }

   private void grow() {
      int i = this.contents.length + Math.max(this.contents.length >> 1, 1);
      Object[] aobject = new Object[i];
      this.copyCount(aobject, this.size);
      this.head = 0;
      this.contents = aobject;
   }

   @Override
   public T remove(int p_297670_) {
      this.verifyIndexInRange(p_297670_);
      int i = this.getIndex(p_297670_);
      T t = this.getInner(i);
      if (p_297670_ == 0) {
         this.contents[i] = null;
         this.head++;
      } else if (p_297670_ == this.size - 1) {
         this.contents[i] = null;
      } else {
         for (int j = p_297670_ + 1; j < this.size; j++) {
            this.contents[this.getIndex(j - 1)] = this.get(j);
         }

         this.contents[this.getIndex(this.size - 1)] = null;
      }

      this.modCount++;
      this.size--;
      return t;
   }

   @Override
   public boolean removeIf(Predicate<? super T> p_300785_) {
      int i = 0;

      for (int j = 0; j < this.size; j++) {
         T t = this.get(j);
         if (p_300785_.test(t)) {
            i++;
         } else if (i != 0) {
            this.contents[this.getIndex(j - i)] = t;
            this.contents[this.getIndex(j)] = null;
         }
      }

      this.modCount += i;
      this.size -= i;
      return i != 0;
   }

   private void copyCount(Object[] p_300471_, int p_298513_) {
      for (int i = 0; i < p_298513_; i++) {
         p_300471_[i] = this.get(i);
      }
   }

   @Override
   public void replaceAll(UnaryOperator<T> p_299491_) {
      for (int i = 0; i < this.size; i++) {
         int j = this.getIndex(i);
         this.contents[j] = Objects.requireNonNull(p_299491_.apply(this.getInner(i)));
      }
   }

   @Override
   public void forEach(Consumer<? super T> p_297273_) {
      for (int i = 0; i < this.size; i++) {
         p_297273_.accept(this.get(i));
      }
   }

   @Override
   public void addFirst(T p_300853_) {
      this.add(0, p_300853_);
   }

   @Override
   public void addLast(T p_301090_) {
      this.add(this.size, p_301090_);
   }

   @Override
   public boolean offerFirst(T p_300075_) {
      this.addFirst(p_300075_);
      return true;
   }

   @Override
   public boolean offerLast(T p_300597_) {
      this.addLast(p_300597_);
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
   public boolean removeFirstOccurrence(Object p_300960_) {
      for (int i = 0; i < this.size; i++) {
         T t = this.get(i);
         if (Objects.equals(p_300960_, t)) {
            this.remove(i);
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean removeLastOccurrence(Object p_297293_) {
      for (int i = this.size - 1; i >= 0; i--) {
         T t = this.get(i);
         if (Objects.equals(p_297293_, t)) {
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

   class DescendingIterator implements Iterator<T> {
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

   class ReversedView extends AbstractList<T> implements ListAndDeque<T> {
      private final ArrayListDeque<T> source;

      public ReversedView(final ArrayListDeque<T> p_335912_) {
         this.source = p_335912_;
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
      public void addFirst(T p_336272_) {
         this.source.addLast(p_336272_);
      }

      @Override
      public void addLast(T p_333987_) {
         this.source.addFirst(p_333987_);
      }

      @Override
      public boolean offerFirst(T p_331206_) {
         return this.source.offerLast(p_331206_);
      }

      @Override
      public boolean offerLast(T p_334399_) {
         return this.source.offerFirst(p_334399_);
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
      public boolean removeFirstOccurrence(Object p_332292_) {
         return this.source.removeLastOccurrence(p_332292_);
      }

      @Override
      public boolean removeLastOccurrence(Object p_328218_) {
         return this.source.removeFirstOccurrence(p_328218_);
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
      public boolean contains(Object p_328039_) {
         return this.source.contains(p_328039_);
      }

      @Override
      public T get(int p_330114_) {
         return this.source.get(this.reverseIndex(p_330114_));
      }

      @Override
      public T set(int p_328364_, T p_330947_) {
         return this.source.set(this.reverseIndex(p_328364_), p_330947_);
      }

      @Override
      public void add(int p_328176_, T p_334553_) {
         this.source.add(this.reverseIndex(p_328176_) + 1, p_334553_);
      }

      @Override
      public T remove(int p_334028_) {
         return this.source.remove(this.reverseIndex(p_334028_));
      }

      @Override
      public int indexOf(Object p_330150_) {
         return this.reverseIndex(this.source.lastIndexOf(p_330150_));
      }

      @Override
      public int lastIndexOf(Object p_332172_) {
         return this.reverseIndex(this.source.indexOf(p_332172_));
      }

      @Override
      public List<T> subList(int p_331831_, int p_330462_) {
         return this.source.subList(this.reverseIndex(p_330462_) + 1, this.reverseIndex(p_331831_) + 1).reversed();
      }

      @Override
      public Iterator<T> iterator() {
         return this.source.descendingIterator();
      }

      @Override
      public void clear() {
         this.source.clear();
      }

      private int reverseIndex(int p_335640_) {
         return p_335640_ == -1 ? -1 : this.source.size() - 1 - p_335640_;
      }
   }
}
