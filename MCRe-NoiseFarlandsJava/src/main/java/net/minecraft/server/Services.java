package net.minecraft.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.io.File;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.util.SignatureValidator;
import org.jspecify.annotations.Nullable;

public record Services(
    MinecraftSessionService sessionService,
    ServicesKeySet servicesKeySet,
    GameProfileRepository profileRepository,
    UserNameToIdResolver nameToIdCache,
    ProfileResolver profileResolver
) {
    private static final String USERID_CACHE_FILE = "usercache.json";

    public static Services create(final YggdrasilAuthenticationService serviceAccess, final File nameCacheDir) {
        MinecraftSessionService sessionService = serviceAccess.createMinecraftSessionService();
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