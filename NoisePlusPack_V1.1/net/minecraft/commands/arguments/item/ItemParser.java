package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableObject;

public class ItemParser {
   static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType(
      p_308407_ -> Component.translatableEscape("argument.item.id.invalid", p_308407_)
   );
   static final DynamicCommandExceptionType ERROR_UNKNOWN_COMPONENT = new DynamicCommandExceptionType(
      p_308406_ -> Component.translatableEscape("arguments.item.component.unknown", p_308406_)
   );
   static final Dynamic2CommandExceptionType ERROR_MALFORMED_COMPONENT = new Dynamic2CommandExceptionType(
      (p_325613_, p_325614_) -> Component.translatableEscape("arguments.item.component.malformed", p_325613_, p_325614_)
   );
   static final SimpleCommandExceptionType ERROR_EXPECTED_COMPONENT = new SimpleCommandExceptionType(
      Component.translatable("arguments.item.component.expected")
   );
   static final DynamicCommandExceptionType ERROR_REPEATED_COMPONENT = new DynamicCommandExceptionType(
      p_325615_ -> Component.translatableEscape("arguments.item.component.repeated", p_325615_)
   );
   private static final DynamicCommandExceptionType ERROR_MALFORMED_ITEM = new DynamicCommandExceptionType(
      p_325616_ -> Component.translatableEscape("arguments.item.malformed", p_325616_)
   );
   public static final char SYNTAX_START_COMPONENTS = '[';
   public static final char SYNTAX_END_COMPONENTS = ']';
   public static final char SYNTAX_COMPONENT_SEPARATOR = ',';
   public static final char SYNTAX_COMPONENT_ASSIGNMENT = '=';
   public static final char SYNTAX_REMOVED_COMPONENT = '!';
   static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
   final HolderLookup.RegistryLookup<Item> items;
   final RegistryOps<Tag> registryOps;
   final TagParser<Tag> tagParser;

   public ItemParser(HolderLookup.Provider p_332470_) {
      this.items = p_332470_.lookupOrThrow(Registries.ITEM);
      this.registryOps = p_332470_.createSerializationContext(NbtOps.INSTANCE);
      this.tagParser = TagParser.create(this.registryOps);
   }

   public ItemParser.ItemResult parse(StringReader p_329942_) throws CommandSyntaxException {
      final MutableObject<Holder<Item>> mutableobject = new MutableObject();
      final DataComponentPatch.Builder datacomponentpatch$builder = DataComponentPatch.builder();
      this.parse(p_329942_, new ItemParser.Visitor() {
         @Override
         public void visitItem(Holder<Item> p_328041_) {
            mutableobject.setValue(p_328041_);
         }

         @Override
         public <T> void visitComponent(DataComponentType<T> p_331133_, T p_330958_) {
            datacomponentpatch$builder.set(p_331133_, p_330958_);
         }

         @Override
         public <T> void visitRemovedComponent(DataComponentType<T> p_342833_) {
            datacomponentpatch$builder.remove(p_342833_);
         }
      });
      Holder<Item> holder = Objects.requireNonNull((Holder<Item>)mutableobject.get(), "Parser gave no item");
      DataComponentPatch datacomponentpatch = datacomponentpatch$builder.build();
      validateComponents(p_329942_, holder, datacomponentpatch);
      return new ItemParser.ItemResult(holder, datacomponentpatch);
   }

   private static void validateComponents(StringReader p_331709_, Holder<Item> p_331328_, DataComponentPatch p_344694_) throws CommandSyntaxException {
      DataComponentMap datacomponentmap = PatchedDataComponentMap.fromPatch(p_331328_.value().components(), p_344694_);
      DataResult<Unit> dataresult = ItemStack.validateComponents(datacomponentmap);
      dataresult.getOrThrow(p_325612_ -> ERROR_MALFORMED_ITEM.createWithContext(p_331709_, p_325612_));
   }

   public void parse(StringReader p_328566_, ItemParser.Visitor p_331669_) throws CommandSyntaxException {
      int i = p_328566_.getCursor();

      try {
         new ItemParser.State(p_328566_, p_331669_).parse();
      } catch (CommandSyntaxException commandsyntaxexception) {
         p_328566_.setCursor(i);
         throw commandsyntaxexception;
      }
   }

   public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder p_235310_) {
      StringReader stringreader = new StringReader(p_235310_.getInput());
      stringreader.setCursor(p_235310_.getStart());
      ItemParser.SuggestionsVisitor itemparser$suggestionsvisitor = new ItemParser.SuggestionsVisitor();
      ItemParser.State itemparser$state = new ItemParser.State(stringreader, itemparser$suggestionsvisitor);

      try {
         itemparser$state.parse();
      } catch (CommandSyntaxException var6) {
      }

      return itemparser$suggestionsvisitor.resolveSuggestions(p_235310_, stringreader);
   }

   public record ItemResult(Holder<Item> item, DataComponentPatch components) {
   }

   class State {
      private final StringReader reader;
      private final ItemParser.Visitor visitor;

      State(final StringReader p_334622_, final ItemParser.Visitor p_332237_) {
         this.reader = p_334622_;
         this.visitor = p_332237_;
      }

      public void parse() throws CommandSyntaxException {
         this.visitor.visitSuggestions(this::suggestItem);
         this.readItem();
         this.visitor.visitSuggestions(this::suggestStartComponents);
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
            this.readComponents();
         }
      }

      private void readItem() throws CommandSyntaxException {
         int i = this.reader.getCursor();
         Identifier identifier = Identifier.read(this.reader);
         this.visitor.visitItem(ItemParser.this.items.get(ResourceKey.create(Registries.ITEM, identifier)).orElseThrow(() -> {
            this.reader.setCursor(i);
            return ItemParser.ERROR_UNKNOWN_ITEM.createWithContext(this.reader, identifier);
         }));
      }

      private void readComponents() throws CommandSyntaxException {
         this.reader.expect('[');
         this.visitor.visitSuggestions(this::suggestComponentAssignmentOrRemoval);
         Set<DataComponentType<?>> set = new ReferenceArraySet();

         while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == '!') {
               this.reader.skip();
               this.visitor.visitSuggestions(this::suggestComponent);
               DataComponentType<?> datacomponenttype1 = readComponentType(this.reader);
               if (!set.add(datacomponenttype1)) {
                  throw ItemParser.ERROR_REPEATED_COMPONENT.create(datacomponenttype1);
               }

               this.visitor.visitRemovedComponent(datacomponenttype1);
               this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
               this.reader.skipWhitespace();
            } else {
               DataComponentType<?> datacomponenttype = readComponentType(this.reader);
               if (!set.add(datacomponenttype)) {
                  throw ItemParser.ERROR_REPEATED_COMPONENT.create(datacomponenttype);
               }

               this.visitor.visitSuggestions(this::suggestAssignment);
               this.reader.skipWhitespace();
               this.reader.expect('=');
               this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
               this.reader.skipWhitespace();
               this.readComponent(ItemParser.this.tagParser, ItemParser.this.registryOps, datacomponenttype);
               this.reader.skipWhitespace();
            }

            this.visitor.visitSuggestions(this::suggestNextOrEndComponents);
            if (!this.reader.canRead() || this.reader.peek() != ',') {
               break;
            }

            this.reader.skip();
            this.reader.skipWhitespace();
            this.visitor.visitSuggestions(this::suggestComponentAssignmentOrRemoval);
            if (!this.reader.canRead()) {
               throw ItemParser.ERROR_EXPECTED_COMPONENT.createWithContext(this.reader);
            }
         }

         this.reader.expect(']');
         this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
      }

      public static DataComponentType<?> readComponentType(StringReader p_330692_) throws CommandSyntaxException {
         if (!p_330692_.canRead()) {
            throw ItemParser.ERROR_EXPECTED_COMPONENT.createWithContext(p_330692_);
         }

         int i = p_330692_.getCursor();
         Identifier identifier = Identifier.read(p_330692_);
         DataComponentType<?> datacomponenttype = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(identifier);
         if (datacomponenttype != null && !datacomponenttype.isTransient()) {
            return datacomponenttype;
         }

         p_330692_.setCursor(i);
         throw ItemParser.ERROR_UNKNOWN_COMPONENT.createWithContext(p_330692_, identifier);
      }

      private <T, O> void readComponent(TagParser<O> p_397960_, RegistryOps<O> p_394302_, DataComponentType<T> p_330643_) throws CommandSyntaxException {
         int i = this.reader.getCursor();
         O o = p_397960_.parseAsArgument(this.reader);
         DataResult<T> dataresult = p_330643_.codecOrThrow().parse(p_394302_, o);
         this.visitor.visitComponent(p_330643_, (T)dataresult.getOrThrow(p_335662_ -> {
            this.reader.setCursor(i);
            return ItemParser.ERROR_MALFORMED_COMPONENT.createWithContext(this.reader, p_330643_.toString(), p_335662_);
         }));
      }

      private CompletableFuture<Suggestions> suggestStartComponents(SuggestionsBuilder p_333169_) {
         if (p_333169_.getRemaining().isEmpty()) {
            p_333169_.suggest(String.valueOf('['));
         }

         return p_333169_.buildFuture();
      }

      private CompletableFuture<Suggestions> suggestNextOrEndComponents(SuggestionsBuilder p_335586_) {
         if (p_335586_.getRemaining().isEmpty()) {
            p_335586_.suggest(String.valueOf(','));
            p_335586_.suggest(String.valueOf(']'));
         }

         return p_335586_.buildFuture();
      }

      private CompletableFuture<Suggestions> suggestAssignment(SuggestionsBuilder p_335223_) {
         if (p_335223_.getRemaining().isEmpty()) {
            p_335223_.suggest(String.valueOf('='));
         }

         return p_335223_.buildFuture();
      }

      private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder p_329594_) {
         return SharedSuggestionProvider.suggestResource(ItemParser.this.items.listElementIds().map(ResourceKey::identifier), p_329594_);
      }

      private CompletableFuture<Suggestions> suggestComponentAssignmentOrRemoval(SuggestionsBuilder p_343738_) {
         p_343738_.suggest(String.valueOf('!'));
         return this.suggestComponent(p_343738_, String.valueOf('='));
      }

      private CompletableFuture<Suggestions> suggestComponent(SuggestionsBuilder p_342558_) {
         return this.suggestComponent(p_342558_, "");
      }

      private CompletableFuture<Suggestions> suggestComponent(SuggestionsBuilder p_345392_, String p_345411_) {
         String s = p_345392_.getRemaining().toLowerCase(Locale.ROOT);
         SharedSuggestionProvider.filterResources(
            BuiltInRegistries.DATA_COMPONENT_TYPE.entrySet(), s, p_448518_ -> p_448518_.getKey().identifier(), p_448521_ -> {
               DataComponentType<?> datacomponenttype = p_448521_.getValue();
               if (datacomponenttype.codec() != null) {
                  Identifier identifier = p_448521_.getKey().identifier();
                  p_345392_.suggest(identifier + p_345411_);
               }
            }
         );
         return p_345392_.buildFuture();
      }
   }

   static class SuggestionsVisitor implements ItemParser.Visitor {
      private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = ItemParser.SUGGEST_NOTHING;

      @Override
      public void visitSuggestions(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> p_328999_) {
         this.suggestions = p_328999_;
      }

      public CompletableFuture<Suggestions> resolveSuggestions(SuggestionsBuilder p_335628_, StringReader p_329757_) {
         return this.suggestions.apply(p_335628_.createOffset(p_329757_.getCursor()));
      }
   }

   public interface Visitor {
      default void visitItem(Holder<Item> p_333631_) {
      }

      default <T> void visitComponent(DataComponentType<T> p_331805_, T p_331331_) {
      }

      default <T> void visitRemovedComponent(DataComponentType<T> p_343605_) {
      }

      default void visitSuggestions(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> p_330945_) {
      }
   }
}
