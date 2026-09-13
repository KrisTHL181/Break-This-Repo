package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import org.jspecify.annotations.Nullable;

public class SnbtOperations {
   static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_STRING_UUID = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_string_uuid"))
   );
   static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NUMBER_OR_BOOLEAN = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_number_or_boolean"))
   );
   public static final String BUILTIN_TRUE = "true";
   public static final String BUILTIN_FALSE = "false";
   public static final Map<SnbtOperations.BuiltinKey, SnbtOperations.BuiltinOperation> BUILTIN_OPERATIONS = Map.of(
      new SnbtOperations.BuiltinKey("bool", 1), new SnbtOperations.BuiltinOperation() {
         @Override
         public <T> T run(DynamicOps<T> p_395654_, List<T> p_392359_, ParseState<StringReader> p_395490_) {
            Boolean obool = convert(p_395654_, p_392359_.getFirst());
            if (obool == null) {
               p_395490_.errorCollector().store(p_395490_.mark(), SnbtOperations.ERROR_EXPECTED_NUMBER_OR_BOOLEAN);
               return null;
            } else {
               return (T)p_395654_.createBoolean(obool);
            }
         }

         private static <T> @Nullable Boolean convert(DynamicOps<T> p_393221_, T p_394284_) {
            Optional<Boolean> optional = p_393221_.getBooleanValue(p_394284_).result();
            if (optional.isPresent()) {
               return optional.get();
            }

            Optional<Number> optional1 = p_393221_.getNumberValue(p_394284_).result();
            return optional1.isPresent() ? optional1.get().doubleValue() != 0.0 : null;
         }
      }, new SnbtOperations.BuiltinKey("uuid", 1), new SnbtOperations.BuiltinOperation() {
         @Override
         public <T> T run(DynamicOps<T> p_395077_, List<T> p_392367_, ParseState<StringReader> p_396804_) {
            Optional<String> optional = p_395077_.getStringValue(p_392367_.getFirst()).result();
            if (optional.isEmpty()) {
               p_396804_.errorCollector().store(p_396804_.mark(), SnbtOperations.ERROR_EXPECTED_STRING_UUID);
               return null;
            }

            UUID uuid;
            try {
               uuid = UUID.fromString(optional.get());
            } catch (IllegalArgumentException illegalargumentexception) {
               p_396804_.errorCollector().store(p_396804_.mark(), SnbtOperations.ERROR_EXPECTED_STRING_UUID);
               return null;
            }

            return (T)p_395077_.createIntList(IntStream.of(UUIDUtil.uuidToIntArray(uuid)));
         }
      }
   );
   public static final SuggestionSupplier<StringReader> BUILTIN_IDS = new SuggestionSupplier<StringReader>() {
      private final Set<String> keys = Stream.concat(
            Stream.of("false", "true"), SnbtOperations.BUILTIN_OPERATIONS.keySet().stream().map(SnbtOperations.BuiltinKey::id)
         )
         .collect(Collectors.toSet());

      @Override
      public Stream<String> possibleValues(ParseState<StringReader> p_397371_) {
         return this.keys.stream();
      }
   };

   public record BuiltinKey(String id, int argCount) {
      @Override
      public String toString() {
         return this.id + "/" + this.argCount;
      }
   }

   public interface BuiltinOperation {
      <T> @Nullable T run(DynamicOps<T> var1, List<T> var2, ParseState<StringReader> var3);
   }
}
