<%@ page import="edu.mayo.mprc.heme.HemeReport" %>
<%@ page import="edu.mayo.mprc.heme.PeptideEntity" %>
<%@ page import="edu.mayo.mprc.heme.ProteinEntity" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<% HemeReport report = (HemeReport) request.getAttribute("report");
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    List<ProteinEntity> confirmedList = report.get_ProteinEntities_by_filter(ProteinEntity.Filter.MUTATION_CONFIRMED);
    List<ProteinEntity> relatedList = report.get_ProteinEntities_by_filter(ProteinEntity.Filter.RELATED_MUTANT);
    List<ProteinEntity> otherList = report.get_ProteinEntities_by_filter(ProteinEntity.Filter.OTHER);

%>
<%!
    public String colorizeSequence(String cigar, String seq) {
        String[] letter = cigar.replaceAll("[0-9]","").split("");
        String[] nums = cigar.split("[MIDNSHPX]");
        int index = 0;
        String htmlEmbedded = "";
        for(int i=0; i<nums.length;i++){
            int tail = Integer.parseInt(nums[i]) + index;
            if( letter[i+1].equals("M") ){
                htmlEmbedded += seq.substring(index, tail);
            }
            else{
                htmlEmbedded += "<span class=\"warn\">"+seq.substring( index, tail )+"</span>";
            }
            index = Integer.parseInt(nums[i]);
        }
        return htmlEmbedded;
    }

    public String nonBlankSpaces(int n){
        String ret="";
        for(int i=0;i<n;i++){
            ret += "&nbsp;";
        }
        return ret;
    }

    public String arrows(){
        return "<div style=\"width:14px;float:right;\"> <img src=\"/common/arrow_up_down.png\">  </div>";
    }

%>
<html>
<head>
    <title>HEME REPORT PAGE</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
    <script type="text/javascript" src="/common/bootstrap/js/bootstrap.js"></script>
    <script type="text/javascript" src="/common/bootstrap/js/tablesorter.js"></script>
</head>
<body>
<style>
  .monoText{
      font-family:"Courier New", Courier, monospace;
      font-size: 16pt;
  }
    .warn{
        color:red;
    }
</style>

<div class="container">
    <div class="navbar navbar-your-class navbar-static-top">
        <div class="navbar-inner">
            <a href="/" class="btn btn-info pull-right">Return to Swift</a>
                <h1>Report: <%=report.getName()%></h1>Target Mass: <%=report.getMass()%> &#177; <%=report.getMassTolerance()%></br>
                <small style="margin-left:20px;">Date: <%=format.format(report.getDate())%> </small>
        </div>
    </div>


    <h3>Hemoglobin Mutant Peptides</h3>

    <blockquote class="muted">
        These protein groups contain at least one protein that matches the requested intact mass within given tolerance.</br>
        If there are multiple proteins listed in a cell, it is because we could not distinguish between them based on the peptide evidence itself.
    </blockquote>


    <table id="confirmed" class="table table-striped">
        <thead>
        <tr>
            <th>Protein <%=arrows()%></th>
            <th>Peptides <%=arrows()%></th>
            <th>Spectra <%=arrows()%></th>
            <th>Description <%=arrows()%></th>
            <th>Mass <%=arrows()%></th>
            <th></th>
        </tr>
        </thead>
        <% for (ProteinEntity p : confirmedList) {%>
        <tr>
            <td><%=p.getAccNum()%></td>
            <td><%=p.getPeptides().size()%></td>
            <td style="text-align:center;"><%=p.getTotalSpectra()%></td>
            <td><%=p.getNeatDescription()%></td>
            <td><%=p.getMass()%></td>
            <td><a href="#modal_<%=p.getAccNum()%>" role="button" class="btn" data-toggle="modal">View</a></td>
        </tr>
        <%}%>
    </table>


    <h3>Related Mutant Proteins</h3>
    <blockquote class="muted">
        <p>These are proteins of interest whose mass delta did not match the query.</p>
    </blockquote>


    <table id="related" class="table table-striped">
                    <th>Protein</th>
                    <th>Spectra</th>
                    <th>Description</th>
                    <th>Mass</th>
        <% for (ProteinEntity p : relatedList) { %>
        <tr>
            <td><%=p.getAccNum()%></td>
            <td style="text-align:center;"><%=p.getTotalSpectra()%></td>
            <td><%=p.getNeatDescription()%></td>
            <td><%=p.getMass()%></td>
        </tr>
        <% } %>
    </table>


    <h3>Other Protein Ids</h3>
    <blockquote class="muted">
        <p>These proteins are unrelated or not targetted, but were detected in the sample.</p>
    </blockquote>

    <table id="other" class="table table-striped">
         <th>Protein</th>
         <th>Spectra</th>
         <th>Description</th>

        <% for (ProteinEntity p : otherList) {%>
        <tr>
            <td><a href="http://www.genome.jp/dbget-bin/www_bget?sp:<%=p.getAccNum()%>"><%=p.getAccNum()%></a></td>
            <td style="text-align:center;"><%=p.getTotalSpectra()%></td>
            <td><small><%=p.getNeatDescription()%></small></td>
        </tr>
        <%}%>
    </table>


</div>





    <!-- HOME FOR MODALS  -->
    <% for (ProteinEntity p : confirmedList) {%>
    <!-- Modal -->
    <div id="modal_<%=p.getAccNum()%>" class="modal hide fade" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
        <h3 id="myModalLabel"><%=p.getAccNum()%></h3>
      </div>
      <div class="modal-body">
          </br>
          <p class="monoText" style="font-weight: bold;"><%=colorizeSequence(p.getCigar(), p.getSequence())%></p>
             <% for (PeptideEntity pep : p.getPeptides()) { %>
                 <p class="monoText"><%= nonBlankSpaces(pep.getStart()) %><%=pep.getSequence()%></p>
             <%}%>
      </div>
      <div class="modal-footer">
        <button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
      </div>
    </div>
    <%}%>


<script>
    $(function () {
        $("#confirmed").tablesorter();
        $("#related").tablesorter();
        $("#other").tablesorter();
    });
</script>


</body>
</html>