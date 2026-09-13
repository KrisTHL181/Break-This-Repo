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

   public AttributeMap(AttributeSupplier p_22144_) {
      this.supplier = p_22144_;
   }

   private void onAttributeModified(AttributeInstance p_22158_) {
      this.attributesToUpdate.add(p_22158_);
      if (p_22158_.getAttribute().value().isClientSyncable()) {
         this.attributesToSync.add(p_22158_);
      }
   }

   public Set<AttributeInstance> getAttributesToSync() {
      return this.attributesToSync;
   }

   public Set<AttributeInstance> getAttributesToUpdate() {
      return this.attributesToUpdate;
   }

   public Collection<AttributeInstance> getSyncableAttributes() {
      return this.attributes.values().stream().filter(p_326797_ -> p_326797_.getAttribute().value().isClientSyncable()).collect(Collectors.toList());
   }

   public @Nullable AttributeInstance getInstance(Holder<Attribute> p_250010_) {
      return this.attributes.computeIfAbsent(p_250010_, p_326793_ -> this.supplier.createInstance(this::onAttributeModified, (Holder<Attribute>)p_326793_));
   }

   public boolean hasAttribute(Holder<Attribute> p_248893_) {
      return this.attributes.get(p_248893_) != null || this.supplier.hasAttribute(p_248893_);
   }

   public boolean hasModifier(Holder<Attribute> p_250299_, Identifier p_452126_) {
      AttributeInstance attributeinstance = this.attributes.get(p_250299_);
      return attributeinstance != null ? attributeinstance.getModifier(p_452126_) != null : this.supplier.hasModifier(p_250299_, p_452126_);
   }

   public double getValue(Holder<Attribute> p_328238_) {
      AttributeInstance attributeinstance = this.attributes.get(p_328238_);
      return attributeinstance != null ? attributeinstance.getValue() : this.supplier.getValue(p_328238_);
   }

   public double getBaseValue(Holder<Attribute> p_329417_) {
      AttributeInstance attributeinstance = this.attributes.get(p_329417_);
      return attributeinstance != null ? attributeinstance.getBaseValue() : this.supplier.getBaseValue(p_329417_);
   }

   public double getModifierValue(Holder<Attribute> p_251534_, Identifier p_450518_) {
      AttributeInstance attributeinstance = this.attributes.get(p_251534_);
      return attributeinstance != null ? attributeinstance.getModifier(p_450518_).amount() : this.supplier.getModifierValue(p_251534_, p_450518_);
   }

   public void addTransientAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> p_342579_) {
      p_342579_.forEach((p_449439_, p_449440_) -> {
         AttributeInstance attributeinstance = this.getInstance(p_449439_);
         if (attributeinstance != null) {
            attributeinstance.removeModifier(p_449440_.id());
            attributeinstance.addTransientModifier(p_449440_);
         }
      });
   }

   public void removeAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> p_342034_) {
      p_342034_.asMap().forEach((p_341283_, p_341284_) -> {
         AttributeInstance attributeinstance = this.attributes.get(p_341283_);
         if (attributeinstance != null) {
            p_341284_.forEach(p_449442_ -> attributeinstance.removeModifier(p_449442_.id()));
         }
      });
   }

   public void assignAllValues(AttributeMap p_22160_) {
      p_22160_.attributes.values().forEach(p_326796_ -> {
         AttributeInstance attributeinstance = this.getInstance(p_326796_.getAttribute());
         if (attributeinstance != null) {
            attributeinstance.replaceFrom(p_326796_);
         }
      });
   }

   public void assignBaseValues(AttributeMap p_344183_) {
      p_344183_.attributes.values().forEach(p_341285_ -> {
         AttributeInstance attributeinstance = this.getInstance(p_341285_.getAttribute());
         if (attributeinstance != null) {
            attributeinstance.setBaseValue(p_341285_.getBaseValue());
         }
      });
   }

   public void assignPermanentModifiers(AttributeMap p_365307_) {
      p_365307_.attributes.values().forEach(p_358913_ -> {
         AttributeInstance attributeinstance = this.getInstance(p_358913_.getAttribute());
         if (attributeinstance != null) {
            attributeinstance.addPermanentModifiers(p_358913_.getPermanentModifiers());
         }
      });
   }

   public boolean resetBaseValue(Holder<Attribute> p_377122_) {
      if (!this.supplier.hasAttribute(p_377122_)) {
         return false;
      }

      AttributeInstance attributeinstance = this.attributes.get(p_377122_);
      if (attributeinstance != null) {
         attributeinstance.setBaseValue(this.supplier.getBaseValue(p_377122_));
      }

      return true;
   }

   public List<AttributeInstance.Packed> pack() {
      List<AttributeInstance.Packed> list = new ArrayList<>(this.attributes.values().size());

      for (AttributeInstance attributeinstance : this.attributes.values()) {
         list.add(attributeinstance.pack());
      }

      return list;
   }

   public void apply(List<AttributeInstance.Packed> p_409870_) {
      for (AttributeInstance.Packed attributeinstance$packed : p_409870_) {
         AttributeInstance attributeinstance = this.getInstance(attributeinstance$packed.attribute());
         if (attributeinstance != null) {
            attributeinstance.apply(attributeinstance$packed);
         }
      }
   }
}
