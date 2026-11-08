package rip.ysm.gui.components.groups;

import net.minecraft.network.chat.Component;
import rip.ysm.gui.OptionGroup;

public final class IdentifiedGroup extends OptionGroup {
    public final String id;
    private final String displayLabel;

    public IdentifiedGroup(String id, String displayLabel) {
        super(id);
        this.id = id;
        this.displayLabel = displayLabel;
    }

    @Override
    public Component getTitle() {
        return Component.literal(displayLabel);
    }
}
