package net.minecraft.client.resources.language;

import java.util.IllegalFormatException;
import java.util.Locale;
import net.minecraft.locale.Language;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class I18n {
    private I18n() {
    }

    public static String get(final String id, final Object... args) {
        String value = Language.getInstance().getOrDefault(id);

        try {
            return String.format(Locale.ROOT, value, args);
        } catch (IllegalFormatException ignored) {
            return "Format error: " + value;
        }
    }
}