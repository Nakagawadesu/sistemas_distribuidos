## How to Compile and Run

### Prerequisites
- Java JDK 17+ installed
- GCC compiler installed (for C version)
- All source files in same directory:
### Steps

### Java Implementation

1. **Compile all classes**:
```bash
javac *.java
```
Run the program:
```bash
java MergeSortComparison
```

### **C Implementation** (mergeSort.c)
Compile with GCC:

```bash
gcc -fopenmp -o mergeSort mergeSort.c  # -fopenmp for OpenMP support if used
```
