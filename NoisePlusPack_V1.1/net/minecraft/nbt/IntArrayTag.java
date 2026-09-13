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
      public IntArrayTag load(DataInput p_128667_, NbtAccounter p_128669_) throws IOException {
         return new IntArrayTag(readAccounted(p_128667_, p_128669_));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197478_, StreamTagVisitor p_197479_, NbtAccounter p_301723_) throws IOException {
         return p_197479_.visit(readAccounted(p_197478_, p_301723_));
      }

      private static int[] readAccounted(DataInput p_301738_, NbtAccounter p_301754_) throws IOException {
         p_301754_.accountBytes(24L);
         int i = p_301738_.readInt();
         p_301754_.accountBytes(4L, i);
         int[] aint = new int[i];

         for (int j = 0; j < i; j++) {
            aint[j] = p_301738_.readInt();
         }

         return aint;
      }

      @Override
      public void skip(DataInput p_197476_, NbtAccounter p_301698_) throws IOException {
         p_197476_.skipBytes(p_197476_.readInt() * 4);
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

   public IntArrayTag(int[] p_128605_) {
      this.data = p_128605_;
   }

   @Override
   public void write(DataOutput p_128616_) throws IOException {
      p_128616_.writeInt(this.data.length);

      for (int i : this.data) {
         p_128616_.writeInt(i);
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
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitIntArray(this);
      return stringtagvisitor.build();
   }

   public IntArrayTag copy() {
      int[] aint = new int[this.data.length];
      System.arraycopy(this.data, 0, aint, 0, this.data.length);
      return new IntArrayTag(aint);
   }

   @Override
   public boolean equals(Object p_128647_) {
      return this == p_128647_ ? true : p_128647_ instanceof IntArrayTag && Arrays.equals(this.data, ((IntArrayTag)p_128647_).data);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }

   public int[] getAsIntArray() {
      return this.data;
   }

   @Override
   public void accept(TagVisitor p_177869_) {
      p_177869_.visitIntArray(this);
   }

   @Override
   public int size() {
      return this.data.length;
   }

   public IntTag get(int p_128608_) {
      return IntTag.valueOf(this.data[p_128608_]);
   }

   @Override
   public boolean setTag(int p_128613_, Tag p_128614_) {
      if (p_128614_ instanceof NumericTag numerictag) {
         this.data[p_128613_] = numerictag.intValue();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(int p_128632_, Tag p_128633_) {
      if (p_128633_ instanceof NumericTag numerictag) {
         this.data = ArrayUtils.add(this.data, p_128632_, numerictag.intValue());
         return true;
      } else {
         return false;
      }
   }

   public IntTag remove(int p_128627_) {
      int i = this.data[p_128627_];
      this.data = ArrayUtils.remove(this.data, p_128627_);
      return IntTag.valueOf(i);
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
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197474_) {
      return p_197474_.visit(this.data);
   }
}
