package com.mojang.blaze3d.buffers;

import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector2ic;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4fc;
import org.joml.Vector4ic;
import org.lwjgl.system.MemoryStack;

public class Std140Builder {
   private final ByteBuffer buffer;
   private final int start;

   private Std140Builder(final ByteBuffer buffer) {
      this.buffer = buffer;
      this.start = buffer.position();
   }

   public static Std140Builder intoBuffer(final ByteBuffer buffer) {
      return new Std140Builder(buffer);
   }

   public static Std140Builder onStack(final MemoryStack stack, final int size) {
      return new Std140Builder(stack.malloc(size));
   }

   public ByteBuffer get() {
      return this.buffer.flip();
   }

   public Std140Builder align(final int alignment) {
      int position = this.buffer.position();
      this.buffer.position(this.start + Mth.roundToward(position - this.start, alignment));
      return this;
   }

   public Std140Builder putFloat(final float value) {
      this.align(4);
      this.buffer.putFloat(value);
      return this;
   }

   public Std140Builder putInt(final int value) {
      this.align(4);
      this.buffer.putInt(value);
      return this;
   }

   public Std140Builder putVec2(final float x, final float y) {
      this.align(8);
      this.buffer.putFloat(x);
      this.buffer.putFloat(y);
      return this;
   }

   public Std140Builder putVec2(final Vector2fc vec) {
      this.align(8);
      vec.get(this.buffer);
      this.buffer.position(this.buffer.position() + 8);
      return this;
   }

   public Std140Builder putIVec2(final int x, final int y) {
      this.align(8);
      this.buffer.putInt(x);
      this.buffer.putInt(y);
      return this;
   }

   public Std140Builder putIVec2(final Vector2ic vec) {
      this.align(8);
      vec.get(this.buffer);
      this.buffer.position(this.buffer.position() + 8);
      return this;
   }

   public Std140Builder putVec3(final float x, final float y, final float z) {
      this.align(16);
      this.buffer.putFloat(x);
      this.buffer.putFloat(y);
      this.buffer.putFloat(z);
      this.buffer.position(this.buffer.position() + 4);
      return this;
   }

   public Std140Builder putVec3(final Vector3fc vec) {
      this.align(16);
      vec.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putIVec3(final int x, final int y, final int z) {
      this.align(16);
      this.buffer.putInt(x);
      this.buffer.putInt(y);
      this.buffer.putInt(z);
      this.buffer.position(this.buffer.position() + 4);
      return this;
   }

   public Std140Builder putIVec3(final Vector3ic vec) {
      this.align(16);
      vec.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putVec4(final float x, final float y, final float z, final float w) {
      this.align(16);
      this.buffer.putFloat(x);
      this.buffer.putFloat(y);
      this.buffer.putFloat(z);
      this.buffer.putFloat(w);
      return this;
   }

   public Std140Builder putVec4(final Vector4fc vec) {
      this.align(16);
      vec.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putIVec4(final int x, final int y, final int z, final int w) {
      this.align(16);
      this.buffer.putInt(x);
      this.buffer.putInt(y);
      this.buffer.putInt(z);
      this.buffer.putInt(w);
      return this;
   }

   public Std140Builder putIVec4(final Vector4ic vec) {
      this.align(16);
      vec.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putMat4f(final Matrix4fc vec) {
      this.align(16);
      vec.get(this.buffer);
      this.buffer.position(this.buffer.position() + 64);
      return this;
   }
}
