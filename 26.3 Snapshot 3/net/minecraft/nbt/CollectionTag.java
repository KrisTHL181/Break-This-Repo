package net.minecraft.nbt;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public sealed interface CollectionTag extends Tag, Iterable<Tag> permits ListTag, ByteArrayTag, IntArrayTag, LongArrayTag {
   void clear();

   boolean setTag(int index, Tag tag);

   boolean addTag(int index, Tag tag);

   Tag remove(int index);

   Tag get(int index);

   int size();

   default boolean isEmpty() {
      return this.size() == 0;
   }

   @Override
   default Iterator<Tag> iterator() {
      return new Iterator<Tag>() {
         private int index;

         @Override
         public boolean hasNext() {
            return this.index < CollectionTag.this.size();
         }

         public Tag next() {
            if (!this.hasNext()) {
               throw new NoSuchElementException();
            } else {
               return CollectionTag.this.get(this.index++);
            }
         }
      };
   }

   default Stream<Tag> stream() {
      return StreamSupport.stream(this.spliterator(), false);
   }
}
