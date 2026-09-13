package net.minecraft.client.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ClientAvatarEntity {
   ClientAvatarState avatarState();

   PlayerSkin getSkin();

   @Nullable Component belowNameDisplay();

   Parrot.@Nullable Variant getParrotVariantOnShoulder(boolean var1);

   boolean showExtraEars();
}
