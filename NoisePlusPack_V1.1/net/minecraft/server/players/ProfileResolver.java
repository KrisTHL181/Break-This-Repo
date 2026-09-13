package net.minecraft.server.players;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.datafixers.util.Either;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.StringUtil;

public interface ProfileResolver {
   Optional<GameProfile> fetchByName(String var1);

   Optional<GameProfile> fetchById(UUID var1);

   default Optional<GameProfile> fetchByNameOrId(Either<String, UUID> p_422954_) {
      return (Optional<GameProfile>)p_422954_.map(this::fetchByName, this::fetchById);
   }

   class Cached implements ProfileResolver {
      private final LoadingCache<String, Optional<GameProfile>> profileCacheByName;
      final LoadingCache<UUID, Optional<GameProfile>> profileCacheById;

      public Cached(final MinecraftSessionService p_425621_, final UserNameToIdResolver p_426272_) {
         this.profileCacheById = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(new CacheLoader<UUID, Optional<GameProfile>>() {
               public Optional<GameProfile> load(UUID p_430979_) {
                  ProfileResult profileresult = p_425621_.fetchProfile(p_430979_, true);
                  return Optional.ofNullable(profileresult).map(ProfileResult::profile);
               }
            });
         this.profileCacheByName = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(
               new CacheLoader<String, Optional<GameProfile>>() {
                  public Optional<GameProfile> load(String p_429387_) {
                     return p_426272_.get(p_429387_)
                        .flatMap(p_423118_ -> (Optional<? extends GameProfile>)Cached.this.profileCacheById.getUnchecked(p_423118_.id()));
                  }
               }
            );
      }

      @Override
      public Optional<GameProfile> fetchByName(String p_422916_) {
         return StringUtil.isValidPlayerName(p_422916_) ? (Optional)this.profileCacheByName.getUnchecked(p_422916_) : Optional.empty();
      }

      @Override
      public Optional<GameProfile> fetchById(UUID p_425059_) {
         return (Optional<GameProfile>)this.profileCacheById.getUnchecked(p_425059_);
      }
   }
}
