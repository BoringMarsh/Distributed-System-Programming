import java.util.Random;

/**
 * 在满足题目要求下，能生成最多数对的种子选择程序
 */
public class MaxSeedFinder {
    // 7639999: 2038对
    // 48968399: 2037对
    // 74968336: 2138对
    // 158276472: 2221对
    // 246531038: 2324对
    // 811697185: 2478对

    public static void main(String[] args) {
        int maxCount = 0;
        int maxSeed = 0;

        for (int i = 1000000000; i <= 2147483646; i++) {
            int count = 0;
            Random generator = new Random(i);
            double num1 = 0.0, num2 = 0.0;

            while (num1 < 0.91 || num2 < 0.91) {
                num1 = generator.nextDouble();
                num2 = generator.nextDouble();
                count++;
            }
            //System.out.println("Seed " + i + " result: " + count);
            if (count > maxCount) {
                maxSeed = i;
                maxCount = count;
            }

            if (i % 1000000 == 0) {
                System.out.println("Seed " + i + " result: " + count);
            }
        }

        System.out.println("MaxSeed: " + maxSeed + ", MaxCount: " + maxCount);

    }
}
