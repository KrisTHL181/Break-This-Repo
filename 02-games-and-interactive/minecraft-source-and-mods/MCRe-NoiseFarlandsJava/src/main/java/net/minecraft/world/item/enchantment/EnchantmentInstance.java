package net.minecraft.world.item.enchantment;

import net.minecraft.core.Holder;

public record EnchantmentInstance(Holder<Enchantment> enchantment, int level) {
    public int weight() {
        return this.enchantment().value().getWeight();
    }
}