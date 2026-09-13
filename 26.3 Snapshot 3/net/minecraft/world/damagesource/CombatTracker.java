package net.minecraft.world.damagesource;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.CommonLinks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class CombatTracker {
   public static final int RESET_DAMAGE_STATUS_TIME = 100;
   public static final int RESET_COMBAT_STATUS_TIME = 300;
   private static final Style INTENTIONAL_GAME_DESIGN_STYLE = Style.EMPTY
      .withClickEvent(new ClickEvent.OpenUrl(CommonLinks.INTENTIONAL_GAME_DESIGN_BUG))
      .withHoverEvent(new HoverEvent.ShowText(Component.literal("MCPE-28723")));
   private final List<CombatEntry> entries = Lists.newArrayList();
   private final LivingEntity mob;
   private int lastDamageTime;
   private int combatStartTime;
   private int combatEndTime;
   private boolean inCombat;
   private boolean takingDamage;

   public CombatTracker(final LivingEntity mob) {
      this.mob = mob;
   }

   public void recordDamage(final DamageSource source, final float damage) {
      this.recheckStatus();
      FallLocation fallLocation = FallLocation.getCurrentFallLocation(this.mob);
      CombatEntry entry = new CombatEntry(source, damage, fallLocation, (float)this.mob.fallDistance);
      this.entries.add(entry);
      this.lastDamageTime = this.mob.tickCount;
      this.takingDamage = true;
      if (!this.inCombat && this.mob.isAlive() && shouldEnterCombat(source)) {
         this.inCombat = true;
         this.combatStartTime = this.mob.tickCount;
         this.combatEndTime = this.combatStartTime;
         this.mob.onEnterCombat();
      }
   }

   private static boolean shouldEnterCombat(final DamageSource source) {
      return source.getEntity() instanceof LivingEntity;
   }

   private Component getMessageForAssistedFall(
      final Entity attackerEntity, final Component attackerName, final String messageWithItem, final String messageWithoutItem
   ) {
      ItemStack attackerItem = attackerEntity instanceof LivingEntity livingEntity ? livingEntity.getMainHandItem() : ItemStack.EMPTY;
      return !attackerItem.isEmpty() && attackerItem.has(DataComponents.CUSTOM_NAME)
         ? Component.translatable(messageWithItem, this.mob.getDisplayName(), attackerName, attackerItem.getDisplayName())
         : Component.translatable(messageWithoutItem, this.mob.getDisplayName(), attackerName);
   }

   private Component getFallMessage(final CombatEntry knockOffEntry, final @Nullable Entity killingEntity) {
      DamageSource knockOffSource = knockOffEntry.source();
      if (!knockOffSource.is(DamageTypeTags.IS_FALL) && !knockOffSource.is(DamageTypeTags.ALWAYS_MOST_SIGNIFICANT_FALL)) {
         Component killerName = getDisplayName(killingEntity);
         Entity attackerEntity = knockOffSource.getEntity();
         Component attackerName = getDisplayName(attackerEntity);
         if (attackerName != null && !attackerName.equals(killerName)) {
            return this.getMessageForAssistedFall(attackerEntity, attackerName, "death.fell.assist.item", "death.fell.assist");
         } else {
            return killerName != null
               ? this.getMessageForAssistedFall(killingEntity, killerName, "death.fell.finish.item", "death.fell.finish")
               : Component.translatable("death.fell.killer", this.mob.getDisplayName());
         }
      } else {
         FallLocation fallLocation = Objects.requireNonNullElse(knockOffEntry.fallLocation(), FallLocation.GENERIC);
         return Component.translatable(fallLocation.languageKey(), this.mob.getDisplayName());
      }
   }

   private static @Nullable Component getDisplayName(final @Nullable Entity entity) {
      return entity == null ? null : entity.getDisplayName();
   }

   public Component getDeathMessage() {
      if (this.entries.isEmpty()) {
         return Component.translatable("death.attack.generic", this.mob.getDisplayName());
      } else {
         CombatEntry killingBlow = this.entries.get(this.entries.size() - 1);
         DamageSource killingSource = killingBlow.source();
         CombatEntry knockOffEntry = this.getMostSignificantFall();
         DeathMessageType messageType = killingSource.type().deathMessageType();
         if (messageType == DeathMessageType.FALL_VARIANTS && knockOffEntry != null) {
            return this.getFallMessage(knockOffEntry, killingSource.getEntity());
         } else if (messageType == DeathMessageType.INTENTIONAL_GAME_DESIGN) {
            String deathMsg = "death.attack." + killingSource.getMsgId();
            Component link = ComponentUtils.wrapInSquareBrackets(Component.translatable(deathMsg + ".link")).withStyle(INTENTIONAL_GAME_DESIGN_STYLE);
            return Component.translatable(deathMsg + ".message", this.mob.getDisplayName(), link);
         } else {
            return killingSource.getLocalizedDeathMessage(this.mob);
         }
      }
   }

   private @Nullable CombatEntry getMostSignificantFall() {
      CombatEntry result = null;
      CombatEntry alternative = null;
      float altDamage = 0.0F;
      float bestFall = 0.0F;

      for (int i = 0; i < this.entries.size(); i++) {
         CombatEntry entry = this.entries.get(i);
         CombatEntry previous = i > 0 ? this.entries.get(i - 1) : null;
         DamageSource source = entry.source();
         boolean isFakeFall = source.is(DamageTypeTags.ALWAYS_MOST_SIGNIFICANT_FALL);
         float fallDistance = isFakeFall ? Float.MAX_VALUE : entry.fallDistance();
         if ((source.is(DamageTypeTags.IS_FALL) || isFakeFall) && fallDistance > 0.0F && (result == null || fallDistance > bestFall)) {
            if (i > 0) {
               result = previous;
            } else {
               result = entry;
            }

            bestFall = fallDistance;
         }

         if (entry.fallLocation() != null && (alternative == null || entry.damage() > altDamage)) {
            alternative = entry;
            altDamage = entry.damage();
         }
      }

      if (bestFall > 5.0F && result != null) {
         return result;
      } else {
         return altDamage > 5.0F && alternative != null ? alternative : null;
      }
   }

   public int getCombatDuration() {
      return this.inCombat ? this.mob.tickCount - this.combatStartTime : this.combatEndTime - this.combatStartTime;
   }

   public void recheckStatus() {
      int reset = this.inCombat ? 300 : 100;
      if (this.takingDamage && (!this.mob.isAlive() || this.mob.tickCount - this.lastDamageTime > reset)) {
         boolean wasInCombat = this.inCombat;
         this.takingDamage = false;
         this.inCombat = false;
         this.combatEndTime = this.mob.tickCount;
         if (wasInCombat) {
            this.mob.onLeaveCombat();
         }

         this.entries.clear();
      }
   }
}
