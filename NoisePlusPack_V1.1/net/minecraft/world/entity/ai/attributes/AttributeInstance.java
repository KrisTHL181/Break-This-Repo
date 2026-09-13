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

   public AttributeInstance(Holder<Attribute> p_335359_, Consumer<AttributeInstance> p_22098_) {
      this.attribute = p_335359_;
      this.onDirty = p_22098_;
      this.baseValue = p_335359_.value().getDefaultValue();
   }

   public Holder<Attribute> getAttribute() {
      return this.attribute;
   }

   public double getBaseValue() {
      return this.baseValue;
   }

   public void setBaseValue(double p_22101_) {
      if (p_22101_ != this.baseValue) {
         this.baseValue = p_22101_;
         this.setDirty();
      }
   }

   @VisibleForTesting
   Map<Identifier, AttributeModifier> getModifiers(AttributeModifier.Operation p_22105_) {
      return this.modifiersByOperation.computeIfAbsent(p_22105_, p_326790_ -> new Object2ObjectOpenHashMap());
   }

   public Set<AttributeModifier> getModifiers() {
      return ImmutableSet.copyOf(this.modifierById.values());
   }

   public Set<AttributeModifier> getPermanentModifiers() {
      return ImmutableSet.copyOf(this.permanentModifiers.values());
   }

   public @Nullable AttributeModifier getModifier(Identifier p_455576_) {
      return this.modifierById.get(p_455576_);
   }

   public boolean hasModifier(Identifier p_459334_) {
      return this.modifierById.get(p_459334_) != null;
   }

   private void addModifier(AttributeModifier p_22134_) {
      AttributeModifier attributemodifier = this.modifierById.putIfAbsent(p_22134_.id(), p_22134_);
      if (attributemodifier != null) {
         throw new IllegalArgumentException("Modifier is already applied on this attribute!");
      }

      this.getModifiers(p_22134_.operation()).put(p_22134_.id(), p_22134_);
      this.setDirty();
   }

   public void addOrUpdateTransientModifier(AttributeModifier p_327789_) {
      AttributeModifier attributemodifier = this.modifierById.put(p_327789_.id(), p_327789_);
      if (p_327789_ != attributemodifier) {
         this.getModifiers(p_327789_.operation()).put(p_327789_.id(), p_327789_);
         this.setDirty();
      }
   }

   public void addTransientModifier(AttributeModifier p_22119_) {
      this.addModifier(p_22119_);
   }

   public void addOrReplacePermanentModifier(AttributeModifier p_343885_) {
      this.removeModifier(p_343885_.id());
      this.addModifier(p_343885_);
      this.permanentModifiers.put(p_343885_.id(), p_343885_);
   }

   public void addPermanentModifier(AttributeModifier p_22126_) {
      this.addModifier(p_22126_);
      this.permanentModifiers.put(p_22126_.id(), p_22126_);
   }

   public void addPermanentModifiers(Collection<AttributeModifier> p_366375_) {
      for (AttributeModifier attributemodifier : p_366375_) {
         this.addPermanentModifier(attributemodifier);
      }
   }

   protected void setDirty() {
      this.dirty = true;
      this.onDirty.accept(this);
   }

   public void removeModifier(AttributeModifier p_22131_) {
      this.removeModifier(p_22131_.id());
   }

   public boolean removeModifier(Identifier p_451790_) {
      AttributeModifier attributemodifier = this.modifierById.remove(p_451790_);
      if (attributemodifier == null) {
         return false;
      }

      this.getModifiers(attributemodifier.operation()).remove(p_451790_);
      this.permanentModifiers.remove(p_451790_);
      this.setDirty();
      return true;
   }

   public void removeModifiers() {
      for (AttributeModifier attributemodifier : this.getModifiers()) {
         this.removeModifier(attributemodifier);
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
      double d0 = this.getBaseValue();

      for (AttributeModifier attributemodifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_VALUE)) {
         d0 += attributemodifier.amount();
      }

      double d1 = d0;

      for (AttributeModifier attributemodifier1 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
         d1 += d0 * attributemodifier1.amount();
      }

      for (AttributeModifier attributemodifier2 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
         d1 *= 1.0 + attributemodifier2.amount();
      }

      return this.attribute.value().sanitizeValue(d1);
   }

   private Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation p_22117_) {
      return this.modifiersByOperation.getOrDefault(p_22117_, Map.of()).values();
   }

   public void replaceFrom(AttributeInstance p_22103_) {
      this.baseValue = p_22103_.baseValue;
      this.modifierById.clear();
      this.modifierById.putAll(p_22103_.modifierById);
      this.permanentModifiers.clear();
      this.permanentModifiers.putAll(p_22103_.permanentModifiers);
      this.modifiersByOperation.clear();
      p_22103_.modifiersByOperation
         .forEach((p_326791_, p_326792_) -> this.getModifiers(p_326791_).putAll((Map<? extends Identifier, ? extends AttributeModifier>)p_326792_));
      this.setDirty();
   }

   public AttributeInstance.Packed pack() {
      return new AttributeInstance.Packed(this.attribute, this.baseValue, List.copyOf(this.permanentModifiers.values()));
   }

   public void apply(AttributeInstance.Packed p_408710_) {
      this.baseValue = p_408710_.baseValue;

      for (AttributeModifier attributemodifier : p_408710_.modifiers) {
         this.modifierById.put(attributemodifier.id(), attributemodifier);
         this.getModifiers(attributemodifier.operation()).put(attributemodifier.id(), attributemodifier);
         this.permanentModifiers.put(attributemodifier.id(), attributemodifier);
      }

      this.setDirty();
   }

   public record Packed(Holder<Attribute> attribute, double baseValue, List<AttributeModifier> modifiers) {
      public static final Codec<AttributeInstance.Packed> CODEC = RecordCodecBuilder.create(
         p_408246_ -> p_408246_.group(
               BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("id").forGetter(AttributeInstance.Packed::attribute),
               Codec.DOUBLE.fieldOf("base").orElse(0.0).forGetter(AttributeInstance.Packed::baseValue),
               AttributeModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(AttributeInstance.Packed::modifiers)
            )
            .apply(p_408246_, AttributeInstance.Packed::new)
      );
      public static final Codec<List<AttributeInstance.Packed>> LIST_CODEC = CODEC.listOf();
   }
}
