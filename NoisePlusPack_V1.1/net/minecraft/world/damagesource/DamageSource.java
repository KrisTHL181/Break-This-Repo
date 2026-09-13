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

   private DamageSource(Holder<DamageType> p_270906_, @Nullable Entity p_270796_, @Nullable Entity p_270459_, @Nullable Vec3 p_270623_) {
      this.type = p_270906_;
      this.causingEntity = p_270459_;
      this.directEntity = p_270796_;
      this.damageSourcePosition = p_270623_;
   }

   public DamageSource(Holder<DamageType> p_270818_, @Nullable Entity p_270162_, @Nullable Entity p_270115_) {
      this(p_270818_, p_270162_, p_270115_, null);
   }

   public DamageSource(Holder<DamageType> p_270690_, Vec3 p_270579_) {
      this(p_270690_, null, null, p_270579_);
   }

   public DamageSource(Holder<DamageType> p_270811_, @Nullable Entity p_270660_) {
      this(p_270811_, p_270660_, p_270660_);
   }

   public DamageSource(Holder<DamageType> p_270475_) {
      this(p_270475_, null, null, null);
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

   public Component getLocalizedDeathMessage(LivingEntity p_19343_) {
      String s = "death.attack." + this.type().msgId();
      if (this.causingEntity == null && this.directEntity == null) {
         LivingEntity livingentity1 = p_19343_.getKillCredit();
         String s1 = s + ".player";
         return livingentity1 != null
            ? Component.translatable(s1, p_19343_.getDisplayName(), livingentity1.getDisplayName())
            : Component.translatable(s, p_19343_.getDisplayName());
      } else {
         Component component = this.causingEntity == null ? this.directEntity.getDisplayName() : this.causingEntity.getDisplayName();
         ItemStack itemstack = this.causingEntity instanceof LivingEntity livingentity ? livingentity.getMainHandItem() : ItemStack.EMPTY;
         return !itemstack.isEmpty() && itemstack.has(DataComponents.CUSTOM_NAME)
            ? Component.translatable(s + ".item", p_19343_.getDisplayName(), component, itemstack.getDisplayName())
            : Component.translatable(s, p_19343_.getDisplayName(), component);
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

   public boolean is(TagKey<DamageType> p_270890_) {
      return this.type.is(p_270890_);
   }

   public boolean is(ResourceKey<DamageType> p_276108_) {
      return this.type.is(p_276108_);
   }

   public DamageType type() {
      return this.type.value();
   }

   public Holder<DamageType> typeHolder() {
      return this.type;
   }
}
