##
# Practical 2
#
# @file
# @version 0.1

all: P2

P2: classfiles
	jar cfe P2.jar Solver *.class

classfiles: BinaryConstraint.java BinaryCSP.java BinaryCSPReader.java BinaryTuple.java Solver.java
	javac *.java

clean:
	rm *.class P2.jar

# end
