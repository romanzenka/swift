Mayo Proteomics Core Services
=============================

Services are configurable modules that provide functionality,
e.g. running an X!Tandem search. 

The implementation is currently not based
on web services, instead we rely on a daemon project at https://github.com/romanzenka/lib/tree/master/daemon that uses JMS for sending messages about progress.
A web service wrapper might be an interesting addition.

Services
--------

### Search Engines

* mascot - support for Matrix Software Mascot, uses web scraping to submit searches
* myrimatch - support for Myrimatch search engine
* omssa - support for OMSSA search engine
* peaks-online - incomplete support for submitting Peaks Online searches
* sequest - Sequest search engine support
* xtandem - X!Tandem search engine support

### Additional support

* db-undeploy - Database undeployer - can remove a database deployed to Mascot/Sequest, etc...
* mgf2mgf - ensures that input .mgf files have proper metadata in their titles that the rest of search engines (and Scaffold) depend on
* msmseval - support for msmsEval tool for determining spectral quality
* qa - Quality Assurance - calculates statistics about the quality of .RAW files
* qstat - helper running the 'qstat' Sun Grid Engine command to provide information about submitted tasks
* raw2mgf - uses Thermo's extract_msn.exe to convert .RAW files to .mgf
* scaffold - support for Proteome Software Scaffold Batch version 3.* and 4.*
* search-db - work in progress - database of Swift's search results
* swift-db - Access to Swift's database (not an actual service)
* swift-search - Runs a Swift search combining all the other provided services

Compilation
-----------

    mvn install

Dependencies
------------

All the modules depend on several third-party libraries that are obtained from http://informatics.mayo.edu/maven Nexus repository.
