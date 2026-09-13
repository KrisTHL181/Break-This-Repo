package net.minecraft.client.multiplayer.chat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum GuiMessageSource {
    PLAYER,
    SYSTEM_SERVER,
    SYSTEM_CLIENT;
}