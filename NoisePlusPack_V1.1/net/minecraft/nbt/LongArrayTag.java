package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;

public final class LongArrayTag implements CollectionTag {
   private static final int SELF_SIZE_IN_BYTES = 24;
   public static final TagType<LongArrayTag> TYPE = new TagType.VariableSize<LongArrayTag>() {
      public LongArrayTag load(DataInput p_128865_, NbtAccounter p_128867_) throws IOException {
         return new LongArrayTag(readAccounted(p_128865_, p_128867_));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197501_, StreamTagVisitor p_197502_, NbtAccounter p_301749_) throws IOException {
         return p_197502_.visit(readAccounted(p_197501_, p_301749_));
      }

      private static long[] readAccounted(DataInput p_301699_, NbtAccounter p_301773_) throws IOException {
         p_301773_.accountBytes(24L);
         int i = p_301699_.readInt();
         p_301773_.accountBytes(8L, i);
         long[] along = new long[i];

         for (int j = 0; j < i; j++) {
            along[j] = p_301699_.readLong();
         }

         return along;
      }

      @Override
      public void skip(DataInput p_197499_, NbtAccounter p_301708_) throws IOException {
         p_197499_.skipBytes(p_197499_.readInt() * 8);
      }

      @Override
      public String getName() {
         return "LONG[]";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Long_Array";
      }
   };
   private long[] data;

   public LongArrayTag(long[] p_128808_) {
      this.data = p_128808_;
   }

   @Override
   public void write(DataOutput p_128819_) throws IOException {
      p_128819_.writeInt(this.data.length);

      for (long i : this.data) {
         p_128819_.writeLong(i);
      }
   }

   @Override
   public int sizeInBytes() {
      return 24 + 8 * this.data.length;
   }

   @Override
   public byte getId() {
      return 12;
   }

   @Override
   public TagType<LongArrayTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitLongArray(this);
      return stringtagvisitor.build();
   }

   public LongArrayTag copy() {
      long[] along = new long[this.data.length];
      System.arraycopy(this.data, 0, along, 0, this.data.length);
      return new LongArrayTag(along);
   }

   @Override
   public boolean equals(Object p_128850_) {
      return this == p_128850_ ? true : p_128850_ instanceof LongArrayTag && Arrays.equals(this.data, ((LongArrayTag)p_128850_).data);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }

   @Override
   public void accept(TagVisitor p_177995_) {
      p_177995_.visitLongArray(this);
   }

   public long[] getAsLongArray() {
      return this.data;
   }

   @Override
   public int size() {
      return this.data.length;
   }

   public LongTag get(int p_128811_) {
      return LongTag.valueOf(this.data[p_128811_]);
   }

   @Override
   public boolean setTag(int p_128816_, Tag p_128817_) {
      if (p_128817_ instanceof NumericTag numerictag) {
         this.data[p_128816_] = numerictag.longValue();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(int p_128835_, Tag p_128836_) {
      if (p_128836_ instanceof NumericTag numerictag) {
         this.data = ArrayUtils.add(this.data, p_128835_, numerictag.longValue());
         return true;
      } else {
         return false;
      }
   }

   public LongTag remove(int p_128830_) {
      long i = this.data[p_128830_];
      this.data = ArrayUtils.remove(this.data, p_128830_);
      return LongTag.valueOf(i);
   }

   @Override
   public void clear() {
      this.data = new long[0];
   }

   @Override
   public Optional<long[]> asLongArray() {
      return Optional.of(this.data);
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197497_) {
      return p_197497_.visit(this.data);
   }
}
