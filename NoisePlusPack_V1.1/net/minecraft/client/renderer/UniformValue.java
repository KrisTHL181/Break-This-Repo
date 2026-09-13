package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4fc;

@OnlyIn(Dist.CLIENT)
public interface UniformValue {
   Codec<UniformValue> CODEC = UniformValue.Type.CODEC.dispatch(UniformValue::type, p_409803_ -> p_409803_.valueCodec);

   void writeTo(Std140Builder var1);

   void addSize(Std140SizeCalculator var1);

   UniformValue.Type type();

   @OnlyIn(Dist.CLIENT)
   record FloatUniform(float value) implements UniformValue {
      public static final Codec<UniformValue.FloatUniform> CODEC = Codec.FLOAT.xmap(UniformValue.FloatUniform::new, UniformValue.FloatUniform::value);

      @Override
      public void writeTo(Std140Builder p_407522_) {
         p_407522_.putFloat(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator p_410682_) {
         p_410682_.putFloat();
      }

      @Override
      public UniformValue.Type type() {
         return UniformValue.Type.FLOAT;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record IVec3Uniform(Vector3ic value) implements UniformValue {
      public static final Codec<UniformValue.IVec3Uniform> CODEC = ExtraCodecs.VECTOR3I.xmap(UniformValue.IVec3Uniform::new, UniformValue.IVec3Uniform::value);

      @Override
      public void writeTo(Std140Builder p_406891_) {
         p_406891_.putIVec3(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator p_410736_) {
         p_410736_.putIVec3();
      }

      @Override
      public UniformValue.Type type() {
         return UniformValue.Type.IVEC3;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record IntUniform(int value) implements UniformValue {
      public static final Codec<UniformValue.IntUniform> CODEC = Codec.INT.xmap(UniformValue.IntUniform::new, UniformValue.IntUniform::value);

      @Override
      public void writeTo(Std140Builder p_408342_) {
         p_408342_.putInt(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator p_408586_) {
         p_408586_.putInt();
      }

      @Override
      public UniformValue.Type type() {
         return UniformValue.Type.INT;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Matrix4x4Uniform(Matrix4fc value) implements UniformValue {
      public static final Codec<UniformValue.Matrix4x4Uniform> CODEC = ExtraCodecs.MATRIX4F
         .xmap(UniformValue.Matrix4x4Uniform::new, UniformValue.Matrix4x4Uniform::value);

      @Override
      public void writeTo(Std140Builder p_407839_) {
         p_407839_.putMat4f(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator p_407050_) {
         p_407050_.putMat4f();
      }

      @Override
      public UniformValue.Type type() {
         return UniformValue.Type.MATRIX4X4;
      }
   }

   @OnlyIn(Dist.CLIENT)
   enum Type implements StringRepresentable {
      INT("int", UniformValue.IntUniform.CODEC),
      IVEC3("ivec3", UniformValue.IVec3Uniform.CODEC),
      FLOAT("float", UniformValue.FloatUniform.CODEC),
      VEC2("vec2", UniformValue.Vec2Uniform.CODEC),
      VEC3("vec3", UniformValue.Vec3Uniform.CODEC),
      VEC4("vec4", UniformValue.Vec4Uniform.CODEC),
      MATRIX4X4("matrix4x4", UniformValue.Matrix4x4Uniform.CODEC);

      public static final StringRepresentable.EnumCodec<UniformValue.Type> CODEC = StringRepresentable.fromEnum(UniformValue.Type::values);
      private final String name;
      final MapCodec<? extends UniformValue> valueCodec;

      Type(final String p_409873_, final Codec<? extends UniformValue> p_409467_) {
         this.name = p_409873_;
         this.valueCodec = p_409467_.fieldOf("value");
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Vec2Uniform(Vector2fc value) implements UniformValue {
      public static final Codec<UniformValue.Vec2Uniform> CODEC = ExtraCodecs.VECTOR2F.xmap(UniformValue.Vec2Uniform::new, UniformValue.Vec2Uniform::value);

      @Override
      public void writeTo(Std140Builder p_407033_) {
         p_407033_.putVec2(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator p_408851_) {
         p_408851_.putVec2();
      }

      @Override
      public UniformValue.Type type() {
         return UniformValue.Type.VEC2;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Vec3Uniform(Vector3fc value) implements UniformValue {
      public static final Codec<UniformValue.Vec3Uniform> CODEC = ExtraCodecs.VECTOR3F.xmap(UniformValue.Vec3Uniform::new, UniformValue.Vec3Uniform::value);

      @Override
      public void writeTo(Std140Builder p_408247_) {
         p_408247_.putVec3(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator p_410678_) {
         p_410678_.putVec3();
      }

      @Override
      public UniformValue.Type type() {
         return UniformValue.Type.VEC3;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Vec4Uniform(Vector4fc value) implements UniformValue {
      public static final Codec<UniformValue.Vec4Uniform> CODEC = ExtraCodecs.VECTOR4F.xmap(UniformValue.Vec4Uniform::new, UniformValue.Vec4Uniform::value);

      @Override
      public void writeTo(Std140Builder p_406912_) {
         p_406912_.putVec4(this.value);
      }

      @Override
      public void addSize(Std140SizeCalculator p_408593_) {
         p_408593_.putVec4();
      }

      @Override
      public UniformValue.Type type() {
         return UniformValue.Type.VEC4;
      }
   }
}
