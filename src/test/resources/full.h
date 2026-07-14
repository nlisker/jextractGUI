#pragma once

extern __declspec(dllexport) void update(double* la, char c, unsigned char uc, short s, unsigned short us,
			int i, unsigned int un, long l, unsigned long ul, long long ll, float f, double d, long double ld, short int id,
			unsigned long long int lli);

extern __declspec(dllexport) void update2(short s);

double* p;

const C = 4;

#define F 5;

int i = 1;

enum E {
	e1 = 2,
	e2 = 3
};

struct S {
	int si;
	double* da;
};

typedef struct S S1;

// bit-fields
struct B {
   unsigned int w : 1;
   unsigned int h : 1;
};

union D {
   int di;
   char ca[10];
};

short s[];
