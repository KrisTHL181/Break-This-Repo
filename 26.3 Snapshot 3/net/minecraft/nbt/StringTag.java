package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public record StringTag(String value) implements PrimitiveTag {
   private static final int SELF_SIZE_IN_BYTES = 36;
   public static final TagType<StringTag> TYPE = new TagType.VariableSize<StringTag>() {
      public StringTag load(final DataInput input, final NbtAccounter accounter) throws IOException {
         return StringTag.valueOf(readAccounted(input, accounter));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
         return output.visit(readAccounted(input, accounter));
      }

      private static String readAccounted(final DataInput input, final NbtAccounter accounter) throws IOException {
         accounter.accountBytes(36L);
         String data = input.readUTF();
         accounter.accountBytes(2L, data.length());
         return data;
      }

      @Override
      public void skip(final DataInput input, final NbtAccounter accounter) throws IOException {
         StringTag.skipString(input);
      }

      @Override
      public String getName() {
         return "STRING";
      }

      @Override
      public String getPrettyName() {
         return "TAG_String";
      }
   };
   private static final StringTag EMPTY = new StringTag("");
   private static final char DOUBLE_QUOTE = '"';
   private static final char SINGLE_QUOTE = '\'';
   private static final char ESCAPE = '\\';
   private static final char NOT_SET = '\u0000';

   @Deprecated(forRemoval = true)
   public StringTag {
   }

   public static void skipString(final DataInput input) throws IOException {
      input.skipBytes(input.readUnsignedShort());
   }

   public static StringTag valueOf(final String data) {
      return data.isEmpty() ? EMPTY : new StringTag(data);
   }

   @Override
   public void write(final DataOutput output) throws IOException {
      output.writeUTF(this.value);
   }

   @Override
   public int sizeInBytes() {
      return 36 + 2 * this.value.length();
   }

   @Override
   public byte getId() {
      return 8;
   }

   @Override
   public TagType<StringTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      StringTagVisitor visitor = new StringTagVisitor();
      visitor.visitString(this);
      return visitor.build();
   }

   public StringTag copy() {
      return this;
   }

   @Override
   public Optional<String> asString() {
      return Optional.of(this.value);
   }

   @Override
   public void accept(final TagVisitor visitor) {
      visitor.visitString(this);
   }

   public static String quoteAndEscape(final String input) {
      StringBuilder result = new StringBuilder();
      quoteAndEscape(input, result);
      return result.toString();
   }

   public static void quoteAndEscape(final String input, final StringBuilder result) {
      int quoteMarkIndex = result.length();
      result.append(' ');
      char quote = 0;

      for (int i = 0; i < input.length(); i++) {
         char c = input.charAt(i);
         if (c == '\\') {
            result.append("\\\\");
         } else if (c != '"' && c != '\'') {
            String escaped = SnbtGrammar.escapeControlCharacters(c);
            if (escaped != null) {
               result.append('\\');
               result.append(escaped);
            } else {
               result.append(c);
            }
         } else {
            if (quote == 0) {
               quote = (char)(c == '"' ? 39 : 34);
            }

            if (quote == c) {
               result.append('\\');
            }

            result.append(c);
         }
      }

      if (quote == 0) {
         quote = '"';
      }

      result.setCharAt(quoteMarkIndex, quote);
      result.append(quote);
   }

   public static String escapeWithoutQuotes(final String input) {
      StringBuilder result = new StringBuilder();
      escapeWithoutQuotes(input, result);
      return result.toString();
   }

   public static void escapeWithoutQuotes(final String input, final StringBuilder result) {
      for (int i = 0; i < input.length(); i++) {
         char c = input.charAt(i);
         switch (c) {
            case '"':
            case '\'':
            case '\\':
               result.append('\\');
               result.append(c);
               break;
            default:
               String escaped = SnbtGrammar.escapeControlCharacters(c);
               if (escaped != null) {
                  result.append('\\');
                  result.append(escaped);
               } else {
                  result.append(c);
               }
         }
      }
   }

   @Override
   public StreamTagVisitor.ValueResult accept(final StreamTagVisitor visitor) {
      return visitor.visit(this.value);
   }
}
