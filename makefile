SHELL = /bin/zsh
OUT_DIR = out
SRC_DIR = src
JC = javac -d $(OUT_DIR)

main: 
	$(JC) $(SRC_DIR)/**/*.java

.PHONY: clean

clean :
	rm -rf $(OUT_DIR)/*
