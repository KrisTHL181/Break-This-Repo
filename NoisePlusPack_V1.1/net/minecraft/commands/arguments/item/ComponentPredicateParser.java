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
   public static <T, C, P> Grammar<List<T>> createGrammar(ComponentPredicateParser.Context<T, C, P> p_329972_) {
      Atom<List<T>> atom = Atom.of("top");
      Atom<Optional<T>> atom1 = Atom.of("type");
      Atom<Unit> atom2 = Atom.of("any_type");
      Atom<T> atom3 = Atom.of("element_type");
      Atom<T> atom4 = Atom.of("tag_type");
      Atom<List<T>> atom5 = Atom.of("conditions");
      Atom<List<T>> atom6 = Atom.of("alternatives");
      Atom<T> atom7 = Atom.of("term");
      Atom<T> atom8 = Atom.of("negation");
      Atom<T> atom9 = Atom.of("test");
      Atom<C> atom10 = Atom.of("component_type");
      Atom<P> atom11 = Atom.of("predicate_type");
      Atom<Identifier> atom12 = Atom.of("id");
      Atom<Dynamic<?>> atom13 = Atom.of("tag");
      Dictionary<StringReader> dictionary = new Dictionary<>();
      NamedRule<StringReader, Identifier> namedrule = dictionary.put(atom12, IdentifierParseRule.INSTANCE);
      NamedRule<StringReader, List<T>> namedrule1 = dictionary.put(
         atom,
         Term.alternative(
            Term.sequence(
               dictionary.named(atom1), StringReaderTerms.character('['), Term.cut(), Term.optional(dictionary.named(atom5)), StringReaderTerms.character(']')
            ),
            dictionary.named(atom1)
         ),
         p_331933_ -> {
            Builder<T> builder = ImmutableList.builder();
            p_331933_.getOrThrow(atom1).ifPresent(builder::add);
            List<T> list = p_331933_.get(atom5);
            if (list != null) {
               builder.addAll(list);
            }

            return builder.build();
         }
      );
      dictionary.put(
         atom1,
         Term.alternative(
            dictionary.named(atom3), Term.sequence(StringReaderTerms.character('#'), Term.cut(), dictionary.named(atom4)), dictionary.named(atom2)
         ),
         p_333155_ -> Optional.ofNullable(p_333155_.getAny(atom3, atom4))
      );
      dictionary.put(atom2, StringReaderTerms.character('*'), p_328666_ -> Unit.INSTANCE);
      dictionary.put(atom3, new ComponentPredicateParser.ElementLookupRule<>(namedrule, p_329972_));
      dictionary.put(atom4, new ComponentPredicateParser.TagLookupRule<>(namedrule, p_329972_));
      dictionary.put(
         atom5, Term.sequence(dictionary.named(atom6), Term.optional(Term.sequence(StringReaderTerms.character(','), dictionary.named(atom5)))), p_332096_ -> {
            T t = p_329972_.anyOf(p_332096_.getOrThrow(atom6));
            return Optional.ofNullable(p_332096_.get(atom5)).map(p_448514_ -> Util.copyAndAdd(t, (List<T>)p_448514_)).orElse(List.of(t));
         }
      );
      dictionary.put(
         atom6, Term.sequence(dictionary.named(atom7), Term.optional(Term.sequence(StringReaderTerms.character('|'), dictionary.named(atom6)))), p_334061_ -> {
            T t = p_334061_.getOrThrow(atom7);
            return Optional.ofNullable(p_334061_.get(atom6)).map(p_448512_ -> Util.copyAndAdd(t, (List<T>)p_448512_)).orElse(List.of(t));
         }
      );
      dictionary.put(
         atom7,
         Term.alternative(dictionary.named(atom9), Term.sequence(StringReaderTerms.character('!'), dictionary.named(atom8))),
         p_335341_ -> p_335341_.getAnyOrThrow(atom9, atom8)
      );
      dictionary.put(atom8, dictionary.named(atom9), p_331974_ -> p_329972_.negate(p_331974_.getOrThrow(atom9)));
      dictionary.putComplex(
         atom9,
         Term.alternative(
            Term.sequence(dictionary.named(atom10), StringReaderTerms.character('='), Term.cut(), dictionary.named(atom13)),
            Term.sequence(dictionary.named(atom11), StringReaderTerms.character('~'), Term.cut(), dictionary.named(atom13)),
            dictionary.named(atom10)
         ),
         p_389645_ -> {
            Scope scope = p_389645_.scope();
            P p = scope.get(atom11);

            try {
               if (p != null) {
                  Dynamic<?> dynamic1 = scope.getOrThrow(atom13);
                  return p_329972_.createPredicateTest((ImmutableStringReader)p_389645_.input(), p, dynamic1);
               } else {
                  C c = scope.getOrThrow(atom10);
                  Dynamic<?> dynamic = scope.get(atom13);
                  return dynamic != null
                     ? p_329972_.createComponentTest((ImmutableStringReader)p_389645_.input(), c, dynamic)
                     : p_329972_.createComponentTest((ImmutableStringReader)p_389645_.input(), c);
               }
            } catch (CommandSyntaxException commandsyntaxexception) {
               p_389645_.errorCollector().store(p_389645_.mark(), commandsyntaxexception);
               return null;
            }
         }
      );
      dictionary.put(atom10, new ComponentPredicateParser.ComponentLookupRule<>(namedrule, p_329972_));
      dictionary.put(atom11, new ComponentPredicateParser.PredicateLookupRule<>(namedrule, p_329972_));
      dictionary.put(atom13, new TagParseRule(NbtOps.INSTANCE));
      return new Grammar<>(dictionary, namedrule1);
   }

   static class ComponentLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, C> {
      ComponentLookupRule(NamedRule<StringReader, Identifier> p_393495_, ComponentPredicateParser.Context<T, C, P> p_336202_) {
         super(p_393495_, p_336202_);
      }

      @Override
      protected C validateElement(ImmutableStringReader p_335905_, Identifier p_458545_) throws Exception {
         return this.context.lookupComponentType(p_335905_, p_458545_);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listComponentTypes();
      }
   }

   public interface Context<T, C, P> {
      T forElementType(ImmutableStringReader var1, Identifier var2) throws CommandSyntaxException;

      Stream<Identifier> listElementTypes();

      T forTagType(ImmutableStringReader var1, Identifier var2) throws CommandSyntaxException;

      Stream<Identifier> listTagTypes();

      C lookupComponentType(ImmutableStringReader var1, Identifier var2) throws CommandSyntaxException;

      Stream<Identifier> listComponentTypes();

      T createComponentTest(ImmutableStringReader var1, C var2, Dynamic<?> var3) throws CommandSyntaxException;

      T createComponentTest(ImmutableStringReader var1, C var2);

      P lookupPredicateType(ImmutableStringReader var1, Identifier var2) throws CommandSyntaxException;

      Stream<Identifier> listPredicateTypes();

      T createPredicateTest(ImmutableStringReader var1, P var2, Dynamic<?> var3) throws CommandSyntaxException;

      T negate(T var1);

      T anyOf(List<T> var1);
   }

   static class ElementLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      ElementLookupRule(NamedRule<StringReader, Identifier> p_391201_, ComponentPredicateParser.Context<T, C, P> p_333665_) {
         super(p_391201_, p_333665_);
      }

      @Override
      protected T validateElement(ImmutableStringReader p_336288_, Identifier p_450478_) throws Exception {
         return this.context.forElementType(p_336288_, p_450478_);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listElementTypes();
      }
   }

   static class PredicateLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, P> {
      PredicateLookupRule(NamedRule<StringReader, Identifier> p_397214_, ComponentPredicateParser.Context<T, C, P> p_335118_) {
         super(p_397214_, p_335118_);
      }

      @Override
      protected P validateElement(ImmutableStringReader p_334282_, Identifier p_460685_) throws Exception {
         return this.context.lookupPredicateType(p_334282_, p_460685_);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listPredicateTypes();
      }
   }

   static class TagLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      TagLookupRule(NamedRule<StringReader, Identifier> p_397926_, ComponentPredicateParser.Context<T, C, P> p_330358_) {
         super(p_397926_, p_330358_);
      }

      @Override
      protected T validateElement(ImmutableStringReader p_335818_, Identifier p_450531_) throws Exception {
         return this.context.forTagType(p_335818_, p_450531_);
      }

      @Override
      public Stream<Identifier> possibleResources() {
         return this.context.listTagTypes();
      }
   }
}
