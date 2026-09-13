// Auto-generated: DO NOT EDIT

package net.jpountz.lz4;

import static net.jpountz.lz4.LZ4Constants.*;
import static net.jpountz.lz4.LZ4Utils.*;

import java.nio.ByteBuffer;

import net.jpountz.util.ByteBufferUtils;
import net.jpountz.util.UnsafeUtils;

/**
 * Decompressor.
 */
final class LZ4JavaUnsafeFastDecompressor extends LZ4FastDecompressor {

  public static final LZ4FastDecompressor INSTANCE = new LZ4JavaUnsafeFastDecompressor();


  @Override
  public int decompress(byte[] src, final int srcOff, byte[] dest, final int destOff, int destLen) {
    
      final int srcEnd = src.length;
    
    return decompress(src, srcOff, srcEnd - srcOff, dest, destOff, destLen);
  }

  private int decompress(byte[] src, final int srcOff, final int srcLen, byte[] dest, final int destOff, int destLen) {
    UnsafeUtils.checkRange(src, srcOff, srcLen);
    UnsafeUtils.checkRange(dest, destOff, destLen);


    if (destLen == 0) {
      // Allow `srcLen > 1` despite just one byte being consumed since this 'fast' decompressor does not have to fully consume the src
      if (srcLen < 1 || UnsafeUtils.readByte(src, srcOff) != 0) {
        throw new LZ4Exception("Malformed input at " + srcOff);
      }
      return 1;
    }

    final int srcEnd = srcOff + srcLen;
    final int destEnd = destOff + destLen;

    int sOff = srcOff;
    int dOff = destOff;

    while (true) {
      if (sOff >= srcEnd) {
        throw new LZ4Exception("Malformed input at " + sOff);
      }
      final int token = UnsafeUtils.readByte(src, sOff) & 0xFF;
      ++sOff;

      // literals
      int literalLen = token >>> ML_BITS;
      if (literalLen == RUN_MASK) {
        byte len = (byte) 0xFF;
        while (sOff < srcEnd && (len = UnsafeUtils.readByte(src, sOff++)) == (byte) 0xFF) {
          literalLen += 0xFF;
          if (literalLen < 0) {
            throw new LZ4Exception("Too large literalLen");
          }
        }
        literalLen += len & 0xFF;
      }

      final int literalCopyEnd = dOff + literalLen;
      // Check for overflow
      if (literalCopyEnd < dOff) {
        throw new LZ4Exception("Too large literalLen");
      }

      if (notEnoughSpace(destEnd - literalCopyEnd, COPY_LENGTH) || notEnoughSpace(srcEnd - sOff, COPY_LENGTH + literalLen)) {

        if (literalCopyEnd != destEnd) {
          throw new LZ4Exception("Malformed input at " + sOff);
        } else if (notEnoughSpace(srcEnd - sOff, literalLen)) {
          throw new LZ4Exception("Malformed input at " + sOff);

        } else {
          LZ4UnsafeUtils.safeArraycopy(src, sOff, dest, dOff, literalLen);
          sOff += literalLen;
          dOff = literalCopyEnd;
          break; // EOF
        }
      }

      LZ4UnsafeUtils.wildArraycopy(src, sOff, dest, dOff, literalLen);
      sOff += literalLen;
      dOff = literalCopyEnd;

      // matchs
      final int matchDec = UnsafeUtils.readShortLE(src, sOff);
      sOff += 2;
      int matchOff = dOff - matchDec;

      if (matchOff < destOff) {
        throw new LZ4Exception("Malformed input at " + sOff);
      }

      int matchLen = token & ML_MASK;
      if (matchLen == ML_MASK) {
        byte len = (byte) 0xFF;
        while (sOff < srcEnd && (len = UnsafeUtils.readByte(src, sOff++)) == (byte) 0xFF) {
          matchLen += 0xFF;
          if (matchLen < 0) {
            throw new LZ4Exception("Too large matchLen");
          }
        }
        matchLen += len & 0xFF;
      }
      matchLen += MIN_MATCH;

      final int matchCopyEnd = dOff + matchLen;
      // Check for overflow
      if (matchCopyEnd < dOff) {
        throw new LZ4Exception("Too large matchLen");
      }

      if (matchDec == 0) {
        if (matchCopyEnd > destEnd) {
          throw new LZ4Exception("Malformed input at " + sOff);
        }
        // With matchDec == 0, matchOff == dOff, so we'd copy in place. Zero the data instead. (CVE-2025-66566)
        assert matchOff == dOff; // should always hold, but this extra check will trigger during fuzzing if my logic is wrong
        LZ4Utils.zero(dest, dOff, matchCopyEnd);
      } else if (notEnoughSpace(destEnd - matchCopyEnd, COPY_LENGTH)) {
        if (matchCopyEnd > destEnd) {
          throw new LZ4Exception("Malformed input at " + sOff);
        }
        LZ4UnsafeUtils.safeIncrementalCopy(dest, matchOff, dOff, matchLen);
      } else {
        LZ4UnsafeUtils.wildIncrementalCopy(dest, matchOff, dOff, matchCopyEnd);
      }
      dOff = matchCopyEnd;
    }


    return sOff - srcOff;

  }


  @Override
  public int decompress(ByteBuffer src, final int srcOff, ByteBuffer dest, final int destOff, int destLen) {
    
      final int srcEnd = src.capacity();
    
    return decompress(src, srcOff, srcEnd - srcOff, dest, destOff, destLen);
  }

  private int decompress(ByteBuffer src, final int srcOff, final int srcLen, ByteBuffer dest, final int destOff, int destLen) {
    ByteBufferUtils.checkRange(src, srcOff, srcLen);
    ByteBufferUtils.checkRange(dest, destOff, destLen);

    if (src.hasArray() && dest.hasArray()) {
      return decompress(src.array(), srcOff + src.arrayOffset(), srcLen, dest.array(), destOff + dest.arrayOffset(), destLen);
    }
    src = ByteBufferUtils.inNativeByteOrder(src);
    dest = ByteBufferUtils.inNativeByteOrder(dest);


    if (destLen == 0) {
      // Allow `srcLen > 1` despite just one byte being consumed since this 'fast' decompressor does not have to fully consume the src
      if (srcLen < 1 || ByteBufferUtils.readByte(src, srcOff) != 0) {
        throw new LZ4Exception("Malformed input at " + srcOff);
      }
      return 1;
    }

    final int srcEnd = srcOff + srcLen;
    final int destEnd = destOff + destLen;

    int sOff = srcOff;
    int dOff = destOff;

    while (true) {
      if (sOff >= srcEnd) {
        throw new LZ4Exception("Malformed input at " + sOff);
      }
      final int token = ByteBufferUtils.readByte(src, sOff) & 0xFF;
      ++sOff;

      // literals
      int literalLen = token >>> ML_BITS;
      if (literalLen == RUN_MASK) {
        byte len = (byte) 0xFF;
        while (sOff < srcEnd && (len = ByteBufferUtils.readByte(src, sOff++)) == (byte) 0xFF) {
          literalLen += 0xFF;
          if (literalLen < 0) {
            throw new LZ4Exception("Too large literalLen");
          }
        }
        literalLen += len & 0xFF;
      }

      final int literalCopyEnd = dOff + literalLen;
      // Check for overflow
      if (literalCopyEnd < dOff) {
        throw new LZ4Exception("Too large literalLen");
      }

      if (notEnoughSpace(destEnd - literalCopyEnd, COPY_LENGTH) || notEnoughSpace(srcEnd - sOff, COPY_LENGTH + literalLen)) {

        if (literalCopyEnd != destEnd) {
          throw new LZ4Exception("Malformed input at " + sOff);
        } else if (notEnoughSpace(srcEnd - sOff, literalLen)) {
          throw new LZ4Exception("Malformed input at " + sOff);

        } else {
          LZ4ByteBufferUtils.safeArraycopy(src, sOff, dest, dOff, literalLen);
          sOff += literalLen;
          dOff = literalCopyEnd;
          break; // EOF
        }
      }

      LZ4ByteBufferUtils.wildArraycopy(src, sOff, dest, dOff, literalLen);
      sOff += literalLen;
      dOff = literalCopyEnd;

      // matchs
      final int matchDec = ByteBufferUtils.readShortLE(src, sOff);
      sOff += 2;
      int matchOff = dOff - matchDec;

      if (matchOff < destOff) {
        throw new LZ4Exception("Malformed input at " + sOff);
      }

      int matchLen = token & ML_MASK;
      if (matchLen == ML_MASK) {
        byte len = (byte) 0xFF;
        while (sOff < srcEnd && (len = ByteBufferUtils.readByte(src, sOff++)) == (byte) 0xFF) {
          matchLen += 0xFF;
          if (matchLen < 0) {
            throw new LZ4Exception("Too large matchLen");
          }
        }
        matchLen += len & 0xFF;
      }
      matchLen += MIN_MATCH;

      final int matchCopyEnd = dOff + matchLen;
      // Check for overflow
      if (matchCopyEnd < dOff) {
        throw new LZ4Exception("Too large matchLen");
      }

      if (matchDec == 0) {
        if (matchCopyEnd > destEnd) {
          throw new LZ4Exception("Malformed input at " + sOff);
        }
        // With matchDec == 0, matchOff == dOff, so we'd copy in place. Zero the data instead. (CVE-2025-66566)
        assert matchOff == dOff; // should always hold, but this extra check will trigger during fuzzing if my logic is wrong
        LZ4Utils.zero(dest, dOff, matchCopyEnd);
      } else if (notEnoughSpace(destEnd - matchCopyEnd, COPY_LENGTH)) {
        if (matchCopyEnd > destEnd) {
          throw new LZ4Exception("Malformed input at " + sOff);
        }
        LZ4ByteBufferUtils.safeIncrementalCopy(dest, matchOff, dOff, matchLen);
      } else {
        LZ4ByteBufferUtils.wildIncrementalCopy(dest, matchOff, dOff, matchCopyEnd);
      }
      dOff = matchCopyEnd;
    }


    return sOff - srcOff;

  }


}

