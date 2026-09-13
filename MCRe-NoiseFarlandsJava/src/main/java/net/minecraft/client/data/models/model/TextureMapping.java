package net.minecraft.client.data.models.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureMapping {
    private final Map<TextureSlot, Material> slots = Maps.newHashMap();
    private final Set<TextureSlot> forcedSlots = Sets.newHashSet();

    public TextureMapping put(final TextureSlot slot, final Material material) {
        this.slots.put(slot, material);
        return this;
    }

    public TextureMapping putForced(final TextureSlot slot, final Material material) {
        this.slots.put(slot, material);
        this.forcedSlots.add(slot);
        return this;
    }

    public Stream<TextureSlot> getForced() {
        return this.forcedSlots.stream();
    }

    public TextureMapping copySlot(final TextureSlot from, final TextureSlot to) {
        return this.put(to, this.slots.get(from));
    }

    public TextureMapping copyForced(final TextureSlot from, final TextureSlot to) {
        return this.putForced(to, this.slots.get(from));
    }

    public Material get(final TextureSlot slot) {
        for (TextureSlot currentSlot = slot; currentSlot != null; currentSlot = currentSlot.getParent()) {
            Material result = this.slots.get(currentSlot);
            if (result != null) {
                return result;
            }
        }

        throw new IllegalStateException("Can't find texture for slot " + slot);
    }

    public TextureMapping copyAndUpdate(final TextureSlot slot, final Material material) {
        TextureMapping result = new TextureMapping();
        result.slots.putAll(this.slots);
        result.forcedSlots.addAll(this.forcedSlots);
        result.put(slot, material);
        return result;
    }

    public TextureMapping updateSlots(final BiFunction<TextureSlot, Material, Material> mapper) {
        this.slots.replaceAll(mapper);
        return this;
    }

    public TextureMapping forceAllTranslucent() {
        return this.updateSlots((var0, material) -> material.withForceTranslucent(true));
    }

    public static TextureMapping cube(final Block block) {
        Material texture = getBlockTexture(block);
        return cube(texture);
    }

    public static TextureMapping defaultTexture(final Block block) {
        Material texture = getBlockTexture(block);
        return defaultTexture(texture);
    }

    public static TextureMapping defaultTexture(final Material texture) {
        return new TextureMapping().put(TextureSlot.TEXTURE, texture);
    }

    public static TextureMapping cube(final Material all) {
        return new TextureMapping().put(TextureSlot.ALL, all);
    }

    public static TextureMapping cross(final Block block) {
        return singleSlot(TextureSlot.CROSS, getBlockTexture(block));
    }

    public static TextureMapping side(final Block block) {
        return singleSlot(TextureSlot.SIDE, getBlockTexture(block));
    }

    public static TextureMapping crossEmissive(final Block block) {
        return new TextureMapping().put(TextureSlot.CROSS, getBlockTexture(block)).put(TextureSlot.CROSS_EMISSIVE, getBlockTexture(block, "_emissive"));
    }

    public static TextureMapping cross(final Material cross) {
        return singleSlot(TextureSlot.CROSS, cross);
    }

    public static TextureMapping plant(final Block block) {
        return singleSlot(TextureSlot.PLANT, getBlockTexture(block));
    }

    public static TextureMapping plantEmissive(final Block block) {
        return new TextureMapping().put(TextureSlot.PLANT, getBlockTexture(block)).put(TextureSlot.CROSS_EMISSIVE, getBlockTexture(block, "_emissive"));
    }

    public static TextureMapping plant(final Material plant) {
        return singleSlot(TextureSlot.PLANT, plant);
    }

    public static TextureMapping rail(final Block block) {
        return singleSlot(TextureSlot.RAIL, getBlockTexture(block));
    }

    public static TextureMapping rail(final Material rail) {
        return singleSlot(TextureSlot.RAIL, rail);
    }

    public static TextureMapping wool(final Block block) {
        return singleSlot(TextureSlot.WOOL, getBlockTexture(block));
    }

    public static TextureMapping flowerbed(final Block block) {
        return new TextureMapping().put(TextureSlot.FLOWERBED, getBlockTexture(block)).put(TextureSlot.STEM, getBlockTexture(block, "_stem"));
    }

    public static TextureMapping wool(final Material cross) {
        return singleSlot(TextureSlot.WOOL, cross);
    }

    public static TextureMapping stem(final Block block) {
        return singleSlot(TextureSlot.STEM, getBlockTexture(block));
    }

    public static TextureMapping attachedStem(final Block stem, final Block upperStem) {
        return new TextureMapping().put(TextureSlot.STEM, getBlockTexture(stem)).put(TextureSlot.UPPER_STEM, getBlockTexture(upperStem));
    }

    public static TextureMapping pattern(final Block block) {
        return singleSlot(TextureSlot.PATTERN, getBlockTexture(block));
    }

    public static TextureMapping fan(final Block block) {
        return singleSlot(TextureSlot.FAN, getBlockTexture(block));
    }

    public static TextureMapping crop(final Material id) {
        return singleSlot(TextureSlot.CROP, id);
    }

    public static TextureMapping pane(final Block body, final Block edge) {
        return new TextureMapping().put(TextureSlot.PANE, getBlockTexture(body)).put(TextureSlot.EDGE, getBlockTexture(edge, "_top"));
    }

    public static TextureMapping singleSlot(final TextureSlot slot, final Material id) {
        return new TextureMapping().put(slot, id);
    }

    public static TextureMapping column(final Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.END, getBlockTexture(block, "_top"));
    }

    public static TextureMapping cubeTop(final Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping pottedAzalea(final Block block) {
        return new TextureMapping()
            .put(TextureSlot.PLANT, getBlockTexture(block, "_plant"))
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping logColumn(final Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block))
            .put(TextureSlot.END, getBlockTexture(block, "_top"))
            .put(TextureSlot.PARTICLE, getBlockTexture(block));
    }

    public static TextureMapping column(final Material side, final Material end) {
        return new TextureMapping().put(TextureSlot.SIDE, side).put(TextureSlot.END, end);
    }

    public static TextureMapping fence(final Block block) {
        return new TextureMapping()
            .put(TextureSlot.TEXTURE, getBlockTexture(block))
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping customParticle(final Block block) {
        return new TextureMapping().put(TextureSlot.TEXTURE, getBlockTexture(block)).put(TextureSlot.PARTICLE, getBlockTexture(block, "_particle"));
    }

    public static TextureMapping cubeBottomTop(final Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping cubeBottomTopWithWall(final Block block) {
        Material side = getBlockTexture(block);
        return new TextureMapping()
            .put(TextureSlot.WALL, side)
            .put(TextureSlot.SIDE, side)
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping columnWithWall(final Block block) {
        Material side = getBlockTexture(block);
        return new TextureMapping()
            .put(TextureSlot.TEXTURE, side)
            .put(TextureSlot.WALL, side)
            .put(TextureSlot.SIDE, side)
            .put(TextureSlot.END, getBlockTexture(block, "_top"));
    }

    public static TextureMapping door(final Material top, final Material bottom) {
        return new TextureMapping().put(TextureSlot.TOP, top).put(TextureSlot.BOTTOM, bottom);
    }

    public static TextureMapping door(final Block block) {
        return new TextureMapping().put(TextureSlot.TOP, getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping bed(final Block block, final BedPart part) {
        TextureMapping mapping = new TextureMapping()
            .put(TextureSlot.UP, getBlockTexture(block, "_" + part + "_up"))
            .put(TextureSlot.EAST, getBlockTexture(block, "_" + part + "_east"))
            .put(TextureSlot.WEST, getBlockTexture(block, "_" + part + "_west"));
        if (part == BedPart.FOOT) {
            mapping.put(TextureSlot.SOUTH, getBlockTexture(block, "_" + part + "_south"));
        }

        return mapping;
    }

    public static TextureMapping particle(final Block block) {
        return new TextureMapping().put(TextureSlot.PARTICLE, getBlockTexture(block));
    }

    public static TextureMapping particle(final Material id) {
        return new TextureMapping().put(TextureSlot.PARTICLE, id);
    }

    public static TextureMapping fire0(final Block block) {
        return new TextureMapping().put(TextureSlot.FIRE, getBlockTexture(block, "_0"));
    }

    public static TextureMapping fire1(final Block block) {
        return new TextureMapping().put(TextureSlot.FIRE, getBlockTexture(block, "_1"));
    }

    public static TextureMapping lantern(final Block block) {
        return new TextureMapping().put(TextureSlot.LANTERN, getBlockTexture(block));
    }

    public static TextureMapping torch(final Block block) {
        return new TextureMapping().put(TextureSlot.TORCH, getBlockTexture(block));
    }

    public static TextureMapping torch(final Material id) {
        return new TextureMapping().put(TextureSlot.TORCH, id);
    }

    public static TextureMapping trialSpawner(final Block block, final String sideSuffix, final String topSuffix) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, sideSuffix))
            .put(TextureSlot.TOP, getBlockTexture(block, topSuffix))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping vault(final Block block, final String frontSuffix, final String sideSuffix, final String topSuffix, final String bottomSuffix) {
        return new TextureMapping()
            .put(TextureSlot.FRONT, getBlockTexture(block, frontSuffix))
            .put(TextureSlot.SIDE, getBlockTexture(block, sideSuffix))
            .put(TextureSlot.TOP, getBlockTexture(block, topSuffix))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, bottomSuffix));
    }

    public static TextureMapping particleFromItem(final Item item) {
        return new TextureMapping().put(TextureSlot.PARTICLE, getItemTexture(item));
    }

    public static TextureMapping commandBlock(final Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(block, "_front"))
            .put(TextureSlot.BACK, getBlockTexture(block, "_back"));
    }

    public static TextureMapping orientableCube(final Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(block, "_front"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping orientableCubeOnlyTop(final Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(block, "_front"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping orientableCubeSameEnds(final Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(block, "_front"))
            .put(TextureSlot.END, getBlockTexture(block, "_end"));
    }

    public static TextureMapping top(final Block block) {
        return new TextureMapping().put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping craftingTable(final Block table, final Block bottomWood) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(table, "_front"))
            .put(TextureSlot.DOWN, getBlockTexture(bottomWood))
            .put(TextureSlot.UP, getBlockTexture(table, "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(table, "_front"))
            .put(TextureSlot.EAST, getBlockTexture(table, "_side"))
            .put(TextureSlot.SOUTH, getBlockTexture(table, "_side"))
            .put(TextureSlot.WEST, getBlockTexture(table, "_front"));
    }

    public static TextureMapping fletchingTable(final Block table, final Block bottomWood) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(table, "_front"))
            .put(TextureSlot.DOWN, getBlockTexture(bottomWood))
            .put(TextureSlot.UP, getBlockTexture(table, "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(table, "_front"))
            .put(TextureSlot.SOUTH, getBlockTexture(table, "_front"))
            .put(TextureSlot.EAST, getBlockTexture(table, "_side"))
            .put(TextureSlot.WEST, getBlockTexture(table, "_side"));
    }

    public static TextureMapping snifferEgg(final String suffix) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_north"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_bottom"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_north"))
            .put(TextureSlot.SOUTH, getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_south"))
            .put(TextureSlot.EAST, getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_east"))
            .put(TextureSlot.WEST, getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_west"));
    }

    public static TextureMapping driedGhast(final String suffix) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.DRIED_GHAST, suffix + "_north"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.DRIED_GHAST, suffix + "_bottom"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.DRIED_GHAST, suffix + "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(Blocks.DRIED_GHAST, suffix + "_north"))
            .put(TextureSlot.SOUTH, getBlockTexture(Blocks.DRIED_GHAST, suffix + "_south"))
            .put(TextureSlot.EAST, getBlockTexture(Blocks.DRIED_GHAST, suffix + "_east"))
            .put(TextureSlot.WEST, getBlockTexture(Blocks.DRIED_GHAST, suffix + "_west"))
            .put(TextureSlot.TENTACLES, getBlockTexture(Blocks.DRIED_GHAST, suffix + "_tentacles"));
    }

    public static TextureMapping campfire(final Block campfire) {
        return new TextureMapping().put(TextureSlot.LIT_LOG, getBlockTexture(campfire, "_log_lit")).put(TextureSlot.FIRE, getBlockTexture(campfire, "_fire"));
    }

    public static TextureMapping candleCake(final Block block, final boolean lit) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAKE, "_side"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAKE, "_bottom"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.CAKE, "_top"))
            .put(TextureSlot.SIDE, getBlockTexture(Blocks.CAKE, "_side"))
            .put(TextureSlot.CANDLE, getBlockTexture(block, lit ? "_lit" : ""));
    }

    public static TextureMapping cauldron(final Material contentTextureLoc) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAULDRON, "_side"))
            .put(TextureSlot.SIDE, getBlockTexture(Blocks.CAULDRON, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.CAULDRON, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAULDRON, "_bottom"))
            .put(TextureSlot.INSIDE, getBlockTexture(Blocks.CAULDRON, "_inner"))
            .put(TextureSlot.CONTENT, contentTextureLoc);
    }

    public static TextureMapping sculkShrieker(final boolean canSummon) {
        String innerTopString = canSummon ? "_can_summon" : "";
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom"))
            .put(TextureSlot.SIDE, getBlockTexture(Blocks.SCULK_SHRIEKER, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.SCULK_SHRIEKER, "_top"))
            .put(TextureSlot.INNER_TOP, getBlockTexture(Blocks.SCULK_SHRIEKER, innerTopString + "_inner_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom"));
    }

    public static TextureMapping bars(final Block block) {
        return new TextureMapping().put(TextureSlot.BARS, getBlockTexture(block)).put(TextureSlot.EDGE, getBlockTexture(block));
    }

    public static TextureMapping layer0(final Item item) {
        return new TextureMapping().put(TextureSlot.LAYER0, getItemTexture(item));
    }

    public static TextureMapping layer0(final Block block) {
        return new TextureMapping().put(TextureSlot.LAYER0, getBlockTexture(block));
    }

    public static TextureMapping layer0(final Material id) {
        return new TextureMapping().put(TextureSlot.LAYER0, id);
    }

    public static TextureMapping layered(final Material layer0, final Material layer1) {
        return new TextureMapping().put(TextureSlot.LAYER0, layer0).put(TextureSlot.LAYER1, layer1);
    }

    public static TextureMapping layered(final Material layer0, final Material layer1, final Material layer2) {
        return new TextureMapping().put(TextureSlot.LAYER0, layer0).put(TextureSlot.LAYER1, layer1).put(TextureSlot.LAYER2, layer2);
    }

    public static Material getBlockTexture(final Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return new Material(id.withPrefix("block/"));
    }

    public static Material getBlockTexture(final Block block, final String suffix) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return getBlockTexture(id, suffix);
    }

    private static Material getBlockTexture(final Identifier id, final String suffix) {
        return new Material(id.withPath(path -> "block/" + path + suffix));
    }

    public static Material getItemTexture(final Item block) {
        Identifier id = BuiltInRegistries.ITEM.getKey(block);
        return new Material(id.withPrefix("item/"));
    }

    public static Material getItemTexture(final Item item, final String suffix) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return new Material(id.withPath(path -> "item/" + path + suffix));
    }
}