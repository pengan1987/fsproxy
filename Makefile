#makefile for FSP proxy server
SOURCES=*.java
DOC=README TODO CHANGES
VERSION=07

default: build
build::
	javac *.java
clean::
	rm -f  *.class *.html *.zip doc
zip: build
	rm  -f fsproxy-*.zip
	zip -r fsproxy-$(VERSION) *.java *.class $(DOC) Makefile
	-advzip -z4 fsproxy-$(VERSION).zip
	
.PHONY: default clean view zip
