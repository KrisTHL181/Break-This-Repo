package net.minecraft.world;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public record LockCode(ItemPredicate predicate) {
   public static final LockCode NO_LOCK = new LockCode(ItemPredicate.Builder.item().build());
   public static final Codec<LockCode> CODEC = ItemPredicate.CODEC.xmap(LockCode::new, LockCode::predicate);
   public static final String TAG_LOCK = "lock";

   public boolean unlocksWith(ItemStack p_19108_) {
      return this.predicate.test(p_19108_);
   }

   public void addToTag(ValueOutput p_408362_) {
      if (this != NO_LOCK) {
         p_408362_.store("lock", CODEC, this);
      }
   }

   public boolean canUnlock(Player p_450686_) {
      return p_450686_.isSpectator() || this.unlocksWith(p_450686_.getMainHandItem());
   }

   public static LockCode fromTag(ValueInput p_406465_) {
      return p_406465_.<LockCode>read("lock", CODEC).orElse(NO_LOCK);
   }
}
