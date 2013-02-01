<!DOCTYPE HTML>
<html lang="en">
<head>
    <title>Swift Extras</title>
    <link href="common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
</head>
<body>
<div class="container">
    <h1>Swift Extras</h1>
    <ul>
        <li><a href="/configuration">Configuration</a></li>
        <li><a href="/feed">RSS Feed</a></li>
        <li><a href="/amino-acids">Defined Amino Acids</a></li>
        <li><a href="/mods">Defined Modifications</a></li>
        <li><a href="/find-protein">Find proteins</a></li>
    </ul>

    <h2>Time Reporting</h2>

    <form action="/time-report">
        <label for="start">Start</label>
        <input type="text" name="start" id="start" value="2011-08-01"/>
        <label for="end">End</label>
        <input type="text" name="end" id="end" value="2011-09-01"/>
        <label for="screen">Display on screen</label>
        <input type="checkbox" name="screen" id="screen" value="1" checked="true"/>
        <input type="submit" name="submit" value="Get Report"/>
    </form>

</div>
<script src="common/bootstrap/js/jquery_1.9.0.min.js"></script>
<script src="common/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>
