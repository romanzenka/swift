<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<!DOCTYPE html>
<html>
<head>
    <title>Configuration Instructions | Swift</title>
    <link rel="stylesheet" href="Configuration.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="/common/bootstrap/js/html5shiv.js"></script>
    <![endif]-->
    <style>
        body {
            padding-top: 60px; /* 60px to make the container go all the way to the bottom of the topbar */
        }
    </style>
</head>
<body>
<div class="container">
    <div class="navbar">
        <div class="navbar navbar-inverse navbar-fixed-top">
            <div class="navbar-inner">
                <div class="container">
                    <button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </button>
                    <a class="brand" href="/configuration">Swift Configuration</a>

                    <div class="nav-collapse collapse">
                        <ul class="nav">
                            <li class="active"><a href="/configuration/">Instructions</a></li>
                            <li><a href="/configuration/config.html">Configuration</a></li>
                        </ul>
                    </div>
                    <!--/.nav-collapse -->
                </div>
            </div>
        </div>
    </div>

    <div id="content" style="">
        <h2>Configuration Instructions</h2>

        <p>
            Welcome to Swift configuration screen. In order for Swift to work, you need to configure its components.
        </p>

        <p>
            Swift can run multiple search engines (Mascot, Sequest, Tandem and MyriMatch) and combine their
            results
            into a single Scaffold file. Swift can run on a single computer,
            multiple computers or an entire cluster. This page lets you set that all up.
        </p>

        <p>
            We recommend going through the items in the configuration tree, one by one.
        <ol>
            <li>Create all the daemons you plan to run Swift on</li>
            <li>Configure these daemons
                <ol>
                    <li>provide host name and OS type, this helps setting up the modules</li>
                    <li>for multi-module setup, do not forget to configure the shared disk space for each of the daemons
                    </li>
                </ol>
            </li>
            <li>Configure the database.
                <ul>
                    <li>The database stores file paths as they appear on the daemon that contains the database module.
                    </li>
                    <li>When moving the database setup to a different daemon, you might have to reconfigure the paths in
                        the database.
                    </li>
                    <li>The database module needs to be defined on same daemon that runs Searcher and Web UI
                        components.
                    </li>
                </ul>
            </li>
            <li>Configure the message broker</li>
            <li>Configure the Swift Searcher.
                <ul>
                    <li>As you go through the searcher configuration, create modules for services you want
                        to use.
                    </li>
                    <li>
                        Configure the <code>.fasta</code> directories for the searcher
                    </li>
                </ul>
            </li>
            <li>Configure all the remaining Swift modules</li>
            <li>Save the configuration</li>
        </ol>

        <p>
            When the configuration gets saved, the following will happen:
        <ol>
            <li>config file
                <code><%= SwiftWebContext.getWebUi().getNewConfigFile().getAbsolutePath() %>
                </code>
                gets created - it defines what services should Swift run, how and where
            </li>
            <li>config check is run, making sure all parts are configured properly</li>
            <li>installation steps are run, making sure that all requested folders are created, and that the database
                is seeded with initial data
            </li>
        </ol>

        <p>

        <p>
            When you are done configuring:

        <ol>
            <li>kill the configuration script</li>
            <li>copy the configuration file from
                <code><%= SwiftWebContext.getWebUi().getNewConfigFile().getAbsolutePath() %>
                </code> to <code>conf/swift.conf</code>.
                <p>This step is done so you have a chance to review the configuration file changes, instead of Swift
                    simply overwriting the last configuration.</p></li>
            <li>start the provided <code>bin/swift</code> script</li>
        </ol>
        </p>

        <p>
            Keep in mind that the daemon containing the embedded Message Broker module has to start first, so other
            modules can use it to communicate.
        </p>

        <a class="btn btn-primary" href="/configuration/config.html">Proceed to Configuration</a>
    </div>
</div>

</body>
</html>
