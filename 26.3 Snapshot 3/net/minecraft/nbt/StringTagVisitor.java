package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class StringTagVisitor implements TagVisitor {
   private static final Pattern UNQUOTED_KEY_MATCH = Pattern.compile("[A-Za-z._]+[A-Za-z0-9._+-]*");
   private final StringBuilder builder = new StringBuilder();

   public String build() {
      return this.builder.toString();
   }

   @Override
   public void visitString(final StringTag tag) {
      this.builder.append(StringTag.quoteAndEscape(tag.value()));
   }

   @Override
   public void visitByte(final ByteTag tag) {
      this.builder.append(tag.value()).append('b');
   }

   @Override
   public void visitShort(final ShortTag tag) {
      this.builder.append(tag.value()).append('s');
   }

   @Override
   public void visitInt(final IntTag tag) {
      this.builder.append(tag.value());
   }

   @Override
   public void visitLong(final LongTag tag) {
      this.builder.append(tag.value()).append('L');
   }

   @Override
   public void visitFloat(final FloatTag tag) {
      this.builder.append(tag.value()).append('f');
   }

   @Override
   public void visitDouble(final DoubleTag tag) {
      this.builder.append(tag.value()).append('d');
   }

   @Override
   public void visitByteArray(final ByteArrayTag tag) {
      this.builder.append("[B;");
      byte[] data = tag.getAsByteArray();

      for (int i = 0; i < data.length; i++) {
         if (i != 0) {
            this.builder.append(',');
         }

         this.builder.append(data[i]).append('B');
      }

      this.builder.append(']');
   }

   @Override
   public void visitIntArray(final IntArrayTag tag) {
      this.builder.append("[I;");
      int[] data = tag.getAsIntArray();

      for (int i = 0; i < data.length; i++) {
         if (i != 0) {
            this.builder.append(',');
         }

         this.builder.append(data[i]);
      }

      this.builder.append(']');
   }

   @Override
   public void visitLongArray(final LongArrayTag tag) {
      this.builder.append("[L;");
      long[] data = tag.getAsLongArray();

      for (int i = 0; i < data.length; i++) {
         if (i != 0) {
            this.builder.append(',');
         }

         this.builder.append(data[i]).append('L');
      }

      this.builder.append(']');
   }

   @Override
   public void visitList(final ListTag tag) {
      this.builder.append('[');

      for (int i = 0; i < tag.size(); i++) {
         if (i != 0) {
            this.builder.append(',');
         }

         tag.get(i).accept(this);
      }

      this.builder.append(']');
   }

   @Override
   public void visitCompound(final CompoundTag tag) {
      this.builder.append('{');
      List<Entry<String, Tag>> entries = new ArrayList<>(tag.entrySet());
      entries.sort(Entry.comparingByKey());

      for (int i = 0; i < entries.size(); i++) {
         Entry<String, Tag> entry = entries.get(i);
         if (i != 0) {
            this.builder.append(',');
         }

         this.handleKeyEscape(entry.getKey());
         this.builder.append(':');
         entry.getValue().accept(this);
      }

      this.builder.append('}');
   }

   private void handleKeyEscape(final String input) {
      if (!input.equalsIgnoreCase("true") && !input.equalsIgnoreCase("false") && UNQUOTED_KEY_MATCH.matcher(input).matches()) {
         this.builder.append(input);
      } else {
         StringTag.quoteAndEscape(input, this.builder);
      }
   }

   @Override
   public void visitEnd(final EndTag tag) {
      this.builder.append("END");
   }
}
