package net.minecraft.client.resources;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DefaultPlayerSkin {
   private static final PlayerSkin[] DEFAULT_SKINS = new PlayerSkin[]{
      create("entity/player/slim/alex", PlayerModelType.SLIM),
      create("entity/player/slim/ari", PlayerModelType.SLIM),
      create("entity/player/slim/efe", PlayerModelType.SLIM),
      create("entity/player/slim/kai", PlayerModelType.SLIM),
      create("entity/player/slim/makena", PlayerModelType.SLIM),
      create("entity/player/slim/noor", PlayerModelType.SLIM),
      create("entity/player/slim/steve", PlayerModelType.SLIM),
      create("entity/player/slim/sunny", PlayerModelType.SLIM),
      create("entity/player/slim/zuri", PlayerModelType.SLIM),
      create("entity/player/wide/alex", PlayerModelType.WIDE),
      create("entity/player/wide/ari", PlayerModelType.WIDE),
      create("entity/player/wide/efe", PlayerModelType.WIDE),
      create("entity/player/wide/kai", PlayerModelType.WIDE),
      create("entity/player/wide/makena", PlayerModelType.WIDE),
      create("entity/player/wide/noor", PlayerModelType.WIDE),
      create("entity/player/wide/steve", PlayerModelType.WIDE),
      create("entity/player/wide/sunny", PlayerModelType.WIDE),
      create("entity/player/wide/zuri", PlayerModelType.WIDE)
   };

   public static Identifier getDefaultTexture() {
      return getDefaultSkin().body().texturePath();
   }

   public static PlayerSkin getDefaultSkin() {
      return DEFAULT_SKINS[6];
   }

   public static PlayerSkin get(UUID p_297375_) {
      return DEFAULT_SKINS[Math.floorMod(p_297375_.hashCode(), DEFAULT_SKINS.length)];
   }

   public static PlayerSkin get(GameProfile p_298897_) {
      return get(p_298897_.id());
   }

   private static PlayerSkin create(String p_298713_, PlayerModelType p_430274_) {
      return new PlayerSkin(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(p_298713_)), null, null, p_430274_, true);
   }
}
