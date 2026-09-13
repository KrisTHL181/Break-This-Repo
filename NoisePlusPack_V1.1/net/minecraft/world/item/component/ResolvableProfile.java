package net.minecraft.world.item.component;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public abstract sealed class ResolvableProfile implements TooltipProvider permits ResolvableProfile.Static, ResolvableProfile.Dynamic {
   private static final Codec<ResolvableProfile> FULL_CODEC = RecordCodecBuilder.create(
      p_422001_ -> p_422001_.group(
            Codec.mapEither(ExtraCodecs.STORED_GAME_PROFILE, ResolvableProfile.Partial.MAP_CODEC).forGetter(ResolvableProfile::unpack),
            PlayerSkin.Patch.MAP_CODEC.forGetter(ResolvableProfile::skinPatch)
         )
         .apply(p_422001_, ResolvableProfile::create)
   );
   public static final Codec<ResolvableProfile> CODEC = Codec.withAlternative(FULL_CODEC, ExtraCodecs.PLAYER_NAME, ResolvableProfile::createUnresolved);
   public static final StreamCodec<ByteBuf, ResolvableProfile> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.either(ByteBufCodecs.GAME_PROFILE, ResolvableProfile.Partial.STREAM_CODEC),
      ResolvableProfile::unpack,
      PlayerSkin.Patch.STREAM_CODEC,
      ResolvableProfile::skinPatch,
      ResolvableProfile::create
   );
   protected final GameProfile partialProfile;
   protected final PlayerSkin.Patch skinPatch;

   private static ResolvableProfile create(Either<GameProfile, ResolvableProfile.Partial> p_430787_, PlayerSkin.Patch p_431421_) {
      return (ResolvableProfile)p_430787_.map(
         p_422003_ -> new ResolvableProfile.Static(Either.left(p_422003_), p_431421_),
         p_421995_ -> p_421995_.properties.isEmpty() && p_421995_.id.isPresent() != p_421995_.name.isPresent()
            ? p_421995_.name
               .<ResolvableProfile>map(p_421999_ -> new ResolvableProfile.Dynamic(Either.left(p_421999_), p_431421_))
               .orElseGet(() -> new ResolvableProfile.Dynamic(Either.right(p_421995_.id.get()), p_431421_))
            : new ResolvableProfile.Static(Either.right(p_421995_), p_431421_)
      );
   }

   public static ResolvableProfile createResolved(GameProfile p_426884_) {
      return new ResolvableProfile.Static(Either.left(p_426884_), PlayerSkin.Patch.EMPTY);
   }

   public static ResolvableProfile createUnresolved(String p_426244_) {
      return new ResolvableProfile.Dynamic(Either.left(p_426244_), PlayerSkin.Patch.EMPTY);
   }

   public static ResolvableProfile createUnresolved(UUID p_423353_) {
      return new ResolvableProfile.Dynamic(Either.right(p_423353_), PlayerSkin.Patch.EMPTY);
   }

   protected abstract Either<GameProfile, ResolvableProfile.Partial> unpack();

   protected ResolvableProfile(GameProfile p_429325_, PlayerSkin.Patch p_428666_) {
      this.partialProfile = p_429325_;
      this.skinPatch = p_428666_;
   }

   public abstract CompletableFuture<GameProfile> resolveProfile(ProfileResolver var1);

   public GameProfile partialProfile() {
      return this.partialProfile;
   }

   public PlayerSkin.Patch skinPatch() {
      return this.skinPatch;
   }

   static GameProfile createPartialProfile(Optional<String> p_429587_, Optional<UUID> p_429212_, PropertyMap p_429243_) {
      String s = p_429587_.orElse("");
      UUID uuid = p_429212_.orElseGet(() -> p_429587_.map(UUIDUtil::createOfflinePlayerUUID).orElse(Util.NIL_UUID));
      return new GameProfile(uuid, s, p_429243_);
   }

   public abstract Optional<String> name();

   public static final class Dynamic extends ResolvableProfile {
      private static final Component DYNAMIC_TOOLTIP = Component.translatable("component.profile.dynamic").withStyle(ChatFormatting.GRAY);
      private final Either<String, UUID> nameOrId;

      Dynamic(Either<String, UUID> p_425272_, PlayerSkin.Patch p_430189_) {
         super(ResolvableProfile.createPartialProfile(p_425272_.left(), p_425272_.right(), PropertyMap.EMPTY), p_430189_);
         this.nameOrId = p_425272_;
      }

      @Override
      public Optional<String> name() {
         return this.nameOrId.left();
      }

      @Override
      public boolean equals(Object p_427792_) {
         return this == p_427792_
            || p_427792_ instanceof ResolvableProfile.Dynamic resolvableprofile$dynamic
               && this.nameOrId.equals(resolvableprofile$dynamic.nameOrId)
               && this.skinPatch.equals(resolvableprofile$dynamic.skinPatch);
      }

      @Override
      public int hashCode() {
         int i = 31 + this.nameOrId.hashCode();
         return 31 * i + this.skinPatch.hashCode();
      }

      @Override
      protected Either<GameProfile, ResolvableProfile.Partial> unpack() {
         return Either.right(new ResolvableProfile.Partial(this.nameOrId.left(), this.nameOrId.right(), PropertyMap.EMPTY));
      }

      @Override
      public CompletableFuture<GameProfile> resolveProfile(ProfileResolver p_423044_) {
         return CompletableFuture.supplyAsync(() -> p_423044_.fetchByNameOrId(this.nameOrId).orElse(this.partialProfile), Util.nonCriticalIoPool());
      }

      @Override
      public void addToTooltip(Item.TooltipContext p_427273_, Consumer<Component> p_427701_, TooltipFlag p_425571_, DataComponentGetter p_429007_) {
         p_427701_.accept(DYNAMIC_TOOLTIP);
      }
   }

   protected record Partial(Optional<String> name, Optional<UUID> id, PropertyMap properties) {
      public static final ResolvableProfile.Partial EMPTY = new ResolvableProfile.Partial(Optional.empty(), Optional.empty(), PropertyMap.EMPTY);
      static final MapCodec<ResolvableProfile.Partial> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_429269_ -> p_429269_.group(
               ExtraCodecs.PLAYER_NAME.optionalFieldOf("name").forGetter(ResolvableProfile.Partial::name),
               UUIDUtil.CODEC.optionalFieldOf("id").forGetter(ResolvableProfile.Partial::id),
               ExtraCodecs.PROPERTY_MAP.optionalFieldOf("properties", PropertyMap.EMPTY).forGetter(ResolvableProfile.Partial::properties)
            )
            .apply(p_429269_, ResolvableProfile.Partial::new)
      );
      public static final StreamCodec<ByteBuf, ResolvableProfile.Partial> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.PLAYER_NAME.apply(ByteBufCodecs::optional),
         ResolvableProfile.Partial::name,
         UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional),
         ResolvableProfile.Partial::id,
         ByteBufCodecs.GAME_PROFILE_PROPERTIES,
         ResolvableProfile.Partial::properties,
         ResolvableProfile.Partial::new
      );

      private GameProfile createProfile() {
         return ResolvableProfile.createPartialProfile(this.name, this.id, this.properties);
      }
   }

   public static final class Static extends ResolvableProfile {
      public static final ResolvableProfile.Static EMPTY = new ResolvableProfile.Static(Either.right(ResolvableProfile.Partial.EMPTY), PlayerSkin.Patch.EMPTY);
      private final Either<GameProfile, ResolvableProfile.Partial> contents;

      Static(Either<GameProfile, ResolvableProfile.Partial> p_428859_, PlayerSkin.Patch p_428890_) {
         super((GameProfile)p_428859_.map(p_431247_ -> p_431247_, ResolvableProfile.Partial::createProfile), p_428890_);
         this.contents = p_428859_;
      }

      @Override
      public CompletableFuture<GameProfile> resolveProfile(ProfileResolver p_427730_) {
         return CompletableFuture.completedFuture(this.partialProfile);
      }

      @Override
      protected Either<GameProfile, ResolvableProfile.Partial> unpack() {
         return this.contents;
      }

      @Override
      public Optional<String> name() {
         return (Optional<String>)this.contents.map(p_428364_ -> Optional.of(p_428364_.name()), p_423458_ -> p_423458_.name);
      }

      @Override
      public boolean equals(Object p_427217_) {
         return this == p_427217_
            || p_427217_ instanceof ResolvableProfile.Static resolvableprofile$static
               && this.contents.equals(resolvableprofile$static.contents)
               && this.skinPatch.equals(resolvableprofile$static.skinPatch);
      }

      @Override
      public int hashCode() {
         int i = 31 + this.contents.hashCode();
         return 31 * i + this.skinPatch.hashCode();
      }

      @Override
      public void addToTooltip(Item.TooltipContext p_426944_, Consumer<Component> p_428668_, TooltipFlag p_429017_, DataComponentGetter p_425405_) {
      }
   }
}
