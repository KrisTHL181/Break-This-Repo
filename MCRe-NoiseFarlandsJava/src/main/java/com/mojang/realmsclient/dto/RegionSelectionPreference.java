package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public enum RegionSelectionPreference {
    AUTOMATIC_PLAYER(0, "realms.configuration.region_preference.automatic_player"),
    AUTOMATIC_OWNER(1, "realms.configuration.region_preference.automatic_owner"),
    MANUAL(2, "");

    public static final RegionSelectionPreference DEFAULT_SELECTION = AUTOMATIC_PLAYER;
    public final int id;
    public final String translationKey;

    RegionSelectionPreference(final int id, final String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    @OnlyIn(Dist.CLIENT)
    public static class RegionSelectionPreferenceJsonAdapter extends TypeAdapter<RegionSelectionPreference> {
        private static final Logger LOGGER = LogUtils.getLogger();

        public void write(final JsonWriter jsonWriter, final RegionSelectionPreference regionSelectionPreference) throws IOException {
            jsonWriter.value(regionSelectionPreference.id);
        }

        public RegionSelectionPreference read(final JsonReader jsonReader) throws IOException {
            int id = jsonReader.nextInt();

            for (RegionSelectionPreference value : RegionSelectionPreference.values()) {
                if (value.id == id) {
                    return value;
                }
            }

            LOGGER.warn("Unsupported RegionSelectionPreference {}", id);
            return RegionSelectionPreference.DEFAULT_SELECTION;
        }
    }
}