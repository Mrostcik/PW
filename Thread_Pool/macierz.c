#include "future.h"
#include <pthread.h>
#include <semaphore.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <unistd.h>

void *compute_value(void *arg, size_t argsz __attribute__((unused)), size_t* retsz __attribute__((unused))){
	int **arg2 = arg;
	int *x = arg2[0];
	int *y = arg2[1];

	int *result = malloc(sizeof(int));
	*result = *x;

	usleep(1000* (*y));

	return result;
}


int main(){
	int k,n;
	scanf("%d%d", &k, &n);

	int x[k*n],y[k*n];
	int sumy[k];

	thread_pool_t pool;
	thread_pool_init(&pool, 4);
	future_t future[k*n];
	callable_t *callable[k*n];
	int **result[k*n];

	for(int i = 0; i < k*n; i++){
		sumy[i/n] = 0;
		result[i] = malloc(sizeof(int*) * 2);
		callable[i] = (callable_t*)malloc(sizeof(callable_t));

		scanf("%d%d", &x[i], &y[i]);
		result[i][0] = &x[i];
		result[i][1] = &y[i];
		callable[i]->arg = result[i];
		callable[i]->argsz = sizeof(int)*2;
		callable[i]->function = compute_value;

		async(&pool, &future[i], *callable[i]);
	}

	for(int i = 0; i < k*n; i++){
		int* res = await(&future[i]);
		sumy[i/n] += *res;
	}

	for(int i = 0; i < k; i++)
		printf("%d\n",sumy[i]);
	
	if(!(&pool)->destroyed)
		thread_pool_destroy(&pool);
}


