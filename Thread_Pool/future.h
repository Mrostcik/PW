#include <pthread.h>
#include <stdbool.h>

#ifndef FUTURE_H_
#define FUTURE_H_

#include "threadpool.h"
typedef struct callable {
  void *(*function)(void *, size_t, size_t *);
  void *arg;
  size_t argsz;
} callable_t;

typedef struct future {
	void *result;
	size_t *size;
	callable_t callable;
	pthread_mutex_t guard;
	pthread_cond_t result_wait;
	bool result_ready;
} future_t;

int async(thread_pool_t *pool, future_t *future, callable_t callable);

int map(thread_pool_t *pool, future_t *future, future_t *from,
        void *(*function)(void *, size_t, size_t *));

void *await(future_t *future);


#endif /* FUTURE_H_ */
