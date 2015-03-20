<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Dashboard</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">

    <!-- Le styles -->
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet">

    <link href="/common/bootstrap/css/bootstrap-responsive.min.css" rel="stylesheet">

    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="/common/bootstrap/js/html5shiv.js"></script>
    <![endif]-->

    <style>
        body {
            padding-top: 70px;
        }

        .hero-unit {
            background-size: cover;
            background-position: top;
            position: relative;
            padding: 150px 10px 13px 15px;
        }

        .hero-unit h1 {
            color: white;

            position: relative;
            bottom: 0;
            left: 0;
            text-shadow: 0px 0px 10px #000000;
        }

        a:hover, a {
            color: white;
            text-decoration: none !important;
        }

        .downloads {
            margin-top: 3em;
            color: #303030;
            font-size: 20px;
        }

        .downloads a, .downloads a:hover {
            color: darkcyan !important;
        }

    </style>
</head>

<body>

<div class="navbar navbar-inverse navbar-fixed-top">
    <div class="navbar-inner">
        <div class="container">
            <button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="brand" href="#">Dashboard</a>
        </div>
    </div>
</div>

<div class="container">
    <div class="row">
        <div class="span4">
            <a href="http://prtp1fo1.mayo.edu">
                <div class="hero-unit" style="background-image: url(/images/loki.jpg);">
                    <h1>Loki</h1>
                </div>
            </a>
        </div>
        <div class="span4">
            <a href="file:////mfad/rchapp/odin/prod">
                <div class="hero-unit" style="background-image: url(/images/odin.jpg);">
                    <h1>Odin</h1>
                </div>
            </a>
        </div>
        <div class="span4">
            <a href="http://rofgmo100a/ganglia">
                <div class="hero-unit" style="background-image: url(/images/ganglia.jpg);">
                    <h1>Ganglia</h1>
                </div>
            </a>
        </div>
    </div>
    <div class="row">
        <div class="span4 offset4">
            <a href="http://protini.mayo.edu">
                <div class="hero-unit" style="background-image: url(/images/protini.jpg);">
                    <h1>Protini</h1>
                </div>
            </a>
        </div>
    </div>
    <!-- Download links -->
    <div>
        <div>
            <a href="file://///mfad/rchapp/odin/prod/apps/scaffold/scaffold_4.4.1/Install_Scaffold_4.4.1_Windows_x64.exe">Scaffold
                4.4.1 install</a> (Windows 64-bit)
        </div>
        <div><a href="file://///mfad/rchapp/odin/dev/source/teracopy/teracopy.exe">TeraCopy</a> - a tool for copying
            files between computers with verification
        </div>
        <div>
            <a href="https://chrome.google.com/webstore/detail/locallinks/jllpkdkcdjndhggodimiphkghogcpida">LocalLinks</a>
            - allows Chrome to access Scaffold files when clinking on a link
        </div>
    </div>
</div>
<!-- /container -->

<!-- Le javascript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->


</body>
</html>
