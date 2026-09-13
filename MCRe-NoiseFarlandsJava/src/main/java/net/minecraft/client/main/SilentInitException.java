package net.minecraft.client.main;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SilentInitException extends RuntimeException {
    public SilentInitException(final String message) {
        super(message);
    }

    public SilentInitException(final String message, final Throwable cause) {
        super(message, cause);
    }
}