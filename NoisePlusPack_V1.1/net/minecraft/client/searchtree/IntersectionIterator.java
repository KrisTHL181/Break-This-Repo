package net.minecraft.client.searchtree;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.Comparator;
import java.util.Iterator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IntersectionIterator<T> extends AbstractIterator<T> {
   private final PeekingIterator<T> firstIterator;
   private final PeekingIterator<T> secondIterator;
   private final Comparator<T> comparator;

   public IntersectionIterator(Iterator<T> p_235178_, Iterator<T> p_235179_, Comparator<T> p_235180_) {
      this.firstIterator = Iterators.peekingIterator(p_235178_);
      this.secondIterator = Iterators.peekingIterator(p_235179_);
      this.comparator = p_235180_;
   }

   protected T computeNext() {
      while (this.firstIterator.hasNext() && this.secondIterator.hasNext()) {
         int i = this.comparator.compare((T)this.firstIterator.peek(), (T)this.secondIterator.peek());
         if (i == 0) {
            this.secondIterator.next();
            return (T)this.firstIterator.next();
         }

         if (i < 0) {
            this.firstIterator.next();
         } else {
            this.secondIterator.next();
         }
      }

      return (T)this.endOfData();
   }
}
