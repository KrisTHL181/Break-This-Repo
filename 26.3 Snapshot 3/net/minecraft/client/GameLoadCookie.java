package net.minecraft.client;

import com.mojang.realmsclient.client.RealmsClient;
import net.minecraft.client.main.GameConfig;

public record GameLoadCookie(RealmsClient realmsClient, GameConfig.QuickPlayData quickPlayData) {
}
