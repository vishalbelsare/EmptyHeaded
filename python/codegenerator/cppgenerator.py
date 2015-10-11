import cppexecutor
import os

def generate(f,name):
	#generate C++ code
	codeString = f()
	os.system("mkdir -p ../storage_engine/generated")
	cppfile = open("../storage_engine/generated/"+name+".cpp","w")
	cppfile.write(codeString)
	cppfile.close()
	os.system("clang-format -style=llvm -i ../storage_engine/generated/"+name+".cpp")

def compileC(name):
	#compile C++ code
	os.chdir("../storage_engine")
	try:
		os.system("make "+name+" >/dev/null")
	except:
		print "C++ compilation failed"
		sys.exit(1)

	os.chdir("../python")

def compileAndRun(f,name):
	generate(f,name) #generates C++ code
	compileC(name) #compiles C++ library
	cppexecutor.execute(name) 	#generates python wrapper and excutes C++ code

