package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Variant(Identifier modelLocation, Variant.SimpleModelState modelState) implements BlockModelPart.Unbaked {
   public static final MapCodec<Variant> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_448207_ -> p_448207_.group(
            Identifier.CODEC.fieldOf("model").forGetter(Variant::modelLocation), Variant.SimpleModelState.MAP_CODEC.forGetter(Variant::modelState)
         )
         .apply(p_448207_, Variant::new)
   );
   public static final Codec<Variant> CODEC = MAP_CODEC.codec();

   public Variant(Identifier p_453797_) {
      this(p_453797_, Variant.SimpleModelState.DEFAULT);
   }

   public Variant withXRot(Quadrant p_397734_) {
      return this.withState(this.modelState.withX(p_397734_));
   }

   public Variant withYRot(Quadrant p_392925_) {
      return this.withState(this.modelState.withY(p_392925_));
   }

   public Variant withZRot(Quadrant p_458956_) {
      return this.withState(this.modelState.withZ(p_458956_));
   }

   public Variant withUvLock(boolean p_394546_) {
      return this.withState(this.modelState.withUvLock(p_394546_));
   }

   public Variant withModel(Identifier p_459116_) {
      return new Variant(p_459116_, this.modelState);
   }

   public Variant withState(Variant.SimpleModelState p_391782_) {
      return new Variant(this.modelLocation, p_391782_);
   }

   public Variant with(VariantMutator p_394270_) {
      return p_394270_.apply(this);
   }

   @Override
   public BlockModelPart bake(ModelBaker p_397047_) {
      return SimpleModelWrapper.bake(p_397047_, this.modelLocation, this.modelState.asModelState());
   }

   @Override
   public void resolveDependencies(ResolvableModel.Resolver p_391294_) {
      p_391294_.markDependency(this.modelLocation);
   }

   @OnlyIn(Dist.CLIENT)
   public record SimpleModelState(Quadrant x, Quadrant y, Quadrant z, boolean uvLock) {
      public static final MapCodec<Variant.SimpleModelState> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448208_ -> p_448208_.group(
               Quadrant.CODEC.optionalFieldOf("x", Quadrant.R0).forGetter(Variant.SimpleModelState::x),
               Quadrant.CODEC.optionalFieldOf("y", Quadrant.R0).forGetter(Variant.SimpleModelState::y),
               Quadrant.CODEC.optionalFieldOf("z", Quadrant.R0).forGetter(Variant.SimpleModelState::z),
               Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(Variant.SimpleModelState::uvLock)
            )
            .apply(p_448208_, Variant.SimpleModelState::new)
      );
      public static final Variant.SimpleModelState DEFAULT = new Variant.SimpleModelState(Quadrant.R0, Quadrant.R0, Quadrant.R0, false);

      public ModelState asModelState() {
         BlockModelRotation blockmodelrotation = BlockModelRotation.get(Quadrant.fromXYZAngles(this.x, this.y, this.z));
         return this.uvLock ? blockmodelrotation.withUvLock() : blockmodelrotation;
      }

      public Variant.SimpleModelState withX(Quadrant p_393583_) {
         return new Variant.SimpleModelState(p_393583_, this.y, this.z, this.uvLock);
      }

      public Variant.SimpleModelState withY(Quadrant p_395288_) {
         return new Variant.SimpleModelState(this.x, p_395288_, this.z, this.uvLock);
      }

      public Variant.SimpleModelState withZ(Quadrant p_450548_) {
         return new Variant.SimpleModelState(this.x, this.y, p_450548_, this.uvLock);
      }

      public Variant.SimpleModelState withUvLock(boolean p_396764_) {
         return new Variant.SimpleModelState(this.x, this.y, this.z, p_396764_);
      }
   }
}
