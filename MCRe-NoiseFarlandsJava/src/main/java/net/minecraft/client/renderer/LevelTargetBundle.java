package net.minecraft.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LevelTargetBundle implements PostChain.TargetBundle {
    public static final Identifier MAIN_TARGET_ID = PostChain.MAIN_TARGET_ID;
    public static final Identifier TRANSLUCENT_TARGET_ID = Identifier.withDefaultNamespace("translucent");
    public static final Identifier ITEM_ENTITY_TARGET_ID = Identifier.withDefaultNamespace("item_entity");
    public static final Identifier PARTICLES_TARGET_ID = Identifier.withDefaultNamespace("particles");
    public static final Identifier WEATHER_TARGET_ID = Identifier.withDefaultNamespace("weather");
    public static final Identifier CLOUDS_TARGET_ID = Identifier.withDefaultNamespace("clouds");
    public static final Identifier ENTITY_OUTLINE_TARGET_ID = Identifier.withDefaultNamespace("entity_outline");
    public static final Set<Identifier> MAIN_TARGETS = Set.of(MAIN_TARGET_ID);
    public static final Set<Identifier> OUTLINE_TARGETS = Set.of(MAIN_TARGET_ID, ENTITY_OUTLINE_TARGET_ID);
    public static final Set<Identifier> SORTING_TARGETS = Set.of(
        MAIN_TARGET_ID, TRANSLUCENT_TARGET_ID, ITEM_ENTITY_TARGET_ID, PARTICLES_TARGET_ID, WEATHER_TARGET_ID, CLOUDS_TARGET_ID
    );
    public ResourceHandle<RenderTarget> main = ResourceHandle.invalid();
    public @Nullable ResourceHandle<RenderTarget> translucent;
    public @Nullable ResourceHandle<RenderTarget> itemEntity;
    public @Nullable ResourceHandle<RenderTarget> particles;
    public @Nullable ResourceHandle<RenderTarget> weather;
    public @Nullable ResourceHandle<RenderTarget> clouds;
    public @Nullable ResourceHandle<RenderTarget> entityOutline;

    @Override
    public void replace(final Identifier id, final ResourceHandle<RenderTarget> handle) {
        if (id.equals(MAIN_TARGET_ID)) {
            this.main = handle;
        } else if (id.equals(TRANSLUCENT_TARGET_ID)) {
            this.translucent = handle;
        } else if (id.equals(ITEM_ENTITY_TARGET_ID)) {
            this.itemEntity = handle;
        } else if (id.equals(PARTICLES_TARGET_ID)) {
            this.particles = handle;
        } else if (id.equals(WEATHER_TARGET_ID)) {
            this.weather = handle;
        } else if (id.equals(CLOUDS_TARGET_ID)) {
            this.clouds = handle;
        } else {
            if (!id.equals(ENTITY_OUTLINE_TARGET_ID)) {
                throw new IllegalArgumentException("No target with id " + id);
            }

            this.entityOutline = handle;
        }
    }

    @Override
    public @Nullable ResourceHandle<RenderTarget> get(final Identifier id) {
        if (id.equals(MAIN_TARGET_ID)) {
            return this.main;
        } else if (id.equals(TRANSLUCENT_TARGET_ID)) {
            return this.translucent;
        } else if (id.equals(ITEM_ENTITY_TARGET_ID)) {
            return this.itemEntity;
        } else if (id.equals(PARTICLES_TARGET_ID)) {
            return this.particles;
        } else if (id.equals(WEATHER_TARGET_ID)) {
            return this.weather;
        } else if (id.equals(CLOUDS_TARGET_ID)) {
            return this.clouds;
        } else {
            return id.equals(ENTITY_OUTLINE_TARGET_ID) ? this.entityOutline : null;
        }
    }

    public void clear() {
        this.main = ResourceHandle.invalid();
        this.translucent = null;
        this.itemEntity = null;
        this.particles = null;
        this.weather = null;
        this.clouds = null;
        this.entityOutline = null;
    }
}