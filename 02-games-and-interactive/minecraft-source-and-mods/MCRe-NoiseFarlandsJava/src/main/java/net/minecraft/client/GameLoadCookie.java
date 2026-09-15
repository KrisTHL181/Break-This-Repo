package net.minecraft.client;

import com.mojang.realmsclient.client.RealmsClient;
import net.minecraft.client.main.GameConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GameLoadCookie(RealmsClient realmsClient, GameConfig.QuickPlayData quickPlayData) {
}