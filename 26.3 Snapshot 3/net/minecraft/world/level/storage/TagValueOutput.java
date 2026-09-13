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

   private TagValueOutput(final ProblemReporter problemReporter, final DynamicOps<Tag> ops, final CompoundTag output) {
      this.problemReporter = problemReporter;
      this.ops = ops;
      this.output = output;
   }

   public static TagValueOutput createWithContext(final ProblemReporter problemReporter, final HolderLookup.Provider provider) {
      return new TagValueOutput(problemReporter, provider.createSerializationContext(NbtOps.INSTANCE), new CompoundTag());
   }

   public static TagValueOutput createWithoutContext(final ProblemReporter problemReporter) {
      return new TagValueOutput(problemReporter, NbtOps.INSTANCE, new CompoundTag());
   }

   @Override
   public <T> void store(final String name, final Codec<T> codec, final T value) {
      switch (codec.encodeStart(this.ops, value)) {
         case Success<Tag> success:
            this.output.put(name, (Tag)success.value());
            break;
         case Error<Tag> error:
            this.problemReporter.report(new TagValueOutput.EncodeToFieldFailedProblem(name, value, error));
            error.partialValue().ifPresent(partial -> this.output.put(name, partial));
            break;
         default:
            throw new MatchException(null, null);
      }
   }

   @Override
   public <T> void storeNullable(final String name, final Codec<T> codec, final @Nullable T value) {
      if (value != null) {
         this.store(name, codec, value);
      }
   }

   @Override
   public <T> void store(final MapCodec<T> codec, final T value) {
      switch (codec.encoder().encodeStart(this.ops, value)) {
         case Success<Tag> success:
            this.output.merge((CompoundTag)success.value());
            break;
         case Error<Tag> error:
            this.problemReporter.report(new TagValueOutput.EncodeToMapFailedProblem(value, error));
            error.partialValue().ifPresent(partial -> this.output.merge((CompoundTag)partial));
            break;
         default:
            throw new MatchException(null, null);
      }
   }

   @Override
   public void putBoolean(final String name, final boolean value) {
      this.output.putBoolean(name, value);
   }

   @Override
   public void putByte(final String name, final byte value) {
      this.output.putByte(name, value);
   }

   @Override
   public void putShort(final String name, final short value) {
      this.output.putShort(name, value);
   }

   @Override
   public void putInt(final String name, final int value) {
      this.output.putInt(name, value);
   }

   @Override
   public void putLong(final String name, final long value) {
      this.output.putLong(name, value);
   }

   @Override
   public void putFloat(final String name, final float value) {
      this.output.putFloat(name, value);
   }

   @Override
   public void putDouble(final String name, final double value) {
      this.output.putDouble(name, value);
   }

   @Override
   public void putString(final String name, final String value) {
      this.output.putString(name, value);
   }

   @Override
   public void putIntArray(final String name, final int[] value) {
      this.output.putIntArray(name, value);
   }

   private ProblemReporter reporterForChild(final String name) {
      return this.problemReporter.forChild(new ProblemReporter.FieldPathElement(name));
   }

   @Override
   public ValueOutput child(final String name) {
      CompoundTag childTag = new CompoundTag();
      this.output.put(name, childTag);
      return new TagValueOutput(this.reporterForChild(name), this.ops, childTag);
   }

   @Override
   public ValueOutput.ValueOutputList childrenList(final String name) {
      ListTag childList = new ListTag();
      this.output.put(name, childList);
      return new TagValueOutput.ListWrapper(name, this.problemReporter, this.ops, childList);
   }

   @Override
   public <T> ValueOutput.TypedOutputList<T> list(final String name, final Codec<T> codec) {
      ListTag childList = new ListTag();
      this.output.put(name, childList);
      return new TagValueOutput.TypedListWrapper<>(this.problemReporter, name, this.ops, codec, childList);
   }

   @Override
   public void discard(final String name) {
      this.output.remove(name);
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

   private static class ListWrapper implements ValueOutput.ValueOutputList {
      private final String fieldName;
      private final ProblemReporter problemReporter;
      private final DynamicOps<Tag> ops;
      private final ListTag output;

      private ListWrapper(final String fieldName, final ProblemReporter problemReporter, final DynamicOps<Tag> ops, final ListTag output) {
         this.fieldName = fieldName;
         this.problemReporter = problemReporter;
         this.ops = ops;
         this.output = output;
      }

      @Override
      public ValueOutput addChild() {
         int newChildIndex = this.output.size();
         CompoundTag child = new CompoundTag();
         this.output.add(child);
         return new TagValueOutput(this.problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(this.fieldName, newChildIndex)), this.ops, child);
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

   private static class TypedListWrapper<T> implements ValueOutput.TypedOutputList<T> {
      private final ProblemReporter problemReporter;
      private final String name;
      private final DynamicOps<Tag> ops;
      private final Codec<T> codec;
      private final ListTag output;

      private TypedListWrapper(final ProblemReporter problemReporter, final String name, final DynamicOps<Tag> ops, final Codec<T> codec, final ListTag output) {
         this.problemReporter = problemReporter;
         this.name = name;
         this.ops = ops;
         this.codec = codec;
         this.output = output;
      }

      @Override
      public void add(final T value) {
         switch (this.codec.encodeStart(this.ops, value)) {
            case Success<Tag> success:
               this.output.add((Tag)success.value());
               break;
            case Error<Tag> error:
               this.problemReporter.report(new TagValueOutput.EncodeToListFailedProblem(this.name, value, error));
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
