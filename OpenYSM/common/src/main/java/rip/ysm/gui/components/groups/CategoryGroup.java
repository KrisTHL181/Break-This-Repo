package rip.ysm.gui.components.groups;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import rip.ysm.gui.OptionGroup;

public class CategoryGroup extends OptionGroup {
    private final String catKey;

    public CategoryGroup(String catKey) {
        super("animation_category." + catKey);
        this.catKey = catKey;
    }

    @Override
    public Component getTitle() {
        String key = "gui.yes_steve_model.animation.category." + catKey;
        if (I18n.exists(key)) return Component.translatable(key);
        return Component.literal(catKey);
    }
}
