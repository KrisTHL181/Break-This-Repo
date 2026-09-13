package net.minecraft.commands.arguments.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.IdentifierParseRule;
import net.minecraft.util.parsing.packrat.commands.ResourceLookupRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.TagParseRule;

public class ComponentPredicateParser {
   public static <T, C, P> Grammar<List<T>> createGrammar(final ComponentPredicateParser.Context<T, C, P> context) {
      Atom<List<T>> top = Atom.of("top");
      Atom<Optional<T>> type = Atom.of("type");
      Atom<Unit> anyType = Atom.of("any_type");
      Atom<T> elementType = Atom.of("element_type");
      Atom<T> tagType = Atom.of("tag_type");
      Atom<List<T>> conditions = Atom.of("conditions");
      Atom<List<T>> alternatives = Atom.of("alternatives");
      Atom<T> term = Atom.of("term");
      Atom<T> negation = Atom.of("negation");
      Atom<T> test = Atom.of("test");
      Atom<C> componentType = Atom.of("component_type");
      Atom<P> predicateType = Atom.of("predicate_type");
      Atom<Identifier> id = Atom.of("id");
      Atom<Dynamic<?>> tag = Atom.of("tag");
      Dictionary<StringReader> rules = new Dictionary<>();
      NamedRule<StringReader, Identifier> idRule = rules.put(id, IdentifierParseRule.INSTANCE);
      NamedRule<StringReader, List<T>> topRule = rules.put(
         top,
         Term.alternative(
            Term.sequence(
               rules.named(type), StringReaderTerms.character('['), Term.cut(), Term.optional(rules.named(conditions)), StringReaderTerms.character(']')
            ),
            rules.named(type)
         ),
         scope -> {
            Builder<T> builder = ImmutableList.builder();
            scope.getOrThrow(type).ifPresent(builder::add);
            List<T> parsedConditions = scope.get(conditions);
            if (parsedConditions != null) {
               builder.addAll(parsedConditions);
            }

            return builder.build();
         }
      );
      rules.put(
         type,
         Term.alternative(rules.named(elementType), Term.sequence(StringReaderTerms.character('#'), Term.cut(), rules.named(tagType)), rules.named(anyType)),
         scope -> Optional.ofNullable(scope.getAny(elementType, tagType))
      );
      rules.put(anyType, StringReaderTerms.character('*'), s -> Unit.INSTANCE);
      rules.put(elementType, new ComponentPredicateParser.ElementLookupRule<>(idRule, context));
      rules.put(tagType, new ComponentPredicateParser.TagLookupRule<>(idRule, context));
      rules.put(
         conditions,
         Term.sequence(rules.named(alternatives), Term.optional(Term.sequence(StringReaderTerms.character(','), rules.named(conditions)))),
         scope -> {
            T parsedCondition = context.anyOf(scope.getOrThrow(alternatives));
            return Optional.ofNullable(scope.get(conditions)).map(rest -> Util.copyAndAdd(parsedCondition, (List<T>)rest)).orElse(List.of(parsedCondition));
         }
      );
      rules.put(
         alternatives, Term.sequence(rules.named(term), Term.optional(Term.sequence(StringReaderTerms.character('|'), rules.named(alternatives)))), scope -> {
            T alternative = scope.getOrThrow(term);
            return Optional.ofNullable(scope.get(alternatives)).map(rest -> Util.copyAndAdd(alternative, (List<T>)rest)).orElse(List.of(alternative));
         }
      );
      rules.put(
         term,
         Term.alternative(rules.named(test), Term.sequence(StringReaderTerms.character('!'), rules.named(negation))),
         scope -> scope.getAnyOrThrow(test, negation)
      );
      rules.put(negation, rules.named(test), scope -> context.negate(scope.getOrThrow(test)));
      rules.putComplex(
         test,
         Term.alternative(
            Term.sequence(rules.named(componentType), StringReaderTerms.character('='), Term.cut(), rules.named(tag)),
            Term.sequence(rules.named(predicateType), StringReaderTerms.character('~'), Term.cut(), rules.named(tag)),
            rules.named(componentType)
         ),
         state -> {
            Scope scope = state.scope();
            P predicate = scope.get(predicateType);

            try {
               if (predicate != null) {
                  Dynamic<?> value = scope.getOrThrow(tag);
                  return context.createPredicateTest((ImmutableStringReader)state.input(), predicate, value);
               } else {
                  C component = scope.getOrThrow(componentType);
                  Dynamic<?> value = scope.get(tag);
                  return value != null
                     ? context.createComponentTest((ImmutableStringReader)state.input(), component, value)
                     : context.createComponentTest((ImmutableStringReader)state.input(), component);
               }
            } catch (CommandSyntaxException e) {
               state.errorCollector().store(state.mark(), e);
               return null;
            }
         }
      );
      rules.put(componentType, new ComponentPredicateParser.ComponentLookupRule<>(idRule, context));
      rules.put(predicateType, new ComponentPredicateParser.PredicateLookupRule<>(idRule, context));
      rules.put(tag, new TagParseRule(NbtOps.INSTANCE));
      return new Grammar<>(rules, topRule);
   }

   private static class ComponentLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, C> {
      private ComponentLookupRule(final NamedRule<StringReader, Identifier> idParser, final ComponentPredicateParser.Context<T, C, P> context) {
         super(idParser, context);
      }

      @Override
      protected C validateElement(final ImmutableStringReader reader, final Identifier id) throws Exception {
         return this.context.lookupComponentType(reader, id);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listComponentTypes();
      }
   }

   public interface Context<T, C, P> {
      T forElementType(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

      Stream<Identifier> listElementTypes();

      T forTagType(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

      Stream<Identifier> listTagTypes();

      C lookupComponentType(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

      Stream<Identifier> listComponentTypes();

      T createComponentTest(ImmutableStringReader reader, C componentType, Dynamic<?> value) throws CommandSyntaxException;

      T createComponentTest(ImmutableStringReader reader, C componentType);

      P lookupPredicateType(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

      Stream<Identifier> listPredicateTypes();

      T createPredicateTest(ImmutableStringReader reader, P predicateType, Dynamic<?> value) throws CommandSyntaxException;

      T negate(T value);

      T anyOf(List<T> alternatives);
   }

   private static class ElementLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      private ElementLookupRule(final NamedRule<StringReader, Identifier> idParser, final ComponentPredicateParser.Context<T, C, P> context) {
         super(idParser, context);
      }

      @Override
      protected T validateElement(final ImmutableStringReader reader, final Identifier id) throws Exception {
         return this.context.forElementType(reader, id);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listElementTypes();
      }
   }

   private static class PredicateLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, P> {
      private PredicateLookupRule(final NamedRule<StringReader, Identifier> idParser, final ComponentPredicateParser.Context<T, C, P> context) {
         super(idParser, context);
      }

      @Override
      protected P validateElement(final ImmutableStringReader reader, final Identifier id) throws Exception {
         return this.context.lookupPredicateType(reader, id);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listPredicateTypes();
      }
   }

   private static class TagLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      private TagLookupRule(final NamedRule<StringReader, Identifier> idParser, final ComponentPredicateParser.Context<T, C, P> context) {
         super(idParser, context);
      }

      @Override
      protected T validateElement(final ImmutableStringReader reader, final Identifier id) throws Exception {
         return this.context.forTagType(reader, id);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listTagTypes();
      }
   }
}
