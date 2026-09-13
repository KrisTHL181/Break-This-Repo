package net.minecraft.util.datafix;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.View;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.BitSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Util;

public class ExtraDataFixUtils {
   public static Dynamic<?> fixBlockPos(final Dynamic<?> pos) {
      Optional<Number> x = pos.get("X").asNumber().result();
      Optional<Number> y = pos.get("Y").asNumber().result();
      Optional<Number> z = pos.get("Z").asNumber().result();
      return !x.isEmpty() && !y.isEmpty() && !z.isEmpty() ? createBlockPos(pos, x.get().intValue(), y.get().intValue(), z.get().intValue()) : pos;
   }

   public static Dynamic<?> fixInlineBlockPos(final Dynamic<?> input, final String fieldX, final String fieldY, final String fieldZ, final String newField) {
      Optional<Number> x = input.get(fieldX).asNumber().result();
      Optional<Number> y = input.get(fieldY).asNumber().result();
      Optional<Number> z = input.get(fieldZ).asNumber().result();
      return !x.isEmpty() && !y.isEmpty() && !z.isEmpty()
         ? input.remove(fieldX).remove(fieldY).remove(fieldZ).set(newField, createBlockPos(input, x.get().intValue(), y.get().intValue(), z.get().intValue()))
         : input;
   }

   public static Dynamic<?> createBlockPos(final Dynamic<?> dynamic, final int x, final int y, final int z) {
      return dynamic.createIntList(IntStream.of(x, y, z));
   }

   public static <T, R> Typed<R> cast(final Type<R> type, final Typed<T> typed) {
      return new Typed(type, typed.getOps(), typed.getValue());
   }

   public static <T> Typed<T> cast(final Type<T> type, final Object value, final DynamicOps<?> ops) {
      return new Typed(type, ops, value);
   }

   public static Type<?> patchSubType(final Type<?> type, final Type<?> find, final Type<?> replace) {
      return type.all(typePatcher(find, replace), true, false).view().newType();
   }

   private static <A, B> TypeRewriteRule typePatcher(final Type<A> inputEntityType, final Type<B> outputEntityType) {
      RewriteResult<A, B> view = RewriteResult.create(View.create("Patcher", inputEntityType, outputEntityType, ops -> a -> {
         throw new UnsupportedOperationException();
      }), new BitSet());
      return TypeRewriteRule.everywhere(TypeRewriteRule.ifSame(inputEntityType, view), PointFreeRule.nop(), true, true);
   }

   @SafeVarargs
   public static <T> Function<Typed<?>, Typed<?>> chainAllFilters(final Function<Typed<?>, Typed<?>>... fixers) {
      return typed -> {
         for (Function<Typed<?>, Typed<?>> fixer : fixers) {
            typed = fixer.apply(typed);
         }

         return typed;
      };
   }

   public static Dynamic<?> blockState(final String id, final Map<String, String> properties) {
      Dynamic<Tag> dynamic = new Dynamic(NbtOps.INSTANCE, new CompoundTag());
      Dynamic<Tag> blockState = dynamic.set("Name", dynamic.createString(id));
      if (!properties.isEmpty()) {
         blockState = blockState.set(
            "Properties",
            dynamic.createMap(
               properties.entrySet()
                  .stream()
                  .collect(Collectors.toMap(entry -> dynamic.createString(entry.getKey()), entry -> dynamic.createString(entry.getValue())))
            )
         );
      }

      return blockState;
   }

   public static Dynamic<?> blockState(final String id) {
      return blockState(id, Map.of());
   }

   public static Dynamic<?> fixStringField(final Dynamic<?> dynamic, final String fieldName, final UnaryOperator<String> fix) {
      return dynamic.update(fieldName, field -> (Dynamic)DataFixUtils.orElse(field.asString().map(fix).map(dynamic::createString).result(), field));
   }

   public static String dyeColorIdToName(final int id) {
      return switch (id) {
         case 1 -> "orange";
         case 2 -> "magenta";
         case 3 -> "light_blue";
         case 4 -> "yellow";
         case 5 -> "lime";
         case 6 -> "pink";
         case 7 -> "gray";
         case 8 -> "light_gray";
         case 9 -> "cyan";
         case 10 -> "purple";
         case 11 -> "blue";
         case 12 -> "brown";
         case 13 -> "green";
         case 14 -> "red";
         case 15 -> "black";
         default -> "white";
      };
   }

   public static <T> Typed<?> readAndSet(final Typed<?> target, final OpticFinder<T> optic, final Dynamic<?> value) {
      return target.set(optic, Util.readTypedOrThrow(optic.type(), value, true));
   }
}
