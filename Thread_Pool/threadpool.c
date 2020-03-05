#include "threadpool.h"
#include <pthread.h>
#include <semaphore.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <signal.h>

 pthread_mutex_t global_guard = PTHREAD_MUTEX_INITIALIZER;
static volatile bool sigaction_set = false;
 pool_list* pools = NULL;

pool_list* new_pool_list(thread_pool_t* pool){
	pool_list* list = (pool_list*)malloc(sizeof(pool_list));
	if(list == NULL)
		return list;

	list->pool = pool;
	list->next = NULL;
	list->prev = NULL;

	return list;
}

void add_pool(pool_list* pools, pool_list* pool_add){
	pools->next = pool_add;
	pool_add->prev = pools;
}

void add_task(task* front, task *back, task *new_task){

	back->next = new_task;
	new_task->prev = back;
	front->size += 1;
}

task* get_task(task *list){
	if(list == NULL)
		return NULL;

	return list;
}

task* new_task(runnable_t runnable){
	task* list = (task*)malloc(sizeof(task));
	if(list == NULL)
		return list;

	list->job = runnable;
	list->next = NULL;
	list->prev = NULL;
	list->size = 1;

	return list;
}

void delete_tasks(task *list){
	if(list == NULL)
		return;

	while(list->next != NULL){
		task *new_list = list->next;
		free(list);
		list = new_list;
	}

	free(list);
}

void sigint_handler(int sig_id __attribute__((unused))){
	if(pthread_mutex_lock(&global_guard) == -1)
		syserr("pthread_mutex_lock of global_guard error");

	while(pools != NULL){
		pool_list* next = pools->next;
		thread_pool_destroy(pools->pool);
		free(pools);
		pools = next;
	}
	pools = NULL;

	if(pthread_mutex_unlock(&global_guard) == -1)
		syserr("pthread_mutex_unlock of global_guard error");
}

void *worker(void *pool_){
	thread_pool_t* pool = (thread_pool_t*)pool_;
	task *toDo = NULL;

	while(true){
		if(pthread_mutex_lock(&pool->guard) == -1)
			syserr("pthread_mutex_lock of guard error");

		while(pool->tasks_front == NULL && !pool->destroyed){
			if(pthread_cond_wait(&pool->has_tasks, &pool->guard) == -1)
				syserr("pthread_cond_wait of guard error");
		}

		if(pool->destroyed && pool->tasks_front == NULL){
			pool->current_pool_size--;
			if(pthread_cond_broadcast(&pool->has_tasks) == -1)
				syserr("pthread_cond_broadcast of has_tasks error");
			if(pool->current_pool_size == 0)
				if(pthread_cond_signal(&pool->wait_all) == -1)
					syserr("pthread_cond_signal of wait_all error");
			if(pthread_mutex_unlock(&pool->guard) == -1)
				syserr("pthread_mutex_unlock of guard error");
			break;
		}

		toDo = pool->tasks_front;
		pool->tasks_front = pool->tasks_front->next;
		if(pool->tasks_front == NULL)
			pool->tasks_back = NULL;

		if(pthread_mutex_unlock(&pool->guard) == -1)
			syserr("pthread_mutex_unlock of guard error");

		void *arg = (&toDo->job)->arg;
		size_t argsz = (&toDo->job)->argsz;
		(&toDo->job)->function(arg, argsz);
		free(toDo);

		if(pthread_mutex_lock(&pool->guard) == -1)
			syserr("pthread_mutex_lock of guard error");

		if(pool->destroyed && pool->tasks_front == NULL){
			if(pthread_cond_broadcast(&pool->has_tasks) == -1)
				syserr("pthread_cond_broadcast of has_tasks error");
		}

		if(pthread_mutex_unlock(&pool->guard) == -1)
			syserr("pthread_mutex_unlock of guard error");
	}

	return 0;
}

int thread_pool_init(thread_pool_t *pool, size_t pool_size){
	thread_pool_t *new_pool = (thread_pool_t*)malloc(sizeof(thread_pool_t));
	if(new_pool == NULL)
		return -1;

	*pool = *new_pool;

	if(pthread_mutex_init(&pool->guard, 0) == -1)
		syserr("pthread_mutex_init of guard error");
	if(pthread_cond_init(&pool->wait_all, 0) == -1)
		syserr("pthread_cond_init of wait_all error");
	if(pthread_cond_init(&pool->has_tasks, 0) == -1)
		syserr("pthread_cond_init of has tasks error");

	pool->current_pool_size = pool_size;
	pool->max_pool_size = pool_size;
	pool->destroyed = false;
	pool->threads = NULL;
	pool->tasks_front = NULL;
	pool->tasks_back = NULL;
	pool->threads = (pthread_t**)malloc(sizeof(pthread_t*) * pool_size);
	if(pool->threads == NULL)
		return -1;

	for(unsigned i = 0; i < pool_size; i++){
		pool->threads[i] = malloc(sizeof(pthread_t));
		if(pool->threads[i] == NULL){
			for(unsigned j = 0; j < i; j++)
				free(pool->threads[j]);

			free(pool->threads);
			return -1;
		}

		if(pthread_create(pool->threads[i], 0, worker, (void*)pool) == -1)
			syserr("pthread_create error");
		if(pthread_detach(*pool->threads[i]) == -1)
			syserr("pthread_detach error");
	}

	if(pthread_mutex_lock(&global_guard) == -1)
		syserr("pthread_mutex_lock of global_guard error");

	if(!sigaction_set){
		struct sigaction act;
		sigemptyset(&act.sa_mask);
		act.sa_flags = 0;
		act.sa_handler = sigint_handler;
		if (sigaction(SIGINT, &act, NULL) == -1)
			syserr("sigaction error");
	}

	pool_list* pool_list = new_pool_list(pool);
	if(pool_list == NULL)
		return -1;

	if(pools == NULL)
		pools = pool_list;
	else
		add_pool(pools, pool_list);

	if(pthread_mutex_unlock(&global_guard) == -1)
		syserr("pthread_mutex_unlock of global_guard error");
	return 0;
}

int defer(thread_pool_t *pool, runnable_t runnable){
	if(pool == NULL)
		return -1;

	if(pthread_mutex_lock(&pool->guard) == -1)
		syserr("pthread_mutex_lock of guard error");

	if(pool->destroyed)
		return -1;

	task* list = new_task(runnable);
	if(list == NULL)
		return -1;

	if(pool->tasks_back == NULL){
		pool->tasks_front = list;
		pool->tasks_back = list;
	}
	else{
		add_task(pool->tasks_front, pool->tasks_back, list);
		pool->tasks_back = list;
	}

	if(pthread_cond_signal(&pool->has_tasks) == -1)
		syserr("pthread_cond_signal of has_tasks error");
	if(pthread_mutex_unlock(&pool->guard) == -1)
		syserr("pthread_mutex_unlock of guard error");

	return 0;
}

void thread_pool_destroy(thread_pool_t *pool){
	if(pthread_mutex_lock(&pool->guard) == -1)
		syserr("pthread_mutex_lock of guard error");

	pool->destroyed = true;

	if(pthread_cond_broadcast(&pool->has_tasks) == -1)
		syserr("pthread_cond_broadcast of has_tasks error");
	if(pthread_cond_wait(&pool->wait_all, &pool->guard) == -1)
		syserr("pthread_cond_wait of guard error");


	for(unsigned i = 0; i < pool->max_pool_size; i++){
		free(pool->threads[i]);
	}
	free(pool->threads);
	pool->current_pool_size = 0;
	pool->max_pool_size = 0;

	if(pthread_cond_destroy(&pool->wait_all) == -1)
		syserr("pthread_cond_destroy of wait_all error");
	if(pthread_cond_destroy(&pool->has_tasks) == -1)
		syserr("pthread_cond_destroy of has_tasks error");
	delete_tasks(pool->tasks_front);

	if(pthread_mutex_destroy(&pool->guard) == -1)
		syserr("pthread_mutex_destroy of guard error");

}

