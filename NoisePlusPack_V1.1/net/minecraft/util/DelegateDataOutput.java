package net.minecraft.util;

import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.SuppressForbidden;

public class DelegateDataOutput implements DataOutput {
   private final DataOutput parent;

   public DelegateDataOutput(DataOutput p_311826_) {
      this.parent = p_311826_;
   }

   @Override
   public void write(int p_312870_) throws IOException {
      this.parent.write(p_312870_);
   }

   @Override
   public void write(byte[] p_311646_) throws IOException {
      this.parent.write(p_311646_);
   }

   @Override
   public void write(byte[] p_309909_, int p_313250_, int p_311853_) throws IOException {
      this.parent.write(p_309909_, p_313250_, p_311853_);
   }

   @Override
   public void writeBoolean(boolean p_310495_) throws IOException {
      this.parent.writeBoolean(p_310495_);
   }

   @Override
   public void writeByte(int p_311940_) throws IOException {
      this.parent.writeByte(p_311940_);
   }

   @Override
   public void writeShort(int p_310680_) throws IOException {
      this.parent.writeShort(p_310680_);
   }

   @Override
   public void writeChar(int p_310364_) throws IOException {
      this.parent.writeChar(p_310364_);
   }

   @Override
   public void writeInt(int p_310767_) throws IOException {
      this.parent.writeInt(p_310767_);
   }

   @Override
   public void writeLong(long p_313222_) throws IOException {
      this.parent.writeLong(p_313222_);
   }

   @Override
   public void writeFloat(float p_311489_) throws IOException {
      this.parent.writeFloat(p_311489_);
   }

   @Override
   public void writeDouble(double p_312046_) throws IOException {
      this.parent.writeDouble(p_312046_);
   }

   @SuppressForbidden(reason = "Delegation is not use")
   @Override
   public void writeBytes(String p_310549_) throws IOException {
      this.parent.writeBytes(p_310549_);
   }

   @Override
   public void writeChars(String p_311977_) throws IOException {
      this.parent.writeChars(p_311977_);
   }

   @Override
   public void writeUTF(String p_309650_) throws IOException {
      this.parent.writeUTF(p_309650_);
   }
}
