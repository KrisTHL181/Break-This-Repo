package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public record StringTag(String value) implements PrimitiveTag {
   private static final int SELF_SIZE_IN_BYTES = 36;
   public static final TagType<StringTag> TYPE = new TagType.VariableSize<StringTag>() {
      public StringTag load(DataInput p_129315_, NbtAccounter p_129317_) throws IOException {
         return StringTag.valueOf(readAccounted(p_129315_, p_129317_));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197570_, StreamTagVisitor p_197571_, NbtAccounter p_301725_) throws IOException {
         return p_197571_.visit(readAccounted(p_197570_, p_301725_));
      }

      private static String readAccounted(DataInput p_301750_, NbtAccounter p_301732_) throws IOException {
         p_301732_.accountBytes(36L);
         String s = p_301750_.readUTF();
         p_301732_.accountBytes(2L, s.length());
         return s;
      }

      @Override
      public void skip(DataInput p_197568_, NbtAccounter p_301752_) throws IOException {
         StringTag.skipString(p_197568_);
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

   public static void skipString(DataInput p_197564_) throws IOException {
      p_197564_.skipBytes(p_197564_.readUnsignedShort());
   }

   public static StringTag valueOf(String p_129298_) {
      return p_129298_.isEmpty() ? EMPTY : new StringTag(p_129298_);
   }

   @Override
   public void write(DataOutput p_129296_) throws IOException {
      p_129296_.writeUTF(this.value);
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
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitString(this);
      return stringtagvisitor.build();
   }

   public StringTag copy() {
      return this;
   }

   @Override
   public Optional<String> asString() {
      return Optional.of(this.value);
   }

   @Override
   public void accept(TagVisitor p_178154_) {
      p_178154_.visitString(this);
   }

   public static String quoteAndEscape(String p_129304_) {
      StringBuilder stringbuilder = new StringBuilder();
      quoteAndEscape(p_129304_, stringbuilder);
      return stringbuilder.toString();
   }

   public static void quoteAndEscape(String p_395036_, StringBuilder p_397484_) {
      int i = p_397484_.length();
      p_397484_.append(' ');
      char c0 = 0;

      for (int j = 0; j < p_395036_.length(); j++) {
         char c1 = p_395036_.charAt(j);
         if (c1 == '\\') {
            p_397484_.append("\\\\");
         } else if (c1 != '"' && c1 != '\'') {
            String s = SnbtGrammar.escapeControlCharacters(c1);
            if (s != null) {
               p_397484_.append('\\');
               p_397484_.append(s);
            } else {
               p_397484_.append(c1);
            }
         } else {
            if (c0 == 0) {
               c0 = (char)(c1 == '"' ? 39 : 34);
            }

            if (c0 == c1) {
               p_397484_.append('\\');
            }

            p_397484_.append(c1);
         }
      }

      if (c0 == 0) {
         c0 = '"';
      }

      p_397484_.setCharAt(i, c0);
      p_397484_.append(c0);
   }

   public static String escapeWithoutQuotes(String p_409290_) {
      StringBuilder stringbuilder = new StringBuilder();
      escapeWithoutQuotes(p_409290_, stringbuilder);
      return stringbuilder.toString();
   }

   public static void escapeWithoutQuotes(String p_409263_, StringBuilder p_408506_) {
      for (int i = 0; i < p_409263_.length(); i++) {
         char c0 = p_409263_.charAt(i);
         switch (c0) {
            case '"':
            case '\'':
            case '\\':
               p_408506_.append('\\');
               p_408506_.append(c0);
               break;
            default:
               String s = SnbtGrammar.escapeControlCharacters(c0);
               if (s != null) {
                  p_408506_.append('\\');
                  p_408506_.append(s);
               } else {
                  p_408506_.append(c0);
               }
         }
      }
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197566_) {
      return p_197566_.visit(this.value);
   }
}
