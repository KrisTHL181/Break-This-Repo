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
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_STRING_UUID = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_string_uuid"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NUMBER_OR_BOOLEAN = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_number_or_boolean"))
   );
   public static final String BUILTIN_TRUE = "true";
   public static final String BUILTIN_FALSE = "false";
   public static final Map<SnbtOperations.BuiltinKey, SnbtOperations.BuiltinOperation> BUILTIN_OPERATIONS = Map.of(
      new SnbtOperations.BuiltinKey("bool", 1), new SnbtOperations.BuiltinOperation() {
         @Override
         public <T> T run(final DynamicOps<T> ops, final List<T> arguments, final ParseState<StringReader> state) {
            Boolean result = convert(ops, arguments.getFirst());
            if (result == null) {
               state.errorCollector().store(state.mark(), SnbtOperations.ERROR_EXPECTED_NUMBER_OR_BOOLEAN);
               return null;
            } else {
               return (T)ops.createBoolean(result);
            }
         }

         private static <T> @Nullable Boolean convert(final DynamicOps<T> ops, final T arg) {
            Optional<Boolean> asBoolean = ops.getBooleanValue(arg).result();
            if (asBoolean.isPresent()) {
               return asBoolean.get();
            }

            Optional<Number> asNumber = ops.getNumberValue(arg).result();
            return asNumber.isPresent() ? asNumber.get().doubleValue() != 0.0 : null;
         }
      }, new SnbtOperations.BuiltinKey("uuid", 1), new SnbtOperations.BuiltinOperation() {
         @Override
         public <T> T run(final DynamicOps<T> ops, final List<T> arguments, final ParseState<StringReader> state) {
            Optional<String> arg = ops.getStringValue(arguments.getFirst()).result();
            if (arg.isEmpty()) {
               state.errorCollector().store(state.mark(), SnbtOperations.ERROR_EXPECTED_STRING_UUID);
               return null;
            }

            UUID uuid;
            try {
               uuid = UUID.fromString(arg.get());
            } catch (IllegalArgumentException e) {
               state.errorCollector().store(state.mark(), SnbtOperations.ERROR_EXPECTED_STRING_UUID);
               return null;
            }

            return (T)ops.createIntList(IntStream.of(UUIDUtil.uuidToIntArray(uuid)));
         }
      }
   );
   public static final SuggestionSupplier<StringReader> BUILTIN_IDS = new SuggestionSupplier<StringReader>() {
      private final Set<String> keys = Stream.concat(
            Stream.of("false", "true"), SnbtOperations.BUILTIN_OPERATIONS.keySet().stream().map(SnbtOperations.BuiltinKey::id)
         )
         .collect(Collectors.toSet());

      @Override
      public Stream<String> possibleValues(final ParseState<StringReader> state) {
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
      <T> @Nullable T run(DynamicOps<T> ops, List<T> arguments, ParseState<StringReader> state);
   }
}
