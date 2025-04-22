import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

public class MergeSortComparison {
    private static final int ARRAY_SIZE = 10_000_000; // Test with large arrays

    public static void main(String[] args) {
        int[] originalArray = generateLargeArray(ARRAY_SIZE);
        int[] arraySequential = originalArray.clone();
        int[] arrayParallel = originalArray.clone();


        // Time Sequential Sort
        long sequentialTime = timeSequentialSort(arraySequential.clone());
        System.out.println("Sequential time: " + sequentialTime + " ms");

        // Time Parallel Sort
        long parallelTime = timeParallelSort(arrayParallel.clone());
        System.out.println("Parallel time: " + parallelTime + " ms");

        // Calculate Speedup
        double speedup = (double) sequentialTime / parallelTime;
        System.out.printf("Speedup: %.2fx%n", speedup);

    }

    private static int[] generateLargeArray(int size) {
        Random rand = new Random();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = rand.nextInt();
        }
        return array;
    }

 

    private static long timeSequentialSort(int[] array) {
        long start = System.currentTimeMillis();
        Arrays.sort(array);
        return System.currentTimeMillis() - start;
    }

    private static long timeParallelSort(int[] array) {
        ForkJoinPool pool = new ForkJoinPool();
        long start = System.currentTimeMillis();
        pool.invoke(new ParallelMergeSort(array, 0, array.length - 1));
        return System.currentTimeMillis() - start;
    }


}