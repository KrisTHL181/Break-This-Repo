package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.slf4j.Logger;

public class SetItemDamageFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<SetItemDamageFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_297150_ -> commonFields(p_297150_)
         .and(
            p_297150_.group(
               NumberProviders.CODEC.fieldOf("damage").forGetter(p_297149_ -> p_297149_.damage),
               Codec.BOOL.fieldOf("add").orElse(false).forGetter(p_297151_ -> p_297151_.add)
            )
         )
         .apply(p_297150_, SetItemDamageFunction::new)
   );
   private final NumberProvider damage;
   private final boolean add;

   private SetItemDamageFunction(List<LootItemCondition> p_300823_, NumberProvider p_165428_, boolean p_165429_) {
      super(p_300823_);
      this.damage = p_165428_;
      this.add = p_165429_;
   }

   @Override
   public LootItemFunctionType<SetItemDamageFunction> getType() {
      return LootItemFunctions.SET_DAMAGE;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.damage.getReferencedContextParams();
   }

   @Override
   public ItemStack run(ItemStack p_81048_, LootContext p_81049_) {
      if (p_81048_.isDamageableItem()) {
         int i = p_81048_.getMaxDamage();
         float f = this.add ? 1.0F - (float)p_81048_.getDamageValue() / i : 0.0F;
         float f1 = 1.0F - Mth.clamp(this.damage.getFloat(p_81049_) + f, 0.0F, 1.0F);
         p_81048_.setDamageValue(Mth.floor(f1 * i));
      } else {
         LOGGER.warn("Couldn't set damage of loot item {}", p_81048_);
      }

      return p_81048_;
   }

   public static LootItemConditionalFunction.Builder<?> setDamage(NumberProvider p_165431_) {
      return simpleBuilder(p_297153_ -> new SetItemDamageFunction(p_297153_, p_165431_, false));
   }

   public static LootItemConditionalFunction.Builder<?> setDamage(NumberProvider p_165433_, boolean p_165434_) {
      return simpleBuilder(p_297148_ -> new SetItemDamageFunction(p_297148_, p_165433_, p_165434_));
   }
}
