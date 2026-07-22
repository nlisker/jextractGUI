// This test header file is to contain all possible parsable symbols by jextract
// https://github.com/openjdk/jextract/blob/master/doc/GUIDE.md#using-the-code-generated-by-jextract
#pragma once

// types
extern __declspec(dllexport) void update(double* la, char c, unsigned char uc, short s, unsigned short us,
			int i, unsigned int un, long l, unsigned long ul, long long ll, float f, double d, long double ld, short int id,
			unsigned long long int lli);

// function
extern __declspec(dllexport) void update2(short s);

// function - variadic
void varfunc(int y, ...);

// var
int aVar = 1;

// var - const
const int aConst = 4;

// var - pointer
double* p;

// var - array
short array[3];

// var - function pointer
int (*fp)(int x);

// macro
#define M 5

// enum
enum E {
	e1 = 2,
	e2 = 3
};

// struct
struct S {
	int si;
	double* da;
};

// struct - bit-fields
struct SB {
   unsigned int w : 1;
   unsigned int h : 1;
};

// struct - nested
struct SN {
    struct {
        int i1;
    };
	int i2;
};

// union
union U {
   int di;
   char ca[10];
};

// typedef
typedef struct S T;
