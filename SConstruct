# Process this file with http://www.scons.org

# init Scons
EnsureSConsVersion(0,96)
PACKAGE='fsproxy'
VERSION='07'

#import env. variabless PATH and CLASSPATH
import os
env = Environment(ENV = {'PATH' : os.environ['PATH'], 'CLASSPATH' : os.environ['CLASSPATH']}, tools = Split("javac zip"))
#Fix javac command line for jdk1.1
env['JAVACCOM'] = '$JAVAC $JAVACFLAGS -d ${TARGET.attributes.java_classdir} $SOURCES'

# process build rules
Export( Split("env PACKAGE VERSION"))
env.SConscript(dirs=['.'])
