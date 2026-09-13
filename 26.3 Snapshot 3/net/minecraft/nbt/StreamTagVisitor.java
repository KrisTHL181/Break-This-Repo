package net.minecraft.nbt;

public interface StreamTagVisitor {
   StreamTagVisitor.ValueResult visitEnd();

   StreamTagVisitor.ValueResult visit(final String value);

   StreamTagVisitor.ValueResult visit(final byte value);

   StreamTagVisitor.ValueResult visit(final short value);

   StreamTagVisitor.ValueResult visit(final int value);

   StreamTagVisitor.ValueResult visit(final long value);

   StreamTagVisitor.ValueResult visit(final float value);

   StreamTagVisitor.ValueResult visit(final double value);

   StreamTagVisitor.ValueResult visit(final byte[] value);

   StreamTagVisitor.ValueResult visit(final int[] value);

   StreamTagVisitor.ValueResult visit(final long[] value);

   StreamTagVisitor.ValueResult visitList(final TagType<?> elementType, final int size);

   StreamTagVisitor.EntryResult visitEntry(final TagType<?> type);

   StreamTagVisitor.EntryResult visitEntry(final TagType<?> type, final String id);

   StreamTagVisitor.EntryResult visitElement(final TagType<?> type, final int index);

   StreamTagVisitor.ValueResult visitContainerEnd();

   StreamTagVisitor.ValueResult visitRootEntry(final TagType<?> type);

   enum EntryResult {
      ENTER,
      SKIP,
      BREAK,
      HALT;
   }

   enum ValueResult {
      CONTINUE,
      BREAK,
      HALT;
   }
}
