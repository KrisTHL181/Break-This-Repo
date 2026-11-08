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
 * Contains the logic translating among different Chinese Romanization systems
 *
 * @author Li Min (xmlerlimin@gmail.com)
 *
 */
class PinyinRomanizationTranslator {
  /**
   * convert the given unformatted Pinyin string from source Romanization
   * system to target Romanization system
   *
   * @param sourcePinyinStr
   *            the given unformatted Pinyin string
   * @param sourcePinyinSystem
   *            the Romanization system which is currently used by the given
   *            unformatted Pinyin string
   * @param targetPinyinSystem
   *            the Romanization system that should be converted to
   * @return unformatted Pinyin string in target Romanization system; null if
   *         error happens
   */
  static String convertRomanizationSystem(String sourcePinyinStr,
      PinyinRomanizationType sourcePinyinSystem, PinyinRomanizationType targetPinyinSystem) {
    String pinyinString = TextHelper.extractPinyinString(sourcePinyinStr);
    String toneNumberStr = TextHelper.extractToneNumber(sourcePinyinStr);

    // return value
    String targetPinyinStr = null;

    Map<String, Map<String, String>> pinyinMappingMap =
        PinyinRomanizationResource.getInstance().getPinyinMappingMap();

    // find the entry keyed by the source Pinyin system presentation
    String hanyuKey = findHanyuKey(pinyinMappingMap, sourcePinyinSystem, pinyinString);

    if (null != hanyuKey) {
      // find the presentation of the target Pinyin system; the Hanyu Pinyin
      // presentation is the key of the entry rather than one of its values
      String targetPinyinStrWithoutToneNumber;
      if (PinyinRomanizationType.HANYU_PINYIN.getTagName().equals(targetPinyinSystem.getTagName())) {
        targetPinyinStrWithoutToneNumber = hanyuKey;
      } else {
        targetPinyinStrWithoutToneNumber =
            pinyinMappingMap.get(hanyuKey).get(targetPinyinSystem.getTagName());
      }

      if (null != targetPinyinStrWithoutToneNumber) {
        targetPinyinStr = targetPinyinStrWithoutToneNumber + toneNumberStr;
      }
    }

    return targetPinyinStr;
  }

  /**
   * Look up the Hanyu Pinyin key of the entry whose presentation in the given
   * Romanization system equals the given unformatted Pinyin string.
   *
   * <p>
   * The mapping resource is keyed by the Hanyu Pinyin presentation, so a lookup
   * for Hanyu Pinyin is a direct one; any other system requires scanning the
   * values.
   * </p>
   *
   * @param pinyinMappingMap
   *            the whole mapping resource
   * @param pinyinSystem
   *            the Romanization system the given string belongs to
   * @param pinyinString
   *            the given unformatted Pinyin string
   * @return the Hanyu Pinyin key of the matching entry; null if no mapping is
   *         found
   */
  private static String findHanyuKey(Map<String, Map<String, String>> pinyinMappingMap,
      PinyinRomanizationType pinyinSystem, String pinyinString) {
    if (PinyinRomanizationType.HANYU_PINYIN.getTagName().equals(pinyinSystem.getTagName())) {
      return pinyinMappingMap.containsKey(pinyinString) ? pinyinString : null;
    }

    String tagName = pinyinSystem.getTagName();
    for (Map.Entry<String, Map<String, String>> entry : pinyinMappingMap.entrySet()) {
      if (pinyinString.equals(entry.getValue().get(tagName))) {
        return entry.getKey();
      }
    }

    return null;
  }
}
