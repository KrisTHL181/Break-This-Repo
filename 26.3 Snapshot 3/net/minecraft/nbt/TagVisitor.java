package net.minecraft.nbt;

public interface TagVisitor {
   void visitString(StringTag tag);

   void visitByte(ByteTag tag);

   void visitShort(ShortTag tag);

   void visitInt(IntTag tag);

   void visitLong(LongTag tag);

   void visitFloat(FloatTag tag);

   void visitDouble(DoubleTag tag);

   void visitByteArray(ByteArrayTag tag);

   void visitIntArray(IntArrayTag tag);

   void visitLongArray(LongArrayTag tag);

   void visitList(ListTag tag);

   void visitCompound(CompoundTag tag);

   void visitEnd(EndTag tag);
}
