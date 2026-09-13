package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

public record SuspiciousStewEffects(List<SuspiciousStewEffects.Entry> effects) implements ConsumableListener, TooltipProvider {
   public static final SuspiciousStewEffects EMPTY = new SuspiciousStewEffects(List.of());
   public static final int DEFAULT_DURATION = 160;
   public static final Codec<SuspiciousStewEffects> CODEC = SuspiciousStewEffects.Entry.CODEC
      .listOf()
      .xmap(SuspiciousStewEffects::new, SuspiciousStewEffects::effects);
   public static final StreamCodec<RegistryFriendlyByteBuf, SuspiciousStewEffects> STREAM_CODEC = SuspiciousStewEffects.Entry.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(SuspiciousStewEffects::new, SuspiciousStewEffects::effects);

   public SuspiciousStewEffects withEffectAdded(SuspiciousStewEffects.Entry p_330002_) {
      return new SuspiciousStewEffects(Util.copyAndAdd(this.effects, p_330002_));
   }

   @Override
   public void onConsume(Level p_365472_, LivingEntity p_363953_, ItemStack p_360994_, Consumable p_366812_) {
      for (SuspiciousStewEffects.Entry suspicioussteweffects$entry : this.effects) {
         p_363953_.addEffect(suspicioussteweffects$entry.createEffectInstance());
      }
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_365951_, Consumer<Component> p_367764_, TooltipFlag p_362463_, DataComponentGetter p_395045_) {
      if (p_362463_.isCreative()) {
         List<MobEffectInstance> list = new ArrayList<>();

         for (SuspiciousStewEffects.Entry suspicioussteweffects$entry : this.effects) {
            list.add(suspicioussteweffects$entry.createEffectInstance());
         }

         PotionContents.addPotionTooltip(list, p_367764_, 1.0F, p_365951_.tickRate());
      }
   }

   public record Entry(Holder<MobEffect> effect, int duration) {
      public static final Codec<SuspiciousStewEffects.Entry> CODEC = RecordCodecBuilder.create(
         p_341579_ -> p_341579_.group(
               MobEffect.CODEC.fieldOf("id").forGetter(SuspiciousStewEffects.Entry::effect),
               Codec.INT.lenientOptionalFieldOf("duration", 160).forGetter(SuspiciousStewEffects.Entry::duration)
            )
            .apply(p_341579_, SuspiciousStewEffects.Entry::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SuspiciousStewEffects.Entry> STREAM_CODEC = StreamCodec.composite(
         MobEffect.STREAM_CODEC,
         SuspiciousStewEffects.Entry::effect,
         ByteBufCodecs.VAR_INT,
         SuspiciousStewEffects.Entry::duration,
         SuspiciousStewEffects.Entry::new
      );

      public MobEffectInstance createEffectInstance() {
         return new MobEffectInstance(this.effect, this.duration);
      }
   }
}
