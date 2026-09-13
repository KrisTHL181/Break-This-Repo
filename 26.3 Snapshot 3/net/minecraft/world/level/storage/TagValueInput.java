package net.minecraft.world.level.storage;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.util.ProblemReporter;
import org.jspecify.annotations.Nullable;

public class TagValueInput implements ValueInput {
   private final ProblemReporter problemReporter;
   private final ValueInputContextHelper context;
   private final CompoundTag input;

   private TagValueInput(final ProblemReporter problemReporter, final ValueInputContextHelper context, final CompoundTag input) {
      this.problemReporter = problemReporter;
      this.context = context;
      this.input = input;
   }

   public static ValueInput create(final ProblemReporter problemReporter, final HolderLookup.Provider holders, final CompoundTag tag) {
      return new TagValueInput(problemReporter, new ValueInputContextHelper(holders, NbtOps.INSTANCE), tag);
   }

   public static ValueInput.ValueInputList create(final ProblemReporter problemReporter, final HolderLookup.Provider holders, final List<CompoundTag> tags) {
      return new TagValueInput.CompoundListWrapper(problemReporter, new ValueInputContextHelper(holders, NbtOps.INSTANCE), tags);
   }

   @Override
   public <T> Optional<T> read(final String name, final Codec<T> codec) {
      Tag tag = this.input.get(name);
      if (tag == null) {
         return Optional.empty();
      }

      return switch (codec.parse(this.context.ops(), tag)) {
         case Success<T> success -> Optional.of(success.value());
         case Error<T> error -> {
            this.problemReporter.report(new TagValueInput.DecodeFromFieldFailedProblem(name, tag, error));
            yield error.partialValue();
         }
         default -> throw new MatchException(null, null);
      };
   }

   @Override
   public <T> Optional<T> read(final MapCodec<T> codec) {
      DynamicOps<Tag> ops = this.context.ops();

      return switch (ops.getMap(this.input).flatMap(map -> codec.decode(ops, map))) {
         case Success<T> success -> Optional.of(success.value());
         case Error<T> error -> {
            this.problemReporter.report(new TagValueInput.DecodeFromMapFailedProblem(error));
            yield error.partialValue();
         }
         default -> throw new MatchException(null, null);
      };
   }

   private <T extends Tag> @Nullable T getOptionalTypedTag(final String name, final TagType<T> expectedType) {
      Tag tag = this.input.get(name);
      if (tag == null) {
         return null;
      } else {
         TagType<?> actualType = tag.getType();
         if (actualType != expectedType) {
            this.problemReporter.report(new TagValueInput.UnexpectedTypeProblem(name, expectedType, actualType));
            return null;
         } else {
            return (T)tag;
         }
      }
   }

   private @Nullable NumericTag getNumericTag(final String name) {
      Tag tag = this.input.get(name);
      if (tag == null) {
         return null;
      } else if (tag instanceof NumericTag numericTag) {
         return numericTag;
      } else {
         this.problemReporter.report(new TagValueInput.UnexpectedNonNumberProblem(name, tag.getType()));
         return null;
      }
   }

   @Override
   public Optional<ValueInput> child(final String name) {
      CompoundTag compound = this.getOptionalTypedTag(name, CompoundTag.TYPE);
      return compound != null ? Optional.of(this.wrapChild(name, compound)) : Optional.empty();
   }

   @Override
   public ValueInput childOrEmpty(final String name) {
      CompoundTag compound = this.getOptionalTypedTag(name, CompoundTag.TYPE);
      return compound != null ? this.wrapChild(name, compound) : this.context.empty();
   }

   @Override
   public Optional<ValueInput.ValueInputList> childrenList(final String name) {
      ListTag list = this.getOptionalTypedTag(name, ListTag.TYPE);
      return list != null ? Optional.of(this.wrapList(name, this.context, list)) : Optional.empty();
   }

   @Override
   public ValueInput.ValueInputList childrenListOrEmpty(final String name) {
      ListTag list = this.getOptionalTypedTag(name, ListTag.TYPE);
      return list != null ? this.wrapList(name, this.context, list) : this.context.emptyList();
   }

   @Override
   public <T> Optional<ValueInput.TypedInputList<T>> list(final String name, final Codec<T> codec) {
      ListTag list = this.getOptionalTypedTag(name, ListTag.TYPE);
      return list != null ? Optional.of(this.wrapTypedList(name, list, codec)) : Optional.empty();
   }

   @Override
   public <T> ValueInput.TypedInputList<T> listOrEmpty(final String name, final Codec<T> codec) {
      ListTag list = this.getOptionalTypedTag(name, ListTag.TYPE);
      return list != null ? this.wrapTypedList(name, list, codec) : this.context.emptyTypedList();
   }

   @Override
   public boolean getBooleanOr(final String name, final boolean defaultValue) {
      NumericTag numericTag = this.getNumericTag(name);
      return numericTag != null ? numericTag.byteValue() != 0 : defaultValue;
   }

   @Override
   public byte getByteOr(final String name, final byte defaultValue) {
      NumericTag numericTag = this.getNumericTag(name);
      return numericTag != null ? numericTag.byteValue() : defaultValue;
   }

   @Override
   public int getShortOr(final String name, final short defaultValue) {
      NumericTag numericTag = this.getNumericTag(name);
      return numericTag != null ? numericTag.shortValue() : defaultValue;
   }

   @Override
   public Optional<Integer> getInt(final String name) {
      NumericTag numericTag = this.getNumericTag(name);
      return numericTag != null ? Optional.of(numericTag.intValue()) : Optional.empty();
   }

   @Override
   public int getIntOr(final String name, final int defaultValue) {
      NumericTag numericTag = this.getNumericTag(name);
      return numericTag != null ? numericTag.intValue() : defaultValue;
   }

   @Override
   public long getLongOr(final String name, final long defaultValue) {
      NumericTag numericTag = this.getNumericTag(name);
      return numericTag != null ? numericTag.longValue() : defaultValue;
   }

   @Override
   public Optional<Long> getLong(final String name) {
      NumericTag numericTag = this.getNumericTag(name);
      return numericTag != null ? Optional.of(numericTag.longValue()) : Optional.empty();
   }

   @Override
   public float getFloatOr(final String name, final float defaultValue) {
      NumericTag numericTag = this.getNumericTag(name);
      return numericTag != null ? numericTag.floatValue() : defaultValue;
   }

   @Override
   public double getDoubleOr(final String name, final double defaultValue) {
      NumericTag numericTag = this.getNumericTag(name);
      return numericTag != null ? numericTag.doubleValue() : defaultValue;
   }

   @Override
   public Optional<String> getString(final String name) {
      StringTag tag = this.getOptionalTypedTag(name, StringTag.TYPE);
      return tag != null ? Optional.of(tag.value()) : Optional.empty();
   }

   @Override
   public String getStringOr(final String name, final String defaultValue) {
      StringTag tag = this.getOptionalTypedTag(name, StringTag.TYPE);
      return tag != null ? tag.value() : defaultValue;
   }

   @Override
   public Optional<int[]> getIntArray(final String name) {
      IntArrayTag tag = this.getOptionalTypedTag(name, IntArrayTag.TYPE);
      return tag != null ? Optional.of(tag.getAsIntArray()) : Optional.empty();
   }

   @Override
   public HolderLookup.Provider lookup() {
      return this.context.lookup();
   }

   private ValueInput wrapChild(final String name, final CompoundTag compoundTag) {
      return compoundTag.isEmpty()
         ? this.context.empty()
         : new TagValueInput(this.problemReporter.forChild(new ProblemReporter.FieldPathElement(name)), this.context, compoundTag);
   }

   private static ValueInput wrapChild(final ProblemReporter problemReporter, final ValueInputContextHelper context, final CompoundTag compoundTag) {
      return compoundTag.isEmpty() ? context.empty() : new TagValueInput(problemReporter, context, compoundTag);
   }

   private ValueInput.ValueInputList wrapList(final String name, final ValueInputContextHelper context, final ListTag list) {
      return list.isEmpty() ? context.emptyList() : new TagValueInput.ListWrapper(this.problemReporter, name, context, list);
   }

   private <T> ValueInput.TypedInputList<T> wrapTypedList(final String name, final ListTag list, final Codec<T> codec) {
      return list.isEmpty() ? this.context.emptyTypedList() : new TagValueInput.TypedListWrapper<>(this.problemReporter, name, this.context, codec, list);
   }

   private static class CompoundListWrapper implements ValueInput.ValueInputList {
      private final ProblemReporter problemReporter;
      private final ValueInputContextHelper context;
      private final List<CompoundTag> list;

      public CompoundListWrapper(final ProblemReporter problemReporter, final ValueInputContextHelper context, final List<CompoundTag> list) {
         this.problemReporter = problemReporter;
         this.context = context;
         this.list = list;
      }

      private ValueInput wrapChild(final int index, final CompoundTag compoundTag) {
         return TagValueInput.wrapChild(this.problemReporter.forChild(new ProblemReporter.IndexedPathElement(index)), this.context, compoundTag);
      }

      @Override
      public boolean isEmpty() {
         return this.list.isEmpty();
      }

      @Override
      public Stream<ValueInput> stream() {
         return Streams.mapWithIndex(this.list.stream(), (value, index) -> this.wrapChild((int)index, value));
      }

      @Override
      public Iterator<ValueInput> iterator() {
         final ListIterator<CompoundTag> iterator = this.list.listIterator();
         return new AbstractIterator<ValueInput>() {
            protected @Nullable ValueInput computeNext() {
               if (iterator.hasNext()) {
                  int index = iterator.nextIndex();
                  CompoundTag value = iterator.next();
                  return CompoundListWrapper.this.wrapChild(index, value);
               } else {
                  return (ValueInput)this.endOfData();
               }
            }
         };
      }
   }

   public record DecodeFromFieldFailedProblem(String name, Tag tag, Error<?> error) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Failed to decode value '" + this.tag + "' from field '" + this.name + "': " + this.error.message();
      }
   }

   public record DecodeFromListFailedProblem(String name, int index, Tag tag, Error<?> error) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Failed to decode value '" + this.tag + "' from field '" + this.name + "' at index " + this.index + "': " + this.error.message();
      }
   }

   public record DecodeFromMapFailedProblem(Error<?> error) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Failed to decode from map: " + this.error.message();
      }
   }

   private static class ListWrapper implements ValueInput.ValueInputList {
      private final ProblemReporter problemReporter;
      private final String name;
      private final ValueInputContextHelper context;
      private final ListTag list;

      private ListWrapper(final ProblemReporter problemReporter, final String name, final ValueInputContextHelper context, final ListTag list) {
         this.problemReporter = problemReporter;
         this.name = name;
         this.context = context;
         this.list = list;
      }

      @Override
      public boolean isEmpty() {
         return this.list.isEmpty();
      }

      private ProblemReporter reporterForChild(final int index) {
         return this.problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(this.name, index));
      }

      private void reportIndexUnwrapProblem(final int index, final Tag value) {
         this.problemReporter.report(new TagValueInput.UnexpectedListElementTypeProblem(this.name, index, CompoundTag.TYPE, value.getType()));
      }

      @Override
      public Stream<ValueInput> stream() {
         return Streams.mapWithIndex(this.list.stream(), (value, index) -> {
            if (value instanceof CompoundTag compoundTag) {
               return TagValueInput.wrapChild(this.reporterForChild((int)index), this.context, compoundTag);
            } else {
               this.reportIndexUnwrapProblem((int)index, value);
               return null;
            }
         }).filter(Objects::nonNull);
      }

      @Override
      public Iterator<ValueInput> iterator() {
         final Iterator<Tag> iterator = this.list.iterator();
         return new AbstractIterator<ValueInput>() {
            private int index;

            protected @Nullable ValueInput computeNext() {
               while (iterator.hasNext()) {
                  Tag value = iterator.next();
                  int currentIndex = this.index++;
                  if (value instanceof CompoundTag compoundTag) {
                     return TagValueInput.wrapChild(ListWrapper.this.reporterForChild(currentIndex), ListWrapper.this.context, compoundTag);
                  }

                  ListWrapper.this.reportIndexUnwrapProblem(currentIndex, value);
               }

               return (ValueInput)this.endOfData();
            }
         };
      }
   }

   private static class TypedListWrapper<T> implements ValueInput.TypedInputList<T> {
      private final ProblemReporter problemReporter;
      private final String name;
      private final ValueInputContextHelper context;
      private final Codec<T> codec;
      private final ListTag list;

      private TypedListWrapper(
         final ProblemReporter problemReporter, final String name, final ValueInputContextHelper context, final Codec<T> codec, final ListTag list
      ) {
         this.problemReporter = problemReporter;
         this.name = name;
         this.context = context;
         this.codec = codec;
         this.list = list;
      }

      @Override
      public boolean isEmpty() {
         return this.list.isEmpty();
      }

      private void reportIndexUnwrapProblem(final int index, final Tag value, final Error<?> error) {
         this.problemReporter.report(new TagValueInput.DecodeFromListFailedProblem(this.name, index, value, error));
      }

      @Override
      public Stream<T> stream() {
         return Streams.mapWithIndex(this.list.stream(), (value, index) -> {
            return switch (this.codec.parse(this.context.ops(), value)) {
               case Success<T> success -> success.value();
               case Error<T> error -> {
                  this.reportIndexUnwrapProblem((int)index, value, error);
                  yield error.partialValue().orElse(null);
               }
               default -> throw new MatchException(null, null);
            };
         }).filter(Objects::nonNull);
      }

      @Override
      public Iterator<T> iterator() {
         final ListIterator<Tag> iterator = this.list.listIterator();
         return new AbstractIterator<T>() {
            protected @Nullable T computeNext() {
               while (iterator.hasNext()) {
                  int index = iterator.nextIndex();
                  Tag value = iterator.next();
                  switch (TypedListWrapper.this.codec.parse(TypedListWrapper.this.context.ops(), value)) {
                     case Success<T> success:
                        return (T)success.value();
                     case Error<T> error:
                        TypedListWrapper.this.reportIndexUnwrapProblem(index, value, error);
                        if (!error.partialValue().isPresent()) {
                           break;
                        }

                        return (T)error.partialValue().get();
                     default:
                        throw new MatchException(null, null);
                  }
               }

               return (T)this.endOfData();
            }
         };
      }
   }

   public record UnexpectedListElementTypeProblem(String name, int index, TagType<?> expected, TagType<?> actual) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Expected list '"
            + this.name
            + "' to contain at index "
            + this.index
            + " value of type "
            + this.expected.getName()
            + ", but got "
            + this.actual.getName();
      }
   }

   public record UnexpectedNonNumberProblem(String name, TagType<?> actual) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Expected field '" + this.name + "' to contain number, but got " + this.actual.getName();
      }
   }

   public record UnexpectedTypeProblem(String name, TagType<?> expected, TagType<?> actual) implements ProblemReporter.Problem {
      @Override
      public String description() {
         return "Expected field '" + this.name + "' to contain value of type " + this.expected.getName() + ", but got " + this.actual.getName();
      }
   }
}
