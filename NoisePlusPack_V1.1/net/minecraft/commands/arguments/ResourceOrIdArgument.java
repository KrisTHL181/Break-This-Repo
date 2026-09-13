package net.minecraft.commands.arguments;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.IdentifierParseRule;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;

public class ResourceOrIdArgument<T> implements ArgumentType<Holder<T>> {
   private static final Collection<String> EXAMPLES = List.of("foo", "foo:bar", "012", "{}", "true");
   public static final DynamicCommandExceptionType ERROR_FAILED_TO_PARSE = new DynamicCommandExceptionType(
      p_334248_ -> Component.translatableEscape("argument.resource_or_id.failed_to_parse", p_334248_)
   );
   public static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ELEMENT = new Dynamic2CommandExceptionType(
      (p_405039_, p_405040_) -> Component.translatableEscape("argument.resource_or_id.no_such_element", p_405039_, p_405040_)
   );
   public static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
   private final HolderLookup.Provider registryLookup;
   private final Optional<? extends HolderLookup.RegistryLookup<T>> elementLookup;
   private final Codec<T> codec;
   private final Grammar<ResourceOrIdArgument.Result<T, Tag>> grammar;
   private final ResourceKey<? extends Registry<T>> registryKey;

   protected ResourceOrIdArgument(CommandBuildContext p_334973_, ResourceKey<? extends Registry<T>> p_336087_, Codec<T> p_332112_) {
      this.registryLookup = p_334973_;
      this.elementLookup = p_334973_.lookup(p_336087_);
      this.registryKey = p_336087_;
      this.codec = p_332112_;
      this.grammar = createGrammar(p_336087_, OPS);
   }

   public static <T, O> Grammar<ResourceOrIdArgument.Result<T, O>> createGrammar(ResourceKey<? extends Registry<T>> p_406976_, DynamicOps<O> p_407708_) {
      Grammar<O> grammar = SnbtGrammar.createParser((DynamicOps<T>)p_407708_);
      Dictionary<StringReader> dictionary = new Dictionary<>();
      Atom<ResourceOrIdArgument.Result<T, O>> atom = Atom.of("result");
      Atom<Identifier> atom1 = Atom.of("id");
      Atom<O> atom2 = Atom.of("value");
      dictionary.put(atom1, IdentifierParseRule.INSTANCE);
      dictionary.put(atom2, grammar.top().value());
      NamedRule<StringReader, ResourceOrIdArgument.Result<T, O>> namedrule = dictionary.put(
         atom, Term.alternative(dictionary.named(atom1), dictionary.named(atom2)), p_448491_ -> {
            Identifier identifier = p_448491_.get(atom1);
            if (identifier != null) {
               return new ResourceOrIdArgument.ReferenceResult<>(ResourceKey.create(p_406976_, identifier));
            }

            O o = p_448491_.getOrThrow(atom2);
            return new ResourceOrIdArgument.InlineResult<>(o);
         }
      );
      return new Grammar<>(dictionary, namedrule);
   }

   public static ResourceOrIdArgument.LootTableArgument lootTable(CommandBuildContext p_329328_) {
      return new ResourceOrIdArgument.LootTableArgument(p_329328_);
   }

   public static Holder<LootTable> getLootTable(CommandContext<CommandSourceStack> p_335148_, String p_329251_) throws CommandSyntaxException {
      return getResource(p_335148_, p_329251_);
   }

   public static ResourceOrIdArgument.LootModifierArgument lootModifier(CommandBuildContext p_329720_) {
      return new ResourceOrIdArgument.LootModifierArgument(p_329720_);
   }

   public static Holder<LootItemFunction> getLootModifier(CommandContext<CommandSourceStack> p_334458_, String p_330525_) {
      return getResource(p_334458_, p_330525_);
   }

   public static ResourceOrIdArgument.LootPredicateArgument lootPredicate(CommandBuildContext p_330159_) {
      return new ResourceOrIdArgument.LootPredicateArgument(p_330159_);
   }

   public static Holder<LootItemCondition> getLootPredicate(CommandContext<CommandSourceStack> p_335366_, String p_334649_) {
      return getResource(p_335366_, p_334649_);
   }

   public static ResourceOrIdArgument.DialogArgument dialog(CommandBuildContext p_409305_) {
      return new ResourceOrIdArgument.DialogArgument(p_409305_);
   }

   public static Holder<Dialog> getDialog(CommandContext<CommandSourceStack> p_407444_, String p_407109_) {
      return getResource(p_407444_, p_407109_);
   }

   private static <T> Holder<T> getResource(CommandContext<CommandSourceStack> p_328476_, String p_329877_) {
      return (Holder<T>)p_328476_.getArgument(p_329877_, Holder.class);
   }

   public @Nullable Holder<T> parse(StringReader p_330381_) throws CommandSyntaxException {
      return this.parse(p_330381_, this.grammar, OPS);
   }

   private <O> @Nullable Holder<T> parse(StringReader p_397396_, Grammar<ResourceOrIdArgument.Result<T, O>> p_406421_, DynamicOps<O> p_406083_) throws CommandSyntaxException {
      ResourceOrIdArgument.Result<T, O> result = p_406421_.parseForCommands(p_397396_);
      return this.elementLookup.isEmpty()
         ? null
         : result.parse(p_397396_, this.registryLookup, p_406083_, this.codec, (HolderLookup.RegistryLookup<T>)this.elementLookup.get());
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_406356_, SuggestionsBuilder p_406812_) {
      return SharedSuggestionProvider.listSuggestions(p_406356_, p_406812_, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class DialogArgument extends ResourceOrIdArgument<Dialog> {
      protected DialogArgument(CommandBuildContext p_406394_) {
         super(p_406394_, Registries.DIALOG, Dialog.DIRECT_CODEC);
      }
   }

   public record InlineResult<T, O>(O value) implements ResourceOrIdArgument.Result<T, O> {
      @Override
      public Holder<T> parse(
         ImmutableStringReader p_409546_,
         HolderLookup.Provider p_410228_,
         DynamicOps<O> p_410382_,
         Codec<T> p_408251_,
         HolderLookup.RegistryLookup<T> p_406267_
      ) throws CommandSyntaxException {
         return Holder.direct(
            (T)p_408251_.parse(p_410228_.createSerializationContext(p_410382_), this.value)
               .getOrThrow(p_408685_ -> ResourceOrIdArgument.ERROR_FAILED_TO_PARSE.createWithContext(p_409546_, p_408685_))
         );
      }
   }

   public static class LootModifierArgument extends ResourceOrIdArgument<LootItemFunction> {
      protected LootModifierArgument(CommandBuildContext p_333515_) {
         super(p_333515_, Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC);
      }
   }

   public static class LootPredicateArgument extends ResourceOrIdArgument<LootItemCondition> {
      protected LootPredicateArgument(CommandBuildContext p_334679_) {
         super(p_334679_, Registries.PREDICATE, LootItemCondition.DIRECT_CODEC);
      }
   }

   public static class LootTableArgument extends ResourceOrIdArgument<LootTable> {
      protected LootTableArgument(CommandBuildContext p_332797_) {
         super(p_332797_, Registries.LOOT_TABLE, LootTable.DIRECT_CODEC);
      }
   }

   public record ReferenceResult<T, O>(ResourceKey<T> key) implements ResourceOrIdArgument.Result<T, O> {
      @Override
      public Holder<T> parse(
         ImmutableStringReader p_410510_,
         HolderLookup.Provider p_406672_,
         DynamicOps<O> p_409410_,
         Codec<T> p_410626_,
         HolderLookup.RegistryLookup<T> p_406658_
      ) throws CommandSyntaxException {
         return p_406658_.get(this.key)
            .orElseThrow(() -> ResourceOrIdArgument.ERROR_NO_SUCH_ELEMENT.createWithContext(p_410510_, this.key.identifier(), this.key.registry()));
      }
   }

   public sealed interface Result<T, O> permits ResourceOrIdArgument.InlineResult, ResourceOrIdArgument.ReferenceResult {
      Holder<T> parse(ImmutableStringReader var1, HolderLookup.Provider var2, DynamicOps<O> var3, Codec<T> var4, HolderLookup.RegistryLookup<T> var5) throws CommandSyntaxException;
   }
}
