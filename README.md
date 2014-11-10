James
=====

James is a modern Java framework for discrete optimization using local search metaheuristics.
A wide range of generic optimization algorithms are provided that can be applied to any user-defined
problem by plugging in a custom solution type and corresponding neighbourhood. Predefined components
are included for subset selection.

### Modules

The James framework consists of several modules:
 
 - [James Core Module][core-module]: this module contains the core of the framework. It includes general
     components for both problem specification and search application. A wide range of generic local search
     algorithms are provided out-of-the-box, including random descent, steepest descent, tabu search, variable
     neighbourhood search and parallel tempering. Moreover, the core module contains implementations of specific
     components for subset selection as well as some specific subset sampling heuristics.
   
 - [James Extensions Module][extensions-module]: this module extends the core with additional tools
        for advanced problem specification and search customization. It also provides specific
        components for some other problem types besides subset selection (e.g. permutation problems).
                 
 - [James Examples Module][examples-module]: this module provides a series of example problem implementations
 	 as described at the [website][examples-website].

Dependencies
============

James requires Java 8 or later.

To perform logging, James depends on the [Simple Logging Facade for Java (SLF4J)][slf4j] which is a general
logging API that provides bindings for several popular Java logging frameworks including log4j, JDK 1.4 logging
and logback. To send all log messages generated by James to your favorite logging framework, simply include
the appropriate binding on your classpath as described in the [SLF4J user manual][slf4j-manual]. If no binding is
found, a warning

```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```

is printed to the console and all log messages are silently discarded. If you do not mind about log messages
it is perfectly safe to ignore this warning.

Download and install
====================

To get started with James, read the instructions on the [website][getstarted].

Building from source code
=========================

James is built using [Maven][maven], so compiling the source code should be as easy as running

```
mvn install
```

from inside the `james` directory

```
|-- james
  |-- james-core
  |-- james-examples
  |-- james-extensions
  |-- pom.xml
```

assuming that [Maven][maven] has been installed on your computer. This will compile the code, create jar packages and install them in your local Maven repository so that they can be added as dependencies in any other Maven project. After building James, you can also grab the created jar packages from the `target` directory within each module.

For the examples module, an additional jar including all dependencies is created in `james-examples/bin`.
To print an overview of the implemented examples and the corresponding usage information, execute

```
$ cd james-examples/bin
$ java -jar james-examples.jar
```

Documentation
=============

More information, user documentation and examples of how to use the framework are provided at the [website][james-website]. Additional developer documentation is posted on the [wiki][james-wiki].

License and copyright
=====================

All modules of the James framework are licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 and the LICENSE and NOTICE files included with each module.

Contact
=======

The James framework is developed and maintained by

 - Herman De Beukelaer (Herman.DeBeukelaer@UGent.be)
 
 
 
[core-module]:       https://github.com/hdbeukel/james/tree/master/james/james-core
[extensions-module]: https://github.com/hdbeukel/james/tree/master/james/james-extensions
[examples-module]:   https://github.com/hdbeukel/james/tree/master/james/james-examples
[examples-website]:  http://www.jamesframework.org/examples
[slf4j]:             http://www.slf4j.org
[slf4j-manual]:      http://www.slf4j.org/manual.html
[sonatype]:          https://oss.sonatype.org/index.html#welcome
[maven]:             http://maven.apache.org
[getstarted]:        http://www.jamesframework.org/getstarted/
[james-website]:     http://www.jamesframework.org
[james-wiki]:        http://github.com/hdbeukel/james/wiki

