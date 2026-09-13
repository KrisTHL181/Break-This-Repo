package net.minecraft.world;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public record LockCode(ItemPredicate predicate) {
   public static final LockCode NO_LOCK = new LockCode(ItemPredicate.Builder.item().build());
   public static final Codec<LockCode> CODEC = ItemPredicate.CODEC.xmap(LockCode::new, LockCode::predicate);
   public static final String TAG_LOCK = "lock";

   public boolean unlocksWith(final ItemStack itemStack) {
      return this.predicate.test(itemStack);
   }

   public void addToTag(final ValueOutput parent) {
      if (this != NO_LOCK) {
         parent.store("lock", CODEC, this);
      }
   }

   public boolean canUnlock(final Player player) {
      return player.isSpectator() || this.unlocksWith(player.getMainHandItem());
   }

   public static LockCode fromTag(final ValueInput parent) {
      return parent.<LockCode>read("lock", CODEC).orElse(NO_LOCK);
   }
}
