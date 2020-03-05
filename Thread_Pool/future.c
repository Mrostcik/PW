#include "future.h"
#include <pthread.h>
#include <semaphore.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

void async_runnable(void *future_, size_t size __attribute__((unused))){
	future_t* future = (future_t*)future_;
	if(pthread_mutex_lock(&future->guard) == -1)
		syserr("pthread_mutex_lock of guard error");

	size_t result_size;
	void *arg = (&(future->callable))->arg;
	size_t argsz = (&(future->callable))->argsz;

	future->result = (&(future->callable))->function(arg, argsz, &result_size);
	future->size = &result_size;
	future->result_ready = true;

	if(pthread_cond_signal(&future->result_wait) == -1)
		syserr("pthread_cond_signal of result_wait error");
	if(pthread_mutex_unlock(&future->guard) == -1)
		syserr("pthread_mutex_unlock of guard error");
}

int async(thread_pool_t *pool, future_t *future, callable_t callable){
	future_t* new_future = (future_t*)malloc(sizeof(future_t));
	if(pool == NULL || new_future == NULL)
		return -1;

	new_future->callable = callable;
	new_future->result_ready = false;
	if(pthread_mutex_init(&new_future->guard, 0) == -1)
		syserr("pthread_mutex_init of guard error");
	if(pthread_cond_init(&new_future->result_wait, 0) == -1)
		syserr("pthread_cond_init of result_wait");

	*future = *new_future;

	runnable_t *runnable = (runnable_t*)malloc(sizeof(runnable_t));
	if(runnable == NULL)
		return -1;

	runnable->function = async_runnable;
	runnable->arg = (void*)future;
	runnable->argsz = sizeof(future);

	if(!defer(pool, *runnable))
		return -1;

	return 0;
}

void *await(future_t *future){
	void *result;
	if(pthread_mutex_lock(&future->guard) == -1)
		syserr("pthread_mutex_lock of guard error");

	while(!future->result_ready)
		if(pthread_cond_wait(&future->result_wait, &future->guard) == -1)
			syserr("pthread_cond_wait of result_wait error");


	result = future->result;

	if(pthread_mutex_unlock(&future->guard) == -1)
		syserr("pthread_mutex_unlock of guard error");

	return result;
}

int map(thread_pool_t *pool, future_t *future, future_t *from,
        void *(*function)(void *, size_t, size_t *)){
	if(pool == NULL || from == NULL || function == NULL)
		return -1;

	void *result = await(from);

	callable_t *callable = (callable_t*)malloc(sizeof(callable_t));
	if(callable == NULL)
		return -1;

	callable->function = function;
	callable->arg = result;
	callable->argsz = *from->size;

	if(!async(pool, future, *callable))
		return -1;

	return 0;
}
