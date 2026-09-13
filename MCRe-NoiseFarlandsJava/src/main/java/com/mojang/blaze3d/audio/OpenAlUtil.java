package com.mojang.blaze3d.audio;

import com.mojang.logging.LogUtils;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class OpenAlUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static String alErrorToString(final int error) {
        return switch (error) {
            case 40961 -> "Invalid name parameter.";
            case 40962 -> "Invalid enumerated parameter value.";
            case 40963 -> "Invalid parameter parameter value.";
            case 40964 -> "Invalid operation.";
            case 40965 -> "Unable to allocate memory.";
            default -> "An unrecognized error occurred.";
        };
    }

    public static boolean checkALError(final String location) {
        int error = AL10.alGetError();
        if (error != 0) {
            LOGGER.error("{}: {}", location, alErrorToString(error));
            return true;
        } else {
            return false;
        }
    }

    private static String alcErrorToString(final int error) {
        return switch (error) {
            case 40961 -> "Invalid device.";
            case 40962 -> "Invalid context.";
            case 40963 -> "Illegal enum.";
            case 40964 -> "Invalid value.";
            case 40965 -> "Unable to allocate memory.";
            default -> "An unrecognized error occurred.";
        };
    }

    public static boolean checkALCError(final long device, final String location) {
        int error = ALC10.alcGetError(device);
        if (error != 0) {
            LOGGER.error("{} ({}): {}", location, device, alcErrorToString(error));
            return true;
        } else {
            return false;
        }
    }

    public static int audioFormatToOpenAl(final AudioFormat audioFormat) {
        Encoding encoding = audioFormat.getEncoding();
        int channels = audioFormat.getChannels();
        int sampleSizeInBits = audioFormat.getSampleSizeInBits();
        if (encoding.equals(Encoding.PCM_UNSIGNED) || encoding.equals(Encoding.PCM_SIGNED)) {
            if (channels == 1) {
                if (sampleSizeInBits == 8) {
                    return 4352;
                }

                if (sampleSizeInBits == 16) {
                    return 4353;
                }
            } else if (channels == 2) {
                if (sampleSizeInBits == 8) {
                    return 4354;
                }

                if (sampleSizeInBits == 16) {
                    return 4355;
                }
            }
        }

        throw new IllegalArgumentException("Invalid audio format: " + audioFormat);
    }
}