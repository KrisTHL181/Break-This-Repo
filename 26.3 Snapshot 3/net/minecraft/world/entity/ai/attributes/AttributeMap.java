package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class AttributeMap {
   private final Map<Holder<Attribute>, AttributeInstance> attributes = new Object2ObjectOpenHashMap();
   private final Set<AttributeInstance> attributesToSync = new ObjectOpenHashSet();
   private final Set<AttributeInstance> attributesToUpdate = new ObjectOpenHashSet();
   private final AttributeSupplier supplier;

   public AttributeMap(final AttributeSupplier supplier) {
      this.supplier = supplier;
   }

   private void onAttributeModified(final AttributeInstance attributeInstance) {
      this.attributesToUpdate.add(attributeInstance);
      if (attributeInstance.getAttribute().value().isClientSyncable()) {
         this.attributesToSync.add(attributeInstance);
      }
   }

   public Set<AttributeInstance> getAttributesToSync() {
      return this.attributesToSync;
   }

   public Set<AttributeInstance> getAttributesToUpdate() {
      return this.attributesToUpdate;
   }

   public Collection<AttributeInstance> getSyncableAttributes() {
      return this.attributes.values().stream().filter(instance -> instance.getAttribute().value().isClientSyncable()).collect(Collectors.toList());
   }

   public @Nullable AttributeInstance getInstance(final Holder<Attribute> attribute) {
      return this.attributes.computeIfAbsent(attribute, key -> this.supplier.createInstance(this::onAttributeModified, (Holder<Attribute>)key));
   }

   public boolean hasAttribute(final Holder<Attribute> attribute) {
      return this.attributes.get(attribute) != null || this.supplier.hasAttribute(attribute);
   }

   public boolean hasModifier(final Holder<Attribute> attribute, final Identifier id) {
      AttributeInstance attributeInstance = this.attributes.get(attribute);
      return attributeInstance != null ? attributeInstance.getModifier(id) != null : this.supplier.hasModifier(attribute, id);
   }

   public double getValue(final Holder<Attribute> attribute) {
      AttributeInstance ownAttribute = this.attributes.get(attribute);
      return ownAttribute != null ? ownAttribute.getValue() : this.supplier.getValue(attribute);
   }

   public double getBaseValue(final Holder<Attribute> attribute) {
      AttributeInstance ownAttribute = this.attributes.get(attribute);
      return ownAttribute != null ? ownAttribute.getBaseValue() : this.supplier.getBaseValue(attribute);
   }

   public double getModifierValue(final Holder<Attribute> attribute, final Identifier id) {
      AttributeInstance attributeInstance = this.attributes.get(attribute);
      return attributeInstance != null ? attributeInstance.getModifier(id).amount() : this.supplier.getModifierValue(attribute, id);
   }

   public void addTransientAttributeModifiers(final Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
      modifiers.forEach((attribute, attributeModifier) -> {
         AttributeInstance instance = this.getInstance(attribute);
         if (instance != null) {
            instance.removeModifier(attributeModifier.id());
            instance.addTransientModifier(attributeModifier);
         }
      });
   }

   public void removeAttributeModifiers(final Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
      modifiers.asMap().forEach((attribute, attributeModifiers) -> {
         AttributeInstance instance = this.attributes.get(attribute);
         if (instance != null) {
            attributeModifiers.forEach(attributeModifier -> instance.removeModifier(attributeModifier.id()));
         }
      });
   }

   public void assignAllValues(final AttributeMap other) {
      other.attributes.values().forEach(otherInstance -> {
         AttributeInstance selfInstance = this.getInstance(otherInstance.getAttribute());
         if (selfInstance != null) {
            selfInstance.replaceFrom(otherInstance);
         }
      });
   }

   public void assignBaseValues(final AttributeMap other) {
      other.attributes.values().forEach(otherInstance -> {
         AttributeInstance selfInstance = this.getInstance(otherInstance.getAttribute());
         if (selfInstance != null) {
            selfInstance.setBaseValue(otherInstance.getBaseValue());
         }
      });
   }

   public void assignPermanentModifiers(final AttributeMap other) {
      other.attributes.values().forEach(otherInstance -> {
         AttributeInstance selfInstance = this.getInstance(otherInstance.getAttribute());
         if (selfInstance != null) {
            selfInstance.addPermanentModifiers(otherInstance.getPermanentModifiers());
         }
      });
   }

   public boolean resetBaseValue(final Holder<Attribute> attribute) {
      if (!this.supplier.hasAttribute(attribute)) {
         return false;
      }

      AttributeInstance instance = this.attributes.get(attribute);
      if (instance != null) {
         instance.setBaseValue(this.supplier.getBaseValue(attribute));
      }

      return true;
   }

   public List<AttributeInstance.Packed> pack() {
      List<AttributeInstance.Packed> result = new ArrayList<>(this.attributes.values().size());

      for (AttributeInstance attribute : this.attributes.values()) {
         result.add(attribute.pack());
      }

      return result;
   }

   public void apply(final List<AttributeInstance.Packed> packedAttributes) {
      for (AttributeInstance.Packed packedAttribute : packedAttributes) {
         AttributeInstance instance = this.getInstance(packedAttribute.attribute());
         if (instance != null) {
            instance.apply(packedAttribute);
         }
      }
   }
}
