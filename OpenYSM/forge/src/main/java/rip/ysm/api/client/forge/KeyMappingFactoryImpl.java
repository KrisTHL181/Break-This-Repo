package rip.ysm.api.client.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

public final class KeyMappingFactoryImpl {

    private KeyMappingFactoryImpl() {
    }

    public static KeyMapping createInGameAlt(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.ALT, type, keyCode, category);
    }

    public static KeyMapping createInGameNone(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.NONE, type, keyCode, category);
    }

    @SuppressWarnings({"removal"})
    public static boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode) {
        return keyMapping.matches(keyCode, scanCode) && keyMapping.getKeyModifier().equals(KeyModifier.getActiveModifier());
    }
}
