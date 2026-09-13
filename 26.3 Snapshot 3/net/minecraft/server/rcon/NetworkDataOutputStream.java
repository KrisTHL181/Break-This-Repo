package net.minecraft.server.rcon;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NetworkDataOutputStream {
   private final ByteArrayOutputStream outputStream;
   private final DataOutputStream dataOutputStream;

   public NetworkDataOutputStream(final int size) {
      this.outputStream = new ByteArrayOutputStream(size);
      this.dataOutputStream = new DataOutputStream(this.outputStream);
   }

   public void writeBytes(final byte[] data) throws IOException {
      this.dataOutputStream.write(data, 0, data.length);
   }

   public void writeString(final String data) throws IOException {
      this.dataOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
      this.dataOutputStream.write(0);
   }

   public void write(final int data) throws IOException {
      this.dataOutputStream.write(data);
   }

   public void writeShort(final short data) throws IOException {
      this.dataOutputStream.writeShort(Short.reverseBytes(data));
   }

   public void writeInt(final int data) throws IOException {
      this.dataOutputStream.writeInt(Integer.reverseBytes(data));
   }

   public void writeFloat(final float data) throws IOException {
      this.dataOutputStream.writeInt(Integer.reverseBytes(Float.floatToIntBits(data)));
   }

   public byte[] toByteArray() {
      return this.outputStream.toByteArray();
   }

   public void reset() {
      this.outputStream.reset();
   }
}
