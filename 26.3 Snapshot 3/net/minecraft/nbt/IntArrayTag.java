package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;

public final class IntArrayTag implements CollectionTag {
   private static final int SELF_SIZE_IN_BYTES = 24;
   public static final TagType<IntArrayTag> TYPE = new TagType.VariableSize<IntArrayTag>() {
      public IntArrayTag load(final DataInput input, final NbtAccounter accounter) throws IOException {
         return new IntArrayTag(readAccounted(input, accounter));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
         return output.visit(readAccounted(input, accounter));
      }

      private static int[] readAccounted(final DataInput input, final NbtAccounter accounter) throws IOException {
         accounter.accountBytes(24L);
         int length = input.readInt();
         accounter.accountBytes(4L, length);
         int[] data = new int[length];

         for (int i = 0; i < length; i++) {
            data[i] = input.readInt();
         }

         return data;
      }

      @Override
      public void skip(final DataInput input, final NbtAccounter accounter) throws IOException {
         input.skipBytes(input.readInt() * 4);
      }

      @Override
      public String getName() {
         return "INT[]";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Int_Array";
      }
   };
   private int[] data;

   public IntArrayTag(final int[] data) {
      this.data = data;
   }

   @Override
   public void write(final DataOutput output) throws IOException {
      output.writeInt(this.data.length);

      for (int i : this.data) {
         output.writeInt(i);
      }
   }

   @Override
   public int sizeInBytes() {
      return 24 + 4 * this.data.length;
   }

   @Override
   public byte getId() {
      return 11;
   }

   @Override
   public TagType<IntArrayTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      StringTagVisitor visitor = new StringTagVisitor();
      visitor.visitIntArray(this);
      return visitor.build();
   }

   public IntArrayTag copy() {
      int[] cp = new int[this.data.length];
      System.arraycopy(this.data, 0, cp, 0, this.data.length);
      return new IntArrayTag(cp);
   }

   @Override
   public boolean equals(final Object obj) {
      return this == obj ? true : obj instanceof IntArrayTag intArrayTag && Arrays.equals(this.data, intArrayTag.data);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }

   public int[] getAsIntArray() {
      return this.data;
   }

   @Override
   public void accept(final TagVisitor visitor) {
      visitor.visitIntArray(this);
   }

   @Override
   public int size() {
      return this.data.length;
   }

   public IntTag get(final int index) {
      return IntTag.valueOf(this.data[index]);
   }

   @Override
   public boolean setTag(final int index, final Tag tag) {
      if (tag instanceof NumericTag numeric) {
         this.data[index] = numeric.intValue();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(final int index, final Tag tag) {
      if (tag instanceof NumericTag numeric) {
         this.data = ArrayUtils.add(this.data, index, numeric.intValue());
         return true;
      } else {
         return false;
      }
   }

   public IntTag remove(final int index) {
      int prev = this.data[index];
      this.data = ArrayUtils.remove(this.data, index);
      return IntTag.valueOf(prev);
   }

   @Override
   public void clear() {
      this.data = new int[0];
   }

   @Override
   public Optional<int[]> asIntArray() {
      return Optional.of(this.data);
   }

   @Override
   public StreamTagVisitor.ValueResult accept(final StreamTagVisitor visitor) {
      return visitor.visit(this.data);
   }
}
