package net.minecraft.client.multiplayer;

import com.google.common.base.Strings;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.InsecurePublicKeyException.MissingException;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse.KeyPair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class AccountProfileKeyPairManager implements ProfileKeyPairManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Duration MINIMUM_PROFILE_KEY_REFRESH_INTERVAL = Duration.ofHours(1L);
   private static final Path PROFILE_KEY_PAIR_DIR = Path.of("profilekeys");
   private final UserApiService userApiService;
   private final Path profileKeyPairPath;
   private CompletableFuture<Optional<ProfileKeyPair>> keyPair = CompletableFuture.completedFuture(Optional.empty());
   private Instant nextProfileKeyRefreshTime = Instant.EPOCH;

   public AccountProfileKeyPairManager(UserApiService p_253640_, UUID p_254415_, Path p_253813_) {
      this.userApiService = p_253640_;
      this.profileKeyPairPath = p_253813_.resolve(PROFILE_KEY_PAIR_DIR).resolve(p_254415_ + ".json");
   }

   @Override
   public CompletableFuture<Optional<ProfileKeyPair>> prepareKeyPair() {
      this.nextProfileKeyRefreshTime = Instant.now().plus(MINIMUM_PROFILE_KEY_REFRESH_INTERVAL);
      this.keyPair = this.keyPair.thenCompose(this::readOrFetchProfileKeyPair);
      return this.keyPair;
   }

   @Override
   public boolean shouldRefreshKeyPair() {
      return this.keyPair.isDone() && Instant.now().isAfter(this.nextProfileKeyRefreshTime)
         ? this.keyPair.join().map(ProfileKeyPair::dueRefresh).orElse(true)
         : false;
   }

   private CompletableFuture<Optional<ProfileKeyPair>> readOrFetchProfileKeyPair(Optional<ProfileKeyPair> p_254074_) {
      return CompletableFuture.supplyAsync(() -> {
         if (p_254074_.isPresent() && !p_254074_.get().dueRefresh()) {
            if (!SharedConstants.IS_RUNNING_IN_IDE) {
               this.writeProfileKeyPair(null);
            }

            return p_254074_;
         } else {
            try {
               ProfileKeyPair profilekeypair = this.fetchProfileKeyPair(this.userApiService);
               this.writeProfileKeyPair(profilekeypair);
               return Optional.ofNullable(profilekeypair);
            } catch (CryptException | MinecraftClientException | IOException ioexception) {
               LOGGER.error("Failed to retrieve profile key pair", ioexception);
               this.writeProfileKeyPair(null);
               return p_254074_;
            }
         }
      }, Util.nonCriticalIoPool());
   }

   private Optional<ProfileKeyPair> readProfileKeyPair() {
      if (Files.notExists(this.profileKeyPairPath)) {
         return Optional.empty();
      }

      try (BufferedReader bufferedreader = Files.newBufferedReader(this.profileKeyPairPath)) {
         return ProfileKeyPair.CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(bufferedreader)).result();
      } catch (Exception exception) {
         LOGGER.error("Failed to read profile key pair file {}", this.profileKeyPairPath, exception);
         return Optional.empty();
      }
   }

   private void writeProfileKeyPair(@Nullable ProfileKeyPair p_254227_) {
      try {
         Files.deleteIfExists(this.profileKeyPairPath);
      } catch (IOException ioexception) {
         LOGGER.error("Failed to delete profile key pair file {}", this.profileKeyPairPath, ioexception);
      }

      if (p_254227_ != null && SharedConstants.IS_RUNNING_IN_IDE) {
         ProfileKeyPair.CODEC.encodeStart(JsonOps.INSTANCE, p_254227_).ifSuccess(p_254406_ -> {
            try {
               Files.createDirectories(this.profileKeyPairPath.getParent());
               Files.writeString(this.profileKeyPairPath, p_254406_.toString());
            } catch (Exception exception) {
               LOGGER.error("Failed to write profile key pair file {}", this.profileKeyPairPath, exception);
            }
         });
      }
   }

   private @Nullable ProfileKeyPair fetchProfileKeyPair(UserApiService p_253844_) throws CryptException, IOException {
      KeyPairResponse keypairresponse = p_253844_.getKeyPair();
      if (keypairresponse != null) {
         ProfilePublicKey.Data profilepublickey$data = parsePublicKey(keypairresponse);
         return new ProfileKeyPair(
            Crypt.stringToPemRsaPrivateKey(keypairresponse.keyPair().privateKey()),
            new ProfilePublicKey(profilepublickey$data),
            Instant.parse(keypairresponse.refreshedAfter())
         );
      } else {
         return null;
      }
   }

   private static ProfilePublicKey.Data parsePublicKey(KeyPairResponse p_253834_) throws CryptException {
      KeyPair keypair = p_253834_.keyPair();
      if (keypair != null
         && !Strings.isNullOrEmpty(keypair.publicKey())
         && p_253834_.publicKeySignature() != null
         && p_253834_.publicKeySignature().array().length != 0) {
         try {
            Instant instant = Instant.parse(p_253834_.expiresAt());
            PublicKey publickey = Crypt.stringToRsaPublicKey(keypair.publicKey());
            ByteBuffer bytebuffer = p_253834_.publicKeySignature();
            return new ProfilePublicKey.Data(instant, publickey, bytebuffer.array());
         } catch (IllegalArgumentException | DateTimeException datetimeexception) {
            throw new CryptException(datetimeexception);
         }
      } else {
         throw new CryptException(new MissingException("Missing public key"));
      }
   }
}
