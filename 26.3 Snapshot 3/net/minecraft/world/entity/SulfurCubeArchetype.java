package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

public record SulfurCubeArchetype(
   HolderSet<Item> items,
   List<SulfurCubeArchetype.AttributeEntry> attributeModifiers,
   boolean buoyant,
   Optional<SulfurCubeArchetype.ExplosionData> explosion,
   Optional<SulfurCubeArchetype.ContactDamage> contactDamage,
   SulfurCubeArchetype.KnockbackModifiers knockbackModifiers,
   SulfurCubeArchetype.SoundSettings soundSettings
) {
   public static final Codec<SulfurCubeArchetype> DIRECT_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(SulfurCubeArchetype::items),
            SulfurCubeArchetype.AttributeEntry.CODEC.listOf().fieldOf("attribute_modifiers").forGetter(SulfurCubeArchetype::attributeModifiers),
            Codec.BOOL.optionalFieldOf("buoyant", false).forGetter(SulfurCubeArchetype::buoyant),
            SulfurCubeArchetype.ExplosionData.CODEC.optionalFieldOf("explosion").forGetter(SulfurCubeArchetype::explosion),
            SulfurCubeArchetype.ContactDamage.CODEC.optionalFieldOf("contact_damage").forGetter(SulfurCubeArchetype::contactDamage),
            SulfurCubeArchetype.KnockbackModifiers.CODEC.fieldOf("knockback_modifiers").forGetter(SulfurCubeArchetype::knockbackModifiers),
            SulfurCubeArchetype.SoundSettings.CODEC.fieldOf("sound_settings").forGetter(SulfurCubeArchetype::soundSettings)
         )
         .apply(i, SulfurCubeArchetype::new)
   );
   public static SulfurCubeArchetype.KnockbackModifiers DEFAULT_KNOCKBACK_MODIFIERS = new SulfurCubeArchetype.KnockbackModifiers(0.33F, 0.06F);
   public static SulfurCubeArchetype.SoundSettings DEFAULT_SOUND_SETTINGS = new SulfurCubeArchetype.SoundSettings(
      SoundEvents.SULFUR_CUBE_REGULAR_HIT, SoundEvents.SULFUR_CUBE_REGULAR_PUSH, 0.2F, 0.5F
   );

   public record AttributeEntry(Holder<Attribute> attribute, AttributeModifier modifier) {
      public static final Codec<SulfurCubeArchetype.AttributeEntry> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Attribute.CODEC.fieldOf("attribute").forGetter(SulfurCubeArchetype.AttributeEntry::attribute),
               AttributeModifier.MAP_CODEC.forGetter(SulfurCubeArchetype.AttributeEntry::modifier)
            )
            .apply(i, SulfurCubeArchetype.AttributeEntry::new)
      );

      public static SulfurCubeArchetype.AttributeEntry add(
         final Holder<Attribute> attribute, final double amount, final ResourceKey<SulfurCubeArchetype> archetype
      ) {
         return new SulfurCubeArchetype.AttributeEntry(
            attribute,
            new AttributeModifier(
               Identifier.withDefaultNamespace(archetype.identifier().getPath() + "_add_" + attribute.unwrapKey().get().identifier().getPath()),
               amount,
               AttributeModifier.Operation.ADD_VALUE
            )
         );
      }

      public static SulfurCubeArchetype.AttributeEntry multiply(
         final Holder<Attribute> attribute, final double amount, final ResourceKey<SulfurCubeArchetype> archetype
      ) {
         return new SulfurCubeArchetype.AttributeEntry(
            attribute,
            new AttributeModifier(
               Identifier.withDefaultNamespace(archetype.identifier().getPath() + "_mul_" + attribute.unwrapKey().get().identifier().getPath()),
               amount - 1.0,
               AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            )
         );
      }
   }

   public record ContactDamage(Holder<DamageType> damageType, FloatProvider amount, boolean attributeToSource) {
      public static final Codec<SulfurCubeArchetype.ContactDamage> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               DamageType.CODEC.fieldOf("damage_type").forGetter(SulfurCubeArchetype.ContactDamage::damageType),
               FloatProviders.codec(0.0F).fieldOf("amount").forGetter(SulfurCubeArchetype.ContactDamage::amount),
               Codec.BOOL.fieldOf("attribute_to_source").forGetter(SulfurCubeArchetype.ContactDamage::attributeToSource)
            )
            .apply(i, SulfurCubeArchetype.ContactDamage::new)
      );
   }

   public record ExplosionData(int power, boolean causesFire, int fuse) {
      public static final Codec<SulfurCubeArchetype.ExplosionData> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("power").forGetter(SulfurCubeArchetype.ExplosionData::power),
               Codec.BOOL.fieldOf("causes_fire").forGetter(SulfurCubeArchetype.ExplosionData::causesFire),
               ExtraCodecs.POSITIVE_INT.fieldOf("fuse").forGetter(SulfurCubeArchetype.ExplosionData::fuse)
            )
            .apply(i, SulfurCubeArchetype.ExplosionData::new)
      );
   }

   public record KnockbackModifiers(float horizontalPower, float verticalPower) {
      public static final Codec<SulfurCubeArchetype.KnockbackModifiers> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.FLOAT.fieldOf("horizontal_power").forGetter(SulfurCubeArchetype.KnockbackModifiers::horizontalPower),
               Codec.FLOAT.fieldOf("vertical_power").forGetter(SulfurCubeArchetype.KnockbackModifiers::verticalPower)
            )
            .apply(i, SulfurCubeArchetype.KnockbackModifiers::new)
      );
   }

   public record SoundSettings(Holder<SoundEvent> hitSound, Holder<SoundEvent> pushSound, float pushSoundImpulseThreshold, float pushSoundCooldown) {
      public static final Codec<SulfurCubeArchetype.SoundSettings> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               SoundEvent.CODEC.fieldOf("hit_sound").forGetter(SulfurCubeArchetype.SoundSettings::hitSound),
               SoundEvent.CODEC.fieldOf("push_sound").forGetter(SulfurCubeArchetype.SoundSettings::pushSound),
               Codec.FLOAT.fieldOf("push_sound_impulse_threshold").forGetter(SulfurCubeArchetype.SoundSettings::pushSoundImpulseThreshold),
               Codec.FLOAT.fieldOf("push_sound_cooldown").forGetter(SulfurCubeArchetype.SoundSettings::pushSoundCooldown)
            )
            .apply(i, SulfurCubeArchetype.SoundSettings::new)
      );
   }
}
