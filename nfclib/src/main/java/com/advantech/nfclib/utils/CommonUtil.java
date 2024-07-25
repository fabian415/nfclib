package com.advantech.nfclib.utils;

import java.util.Objects;

public class CommonUtil {
    public static byte[] charToByteArray(char[] x) {
        final byte[] res = new byte[x.length];
        for (int i = 0; i < x.length; i++) {
            res[i] = (byte) x[i];
        }
        return res;
    }

    public static boolean isFWSupport(String version1, String version2) {
        Objects.requireNonNull(version1);
        Objects.requireNonNull(version2);

        String[] version1Parts = version1.split("\\.");
        String[] version2Parts = version2.split("\\.");

        int minLength = Math.min(version1Parts.length, version2Parts.length);
        for (int i = 0; i < minLength; i++) {
            int part1 = Integer.parseInt(version1Parts[i]);
            int part2 = Integer.parseInt(version2Parts[i]);
            if (part1 < part2) {
                return false;
            }
            if (part1 > part2) {
                return true;
            }
        }
        return true;
    }
}
