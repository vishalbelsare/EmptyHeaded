#ifndef _QUERY_HASHSTRING_H_
#define _QUERY_HASHSTRING_H_

#include <vector>
#include <stdint.h>
#include <tuple>
#include "Trie.hpp"
#include "Encoding.hpp"

#ifdef EXECUTABLE
#include "main.hpp"
#else
struct application{};
#endif

struct ParMMapBuffer;
struct ParMemoryBuffer;

//template types are the types of the attributes, followed by the type of the annotation
struct Query_HASHSTRING : public application {
  void* result_HASHSTRING;

	Query_HASHSTRING(){}
	void run_HASHSTRING();
};

#ifdef EXECUTABLE
application* init_app(){
  return new Query_HASHSTRING(); 
}
#endif

#endif