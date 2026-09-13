package net.minecraft.client.multiplayer;

import com.mojang.authlib.minecraft.UserApiService;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.User;
import net.minecraft.world.entity.player.ProfileKeyPair;

public interface ProfileKeyPairManager {
   ProfileKeyPairManager EMPTY_KEY_MANAGER = new ProfileKeyPairManager() {
      @Override
      public CompletableFuture<Optional<ProfileKeyPair>> prepareKeyPair() {
         return CompletableFuture.completedFuture(Optional.empty());
      }

      @Override
      public boolean shouldRefreshKeyPair() {
         return false;
      }
   };

   static ProfileKeyPairManager create(final UserApiService userApiService, final User user, final Path gameDirectory) {
      return new AccountProfileKeyPairManager(userApiService, user.getProfileId(), gameDirectory);
   }

   CompletableFuture<Optional<ProfileKeyPair>> prepareKeyPair();

   boolean shouldRefreshKeyPair();
}
