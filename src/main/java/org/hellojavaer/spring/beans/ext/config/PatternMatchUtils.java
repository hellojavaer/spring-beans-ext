/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hellojavaer.spring.beans.ext.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author <a href="mailto:hellojavaer@gmail.com">zoukaiming</a>
 */
public class PatternMatchUtils {

    public static boolean simpleMatch(String pattern, String str, List<String> placeholderStrings) {
        Stack<String> matchResult = new Stack<String>();
        String normalPattern = checkAndBuildNormalString(pattern);
        boolean match = simpleMatch(normalPattern, str, matchResult);
        if (match) {
            String[] matchResultArray = new String[matchResult.size()];
            int index = 0;
            while (!matchResult.isEmpty()) {
                matchResultArray[index++] = matchResult.pop();
            }
            List<String> result = new ArrayList<String>();
            List<String> placeHolders = getPlaceHolders(pattern, matchResultArray);
            for (String placeholderStr : placeholderStrings) {
                result.add(replacePlaceholders(placeholderStr, placeHolders));
            }
            placeholderStrings.clear();
            placeholderStrings.addAll(result);
        }
        return match;
    }

    private static String checkAndBuildNormalString(String str) {
        if (str == null) {
            return null;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            boolean tag = false;
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                if (ch == '{') {
                    if (tag) {
                        throw new IllegalArgumentException(str);
                    }
                    tag = true;
                } else if (ch == '}') {
                    if (!tag) {
                        throw new IllegalArgumentException(str);
                    }
                    tag = false;
                } else {
                    stringBuilder.append(ch);
                }
            }
            return stringBuilder.toString();
        }
    }

    private static List<String> getPlaceHolders(String str, String[] matchResultArray) {
        StringBuilder stringBuilder = null;
        List<String> list = new ArrayList<String>();
        Integer beginIndex = null;
        Integer matchArrayIndex = -1;
        Integer asteriskIndex = null;
        char prech = 0;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '*') {
                if (prech != '*') {
                    matchArrayIndex++;
                    if (beginIndex != null) {
                        stringBuilder.append(str.substring(beginIndex + 1, i));
                        stringBuilder.append(matchResultArray[matchArrayIndex]);
                        asteriskIndex = i;
                    }
                } else if (asteriskIndex != null) {
                    asteriskIndex++;
                }
            } else if (ch == '{') {
                stringBuilder = new StringBuilder();
                if (beginIndex == null) {
                    beginIndex = i;
                } else {
                    throw new IllegalArgumentException(str);
                }
            } else if (ch == '}') {
                if (beginIndex == null) {
                    throw new IllegalArgumentException(str);
                } else {
                    if (asteriskIndex != null) {
                        stringBuilder.append(str.substring(asteriskIndex + 1, i));
                    } else {
                        stringBuilder.append(str.substring(beginIndex + 1, i));
                    }
                    list.add(stringBuilder.toString());
                    stringBuilder = null;
                    asteriskIndex = null;
                    beginIndex = null;
                }
            }
            prech = ch;
        }
        if (beginIndex != null) {
            throw new IllegalArgumentException(str);
        }
        return list;
    }

    private static String replacePlaceholders(String str, List<String> placeholders) {
        StringBuilder sb = new StringBuilder();
        Integer beginIndex = null;
        Integer endIndex = 0;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '{') {
                if (beginIndex == null) {
                    beginIndex = i;
                    if (i > endIndex) {
                        sb.append(str.substring(endIndex, i));
                    }
                } else {
                    throw new IllegalArgumentException(str);
                }
            } else if (ch == '}') {
                if (beginIndex == null) {
                    throw new IllegalArgumentException(str);
                } else {
                    String indexStr = str.substring(beginIndex + 1, i);
                    Integer index = Integer.valueOf(indexStr);
                    if (index < placeholders.size()) {
                        String v = placeholders.get(index);
                        if (v != null) {
                            sb.append(v);
                        }
                    }
                    endIndex = i + 1;
                    beginIndex = null;
                }
            }
        }
        if (beginIndex != null) {
            throw new IllegalArgumentException(str);
        }
        if (endIndex == 0) {
            return str;
        }
        sb.append(str.substring(endIndex));
        return sb.toString();
    }

    /**
     * Match a String against the given pattern, supporting the following simple
     * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
     * arbitrary number of pattern parts), as well as direct equality.
     * If the String matches the given pattern '*' will else do noting for matchResult.
     * @param pattern the pattern to match against
     * @param str the String to match
     * @return whether the String matches the given pattern
     */
    private static boolean simpleMatch(String pattern, String str, Stack<String> matchResult) {
        if (pattern == null || str == null) {
            return false;
        }
        int firstIndex = pattern.indexOf('*');
        if (firstIndex == -1) {
            return pattern.equals(str);
        }
        if (firstIndex == 0) {
            if (pattern.length() == 1) {
                matchResult.push(str);
                return true;
            }
            int nextIndex = pattern.indexOf('*', firstIndex + 1);
            if (nextIndex == -1) {
                boolean match = str.endsWith(pattern.substring(1));
                if (match) {
                    matchResult.push(str.substring(0, str.length() - pattern.length() + 1));
                }
                return match;
            }
            String part = pattern.substring(1, nextIndex);
            if ("".equals(part)) {
                return simpleMatch(pattern.substring(nextIndex), str, matchResult);
            }
            int partIndex = str.indexOf(part);
            while (partIndex != -1) {
                if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()), matchResult)) {
                    matchResult.push(str.substring(0, partIndex));
                    return true;
                }
                partIndex = str.indexOf(part, partIndex + 1);
            }
            return false;
        }
        return (str.length() >= firstIndex && pattern.substring(0, firstIndex).equals(str.substring(0, firstIndex)) && simpleMatch(pattern.substring(firstIndex),
                                                                                                                                   str.substring(firstIndex),
                                                                                                                                   matchResult));
    }

    /**
     * Match a String against the given pattern, supporting the following simple
     * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
     * arbitrary number of pattern parts), as well as direct equality.
     * @param pattern the pattern to match against
     * @param str the String to match
     * @return whether the String matches the given pattern
     */
    public static boolean simpleMatch(String pattern, String str) {
        if (pattern == null || str == null) {
            return false;
        }
        int firstIndex = pattern.indexOf('*');
        if (firstIndex == -1) {
            return pattern.equals(str);
        }
        if (firstIndex == 0) {
            if (pattern.length() == 1) {
                return true;
            }
            int nextIndex = pattern.indexOf('*', firstIndex + 1);
            if (nextIndex == -1) {
                return str.endsWith(pattern.substring(1));
            }
            String part = pattern.substring(1, nextIndex);
            if ("".equals(part)) {
                return simpleMatch(pattern.substring(nextIndex), str);
            }
            int partIndex = str.indexOf(part);
            while (partIndex != -1) {
                if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()))) {
                    return true;
                }
                partIndex = str.indexOf(part, partIndex + 1);
            }
            return false;
        }
        return (str.length() >= firstIndex && pattern.substring(0, firstIndex).equals(str.substring(0, firstIndex)) && simpleMatch(pattern.substring(firstIndex),
                                                                                                                                   str.substring(firstIndex)));
    }

    /**
     * Match a String against the given patterns, supporting the following simple
     * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
     * arbitrary number of pattern parts), as well as direct equality.
     * @param patterns the patterns to match against
     * @param str the String to match
     * @return whether the String matches any of the given patterns
     */
    public static boolean simpleMatch(String[] patterns, String str) {
        if (patterns != null) {
            for (String pattern : patterns) {
                if (simpleMatch(pattern, str)) {
                    return true;
                }
            }
        }
        return false;
    }

}
