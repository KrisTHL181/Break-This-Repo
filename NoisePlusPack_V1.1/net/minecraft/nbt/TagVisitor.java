package net.minecraft.nbt;

public interface TagVisitor {
   void visitString(StringTag var1);

   void visitByte(ByteTag var1);

   void visitShort(ShortTag var1);

   void visitInt(IntTag var1);

   void visitLong(LongTag var1);

   void visitFloat(FloatTag var1);

   void visitDouble(DoubleTag var1);

   void visitByteArray(ByteArrayTag var1);

   void visitIntArray(IntArrayTag var1);

   void visitLongArray(LongArrayTag var1);

   void visitList(ListTag var1);

   void visitCompound(CompoundTag var1);

   void visitEnd(EndTag var1);
}
