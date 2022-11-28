//
// Created by yangkui on 2022/11/26.
//

#include <string.h>
#include <malloc.h>
#include "include/str_util.h"

extern String Nes4j_str_clone(String src) {
    usize len = strlen(src);
    String dst = malloc(len + 1);
    memset(dst, '\0', len);
    usize pos = 0;
    while (pos < len) {
        *(dst + pos) = (char) *(src + pos);
        pos++;
    }
    return dst;
}
