package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;
import org.apache.commons.lang3.mutable.MutableObject;

public class CopyCustomDataFunction extends LootItemConditionalFunction {
   public static final MapCodec<CopyCustomDataFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               NbtProviders.CODEC.fieldOf("source").forGetter(f -> f.source),
               CopyCustomDataFunction.CopyOperation.CODEC.listOf().fieldOf("ops").forGetter(f -> f.operations)
            )
         )
         .apply(i, CopyCustomDataFunction::new)
   );
   private final NbtProvider source;
   private final List<CopyCustomDataFunction.CopyOperation> operations;

   private CopyCustomDataFunction(
      final List<LootItemCondition> predicates, final NbtProvider source, final List<CopyCustomDataFunction.CopyOperation> operations
   ) {
      super(predicates);
      this.source = source;
      this.operations = List.copyOf(operations);
   }

   @Override
   public MapCodec<CopyCustomDataFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      Validatable.validate(context, "source", this.source);
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      Tag sourceTag = this.source.get(context);
      if (sourceTag == null) {
         return itemStack;
      }

      MutableObject<CompoundTag> result = new MutableObject();
      Supplier<Tag> lazyTargetCopy = () -> {
         if (result.get() == null) {
            result.setValue(itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
         }

         return (Tag)result.get();
      };
      this.operations.forEach(op -> op.apply(lazyTargetCopy, sourceTag));
      CompoundTag resultTag = (CompoundTag)result.get();
      if (resultTag != null) {
         CustomData.set(DataComponents.CUSTOM_DATA, itemStack, resultTag);
      }

      return itemStack;
   }

   @Deprecated
   public static CopyCustomDataFunction.Builder copyData(final NbtProvider source) {
      return new CopyCustomDataFunction.Builder(source);
   }

   public static CopyCustomDataFunction.Builder copyData(final LootContext.EntityTarget source) {
      return new CopyCustomDataFunction.Builder(ContextNbtProvider.forContextEntity(source));
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyCustomDataFunction.Builder> {
      private final NbtProvider source;
      private final List<CopyCustomDataFunction.CopyOperation> ops = Lists.newArrayList();

      private Builder(final NbtProvider source) {
         this.source = source;
      }

      public CopyCustomDataFunction.Builder copy(final String sourcePath, final String targetPath, final CopyCustomDataFunction.MergeStrategy mergeStrategy) {
         try {
            this.ops
               .add(new CopyCustomDataFunction.CopyOperation(NbtPathArgument.NbtPath.of(sourcePath), NbtPathArgument.NbtPath.of(targetPath), mergeStrategy));
            return this;
         } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException(e);
         }
      }

      public CopyCustomDataFunction.Builder copy(final String sourcePath, final String targetPath) {
         return this.copy(sourcePath, targetPath, CopyCustomDataFunction.MergeStrategy.REPLACE);
      }

      protected CopyCustomDataFunction.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new CopyCustomDataFunction(this.getConditions(), this.source, this.ops);
      }
   }

   private record CopyOperation(NbtPathArgument.NbtPath sourcePath, NbtPathArgument.NbtPath targetPath, CopyCustomDataFunction.MergeStrategy op) {
      public static final Codec<CopyCustomDataFunction.CopyOperation> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               NbtPathArgument.NbtPath.CODEC.fieldOf("source").forGetter(CopyCustomDataFunction.CopyOperation::sourcePath),
               NbtPathArgument.NbtPath.CODEC.fieldOf("target").forGetter(CopyCustomDataFunction.CopyOperation::targetPath),
               CopyCustomDataFunction.MergeStrategy.CODEC.fieldOf("op").forGetter(CopyCustomDataFunction.CopyOperation::op)
            )
            .apply(i, CopyCustomDataFunction.CopyOperation::new)
      );

      public void apply(final Supplier<Tag> target, final Tag source) {
         try {
            List<Tag> sourceTags = this.sourcePath.get(source);
            if (!sourceTags.isEmpty()) {
               this.op.merge(target.get(), this.targetPath, sourceTags);
            }
         } catch (CommandSyntaxException var4) {
         }
      }
   }

   public enum MergeStrategy implements StringRepresentable {
      REPLACE("replace") {
         @Override
         public void merge(final Tag target, final NbtPathArgument.NbtPath path, final List<Tag> sources) throws CommandSyntaxException {
            path.set(target, (Tag)Iterables.getLast(sources));
         }
      },
      APPEND("append") {
         @Override
         public void merge(final Tag target, final NbtPathArgument.NbtPath path, final List<Tag> sources) throws CommandSyntaxException {
            List<Tag> targets = path.getOrCreate(target, ListTag::new);
            targets.forEach(tag -> {
               if (tag instanceof ListTag listTag) {
                  sources.forEach(source -> listTag.add(source.copy()));
               }
            });
         }
      },
      MERGE("merge") {
         @Override
         public void merge(final Tag target, final NbtPathArgument.NbtPath path, final List<Tag> sources) throws CommandSyntaxException {
            List<Tag> targets = path.getOrCreate(target, CompoundTag::new);
            targets.forEach(tag -> {
               if (tag instanceof CompoundTag compoundTag) {
                  sources.forEach(source -> {
                     if (source instanceof CompoundTag sourceTag) {
                        compoundTag.merge(sourceTag);
                     }
                  });
               }
            });
         }
      };

      public static final Codec<CopyCustomDataFunction.MergeStrategy> CODEC = StringRepresentable.fromEnum(CopyCustomDataFunction.MergeStrategy::values);
      private final String name;

      public abstract void merge(final Tag target, final NbtPathArgument.NbtPath path, List<Tag> sources) throws CommandSyntaxException;

      MergeStrategy(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
