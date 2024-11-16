#ifndef RND_RANDOM_INCLUDED
#define RND_RANDOM_INCLUDED
/**
 * @file rnd_random.h
 * Simple support for pseudo random number generation. The C libs rand() is extended to
 * support floating point and Boolean data types and integers of different ranges are
 * supported.
 *
 * Copyright (C) 2021 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Include files
 */

#include <stdint.h>
#include <stdbool.h>


/*
 * Defines
 */

/* Note: The random macros make use of an unspecified function int random(void), which
   behaves like rand() from the C standard library. The C library may be used, but it is
   accessible only from process P1. Code running in other processes will require an own
   implementation of the function. Moreover, race conditions between different tasks or
   cores all using the macros may have to be handled by the chosen implementation of
   random(), too. */

/** A scaled floating point random number in the range [a, b). */
#define RND_FRAND(a,b) ((double)((b)-(a))*DRAND()+(double)(a))

/** A signed integer in the range [i, j]. */
#define RND_IRAND(i,j) (random()%((j)-(i)+1)+(i))

/** An unsigned integer in the range [i, j]. */
#define RND_URAND(i,j) ((unsigned int)random()%((j)-(i)+1u)+(i))

/** A Boolean random number with given probability p of getting a true. */
#define RND_BRAND(p) ((RND_DRAND()<(p))? true: false)


/** Floating point random number with more than 15 Bit resolution; taken fron
    http://www.azillionmonkeys.com/qed/random.html on Jan 23, 2017. */
#define RND_DRAND() ({                                                                       \
    double d;                                                                                \
    do {                                                                                     \
       d = (((random() * RND_RS_SCALE) + random()) * RND_RS_SCALE + random()) * RND_RS_SCALE;\
    } while(d >= 1); /* Round off */                                                         \
    d;                                                                                       \
})

/** Helper for #DRAND. */
#define RND_RS_SCALE (1.0 / (1.0 + RAND_MAX))


/*
 * Global type definitions
 */


/*
 * Global data declarations
 */


/*
 * Global prototypes
 */



/*
 * Global inline functions
 */


#endif  /* RND_RANDOM_INCLUDED */
