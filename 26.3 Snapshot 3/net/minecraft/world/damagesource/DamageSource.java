package net.minecraft.world.damagesource;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DamageSource {
   private final Holder<DamageType> type;
   private final @Nullable Entity causingEntity;
   private final @Nullable Entity directEntity;
   private final @Nullable Vec3 damageSourcePosition;

   @Override
   public String toString() {
      return "DamageSource (" + this.type().msgId() + ")";
   }

   public float getFoodExhaustion() {
      return this.type().exhaustion();
   }

   public boolean isDirect() {
      return this.causingEntity == this.directEntity;
   }

   private DamageSource(
      final Holder<DamageType> type, final @Nullable Entity directEntity, final @Nullable Entity causingEntity, final @Nullable Vec3 damageSourcePosition
   ) {
      this.type = type;
      this.causingEntity = causingEntity;
      this.directEntity = directEntity;
      this.damageSourcePosition = damageSourcePosition;
   }

   public DamageSource(final Holder<DamageType> type, final @Nullable Entity directEntity, final @Nullable Entity causingEntity) {
      this(type, directEntity, causingEntity, null);
   }

   public DamageSource(final Holder<DamageType> type, final Vec3 damageSourcePosition) {
      this(type, null, null, damageSourcePosition);
   }

   public DamageSource(final Holder<DamageType> type, final @Nullable Entity causingEntity) {
      this(type, causingEntity, causingEntity);
   }

   public DamageSource(final Holder<DamageType> type) {
      this(type, null, null, null);
   }

   public @Nullable Entity getDirectEntity() {
      return this.directEntity;
   }

   public @Nullable Entity getEntity() {
      return this.causingEntity;
   }

   public @Nullable ItemStack getWeaponItem() {
      return this.directEntity != null ? this.directEntity.getWeaponItem() : null;
   }

   public Component getLocalizedDeathMessage(final LivingEntity victim) {
      String deathMsg = "death.attack." + this.type().msgId();
      if (this.causingEntity == null && this.directEntity == null) {
         LivingEntity source = victim.getKillCredit();
         String playerMsg = deathMsg + ".player";
         return source != null
            ? Component.translatable(playerMsg, victim.getDisplayName(), source.getDisplayName())
            : Component.translatable(deathMsg, victim.getDisplayName());
      } else {
         Component name = this.causingEntity == null ? this.directEntity.getDisplayName() : this.causingEntity.getDisplayName();
         ItemStack held = this.causingEntity instanceof LivingEntity livingEntity ? livingEntity.getMainHandItem() : ItemStack.EMPTY;
         return !held.isEmpty() && held.has(DataComponents.CUSTOM_NAME)
            ? Component.translatable(deathMsg + ".item", victim.getDisplayName(), name, held.getDisplayName())
            : Component.translatable(deathMsg, victim.getDisplayName(), name);
      }
   }

   public String getMsgId() {
      return this.type().msgId();
   }

   public boolean scalesWithDifficulty() {
      return switch (this.type().scaling()) {
         case NEVER -> false;
         case WHEN_CAUSED_BY_LIVING_NON_PLAYER -> this.causingEntity instanceof LivingEntity && !(this.causingEntity instanceof Player);
         case ALWAYS -> true;
      };
   }

   public boolean isCreativePlayer() {
      return this.getEntity() instanceof Player player && player.getAbilities().instabuild;
   }

   public @Nullable Vec3 getSourcePosition() {
      if (this.damageSourcePosition != null) {
         return this.damageSourcePosition;
      } else {
         return this.directEntity != null ? this.directEntity.position() : null;
      }
   }

   public @Nullable Vec3 sourcePositionRaw() {
      return this.damageSourcePosition;
   }

   public boolean is(final TagKey<DamageType> tag) {
      return this.type.is(tag);
   }

   public boolean is(final ResourceKey<DamageType> typeKey) {
      return this.type.is(typeKey);
   }

   public DamageType type() {
      return this.type.value();
   }

   public Holder<DamageType> typeHolder() {
      return this.type;
   }
}
