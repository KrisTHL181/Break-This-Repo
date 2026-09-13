package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockEntityRenderers {
    private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?, ?>> PROVIDERS = Maps.newHashMap();

    private static <T extends BlockEntity, S extends BlockEntityRenderState> void register(
        final BlockEntityType<? extends T> type, final BlockEntityRendererProvider<T, S> renderer
    ) {
        PROVIDERS.put(type, renderer);
    }

    public static Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> createEntityRenderers(final BlockEntityRendererProvider.Context context) {
        Builder<BlockEntityType<?>, BlockEntityRenderer<?, ?>> result = ImmutableMap.builder();
        PROVIDERS.forEach((type, provider) -> {
            try {
                result.put((BlockEntityType<?>)type, provider.create(context));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create model for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>)type), e);
            }
        });
        return result.build();
    }

    static {
        register(BlockEntityTypes.SIGN, StandingSignRenderer::new);
        register(BlockEntityTypes.HANGING_SIGN, HangingSignRenderer::new);
        register(BlockEntityTypes.MOB_SPAWNER, SpawnerRenderer::new);
        register(BlockEntityTypes.PISTON, var0 -> new PistonHeadRenderer());
        register(BlockEntityTypes.CHEST, ChestRenderer::new);
        register(BlockEntityTypes.ENDER_CHEST, ChestRenderer::new);
        register(BlockEntityTypes.TRAPPED_CHEST, ChestRenderer::new);
        register(BlockEntityTypes.ENCHANTING_TABLE, EnchantTableRenderer::new);
        register(BlockEntityTypes.LECTERN, LecternRenderer::new);
        register(BlockEntityTypes.END_PORTAL, var0 -> new TheEndPortalRenderer());
        register(BlockEntityTypes.END_GATEWAY, var0 -> new TheEndGatewayRenderer());
        register(BlockEntityTypes.BEACON, var0 -> new BeaconRenderer());
        register(BlockEntityTypes.SKULL, SkullBlockRenderer::new);
        register(BlockEntityTypes.BANNER, BannerRenderer::new);
        register(BlockEntityTypes.STRUCTURE_BLOCK, var0 -> new BlockEntityWithBoundingBoxRenderer());
        register(BlockEntityTypes.TEST_INSTANCE_BLOCK, var0 -> new TestInstanceRenderer());
        register(BlockEntityTypes.SHULKER_BOX, ShulkerBoxRenderer::new);
        register(BlockEntityTypes.CONDUIT, ConduitRenderer::new);
        register(BlockEntityTypes.BELL, BellRenderer::new);
        register(BlockEntityTypes.CAMPFIRE, CampfireRenderer::new);
        register(BlockEntityTypes.BRUSHABLE_BLOCK, BrushableBlockRenderer::new);
        register(BlockEntityTypes.DECORATED_POT, DecoratedPotRenderer::new);
        register(BlockEntityTypes.TRIAL_SPAWNER, TrialSpawnerRenderer::new);
        register(BlockEntityTypes.VAULT, VaultRenderer::new);
        register(BlockEntityTypes.COPPER_GOLEM_STATUE, CopperGolemStatueBlockRenderer::new);
        register(BlockEntityTypes.SHELF, ShelfRenderer::new);
    }
}