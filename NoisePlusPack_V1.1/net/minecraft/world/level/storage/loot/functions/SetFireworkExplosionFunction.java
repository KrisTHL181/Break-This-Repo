package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetFireworkExplosionFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetFireworkExplosionFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_332148_ -> commonFields(p_332148_)
         .and(
            p_332148_.group(
               FireworkExplosion.Shape.CODEC.optionalFieldOf("shape").forGetter(p_328575_ -> p_328575_.shape),
               FireworkExplosion.COLOR_LIST_CODEC.optionalFieldOf("colors").forGetter(p_335296_ -> p_335296_.colors),
               FireworkExplosion.COLOR_LIST_CODEC.optionalFieldOf("fade_colors").forGetter(p_333347_ -> p_333347_.fadeColors),
               Codec.BOOL.optionalFieldOf("trail").forGetter(p_329057_ -> p_329057_.trail),
               Codec.BOOL.optionalFieldOf("twinkle").forGetter(p_333792_ -> p_333792_.twinkle)
            )
         )
         .apply(p_332148_, SetFireworkExplosionFunction::new)
   );
   public static final FireworkExplosion DEFAULT_VALUE = new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL, IntList.of(), IntList.of(), false, false);
   final Optional<FireworkExplosion.Shape> shape;
   final Optional<IntList> colors;
   final Optional<IntList> fadeColors;
   final Optional<Boolean> trail;
   final Optional<Boolean> twinkle;

   public SetFireworkExplosionFunction(
      List<LootItemCondition> p_328435_,
      Optional<FireworkExplosion.Shape> p_335053_,
      Optional<IntList> p_331523_,
      Optional<IntList> p_331948_,
      Optional<Boolean> p_330337_,
      Optional<Boolean> p_335969_
   ) {
      super(p_328435_);
      this.shape = p_335053_;
      this.colors = p_331523_;
      this.fadeColors = p_331948_;
      this.trail = p_330337_;
      this.twinkle = p_335969_;
   }

   @Override
   protected ItemStack run(ItemStack p_328627_, LootContext p_327748_) {
      p_328627_.update(DataComponents.FIREWORK_EXPLOSION, DEFAULT_VALUE, this::apply);
      return p_328627_;
   }

   private FireworkExplosion apply(FireworkExplosion p_329657_) {
      return new FireworkExplosion(
         this.shape.orElseGet(p_329657_::shape),
         this.colors.orElseGet(p_329657_::colors),
         this.fadeColors.orElseGet(p_329657_::fadeColors),
         this.trail.orElseGet(p_329657_::hasTrail),
         this.twinkle.orElseGet(p_329657_::hasTwinkle)
      );
   }

   @Override
   public LootItemFunctionType<SetFireworkExplosionFunction> getType() {
      return LootItemFunctions.SET_FIREWORK_EXPLOSION;
   }
}
