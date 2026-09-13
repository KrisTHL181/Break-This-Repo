package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record IntTag(int value) implements NumericTag {
   private static final int SELF_SIZE_IN_BYTES = 12;
   public static final TagType<IntTag> TYPE = new TagType.StaticSize<IntTag>() {
      public IntTag load(final DataInput input, final NbtAccounter accounter) throws IOException {
         return IntTag.valueOf(readAccounted(input, accounter));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
         return output.visit(readAccounted(input, accounter));
      }

      private static int readAccounted(final DataInput input, final NbtAccounter accounter) throws IOException {
         accounter.accountBytes(12L);
         return input.readInt();
      }

      @Override
      public int size() {
         return 4;
      }

      @Override
      public String getName() {
         return "INT";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Int";
      }
   };

   @Deprecated(forRemoval = true)
   public IntTag {
   }

   public static IntTag valueOf(final int i) {
      return i >= -128 && i <= 1024 ? IntTag.Cache.cache[i - -128] : new IntTag(i);
   }

   @Override
   public void write(final DataOutput output) throws IOException {
      output.writeInt(this.value);
   }

   @Override
   public int sizeInBytes() {
      return 12;
   }

   @Override
   public byte getId() {
      return 3;
   }

   @Override
   public TagType<IntTag> getType() {
      return TYPE;
   }

   public IntTag copy() {
      return this;
   }

   @Override
   public void accept(final TagVisitor visitor) {
      visitor.visitInt(this);
   }

   @Override
   public long longValue() {
      return this.value;
   }

   @Override
   public int intValue() {
      return this.value;
   }

   @Override
   public short shortValue() {
      return (short)(this.value & 65535);
   }

   @Override
   public byte byteValue() {
      return (byte)(this.value & 0xFF);
   }

   @Override
   public double doubleValue() {
      return this.value;
   }

   @Override
   public float floatValue() {
      return this.value;
   }

   @Override
   public Number box() {
      return this.value;
   }

   @Override
   public StreamTagVisitor.ValueResult accept(final StreamTagVisitor visitor) {
      return visitor.visit(this.value);
   }

   @Override
   public String toString() {
      StringTagVisitor visitor = new StringTagVisitor();
      visitor.visitInt(this);
      return visitor.build();
   }

   private static class Cache {
      private static final int HIGH = 1024;
      private static final int LOW = -128;
      private static final IntTag[] cache = new IntTag[1153];

      static {
         for (int i = 0; i < cache.length; i++) {
            cache[i] = new IntTag(-128 + i);
         }
      }
   }
}
