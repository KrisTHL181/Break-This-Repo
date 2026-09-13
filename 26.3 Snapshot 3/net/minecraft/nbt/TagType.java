package net.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;

public interface TagType<T extends Tag> {
   T load(DataInput input, NbtAccounter accounter) throws IOException;

   StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException;

   default void parseRoot(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
      switch (output.visitRootEntry(this)) {
         case CONTINUE:
            this.parse(input, output, accounter);
         case HALT:
         default:
            break;
         case BREAK:
            this.skip(input, accounter);
      }
   }

   void skip(DataInput input, int count, NbtAccounter accounter) throws IOException;

   void skip(DataInput input, NbtAccounter accounter) throws IOException;

   String getName();

   String getPrettyName();

   static TagType<EndTag> createInvalid(final int id) {
      return new TagType<EndTag>() {
         private IOException createException() {
            return new IOException("Invalid tag id: " + id);
         }

         public EndTag load(final DataInput input, final NbtAccounter accounter) throws IOException {
            throw this.createException();
         }

         @Override
         public StreamTagVisitor.ValueResult parse(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
            throw this.createException();
         }

         @Override
         public void skip(final DataInput input, final int count, final NbtAccounter accounter) throws IOException {
            throw this.createException();
         }

         @Override
         public void skip(final DataInput input, final NbtAccounter accounter) throws IOException {
            throw this.createException();
         }

         @Override
         public String getName() {
            return "INVALID[" + id + "]";
         }

         @Override
         public String getPrettyName() {
            return "UNKNOWN_" + id;
         }
      };
   }

   interface StaticSize<T extends Tag> extends TagType<T> {
      @Override
      default void skip(final DataInput input, final NbtAccounter accounter) throws IOException {
         input.skipBytes(this.size());
      }

      @Override
      default void skip(final DataInput input, final int count, final NbtAccounter accounter) throws IOException {
         input.skipBytes(this.size() * count);
      }

      int size();
   }

   interface VariableSize<T extends Tag> extends TagType<T> {
      @Override
      default void skip(final DataInput input, final int count, final NbtAccounter accounter) throws IOException {
         for (int i = 0; i < count; i++) {
            this.skip(input, accounter);
         }
      }
   }
}
