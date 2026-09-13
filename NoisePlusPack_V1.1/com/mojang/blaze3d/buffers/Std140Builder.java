package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector2ic;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4fc;
import org.joml.Vector4ic;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class Std140Builder {
   private final ByteBuffer buffer;
   private final int start;

   private Std140Builder(ByteBuffer p_406773_) {
      this.buffer = p_406773_;
      this.start = p_406773_.position();
   }

   public static Std140Builder intoBuffer(ByteBuffer p_405910_) {
      return new Std140Builder(p_405910_);
   }

   public static Std140Builder onStack(MemoryStack p_406566_, int p_406194_) {
      return new Std140Builder(p_406566_.malloc(p_406194_));
   }

   public ByteBuffer get() {
      return this.buffer.flip();
   }

   public Std140Builder align(int p_406563_) {
      int i = this.buffer.position();
      this.buffer.position(this.start + Mth.roundToward(i - this.start, p_406563_));
      return this;
   }

   public Std140Builder putFloat(float p_406399_) {
      this.align(4);
      this.buffer.putFloat(p_406399_);
      return this;
   }

   public Std140Builder putInt(int p_407089_) {
      this.align(4);
      this.buffer.putInt(p_407089_);
      return this;
   }

   public Std140Builder putVec2(float p_410305_, float p_407963_) {
      this.align(8);
      this.buffer.putFloat(p_410305_);
      this.buffer.putFloat(p_407963_);
      return this;
   }

   public Std140Builder putVec2(Vector2fc p_408659_) {
      this.align(8);
      p_408659_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 8);
      return this;
   }

   public Std140Builder putIVec2(int p_405975_, int p_410747_) {
      this.align(8);
      this.buffer.putInt(p_405975_);
      this.buffer.putInt(p_410747_);
      return this;
   }

   public Std140Builder putIVec2(Vector2ic p_408625_) {
      this.align(8);
      p_408625_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 8);
      return this;
   }

   public Std140Builder putVec3(float p_406926_, float p_407578_, float p_408599_) {
      this.align(16);
      this.buffer.putFloat(p_406926_);
      this.buffer.putFloat(p_407578_);
      this.buffer.putFloat(p_408599_);
      this.buffer.position(this.buffer.position() + 4);
      return this;
   }

   public Std140Builder putVec3(Vector3fc p_409552_) {
      this.align(16);
      p_409552_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putIVec3(int p_406336_, int p_406334_, int p_410026_) {
      this.align(16);
      this.buffer.putInt(p_406336_);
      this.buffer.putInt(p_406334_);
      this.buffer.putInt(p_410026_);
      this.buffer.position(this.buffer.position() + 4);
      return this;
   }

   public Std140Builder putIVec3(Vector3ic p_408980_) {
      this.align(16);
      p_408980_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putVec4(float p_409173_, float p_409110_, float p_409485_, float p_407618_) {
      this.align(16);
      this.buffer.putFloat(p_409173_);
      this.buffer.putFloat(p_409110_);
      this.buffer.putFloat(p_409485_);
      this.buffer.putFloat(p_407618_);
      return this;
   }

   public Std140Builder putVec4(Vector4fc p_408230_) {
      this.align(16);
      p_408230_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putIVec4(int p_410721_, int p_406066_, int p_408681_, int p_409215_) {
      this.align(16);
      this.buffer.putInt(p_410721_);
      this.buffer.putInt(p_406066_);
      this.buffer.putInt(p_408681_);
      this.buffer.putInt(p_409215_);
      return this;
   }

   public Std140Builder putIVec4(Vector4ic p_410711_) {
      this.align(16);
      p_410711_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 16);
      return this;
   }

   public Std140Builder putMat4f(Matrix4fc p_409496_) {
      this.align(16);
      p_409496_.get(this.buffer);
      this.buffer.position(this.buffer.position() + 64);
      return this;
   }
}
