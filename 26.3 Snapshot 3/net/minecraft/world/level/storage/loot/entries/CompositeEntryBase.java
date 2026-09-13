package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class CompositeEntryBase extends LootPoolEntryContainer {
   public static final ProblemReporter.Problem NO_CHILDREN_PROBLEM = new ProblemReporter.Problem() {
      @Override
      public String description() {
         return "Empty children list";
      }
   };
   protected final List<LootPoolEntryContainer> children;
   private final ComposableEntryContainer composedChildren;

   protected CompositeEntryBase(final List<LootPoolEntryContainer> children, final List<LootItemCondition> conditions) {
      super(conditions);
      this.children = children;
      this.composedChildren = this.compose(children);
   }

   @Override
   public abstract MapCodec<? extends CompositeEntryBase> codec();

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      if (this.children.isEmpty()) {
         context.reportProblem(NO_CHILDREN_PROBLEM);
      }

      Validatable.validate(context, "children", this.children);
   }

   protected abstract ComposableEntryContainer compose(List<? extends ComposableEntryContainer> entries);

   @Override
   public final boolean expand(final LootContext context, final Consumer<LootPoolEntry> output) {
      return !this.canRun(context) ? false : this.composedChildren.expand(context, output);
   }

   public static <T extends CompositeEntryBase> MapCodec<T> createCodec(final CompositeEntryBase.CompositeEntryConstructor<T> constructor) {
      return RecordCodecBuilder.mapCodec(
         i -> i.group(LootPoolEntries.CODEC.listOf().optionalFieldOf("children", List.of()).forGetter(e -> e.children))
            .and(commonFields(i).t1())
            .apply(i, constructor::create)
      );
   }

   @FunctionalInterface
   public interface CompositeEntryConstructor<T extends CompositeEntryBase> {
      T create(List<LootPoolEntryContainer> children, List<LootItemCondition> conditions);
   }
}
