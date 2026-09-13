package net.minecraft.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.authlib.services.ServicesKeySet;
import com.mojang.authlib.services.ServicesKeyType;
import java.io.File;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.util.SignatureValidator;
import org.jspecify.annotations.Nullable;

public record Services(
   SessionService sessionService,
   ServicesKeySet servicesKeySet,
   GameProfileRepository profileRepository,
   UserNameToIdResolver nameToIdCache,
   ProfileResolver profileResolver
) {
   private static final String USERID_CACHE_FILE = "usercache.json";

   public static Services create(final MinecraftServicesDiscoveryService serviceAccess, final File nameCacheDir) {
      SessionService sessionService = serviceAccess.createMinecraftSessionService();
      GameProfileRepository profileRepository = serviceAccess.createProfileRepository();
      UserNameToIdResolver profileCache = new CachedUserNameToIdResolver(profileRepository, new File(nameCacheDir, "usercache.json"));
      ProfileResolver profileResolver = new ProfileResolver.Cached(sessionService, profileCache);
      return new Services(sessionService, serviceAccess.getServicesKeySet(), profileRepository, profileCache, profileResolver);
   }

   public @Nullable SignatureValidator profileKeySignatureValidator() {
      return SignatureValidator.from(this.servicesKeySet, ServicesKeyType.PROFILE_KEY);
   }

   public boolean canValidateProfileKeys() {
      return !this.servicesKeySet.keys(ServicesKeyType.PROFILE_KEY).isEmpty();
   }
}
