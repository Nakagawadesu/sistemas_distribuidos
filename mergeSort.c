#include <stdio.h>
#include <stdlib.h>
#include <omp.h>
#include <time.h>

#define CUTOFF 10000

void merge(int arr[], int left, int mid, int right) 
{
    int i, j, k;
    int n1 = mid - left + 1;
    int n2 = right - mid;
    
    int *leftArr = (int*)malloc(n1 * sizeof(int));
    int *rightArr = (int*)malloc(n2 * sizeof(int));
    
    for (i = 0; i < n1; i++)
    {
        leftArr[i] = arr[left + i];
    }
    for (j = 0; j < n2; j++)
    {
        rightArr[j] = arr[mid + 1 + j];
    }
        
    i = 0;
    j = 0;
    k = left;
    while (i < n1 && j < n2) 
    {
        if (leftArr[i] <= rightArr[j]) 
        {
            arr[k] = leftArr[i];
            i++;
        }
        else 
        {
            arr[k] = rightArr[j];
            j++;
        }
        k++;
    }
    
    while (i < n1) 
    {
        arr[k] = leftArr[i];
        i++;
        k++;
    }
    
    while (j < n2) 
    {
        arr[k] = rightArr[j];
        j++;
        k++;
    }
    
    free(leftArr);
    free(rightArr);
}

void sequentialMergeSort(int arr[], int left, int right) 
{
    if (left < right) 
    {
        int mid = left + (right - left) / 2;
        sequentialMergeSort(arr, left, mid);
        sequentialMergeSort(arr, mid + 1, right);
        merge(arr, left, mid, right);
    }
}

void parallelMergeSort(int arr[], int left, int right) 
{
    if (right - left <= CUTOFF) 
    {
        sequentialMergeSort(arr, left, right);
        return;
    }
    
    if (left < right) 
    {
        int mid = left + (right - left) / 2;
        
        #pragma omp task shared(arr) if(right-left > CUTOFF*2)
        parallelMergeSort(arr, left, mid);
        
        #pragma omp task shared(arr) if(right-left > CUTOFF*2)
        parallelMergeSort(arr, mid + 1, right);
        
        #pragma omp taskwait
        merge(arr, left, mid, right);
    }
}

int main() 
{
    int i;
    const int n = 1000000;
    int *arr = (int*)malloc(n * sizeof(int));
    
    srand(time(NULL));
    for (i = 0; i < n; i++)
    {
        arr[i] = rand();
    }
        
    int *arr_seq = (int*)malloc(n * sizeof(int));
    for (i = 0; i < n; i++)
    {
        arr_seq[i] = arr[i];
    }
        
    printf("Array size: %d elements\n", n);
    
    clock_t seq_start = clock();
    sequentialMergeSort(arr_seq, 0, n - 1);
    clock_t seq_end = clock();
    double seq_time = ((double)(seq_end - seq_start)) / CLOCKS_PER_SEC;
    printf("Sequential time: %f seconds\n", seq_time);
    
    int thread_counts[] = {1, 2, 4, 8, 16};
    int num_tests = sizeof(thread_counts) / sizeof(thread_counts[0]);
    
    for (int t = 0; t < num_tests; t++) 
    {
        int num_threads = thread_counts[t];
        
        for (i = 0; i < n; i++)
            arr[i] = arr_seq[i];
        
        omp_set_num_threads(num_threads);
        
        double start_time = omp_get_wtime(); 
        
        #pragma omp parallel
        {
            #pragma omp single nowait
            {
                parallelMergeSort(arr, 0, n - 1);
            }
        }
        
        double end_time = omp_get_wtime();
        double par_time = end_time - start_time;
        
        printf("Parallel with %2d threads: %f seconds (Speedup: %.2fx)\n", num_threads, par_time, seq_time/par_time);
        
        for (i = 0; i < n-1; i++) 
        {
            if (arr[i] > arr[i+1]) 
            {
                printf("ERROR: Sort failed at position %d\n", i);
                break;
            }
            if (i > 100) 
            {
                break;
            }
        }
    }
    
    free(arr);
    free(arr_seq);
    return 0;
}
