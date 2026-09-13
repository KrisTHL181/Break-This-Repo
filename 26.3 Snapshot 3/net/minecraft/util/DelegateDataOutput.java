package net.minecraft.util;

import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.SuppressForbidden;

public class DelegateDataOutput implements DataOutput {
   private final DataOutput parent;

   public DelegateDataOutput(final DataOutput parent) {
      this.parent = parent;
   }

   @Override
   public void write(final int b) throws IOException {
      this.parent.write(b);
   }

   @Override
   public void write(final byte[] b) throws IOException {
      this.parent.write(b);
   }

   @Override
   public void write(final byte[] b, final int off, final int len) throws IOException {
      this.parent.write(b, off, len);
   }

   @Override
   public void writeBoolean(final boolean v) throws IOException {
      this.parent.writeBoolean(v);
   }

   @Override
   public void writeByte(final int v) throws IOException {
      this.parent.writeByte(v);
   }

   @Override
   public void writeShort(final int v) throws IOException {
      this.parent.writeShort(v);
   }

   @Override
   public void writeChar(final int v) throws IOException {
      this.parent.writeChar(v);
   }

   @Override
   public void writeInt(final int v) throws IOException {
      this.parent.writeInt(v);
   }

   @Override
   public void writeLong(final long v) throws IOException {
      this.parent.writeLong(v);
   }

   @Override
   public void writeFloat(final float v) throws IOException {
      this.parent.writeFloat(v);
   }

   @Override
   public void writeDouble(final double v) throws IOException {
      this.parent.writeDouble(v);
   }

   @SuppressForbidden(reason = "Delegation is not use")
   @Override
   public void writeBytes(final String s) throws IOException {
      this.parent.writeBytes(s);
   }

   @Override
   public void writeChars(final String s) throws IOException {
      this.parent.writeChars(s);
   }

   @Override
   public void writeUTF(final String s) throws IOException {
      this.parent.writeUTF(s);
   }
}
