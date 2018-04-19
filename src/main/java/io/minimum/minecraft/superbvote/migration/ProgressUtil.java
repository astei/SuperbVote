package io.minimum.minecraft.superbvote.migration;

public class ProgressUtil {
    private static final int[] PROGRESS_BY = new int[]{
            500, 50
    };

    static int findBestDivisor(int records) {
        for (int i : PROGRESS_BY) {
            if (records / i >= 1) {
                return i;
            }
        }
        return 50;
    }
}
