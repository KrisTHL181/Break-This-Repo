package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class AttributeSupplier {
   private final Map<Holder<Attribute>, AttributeInstance> instances;

   AttributeSupplier(Map<Holder<Attribute>, AttributeInstance> p_22243_) {
      this.instances = p_22243_;
   }

   private AttributeInstance getAttributeInstance(Holder<Attribute> p_335900_) {
      AttributeInstance attributeinstance = this.instances.get(p_335900_);
      if (attributeinstance == null) {
         throw new IllegalArgumentException("Can't find attribute " + p_335900_.getRegisteredName());
      } else {
         return attributeinstance;
      }
   }

   public double getValue(Holder<Attribute> p_333974_) {
      return this.getAttributeInstance(p_333974_).getValue();
   }

   public double getBaseValue(Holder<Attribute> p_333849_) {
      return this.getAttributeInstance(p_333849_).getBaseValue();
   }

   public double getModifierValue(Holder<Attribute> p_333807_, Identifier p_450901_) {
      AttributeModifier attributemodifier = this.getAttributeInstance(p_333807_).getModifier(p_450901_);
      if (attributemodifier == null) {
         throw new IllegalArgumentException("Can't find modifier " + p_450901_ + " on attribute " + p_333807_.getRegisteredName());
      } else {
         return attributemodifier.amount();
      }
   }

   public @Nullable AttributeInstance createInstance(Consumer<AttributeInstance> p_22251_, Holder<Attribute> p_333997_) {
      AttributeInstance attributeinstance = this.instances.get(p_333997_);
      if (attributeinstance == null) {
         return null;
      }

      AttributeInstance attributeinstance1 = new AttributeInstance(p_333997_, p_22251_);
      attributeinstance1.replaceFrom(attributeinstance);
      return attributeinstance1;
   }

   public static AttributeSupplier.Builder builder() {
      return new AttributeSupplier.Builder();
   }

   public boolean hasAttribute(Holder<Attribute> p_331710_) {
      return this.instances.containsKey(p_331710_);
   }

   public boolean hasModifier(Holder<Attribute> p_335566_, Identifier p_459925_) {
      AttributeInstance attributeinstance = this.instances.get(p_335566_);
      return attributeinstance != null && attributeinstance.getModifier(p_459925_) != null;
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<Holder<Attribute>, AttributeInstance> builder = ImmutableMap.builder();
      private boolean instanceFrozen;

      private AttributeInstance create(Holder<Attribute> p_334904_) {
         AttributeInstance attributeinstance = new AttributeInstance(p_334904_, p_326800_ -> {
            if (this.instanceFrozen) {
               throw new UnsupportedOperationException("Tried to change value for default attribute instance: " + p_334904_.getRegisteredName());
            }
         });
         this.builder.put(p_334904_, attributeinstance);
         return attributeinstance;
      }

      public AttributeSupplier.Builder add(Holder<Attribute> p_334664_) {
         this.create(p_334664_);
         return this;
      }

      public AttributeSupplier.Builder add(Holder<Attribute> p_329131_, double p_22270_) {
         AttributeInstance attributeinstance = this.create(p_329131_);
         attributeinstance.setBaseValue(p_22270_);
         return this;
      }

      public AttributeSupplier build() {
         this.instanceFrozen = true;
         return new AttributeSupplier(this.builder.buildKeepingLast());
      }
   }
}
