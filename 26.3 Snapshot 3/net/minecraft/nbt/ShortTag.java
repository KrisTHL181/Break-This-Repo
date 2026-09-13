package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record ShortTag(short value) implements NumericTag {
   private static final int SELF_SIZE_IN_BYTES = 10;
   public static final TagType<ShortTag> TYPE = new TagType.StaticSize<ShortTag>() {
      public ShortTag load(final DataInput input, final NbtAccounter accounter) throws IOException {
         return ShortTag.valueOf(readAccounted(input, accounter));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
         return output.visit(readAccounted(input, accounter));
      }

      private static short readAccounted(final DataInput input, final NbtAccounter accounter) throws IOException {
         accounter.accountBytes(10L);
         return input.readShort();
      }

      @Override
      public int size() {
         return 2;
      }

      @Override
      public String getName() {
         return "SHORT";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Short";
      }
   };

   @Deprecated(forRemoval = true)
   public ShortTag {
   }

   public static ShortTag valueOf(final short i) {
      return i >= -128 && i <= 1024 ? ShortTag.Cache.cache[i - -128] : new ShortTag(i);
   }

   @Override
   public void write(final DataOutput output) throws IOException {
      output.writeShort(this.value);
   }

   @Override
   public int sizeInBytes() {
      return 10;
   }

   @Override
   public byte getId() {
      return 2;
   }

   @Override
   public TagType<ShortTag> getType() {
      return TYPE;
   }

   public ShortTag copy() {
      return this;
   }

   @Override
   public void accept(final TagVisitor visitor) {
      visitor.visitShort(this);
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
      return this.value;
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
      visitor.visitShort(this);
      return visitor.build();
   }

   private static class Cache {
      private static final int HIGH = 1024;
      private static final int LOW = -128;
      private static final ShortTag[] cache = new ShortTag[1153];

      static {
         for (int i = 0; i < cache.length; i++) {
            cache[i] = new ShortTag((short)(-128 + i));
         }
      }
   }
}
