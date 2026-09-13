package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;

public record PiercingWeapon(boolean dealsKnockback, boolean dismounts, Optional<Holder<SoundEvent>> sound, Optional<Holder<SoundEvent>> hitSound) {
   public static final Codec<PiercingWeapon> CODEC = RecordCodecBuilder.create(
      p_460493_ -> p_460493_.group(
            Codec.BOOL.optionalFieldOf("deals_knockback", true).forGetter(PiercingWeapon::dealsKnockback),
            Codec.BOOL.optionalFieldOf("dismounts", false).forGetter(PiercingWeapon::dismounts),
            SoundEvent.CODEC.optionalFieldOf("sound").forGetter(PiercingWeapon::sound),
            SoundEvent.CODEC.optionalFieldOf("hit_sound").forGetter(PiercingWeapon::hitSound)
         )
         .apply(p_460493_, PiercingWeapon::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PiercingWeapon> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      PiercingWeapon::dealsKnockback,
      ByteBufCodecs.BOOL,
      PiercingWeapon::dismounts,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PiercingWeapon::sound,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PiercingWeapon::hitSound,
      PiercingWeapon::new
   );

   public void makeSound(Entity p_453062_) {
      this.sound
         .ifPresent(
            p_461004_ -> p_453062_.level()
               .playSound(
                  p_453062_, p_453062_.getX(), p_453062_.getY(), p_453062_.getZ(), (Holder<SoundEvent>)p_461004_, p_453062_.getSoundSource(), 1.0F, 1.0F
               )
         );
   }

   public void makeHitSound(Entity p_460834_) {
      this.hitSound
         .ifPresent(
            p_455394_ -> p_460834_.level()
               .playSound(null, p_460834_.getX(), p_460834_.getY(), p_460834_.getZ(), (Holder<SoundEvent>)p_455394_, p_460834_.getSoundSource(), 1.0F, 1.0F)
         );
   }

   public static boolean canHitEntity(Entity p_452642_, Entity p_454622_) {
      if (p_454622_.isInvulnerable() || !p_454622_.isAlive()) {
         return false;
      } else if (p_454622_ instanceof Interaction) {
         return true;
      } else if (!p_454622_.canBeHitByProjectile()) {
         return false;
      } else {
         return p_454622_ instanceof Player player && p_452642_ instanceof Player player1 && !player1.canHarmPlayer(player)
            ? false
            : !p_452642_.isPassengerOfSameVehicle(p_454622_);
      }
   }

   public void attack(LivingEntity p_459872_, EquipmentSlot p_457940_) {
      float f = (float)p_459872_.getAttributeValue(Attributes.ATTACK_DAMAGE);
      AttackRange attackrange = p_459872_.entityAttackRange();
      boolean flag = false;

      for (EntityHitResult entityhitresult : (Collection)ProjectileUtil.getHitEntitiesAlong(
            p_459872_, attackrange, p_453749_ -> canHitEntity(p_459872_, p_453749_), ClipContext.Block.COLLIDER
         )
         .map(p_452187_ -> List.of(), p_450514_ -> p_450514_)) {
         flag |= p_459872_.stabAttack(p_457940_, entityhitresult.getEntity(), f, true, this.dealsKnockback, this.dismounts);
      }

      p_459872_.onAttack();
      p_459872_.lungeForwardMaybe();
      if (flag) {
         this.makeHitSound(p_459872_);
      }

      this.makeSound(p_459872_);
      p_459872_.swing(InteractionHand.MAIN_HAND, false);
   }
}
