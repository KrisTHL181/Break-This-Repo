package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;

public class GeodeLayerSettings {
    private static boolean expandNoiseValueRetrievalLimitMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.expandNoiseValueRetrievalLimit;
    }
    private static Codec<Double> LAYER_RANGE = createCodec();
    // 工厂方法：根据当前开关生成 Codec
    private static Codec<Double> createCodec() {
        if (expandNoiseValueRetrievalLimitMode()) {
            return Codec.doubleRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        } else {
            // 使用原版范围，自行替换为实际值
            return Codec.doubleRange(0.01, 50);
        }
    }

    // 当配置改变时，外部调用此方法刷新 Codec
    public static void refreshCodec() {
        LAYER_RANGE = createCodec();
    }
    public static final Codec<GeodeLayerSettings> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                LAYER_RANGE.optionalFieldOf("filling", 1.7).forGetter(c -> c.filling),
                LAYER_RANGE.optionalFieldOf("inner_layer", 2.2).forGetter(c -> c.innerLayer),
                LAYER_RANGE.optionalFieldOf("middle_layer", 3.2).forGetter(c -> c.middleLayer),
                LAYER_RANGE.optionalFieldOf("outer_layer", 4.2).forGetter(c -> c.outerLayer)
            )
            .apply(i, GeodeLayerSettings::new)
    );
    public final double filling;
    public final double innerLayer;
    public final double middleLayer;
    public final double outerLayer;

    public GeodeLayerSettings(final double filling, final double innerLayer, final double middleLayer, final double outerLayer) {
        this.filling = filling;
        this.innerLayer = innerLayer;
        this.middleLayer = middleLayer;
        this.outerLayer = outerLayer;
    }
}