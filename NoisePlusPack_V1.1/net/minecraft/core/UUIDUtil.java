package net.minecraft.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.util.UndashedUuid;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;

public final class UUIDUtil {
   public static final Codec<UUID> CODEC = Codec.INT_STREAM
      .comapFlatMap(p_448580_ -> Util.fixedSize(p_448580_, 4).map(UUIDUtil::uuidFromIntArray), p_235888_ -> Arrays.stream(uuidToIntArray(p_235888_)));
   public static final Codec<Set<UUID>> CODEC_SET = Codec.list(CODEC).xmap(Sets::newHashSet, Lists::newArrayList);
   public static final Codec<Set<UUID>> CODEC_LINKED_SET = Codec.list(CODEC).xmap(Sets::newLinkedHashSet, Lists::newArrayList);
   public static final Codec<UUID> STRING_CODEC = Codec.STRING.comapFlatMap(p_274732_ -> {
      try {
         return DataResult.success(UUID.fromString(p_274732_), Lifecycle.stable());
      } catch (IllegalArgumentException illegalargumentexception) {
         return DataResult.error(() -> "Invalid UUID " + p_274732_ + ": " + illegalargumentexception.getMessage());
      }
   }, UUID::toString);
   public static final Codec<UUID> AUTHLIB_CODEC = Codec.withAlternative(Codec.STRING.comapFlatMap(p_296331_ -> {
      try {
         return DataResult.success(UndashedUuid.fromStringLenient(p_296331_), Lifecycle.stable());
      } catch (IllegalArgumentException illegalargumentexception) {
         return DataResult.error(() -> "Invalid UUID " + p_296331_ + ": " + illegalargumentexception.getMessage());
      }
   }, UndashedUuid::toString), CODEC);
   public static final Codec<UUID> LENIENT_CODEC = Codec.withAlternative(CODEC, STRING_CODEC);
   public static final StreamCodec<ByteBuf, UUID> STREAM_CODEC = new StreamCodec<ByteBuf, UUID>() {
      public UUID decode(ByteBuf p_332317_) {
         return FriendlyByteBuf.readUUID(p_332317_);
      }

      public void encode(ByteBuf p_331213_, UUID p_327754_) {
         FriendlyByteBuf.writeUUID(p_331213_, p_327754_);
      }
   };
   public static final int UUID_BYTES = 16;
   private static final String UUID_PREFIX_OFFLINE_PLAYER = "OfflinePlayer:";

   private UUIDUtil() {
   }

   public static UUID uuidFromIntArray(int[] p_235886_) {
      return new UUID((long)p_235886_[0] << 32 | p_235886_[1] & 4294967295L, (long)p_235886_[2] << 32 | p_235886_[3] & 4294967295L);
   }

   public static int[] uuidToIntArray(UUID p_235882_) {
      long i = p_235882_.getMostSignificantBits();
      long j = p_235882_.getLeastSignificantBits();
      return leastMostToIntArray(i, j);
   }

   private static int[] leastMostToIntArray(long p_235873_, long p_235874_) {
      return new int[]{(int)(p_235873_ >> 32), (int)p_235873_, (int)(p_235874_ >> 32), (int)p_235874_};
   }

   public static byte[] uuidToByteArray(UUID p_241285_) {
      byte[] abyte = new byte[16];
      ByteBuffer.wrap(abyte).order(ByteOrder.BIG_ENDIAN).putLong(p_241285_.getMostSignificantBits()).putLong(p_241285_.getLeastSignificantBits());
      return abyte;
   }

   public static UUID readUUID(Dynamic<?> p_235878_) {
      int[] aint = p_235878_.asIntStream().toArray();
      if (aint.length != 4) {
         throw new IllegalArgumentException("Could not read UUID. Expected int-array of length 4, got " + aint.length + ".");
      } else {
         return uuidFromIntArray(aint);
      }
   }

   public static UUID createOfflinePlayerUUID(String p_235880_) {
      return UUID.nameUUIDFromBytes(("OfflinePlayer:" + p_235880_).getBytes(StandardCharsets.UTF_8));
   }

   public static GameProfile createOfflineProfile(String p_309926_) {
      UUID uuid = createOfflinePlayerUUID(p_309926_);
      return new GameProfile(uuid, p_309926_);
   }
}
