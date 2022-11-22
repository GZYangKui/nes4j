
#ifndef NATIVE_TYPE_H
#define NATIVE_TYPE_H

#define True (1)
#define False (0)

typedef int bool;
typedef void *Object;
typedef char *String;
typedef unsigned char byte;
typedef unsigned long usize;
typedef struct LinkedList0 LinkedList;
typedef struct SoundHardware0 SoundHardware;

struct SoundHardware0 {
    Object context;
    int hash_code;
};

struct LinkedList0 {
    Object content;
    struct LinkedList *pre;
    struct LinkedList *next;
};


#endif //NATIVE_TYPE_H
