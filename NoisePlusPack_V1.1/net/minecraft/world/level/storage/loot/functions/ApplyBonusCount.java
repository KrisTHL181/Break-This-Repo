package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyBonusCount extends LootItemConditionalFunction {
   private static final Map<Identifier, ApplyBonusCount.FormulaType> FORMULAS = Stream.of(
         ApplyBonusCount.BinomialWithBonusCount.TYPE, ApplyBonusCount.OreDrops.TYPE, ApplyBonusCount.UniformBonusCount.TYPE
      )
      .collect(Collectors.toMap(ApplyBonusCount.FormulaType::id, Function.identity()));
   private static final Codec<ApplyBonusCount.FormulaType> FORMULA_TYPE_CODEC = Identifier.CODEC
      .comapFlatMap(
         p_454505_ -> {
            ApplyBonusCount.FormulaType applybonuscount$formulatype = FORMULAS.get(p_454505_);
            return applybonuscount$formulatype != null
               ? DataResult.success(applybonuscount$formulatype)
               : DataResult.error(() -> "No formula type with id: '" + p_454505_ + "'");
         },
         ApplyBonusCount.FormulaType::id
      );
   private static final MapCodec<ApplyBonusCount.Formula> FORMULA_CODEC = ExtraCodecs.dispatchOptionalValue(
      "formula", "parameters", FORMULA_TYPE_CODEC, ApplyBonusCount.Formula::getType, ApplyBonusCount.FormulaType::codec
   );
   public static final MapCodec<ApplyBonusCount> CODEC = RecordCodecBuilder.mapCodec(
      p_341977_ -> commonFields(p_341977_)
         .and(
            p_341977_.group(
               Enchantment.CODEC.fieldOf("enchantment").forGetter(p_297072_ -> p_297072_.enchantment), FORMULA_CODEC.forGetter(p_297058_ -> p_297058_.formula)
            )
         )
         .apply(p_341977_, ApplyBonusCount::new)
   );
   private final Holder<Enchantment> enchantment;
   private final ApplyBonusCount.Formula formula;

   private ApplyBonusCount(List<LootItemCondition> p_298095_, Holder<Enchantment> p_298508_, ApplyBonusCount.Formula p_79905_) {
      super(p_298095_);
      this.enchantment = p_298508_;
      this.formula = p_79905_;
   }

   @Override
   public LootItemFunctionType<ApplyBonusCount> getType() {
      return LootItemFunctions.APPLY_BONUS;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.TOOL);
   }

   @Override
   public ItemStack run(ItemStack p_79913_, LootContext p_79914_) {
      ItemStack itemstack = p_79914_.getOptionalParameter(LootContextParams.TOOL);
      if (itemstack != null) {
         int i = EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, itemstack);
         int j = this.formula.calculateNewCount(p_79914_.getRandom(), p_79913_.getCount(), i);
         p_79913_.setCount(j);
      }

      return p_79913_;
   }

   public static LootItemConditionalFunction.Builder<?> addBonusBinomialDistributionCount(Holder<Enchantment> p_345010_, float p_79919_, int p_79920_) {
      return simpleBuilder(p_341983_ -> new ApplyBonusCount(p_341983_, p_345010_, new ApplyBonusCount.BinomialWithBonusCount(p_79920_, p_79919_)));
   }

   public static LootItemConditionalFunction.Builder<?> addOreBonusCount(Holder<Enchantment> p_344898_) {
      return simpleBuilder(p_450077_ -> new ApplyBonusCount(p_450077_, p_344898_, ApplyBonusCount.OreDrops.INSTANCE));
   }

   public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Holder<Enchantment> p_344489_) {
      return simpleBuilder(p_341988_ -> new ApplyBonusCount(p_341988_, p_344489_, new ApplyBonusCount.UniformBonusCount(1)));
   }

   public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Holder<Enchantment> p_344963_, int p_79923_) {
      return simpleBuilder(p_341986_ -> new ApplyBonusCount(p_341986_, p_344963_, new ApplyBonusCount.UniformBonusCount(p_79923_)));
   }

   record BinomialWithBonusCount(int extraRounds, float probability) implements ApplyBonusCount.Formula {
      private static final Codec<ApplyBonusCount.BinomialWithBonusCount> CODEC = RecordCodecBuilder.create(
         p_299643_ -> p_299643_.group(
               Codec.INT.fieldOf("extra").forGetter(ApplyBonusCount.BinomialWithBonusCount::extraRounds),
               Codec.FLOAT.fieldOf("probability").forGetter(ApplyBonusCount.BinomialWithBonusCount::probability)
            )
            .apply(p_299643_, ApplyBonusCount.BinomialWithBonusCount::new)
      );
      public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(
         Identifier.withDefaultNamespace("binomial_with_bonus_count"), CODEC
      );

      @Override
      public int calculateNewCount(RandomSource p_230965_, int p_230966_, int p_230967_) {
         for (int i = 0; i < p_230967_ + this.extraRounds; i++) {
            if (p_230965_.nextFloat() < this.probability) {
               p_230966_++;
            }
         }

         return p_230966_;
      }

      @Override
      public ApplyBonusCount.FormulaType getType() {
         return TYPE;
      }
   }

   interface Formula {
      int calculateNewCount(RandomSource var1, int var2, int var3);

      ApplyBonusCount.FormulaType getType();
   }

   record FormulaType(Identifier id, Codec<? extends ApplyBonusCount.Formula> codec) {
   }

   record OreDrops() implements ApplyBonusCount.Formula {
      public static final ApplyBonusCount.OreDrops INSTANCE = new ApplyBonusCount.OreDrops();
      public static final Codec<ApplyBonusCount.OreDrops> CODEC = MapCodec.unitCodec(INSTANCE);
      public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(Identifier.withDefaultNamespace("ore_drops"), CODEC);

      @Override
      public int calculateNewCount(RandomSource p_230972_, int p_230973_, int p_230974_) {
         if (p_230974_ > 0) {
            int i = p_230972_.nextInt(p_230974_ + 2) - 1;
            if (i < 0) {
               i = 0;
            }

            return p_230973_ * (i + 1);
         } else {
            return p_230973_;
         }
      }

      @Override
      public ApplyBonusCount.FormulaType getType() {
         return TYPE;
      }
   }

   record UniformBonusCount(int bonusMultiplier) implements ApplyBonusCount.Formula {
      public static final Codec<ApplyBonusCount.UniformBonusCount> CODEC = RecordCodecBuilder.create(
         p_297464_ -> p_297464_.group(Codec.INT.fieldOf("bonusMultiplier").forGetter(ApplyBonusCount.UniformBonusCount::bonusMultiplier))
            .apply(p_297464_, ApplyBonusCount.UniformBonusCount::new)
      );
      public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(Identifier.withDefaultNamespace("uniform_bonus_count"), CODEC);

      @Override
      public int calculateNewCount(RandomSource p_230976_, int p_230977_, int p_230978_) {
         return p_230977_ + p_230976_.nextInt(this.bonusMultiplier * p_230978_ + 1);
      }

      @Override
      public ApplyBonusCount.FormulaType getType() {
         return TYPE;
      }
   }
}
