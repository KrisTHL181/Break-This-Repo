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
      public LongArrayTag load(final DataInput input, final NbtAccounter accounter) throws IOException {
         return new LongArrayTag(readAccounted(input, accounter));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
         return output.visit(readAccounted(input, accounter));
      }

      private static long[] readAccounted(final DataInput input, final NbtAccounter accounter) throws IOException {
         accounter.accountBytes(24L);
         int length = input.readInt();
         accounter.accountBytes(8L, length);
         long[] data = new long[length];

         for (int i = 0; i < length; i++) {
            data[i] = input.readLong();
         }

         return data;
      }

      @Override
      public void skip(final DataInput input, final NbtAccounter accounter) throws IOException {
         input.skipBytes(input.readInt() * 8);
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

   public LongArrayTag(final long[] data) {
      this.data = data;
   }

   @Override
   public void write(final DataOutput output) throws IOException {
      output.writeInt(this.data.length);

      for (long i : this.data) {
         output.writeLong(i);
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
      StringTagVisitor visitor = new StringTagVisitor();
      visitor.visitLongArray(this);
      return visitor.build();
   }

   public LongArrayTag copy() {
      long[] cp = new long[this.data.length];
      System.arraycopy(this.data, 0, cp, 0, this.data.length);
      return new LongArrayTag(cp);
   }

   @Override
   public boolean equals(final Object obj) {
      return this == obj ? true : obj instanceof LongArrayTag longArrayTag && Arrays.equals(this.data, longArrayTag.data);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }

   @Override
   public void accept(final TagVisitor visitor) {
      visitor.visitLongArray(this);
   }

   public long[] getAsLongArray() {
      return this.data;
   }

   @Override
   public int size() {
      return this.data.length;
   }

   public LongTag get(final int index) {
      return LongTag.valueOf(this.data[index]);
   }

   @Override
   public boolean setTag(final int index, final Tag tag) {
      if (tag instanceof NumericTag numeric) {
         this.data[index] = numeric.longValue();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(final int index, final Tag tag) {
      if (tag instanceof NumericTag numeric) {
         this.data = ArrayUtils.add(this.data, index, numeric.longValue());
         return true;
      } else {
         return false;
      }
   }

   public LongTag remove(final int index) {
      long prev = this.data[index];
      this.data = ArrayUtils.remove(this.data, index);
      return LongTag.valueOf(prev);
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
   public StreamTagVisitor.ValueResult accept(final StreamTagVisitor visitor) {
      return visitor.visit(this.data);
   }
}
