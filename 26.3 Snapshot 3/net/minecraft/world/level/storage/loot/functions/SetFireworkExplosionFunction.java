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
   public static final MapCodec<SetFireworkExplosionFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               FireworkExplosion.Shape.CODEC.optionalFieldOf("shape").forGetter(f -> f.shape),
               FireworkExplosion.COLOR_LIST_CODEC.optionalFieldOf("colors").forGetter(f -> f.colors),
               FireworkExplosion.COLOR_LIST_CODEC.optionalFieldOf("fade_colors").forGetter(f -> f.fadeColors),
               Codec.BOOL.optionalFieldOf("trail").forGetter(f -> f.trail),
               Codec.BOOL.optionalFieldOf("twinkle").forGetter(f -> f.twinkle)
            )
         )
         .apply(i, SetFireworkExplosionFunction::new)
   );
   public static final FireworkExplosion DEFAULT_VALUE = new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL, IntList.of(), IntList.of(), false, false);
   private final Optional<FireworkExplosion.Shape> shape;
   private final Optional<IntList> colors;
   private final Optional<IntList> fadeColors;
   private final Optional<Boolean> trail;
   private final Optional<Boolean> twinkle;

   public SetFireworkExplosionFunction(
      final List<LootItemCondition> predicates,
      final Optional<FireworkExplosion.Shape> shape,
      final Optional<IntList> colors,
      final Optional<IntList> fadeColors,
      final Optional<Boolean> hasTrail,
      final Optional<Boolean> hasTwinkle
   ) {
      super(predicates);
      this.shape = shape;
      this.colors = colors;
      this.fadeColors = fadeColors;
      this.trail = hasTrail;
      this.twinkle = hasTwinkle;
   }

   @Override
   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      itemStack.update(DataComponents.FIREWORK_EXPLOSION, DEFAULT_VALUE, this::apply);
      return itemStack;
   }

   private FireworkExplosion apply(final FireworkExplosion original) {
      return new FireworkExplosion(
         this.shape.orElseGet(original::shape),
         this.colors.orElseGet(original::colors),
         this.fadeColors.orElseGet(original::fadeColors),
         this.trail.orElseGet(original::hasTrail),
         this.twinkle.orElseGet(original::hasTwinkle)
      );
   }

   @Override
   public MapCodec<SetFireworkExplosionFunction> codec() {
      return MAP_CODEC;
   }
}
