import java.util.concurrent.RecursiveAction;

public class ParallelMergeSort extends RecursiveAction {
    private int[] array;
    private int start;
    private int end;
    // calculei threshold ideal para o meu computador que tem 12 threads
    // 10000000 / 12 = 833333, arredondado para 850000
    private static final int THRESHOLD = 850_000; // Tamanho do array para dividir em tarefas paralelas 

    public ParallelMergeSort(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected void compute() {
        if (end - start < THRESHOLD) {
            sequentialMergeSort(array, start, end); 
            return;
        }

        int mid = (start + end) / 2;
        ParallelMergeSort leftTask = new ParallelMergeSort(array, start, mid);
        ParallelMergeSort rightTask = new ParallelMergeSort(array, mid + 1, end);

        invokeAll(leftTask, rightTask); 
        merge(array, start, mid, end);
    }

    private void merge(int[] array, int start, int mid, int end) {
        int[] temp = new int[end - start + 1];
        int i = start, j = mid + 1, k = 0;

        while (i <= mid && j <= end) {
            if (array[i] <= array[j]) {
                temp[k++] = array[i++];
            } else {
                temp[k++] = array[j++];
            }
        }

        while (i <= mid) temp[k++] = array[i++];
        while (j <= end) temp[k++] = array[j++];

        System.arraycopy(temp, 0, array, start, temp.length);
    }
    private void sequentialMergeSort(int[] array, int start, int end) {
        if (start >= end) return;

        int mid = (start + end) / 2;
        sequentialMergeSort(array, start, mid);    // esquerda
        sequentialMergeSort(array, mid + 1, end);  //  direita
        merge(array, start, mid, end);             
    }
}