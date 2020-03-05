#include "future.h"
#include <pthread.h>
#include <semaphore.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>


void *silnia(void *arg, size_t argsz __attribute__((unused)), size_t *retsz __attribute__((unused))){

	int **arg2 = arg;
	int *x = arg2[0];
	int *y = arg2[1];

	int **result = malloc(sizeof(int*) * 2);

	int *one_p = malloc(sizeof(int));
	*one_p = 1;

	int *factorial_p = malloc(sizeof(int));
	*factorial_p = (*x)*(*y);

	int *y_increased_p = malloc(sizeof(int));
	*y_increased_p = *y+1;

	if(*x == 0 || *y == 0)
		result[0] = one_p;
	else
		result[0] = factorial_p;

	result[1] = y_increased_p;

	return result;
}


int main(){
	int n;
	scanf("%d", &n);
	if(n == 0){
		printf("%d\n", 0);
		return 0;
	}
	thread_pool_t pool;
	thread_pool_init(&pool, 3);
	future_t future[n];
	callable_t *callable = (callable_t*)malloc(sizeof(callable_t));

	int **result = malloc(sizeof(int*) * 2);
	int zero = 0;
	int one = 1;
	result[0] = &zero;
	result[1] = &one;

	callable->arg = result;
	callable->argsz = sizeof(int)*2;
	callable->function = silnia;

	async(&pool, &future[0], *callable);

	for(int i = 1; i <= n-1; i++){
		map(&pool, &future[i], &future[i-1], silnia);
	}

	int **res = await(&future[n-1]);
	int *res1 = res[0];
	printf("%d\n", *res1);

	if(!(&pool)->destroyed)
		thread_pool_destroy(&pool);

}

