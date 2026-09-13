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
import net.minecraft.tags.TagKey;
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
   Optional<TagKey<DamageType>> bypassedBy,
   Optional<Holder<SoundEvent>> blockSound,
   Optional<Holder<SoundEvent>> disableSound
) {
   public static final Codec<BlocksAttacks> CODEC = RecordCodecBuilder.create(
      p_392703_ -> p_392703_.group(
            ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("block_delay_seconds", 0.0F).forGetter(BlocksAttacks::blockDelaySeconds),
            ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("disable_cooldown_scale", 1.0F).forGetter(BlocksAttacks::disableCooldownScale),
            BlocksAttacks.DamageReduction.CODEC
               .listOf()
               .optionalFieldOf("damage_reductions", List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)))
               .forGetter(BlocksAttacks::damageReductions),
            BlocksAttacks.ItemDamageFunction.CODEC
               .optionalFieldOf("item_damage", BlocksAttacks.ItemDamageFunction.DEFAULT)
               .forGetter(BlocksAttacks::itemDamage),
            TagKey.hashedCodec(Registries.DAMAGE_TYPE).optionalFieldOf("bypassed_by").forGetter(BlocksAttacks::bypassedBy),
            SoundEvent.CODEC.optionalFieldOf("block_sound").forGetter(BlocksAttacks::blockSound),
            SoundEvent.CODEC.optionalFieldOf("disabled_sound").forGetter(BlocksAttacks::disableSound)
         )
         .apply(p_392703_, BlocksAttacks::new)
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
      TagKey.streamCodec(Registries.DAMAGE_TYPE).apply(ByteBufCodecs::optional),
      BlocksAttacks::bypassedBy,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      BlocksAttacks::blockSound,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      BlocksAttacks::disableSound,
      BlocksAttacks::new
   );

   public void onBlocked(ServerLevel p_396721_, LivingEntity p_393154_) {
      this.blockSound
         .ifPresent(
            p_449815_ -> p_396721_.playSound(
               null,
               p_393154_.getX(),
               p_393154_.getY(),
               p_393154_.getZ(),
               (Holder<SoundEvent>)p_449815_,
               p_393154_.getSoundSource(),
               1.0F,
               0.8F + p_396721_.random.nextFloat() * 0.4F
            )
         );
   }

   public void disable(ServerLevel p_391618_, LivingEntity p_393822_, float p_391709_, ItemStack p_397839_) {
      int i = this.disableBlockingForTicks(p_391709_);
      if (i > 0) {
         if (p_393822_ instanceof Player player) {
            player.getCooldowns().addCooldown(p_397839_, i);
         }

         p_393822_.stopUsingItem();
         this.disableSound
            .ifPresent(
               p_449818_ -> p_391618_.playSound(
                  null,
                  p_393822_.getX(),
                  p_393822_.getY(),
                  p_393822_.getZ(),
                  (Holder<SoundEvent>)p_449818_,
                  p_393822_.getSoundSource(),
                  0.8F,
                  0.8F + p_391618_.random.nextFloat() * 0.4F
               )
            );
      }
   }

   public void hurtBlockingItem(Level p_397889_, ItemStack p_393334_, LivingEntity p_394513_, InteractionHand p_392331_, float p_394955_) {
      if (p_394513_ instanceof Player player) {
         if (!p_397889_.isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(p_393334_.getItem()));
         }

         int i = this.itemDamage.apply(p_394955_);
         if (i > 0) {
            p_393334_.hurtAndBreak(i, p_394513_, p_392331_.asEquipmentSlot());
         }
      }
   }

   private int disableBlockingForTicks(float p_391191_) {
      float f = p_391191_ * this.disableCooldownScale;
      return f > 0.0F ? Math.round(f * 20.0F) : 0;
   }

   public int blockDelayTicks() {
      return Math.round(this.blockDelaySeconds * 20.0F);
   }

   public float resolveBlockedDamage(DamageSource p_396469_, float p_394060_, double p_394412_) {
      float f = 0.0F;

      for (BlocksAttacks.DamageReduction blocksattacks$damagereduction : this.damageReductions) {
         f += blocksattacks$damagereduction.resolve(p_396469_, p_394060_, p_394412_);
      }

      return Mth.clamp(f, 0.0F, p_394060_);
   }

   public record DamageReduction(float horizontalBlockingAngle, Optional<HolderSet<DamageType>> type, float base, float factor) {
      public static final Codec<BlocksAttacks.DamageReduction> CODEC = RecordCodecBuilder.create(
         p_397513_ -> p_397513_.group(
               ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("horizontal_blocking_angle", 90.0F).forGetter(BlocksAttacks.DamageReduction::horizontalBlockingAngle),
               RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).optionalFieldOf("type").forGetter(BlocksAttacks.DamageReduction::type),
               Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacks.DamageReduction::base),
               Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacks.DamageReduction::factor)
            )
            .apply(p_397513_, BlocksAttacks.DamageReduction::new)
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

      public float resolve(DamageSource p_397046_, float p_392066_, double p_396817_) {
         if (p_396817_ > (float) (Math.PI / 180.0) * this.horizontalBlockingAngle) {
            return 0.0F;
         } else {
            return this.type.isPresent() && !this.type.get().contains(p_397046_.typeHolder())
               ? 0.0F
               : Mth.clamp(this.base + this.factor * p_392066_, 0.0F, p_392066_);
         }
      }
   }

   public record ItemDamageFunction(float threshold, float base, float factor) {
      public static final Codec<BlocksAttacks.ItemDamageFunction> CODEC = RecordCodecBuilder.create(
         p_397778_ -> p_397778_.group(
               ExtraCodecs.NON_NEGATIVE_FLOAT.fieldOf("threshold").forGetter(BlocksAttacks.ItemDamageFunction::threshold),
               Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacks.ItemDamageFunction::base),
               Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacks.ItemDamageFunction::factor)
            )
            .apply(p_397778_, BlocksAttacks.ItemDamageFunction::new)
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

      public int apply(float p_396384_) {
         return p_396384_ < this.threshold ? 0 : Mth.floor(this.base + this.factor * p_396384_);
      }
   }
}
