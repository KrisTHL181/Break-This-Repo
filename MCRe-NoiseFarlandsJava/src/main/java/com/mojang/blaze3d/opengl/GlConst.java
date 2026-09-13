package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.BlendOp;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.AddressMode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlConst {
    public static final int GL_READ_FRAMEBUFFER = 36008;
    public static final int GL_DRAW_FRAMEBUFFER = 36009;
    public static final int GL_TRUE = 1;
    public static final int GL_FALSE = 0;
    public static final int GL_NONE = 0;
    public static final int GL_LINES = 1;
    public static final int GL_LINE_STRIP = 3;
    public static final int GL_TRIANGLE_STRIP = 5;
    public static final int GL_TRIANGLE_FAN = 6;
    public static final int GL_TRIANGLES = 4;
    public static final int GL_POINTS = 0;
    public static final int GL_WRITE_ONLY = 35001;
    public static final int GL_READ_ONLY = 35000;
    public static final int GL_READ_WRITE = 35002;
    public static final int GL_MAP_READ_BIT = 1;
    public static final int GL_MAP_WRITE_BIT = 2;
    public static final int GL_EQUAL = 514;
    public static final int GL_LEQUAL = 515;
    public static final int GL_LESS = 513;
    public static final int GL_GREATER = 516;
    public static final int GL_GEQUAL = 518;
    public static final int GL_ALWAYS = 519;
    public static final int GL_TEXTURE_MAG_FILTER = 10240;
    public static final int GL_TEXTURE_MIN_FILTER = 10241;
    public static final int GL_TEXTURE_WRAP_S = 10242;
    public static final int GL_TEXTURE_WRAP_T = 10243;
    public static final int GL_NEAREST = 9728;
    public static final int GL_LINEAR = 9729;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 9986;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 9987;
    public static final int GL_CLAMP_TO_EDGE = 33071;
    public static final int GL_REPEAT = 10497;
    public static final int GL_FRONT = 1028;
    public static final int GL_FRONT_AND_BACK = 1032;
    public static final int GL_LINE = 6913;
    public static final int GL_FILL = 6914;
    public static final int GL_BYTE = 5120;
    public static final int GL_UNSIGNED_BYTE = 5121;
    public static final int GL_SHORT = 5122;
    public static final int GL_UNSIGNED_SHORT = 5123;
    public static final int GL_INT = 5124;
    public static final int GL_UNSIGNED_INT = 5125;
    public static final int GL_FLOAT = 5126;
    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_COLOR = 768;
    public static final int GL_ONE_MINUS_SRC_COLOR = 769;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_DST_ALPHA = 772;
    public static final int GL_ONE_MINUS_DST_ALPHA = 773;
    public static final int GL_DST_COLOR = 774;
    public static final int GL_ONE_MINUS_DST_COLOR = 775;
    public static final int GL_REPLACE = 7681;
    public static final int GL_DEPTH_BUFFER_BIT = 256;
    public static final int GL_COLOR_BUFFER_BIT = 16384;
    public static final int GL_RGBA8 = 32856;
    public static final int GL_PROXY_TEXTURE_2D = 32868;
    public static final int GL_RGBA = 6408;
    public static final int GL_TEXTURE_WIDTH = 4096;
    public static final int GL_BGR = 32992;
    public static final int GL_FUNC_ADD = 32774;
    public static final int GL_MIN = 32775;
    public static final int GL_MAX = 32776;
    public static final int GL_FUNC_SUBTRACT = 32778;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 32779;
    public static final int GL_DEPTH_COMPONENT24 = 33190;
    public static final int GL_STATIC_DRAW = 35044;
    public static final int GL_DYNAMIC_DRAW = 35048;
    public static final int GL_STREAM_DRAW = 35040;
    public static final int GL_STATIC_READ = 35045;
    public static final int GL_DYNAMIC_READ = 35049;
    public static final int GL_STREAM_READ = 35041;
    public static final int GL_STATIC_COPY = 35046;
    public static final int GL_DYNAMIC_COPY = 35050;
    public static final int GL_STREAM_COPY = 35042;
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 37143;
    public static final int GL_TIMEOUT_EXPIRED = 37147;
    public static final int GL_WAIT_FAILED = 37149;
    public static final int GL_UNPACK_SWAP_BYTES = 3312;
    public static final int GL_UNPACK_LSB_FIRST = 3313;
    public static final int GL_UNPACK_ROW_LENGTH = 3314;
    public static final int GL_UNPACK_SKIP_ROWS = 3315;
    public static final int GL_UNPACK_SKIP_PIXELS = 3316;
    public static final int GL_UNPACK_ALIGNMENT = 3317;
    public static final int GL_PACK_ALIGNMENT = 3333;
    public static final int GL_PACK_ROW_LENGTH = 3330;
    public static final int GL_MAX_TEXTURE_SIZE = 3379;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int[] CUBEMAP_TARGETS = new int[]{34069, 34070, 34071, 34072, 34073, 34074};
    public static final int GL_DEPTH_COMPONENT = 6402;
    public static final int GL_DEPTH_COMPONENT32 = 33191;
    public static final int GL_FRAMEBUFFER = 36160;
    public static final int GL_RENDERBUFFER = 36161;
    public static final int GL_COLOR_ATTACHMENT0 = 36064;
    public static final int GL_DEPTH_ATTACHMENT = 36096;
    public static final int GL_FRAMEBUFFER_COMPLETE = 36053;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 36054;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 36055;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER = 36059;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER = 36060;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED = 36061;
    public static final int GL_LINK_STATUS = 35714;
    public static final int GL_COMPILE_STATUS = 35713;
    public static final int GL_VERTEX_SHADER = 35633;
    public static final int GL_FRAGMENT_SHADER = 35632;
    public static final int GL_TEXTURE0 = 33984;
    public static final int GL_TEXTURE1 = 33985;
    public static final int GL_TEXTURE2 = 33986;
    public static final int GL_TEXTURE_COMPARE_MODE = 34892;
    public static final int GL_ARRAY_BUFFER = 34962;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 34963;
    public static final int GL_PIXEL_PACK_BUFFER = 35051;
    public static final int GL_COPY_READ_BUFFER = 36662;
    public static final int GL_COPY_WRITE_BUFFER = 36663;
    public static final int GL_PIXEL_UNPACK_BUFFER = 35052;
    public static final int GL_UNIFORM_BUFFER = 35345;
    public static final int GL_RGB = 6407;
    public static final int GL_RG = 33319;
    public static final int GL_R8 = 33321;
    public static final int GL_RED = 6403;
    public static final int GL_OUT_OF_MEMORY = 1285;

    public static int toGl(final CompareOp compareOp) {
        return switch (compareOp) {
            case ALWAYS_PASS -> 519;
            case LESS_THAN -> 513;
            case LESS_THAN_OR_EQUAL -> 515;
            case EQUAL -> 514;
            case NOT_EQUAL -> 517;
            case GREATER_THAN_OR_EQUAL -> 518;
            case GREATER_THAN -> 516;
            case NEVER_PASS -> 512;
        };
    }

    public static int toGl(final PolygonMode polygonMode) {
        return switch (polygonMode) {
            case WIREFRAME -> 6913;
            default -> 6914;
        };
    }

    public static int toGl(final BlendFactor blendFactor) {
        return switch (blendFactor) {
            case CONSTANT_ALPHA -> 32771;
            case CONSTANT_COLOR -> 32769;
            case DST_ALPHA -> 772;
            case DST_COLOR -> 774;
            case ONE -> 1;
            case ONE_MINUS_CONSTANT_ALPHA -> 32772;
            case ONE_MINUS_CONSTANT_COLOR -> 32770;
            case ONE_MINUS_DST_ALPHA -> 773;
            case ONE_MINUS_DST_COLOR -> 775;
            case ONE_MINUS_SRC_ALPHA -> 771;
            case ONE_MINUS_SRC_COLOR -> 769;
            case SRC_ALPHA -> 770;
            case SRC_ALPHA_SATURATE -> 776;
            case SRC_COLOR -> 768;
            case ZERO -> 0;
        };
    }

    public static int toGl(final BlendOp blendOp) {
        return switch (blendOp) {
            case ADD -> 32774;
            case SUBTRACT -> 32778;
            case REVERSE_SUBTRACT -> 32779;
            case MIN -> 32775;
            case MAX -> 32776;
        };
    }

    public static int toGl(final PrimitiveTopology primitiveTopology) {
        return switch (primitiveTopology) {
            case LINES -> 4;
            case DEBUG_LINES -> 1;
            case DEBUG_LINE_STRIP -> 3;
            case POINTS -> 0;
            case TRIANGLES -> 4;
            case TRIANGLE_STRIP -> 5;
            case TRIANGLE_FAN -> 6;
            case QUADS -> 4;
        };
    }

    public static int toGl(final IndexType indexType) {
        return switch (indexType) {
            case SHORT -> 5123;
            case INT -> 5125;
        };
    }

    public static int toGl(final AddressMode addressMode) {
        return switch (addressMode) {
            case REPEAT -> 10497;
            case CLAMP_TO_EDGE -> 33071;
        };
    }

    public static int glFormatChannelCount(final int glExternalID) {
        if (glExternalID == 36249 || glExternalID == 6408) {
            return 4;
        } else if (glExternalID == 36248 || glExternalID == 6407) {
            return 3;
        } else if (glExternalID == 33320 || glExternalID == 33319) {
            return 2;
        } else {
            return glExternalID != 36244 && glExternalID != 6403 ? 0 : 1;
        }
    }

    public static boolean isGlFormatInteger(final int glExternalID) {
        return glExternalID == 36249 || glExternalID == 36248 || glExternalID == 33320 || glExternalID == 36244;
    }

    public static boolean isFormatNormalized(final GpuFormat gpuFormat) {
        return switch (gpuFormat) {
            case R8_UNORM, R8_SNORM, R16_UNORM, R16_SNORM, RG8_UNORM, RG8_SNORM, RG16_UNORM, RG16_SNORM, RGB8_UNORM, RGB8_SNORM, RGB16_UNORM, RGB16_SNORM, RGBA8_UNORM, RGBA8_SNORM, RGBA16_UNORM, RGB10A2_UNORM, D16_UNORM -> true;
            default -> false;
        };
    }

    public static int toGlInternalId(final GpuFormat gpuFormat) {
        return switch (gpuFormat) {
            case R8_UNORM -> 33321;
            case R8_SNORM -> 36756;
            case R16_UNORM -> 33322;
            case R16_SNORM -> 36760;
            case RG8_UNORM -> 33323;
            case RG8_SNORM -> 36757;
            case RG16_UNORM -> 33324;
            case RG16_SNORM -> 36761;
            default -> 0;
            case RGBA8_UNORM -> 32856;
            case RGBA8_SNORM -> 36759;
            case RGBA16_UNORM -> 32859;
            case RGB10A2_UNORM -> 32857;
            case D16_UNORM -> 33189;
            case RGBA16_SNORM -> 36763;
            case R8_UINT -> 33330;
            case R8_SINT -> 33329;
            case RG8_UINT -> 33336;
            case RG8_SINT -> 33335;
            case RGBA8_UINT -> 36220;
            case RGBA8_SINT -> 36238;
            case R16_UINT -> 33332;
            case R16_SINT -> 33331;
            case RG16_UINT -> 33338;
            case RG16_SINT -> 33337;
            case RGBA16_UINT -> 36214;
            case RGBA16_SINT -> 36232;
            case R32_UINT -> 33334;
            case R32_SINT -> 33333;
            case RG32_UINT -> 33340;
            case RG32_SINT -> 33339;
            case RGB32_UINT -> 36209;
            case RGB32_SINT -> 36227;
            case RGBA32_UINT -> 36208;
            case RGBA32_SINT -> 36226;
            case R16_FLOAT -> 33325;
            case RG16_FLOAT -> 33327;
            case RGBA16_FLOAT -> 34842;
            case R32_FLOAT -> 33326;
            case RG32_FLOAT -> 33328;
            case RGBA32_FLOAT -> 34836;
            case RGB10A2_UINT -> 36975;
            case RG11B10_FLOAT -> 35898;
            case D32_FLOAT -> 36012;
            case D32_FLOAT_S8_UINT -> 36013;
            case D24_UNORM_S8_UINT -> 35056;
            case S8_UINT -> 36168;
        };
    }

    public static int toGlExternalId(final GpuFormat gpuFormat) {
        return switch (gpuFormat) {
            case R8_UNORM, R8_SNORM, R16_UNORM, R16_SNORM, R16_FLOAT, R32_FLOAT -> 6403;
            case RG8_UNORM, RG8_SNORM, RG16_UNORM, RG16_SNORM, RG16_FLOAT, RG32_FLOAT -> 33319;
            case RGB8_UNORM, RGB8_SNORM, RGB16_UNORM, RGB16_SNORM, RG11B10_FLOAT, RGB16_FLOAT, RGB32_FLOAT -> 6407;
            case RGBA8_UNORM, RGBA8_SNORM, RGBA16_UNORM, RGB10A2_UNORM, RGBA16_SNORM, RGBA16_FLOAT, RGBA32_FLOAT -> 6408;
            case D16_UNORM, D32_FLOAT -> 6402;
            case R8_UINT, R8_SINT, R16_UINT, R16_SINT, R32_UINT, R32_SINT -> 36244;
            case RG8_UINT, RG8_SINT, RG16_UINT, RG16_SINT, RG32_UINT, RG32_SINT -> 33320;
            case RGBA8_UINT, RGBA8_SINT, RGBA16_UINT, RGBA16_SINT, RGBA32_UINT, RGBA32_SINT, RGB10A2_UINT -> 36249;
            case RGB32_UINT, RGB32_SINT, RGB8_UINT, RGB8_SINT, RGB16_UINT, RGB16_SINT -> 36248;
            case D32_FLOAT_S8_UINT, D24_UNORM_S8_UINT -> 34041;
            case S8_UINT -> 6401;
            default -> 0;
        };
    }

    public static int toGlType(final GpuFormat gpuFormat) {
        return switch (gpuFormat) {
            case R8_UNORM, RG8_UNORM, RGB8_UNORM, RGBA8_UNORM, R8_UINT, RG8_UINT, RGBA8_UINT, S8_UINT, RGB8_UINT -> 5121;
            case R8_SNORM, RG8_SNORM, RGB8_SNORM, RGBA8_SNORM, R8_SINT, RG8_SINT, RGBA8_SINT, RGB8_SINT -> 5120;
            case R16_UNORM, RG16_UNORM, RGB16_UNORM, RGBA16_UNORM, D16_UNORM, R16_UINT, RG16_UINT, RGBA16_UINT, RGB16_UINT -> 5123;
            case R16_SNORM, RG16_SNORM, RGB16_SNORM, RGBA16_SNORM, R16_SINT, RG16_SINT, RGBA16_SINT, RGB16_SINT -> 5122;
            case RGB10A2_UNORM, RGB10A2_UINT -> 33640;
            case R32_UINT, RG32_UINT, RGB32_UINT, RGBA32_UINT -> 5125;
            case R32_SINT, RG32_SINT, RGB32_SINT, RGBA32_SINT -> 5124;
            case R16_FLOAT, RG16_FLOAT, RGBA16_FLOAT, RGB16_FLOAT -> 5131;
            case R32_FLOAT, RG32_FLOAT, RGBA32_FLOAT, D32_FLOAT, RGB32_FLOAT -> 5126;
            case RG11B10_FLOAT -> 35899;
            case D32_FLOAT_S8_UINT -> 36269;
            case D24_UNORM_S8_UINT -> 34042;
            default -> 0;
        };
    }

    public static int toGl(final ShaderType type) {
        return switch (type) {
            case VERTEX -> 35633;
            case FRAGMENT -> 35632;
        };
    }

    public static int bufferUsageToGlFlag(final @GpuBuffer.Usage int usage) {
        int result = 0;
        if ((usage & 1) != 0) {
            result |= 65;
        }

        if ((usage & 2) != 0) {
            result |= 66;
        }

        if ((usage & 8) != 0) {
            result |= 256;
        }

        if ((usage & 4) != 0) {
            result |= 512;
        }

        return result;
    }

    public static int bufferUsageToGlEnum(final @GpuBuffer.Usage int usage) {
        boolean clientStorage = (usage & 4) != 0;
        if ((usage & 2) != 0) {
            return clientStorage ? 35040 : 35044;
        } else if ((usage & 1) != 0) {
            return clientStorage ? 35041 : 35045;
        } else {
            return 35044;
        }
    }
}