Import(Split("env PACKAGE VERSION"))
sources=Split("fsploop.java  fspproxy.java fspreq.java")

docs=Split("README TODO CHANGES")
extra=Split("Makefile SConstruct SConscript") 


#build
classes=env.Java(target = '.',source = '.')

#make zip
ZIPFILE=PACKAGE+'-'+VERSION+'.zip'
env.Zip(ZIPFILE,docs)
env.Zip(ZIPFILE,extra)
env.Zip(ZIPFILE,classes)
env.Zip(ZIPFILE,sources)

