package com.mojang.realmsclient.exception;

import com.mojang.realmsclient.client.RealmsError;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsServiceException extends Exception {
    public final RealmsError realmsError;

    public RealmsServiceException(final RealmsError error) {
        this.realmsError = error;
    }

    @Override
    public String getMessage() {
        return this.realmsError.logMessage();
    }
}