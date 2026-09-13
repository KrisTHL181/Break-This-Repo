package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import org.jspecify.annotations.Nullable;

public class TagValueOutput implements ValueOutput {
   private final ProblemReporter problemReporter;
   private final DynamicOps<Tag> ops;
   private final CompoundTag output;

   TagValueOutput(ProblemReporter p_407091_, DynamicOps<Tag> p_407083_, CompoundTag p_409593_) {
      this.problemReporter = p_407091_;
      this.ops = p_407083_;
      this.output = p_409593_;
   }

   public static TagValueOutput createWithContext(ProblemReporter p_409887_, HolderLookup.Provider p_409738_) {
      return new TagValueOutput(p_409887_, p_409738_.createSerializationContext(NbtOps.INSTANCE), new CompoundTag());
   }

   public static TagValueOutput createWithoutContext(ProblemReporter p_405875_) {
      return new TagValueOutput(p_405875_, NbtOps.INSTANCE, new CompoundTag());
   }

   @Override
   public <T> void store(String p_410452_, Codec<T> p_410260_, T p_406127_) {
      switch (p_410260_.encodeStart(this.ops, p_406127_)) {
         case Success<Tag> success:
            this.output.put(p_410452_, (Tag)success.value());
            break;
         case Error<Tag> error:
            this.problemReporter.report(new TagValueOutput.EncodeToFieldFailedProblem(p_410452_, p_406127_, error));
            error.partialValue().ifPresent(p_408267_ -> this.output.put(p_410452_, p_408267_));
            break;
         default:
            throw new MatchException(null, null);
      }
   }

   @Override
   public <T> void storeNullable(String p_406228_, Codec<T> p_409087_, @Nullable T p_409771_) {
      if (p_409771_ != null) {
         this.store(p_406228_, p_409087_, p_409771_);
      }
   }

   @Override
   public <T> void store(MapCodec<T> p_410256_, T p_409723_) {
      switch (p_410256_.encoder().encodeStart(this.ops, p_409723_)) {
         case Success<Tag> success:
            this.output.merge((CompoundTag)success.value());
            break;
         case Error<Tag> error:
            this.problemReporter.report(new TagValueOutput.EncodeToMapFailedProblem(p_409723_, error));
            error.partialValue().ifPresent(p_409806_ -> this.output.merge((CompoundTag)p_409806_));
            break;
         default:
            throw new MatchException(null, null);
      }
   }

   @Override
   public void putBoolean(String p_409958_, boolean p_407625_) {
      this.output.putBoolean(p_409958_, p_407625_);
   }

   @Override
   public void putByte(String p_410202_, byte p_408016_) {
      this.output.putByte(p_410202_, p_408016_);
   }

   @Override
   public void putShort(String p_407617_, short p_409280_) {
      this.output.putShort(p_407617_, p_409280_);
   }

   @Override
   public void putInt(String p_407860_, int p_408971_) {
      this.output.putInt(p_407860_, p_408971_);
   }

   @Override
   public void putLong(String p_406263_, long p_409384_) {
      this.output.putLong(p_406263_, p_409384_);
   }

   @Override
   public void putFloat(String p_408522_, float p_409966_) {
      this.output.putFloat(p_408522_, p_409966_);
   }

   @Override
   public void putDouble(String p_408079_, double p_410601_) {
      this.output.putDouble(p_408079_, p_410601_);
   }

   @Override
   public void putString(String p_407363_, String p_407217_) {
      this.output.putString(p_407363_, p_407217_);
   }

   @Override
   public void putIntArray(String p_408263_, int[] p_409875_) {
      this.output.putIntArray(p_408263_, p_409875_);
   }

   private ProblemReporter reporterForChild(String p_406167_) {
      return this.problemReporter.forChild(new ProblemReporter.FieldPathElement(p_406167_));
   }

   @Override
   public ValueOutput child(String p_408121_) {
      CompoundTag compoundtag = new CompoundTag();
      this.output.put(p_408121_, compoundtag);
      return new TagValueOutput(this.reporterForChild(p_408121_), this.ops, compoundtag);
   }

   @Override
   public ValueOutput.ValueOutputList childrenList(String p_406376_) {
      ListTag listtag = new ListTag();
      this.output.put(p_406376_, listtag);
      return new TagValueOutput.ListWrapper(p_406376_, this.problemReporter, this.ops, listtag);
   }

   @Override
   public <T> ValueOutput.TypedOutputList<T> list(String p_408848_, Codec<T> p_406807_) {
      ListTag listtag = new ListTag();
      this.output.put(p_408848_, listtag);
      return new TagValueOutput.TypedListWrapper<>(this.problemReporter, p_408848_, this.ops, p_406807_, listtag);
   }

   @Override
   public void discard(String p_409717_) {
      this.output.remove(p_409717_);
   }

   @Override
   public boolean isEmpty() {
      return this.output.isEmpty();
   }

   public CompoundTag buildResult() {
      return this.output;
   }

   public record EncodeToFieldFailedProblem(String name, Object value, Error<?> error) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Failed to encode value '" + this.value + "' to field '" + this.name + "': " + this.error.message();
      }
   }

   public record EncodeToListFailedProblem(String name, Object value, Error<?> error) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Failed to append value '" + this.value + "' to list '" + this.name + "': " + this.error.message();
      }
   }

   public record EncodeToMapFailedProblem(Object value, Error<?> error) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Failed to merge value '" + this.value + "' to an object: " + this.error.message();
      }
   }

   static class ListWrapper implements ValueOutput.ValueOutputList {
      private final String fieldName;
      private final ProblemReporter problemReporter;
      private final DynamicOps<Tag> ops;
      private final ListTag output;

      ListWrapper(String p_410679_, ProblemReporter p_407412_, DynamicOps<Tag> p_410022_, ListTag p_406938_) {
         this.fieldName = p_410679_;
         this.problemReporter = p_407412_;
         this.ops = p_410022_;
         this.output = p_406938_;
      }

      @Override
      public ValueOutput addChild() {
         int i = this.output.size();
         CompoundTag compoundtag = new CompoundTag();
         this.output.add(compoundtag);
         return new TagValueOutput(this.problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(this.fieldName, i)), this.ops, compoundtag);
      }

      @Override
      public void discardLast() {
         this.output.removeLast();
      }

      @Override
      public boolean isEmpty() {
         return this.output.isEmpty();
      }
   }

   static class TypedListWrapper<T> implements ValueOutput.TypedOutputList<T> {
      private final ProblemReporter problemReporter;
      private final String name;
      private final DynamicOps<Tag> ops;
      private final Codec<T> codec;
      private final ListTag output;

      TypedListWrapper(ProblemReporter p_407252_, String p_409336_, DynamicOps<Tag> p_409770_, Codec<T> p_407275_, ListTag p_409925_) {
         this.problemReporter = p_407252_;
         this.name = p_409336_;
         this.ops = p_409770_;
         this.codec = p_407275_;
         this.output = p_409925_;
      }

      @Override
      public void add(T p_406423_) {
         switch (this.codec.encodeStart(this.ops, p_406423_)) {
            case Success<Tag> success:
               this.output.add((Tag)success.value());
               break;
            case Error<Tag> error:
               this.problemReporter.report(new TagValueOutput.EncodeToListFailedProblem(this.name, p_406423_, error));
               error.partialValue().ifPresent(this.output::add);
               break;
            default:
               throw new MatchException(null, null);
         }
      }

      @Override
      public boolean isEmpty() {
         return this.output.isEmpty();
      }
   }
}
