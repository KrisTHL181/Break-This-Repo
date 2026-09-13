package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class FillPlayerHead extends LootItemConditionalFunction {
   public static final MapCodec<FillPlayerHead> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i).and(LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(f -> f.entityTarget)).apply(i, FillPlayerHead::new)
   );
   private final LootContext.EntityTarget entityTarget;

   public FillPlayerHead(final List<LootItemCondition> predicates, final LootContext.EntityTarget entityTarget) {
      super(predicates);
      this.entityTarget = entityTarget;
   }

   @Override
   public MapCodec<FillPlayerHead> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(this.entityTarget.contextParam());
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      if (itemStack.is(Items.PLAYER_HEAD) && context.getOptionalParameter(this.entityTarget.contextParam()) instanceof Player dataDonor) {
         itemStack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(dataDonor.getGameProfile()));
      }

      return itemStack;
   }

   public static LootItemConditionalFunction.Builder<?> fillPlayerHead(final LootContext.EntityTarget entityTarget) {
      return simpleBuilder(conditions -> new FillPlayerHead(conditions, entityTarget));
   }
}
