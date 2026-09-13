package net.minecraft.world.entity.ai.attributes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public class AttributeInstance {
   private final Holder<Attribute> attribute;
   private final Map<AttributeModifier.Operation, Map<Identifier, AttributeModifier>> modifiersByOperation = Maps.newEnumMap(AttributeModifier.Operation.class);
   private final Map<Identifier, AttributeModifier> modifierById = new Object2ObjectArrayMap();
   private final Map<Identifier, AttributeModifier> permanentModifiers = new Object2ObjectArrayMap();
   private double baseValue;
   private boolean dirty = true;
   private double cachedValue;
   private final Consumer<AttributeInstance> onDirty;

   public AttributeInstance(final Holder<Attribute> attribute, final Consumer<AttributeInstance> onDirty) {
      this.attribute = attribute;
      this.onDirty = onDirty;
      this.baseValue = attribute.value().getDefaultValue();
   }

   public Holder<Attribute> getAttribute() {
      return this.attribute;
   }

   public double getBaseValue() {
      return this.baseValue;
   }

   public void setBaseValue(final double baseValue) {
      if (baseValue != this.baseValue) {
         this.baseValue = baseValue;
         this.setDirty();
      }
   }

   @VisibleForTesting
   Map<Identifier, AttributeModifier> getModifiers(final AttributeModifier.Operation operation) {
      return this.modifiersByOperation.computeIfAbsent(operation, key -> new Object2ObjectOpenHashMap());
   }

   public Set<AttributeModifier> getModifiers() {
      return ImmutableSet.copyOf(this.modifierById.values());
   }

   public Set<AttributeModifier> getPermanentModifiers() {
      return ImmutableSet.copyOf(this.permanentModifiers.values());
   }

   public @Nullable AttributeModifier getModifier(final Identifier id) {
      return this.modifierById.get(id);
   }

   public boolean hasModifier(final Identifier modifier) {
      return this.modifierById.get(modifier) != null;
   }

   private void addModifier(final AttributeModifier modifier) {
      AttributeModifier previous = this.modifierById.putIfAbsent(modifier.id(), modifier);
      if (previous != null) {
         throw new IllegalArgumentException("Modifier is already applied on this attribute!");
      }

      this.getModifiers(modifier.operation()).put(modifier.id(), modifier);
      this.setDirty();
   }

   public void addOrUpdateTransientModifier(final AttributeModifier modifier) {
      AttributeModifier oldModifier = this.modifierById.put(modifier.id(), modifier);
      if (modifier != oldModifier) {
         this.getModifiers(modifier.operation()).put(modifier.id(), modifier);
         this.setDirty();
      }
   }

   public void addTransientModifier(final AttributeModifier modifier) {
      this.addModifier(modifier);
   }

   public void addOrReplacePermanentModifier(final AttributeModifier modifier) {
      this.removeModifier(modifier.id());
      this.addModifier(modifier);
      this.permanentModifiers.put(modifier.id(), modifier);
   }

   public void addPermanentModifier(final AttributeModifier modifier) {
      this.addModifier(modifier);
      this.permanentModifiers.put(modifier.id(), modifier);
   }

   public void addPermanentModifiers(final Collection<AttributeModifier> modifiers) {
      for (AttributeModifier modifier : modifiers) {
         this.addPermanentModifier(modifier);
      }
   }

   protected void setDirty() {
      this.dirty = true;
      this.onDirty.accept(this);
   }

   public void removeModifier(final AttributeModifier modifier) {
      this.removeModifier(modifier.id());
   }

   public boolean removeModifier(final Identifier id) {
      AttributeModifier modifier = this.modifierById.remove(id);
      if (modifier == null) {
         return false;
      }

      this.getModifiers(modifier.operation()).remove(id);
      this.permanentModifiers.remove(id);
      this.setDirty();
      return true;
   }

   public void removeModifiers() {
      for (AttributeModifier modifier : this.getModifiers()) {
         this.removeModifier(modifier);
      }
   }

   public double getValue() {
      if (this.dirty) {
         this.cachedValue = this.calculateValue();
         this.dirty = false;
      }

      return this.cachedValue;
   }

   private double calculateValue() {
      double base = this.getBaseValue();

      for (AttributeModifier modifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_VALUE)) {
         base += modifier.amount();
      }

      double result = base;

      for (AttributeModifier modifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
         result += base * modifier.amount();
      }

      for (AttributeModifier modifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
         result *= 1.0 + modifier.amount();
      }

      return this.attribute.value().sanitizeValue(result);
   }

   private Collection<AttributeModifier> getModifiersOrEmpty(final AttributeModifier.Operation operation) {
      return this.modifiersByOperation.getOrDefault(operation, Map.of()).values();
   }

   public void replaceFrom(final AttributeInstance other) {
      this.baseValue = other.baseValue;
      this.modifierById.clear();
      this.modifierById.putAll(other.modifierById);
      this.permanentModifiers.clear();
      this.permanentModifiers.putAll(other.permanentModifiers);
      this.modifiersByOperation.clear();
      other.modifiersByOperation
         .forEach(
            (operation, attributeModifiers) -> this.getModifiers(operation).putAll((Map<? extends Identifier, ? extends AttributeModifier>)attributeModifiers)
         );
      this.setDirty();
   }

   public AttributeInstance.Packed pack() {
      return new AttributeInstance.Packed(this.attribute, this.baseValue, List.copyOf(this.permanentModifiers.values()));
   }

   public void apply(final AttributeInstance.Packed packed) {
      this.baseValue = packed.baseValue;

      for (AttributeModifier modifier : packed.modifiers) {
         this.modifierById.put(modifier.id(), modifier);
         this.getModifiers(modifier.operation()).put(modifier.id(), modifier);
         this.permanentModifiers.put(modifier.id(), modifier);
      }

      this.setDirty();
   }

   public record Packed(Holder<Attribute> attribute, double baseValue, List<AttributeModifier> modifiers) {
      public static final Codec<AttributeInstance.Packed> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("id").forGetter(AttributeInstance.Packed::attribute),
               ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.DOUBLE, "base", 0.0).forGetter(AttributeInstance.Packed::baseValue),
               AttributeModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(AttributeInstance.Packed::modifiers)
            )
            .apply(i, AttributeInstance.Packed::new)
      );
      public static final Codec<List<AttributeInstance.Packed>> LIST_CODEC = CODEC.listOf();
   }
}
