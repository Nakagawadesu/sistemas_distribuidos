import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

public class MergeSortComparison {
    private static final int ARRAY_SIZE = 10_000_000;

    public static void main(String[] args) {
        // ### TESTES DE VALIDAÇÃO ###
        int[] testArray = {5, 3, 8, 4, 2, 7, 1, 6, 9, 0};
        System.out.println("Array original de teste: " + Arrays.toString(testArray));

        int[] seqTest = testArray.clone();
        Arrays.sort(seqTest);
        System.out.println("Resultado sequencial:   " + Arrays.toString(seqTest));

        int[] parTest = testArray.clone();
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new ParallelMergeSort(parTest, 0, parTest.length - 1));
        System.out.println("Resultado paralelo:     " + Arrays.toString(parTest));

        System.out.println("\n--- Iniciando testes de desempenho ---");

        // ### TESTES DE DESEMPENHO ###
        int[] originalArray = generateLargeArray(ARRAY_SIZE);
        int[] arraySequential = originalArray.clone();
        int[] arrayParallel = originalArray.clone();

        long sequentialTime = timeSequentialSort(arraySequential);
        System.out.println("\nTempo sequencial: " + sequentialTime + " ms");

        long parallelTime = timeParallelSort(arrayParallel);
        System.out.println("Tempo paralelo:   " + parallelTime + " ms");

        double speedup = (double) sequentialTime / parallelTime;
        System.out.printf("Speedup: %.2fx%n", speedup);

        System.out.println("\nVerificação final:");
        System.out.println("Ordenação sequencial correta: " + isSorted(arraySequential));
        System.out.println("Ordenação paralela correta:   " + isSorted(arrayParallel));
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
        SequentialMergeSort sorter = new SequentialMergeSort();
        long start = System.currentTimeMillis();
        sorter.mergeSort(array);
        return System.currentTimeMillis() - start;
    }

    private static long timeParallelSort(int[] array) {
        ForkJoinPool pool = new ForkJoinPool();
        System.out.println("[Debug] Threads usadas no teste de desempenho: " + pool.getParallelism()); 
        long start = System.currentTimeMillis();
        pool.invoke(new ParallelMergeSort(array, 0, array.length - 1));
        return System.currentTimeMillis() - start;
    }

    private static boolean isSorted(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i] > array[i + 1]) return false;
        }
        return true;
    }
}