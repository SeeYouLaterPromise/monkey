package org.urbcomp.startdb.compress.elf.utils;

import java.math.BigDecimal;

public class mon64 {
//     E-theta: look-up
    // 41
    private final static int[] t_up =
        new int[] {1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 12,
    12, 12, 13,};

    // 40
    private final static int[] t_down =
        new int[] {0, 0, 0, -1, -1, -1, -2, -2, -2, -3, -3, -3, -3, -4, -4, -4, -5, -5, -5, -6, -6, -6, -6, -7, -7,
        -7, -8, -8, -8, -9, -9, -9, -9, -10, -10, -10, -11, -11, -11, -12};

    // αlog_2(10) for look-up
    private final static int[] f =
        new int[] {0, 4, 7, 10, 14, 17, 20, 24, 27, 30, 34, 37, 40, 44, 47, 50, 54, 57,
            60, 64, 67};

    private final static double[] map10iP =
        new double[] {1.0, 1.0E1, 1.0E2, 1.0E3, 1.0E4, 1.0E5, 1.0E6, 1.0E7,
            1.0E8, 1.0E9, 1.0E10, 1.0E11, 1.0E12, 1.0E13, 1.0E14,
            1.0E15, 1.0E16, 1.0E17, 1.0E18, 1.0E19, 1.0E20};

    private final static double[] map10iN =
        new double[] {1.0, 1.0E-1, 1.0E-2, 1.0E-3, 1.0E-4, 1.0E-5, 1.0E-6, 1.0E-7,
            1.0E-8, 1.0E-9, 1.0E-10, 1.0E-11, 1.0E-12, 1.0E-13, 1.0E-14,
            1.0E-15, 1.0E-16, 1.0E-17, 1.0E-18, 1.0E-19, 1.0E-20};

    private final static long[] mapSPGreater1 =
        new long[] {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    private final static double[] mapSPLess1 =
        new double[] {1, 0.1, 0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.0000001, 0.00000001,
            0.000000001, 0.0000000001};

    private final static double LOG_2_10 = Math.log(10) / Math.log(2);

    private final static double LOG_10_2 = Math.log(2) / Math.log(10);

    public static int getDecimalPlaceCount(double value) {
        if (value == 0) return 0;  // No decimal places if the number is zero

        BigDecimal bigValue = BigDecimal.valueOf(value);
        return bigValue.scale();
    }


    public static double Pow10(int n) {
        if(n > mapSPGreater1.length || n < -mapSPLess1.length) {
            return Math.pow(10, n);
        }else if(n >= 0) {
            return mapSPGreater1[n];
        }else {
            return mapSPLess1[-n];
        }
    }

    public static int getEFromLead(long E_Extracted) {
        return (int)(E_Extracted & 0x7ff) - 1023;
    }

    public static int getEFromValue(long value) {
        return (int)((value >> 52) & 0x7ff) - 1023;
    }

    public static boolean needRevise(int E, int theta) {
        return (E + 1 >= 0 ? 1L << (E + 1) : 1.0 / (1L << -E-1)) > Pow10(theta);
    }

    public static boolean needReviseForInteger(int E, int theta) {
        double power10theta = mon64.Pow10(theta);
        return E > 0 && ((1L << (E + 1)) > power10theta);
    }

    public static boolean furtherCheck(int E) {
        if (E <= 0) return false;
        return getTheta(E - 1) != getTheta(E + 1);
    }

    public static String showBinaryString(long value, int len) {
        String v = Long.toBinaryString(value);
        if(v.length() < len) {
            StringBuilder padding = new StringBuilder();
            for(int i = 0; i < len - v.length(); i++) {
                padding.append('0');
            }
            v = padding + v;
        }
        System.out.println(v);
        return v;
    }

    public static String showBinaryString(long value) {
        String v = Long.toBinaryString(value);
        if(v.length() < 64) {
            StringBuilder padding = new StringBuilder();
            for(int i = 0; i < 64 - v.length(); i++) {
                padding.append('0');
            }
            v = padding + v;
        }
        System.out.println(v);
        return v;
    }

    public static int getTheta0(int E) {
//        further, I can follow the example as elf to construct an array to improve the performance.
        if (E == 0) return 1;
        else if (E > 0) return (int) Math.ceil(E * LOG_10_2);
        else return -(int) Math.floor(-E * LOG_10_2);
    }

    public static int getTheta(int E) {
        if (E >= t_up.length) {
            return (int) Math.ceil(E * LOG_10_2);
        }else if (E >= 0) {
            return t_up[E];
        }else if (E < -t_down.length) {
            return -(int) Math.floor(-E * LOG_10_2);
        }else {
            return t_down[-E-1];
        }
    }

    public static int getFAlpha(int alpha) {
        if (alpha < 0) {
            throw new IllegalArgumentException("The argument should be greater than 0");
        }
        if (alpha >= f.length) {
            return (int) Math.ceil(alpha * LOG_2_10);
        } else {
            return f[alpha];
        }
    }

    public static int getBetaStar(double v, int last) {
        if (v < 0) {
            v = -v;
        }
        int[] spAnd10iNFlag = getSPAnd10iNFlag(v);
        int beta = getSignificantCount(v, spAnd10iNFlag[0], last);
        return spAnd10iNFlag[1] == 1 ? 0 : beta;
    }

    public static int ourGetBeta(double v, int last) {
        if (v < 0) {
            v = -v;
        }
        int[] spAnd10iNFlag = getSPAnd10iNFlag(v);
        int beta = ourGetSignificantCount(v, spAnd10iNFlag[0], last);
        return spAnd10iNFlag[1] == 1 ? 0 : beta;
    }



    public static int[] ourGetAlphaAndBetaStar(double v, int lastBetaStar) {
        if (v < 0) {
            v = -v;
        }
//        BigDecimal bigValue = BigDecimal.valueOf(v);
        int[] alphaAndBetaStar = new int[2];
        int[] spAnd10iNFlag = getSPAnd10iNFlag(v);
        int beta = ourGetSignificantCount(v, spAnd10iNFlag[0], lastBetaStar);
        alphaAndBetaStar[0] = beta - spAnd10iNFlag[0] - 1;
        alphaAndBetaStar[1] = spAnd10iNFlag[1] == 1 ? 0 : beta;

        return alphaAndBetaStar;
    }

    public static int[] getAlphaAndBetaStar(double v, int lastBetaStar) {
        if (v < 0) {
            v = -v;
        }
//        BigDecimal bigValue = BigDecimal.valueOf(v);
        int[] alphaAndBetaStar = new int[2];
        int[] spAnd10iNFlag = getSPAnd10iNFlag(v);
        int beta = getSignificantCount(v, spAnd10iNFlag[0], lastBetaStar);
        alphaAndBetaStar[0] = beta - spAnd10iNFlag[0] - 1;
        alphaAndBetaStar[1] = spAnd10iNFlag[1] == 1 ? 0 : beta;

        return alphaAndBetaStar;
    }

    public static double roundUp(double v, int alpha) {
        double scale = get10iP(alpha);
        if (v < 0) {
            return Math.floor(v * scale) / scale;
        } else {
            return Math.ceil(v * scale) / scale;
        }
    }

    public static double roundDown(double v, int alpha) {
        double scale = get10iP(alpha);
        v *= scale;
        if(v > 0) {
            v = Math.floor(v);
            v -= alpha;
        }else {
            v = Math.ceil(v);
            v += alpha;
        }
        return v / scale;
    }

    public static int getSC(double v) {
        if (v == 0.0) {
            return 0;
        }

        v = Math.abs(v);
        int i = 0;
        int sp = getSP(v);
        if (sp < 0) {
            i = -sp + 1;
        }
        double epsilon = 1e-12;
        double temp = v;

        // Scale 'v' until it becomes an integer
        while (Math.abs(temp - Math.floor(temp)) > epsilon && i < 16) {
            temp *= 10;
            i++;
        }
        long tempLong = Math.round(temp);

        // Count trailing zeros
        int trailingZeros = 0;
        while (tempLong % 10 == 0 && tempLong != 0) {
            tempLong /= 10;
            trailingZeros++;
        }

        // Count the number of digits in tempLong
        int significantDigits = 0;
        long tempCopy = tempLong;
        while (tempCopy != 0) {
            tempCopy /= 10;
            significantDigits++;
        }

        // Total significant digits include digits and trailing zeros (if any)
        significantDigits += trailingZeros;

        return significantDigits;
    }

    public static int ourGetSignificantCount(double v, int sp, int lastBetaStar) {
        int i;
        if(lastBetaStar != Integer.MAX_VALUE && lastBetaStar != 0) {
            i = Math.max(lastBetaStar - sp - 1, 1);
        } else if (lastBetaStar == Integer.MAX_VALUE) {
            i = 17 - sp - 1;
        } else if (sp >= 0) {
            i = 1;
        } else {
            i = -sp;
        }

        double temp = v * get10iP(i);
        long tempLong = (long) temp;
        while (tempLong != temp) {
            i++;
            temp = v * get10iP(i);
            tempLong = (long) temp;
        }

        if (temp / get10iP(i) - v > 1e-12) {
            return 17;
        } else {
            if (i>12) {
                if (tempLong / mapSPGreater1[i-13] % 10 >= 5){
                    tempLong += mapSPGreater1[i-12];
                }
                tempLong = tempLong / (mapSPGreater1[i-12]);
                i = 12;
            }
            while (i > 0 && tempLong % 10 == 0) {
                i--;
                tempLong = tempLong / 10;
            }
            return sp + i + 1;
        }


//        int i;
//        if(lastBetaStar != Integer.MAX_VALUE && lastBetaStar != 0) {
//            i = Math.max(lastBetaStar - sp - 1, 1);
//        } else if (lastBetaStar == Integer.MAX_VALUE) {
//            i = 17 - sp - 1;
//        } else if (sp >= 0) {
//            i = 1;
//        } else {
//            i = -sp;
//        }
//
//        double temp = v * get10iP(i);
//        // judge whether decimal place count equals to zero
//        long tempLong = (long) temp;
//        // Integer judgement: if still has decimal place count, multiply more 10
//        while (tempLong != temp) {
//            i++;
//            temp = v * get10iP(i);
//            tempLong = (long) temp;
//        }
//
//        if (temp / get10iP(i) - v > 1e-12) {
//            return 17;
//        } else {
//            if (i>12) {
//                if (tempLong / mapSPGreater1[i-13] % 10 >= 5) {
//                    tempLong += mapSPGreater1[i-12];
//                }
//                tempLong = tempLong / (mapSPGreater1[i-12]);
//                i = 12;
//            }
//            while (i > 0 && tempLong % 10 == 0) {
//                i--;
//                tempLong = tempLong / 10;
//            }
//            return sp + i + 1;
//        }
    }

    public static int getSignificantCount(double v, int sp, int lastBetaStar) {
        int i;
        if(lastBetaStar != Integer.MAX_VALUE && lastBetaStar != 0) {
            i = Math.max(lastBetaStar - sp - 1, 1);
        } else if (lastBetaStar == Integer.MAX_VALUE) {
            i = 17 - sp - 1;
        } else if (sp >= 0) {
            i = 1;
        } else {
            i = -sp;
        }

        double temp = v * get10iP(i);
        // judge whether decimal place count equals to zero
        long tempLong = (long) temp;
        // Integer judgement: if still has decimal place count, multiply more 10
        while (tempLong != temp) {
            i++;
            temp = v * get10iP(i);
            tempLong = (long) temp;
        }

        // There are some bugs for those with high significand, i.e., 0.23911204406033099
        // So we should further check
        if (temp / get10iP(i) != v) {
            return 17;
        } else {
            while (i > 0 && tempLong % 10 == 0) {
                i--;
                tempLong = tempLong / 10;
            }
            return sp + i + 1;
        }
    }

    // P:positive N:negative
    private static double get10iP(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("This error lies in the utility function (get10iP): The argument should be greater than 0");
        }
        if (i >= map10iP.length) {
            return Double.parseDouble("1.0E" + i);
        } else {
            return map10iP[i];
        }
    }

    public static double get10iN(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("This error lies in the utility function (get10iN): The argument should be greater than 0");
        }
        if (i >= map10iN.length) {
            return Double.parseDouble("1.0E-" + i);
        } else {
            return map10iN[i];
        }
    }

    public static int getSP(double v) {
        if (v >= 1) {
            int i = 0;
            while (i < mapSPGreater1.length - 1) {
                if (v < mapSPGreater1[i + 1]) {
                    return i;
                }
                i++;
            }
        } else {
            int i = 1;
            while (i < mapSPLess1.length) {
                if (v >= mapSPLess1[i]) {
                    return -i;
                }
                i++;
            }
        }
        return (int) Math.floor(Math.log10(v));
    }

    public static int[] getSPAnd10iNFlag(double v) {
        int[] spAnd10iNFlag = new int[2];
        if (v >= 1) {
            int i = 0;
            while (i < mapSPGreater1.length - 1) {
                if (v < mapSPGreater1[i + 1]) {
                    spAnd10iNFlag[0] = i;
                    return spAnd10iNFlag;
                }
                i++;
            }
        } else {
            int i = 1;
            while (i < mapSPLess1.length) {
                if (v >= mapSPLess1[i]) {
                    spAnd10iNFlag[0] = -i;
                    spAnd10iNFlag[1] = v == mapSPLess1[i] ? 1 : 0;
                    return spAnd10iNFlag;
                }
                i++;
            }
        }
        double log10v = Math.log10(v);
        spAnd10iNFlag[0] = (int) Math.floor(log10v);
        spAnd10iNFlag[1] = log10v == (long)log10v ? 1 : 0;
        return spAnd10iNFlag;
    }
}
