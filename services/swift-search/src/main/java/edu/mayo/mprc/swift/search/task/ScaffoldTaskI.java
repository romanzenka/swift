package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.workflow.engine.Task;

import java.io.File;

public interface ScaffoldTaskI extends Task {
    String getScaffoldVersion();

    File getResultingFile();

    File getScaffoldXmlFile();

    File getScaffoldSpectraFile();

    void addInput(FileSearch fileSearch, EngineSearchTask search);

    /**
     * Which input file/search parameters tuple gets outputs from which engine search.
     */
    void addDatabase(String id, DatabaseDeployment dbDeployment);

    /**
     * Set the main {@link ReportData} object that is associated with this task.
     * The task needs to know what report it stored into the database so further processes can link to it.
     *
     * @param reportData Report data saved in the database.
     */
    void setReportData(ReportData reportData);

    /**
     * @return {@link ReportData} linked with this Scaffold task. Points to a report containing the main Scaffold
     *         .sfd or .sf3 file. Null if the task has not completed yet.
     */
    ReportData getReportData();
}
