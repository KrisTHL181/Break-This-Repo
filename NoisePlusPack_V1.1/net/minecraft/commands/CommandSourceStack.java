package net.minecraft.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.TaskChainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CommandSourceStack implements ExecutionCommandSource<CommandSourceStack>, SharedSuggestionProvider {
   public static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(Component.translatable("permissions.requires.player"));
   public static final SimpleCommandExceptionType ERROR_NOT_ENTITY = new SimpleCommandExceptionType(Component.translatable("permissions.requires.entity"));
   private final CommandSource source;
   private final Vec3 worldPosition;
   private final ServerLevel level;
   private final PermissionSet permissions;
   private final String textName;
   private final Component displayName;
   private final MinecraftServer server;
   private final boolean silent;
   private final @Nullable Entity entity;
   private final CommandResultCallback resultCallback;
   private final EntityAnchorArgument.Anchor anchor;
   private final Vec2 rotation;
   private final CommandSigningContext signingContext;
   private final TaskChainer chatMessageChainer;

   public CommandSourceStack(
      CommandSource p_282943_,
      Vec3 p_282023_,
      Vec2 p_282896_,
      ServerLevel p_282659_,
      PermissionSet p_450311_,
      String p_282379_,
      Component p_282469_,
      MinecraftServer p_281590_,
      @Nullable Entity p_281515_
   ) {
      this(
         p_282943_,
         p_282023_,
         p_282896_,
         p_282659_,
         p_450311_,
         p_282379_,
         p_282469_,
         p_281590_,
         p_281515_,
         false,
         CommandResultCallback.EMPTY,
         EntityAnchorArgument.Anchor.FEET,
         CommandSigningContext.ANONYMOUS,
         TaskChainer.immediate(p_281590_)
      );
   }

   private CommandSourceStack(
      CommandSource p_81302_,
      Vec3 p_81303_,
      Vec2 p_81304_,
      ServerLevel p_81305_,
      PermissionSet p_459601_,
      String p_81307_,
      Component p_81308_,
      MinecraftServer p_81309_,
      @Nullable Entity p_81310_,
      boolean p_451336_,
      CommandResultCallback p_455643_,
      EntityAnchorArgument.Anchor p_455259_,
      CommandSigningContext p_459795_,
      TaskChainer p_455967_
   ) {
      this.source = p_81302_;
      this.worldPosition = p_81303_;
      this.level = p_81305_;
      this.silent = p_451336_;
      this.entity = p_81310_;
      this.permissions = p_459601_;
      this.textName = p_81307_;
      this.displayName = p_81308_;
      this.server = p_81309_;
      this.resultCallback = p_455643_;
      this.anchor = p_455259_;
      this.rotation = p_81304_;
      this.signingContext = p_459795_;
      this.chatMessageChainer = p_455967_;
   }

   public CommandSourceStack withSource(CommandSource p_165485_) {
      return this.source == p_165485_
         ? this
         : new CommandSourceStack(
            p_165485_,
            this.worldPosition,
            this.rotation,
            this.level,
            this.permissions,
            this.textName,
            this.displayName,
            this.server,
            this.entity,
            this.silent,
            this.resultCallback,
            this.anchor,
            this.signingContext,
            this.chatMessageChainer
         );
   }

   public CommandSourceStack withEntity(Entity p_81330_) {
      return this.entity == p_81330_
         ? this
         : new CommandSourceStack(
            this.source,
            this.worldPosition,
            this.rotation,
            this.level,
            this.permissions,
            p_81330_.getPlainTextName(),
            p_81330_.getDisplayName(),
            this.server,
            p_81330_,
            this.silent,
            this.resultCallback,
            this.anchor,
            this.signingContext,
            this.chatMessageChainer
         );
   }

   public CommandSourceStack withPosition(Vec3 p_81349_) {
      return this.worldPosition.equals(p_81349_)
         ? this
         : new CommandSourceStack(
            this.source,
            p_81349_,
            this.rotation,
            this.level,
            this.permissions,
            this.textName,
            this.displayName,
            this.server,
            this.entity,
            this.silent,
            this.resultCallback,
            this.anchor,
            this.signingContext,
            this.chatMessageChainer
         );
   }

   public CommandSourceStack withRotation(Vec2 p_81347_) {
      return this.rotation.equals(p_81347_)
         ? this
         : new CommandSourceStack(
            this.source,
            this.worldPosition,
            p_81347_,
            this.level,
            this.permissions,
            this.textName,
            this.displayName,
            this.server,
            this.entity,
            this.silent,
            this.resultCallback,
            this.anchor,
            this.signingContext,
            this.chatMessageChainer
         );
   }

   public CommandSourceStack withCallback(CommandResultCallback p_310737_) {
      return Objects.equals(this.resultCallback, p_310737_)
         ? this
         : new CommandSourceStack(
            this.source,
            this.worldPosition,
            this.rotation,
            this.level,
            this.permissions,
            this.textName,
            this.displayName,
            this.server,
            this.entity,
            this.silent,
            p_310737_,
            this.anchor,
            this.signingContext,
            this.chatMessageChainer
         );
   }

   public CommandSourceStack withCallback(CommandResultCallback p_311586_, BinaryOperator<CommandResultCallback> p_81338_) {
      CommandResultCallback commandresultcallback = p_81338_.apply(this.resultCallback, p_311586_);
      return this.withCallback(commandresultcallback);
   }

   public CommandSourceStack withSuppressedOutput() {
      return !this.silent && !this.source.alwaysAccepts()
         ? new CommandSourceStack(
            this.source,
            this.worldPosition,
            this.rotation,
            this.level,
            this.permissions,
            this.textName,
            this.displayName,
            this.server,
            this.entity,
            true,
            this.resultCallback,
            this.anchor,
            this.signingContext,
            this.chatMessageChainer
         )
         : this;
   }

   public CommandSourceStack withPermission(PermissionSet p_454972_) {
      return p_454972_ == this.permissions
         ? this
         : new CommandSourceStack(
            this.source,
            this.worldPosition,
            this.rotation,
            this.level,
            p_454972_,
            this.textName,
            this.displayName,
            this.server,
            this.entity,
            this.silent,
            this.resultCallback,
            this.anchor,
            this.signingContext,
            this.chatMessageChainer
         );
   }

   public CommandSourceStack withMaximumPermission(PermissionSet p_451751_) {
      return this.withPermission(this.permissions.union(p_451751_));
   }

   public CommandSourceStack withAnchor(EntityAnchorArgument.Anchor p_81351_) {
      return p_81351_ == this.anchor
         ? this
         : new CommandSourceStack(
            this.source,
            this.worldPosition,
            this.rotation,
            this.level,
            this.permissions,
            this.textName,
            this.displayName,
            this.server,
            this.entity,
            this.silent,
            this.resultCallback,
            p_81351_,
            this.signingContext,
            this.chatMessageChainer
         );
   }

   public CommandSourceStack withLevel(ServerLevel p_81328_) {
      if (p_81328_ == this.level) {
         return this;
      }

      double d0 = DimensionType.getTeleportationScale(this.level.dimensionType(), p_81328_.dimensionType());
      Vec3 vec3 = new Vec3(this.worldPosition.x * d0, this.worldPosition.y, this.worldPosition.z * d0);
      return new CommandSourceStack(
         this.source,
         vec3,
         this.rotation,
         p_81328_,
         this.permissions,
         this.textName,
         this.displayName,
         this.server,
         this.entity,
         this.silent,
         this.resultCallback,
         this.anchor,
         this.signingContext,
         this.chatMessageChainer
      );
   }

   public CommandSourceStack facing(Entity p_81332_, EntityAnchorArgument.Anchor p_81333_) {
      return this.facing(p_81333_.apply(p_81332_));
   }

   public CommandSourceStack facing(Vec3 p_81365_) {
      Vec3 vec3 = this.anchor.apply(this);
      double d0 = p_81365_.x - vec3.x;
      double d1 = p_81365_.y - vec3.y;
      double d2 = p_81365_.z - vec3.z;
      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      float f = Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * 180.0 / (float) Math.PI)));
      float f1 = Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * 180.0 / (float) Math.PI) - 90.0F);
      return this.withRotation(new Vec2(f, f1));
   }

   public CommandSourceStack withSigningContext(CommandSigningContext p_230894_, TaskChainer p_301144_) {
      return p_230894_ == this.signingContext && p_301144_ == this.chatMessageChainer
         ? this
         : new CommandSourceStack(
            this.source,
            this.worldPosition,
            this.rotation,
            this.level,
            this.permissions,
            this.textName,
            this.displayName,
            this.server,
            this.entity,
            this.silent,
            this.resultCallback,
            this.anchor,
            p_230894_,
            p_301144_
         );
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public String getTextName() {
      return this.textName;
   }

   @Override
   public PermissionSet permissions() {
      return this.permissions;
   }

   public Vec3 getPosition() {
      return this.worldPosition;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public @Nullable Entity getEntity() {
      return this.entity;
   }

   public Entity getEntityOrException() throws CommandSyntaxException {
      if (this.entity == null) {
         throw ERROR_NOT_ENTITY.create();
      } else {
         return this.entity;
      }
   }

   public ServerPlayer getPlayerOrException() throws CommandSyntaxException {
      if (this.entity instanceof ServerPlayer serverplayer) {
         return serverplayer;
      } else {
         throw ERROR_NOT_PLAYER.create();
      }
   }

   public @Nullable ServerPlayer getPlayer() {
      return this.entity instanceof ServerPlayer serverplayer ? serverplayer : null;
   }

   public boolean isPlayer() {
      return this.entity instanceof ServerPlayer;
   }

   public Vec2 getRotation() {
      return this.rotation;
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   public EntityAnchorArgument.Anchor getAnchor() {
      return this.anchor;
   }

   public CommandSigningContext getSigningContext() {
      return this.signingContext;
   }

   public TaskChainer getChatMessageChainer() {
      return this.chatMessageChainer;
   }

   public boolean shouldFilterMessageTo(ServerPlayer p_243268_) {
      ServerPlayer serverplayer = this.getPlayer();
      return p_243268_ == serverplayer ? false : serverplayer != null && serverplayer.isTextFilteringEnabled() || p_243268_.isTextFilteringEnabled();
   }

   public void sendChatMessage(OutgoingChatMessage p_251464_, boolean p_252146_, ChatType.Bound p_250406_) {
      if (!this.silent) {
         ServerPlayer serverplayer = this.getPlayer();
         if (serverplayer != null) {
            serverplayer.sendChatMessage(p_251464_, p_252146_, p_250406_);
         } else {
            this.source.sendSystemMessage(p_250406_.decorate(p_251464_.content()));
         }
      }
   }

   public void sendSystemMessage(Component p_243331_) {
      if (!this.silent) {
         ServerPlayer serverplayer = this.getPlayer();
         if (serverplayer != null) {
            serverplayer.sendSystemMessage(p_243331_);
         } else {
            this.source.sendSystemMessage(p_243331_);
         }
      }
   }

   public void sendSuccess(Supplier<Component> p_288979_, boolean p_289007_) {
      boolean flag = this.source.acceptsSuccess() && !this.silent;
      boolean flag1 = p_289007_ && this.source.shouldInformAdmins() && !this.silent;
      if (flag || flag1) {
         Component component = p_288979_.get();
         if (flag) {
            this.source.sendSystemMessage(component);
         }

         if (flag1) {
            this.broadcastToAdmins(component);
         }
      }
   }

   private void broadcastToAdmins(Component p_81367_) {
      Component component = Component.translatable("chat.type.admin", this.getDisplayName(), p_81367_).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
      GameRules gamerules = this.level.getGameRules();
      if (gamerules.get(GameRules.SEND_COMMAND_FEEDBACK)) {
         for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            if (serverplayer.commandSource() != this.source && this.server.getPlayerList().isOp(serverplayer.nameAndId())) {
               serverplayer.sendSystemMessage(component);
            }
         }
      }

      if (this.source != this.server && gamerules.get(GameRules.LOG_ADMIN_COMMANDS)) {
         this.server.sendSystemMessage(component);
      }
   }

   public void sendFailure(Component p_81353_) {
      if (this.source.acceptsFailure() && !this.silent) {
         this.source.sendSystemMessage(Component.empty().append(p_81353_).withStyle(ChatFormatting.RED));
      }
   }

   @Override
   public CommandResultCallback callback() {
      return this.resultCallback;
   }

   @Override
   public Collection<String> getOnlinePlayerNames() {
      return Lists.newArrayList(this.server.getPlayerNames());
   }

   @Override
   public Collection<String> getAllTeams() {
      return this.server.getScoreboard().getTeamNames();
   }

   @Override
   public Stream<Identifier> getAvailableSounds() {
      return BuiltInRegistries.SOUND_EVENT.stream().map(SoundEvent::location);
   }

   @Override
   public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> p_212324_) {
      return Suggestions.empty();
   }

   @Override
   public CompletableFuture<Suggestions> suggestRegistryElements(
      ResourceKey<? extends Registry<?>> p_212330_,
      SharedSuggestionProvider.ElementSuggestionType p_212331_,
      SuggestionsBuilder p_212332_,
      CommandContext<?> p_212333_
   ) {
      if (p_212330_ == Registries.RECIPE) {
         return SharedSuggestionProvider.suggestResource(
            this.server.getRecipeManager().getRecipes().stream().map(p_448473_ -> p_448473_.id().identifier()), p_212332_
         );
      } else if (p_212330_ == Registries.ADVANCEMENT) {
         Collection<AdvancementHolder> collection = this.server.getAdvancements().getAllAdvancements();
         return SharedSuggestionProvider.suggestResource(collection.stream().map(AdvancementHolder::id), p_212332_);
      } else {
         return this.getLookup(p_212330_).map(p_405038_ -> {
            this.suggestRegistryElements((HolderLookup<?>)p_405038_, p_212331_, p_212332_);
            return p_212332_.buildFuture();
         }).orElseGet(Suggestions::empty);
      }
   }

   private Optional<? extends HolderLookup<?>> getLookup(ResourceKey<? extends Registry<?>> p_406249_) {
      Optional<? extends Registry<?>> optional = this.registryAccess().lookup(p_406249_);
      return optional.isPresent() ? optional : this.server.reloadableRegistries().lookup().lookup(p_406249_);
   }

   @Override
   public Set<ResourceKey<Level>> levels() {
      return this.server.levelKeys();
   }

   @Override
   public RegistryAccess registryAccess() {
      return this.server.registryAccess();
   }

   @Override
   public FeatureFlagSet enabledFeatures() {
      return this.level.enabledFeatures();
   }

   @Override
   public CommandDispatcher<CommandSourceStack> dispatcher() {
      return this.getServer().getFunctions().getDispatcher();
   }

   @Override
   public void handleError(CommandExceptionType p_311431_, Message p_311914_, boolean p_312997_, @Nullable TraceCallbacks p_310681_) {
      if (p_310681_ != null) {
         p_310681_.onError(p_311914_.getString());
      }

      if (!p_312997_) {
         this.sendFailure(ComponentUtils.fromMessage(p_311914_));
      }
   }

   @Override
   public boolean isSilent() {
      return this.silent;
   }
}
