package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record LongTag(long value) implements NumericTag {
   private static final int SELF_SIZE_IN_BYTES = 16;
   public static final TagType<LongTag> TYPE = new TagType.StaticSize<LongTag>() {
      public LongTag load(final DataInput input, final NbtAccounter accounter) throws IOException {
         return LongTag.valueOf(readAccounted(input, accounter));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
         return output.visit(readAccounted(input, accounter));
      }

      private static long readAccounted(final DataInput input, final NbtAccounter accounter) throws IOException {
         accounter.accountBytes(16L);
         return input.readLong();
      }

      @Override
      public int size() {
         return 8;
      }

      @Override
      public String getName() {
         return "LONG";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Long";
      }
   };

   @Deprecated(forRemoval = true)
   public LongTag {
   }

   public static LongTag valueOf(final long i) {
      return i >= -128L && i <= 1024L ? LongTag.Cache.cache[(int)i - -128] : new LongTag(i);
   }

   @Override
   public void write(final DataOutput output) throws IOException {
      output.writeLong(this.value);
   }

   @Override
   public int sizeInBytes() {
      return 16;
   }

   @Override
   public byte getId() {
      return 4;
   }

   @Override
   public TagType<LongTag> getType() {
      return TYPE;
   }

   public LongTag copy() {
      return this;
   }

   @Override
   public void accept(final TagVisitor visitor) {
      visitor.visitLong(this);
   }

   @Override
   public long longValue() {
      return this.value;
   }

   @Override
   public int intValue() {
      return (int)(this.value & -1L);
   }

   @Override
   public short shortValue() {
      return (short)(this.value & 65535L);
   }

   @Override
   public byte byteValue() {
      return (byte)(this.value & 255L);
   }

   @Override
   public double doubleValue() {
      return this.value;
   }

   @Override
   public float floatValue() {
      return (float)this.value;
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
      visitor.visitLong(this);
      return visitor.build();
   }

   private static class Cache {
      private static final int HIGH = 1024;
      private static final int LOW = -128;
      private static final LongTag[] cache = new LongTag[1153];

      static {
         for (int i = 0; i < cache.length; i++) {
            cache[i] = new LongTag(-128 + i);
         }
      }
   }
}
