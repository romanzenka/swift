Swift
=====

Search multiple tandem mass spec. datafiles using multiple search engines at once: Mascot, Sequest, X!Tandem, and MyriMatch.

### Swift inputs

Swift accepts one or many raw, mgf, mzML or Agilent .d files. You can process separate files or entire directories.

### Swift outputs

Swift produces Scaffold 3 reports (.sf3 files). You can view these reports on your own computer, just download and install the free Scaffold 3 viewer. There are several possibilities how to map input files to Scaffold reports. 

Build
-----

To build Swift, the following is required:

* Java Development Kit 6 (or 7) ( http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase6-419409.html#jdk-6u27-oth-JPR )
* Maven 3.0.3 or newer ( http://maven.apache.org/download.html )
* git (optional) to obtain the source

Once you have Java and Maven setup, you can build Swift as follows:

#### Get Swift from github

	git clone git://github.com/romanzenka/swift.git

#### Create swift-$VER-install.tar.gz

	cd swift
	mvn install -DskipTests
	cd swift/installer/target
	ls
	# You should see swift-$VER-install.tar.gz

* If you want to run all the unit tests, feel free to omit the -DskipTests clause!


#### Congratulations!

You are ready to install Swift.

* Copy the swift/installer/target/swift-$VER-install.tar.gz to your target machine.
* Unzip it
* Install instructions are in
	swift-$VER/INSTALL.txt

Please mail zenka.roman@mayo.edu if you have any problems!