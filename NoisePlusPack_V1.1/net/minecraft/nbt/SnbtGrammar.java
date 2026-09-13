package net.minecraft.nbt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.UnsignedBytes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.chars.CharList;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.GreedyPatternParseRule;
import net.minecraft.util.parsing.packrat.commands.GreedyPredicateParseRule;
import net.minecraft.util.parsing.packrat.commands.NumberRunParseRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.UnquotedStringParseRule;
import org.jspecify.annotations.Nullable;

public class SnbtGrammar {
   private static final DynamicCommandExceptionType ERROR_NUMBER_PARSE_FAILURE = new DynamicCommandExceptionType(
      p_391930_ -> Component.translatableEscape("snbt.parser.number_parse_failure", p_391930_)
   );
   static final DynamicCommandExceptionType ERROR_EXPECTED_HEX_ESCAPE = new DynamicCommandExceptionType(
      p_392560_ -> Component.translatableEscape("snbt.parser.expected_hex_escape", p_392560_)
   );
   private static final DynamicCommandExceptionType ERROR_INVALID_CODEPOINT = new DynamicCommandExceptionType(
      p_392900_ -> Component.translatableEscape("snbt.parser.invalid_codepoint", p_392900_)
   );
   private static final DynamicCommandExceptionType ERROR_NO_SUCH_OPERATION = new DynamicCommandExceptionType(
      p_392553_ -> Component.translatableEscape("snbt.parser.no_such_operation", p_392553_)
   );
   static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_INTEGER_TYPE = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_integer_type"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_FLOAT_TYPE = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_float_type"))
   );
   static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NON_NEGATIVE_NUMBER = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_non_negative_number"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_INVALID_CHARACTER_NAME = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_character_name"))
   );
   static final DelayedException<CommandSyntaxException> ERROR_INVALID_ARRAY_ELEMENT_TYPE = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_array_element_type"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_INVALID_UNQUOTED_START = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_unquoted_start"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_UNQUOTED_STRING = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_unquoted_string"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_INVALID_STRING_CONTENTS = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_string_contents"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_BINARY_NUMERAL = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_binary_numeral"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_UNDESCORE_NOT_ALLOWED = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.underscore_not_allowed"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_DECIMAL_NUMERAL = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_decimal_numeral"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_HEX_NUMERAL = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_hex_numeral"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EMPTY_KEY = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.empty_key"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_LEADING_ZERO_NOT_ALLOWED = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.leading_zero_not_allowed"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_INFINITY_NOT_ALLOWED = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.infinity_not_allowed"))
   );
   private static final HexFormat HEX_ESCAPE = HexFormat.of().withUpperCase();
   private static final NumberRunParseRule BINARY_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_BINARY_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
      @Override
      protected boolean isAccepted(char p_393182_) {
         return switch (p_393182_) {
            case '0', '1', '_' -> true;
            default -> false;
         };
      }
   };
   private static final NumberRunParseRule DECIMAL_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_DECIMAL_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
      @Override
      protected boolean isAccepted(char p_397542_) {
         return switch (p_397542_) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_' -> true;
            default -> false;
         };
      }
   };
   private static final NumberRunParseRule HEX_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_HEX_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
      @Override
      protected boolean isAccepted(char p_396361_) {
         return switch (p_396361_) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', '_', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
            default -> false;
         };
      }
   };
   private static final GreedyPredicateParseRule PLAIN_STRING_CHUNK = new GreedyPredicateParseRule(1, ERROR_INVALID_STRING_CONTENTS) {
      @Override
      protected boolean isAccepted(char p_393190_) {
         return switch (p_393190_) {
            case '"', '\'', '\\' -> false;
            default -> true;
         };
      }
   };
   private static final StringReaderTerms.TerminalCharacters NUMBER_LOOKEAHEAD = new StringReaderTerms.TerminalCharacters(CharList.of()) {
      @Override
      protected boolean isAccepted(char p_392269_) {
         return SnbtGrammar.canStartNumber(p_392269_);
      }
   };
   private static final Pattern UNICODE_NAME = Pattern.compile("[-a-zA-Z0-9 ]+");

   static DelayedException<CommandSyntaxException> createNumberParseError(NumberFormatException p_391910_) {
      return DelayedException.create(ERROR_NUMBER_PARSE_FAILURE, p_391910_.getMessage());
   }

   public static @Nullable String escapeControlCharacters(char p_396580_) {
      return switch (p_396580_) {
         case '\b' -> "b";
         case '\t' -> "t";
         case '\n' -> "n";
         default -> p_396580_ < ' ' ? "x" + HEX_ESCAPE.toHexDigits((byte)p_396580_) : null;
         case '\f' -> "f";
         case '\r' -> "r";
      };
   }

   private static boolean isAllowedToStartUnquotedString(char p_395523_) {
      return !canStartNumber(p_395523_);
   }

   static boolean canStartNumber(char p_396460_) {
      return switch (p_396460_) {
         case '+', '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true;
         default -> false;
      };
   }

   static boolean needsUnderscoreRemoval(String p_393499_) {
      return p_393499_.indexOf(95) != -1;
   }

   private static void cleanAndAppend(StringBuilder p_391843_, String p_394727_) {
      cleanAndAppend(p_391843_, p_394727_, needsUnderscoreRemoval(p_394727_));
   }

   static void cleanAndAppend(StringBuilder p_394018_, String p_397997_, boolean p_393014_) {
      if (p_393014_) {
         for (char c0 : p_397997_.toCharArray()) {
            if (c0 != '_') {
               p_394018_.append(c0);
            }
         }
      } else {
         p_394018_.append(p_397997_);
      }
   }

   static short parseUnsignedShort(String p_392139_, int p_393529_) {
      int i = Integer.parseInt(p_392139_, p_393529_);
      if (i >> 16 == 0) {
         return (short)i;
      } else {
         throw new NumberFormatException("out of range: " + i);
      }
   }

   private static <T> @Nullable T createFloat(
      DynamicOps<T> p_393782_,
      SnbtGrammar.Sign p_396583_,
      @Nullable String p_394713_,
      @Nullable String p_392277_,
      SnbtGrammar.@Nullable Signed<String> p_397728_,
      SnbtGrammar.@Nullable TypeSuffix p_392491_,
      ParseState<?> p_395596_
   ) {
      StringBuilder stringbuilder = new StringBuilder();
      p_396583_.append(stringbuilder);
      if (p_394713_ != null) {
         cleanAndAppend(stringbuilder, p_394713_);
      }

      if (p_392277_ != null) {
         stringbuilder.append('.');
         cleanAndAppend(stringbuilder, p_392277_);
      }

      if (p_397728_ != null) {
         stringbuilder.append('e');
         p_397728_.sign().append(stringbuilder);
         cleanAndAppend(stringbuilder, p_397728_.value);
      }

      try {
         String s = stringbuilder.toString();

         return (T)(switch (p_392491_) {
            case null -> convertDouble(p_393782_, p_395596_, s);
            case FLOAT -> convertFloat(p_393782_, p_395596_, s);
            case DOUBLE -> convertDouble(p_393782_, p_395596_, s);
            default -> {
               p_395596_.errorCollector().store(p_395596_.mark(), ERROR_EXPECTED_FLOAT_TYPE);
               yield null;
            }
         });
      } catch (NumberFormatException numberformatexception) {
         p_395596_.errorCollector().store(p_395596_.mark(), createNumberParseError(numberformatexception));
         return null;
      }
   }

   private static <T> @Nullable T convertFloat(DynamicOps<T> p_393273_, ParseState<?> p_392261_, String p_394754_) {
      float f = Float.parseFloat(p_394754_);
      if (!Float.isFinite(f)) {
         p_392261_.errorCollector().store(p_392261_.mark(), ERROR_INFINITY_NOT_ALLOWED);
         return null;
      } else {
         return (T)p_393273_.createFloat(f);
      }
   }

   private static <T> @Nullable T convertDouble(DynamicOps<T> p_392730_, ParseState<?> p_394263_, String p_391648_) {
      double d0 = Double.parseDouble(p_391648_);
      if (!Double.isFinite(d0)) {
         p_394263_.errorCollector().store(p_394263_.mark(), ERROR_INFINITY_NOT_ALLOWED);
         return null;
      } else {
         return (T)p_392730_.createDouble(d0);
      }
   }

   private static String joinList(List<String> p_397403_) {
      return switch (p_397403_.size()) {
         case 0 -> "";
         case 1 -> (String)p_397403_.getFirst();
         default -> String.join("", p_397403_);
      };
   }

   public static <T> Grammar<T> createParser(DynamicOps<T> p_397642_) {
      T t = (T)p_397642_.createBoolean(true);
      T t1 = (T)p_397642_.createBoolean(false);
      T t2 = (T)p_397642_.emptyMap();
      T t3 = (T)p_397642_.emptyList();
      Dictionary<StringReader> dictionary = new Dictionary<>();
      Atom<SnbtGrammar.Sign> atom = Atom.of("sign");
      dictionary.put(
         atom,
         Term.alternative(
            Term.sequence(StringReaderTerms.character('+'), Term.marker(atom, SnbtGrammar.Sign.PLUS)),
            Term.sequence(StringReaderTerms.character('-'), Term.marker(atom, SnbtGrammar.Sign.MINUS))
         ),
         p_392282_ -> p_392282_.getOrThrow(atom)
      );
      Atom<SnbtGrammar.IntegerSuffix> atom1 = Atom.of("integer_suffix");
      dictionary.put(
         atom1,
         Term.alternative(
            Term.sequence(
               StringReaderTerms.characters('u', 'U'),
               Term.alternative(
                  Term.sequence(
                     StringReaderTerms.characters('b', 'B'),
                     Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.BYTE))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('s', 'S'),
                     Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.SHORT))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('i', 'I'),
                     Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.INT))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('l', 'L'),
                     Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.LONG))
                  )
               )
            ),
            Term.sequence(
               StringReaderTerms.characters('s', 'S'),
               Term.alternative(
                  Term.sequence(
                     StringReaderTerms.characters('b', 'B'),
                     Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.BYTE))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('s', 'S'),
                     Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.SHORT))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('i', 'I'),
                     Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.INT))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('l', 'L'),
                     Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.LONG))
                  )
               )
            ),
            Term.sequence(StringReaderTerms.characters('b', 'B'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.BYTE))),
            Term.sequence(StringReaderTerms.characters('s', 'S'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.SHORT))),
            Term.sequence(StringReaderTerms.characters('i', 'I'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.INT))),
            Term.sequence(StringReaderTerms.characters('l', 'L'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.LONG)))
         ),
         p_395615_ -> p_395615_.getOrThrow(atom1)
      );
      Atom<String> atom2 = Atom.of("binary_numeral");
      dictionary.put(atom2, BINARY_NUMERAL);
      Atom<String> atom3 = Atom.of("decimal_numeral");
      dictionary.put(atom3, DECIMAL_NUMERAL);
      Atom<String> atom4 = Atom.of("hex_numeral");
      dictionary.put(atom4, HEX_NUMERAL);
      Atom<SnbtGrammar.IntegerLiteral> atom5 = Atom.of("integer_literal");
      NamedRule<StringReader, SnbtGrammar.IntegerLiteral> namedrule = dictionary.put(
         atom5,
         Term.sequence(
            Term.optional(dictionary.named(atom)),
            Term.alternative(
               Term.sequence(
                  StringReaderTerms.character('0'),
                  Term.cut(),
                  Term.alternative(
                     Term.sequence(StringReaderTerms.characters('x', 'X'), Term.cut(), dictionary.named(atom4)),
                     Term.sequence(StringReaderTerms.characters('b', 'B'), dictionary.named(atom2)),
                     Term.sequence(dictionary.named(atom3), Term.cut(), Term.fail(ERROR_LEADING_ZERO_NOT_ALLOWED)),
                     Term.marker(atom3, "0")
                  )
               ),
               dictionary.named(atom3)
            ),
            Term.optional(dictionary.named(atom1))
         ),
         p_397578_ -> {
            SnbtGrammar.IntegerSuffix snbtgrammar$integersuffix = p_397578_.getOrDefault(atom1, SnbtGrammar.IntegerSuffix.EMPTY);
            SnbtGrammar.Sign snbtgrammar$sign = p_397578_.getOrDefault(atom, SnbtGrammar.Sign.PLUS);
            String s = p_397578_.get(atom3);
            if (s != null) {
               return new SnbtGrammar.IntegerLiteral(snbtgrammar$sign, SnbtGrammar.Base.DECIMAL, s, snbtgrammar$integersuffix);
            }

            String s1 = p_397578_.get(atom4);
            if (s1 != null) {
               return new SnbtGrammar.IntegerLiteral(snbtgrammar$sign, SnbtGrammar.Base.HEX, s1, snbtgrammar$integersuffix);
            }

            String s2 = p_397578_.getOrThrow(atom2);
            return new SnbtGrammar.IntegerLiteral(snbtgrammar$sign, SnbtGrammar.Base.BINARY, s2, snbtgrammar$integersuffix);
         }
      );
      Atom<SnbtGrammar.TypeSuffix> atom6 = Atom.of("float_type_suffix");
      dictionary.put(
         atom6,
         Term.alternative(
            Term.sequence(StringReaderTerms.characters('f', 'F'), Term.marker(atom6, SnbtGrammar.TypeSuffix.FLOAT)),
            Term.sequence(StringReaderTerms.characters('d', 'D'), Term.marker(atom6, SnbtGrammar.TypeSuffix.DOUBLE))
         ),
         p_395651_ -> p_395651_.getOrThrow(atom6)
      );
      Atom<SnbtGrammar.Signed<String>> atom7 = Atom.of("float_exponent_part");
      dictionary.put(
         atom7,
         Term.sequence(StringReaderTerms.characters('e', 'E'), Term.optional(dictionary.named(atom)), dictionary.named(atom3)),
         p_396537_ -> new SnbtGrammar.Signed<>(p_396537_.getOrDefault(atom, SnbtGrammar.Sign.PLUS), p_396537_.getOrThrow(atom3))
      );
      Atom<String> atom8 = Atom.of("float_whole_part");
      Atom<String> atom9 = Atom.of("float_fraction_part");
      Atom<T> atom10 = Atom.of("float_literal");
      dictionary.putComplex(
         atom10,
         Term.sequence(
            Term.optional(dictionary.named(atom)),
            Term.alternative(
               Term.sequence(
                  dictionary.namedWithAlias(atom3, atom8),
                  StringReaderTerms.character('.'),
                  Term.cut(),
                  Term.optional(dictionary.namedWithAlias(atom3, atom9)),
                  Term.optional(dictionary.named(atom7)),
                  Term.optional(dictionary.named(atom6))
               ),
               Term.sequence(
                  StringReaderTerms.character('.'),
                  Term.cut(),
                  dictionary.namedWithAlias(atom3, atom9),
                  Term.optional(dictionary.named(atom7)),
                  Term.optional(dictionary.named(atom6))
               ),
               Term.sequence(dictionary.namedWithAlias(atom3, atom8), dictionary.named(atom7), Term.cut(), Term.optional(dictionary.named(atom6))),
               Term.sequence(dictionary.namedWithAlias(atom3, atom8), Term.optional(dictionary.named(atom7)), dictionary.named(atom6))
            )
         ),
         p_392830_ -> {
            Scope scope = p_392830_.scope();
            SnbtGrammar.Sign snbtgrammar$sign = scope.getOrDefault(atom, SnbtGrammar.Sign.PLUS);
            String s = scope.get(atom8);
            String s1 = scope.get(atom9);
            SnbtGrammar.Signed<String> signed = scope.get(atom7);
            SnbtGrammar.TypeSuffix snbtgrammar$typesuffix = scope.get(atom6);
            return createFloat(p_397642_, snbtgrammar$sign, s, s1, signed, snbtgrammar$typesuffix, p_392830_);
         }
      );
      Atom<String> atom11 = Atom.of("string_hex_2");
      dictionary.put(atom11, new SnbtGrammar.SimpleHexLiteralParseRule(2));
      Atom<String> atom12 = Atom.of("string_hex_4");
      dictionary.put(atom12, new SnbtGrammar.SimpleHexLiteralParseRule(4));
      Atom<String> atom13 = Atom.of("string_hex_8");
      dictionary.put(atom13, new SnbtGrammar.SimpleHexLiteralParseRule(8));
      Atom<String> atom14 = Atom.of("string_unicode_name");
      dictionary.put(atom14, new GreedyPatternParseRule(UNICODE_NAME, ERROR_INVALID_CHARACTER_NAME));
      Atom<String> atom15 = Atom.of("string_escape_sequence");
      dictionary.putComplex(
         atom15,
         Term.alternative(
            Term.sequence(StringReaderTerms.character('b'), Term.marker(atom15, "\b")),
            Term.sequence(StringReaderTerms.character('s'), Term.marker(atom15, " ")),
            Term.sequence(StringReaderTerms.character('t'), Term.marker(atom15, "\t")),
            Term.sequence(StringReaderTerms.character('n'), Term.marker(atom15, "\n")),
            Term.sequence(StringReaderTerms.character('f'), Term.marker(atom15, "\f")),
            Term.sequence(StringReaderTerms.character('r'), Term.marker(atom15, "\r")),
            Term.sequence(StringReaderTerms.character('\\'), Term.marker(atom15, "\\")),
            Term.sequence(StringReaderTerms.character('\''), Term.marker(atom15, "'")),
            Term.sequence(StringReaderTerms.character('"'), Term.marker(atom15, "\"")),
            Term.sequence(StringReaderTerms.character('x'), dictionary.named(atom11)),
            Term.sequence(StringReaderTerms.character('u'), dictionary.named(atom12)),
            Term.sequence(StringReaderTerms.character('U'), dictionary.named(atom13)),
            Term.sequence(StringReaderTerms.character('N'), StringReaderTerms.character('{'), dictionary.named(atom14), StringReaderTerms.character('}'))
         ),
         p_395504_ -> {
            Scope scope = p_395504_.scope();
            String s = scope.getAny(atom15);
            if (s != null) {
               return s;
            }

            String s1 = scope.getAny(atom11, atom12, atom13);
            if (s1 != null) {
               int j = HexFormat.fromHexDigits(s1);
               if (!Character.isValidCodePoint(j)) {
                  p_395504_.errorCollector().store(p_395504_.mark(), DelayedException.create(ERROR_INVALID_CODEPOINT, String.format(Locale.ROOT, "U+%08X", j)));
                  return null;
               } else {
                  return Character.toString(j);
               }
            } else {
               String s2 = scope.getOrThrow(atom14);

               int i;
               try {
                  i = Character.codePointOf(s2);
               } catch (IllegalArgumentException illegalargumentexception) {
                  p_395504_.errorCollector().store(p_395504_.mark(), ERROR_INVALID_CHARACTER_NAME);
                  return null;
               }

               return Character.toString(i);
            }
         }
      );
      Atom<String> atom16 = Atom.of("string_plain_contents");
      dictionary.put(atom16, PLAIN_STRING_CHUNK);
      Atom<List<String>> atom17 = Atom.of("string_chunks");
      Atom<String> atom18 = Atom.of("string_contents");
      Atom<String> atom19 = Atom.of("single_quoted_string_chunk");
      NamedRule<StringReader, String> namedrule1 = dictionary.put(
         atom19,
         Term.alternative(
            dictionary.namedWithAlias(atom16, atom18),
            Term.sequence(StringReaderTerms.character('\\'), dictionary.namedWithAlias(atom15, atom18)),
            Term.sequence(StringReaderTerms.character('"'), Term.marker(atom18, "\""))
         ),
         p_393541_ -> p_393541_.getOrThrow(atom18)
      );
      Atom<String> atom20 = Atom.of("single_quoted_string_contents");
      dictionary.put(atom20, Term.repeated(namedrule1, atom17), p_391866_ -> joinList(p_391866_.getOrThrow(atom17)));
      Atom<String> atom21 = Atom.of("double_quoted_string_chunk");
      NamedRule<StringReader, String> namedrule2 = dictionary.put(
         atom21,
         Term.alternative(
            dictionary.namedWithAlias(atom16, atom18),
            Term.sequence(StringReaderTerms.character('\\'), dictionary.namedWithAlias(atom15, atom18)),
            Term.sequence(StringReaderTerms.character('\''), Term.marker(atom18, "'"))
         ),
         p_396068_ -> p_396068_.getOrThrow(atom18)
      );
      Atom<String> atom22 = Atom.of("double_quoted_string_contents");
      dictionary.put(atom22, Term.repeated(namedrule2, atom17), p_397848_ -> joinList(p_397848_.getOrThrow(atom17)));
      Atom<String> atom23 = Atom.of("quoted_string_literal");
      dictionary.put(
         atom23,
         Term.alternative(
            Term.sequence(
               StringReaderTerms.character('"'), Term.cut(), Term.optional(dictionary.namedWithAlias(atom22, atom18)), StringReaderTerms.character('"')
            ),
            Term.sequence(StringReaderTerms.character('\''), Term.optional(dictionary.namedWithAlias(atom20, atom18)), StringReaderTerms.character('\''))
         ),
         p_396331_ -> p_396331_.getOrThrow(atom18)
      );
      Atom<String> atom24 = Atom.of("unquoted_string");
      dictionary.put(atom24, new UnquotedStringParseRule(1, ERROR_EXPECTED_UNQUOTED_STRING));
      Atom<T> atom25 = Atom.of("literal");
      Atom<List<T>> atom26 = Atom.of("arguments");
      dictionary.put(
         atom26,
         Term.repeatedWithTrailingSeparator(dictionary.forward(atom25), atom26, StringReaderTerms.character(',')),
         p_397099_ -> p_397099_.getOrThrow(atom26)
      );
      Atom<T> atom27 = Atom.of("unquoted_string_or_builtin");
      dictionary.putComplex(
         atom27,
         Term.sequence(
            dictionary.named(atom24),
            Term.optional(Term.sequence(StringReaderTerms.character('('), dictionary.named(atom26), StringReaderTerms.character(')')))
         ),
         p_394493_ -> {
            Scope scope = p_394493_.scope();
            String s = scope.getOrThrow(atom24);
            if (!s.isEmpty() && isAllowedToStartUnquotedString(s.charAt(0))) {
               List<T> list = scope.get(atom26);
               if (list != null) {
                  SnbtOperations.BuiltinKey snbtoperations$builtinkey = new SnbtOperations.BuiltinKey(s, list.size());
                  SnbtOperations.BuiltinOperation snbtoperations$builtinoperation = SnbtOperations.BUILTIN_OPERATIONS.get(snbtoperations$builtinkey);
                  if (snbtoperations$builtinoperation != null) {
                     return snbtoperations$builtinoperation.run(p_397642_, list, p_394493_);
                  }

                  p_394493_.errorCollector().store(p_394493_.mark(), DelayedException.create(ERROR_NO_SUCH_OPERATION, snbtoperations$builtinkey.toString()));
                  return null;
               } else if (s.equalsIgnoreCase("true")) {
                  return t;
               } else {
                  return (T)(s.equalsIgnoreCase("false") ? t1 : p_397642_.createString(s));
               }
            } else {
               p_394493_.errorCollector().store(p_394493_.mark(), SnbtOperations.BUILTIN_IDS, ERROR_INVALID_UNQUOTED_START);
               return null;
            }
         }
      );
      Atom<String> atom28 = Atom.of("map_key");
      dictionary.put(atom28, Term.alternative(dictionary.named(atom23), dictionary.named(atom24)), p_391933_ -> p_391933_.getAnyOrThrow(atom23, atom24));
      Atom<Entry<String, T>> atom29 = Atom.of("map_entry");
      NamedRule<StringReader, Entry<String, T>> namedrule3 = dictionary.putComplex(
         atom29, Term.sequence(dictionary.named(atom28), StringReaderTerms.character(':'), dictionary.named(atom25)), p_396744_ -> {
            Scope scope = p_396744_.scope();
            String s = scope.getOrThrow(atom28);
            if (s.isEmpty()) {
               p_396744_.errorCollector().store(p_396744_.mark(), ERROR_EMPTY_KEY);
               return null;
            } else {
               T t4 = scope.getOrThrow(atom25);
               return Map.entry(s, t4);
            }
         }
      );
      Atom<List<Entry<String, T>>> atom30 = Atom.of("map_entries");
      dictionary.put(
         atom30, Term.repeatedWithTrailingSeparator(namedrule3, atom30, StringReaderTerms.character(',')), p_398026_ -> p_398026_.getOrThrow(atom30)
      );
      Atom<T> atom31 = Atom.of("map_literal");
      dictionary.put(atom31, Term.sequence(StringReaderTerms.character('{'), dictionary.named(atom30), StringReaderTerms.character('}')), p_397471_ -> {
         List<Entry<String, T>> list = p_397471_.getOrThrow(atom30);
         if (list.isEmpty()) {
            return t2;
         }

         Builder<T, T> builder = ImmutableMap.builderWithExpectedSize(list.size());

         for (Entry<String, T> entry : list) {
            builder.put(p_397642_.createString(entry.getKey()), entry.getValue());
         }

         return (T)p_397642_.createMap(builder.buildKeepingLast());
      });
      Atom<List<T>> atom32 = Atom.of("list_entries");
      dictionary.put(
         atom32,
         Term.repeatedWithTrailingSeparator(dictionary.forward(atom25), atom32, StringReaderTerms.character(',')),
         p_392047_ -> p_392047_.getOrThrow(atom32)
      );
      Atom<SnbtGrammar.ArrayPrefix> atom33 = Atom.of("array_prefix");
      dictionary.put(
         atom33,
         Term.alternative(
            Term.sequence(StringReaderTerms.character('B'), Term.marker(atom33, SnbtGrammar.ArrayPrefix.BYTE)),
            Term.sequence(StringReaderTerms.character('L'), Term.marker(atom33, SnbtGrammar.ArrayPrefix.LONG)),
            Term.sequence(StringReaderTerms.character('I'), Term.marker(atom33, SnbtGrammar.ArrayPrefix.INT))
         ),
         p_393699_ -> p_393699_.getOrThrow(atom33)
      );
      Atom<List<SnbtGrammar.IntegerLiteral>> atom34 = Atom.of("int_array_entries");
      dictionary.put(atom34, Term.repeatedWithTrailingSeparator(namedrule, atom34, StringReaderTerms.character(',')), p_393877_ -> p_393877_.getOrThrow(atom34));
      Atom<T> atom35 = Atom.of("list_literal");
      dictionary.putComplex(
         atom35,
         Term.sequence(
            StringReaderTerms.character('['),
            Term.alternative(Term.sequence(dictionary.named(atom33), StringReaderTerms.character(';'), dictionary.named(atom34)), dictionary.named(atom32)),
            StringReaderTerms.character(']')
         ),
         p_396934_ -> {
            Scope scope = p_396934_.scope();
            SnbtGrammar.ArrayPrefix snbtgrammar$arrayprefix = scope.get(atom33);
            if (snbtgrammar$arrayprefix != null) {
               List<SnbtGrammar.IntegerLiteral> list1 = scope.getOrThrow(atom34);
               return list1.isEmpty() ? snbtgrammar$arrayprefix.create(p_397642_) : snbtgrammar$arrayprefix.create(p_397642_, list1, p_396934_);
            } else {
               List<T> list = scope.getOrThrow(atom32);
               return (T)(list.isEmpty() ? t3 : p_397642_.createList(list.stream()));
            }
         }
      );
      NamedRule<StringReader, T> namedrule4 = dictionary.putComplex(
         atom25,
         Term.alternative(
            Term.sequence(Term.positiveLookahead(NUMBER_LOOKEAHEAD), Term.alternative(dictionary.namedWithAlias(atom10, atom25), dictionary.named(atom5))),
            Term.sequence(Term.positiveLookahead(StringReaderTerms.characters('"', '\'')), Term.cut(), dictionary.named(atom23)),
            Term.sequence(Term.positiveLookahead(StringReaderTerms.character('{')), Term.cut(), dictionary.namedWithAlias(atom31, atom25)),
            Term.sequence(Term.positiveLookahead(StringReaderTerms.character('[')), Term.cut(), dictionary.namedWithAlias(atom35, atom25)),
            dictionary.namedWithAlias(atom27, atom25)
         ),
         p_396574_ -> {
            Scope scope = p_396574_.scope();
            String s = scope.get(atom23);
            if (s != null) {
               return (T)p_397642_.createString(s);
            }

            SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral = scope.get(atom5);
            return snbtgrammar$integerliteral != null ? snbtgrammar$integerliteral.create(p_397642_, p_396574_) : scope.getOrThrow(atom25);
         }
      );
      return new Grammar<>(dictionary, namedrule4);
   }

   enum ArrayPrefix {
      BYTE(SnbtGrammar.TypeSuffix.BYTE) {
         private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

         @Override
         public <T> T create(DynamicOps<T> p_391802_) {
            return (T)p_391802_.createByteList(EMPTY_BUFFER);
         }

         @Override
         public <T> @Nullable T create(DynamicOps<T> p_394098_, List<SnbtGrammar.IntegerLiteral> p_396273_, ParseState<?> p_393802_) {
            ByteList bytelist = new ByteArrayList();

            for (SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral : p_396273_) {
               Number number = this.buildNumber(snbtgrammar$integerliteral, p_393802_);
               if (number == null) {
                  return null;
               }

               bytelist.add(number.byteValue());
            }

            return (T)p_394098_.createByteList(ByteBuffer.wrap(bytelist.toByteArray()));
         }
      },
      INT(SnbtGrammar.TypeSuffix.INT, SnbtGrammar.TypeSuffix.BYTE, SnbtGrammar.TypeSuffix.SHORT) {
         @Override
         public <T> T create(DynamicOps<T> p_396824_) {
            return (T)p_396824_.createIntList(IntStream.empty());
         }

         @Override
         public <T> @Nullable T create(DynamicOps<T> p_391735_, List<SnbtGrammar.IntegerLiteral> p_396111_, ParseState<?> p_395361_) {
            java.util.stream.IntStream.Builder builder = IntStream.builder();

            for (SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral : p_396111_) {
               Number number = this.buildNumber(snbtgrammar$integerliteral, p_395361_);
               if (number == null) {
                  return null;
               }

               builder.add(number.intValue());
            }

            return (T)p_391735_.createIntList(builder.build());
         }
      },
      LONG(SnbtGrammar.TypeSuffix.LONG, SnbtGrammar.TypeSuffix.BYTE, SnbtGrammar.TypeSuffix.SHORT, SnbtGrammar.TypeSuffix.INT) {
         @Override
         public <T> T create(DynamicOps<T> p_392954_) {
            return (T)p_392954_.createLongList(LongStream.empty());
         }

         @Override
         public <T> @Nullable T create(DynamicOps<T> p_397499_, List<SnbtGrammar.IntegerLiteral> p_391287_, ParseState<?> p_392056_) {
            java.util.stream.LongStream.Builder builder = LongStream.builder();

            for (SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral : p_391287_) {
               Number number = this.buildNumber(snbtgrammar$integerliteral, p_392056_);
               if (number == null) {
                  return null;
               }

               builder.add(number.longValue());
            }

            return (T)p_397499_.createLongList(builder.build());
         }
      };

      private final SnbtGrammar.TypeSuffix defaultType;
      private final Set<SnbtGrammar.TypeSuffix> additionalTypes;

      ArrayPrefix(final SnbtGrammar.TypeSuffix p_395393_, final SnbtGrammar.TypeSuffix... p_396535_) {
         this.additionalTypes = Set.of(p_396535_);
         this.defaultType = p_395393_;
      }

      public boolean isAllowed(SnbtGrammar.TypeSuffix p_392160_) {
         return p_392160_ == this.defaultType || this.additionalTypes.contains(p_392160_);
      }

      public abstract <T> T create(DynamicOps<T> var1);

      public abstract <T> @Nullable T create(DynamicOps<T> var1, List<SnbtGrammar.IntegerLiteral> var2, ParseState<?> var3);

      protected @Nullable Number buildNumber(SnbtGrammar.IntegerLiteral p_391520_, ParseState<?> p_393715_) {
         SnbtGrammar.TypeSuffix snbtgrammar$typesuffix = this.computeType(p_391520_.suffix);
         if (snbtgrammar$typesuffix == null) {
            p_393715_.errorCollector().store(p_393715_.mark(), SnbtGrammar.ERROR_INVALID_ARRAY_ELEMENT_TYPE);
            return null;
         } else {
            return p_391520_.create(JavaOps.INSTANCE, snbtgrammar$typesuffix, p_393715_);
         }
      }

      private SnbtGrammar.@Nullable TypeSuffix computeType(SnbtGrammar.IntegerSuffix p_394593_) {
         SnbtGrammar.TypeSuffix snbtgrammar$typesuffix = p_394593_.type();
         if (snbtgrammar$typesuffix == null) {
            return this.defaultType;
         } else {
            return !this.isAllowed(snbtgrammar$typesuffix) ? null : snbtgrammar$typesuffix;
         }
      }
   }

   enum Base {
      BINARY,
      DECIMAL,
      HEX;
   }

   record IntegerLiteral(SnbtGrammar.Sign sign, SnbtGrammar.Base base, String digits, SnbtGrammar.IntegerSuffix suffix) {
      private SnbtGrammar.SignedPrefix signedOrDefault() {
         if (this.suffix.signed != null) {
            return this.suffix.signed;
         }

         return switch (this.base) {
            case BINARY, HEX -> SnbtGrammar.SignedPrefix.UNSIGNED;
            case DECIMAL -> SnbtGrammar.SignedPrefix.SIGNED;
         };
      }

      private String cleanupDigits(SnbtGrammar.Sign p_393083_) {
         boolean flag = SnbtGrammar.needsUnderscoreRemoval(this.digits);
         if (p_393083_ != SnbtGrammar.Sign.MINUS && !flag) {
            return this.digits;
         }

         StringBuilder stringbuilder = new StringBuilder();
         p_393083_.append(stringbuilder);
         SnbtGrammar.cleanAndAppend(stringbuilder, this.digits, flag);
         return stringbuilder.toString();
      }

      public <T> @Nullable T create(DynamicOps<T> p_393106_, ParseState<?> p_394025_) {
         return this.create(p_393106_, Objects.requireNonNullElse(this.suffix.type, SnbtGrammar.TypeSuffix.INT), p_394025_);
      }

      public <T> @Nullable T create(DynamicOps<T> p_397874_, SnbtGrammar.TypeSuffix p_394212_, ParseState<?> p_397774_) {
         boolean flag = this.signedOrDefault() == SnbtGrammar.SignedPrefix.SIGNED;
         if (!flag && this.sign == SnbtGrammar.Sign.MINUS) {
            p_397774_.errorCollector().store(p_397774_.mark(), SnbtGrammar.ERROR_EXPECTED_NON_NEGATIVE_NUMBER);
            return null;
         }

         String s = this.cleanupDigits(this.sign);

         int i = switch (this.base) {
            case BINARY -> 2;
            case DECIMAL -> 10;
            case HEX -> 16;
         };

         try {
            if (flag) {
               return (T)(switch (p_394212_) {
                  case BYTE -> p_397874_.createByte(Byte.parseByte(s, i));
                  case SHORT -> p_397874_.createShort(Short.parseShort(s, i));
                  case INT -> p_397874_.createInt(Integer.parseInt(s, i));
                  case LONG -> p_397874_.createLong(Long.parseLong(s, i));
                  default -> {
                     p_397774_.errorCollector().store(p_397774_.mark(), SnbtGrammar.ERROR_EXPECTED_INTEGER_TYPE);
                     yield null;
                  }
               });
            } else {
               return (T)(switch (p_394212_) {
                  case BYTE -> p_397874_.createByte(UnsignedBytes.parseUnsignedByte(s, i));
                  case SHORT -> p_397874_.createShort(SnbtGrammar.parseUnsignedShort(s, i));
                  case INT -> p_397874_.createInt(Integer.parseUnsignedInt(s, i));
                  case LONG -> p_397874_.createLong(Long.parseUnsignedLong(s, i));
                  default -> {
                     p_397774_.errorCollector().store(p_397774_.mark(), SnbtGrammar.ERROR_EXPECTED_INTEGER_TYPE);
                     yield null;
                  }
               });
            }
         } catch (NumberFormatException numberformatexception) {
            p_397774_.errorCollector().store(p_397774_.mark(), SnbtGrammar.createNumberParseError(numberformatexception));
            return null;
         }
      }
   }

   record IntegerSuffix(SnbtGrammar.@Nullable SignedPrefix signed, SnbtGrammar.@Nullable TypeSuffix type) {
      public static final SnbtGrammar.IntegerSuffix EMPTY = new SnbtGrammar.IntegerSuffix(null, null);
   }

   enum Sign {
      PLUS,
      MINUS;

      public void append(StringBuilder p_396977_) {
         if (this == MINUS) {
            p_396977_.append("-");
         }
      }
   }

   record Signed<T>(SnbtGrammar.Sign sign, T value) {
   }

   enum SignedPrefix {
      SIGNED,
      UNSIGNED;
   }

   static class SimpleHexLiteralParseRule extends GreedyPredicateParseRule {
      public SimpleHexLiteralParseRule(int p_395601_) {
         super(p_395601_, p_395601_, DelayedException.create(SnbtGrammar.ERROR_EXPECTED_HEX_ESCAPE, String.valueOf(p_395601_)));
      }

      @Override
      protected boolean isAccepted(char p_396864_) {
         return switch (p_396864_) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
            default -> false;
         };
      }
   }

   enum TypeSuffix {
      FLOAT,
      DOUBLE,
      BYTE,
      SHORT,
      INT,
      LONG;
   }
}
