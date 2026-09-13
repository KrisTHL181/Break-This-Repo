package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientSuggestionProvider implements SharedSuggestionProvider {
    private final ClientPacketListener connection;
    private final Minecraft minecraft;
    private int pendingSuggestionsId = -1;
    private @Nullable CompletableFuture<Suggestions> pendingSuggestionsFuture;
    private final Set<String> customCompletionSuggestions = new HashSet<>();
    private final PermissionSet permissions;

    public ClientSuggestionProvider(final ClientPacketListener connection, final Minecraft minecraft, final PermissionSet permissions) {
        this.connection = connection;
        this.minecraft = minecraft;
        this.permissions = permissions;
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        List<String> result = Lists.newArrayList();

        for (PlayerInfo info : this.connection.getOnlinePlayers()) {
            result.add(info.getProfile().name());
        }

        return result;
    }

    @Override
    public Collection<String> getCustomTabSuggestions() {
        if (this.customCompletionSuggestions.isEmpty()) {
            return this.getOnlinePlayerNames();
        }

        Set<String> result = new HashSet<>(this.getOnlinePlayerNames());
        result.addAll(this.customCompletionSuggestions);
        return result;
    }

    @Override
    public Collection<String> getSelectedEntities() {
        return this.minecraft.hitResult != null && this.minecraft.hitResult.getType() == HitResult.Type.ENTITY
            ? Collections.singleton(((EntityHitResult)this.minecraft.hitResult).getEntity().getStringUUID())
            : Collections.emptyList();
    }

    @Override
    public Collection<String> getAllTeams() {
        return this.connection.scoreboard().getTeamNames();
    }

    @Override
    public Stream<Identifier> getAvailableSounds() {
        return this.minecraft.getSoundManager().getAvailableSounds().stream();
    }

    @Override
    public PermissionSet permissions() {
        return this.permissions;
    }

    @Override
    public CompletableFuture<Suggestions> suggestRegistryElements(
        final ResourceKey<? extends Registry<?>> key,
        final SharedSuggestionProvider.ElementSuggestionType elements,
        final SuggestionsBuilder builder,
        final CommandContext<?> context
    ) {
        return this.registryAccess().lookup(key).map(registry -> {
            this.suggestRegistryElements(registry, elements, builder);
            return builder.buildFuture();
        }).orElseGet(() -> this.customSuggestion(context));
    }

    @Override
    public CompletableFuture<Suggestions> customSuggestion(final CommandContext<?> context) {
        if (this.pendingSuggestionsFuture != null) {
            this.pendingSuggestionsFuture.cancel(false);
        }

        this.pendingSuggestionsFuture = new CompletableFuture<>();
        int id = ++this.pendingSuggestionsId;
        this.connection.send(new ServerboundCommandSuggestionPacket(id, context.getInput()));
        return this.pendingSuggestionsFuture;
    }

    private static String prettyPrint(final double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String prettyPrint(final int value) {
        return Integer.toString(value);
    }

    @Override
    public Collection<SharedSuggestionProvider.TextCoordinates> getRelevantCoordinates() {
        HitResult hitResult = this.minecraft.hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
            return Collections.singleton(
                new SharedSuggestionProvider.TextCoordinates(prettyPrint(pos.getX()), prettyPrint(pos.getY()), prettyPrint(pos.getZ()))
            );
        } else {
            return SharedSuggestionProvider.super.getRelevantCoordinates();
        }
    }

    @Override
    public Collection<SharedSuggestionProvider.TextCoordinates> getAbsoluteCoordinates() {
        HitResult hitResult = this.minecraft.hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            Vec3 pos = hitResult.getLocation();
            return Collections.singleton(new SharedSuggestionProvider.TextCoordinates(prettyPrint(pos.x), prettyPrint(pos.y), prettyPrint(pos.z)));
        } else {
            return SharedSuggestionProvider.super.getAbsoluteCoordinates();
        }
    }

    @Override
    public Set<ResourceKey<Level>> levels() {
        return this.connection.levels();
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.connection.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.connection.enabledFeatures();
    }

    public void completeCustomSuggestions(final int id, final Suggestions result) {
        if (id == this.pendingSuggestionsId) {
            this.pendingSuggestionsFuture.complete(result);
            this.pendingSuggestionsFuture = null;
            this.pendingSuggestionsId = -1;
        }
    }

    public void modifyCustomCompletions(final ClientboundCustomChatCompletionsPacket.Action action, final List<String> entries) {
        switch (action) {
            case ADD:
                this.customCompletionSuggestions.addAll(entries);
                break;
            case REMOVE:
                entries.forEach(this.customCompletionSuggestions::remove);
                break;
            case SET:
                this.customCompletionSuggestions.clear();
                this.customCompletionSuggestions.addAll(entries);
        }
    }
}