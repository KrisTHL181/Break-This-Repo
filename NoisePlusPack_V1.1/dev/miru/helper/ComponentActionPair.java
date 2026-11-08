package dev.miru.helper;

import net.minecraft.network.chat.Component;

public record ComponentActionPair(Component component, Runnable runnable) {
}
