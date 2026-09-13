package net.minecraft.world.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

public class SulfurCubeArchetypes {
   public static final ResourceKey<SulfurCubeArchetype> REGULAR = createKey(Identifier.withDefaultNamespace("regular"));
   public static final ResourceKey<SulfurCubeArchetype> BOUNCY = createKey(Identifier.withDefaultNamespace("bouncy"));
   public static final ResourceKey<SulfurCubeArchetype> SLOW_BOUNCY = createKey(Identifier.withDefaultNamespace("slow_bouncy"));
   public static final ResourceKey<SulfurCubeArchetype> SLOW_FLAT = createKey(Identifier.withDefaultNamespace("slow_flat"));
   public static final ResourceKey<SulfurCubeArchetype> FAST_FLAT = createKey(Identifier.withDefaultNamespace("fast_flat"));
   public static final ResourceKey<SulfurCubeArchetype> LIGHT = createKey(Identifier.withDefaultNamespace("light"));
   public static final ResourceKey<SulfurCubeArchetype> FAST_SLIDING = createKey(Identifier.withDefaultNamespace("fast_sliding"));
   public static final ResourceKey<SulfurCubeArchetype> SLOW_SLIDING = createKey(Identifier.withDefaultNamespace("slow_sliding"));
   public static final ResourceKey<SulfurCubeArchetype> HIGH_RESISTANCE = createKey(Identifier.withDefaultNamespace("high_resistance"));
   public static final ResourceKey<SulfurCubeArchetype> STICKY = createKey(Identifier.withDefaultNamespace("sticky"));
   public static final ResourceKey<SulfurCubeArchetype> EXPLOSIVE = createKey(Identifier.withDefaultNamespace("explosive"));
   public static final ResourceKey<SulfurCubeArchetype> HOT = createKey(Identifier.withDefaultNamespace("hot"));

   public static void bootstrap(final BootstrapContext<SulfurCubeArchetype> context) {
      register(
         context,
         REGULAR,
         ItemTags.SULFUR_CUBE_ARCHETYPE_REGULAR,
         archetype(1.0F, 0.5F, 0.3F, 0.1F),
         true,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.4125F, 0.09F),
         soundSettings(SoundEvents.SULFUR_CUBE_REGULAR_HIT, SoundEvents.SULFUR_CUBE_REGULAR_PUSH, 0.2F, 0.5F)
      );
      register(
         context,
         BOUNCY,
         ItemTags.SULFUR_CUBE_ARCHETYPE_BOUNCY,
         archetype(2.0F, 0.9F, 0.3F, 0.01F),
         true,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.4125F, 0.105F),
         soundSettings(SoundEvents.SULFUR_CUBE_BOUNCY_HIT, SoundEvents.SULFUR_CUBE_BOUNCY_PUSH, 0.3F, 0.7F)
      );
      register(
         context,
         SLOW_BOUNCY,
         ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_BOUNCY,
         archetype(-0.4F, 0.6F, 0.3F, 0.05F),
         false,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.4125F, 0.24F),
         soundSettings(SoundEvents.SULFUR_CUBE_SLOW_BOUNCY_HIT, SoundEvents.SULFUR_CUBE_SLOW_BOUNCY_PUSH, 0.05F, 0.5F)
      );
      register(
         context,
         SLOW_FLAT,
         ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_FLAT,
         archetype(-0.5F, 0.4F, 0.4F, 0.1F),
         false,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.4125F, 0.105F),
         soundSettings(SoundEvents.SULFUR_CUBE_SLOW_FLAT_HIT, SoundEvents.SULFUR_CUBE_SLOW_FLAT_PUSH, 0.03F, 0.9F)
      );
      register(
         context,
         FAST_FLAT,
         ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_FLAT,
         archetype(1.0F, 0.5F, 0.2F, 0.01F),
         false,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.9125F, 0.09F),
         soundSettings(SoundEvents.SULFUR_CUBE_FAST_FLAT_HIT, SoundEvents.SULFUR_CUBE_FAST_FLAT_PUSH, 0.03F, 0.9F)
      );
      register(
         context,
         LIGHT,
         ItemTags.SULFUR_CUBE_ARCHETYPE_LIGHT,
         archetype(1.0F, 1.0F, 0.3F, 1.8F),
         true,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.4125F, 0.18F),
         soundSettings(SoundEvents.SULFUR_CUBE_LIGHT_HIT, SoundEvents.SULFUR_CUBE_LIGHT_PUSH, 0.2F, 0.7F)
      );
      register(
         context,
         FAST_SLIDING,
         ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_SLIDING,
         archetype(-0.5F, 0.1F, 0.05F, 0.01F),
         false,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.6625F, 0.09F),
         soundSettings(SoundEvents.SULFUR_CUBE_FAST_SLIDING_HIT, SoundEvents.SULFUR_CUBE_FAST_SLIDING_PUSH, 0.05F, 1.0F)
      );
      register(
         context,
         SLOW_SLIDING,
         ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_SLIDING,
         archetype(-0.8F, 0.1F, 0.05F, 0.01F),
         false,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.4125F, 0.09F),
         soundSettings(SoundEvents.SULFUR_CUBE_SLOW_SLIDING_HIT, SoundEvents.SULFUR_CUBE_SLOW_SLIDING_PUSH, 0.02F, 1.0F)
      );
      register(
         context,
         STICKY,
         ItemTags.SULFUR_CUBE_ARCHETYPE_STICKY,
         archetype(2.0F, 0.0F, 2.0F, 0.01F),
         false,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.4125F, 0.09F),
         soundSettings(SoundEvents.SULFUR_CUBE_STICKY_HIT, SoundEvents.SULFUR_CUBE_STICKY_PUSH, 0.05F, 0.5F)
      );
      register(
         context,
         HIGH_RESISTANCE,
         ItemTags.SULFUR_CUBE_ARCHETYPE_HIGH_RESISTANCE,
         archetype(-0.7F, 0.2F, 1.0F, 0.01F),
         false,
         Optional.empty(),
         Optional.empty(),
         knockBackHitScale(0.4125F, 0.09F),
         soundSettings(SoundEvents.SULFUR_CUBE_HIGH_RESISTANCE_HIT, SoundEvents.SULFUR_CUBE_HIGH_RESISTANCE_PUSH, 0.03F, 0.7F)
      );
      register(
         context,
         EXPLOSIVE,
         ItemTags.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE,
         archetype(1.0F, 0.5F, 0.3F, 0.3F),
         true,
         Optional.of(new SulfurCubeArchetype.ExplosionData(3, false, 120)),
         Optional.empty(),
         knockBackHitScale(0.4125F, 0.09F),
         soundSettings(SoundEvents.SULFUR_CUBE_EXPLOSIVE_HIT, SoundEvents.SULFUR_CUBE_EXPLOSIVE_PUSH, 0.1F, 0.7F)
      );
      register(
         context,
         HOT,
         ItemTags.SULFUR_CUBE_ARCHETYPE_HOT,
         archetype(1.0F, 0.5F, 0.3F, 0.1F),
         true,
         Optional.empty(),
         Optional.of(contactDamage(context, DamageTypes.SULFUR_CUBE_HOT, ConstantFloat.of(1.0F), false)),
         knockBackHitScale(0.4125F, 0.09F),
         soundSettings(SoundEvents.SULFUR_CUBE_HOT_HIT, SoundEvents.SULFUR_CUBE_HOT_PUSH, 0.2F, 0.7F)
      );
   }

   private static ResourceKey<SulfurCubeArchetype> createKey(final Identifier id) {
      return ResourceKey.create(Registries.SULFUR_CUBE_ARCHETYPE, id);
   }

   private static Function<ResourceKey<SulfurCubeArchetype>, SulfurCubeArchetype.AttributeEntry> add(final Holder<Attribute> attribute, final double amount) {
      return key -> SulfurCubeArchetype.AttributeEntry.add(attribute, amount, key);
   }

   private static Function<ResourceKey<SulfurCubeArchetype>, SulfurCubeArchetype.AttributeEntry> multiply(
      final Holder<Attribute> attribute, final double amount
   ) {
      return key -> SulfurCubeArchetype.AttributeEntry.multiply(attribute, amount, key);
   }

   private static List<Function<ResourceKey<SulfurCubeArchetype>, SulfurCubeArchetype.AttributeEntry>> archetype(
      final float speed, final float bounce, final float friction, final float drag
   ) {
      return List.of(
         add(Attributes.KNOCKBACK_RESISTANCE, -speed),
         add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, -speed),
         add(Attributes.BOUNCINESS, bounce),
         multiply(Attributes.FRICTION_MODIFIER, friction),
         multiply(Attributes.AIR_DRAG_MODIFIER, drag)
      );
   }

   private static SulfurCubeArchetype.ContactDamage contactDamage(
      final BootstrapContext<SulfurCubeArchetype> context,
      final ResourceKey<DamageType> damageType,
      final FloatProvider amount,
      final boolean attributeToSource
   ) {
      return new SulfurCubeArchetype.ContactDamage(context.lookup(Registries.DAMAGE_TYPE).getOrThrow(damageType), amount, attributeToSource);
   }

   private static SulfurCubeArchetype.KnockbackModifiers knockBackHitScale(final float horizontalPower, final float verticalPower) {
      return new SulfurCubeArchetype.KnockbackModifiers(horizontalPower, verticalPower);
   }

   private static SulfurCubeArchetype.SoundSettings soundSettings(
      final Holder<SoundEvent> hitSound, final Holder<SoundEvent> pushSound, final float threshold, final float cooldown
   ) {
      return new SulfurCubeArchetype.SoundSettings(hitSound, pushSound, threshold, cooldown);
   }

   private static void register(
      final BootstrapContext<SulfurCubeArchetype> context,
      final ResourceKey<SulfurCubeArchetype> name,
      final TagKey<Item> blocks,
      final List<Function<ResourceKey<SulfurCubeArchetype>, SulfurCubeArchetype.AttributeEntry>> modifiers,
      final boolean floats,
      final Optional<SulfurCubeArchetype.ExplosionData> maxFuse,
      final Optional<SulfurCubeArchetype.ContactDamage> contactDamage,
      final SulfurCubeArchetype.KnockbackModifiers knockbackModifiers,
      final SulfurCubeArchetype.SoundSettings soundSettings
   ) {
      context.register(
         name,
         new SulfurCubeArchetype(
            context.lookup(Registries.ITEM).getOrThrow(blocks),
            modifiers.stream().map(f -> f.apply(name)).toList(),
            floats,
            maxFuse,
            contactDamage,
            knockbackModifiers,
            soundSettings
         )
      );
   }
}
