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

   public SuspiciousStewEffects withEffectAdded(final SuspiciousStewEffects.Entry entry) {
      return new SuspiciousStewEffects(Util.copyAndAdd(this.effects, entry));
   }

   @Override
   public void onConsume(final Level level, final LivingEntity user, final ItemStack stack, final Consumable consumable) {
      for (SuspiciousStewEffects.Entry effect : this.effects) {
         user.addEffect(effect.createEffectInstance());
      }
   }

   @Override
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      if (flag.isCreative()) {
         List<MobEffectInstance> effectInstances = new ArrayList<>();

         for (SuspiciousStewEffects.Entry effect : this.effects) {
            effectInstances.add(effect.createEffectInstance());
         }

         PotionContents.addPotionTooltip(effectInstances, consumer, 1.0F, context.tickRate());
      }
   }

   public record Entry(Holder<MobEffect> effect, int duration) {
      public static final Codec<SuspiciousStewEffects.Entry> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               MobEffect.CODEC.fieldOf("id").forGetter(SuspiciousStewEffects.Entry::effect),
               Codec.INT.lenientOptionalFieldOf("duration", 160).forGetter(SuspiciousStewEffects.Entry::duration)
            )
            .apply(i, SuspiciousStewEffects.Entry::new)
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
