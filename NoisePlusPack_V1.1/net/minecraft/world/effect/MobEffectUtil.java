package net.minecraft.world.effect;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class MobEffectUtil {
   public static Component formatDuration(MobEffectInstance p_268116_, float p_268280_, float p_310568_) {
      if (p_268116_.isInfiniteDuration()) {
         return Component.translatable("effect.duration.infinite");
      }

      int i = Mth.floor(p_268116_.getDuration() * p_268280_);
      return Component.literal(StringUtil.formatTickDuration(i, p_310568_));
   }

   public static boolean hasDigSpeed(LivingEntity p_19585_) {
      return p_19585_.hasEffect(MobEffects.HASTE) || p_19585_.hasEffect(MobEffects.CONDUIT_POWER);
   }

   public static int getDigSpeedAmplification(LivingEntity p_19587_) {
      int i = 0;
      int j = 0;
      if (p_19587_.hasEffect(MobEffects.HASTE)) {
         i = p_19587_.getEffect(MobEffects.HASTE).getAmplifier();
      }

      if (p_19587_.hasEffect(MobEffects.CONDUIT_POWER)) {
         j = p_19587_.getEffect(MobEffects.CONDUIT_POWER).getAmplifier();
      }

      return Math.max(i, j);
   }

   public static boolean hasWaterBreathing(LivingEntity p_19589_) {
      return p_19589_.hasEffect(MobEffects.WATER_BREATHING)
         || p_19589_.hasEffect(MobEffects.CONDUIT_POWER)
         || p_19589_.hasEffect(MobEffects.BREATH_OF_THE_NAUTILUS);
   }

   public static boolean shouldEffectsRefillAirsupply(LivingEntity p_453469_) {
      return !p_453469_.hasEffect(MobEffects.BREATH_OF_THE_NAUTILUS)
         || p_453469_.hasEffect(MobEffects.WATER_BREATHING)
         || p_453469_.hasEffect(MobEffects.CONDUIT_POWER);
   }

   public static List<ServerPlayer> addEffectToPlayersAround(
      ServerLevel p_216947_, @Nullable Entity p_216948_, Vec3 p_216949_, double p_216950_, MobEffectInstance p_216951_, int p_216952_
   ) {
      Holder<MobEffect> holder = p_216951_.getEffect();
      List<ServerPlayer> list = p_216947_.getPlayers(
         p_267925_ -> p_267925_.gameMode.isSurvival()
            && (p_216948_ == null || !p_216948_.isAlliedTo(p_267925_))
            && p_216949_.closerThan(p_267925_.position(), p_216950_)
            && (
               !p_267925_.hasEffect(holder)
                  || p_267925_.getEffect(holder).getAmplifier() < p_216951_.getAmplifier()
                  || p_267925_.getEffect(holder).endsWithin(p_216952_ - 1)
            )
      );
      list.forEach(p_238232_ -> p_238232_.addEffect(new MobEffectInstance(p_216951_), p_216948_));
      return list;
   }
}
