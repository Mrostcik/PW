#include <stdlib.h>
#include <stdbool.h>
#include <pthread.h>
#ifndef THREADPOOL_H_
#define THREADPOOL_H_
#include "err.h"
typedef struct runnable {
  void (*function)(void *, size_t);
  void *arg;
  size_t argsz;
} runnable_t;

typedef struct tasks_list{
	runnable_t job;
	struct tasks_list* next;
	struct tasks_list* prev;
	size_t size;
} task;

typedef struct thread_pool {
	pthread_t** threads;
	size_t max_pool_size;
	size_t current_pool_size;
	pthread_mutex_t guard;
	pthread_cond_t has_tasks;
	pthread_cond_t wait_all;
	task* tasks_front;
	task* tasks_back;
	bool destroyed;
} thread_pool_t;

typedef struct pool_list{
	thread_pool_t* pool;
	struct pool_list* next;
	struct pool_list* prev;
} pool_list;

int thread_pool_init(thread_pool_t *pool, size_t pool_size);

void thread_pool_destroy(thread_pool_t *pool);

int defer(thread_pool_t *pool, runnable_t runnable);

#endif /* THREADPOOL_H_ */
