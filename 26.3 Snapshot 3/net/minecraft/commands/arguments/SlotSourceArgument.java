package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.item.slot.RangeSlotSource;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.item.slot.SlotSources;

public class SlotSourceArgument implements ArgumentType<SlotSourceArgument.Result> {
   private static final Collection<String> EXAMPLES = Util.join(ResourceOrIdArgument.EXAMPLES, SlotsArgument.EXAMPLES);
   private final ArgumentType<Holder<SlotSource>> holderArgument;

   private SlotSourceArgument(final CommandBuildContext context) {
      this.holderArgument = new ResourceOrIdArgument<>(context, Registries.SLOT_SOURCE, SlotSources.DIRECT_CODEC);
   }

   public static SlotSourceArgument slotSource(final CommandBuildContext context) {
      return new SlotSourceArgument(context);
   }

   public static SlotSourceArgument.Result getSlotSource(final CommandContext<CommandSourceStack> context, final String name) {
      return (SlotSourceArgument.Result)context.getArgument(name, SlotSourceArgument.Result.class);
   }

   public SlotSourceArgument.Result parse(final StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();
      SlotRange slotRange = SlotRanges.tryRead(reader);
      if (slotRange != null) {
         return new SlotSourceArgument.LiteralResult(slotRange);
      }

      reader.setCursor(start);
      return new SlotSourceArgument.HolderResult((Holder<SlotSource>)this.holderArgument.parse(reader));
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> contextBuilder, final SuggestionsBuilder builder) {
      SuggestionsBuilder sub = builder.restart();
      SharedSuggestionProvider.suggest(SlotRanges.allNames(), sub);
      builder.add(sub);
      return this.holderArgument.listSuggestions(contextBuilder, builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public record HolderResult(Holder<SlotSource> holder) implements SlotSourceArgument.Result {
      @Override
      public SlotSource value() {
         return this.holder.value();
      }

      @Override
      public Optional<String> name() {
         return this.holder.getRegisteredNameIfPresent();
      }
   }

   public record LiteralResult(SlotRange slotRange) implements SlotSourceArgument.Result {
      @Override
      public SlotSource value() {
         return RangeSlotSource.slotRange(this.slotRange);
      }

      @Override
      public Optional<String> name() {
         return Optional.of(this.slotRange.getSerializedName());
      }
   }

   public sealed interface Result permits SlotSourceArgument.HolderResult, SlotSourceArgument.LiteralResult {
      SlotSource value();

      Optional<String> name();
   }
}
