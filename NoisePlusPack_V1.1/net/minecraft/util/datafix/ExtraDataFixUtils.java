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
   public static Dynamic<?> fixBlockPos(Dynamic<?> p_330445_) {
      Optional<Number> optional = p_330445_.get("X").asNumber().result();
      Optional<Number> optional1 = p_330445_.get("Y").asNumber().result();
      Optional<Number> optional2 = p_330445_.get("Z").asNumber().result();
      return !optional.isEmpty() && !optional1.isEmpty() && !optional2.isEmpty()
         ? createBlockPos(p_330445_, optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue())
         : p_330445_;
   }

   public static Dynamic<?> fixInlineBlockPos(Dynamic<?> p_396332_, String p_394183_, String p_393393_, String p_396117_, String p_391755_) {
      Optional<Number> optional = p_396332_.get(p_394183_).asNumber().result();
      Optional<Number> optional1 = p_396332_.get(p_393393_).asNumber().result();
      Optional<Number> optional2 = p_396332_.get(p_396117_).asNumber().result();
      return !optional.isEmpty() && !optional1.isEmpty() && !optional2.isEmpty()
         ? p_396332_.remove(p_394183_)
            .remove(p_393393_)
            .remove(p_396117_)
            .set(p_391755_, createBlockPos(p_396332_, optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue()))
         : p_396332_;
   }

   public static Dynamic<?> createBlockPos(Dynamic<?> p_396037_, int p_393850_, int p_395203_, int p_393761_) {
      return p_396037_.createIntList(IntStream.of(p_393850_, p_395203_, p_393761_));
   }

   public static <T, R> Typed<R> cast(Type<R> p_332791_, Typed<T> p_329826_) {
      return new Typed(p_332791_, p_329826_.getOps(), p_329826_.getValue());
   }

   public static <T> Typed<T> cast(Type<T> p_397444_, Object p_396508_, DynamicOps<?> p_394824_) {
      return new Typed(p_397444_, p_394824_, p_396508_);
   }

   public static Type<?> patchSubType(Type<?> p_362193_, Type<?> p_361959_, Type<?> p_368313_) {
      return p_362193_.all(typePatcher(p_361959_, p_368313_), true, false).view().newType();
   }

   private static <A, B> TypeRewriteRule typePatcher(Type<A> p_365721_, Type<B> p_368008_) {
      RewriteResult<A, B> rewriteresult = RewriteResult.create(View.create("Patcher", p_365721_, p_368008_, p_358817_ -> p_358825_ -> {
         throw new UnsupportedOperationException();
      }), new BitSet());
      return TypeRewriteRule.everywhere(TypeRewriteRule.ifSame(p_365721_, rewriteresult), PointFreeRule.nop(), true, true);
   }

   @SafeVarargs
   public static <T> Function<Typed<?>, Typed<?>> chainAllFilters(Function<Typed<?>, Typed<?>>... p_343226_) {
      return p_344666_ -> {
         for (Function<Typed<?>, Typed<?>> function : p_343226_) {
            p_344666_ = function.apply(p_344666_);
         }

         return p_344666_;
      };
   }

   public static Dynamic<?> blockState(String p_364037_, Map<String, String> p_363925_) {
      Dynamic<Tag> dynamic = new Dynamic(NbtOps.INSTANCE, new CompoundTag());
      Dynamic<Tag> dynamic1 = dynamic.set("Name", dynamic.createString(p_364037_));
      if (!p_363925_.isEmpty()) {
         dynamic1 = dynamic1.set(
            "Properties",
            dynamic.createMap(
               p_363925_.entrySet()
                  .stream()
                  .collect(Collectors.toMap(p_358821_ -> dynamic.createString(p_358821_.getKey()), p_358819_ -> dynamic.createString(p_358819_.getValue())))
            )
         );
      }

      return dynamic1;
   }

   public static Dynamic<?> blockState(String p_365301_) {
      return blockState(p_365301_, Map.of());
   }

   public static Dynamic<?> fixStringField(Dynamic<?> p_360869_, String p_368546_, UnaryOperator<String> p_368245_) {
      return p_360869_.update(
         p_368546_, p_358824_ -> (Dynamic)DataFixUtils.orElse(p_358824_.asString().map(p_368245_).map(p_360869_::createString).result(), p_358824_)
      );
   }

   public static String dyeColorIdToName(int p_395989_) {
      return switch (p_395989_) {
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

   public static <T> Typed<?> readAndSet(Typed<?> p_391819_, OpticFinder<T> p_391269_, Dynamic<?> p_395267_) {
      return p_391819_.set(p_391269_, Util.readTypedOrThrow(p_391269_.type(), p_395267_, true));
   }
}
