/**
 * This file is part of pinyin4j (http://sourceforge.net/projects/pinyin4j/) and distributed under
 * GNU GENERAL PUBLIC LICENSE (GPL).
 * 
 * pinyin4j is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * pinyin4j is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with pinyin4j.
 */

/**
 *
 */
package net.sourceforge.pinyin4j;

import java.util.Map;

/**
 * A class contains logic that translates from Hanyu Pinyin to Gwoyeu Romatzyh
 *
 * @author Li Min (xmlerlimin@gmail.com)
 *
 */
class GwoyeuRomatzyhTranslator {
  /**
   * @param hanyuPinyinStr
   *            Given unformatted Hanyu Pinyin with tone number
   * @return Corresponding Gwoyeu Romatzyh; null if no mapping is found.
   */
  static String convertHanyuPinyinToGwoyeuRomatzyh(String hanyuPinyinStr) {
    String pinyinString = TextHelper.extractPinyinString(hanyuPinyinStr);
    String toneNumberStr = TextHelper.extractToneNumber(hanyuPinyinStr);

    // return value
    String gwoyeuStr = null;

    Map<String, Map<String, String>> pinyinToGwoyeuMappingMap =
        GwoyeuRomatzyhResource.getInstance().getPinyinToGwoyeuMappingMap();

    // find the entry keyed by the Hanyu Pinyin presentation
    Map<String, String> mappingEntry = pinyinToGwoyeuMappingMap.get(pinyinString);

    if (null != mappingEntry) {
      String toneTag = getToneTag(toneNumberStr);

      if (null != toneTag) {
        // find the Gwoyeu Romatzyh presentation of the given tone
        gwoyeuStr = mappingEntry.get(PinyinRomanizationType.GWOYEU_ROMATZYH.getTagName() + toneTag);
      }
    }

    return gwoyeuStr;
  }

  /**
   * @param toneNumberStr
   *            the tone number of the given unformatted Hanyu Pinyin
   * @return the postfix distinguishing the Gwoyeu Romatzyh of the given tone;
   *         null if the given tone number is not a supported one
   */
  private static String getToneTag(String toneNumberStr) {
    int toneNumber;
    try {
      toneNumber = Integer.parseInt(toneNumberStr);
    } catch (NumberFormatException e) {
      return null;
    }

    if (toneNumber < 1 || toneNumber > tones.length) {
      return null;
    }

    return tones[toneNumber - 1];
  }

  /**
   * The postfixs to distinguish different tone of Gwoyeu Romatzyh
   */
  static private String[] tones = new String[] {"_I", "_II", "_III", "_IV", "_V"};
}
