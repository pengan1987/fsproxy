#makefile for FSP proxy server
SOURCES=*.java
DOC=README TODO CHANGES
VERSION=04
default:
build:
	javac *.java
clean:
	rm -f  *.class *.html *.zip doc
zip: build
	rm  -f fsproxy-*.zip
	zip -r fsproxy-$(VERSION) *.java *.class $(DOC) Makefile
	
.PHONY: default clean view zip
