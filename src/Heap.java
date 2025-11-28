public class Heap<T extends Comparable<T>> {
    private int size ;
    private T[] heap ;

    public Heap(int capacity) {
        this.heap =  (T[]) new Comparable[capacity + 1]; // +1 for 1-based indexing
        this.size = 0 ;
    }

    // Add element to heap
    public void add(T value) {
            // Check if heap is full and resize if necessary
            if (size + 1 >= heap.length) {
                resizeHeap();
            }

            size++ ;
            heap[size] = value ;
            int i = size;
            while (i > 1 && heap[i].compareTo(heap[i/2]) < 0) {
                // swap
                T temp = heap[i];
                heap[i] = heap[i/2];
                heap[i/2] = temp;
                i = i / 2;
            }
    }

    // Resize the heap array when it's full
    private void resizeHeap() {
        T[] newHeap = (T[]) new Comparable[heap.length * 2];
        System.arraycopy(heap, 0, newHeap, 0, heap.length);
        heap = newHeap;
    }


    // Remove and return root
    public T poll() {
        if (size > 0) {
            T root = heap[1];
            heap[1] = heap[size];
            heap[size] = null;
            size-- ;
            int i = 1;
            while (i * 2 <= size) {
                int child = i * 2;
                if (child + 1 <= size && heap[child + 1].compareTo(heap[child]) < 0) {
                    child = child + 1;
                }
                if (heap[i].compareTo(heap[child]) > 0) {
                    // swap
                    T temp = heap[i];
                    heap[i] = heap[child];
                    heap[child] = temp;
                    i = child;
                } else {
                    break;
                }
            }
            return root;
        }
       return null;
    }



    public int getSize() {
        return this.size ;
    }

    @Override
    public String toString() {
        return java.util.Arrays.toString(heap);
    }

    // Example usage
    public static void main(String[] args) {
        Heap<Integer> minHeap = new Heap<Integer>(3);
        minHeap.add(5);
        minHeap.add(3);
        minHeap.add(8);
        minHeap.add(1);
        System.out.println("MinHeap: " + minHeap);
        System.out.println("Remove root: " + minHeap.poll());
        System.out.println("After poll: " + minHeap);
    }
}
