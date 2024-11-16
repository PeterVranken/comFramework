**A thread-safe Queue Implementation**

# Thread safe queue as linked list

## Data

-   Head, Tail: Pointers to first and last produced list elements, owned by producer
-   Read: Pointer to one list element, owned by consumer
-   isData: Owned by consumer. Set true by peek function if new data has been received. Set
    false after having this data consumed with read function.

## Initialization

    Head = Tail = Read = new Element
    Head->n = NULL
    isData = false

## Producer

The producer code is shown without an API to the queue, filling the new queue element with
data is part of the pseudo code:

    tmp = new Element
    fill tmp, tmp->n = NULL
    Tail->n = tmp
    Tail = tmp
    copyRead = Read
    while Head != copyRead
        tmp = Head->n
        free Head
        Head = tmp
    end

## Consumer

The consumer code is split into two APIs, the client code will first peek for new data and
use the read function only if new data is available. It's straightforward to combine
these two functions to a single non blocking read function, which may return an error if no
new data is available.

Consumer, API isDataAvailable:

    if !isData
        isData = Read->n != NULL
        if isData
            Read = Read->n
        end
    end
    return isData

Consumer, API getData:

    assert isDataAvailable() // stricter, therefore better: assert isData
    y = Read->...
    isData = false

## In short, condensed

### Producer

    Tail->n = new Element
    Tail = Tail->n
    while Head != Read
        free Head
        Head = Head->n
    end

### Consumer

    if Read->n != NULL
        Read = Read->n
        y = Read->...
    else
        y = NULL
    end

## Principle of operation

The basic principle of operation is to first do the data  operation and
then signal the completion of this operation to the other end by a single,
atomic operation. This operation is setting a pointer to a new value:

-   The producer updates the link pointer of the last element in order to let it
    point to the new data - after this data element has been prepared
-   The consumer moves his read pointer to the next queue element after
    having the data consumed
    
These pointers implicitly have the meaning of flags indicating new or
consumed data. Both stakeholders read the flag value of the other one but
they won't ever write on it, e.g. to acknowledge the information.
Therefore no complex atomic read-modify-write operations are required.

The implementation without read-modify-write is achieved by one additional
list element. Usually a list implementation will begin with an empty list
and all pointers are set to NULL. When the first element is produced then
the producer would have to update the read pointer of the consumer to
indicate the new element to him. Here, processing starts with a dummy
element, which is initializes as if it were an already consumed element at
run-time. At run-time the read pointer always remains on the last recently
consumed element and this element is the minimum list contents. The list
is never empty and the read and write pointers will never be set to NULL
or changed from NULL.

The concept of first producing/consuming the data and only then notifying
the other stakeholder requires strict execution order of these two
elements. Any real implementation will need to consider memory barriers
between the two steps.

# Implementation using ring buffer

The same concept can be implemented using a ring buffer and indexes
instead of pointers. A new concept is introduced. The ringbuffer implies
the idea of reusing the always same data buffers for the queue elements
after while. The length of the ring is limited and the degree of filling
becomes essential. Such a queue can be full, the producer can fail.

The degree of filling can be easily implemented by the producer; as for
the linked list solution he does both, entering new elements and removing
those, which are surely consumed. A race condition free update of a
counter of the available elements is straightforwardly implemented.

## Initialization

    H = T = R = 0
    noFree = sizeof(ringbuffer)-1

## Producer

The producer code is shown without an API to the queue, filling the next
queue element with data is part of the pseudo code:

    while H != R
        ++H
        ++noFree
    end
    if noFree
        --noFree
        newT = T+1 // Meant cyclically
        fill element newT
        T = newT
    end

## Consumer

The consumer code is offers an API, which peeks for new data. If there is
such new data it returns the reference to the ringbuffer element with the
new data. The client code owns this element until the consumer API is
invoked the next time and when it indicates the next new data. With other
words, received data is never explicitly returned or freed. This is done
implicitly by receiving the next data. One element of the ringbuffer is
thus permanently owned by the consumer.

Consumer:

    if R != T
        ++R // Meant cyclically
        return R
    else
        return -1
    end
