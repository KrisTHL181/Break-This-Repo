package com.mojang.authlib.minecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.services.ProfileResult;
import java.net.InetAddress;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public interface SessionService {
   void joinServer(UUID var1, String var2, String var3) throws AuthenticationException;

   @Nullable
   ProfileResult hasJoinedServer(String var1, String var2, @Nullable InetAddress var3) throws AuthenticationUnavailableException;

   @Nullable
   Property getPackedTextures(GameProfile var1);

   MinecraftProfileTextures unpackTextures(Property var1);

   default MinecraftProfileTextures getTextures(GameProfile profile) {
      Property packed = this.getPackedTextures(profile);
      return packed != null ? this.unpackTextures(packed) : MinecraftProfileTextures.EMPTY;
   }

   @Nullable
   ProfileResult fetchProfile(UUID var1, boolean var2);

   String getSecurePropertyValue(Property var1) throws InsecurePublicKeyException;
}
