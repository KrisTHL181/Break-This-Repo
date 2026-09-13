package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record BlocksAttacks(
   float blockDelaySeconds,
   float disableCooldownScale,
   List<BlocksAttacks.DamageReduction> damageReductions,
   BlocksAttacks.ItemDamageFunction itemDamage,
   Optional<HolderSet<DamageType>> bypassedBy,
   Optional<Holder<SoundEvent>> blockSound,
   Optional<Holder<SoundEvent>> disableSound
) {
   public static final Codec<BlocksAttacks> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("block_delay_seconds", 0.0F).forGetter(BlocksAttacks::blockDelaySeconds),
            ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("disable_cooldown_scale", 1.0F).forGetter(BlocksAttacks::disableCooldownScale),
            BlocksAttacks.DamageReduction.CODEC
               .listOf()
               .optionalFieldOf("damage_reductions", List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)))
               .forGetter(BlocksAttacks::damageReductions),
            BlocksAttacks.ItemDamageFunction.CODEC
               .optionalFieldOf("item_damage", BlocksAttacks.ItemDamageFunction.DEFAULT)
               .forGetter(BlocksAttacks::itemDamage),
            RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).optionalFieldOf("bypassed_by").forGetter(BlocksAttacks::bypassedBy),
            SoundEvent.CODEC.optionalFieldOf("block_sound").forGetter(BlocksAttacks::blockSound),
            SoundEvent.CODEC.optionalFieldOf("disabled_sound").forGetter(BlocksAttacks::disableSound)
         )
         .apply(i, BlocksAttacks::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, BlocksAttacks> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      BlocksAttacks::blockDelaySeconds,
      ByteBufCodecs.FLOAT,
      BlocksAttacks::disableCooldownScale,
      BlocksAttacks.DamageReduction.STREAM_CODEC.apply(ByteBufCodecs.list()),
      BlocksAttacks::damageReductions,
      BlocksAttacks.ItemDamageFunction.STREAM_CODEC,
      BlocksAttacks::itemDamage,
      ByteBufCodecs.holderSet(Registries.DAMAGE_TYPE).apply(ByteBufCodecs::optional),
      BlocksAttacks::bypassedBy,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      BlocksAttacks::blockSound,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      BlocksAttacks::disableSound,
      BlocksAttacks::new
   );

   public void onBlocked(final ServerLevel level, final LivingEntity user) {
      this.blockSound
         .ifPresent(
            sound -> level.playSound(
               null, user.getX(), user.getY(), user.getZ(), (Holder<SoundEvent>)sound, user.getSoundSource(), 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F
            )
         );
   }

   public void disable(final ServerLevel level, final LivingEntity user, final float baseSeconds, final ItemStack blockingWith) {
      int cooldownTicks = this.disableBlockingForTicks(baseSeconds);
      if (cooldownTicks > 0) {
         if (user instanceof Player player) {
            player.getCooldowns().addCooldown(blockingWith, cooldownTicks);
         }

         user.stopUsingItem();
         this.disableSound
            .ifPresent(
               sound -> level.playSound(
                  null,
                  user.getX(),
                  user.getY(),
                  user.getZ(),
                  (Holder<SoundEvent>)sound,
                  user.getSoundSource(),
                  0.8F,
                  0.8F + level.getRandom().nextFloat() * 0.4F
               )
            );
      }
   }

   public void hurtBlockingItem(final Level level, final ItemStack item, final LivingEntity user, final InteractionHand hand, final float damage) {
      if (user instanceof Player player) {
         if (!level.isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(item.getItem()));
         }

         int itemDamage = this.itemDamage.apply(damage);
         if (itemDamage > 0) {
            item.hurtAndBreak(itemDamage, user, hand.asEquipmentSlot());
         }
      }
   }

   private int disableBlockingForTicks(final float baseSeconds) {
      float seconds = baseSeconds * this.disableCooldownScale;
      return seconds > 0.0F ? Math.round(seconds * 20.0F) : 0;
   }

   public int blockDelayTicks() {
      return Math.round(this.blockDelaySeconds * 20.0F);
   }

   public float resolveBlockedDamage(final DamageSource source, final float dealtDamage, final double angle) {
      float blockedDamage = 0.0F;

      for (BlocksAttacks.DamageReduction reduction : this.damageReductions) {
         blockedDamage += reduction.resolve(source, dealtDamage, angle);
      }

      return Mth.clamp(blockedDamage, 0.0F, dealtDamage);
   }

   public record DamageReduction(float horizontalBlockingAngle, Optional<HolderSet<DamageType>> type, float base, float factor) {
      public static final Codec<BlocksAttacks.DamageReduction> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("horizontal_blocking_angle", 90.0F).forGetter(BlocksAttacks.DamageReduction::horizontalBlockingAngle),
               RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).optionalFieldOf("type").forGetter(BlocksAttacks.DamageReduction::type),
               Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacks.DamageReduction::base),
               Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacks.DamageReduction::factor)
            )
            .apply(i, BlocksAttacks.DamageReduction::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, BlocksAttacks.DamageReduction> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.FLOAT,
         BlocksAttacks.DamageReduction::horizontalBlockingAngle,
         ByteBufCodecs.holderSet(Registries.DAMAGE_TYPE).apply(ByteBufCodecs::optional),
         BlocksAttacks.DamageReduction::type,
         ByteBufCodecs.FLOAT,
         BlocksAttacks.DamageReduction::base,
         ByteBufCodecs.FLOAT,
         BlocksAttacks.DamageReduction::factor,
         BlocksAttacks.DamageReduction::new
      );

      public float resolve(final DamageSource source, final float dealtDamage, final double angle) {
         if (angle > (float) (Math.PI / 180.0) * this.horizontalBlockingAngle) {
            return 0.0F;
         } else {
            return this.type.isPresent() && !this.type.get().contains(source.typeHolder())
               ? 0.0F
               : Mth.clamp(this.base + this.factor * dealtDamage, 0.0F, dealtDamage);
         }
      }
   }

   public record ItemDamageFunction(float threshold, float base, float factor) {
      public static final Codec<BlocksAttacks.ItemDamageFunction> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               ExtraCodecs.NON_NEGATIVE_FLOAT.fieldOf("threshold").forGetter(BlocksAttacks.ItemDamageFunction::threshold),
               Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacks.ItemDamageFunction::base),
               Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacks.ItemDamageFunction::factor)
            )
            .apply(i, BlocksAttacks.ItemDamageFunction::new)
      );
      public static final StreamCodec<ByteBuf, BlocksAttacks.ItemDamageFunction> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.FLOAT,
         BlocksAttacks.ItemDamageFunction::threshold,
         ByteBufCodecs.FLOAT,
         BlocksAttacks.ItemDamageFunction::base,
         ByteBufCodecs.FLOAT,
         BlocksAttacks.ItemDamageFunction::factor,
         BlocksAttacks.ItemDamageFunction::new
      );
      public static final BlocksAttacks.ItemDamageFunction DEFAULT = new BlocksAttacks.ItemDamageFunction(1.0F, 0.0F, 1.0F);

      public int apply(final float dealtDamage) {
         return dealtDamage < this.threshold ? 0 : Mth.floor(this.base + this.factor * dealtDamage);
      }
   }
}
