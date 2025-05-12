package trabalho1;
import java.util.Arrays;

public class SequentialMergeSort {

    public void mergeSort(int[] array) {
        if (array.length > 1) {
            int mid = array.length / 2;
            int[] left = Arrays.copyOfRange(array, 0, mid);
            int[] right = Arrays.copyOfRange(array, mid, array.length);
            mergeSort(left);
            mergeSort(right);
            merge(array, left, right);
        }
    }

    private void merge(int[] result, int[] left, int[] right) {
        int i = 0, j = 0, k = 0;
        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                result[k++] = left[i++];
            } else {
                result[k++] = right[j++];
            }
        }
        while (i < left.length) {
            result[k++] = left[i++];
        }
        while (j < right.length) {
            result[k++] = right[j++];
        }
    }

    public static void main(String[] args) {
        int[] array = {5, 3, 8, 4, 2, 7, 1, 6};
        SequentialMergeSort sorter = new SequentialMergeSort();
        sorter.mergeSort(array);
        System.out.println("Sorted array: " + Arrays.toString(array));
    }
}