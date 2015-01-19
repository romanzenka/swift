# Make sure the warnings get displayed
options(warn=1)

# Make everything go to stdout so we do not overwhelm the logs
sink(type = "message")

library("gam")
require("png")
require("caTools")

# Setup the colors
colorsByCharge<-c("lightgray","red","darkgreen","blue","orange","yellow","pink","purple")
legendForCharge<-c("0","1","2","3","4","5","6","7","8")
ci95Col <- rgb(235, 235, 235, maxColorValue=255)
colors.by.mslevel <- c(
  "red", # We do not smooth MS data
  "#DCDCFF", 
  "#DCFFDC",     
  "#FFFFDC")
colors.by.mslevel.smoothed <- c("red", "blue", "green", "yellow")
colors.by.mslevel.mean <- c( # Average lines
  "#FFAAAA", 
  "#AAAAFF", 
  "#AAFFAA",     
  "#FFFFAA")

# Plot names
lockmass.title<-"Mass Calibration vs. RT"
lockmass.title.nort<-"Mass Calibration vs. Scan Id"
calibration.title<-"Mass Calibration vs. m/z"
mz.title <- "Measured m/z vs. RT QA"
mz.title.nort <- "Measured m/z vs. Scan Id QA"
current.title <- "Source Current vs. RT QA"
current.title.nort <- "Source Current vs. Scan Id QA"
pepTol.title <- "Peptide Tolerance"
tic.title <- "TIC vs. RT"
msmsEval.title <- "msmsEval Discriminant Histogram"
uv.title <- "Pump Stats" # This is called "uv" as the pump pressure is measured using ultraviolet light

# Plot dimensions
plot.dimension.full <- c(1000, 800) # The actual plot dimensions
plot.dimension.thumb <- c(500, 400) # Dimension of the thumbnail. Should allow quality zooming
plot.dimension.web <- c(250, 200)   # Dimension set for the HTML code

# Calibration plot ranges
ppmRange1 <- c(-8, 8)
ppmRangeUnit1 <- "ppm"

ppmRange2 <- c(-2, 2)
ppmRangeUnit2 <- "Da"

# Setup the TIC plot
legend.by.mslevel <- c("MS", "MS/MS", "MS3", "MS4")
tic.moving.average <- 30 # Smooth 30 spectra (~3-6 seconds)

# Spectrum classes - utilities and data

# All spectra
spectrum.all.title <- "All Spectra"
spectrum.all.col <- "grey60"
spectrum.all.border <- "grey50"
spectrum.all.colHtml <- "#a00"

# MSn spectra
spectrum.msn.title <- "MSn Spectra"
spectrum.msn.col <- "grey60"
spectrum.msn.border <- "grey60"

# .dat spectra (MSn spectra that made it through the conversion)
spectrum.dat.title <- ".dat Spectra"
spectrum.dat.col <- "grey80"
spectrum.dat.border <- "grey70"

# Spectra identified by search engines
spectrum.id.title <- "Identified"
spectrum.id.col <- "red"
spectrum.id.colHtml <- "#c00"
spectrum.id.border <- "red3"
spectrum.id.test <- function(accessions) { !is.na(accessions) && !accessions=="" }
spectrum.id.symbol <- 20 # Small dot for normal hits

# Spectra identified by search engines while lockmass was on
spectrum.id.lm.title <- "Identified (LM)"
spectrum.id.lm.col <- "red3"
spectrum.id.lm.colHtml <- "#b00"
spectrum.id.lm.border <- "red4"

# Non-identified spectra
spectrum.nonid.title <- "Unidentified"
spectrum.nonid.col <- "lightblue"
spectrum.nonid.border <- "lightblue3"
spectrum.nonid.symbol <- 46 # Tiny dot

# Spectra identified as reverse hits
spectrum.rev.title <- "Reverse Hit"
spectrum.rev.col <- "orange"
spectrum.rev.colHtml <- "#c80"
spectrum.rev.border <- "orange"
spectrum.rev.test <- function(sequence, decoyRegex) { substring(sequence, 1, nchar(decoyRegex))==decoyRegex }
spectrum.rev.symbol <- 4 # X for reverse hits

# Spectra identified as polymers
spectrum.polymer.title <- "Polymer"
spectrum.polymer.col <- "violet"
spectrum.polymer.colHtml <- "#f0f"
spectrum.polymer.test <- function(polymer.segment.size, polymer.p.value) { polymer.segment.size==44 & polymer.p.value<=0.001 }
spectrum.polymer.symbol <- 25 # Reverse triangle

# Unfragmented spectra (there is a single tall peak matching the precursor mass)
spectrum.unfrag.title <- "Unfragmented"
spectrum.unfrag.col <- "deepskyblue1"
spectrum.unfrag.colHtml <- "#00bfff"
spectrum.unfrag.border <- "deepskyblue2"
spectrum.unfrag.test <- function(base.peak.intensity, second.peak.intensity, parent.mz, base.peak.mz, ms.level) { 
  ms.level>1 & 
    (second.peak.intensity==0 | (base.peak.intensity/second.peak.intensity>=50)) &
    abs(parent.mz-base.peak.mz)<1.5
}
spectrum.unfrag.symbol <- 8 # Star

# Dominant fragment spectra (there is a single tall peak not matching precursor)
spectrum.domfrag.title <- "Dominant Fragment"
spectrum.domfrag.col <- "dodgerblue1"
spectrum.domfrag.colHtml <- "#1e90ff"
spectrum.domfrag.border <- "dodgerblue2"
spectrum.domfrag.test <- function(base.peak.intensity, second.peak.intensity, parent.mz, base.peak.mz, ms.level) { 
  ms.level>1 & 
    (second.peak.intensity==0 | (base.peak.intensity/second.peak.intensity>=50)) &
    abs(parent.mz-base.peak.mz)>=1.5
}
spectrum.domfrag.symbol <- 24 # Triangle up

# Lockmass line
lockmass.found.col <- "grey"
lockmass.lost.col <- "red"

# UV plot configuration
uv.loading.pressure.color <- "#AA0000"        
uv.loading.pressure.title <- "Loading Pump (PSI)"

uv.nc.pressure.color <- "#0000AA" 
uv.nc.pressure.title <- "NC Pump (PSI)"

uv.column.temperature.color <- "#AA8800"
uv.column.temperature.title <- "Oven temperature (Celsius)"
uv.column.temperature.yspan <- 10 # degrees Celsius span for Y axis minimum

uv.percent.b.color <- "#008800"
uv.percent.b.title <- "%B"

uv.pressure.range <- range(0, 6000) # Fixed range for PSI.


# How to visualize a hit?
spectrum.symbol <- function(identified, reverse, polymer, unfrag, domfrag) {
  ifelse(polymer, spectrum.polymer.symbol, 
         ifelse(reverse, spectrum.rev.symbol, 
                ifelse(identified, spectrum.id.symbol, 
                       ifelse(unfrag, spectrum.unfrag.symbol,
                              ifelse(domfrag, spectrum.domfrag.symbol, spectrum.nonid.symbol)))))
}

# Color mapping for the chromatogram
# The RGB values are mapped in log10 scale. 0->1, #ffffff->10^10. Retrieve the exponent in log scale:
rgbToDouble <- function(rgb) { rgb<-col2rgb(rgb); val<-(rgb[1]*256+rgb[2])*256+rgb[3]; val<-val/(2^24-1)*10.0; }
GradientEntry <- function(r, g, b, val) { c(log10(val), rgb(r, g, b, maxColorValue=255)) }
chromaColors <- cbind(
  GradientEntry(240, 240, 240, 1),
  GradientEntry(200, 200, 200, 1e2),
  GradientEntry(168, 168, 255, 1e3),
  GradientEntry(180, 240, 240, 5e3),
  GradientEntry(168, 255, 168, 1e4),
  GradientEntry(255, 255, 168, 1e5),
  GradientEntry(255, 168, 168, 1e6),
  GradientEntry(255, 168, 255, 1e7),
  GradientEntry(140, 140, 140, 1e100)
)
chromaIntensities <- as.numeric(chromaColors[1,])
doubleToRgb <- function(v) {
  if(v==0.0) {
    return("#FFFFFF");
  }
  i<-findInterval(v, chromaIntensities);
  t<-(v-chromaIntensities[i])/(chromaIntensities[i+1]-chromaIntensities[i]);
  rgb1<-col2rgb(chromaColors[2,i]);
  rgb2<-col2rgb(chromaColors[2,i+1]);
  res<-rgb1*(1-t)+rgb2*t;
  rgb(res[1], res[2], res[3], maxColorValue=255)
}
chromatogram.recolor <- function(color) { doubleToRgb(rgbToDouble(color)) }

# Start a new plot (has default dimensions)
startPlot <- function(plotName, fileName) {
  print(paste("Generating '", plotName, "' image file: ", fileName, sep="")) 
  png(file=fileName, width=plot.dimension.full[1], height=plot.dimension.full[2])
}

emptyPlot <- function() {
  plot(x=c(), xlim=c(-1, 1), ylim=c(-1, 1), axes= F, xlab= "", ylab= "")
  text(0, 0, "No data available")
}

# Make a thumbnail from given .png file, shrinking it to width x height
makeThumb <- function(file, width, height) {
  img <- readPNG(file)
  # newFileName: /c/d/test.png -> test_thumb.png
  newFileName <- paste(sub("[.][^.]+$", "", basename(file)), "_thumb.png", sep="")
  png(file = file.path(dirname(file), newFileName), height = height, width = width)
  oldpar <- par(mar=c(0,0,0,0), xaxs="i", yaxs="i", ann=FALSE)
  plot(x=c(0, 100), y=c(0, 100), type='n', xaxt='n')
  lim <- par()
  rasterImage(img, lim$usr[1], lim$usr[3], lim$usr[2], lim$usr[4], interpolate=TRUE)
  par(oldpar)
  dev.off()
  newFileName
}

##Plot types
idVsPpm <- 1
mzVsPpm <- 2

# Shows a number as X (Y%) - if X=0, the percent part is not added
toPercentString <- function(x, total) { 
  if(!is.null(x) && x>0 && !is.null(total) && total>0) {
    return(paste(x, " (", round(100*x/total, digits=2), "%)", sep=""))
  } else {
    return (x)
  }
}

# Plot a portion of the data over the full histogram
subHistogramWithGaussian<-function(points, xlim, breaks, totalPoints, col, border) {
  subPlotPoints <- length(points)
  if(subPlotPoints>0) {
    subHistData <- hist(points, breaks=breaks, xlim=xlim, freq=F, plot=F)
    # the density is scaled down to be comparable to the total amount of points
    subHistData$density <- subHistData$density*subPlotPoints/totalPoints
    plot(subHistData, add=TRUE, axes=FALSE, ann=FALSE, col=col, border=border)
    
    curve(dnorm(x,mean=mean(points),sd=sd(points))*subPlotPoints/totalPoints,add=TRUE, col=border)
  }
  return(subPlotPoints)
}

# fits threedimensional data (time, mz, diff) and produces fitting curves for time and mz axis
# returns an object with 
# - lockmass.x and lockmass.y set for the lockmass plot (ppm vs. scan id)
# - calibration.x and calibration.y set for the calibration plot (ppm vs. m/z)
fitHits <- function(scanId, mz, diff) 
{
  tryCatch(
{
  r<-list()
  fit <- gam(diff ~ lo(scanId, span=0.10) + lo(mz, span=0.10))
  r$lockmass.x <- seq(from=min(scanId), to=max(scanId), length=100)
  midMz = min(mz)+max(mz)/2
  r$lockmass.y <- predict(fit, newdata = data.frame(scanId=r$lockmass.x, mz=midMz))
  r$lockmass.y <- r$lockmass.y-mean(r$lockmass.y)+mean(diff)
  
  r$calibration.x <- seq(from=min(mz), to=max(mz), length=100)
  midScanId = (min(scanId)+max(scanId))/2
  r$calibration.y <- predict(fit, newdata = data.frame(mz=r$calibration.x, scanId=midScanId))
  r$calibration.y <- r$calibration.y-mean(r$calibration.y)+mean(diff)
  r$s <- sd(residuals(fit))
  return(r)
}, error=function(err) { 
  print("ERROR when fitting data: ") 
  print(err) 
}   
  )

NA
}

# Try first range - if we get less than 50% of data, expand the range
# Return range number - 1 or 2
selectBestRange<-function(dataTab) {
  totalLength <- length(dataTab$Scan.Id)
  inYRange <- sum(dataTab$Actual.minus.calculated.peptide.mass..PPM. >= ppmRange1[1]*2 & dataTab$Actual.minus.calculated.peptide.mass..PPM. <= ppmRange1[2]*2)
  if(totalLength>0 && (inYRange+0.0)/totalLength < 0.5) {
    2
  } else {
    1
  }
}

# Produces either idVsPpm and mzVsPpm plots, together with loess fits
# Candidate for refactoring - split into smaller subsections
# plotType is one of above mentioned plot types
fitAndPpmPlots<-function(plotType, dataTab, spectrumInfo, curveColor, shadeColor95, plotTitle, xLabel, yLabel) {
  
  bestRange <- selectBestRange(dataTab)
  if(bestRange == 1) {
    yLim <- ppmRange1
    yLimUnit <- ppmRangeUnit1
  } else {
    yLim <- ppmRange2
    yLimUnit <- ppmRangeUnit2
  }
  
  totalLength <- length(dataTab$Scan.Id)
  
  yLabel <- paste(yLabel, " (", yLimUnit, ")", sep="")
  
  # We fit only data within the boundaries -0.5 Da to +0.5 Da (to discount points that 1-Da off) 
  # that are not reversed - reverse hits are noise, we do not want to taint our fits with them
  toProcess <- dataTab$Actual.minus.calculated.peptide.mass..AMU. >= -0.5 & dataTab$Actual.minus.calculated.peptide.mass..AMU. <= 0.5 & !dataTab$rev
  
  dataTabSubset <- dataTab[toProcess,]
  subsetContainsData <- sum(toProcess)!=0
  
  xLim <- c(0, 1) # Default limit so we do not crash on empty data
  
  if(is.null(spectrumInfo) || sum(!is.na(spectrumInfo$RT))==0) {
    timeDimension <- dataTab$Scan.Id;
  } else {
    timeDimension <- spectrumInfo$RT[match(dataTab$Scan.Id, spectrumInfo$Scan.Id)];
    xLim <- range(spectrumInfo$RT) # Our X-axis is the entire range of all retention times
  }
  timeDimensionSubset <- timeDimension[toProcess]    
  
  mzDimension <- dataTab$Observed.m.z
  mzDimensionSubset <- mzDimension[toProcess]
  
  if (plotType == idVsPpm) {        
    fullX <- timeDimension
    x <- timeDimensionSubset
  } else {
    fullX <- mzDimension
    x <- mzDimensionSubset
    xLim <- c(400, 1600) # Default limit
  }
  
  if (length(fullX)>0) {
    if (plotType == idVsPpm) {
      xLim <- range(xLim, fullX)
    } else {
      xLim <- range(fullX)
    }
  }    
  
  if(yLimUnit == "ppm") {    
    y <- dataTabSubset$Actual.minus.calculated.peptide.mass..PPM.
    fullY <- dataTab$Actual.minus.calculated.peptide.mass..PPM.
  } else {
    y <- dataTabSubset$Actual.minus.calculated.peptide.mass..AMU.
    fullY <- dataTab$Actual.minus.calculated.peptide.mass..AMU.        
  }
  
  plot(x, y, type="n", xlim=xLim, ylim=yLim,
       main=plotTitle,
       xlab=xLabel, ylab=yLabel, lab=c(5, 10, 7))
  
  if (subsetContainsData) {
    # Try fitting using gam - generalized additive model. If this fails, the fit information will not be displayed
    fit <- fitHits(timeDimensionSubset, mzDimensionSubset, y)
    if(is.list(fit)) {            
      if(plotType==idVsPpm) {
        xf <- fit$lockmass.x
        yf <- fit$lockmass.y
      } else {
        xf <- fit$calibration.x
        yf <- fit$calibration.y
      }                
      polygon(c(xf, rev(xf)), c(-(1.96 * fit$s)+yf, (1.96 * fit$s)+rev(yf)), border=NA, col=shadeColor95)
      lines(x=xf, y=yf, col = curveColor)
    }        
  }
  
  # We display lockmass shift on the ppm plot only if it has retention time as X axis
  if(plotType == idVsPpm && !is.null(spectrumInfo) && yLimUnit != 'Da') {
    ms1<-spectrumInfo$MS.Level==1
    lines(x=spectrumInfo$RT[ms1], y=spectrumInfo$Lock.Mass.Shift[ms1], col=lockmass.found.col)
    lines(x=ifelse(spectrumInfo$Lock.Mass.Found[ms1]==0, spectrumInfo$RT[ms1], NA), y=spectrumInfo$Lock.Mass.Shift[ms1], col=lockmass.lost.col)
  }
  
  spectrum.symbols <- spectrum.symbol(
    dataTab$identified,
    dataTab$rev, 
    rep(FALSE, times=length(fullX)),
    rep(FALSE, times=length(fullX)))
  points(fullX, fullY, pch=spectrum.symbols, col=colorsByCharge[dataTab$Z + 1])
  
  if(subsetContainsData && is.list(fit)) {
    legend("topleft", c(paste("95% +-", round(1.96 * fit$s, digits=2), " ", yLimUnit, sep="")), fill=c(ci95Col), bty="n")    
  }
  
  legend("bottomleft",
         c(
           paste("Total ids:", length(dataTab$Scan.Id),
                 "     reversed/sp:", toPercentString(sum(dataTab$rev), totalLength)),
           paste("Displayed ids: ", toPercentString(length(fullY[fullY >= yLim[1] & fullY <= yLim[2]]), totalLength),
                 "     reversed:", toPercentString(sum(dataTab$rev[fullY >= yLim[1] & fullY <= yLim[2]]), totalLength)
                 , sep="")
         ), bty="n")
  usedChargesLegend(dataTab$Z, spectrum.symbols)
}

# Draws a legend for used charges
# If identified is true, the legend is only for identified spectra (do not display polymer/bad frag)
usedChargesLegend <- function(charges, symbols) {
  usedCharges <- as.numeric(levels(as.ordered(as.factor(charges)))) + 1
  title<-c(legendForCharge[usedCharges])
  pch<-rep(spectrum.id.symbol, length(usedCharges))
  col<-c(colorsByCharge[usedCharges])
  
  sym <- spectrum.rev.symbol
  tit <- spectrum.rev.title
  num <- sum(symbols==sym)
  if(is.na(num)) num<-0
  if(num>0) {       
    title <- c(title, paste(tit, " (", num, ")", sep=""))
    pch <- c(pch, sym)
    col <- c(col, "black")
  }
  sym <- spectrum.polymer.symbol
  tit <- spectrum.polymer.title
  num <- sum(symbols==sym)
  if(is.na(num)) num<-0
  if(num>0) {       
    title <- c(title, paste(tit, " (", num, ")", sep=""))
    pch <- c(pch, sym)
    col <- c(col, "black")
  }
  sym <- spectrum.unfrag.symbol
  tit <- spectrum.unfrag.title
  num <- sum(symbols==sym)
  if(is.na(num)) num<-0
  if(num>0) {       
    title <- c(title, paste(tit, " (", num, ")", sep=""))
    pch <- c(pch, sym)
    col <- c(col, "black")
  }
  sym <- spectrum.domfrag.symbol
  tit <- spectrum.domfrag.title
  num <- sum(symbols==sym)
  if(is.na(num)) num<-0
  if(num>0) {       
    title <- c(title, paste(tit, " (", num, ")", sep=""))
    pch <- c(pch, sym)
    col <- c(col, "black")
  }
  
  if(length(title)>0) {
    legend("topright", title,
           pch=pch,
           col=col,
           title="Charge")
  }
}

addQaColumn<-function(columns, name, type) {
  if(name %in% names(columns)) {
    columns[[name]] <- type
  }
  columns
}

readQaFile<-function(dataFile, decoyRegex) {
  print(paste("Reading QA data file: ", dataFile))
  
  header<-read.delim(dataFile, header=TRUE, sep="\t", nrows=1, fileEncoding="UTF-8", quote="")
  colClasses<-rep("NULL", length(names(header)))
  names(colClasses)<-names(header)
  colClasses<-addQaColumn(colClasses, "Scan.Id", "integer")
  colClasses<-addQaColumn(colClasses, "Mz", "numeric")
  colClasses<-addQaColumn(colClasses, "Z", "integer")
  colClasses<-addQaColumn(colClasses, "Parent.m.z", "numeric")
  colClasses<-addQaColumn(colClasses, "Protein.accession.numbers", "character")
  colClasses<-addQaColumn(colClasses, "Peptide.sequence", "character")
  colClasses<-addQaColumn(colClasses, "Observed.m.z", "numeric")
  colClasses<-addQaColumn(colClasses, "Actual.peptide.mass..AMU.", "numeric")
  colClasses<-addQaColumn(colClasses, "Actual.minus.calculated.peptide.mass..PPM.", "numeric")
  colClasses<-addQaColumn(colClasses, "Actual.minus.calculated.peptide.mass..AMU.", "numeric")
  colClasses<-addQaColumn(colClasses, "discriminant", "numeric")
  colClasses<-addQaColumn(colClasses, "TIC", "numeric")
  colClasses<-addQaColumn(colClasses, "RT", "numeric")
  colClasses<-addQaColumn(colClasses, "MS.Level", "integer")
  colClasses<-addQaColumn(colClasses, "Polymer.Segment.Size", "numeric")
  colClasses<-addQaColumn(colClasses, "Polymer.Score", "numeric")
  colClasses<-addQaColumn(colClasses, "Polymer.p.value", "numeric")
  colClasses<-addQaColumn(colClasses, "Base.Peak.m.z", "numeric")
  colClasses<-addQaColumn(colClasses, "Base.Peak.Intensity", "numeric")
  colClasses<-addQaColumn(colClasses, "Second.Peak.Intensity", "numeric")
  colClasses<-addQaColumn(colClasses, "Source.Current..uA.", "numeric")
  colClasses<-addQaColumn(colClasses, "Lock.Mass.Found", "numeric")    
  colClasses<-addQaColumn(colClasses, "Lock.Mass.Shift", "numeric")  
  colClasses<-addQaColumn(colClasses, "UV.RT", "numeric")
  colClasses<-addQaColumn(colClasses, "PumpModule.NC_Pump..B", "numeric")
  colClasses<-addQaColumn(colClasses, "PumpModule.LoadingPump.Pressure", "numeric")
  colClasses<-addQaColumn(colClasses, "PumpModule.NC_Pump.Pressure", "numeric")
  colClasses<-addQaColumn(colClasses, "ColumnOven.Temperature", "numeric")
  
  dataTabFull<-read.delim(dataFile, header=TRUE, sep="\t", colClasses=colClasses, fileEncoding="UTF-8", quote="")
  
  # Fill in missing columns
  addColumns <- c("Source.Current..uA.", "Lock.Mass.Found", "Lock.Mass.Shift")
  for(addColumn in addColumns) {
     if(!(addColumn %in% colnames(dataTabFull))) {
        dataTabFull[, addColumn] <- 0
     }
  }
  
  # Add a few calculated columns
  dataTabFull$identified <- dataTabFull$Protein.accession.numbers!=""
  dataTabFull$rev <- spectrum.rev.test(dataTabFull$Protein.accession.numbers, decoyRegex)
  dataTabFull$polymer <- spectrum.polymer.test(dataTabFull$Polymer.Segment.Size, dataTabFull$Polymer.p.value) 
  if("Base.Peak.Intensity" %in% names(dataTabFull)) {
    dataTabFull$unfrag <- spectrum.unfrag.test(
      dataTabFull$Base.Peak.Intensity, 
      dataTabFull$Second.Peak.Intensity, 
      dataTabFull$Parent.m.z,
      dataTabFull$Base.Peak.m.z,
      dataTabFull$MS.Level)
    dataTabFull$domfrag <- spectrum.domfrag.test(
      dataTabFull$Base.Peak.Intensity, 
      dataTabFull$Second.Peak.Intensity, 
      dataTabFull$Parent.m.z,
      dataTabFull$Base.Peak.m.z,
      dataTabFull$MS.Level)
  } else {
    dataTabFull['unfrag'] <- 0
    dataTabFull['domfrag'] <- 0
  }
  
  dataTabFull
}

movingAverage <- function(x,n=5){
  kernel <- rep(1/n,n)
  if(length(x)>=n) {
    filter(x, kernel, sides=2)
  } else {
    x
  }
}

#' Generates series of images for one .RAW file
#'
#' @return a summary of results to be written in the summary.xml file 
imageGenerator<-function(dataFile, msmsEvalDataFile, infoFile, spectrumFile, chromatogramFile, outputImages, generate, decoyRegex) {
  # We return the passed file names as a part of our result
  result<-outputImages
  result$data.file <- dataFile
  
  if ("true" == generate || "TRUE" == generate) {
    
    plotName <- basename(file.path(dataFile))
    plotNameLen <- nchar(plotName)
    plotName <- substr(plotName, 1, plotNameLen-nchar(".sfX.sfs"))
    
    dataTabFull <- readQaFile(dataFile, decoyRegex)        
    
    spectrumInfo <- dataTabFull[,c("Scan.Id", "MS.Level", "RT", "TIC", "Source.Current..uA.", "Lock.Mass.Found", "Lock.Mass.Shift")]        
    if(sum(!is.na(spectrumInfo$RT))>0) {             
      ### Total Ion Current plot 
      startPlot(tic.title, outputImages$tic.file)
      
      ms <- spectrumInfo$MS.Level==1
      maxTic = 1e11;
      xlim <- c(0, max(spectrumInfo$RT))
      plot(
        x=NULL, 
        y=NULL,
        xlim=xlim,
        ylim=c(1, maxTic),
        main=c(plotName, tic.title), 
        xlab="Retention Time (min)", 
        ylab="TIC (log scale)",
        pch=20,
        log="y",
        type="n"
      )
      
      used.mslevel <- sort(unique(spectrumInfo$MS.Level))
      
      # Draw raw data first (only  MSn - they are expected to be more noisy)
      for(level in used.mslevel) {
        if(level!=1) {
          keep <- spectrumInfo$MS.Level==level
          lines(
            x=spectrumInfo$RT[keep], 
            y=spectrumInfo$TIC[keep]+1, 
            col=colors.by.mslevel[level]
          )     
        }
      }

      # Draw helper lines
      abline(h=1e3);
      abline(h=1e6);     
      abline(h=1e9);
      
      # Draw averages
      for(level in used.mslevel) {
        keep <- spectrumInfo$MS.Level==level
        
        mean.tic <- mean(spectrumInfo$TIC[keep])     
        mean.color <- colors.by.mslevel.mean[level]
        abline(h=mean.tic, col=mean.color)
        axis(4, at=mean.tic, labels=format(mean.tic, scientific=TRUE, digits=3), tick=TRUE, 
             col = mean.color, col.ticks = mean.color, col.axis = mean.color)
      }    
            
      # Draw smoothed data
      for(level in used.mslevel) {
        keep <- spectrumInfo$MS.Level==level        
        if(level!=1) {
          lines(
            x=spectrumInfo$RT[keep],
            y=exp(movingAverage(log(spectrumInfo$TIC[keep]+1), tic.moving.average)), # Average consecutive spectra (in log scale so the plots make sense)
            col=colors.by.mslevel.smoothed[level]
          )     
        }
      }
      
      # Draw the MS-line as is (no smoothing)
      lines(x=spectrumInfo$RT[ms],
            y=spectrumInfo$TIC[ms]+1,
            col=colors.by.mslevel[1]
      )
      
      legend("topright", legend.by.mslevel[used.mslevel], pch=20, col=colors.by.mslevel.smoothed[used.mslevel], title="MS Level")
      
      dev.off()
    }
    spectrumInfoAvailable<-!is.null(spectrumInfo)
    
    # Filter out all the MS data - hack because our format has changed
    result$spectrum.all.count <- length(unique(dataTabFull$Scan.Id))
    result$spectrum.msn.count <- length(unique(dataTabFull$Scan.Id[dataTabFull$MS.Level>1]))
    result$spectrum.polymer.count <- length(unique(dataTabFull$Scan.Id[dataTabFull$polymer]))
    result$spectrum.unfrag.count <- length(unique(dataTabFull$Scan.Id[dataTabFull$unfrag]))
    result$spectrum.domfrag.count <- length(unique(dataTabFull$Scan.Id[dataTabFull$domfrag]))
    
    dataTab <- subset(dataTabFull, nchar(as.character(Peptide.sequence))>0)
    
    # Load chromatogram
    chromatogram <- NULL
    if(file.exists(chromatogramFile)) {
      chromatogram <- read.gif(chromatogramFile, flip=TRUE)
      mzDims <- unlist(strsplit(chromatogram$comment[1], "[:,]"))            
      chromatogram.minMz <- as.double(mzDims[2])
      chromatogram.maxMz <- as.double(mzDims[3])
      chromatogram$col <- sapply(chromatogram$col, chromatogram.recolor)
    }
    
    # Generate images
    result$peptides.all.count <- length(unique(dataTab$Peptide.sequence))            
    result$peptides.rev.count <- length(unique(dataTab$Peptide.sequence[dataTab$rev]))
    
    # We do not count proteins, we count distinct GROUPS of proteins. E.g. if we see
    # ALBU_HUMAN, ALBU_RAT, ALBU_MOUSE assigned several times, we count them as a single protein only
    # This is an approximation to minimum hitting set problem and needs to be improved upon
    collectGroups <- function(x) { 
      proteinGroups<<-c(proteinGroups, list(sort(x)))
      0 
    }
    result$proteins.all.count <- length(unique(dataTab$Protein.accession.numbers))
    result$proteins.rev.count <- length(unique(dataTab$Protein.accession.numbers[dataTab$rev]))
    
    scanXlim <- c(0, 1000)
    mzXlim <- c(400, 1600)
    if (length(dataTab$Scan.Id)!=0) {
      scanXlim <- range(0, dataTab$Scan.Id)
      mzXlim <- range(0, dataTab$Observed.m.z)
    }
    
    xAxisTitleScanOrRT <- ifelse(spectrumInfoAvailable, "Retention Time (min)", "Scan Id")
    
    ### Lockmass QA - Scan ID versus ppm           
    startPlot(ifelse(spectrumInfoAvailable, lockmass.title, lockmass.title.nort), outputImages$lockmass.file)
    fitAndPpmPlots(idVsPpm, dataTab, spectrumInfo, "white", ci95Col, c(plotName, ifelse(spectrumInfoAvailable, lockmass.title, lockmass.title.nort)), xAxisTitleScanOrRT, "Measured m/z - theoretical m/z")
    dev.off()
    
    ### Mass calibration QA - m/z versus ppm
    startPlot(calibration.title, outputImages$calibration.file)            
    fitAndPpmPlots(mzVsPpm, dataTab, spectrumInfo, "white", ci95Col, c(plotName, calibration.title), "theoretical m/z", "Measured m/z - theoretical m/z")
    dev.off()
    
    ### Prepare MS x axis - either RT or Scan Ids of all MS scans
    msOnly <- dataTabFull[dataTabFull$MS.Level==1,]
    if(spectrumInfoAvailable) {
      xAxisMs <- spectrumInfo$RT[match(msOnly$Scan.Id, spectrumInfo$Scan.Id)];
    } else {
      xAxisMs <- msOnly$Scan.Id;        
    }
    
    ### Prepare MS2 x axis - either RT or Scan Ids of all MS2 scans
    ms2Only <- dataTabFull[dataTabFull$MS.Level>1,]
    if(spectrumInfoAvailable) {
      xAxis <- spectrumInfo$RT[match(ms2Only$Scan.Id, spectrumInfo$Scan.Id)];
    } else {
      xAxis <- ms2Only$Scan.Id;        
    }
    
    ### Scan ID versus m/z
    startPlot(ifelse(spectrumInfoAvailable, mz.title, mz.title.nort), outputImages$mz.file)
    
    if(sum(!is.na(xAxis))>0) {
      plot(xAxis, ms2Only$Mz, type="n",
           main=c(plotName, mz.title),
           xlab=xAxisTitleScanOrRT, 
           ylab="Measured m/z",
           xlim=range(0, xAxisMs, xAxis))
      
      if(!is.null(chromatogram)) {
        image(
          x=xAxisMs,
          y=seq(chromatogram.minMz, chromatogram.maxMz, length.out=dim(chromatogram$image)[2]),
          z=chromatogram$image,
          col=chromatogram$col,
          add=TRUE)
      }
      
      spectrum.symbols <- spectrum.symbol(
        ms2Only$identified,
        ms2Only$rev, 
        ms2Only$polymer,
        ms2Only$unfrag,
        ms2Only$domfrag) 
      points(xAxis, ms2Only$Mz, 
             pch=spectrum.symbols, 
             col=colorsByCharge[ms2Only$Z + 1])            
      usedChargesLegend(ms2Only$Z, spectrum.symbols)
    } else {
      emptyPlot()
    }
    
    dev.off()
    
    ### Source current vs. mass id/RT
    startPlot(ifelse(spectrumInfoAvailable, current.title, current.title.nort), outputImages$source.current.file)
    
    if(spectrumInfoAvailable && sum(!is.na(spectrumInfo$RT))) {
      plot(spectrumInfo$RT, spectrumInfo$Source.Current..uA., type="p",
           main=c(plotName, current.title),
           xlab=xAxisTitleScanOrRT, ylab="Source Current (uA)", pch=20)
    } else {
      emptyPlot()
    }
    dev.off()

    ### UV pump info    
    startPlot(uv.title, outputImages$uv.file)
    uvData <- dataTabFull[!duplicated(dataTabFull$UV.RT),]
    uvData <- uvData[!is.na(uvData$UV.RT),]
        
    if(nrow(uvData)>0) {
      
      oldPar <- par(mar=c(5,4,4,5)+.1)
            
      color <- uv.loading.pressure.color   
      plot(uvData$UV.RT, uvData$PumpModule.LoadingPump.Pressure, type="l", axes=FALSE, xlab=NA, ylab=NA, yaxt="n", col=color, lwd=2, ylim=uv.pressure.range)
      axis(side=2, at = pretty(uv.pressure.range), col=color, col.axis=color, lwd.ticks=1, lwd=-1)      
      mtext(uv.loading.pressure.title, side=2, line=3, col=color)
      result$uv.loading.pressure.mean <- mean(uvData$PumpModule.LoadingPump.Pressure)      
      
      par(new=TRUE)

      color <- uv.nc.pressure.color     
      plot(uvData$UV.RT, uvData$PumpModule.NC_Pump.Pressure, type="l", axes=FALSE, xlab=NA, ylab=NA, yaxt="n", col=color, lwd=2, ylim=uv.pressure.range)
      axis(side=2, at = pretty(uv.pressure.range), col=color, col.axis=color, lwd.ticks=1, lwd=-1, tck=0.01)    
      mtext(uv.nc.pressure.title, side=2, line=2, col=color)
      result$uv.nc.pressure.mean <- mean(uvData$PumpModule.NC_Pump.Pressure)

      par(new=TRUE)
      
      color <- uv.column.temperature.color
      result$uv.column.temperature.min <- min(uvData$ColumnOven.Temperature)
      result$uv.column.temperature.max <- max(uvData$ColumnOven.Temperature)
      uvMean <- mean(uvData$ColumnOven.Temperature)
      uvData.range <- range(uvData$ColumnOven.Temperature, uvMean-uv.column.temperature.yspan/2, uvMean+uv.column.temperature.yspan/2)
      plot(uvData$UV.RT, uvData$ColumnOven.Temperature, type="l", axes=FALSE, xlab=xAxisTitleScanOrRT, ylab=NA,
           main=c(plotName, uv.title), col=color, ylim=uvData.range)
      axis(side=4, at = pretty(uvData.range), col=color, col.axis=color, lwd.ticks=1, lwd=-1, tck=0.01, mgp = c(0, -1.4, 0) )
      mtext(uv.column.temperature.title, side=4, line=2, col=color)

      par(new=TRUE)
      
      color <- uv.percent.b.color 
      plot(uvData$UV.RT, uvData$PumpModule.NC_Pump..B, type="l", xlab=NA, ylab=NA, ylim=c(0, 100), yaxt="n", col=color, lwd=2)
      axis(side=4, at = pretty(range(c(0, 100))), col=color, col.axis=color, lwd.ticks=1, lwd=-1)
      mtext(uv.percent.b.title, side=4, line=1, col=color)
            
      par(oldPar)          
      
    } else {
      emptyPlot()
      result$uv.loading.pressure.mean <- NA
      result$uv.nc.pressure.mean <- NA
      result$uv.column.temperature.min <- NA
      result$uv.column.temperature.max <- NA      
    }
    dev.off()
    
    ### Peptide tolerance
    startPlot(pepTol.title, outputImages$pepTol.file)
    
    if(length(dataTab$Actual.minus.calculated.peptide.mass..PPM.)!=0) {
      if(selectBestRange(dataTab)==1) {
        pepTol.xlim<-ppmRange1
        pepTol.unit<-ppmRangeUnit1
      } else {
        pepTol.xlim<-ppmRange2
        pepTol.unit<-ppmRangeUnit2
      }
      
      if(pepTol.unit=="ppm") {
        pepTol.data <- dataTab$Actual.minus.calculated.peptide.mass..PPM.
      } else {
        pepTol.data <- dataTab$Actual.minus.calculated.peptide.mass..AMU.                
      }
      
      # Display double the amount of data compared to the original plot
      pepTol.xlim<-pepTol.xlim*2
      
      pepTol.reasonable.xlim <- c(pepTol.xlim[1]*5, pepTol.xlim[2]*5) # We will fit the data within yet wider range compared to what we display
      breaks <- c(-1E10, seq(pepTol.xlim[1], pepTol.xlim[2], length.out=40*5+1), 1E10)
      
      reasonable.point <- 
        ((pepTol.data>=pepTol.reasonable.xlim[1]) &
           (pepTol.data<=pepTol.reasonable.xlim[2]))
      
      histData <- hist(pepTol.data[reasonable.point],
                       main=c(plotName, pepTol.title),
                       xlab=paste("Measured m/z - theoretical m/z (", pepTol.unit, ")", sep=""),
                       col=spectrum.id.col,
                       border=spectrum.id.border,
                       breaks=breaks,
                       xlim=pepTol.xlim,
                       freq=F)           
      
      
      if(!is.null(spectrumInfo)) {
        
        ms2.lockmass <- rep.int(0, nrow(spectrumInfo))
        prev.lockmass <- spectrumInfo$Lock.Mass.Found[1]
        for(i in seq_along(ms2.lockmass)[-1]) {                    
          if(spectrumInfo$MS.Level[i]!=1) { 
            ms2.lockmass[i] = prev.lockmass
          } else {
            prev.lockmass = spectrumInfo$Lock.Mass.Found[i]
            ms2.lockmass[i] = 0
          }
        }
        
        subHistogramWithGaussian(
          points = pepTol.data[
            ms2.lockmass[dataTab$Scan.Id]!=0 & reasonable.point],
          xlim = pepTol.xlim,
          breaks = breaks,
          totalPoints = nrow(dataTab),
          col = spectrum.id.lm.col,
          border = spectrum.id.lm.border)            
      }
      
      subHistogramWithGaussian(
        points = pepTol.data[dataTab$rev & reasonable.point],
        xlim = pepTol.xlim,
        breaks = breaks,
        totalPoints = nrow(dataTab),
        col = spectrum.rev.col,
        border = spectrum.rev.border)
      
      if(spectrumInfoAvailable) {
        legend("topright", 
               c(spectrum.id.title, spectrum.id.lm.title, spectrum.rev.title),
               fill=c(spectrum.id.col, spectrum.id.lm.col, spectrum.rev.col))
      } else {
        legend("topright", 
               c(spectrum.id.title, spectrum.rev.title),
               fill=c(spectrum.id.col, spectrum.rev.col))                
      }
    } else {
      emptyPlot()
    }
    
    dev.off()
    
    msmsDataTab <- dataTabFull[!is.na(dataTabFull$discriminant), c("Scan.Id", "discriminant")]
    duplicatedMsmsDataRows <- duplicated(msmsDataTab$Scan.Id)
    msmsDataTab <- msmsDataTab[!duplicatedMsmsDataRows,]
    
    if (nrow(msmsDataTab)>0) {
      startPlot(msmsEval.title, outputImages$msmsEval.file)
      
      # We use fixed range for the discriminant scores to make the graphs visually comparable
      msmsEval.xlim <- c(-15, 20)
      # First and last bin extend to +-1E5 and are catching the outliers
      histogramBreakPoints <- c(-1E5, seq(msmsEval.xlim[1], msmsEval.xlim[2], length.out=106), 1E5)
      
      spectrum.dat.count <- length(msmsDataTab$discriminant)
      
      result$spectrum.dat.count <- spectrum.dat.count
      
      histData <- hist(
        msmsDataTab$discriminant, 
        main=c(plotName, msmsEval.title), 
        xlab="msmsEval Discriminant", 
        col=spectrum.dat.col, 
        border=spectrum.dat.border, 
        breaks=histogramBreakPoints, 
        xlim=msmsEval.xlim, 
        freq=F)
      
      if (length(dataTab$Scan.Id) > 0) {
        spectrum.id.scans <- (msmsDataTab$Scan.Id %in% dataTab$Scan.Id)                    
        reverseScanIds <- dataTab$Scan.Id[dataTab$rev]
        spectrum.rev.scans <- (msmsDataTab$Scan.Id %in% reverseScanIds)
        unfragScanIds <- dataTabFull$Scan.Id[dataTabFull$unfrag]
        spectrum.unfrag.scans <- (msmsDataTab$Scan.Id %in% unfragScanIds)
        domfragScanIds <- dataTabFull$Scan.Id[dataTabFull$domfrag]
        spectrum.domfrag.scans <- (msmsDataTab$Scan.Id %in% domfragScanIds)
        
        # Plots a part (boolean vector) of all data as additional histogram
        msmsEvalSubHist <- function(part, col, border) {
          subCount <- subHistogramWithGaussian(
            points = msmsDataTab$discriminant[part],
            xlim = msmsEval.xlim,
            breaks = histogramBreakPoints,
            totalPoints = spectrum.dat.count,
            col = col,
            border = border)                    
          return (subCount)
        }
        
        # Non-IDed spectra
        spectrum.nonid.count<-msmsEvalSubHist(part = !spectrum.id.scans, col = spectrum.nonid.col, border = spectrum.nonid.border)
        result$spectrum.nonid.count<-spectrum.nonid.count
        # Unfragmented spectra
        spectrum.unfrag.count<-msmsEvalSubHist(part = spectrum.unfrag.scans, col = spectrum.unfrag.col, border = spectrum.unfrag.border)                   
        # Dominant fragment spectra
        spectrum.domfrag.count<-msmsEvalSubHist(part = spectrum.domfrag.scans, col = spectrum.domfrag.col, border = spectrum.domfrag.border)                   
        # IDed spectra
        spectrum.id.count<-msmsEvalSubHist(part = spectrum.id.scans, col = spectrum.id.col, border = spectrum.id.border)
        result$spectrum.id.count<-spectrum.id.count
        # IDed reverse spectra
        spectrum.rev.count<-msmsEvalSubHist(part = spectrum.rev.scans, col = spectrum.rev.col, border = spectrum.rev.border)
        result$spectrum.rev.count<-spectrum.rev.count
        
        legendPercents <- function(title, count) { paste(title, toPercentString(count, spectrum.dat.count)) }
        legend("topright", 
               c(legendPercents(spectrum.dat.title, spectrum.dat.count), 
                 legendPercents(spectrum.id.title, spectrum.id.count), 
                 legendPercents(spectrum.rev.title, spectrum.rev.count), 
                 legendPercents(spectrum.nonid.title, spectrum.nonid.count),
                 legendPercents(spectrum.unfrag.title, spectrum.unfrag.count),
                 legendPercents(spectrum.domfrag.title, spectrum.domfrag.count)), 
               
               fill=c(spectrum.dat.col, spectrum.id.col, spectrum.rev.col, spectrum.nonid.col, spectrum.unfrag.col, spectrum.domfrag.col), 
               title="Histograms")
      } else {
        legend("topright", c(paste(spectrum.dat.title, spectrum.dat.count)), fill=c(spectrum.dat.col), title="Histograms")
      }
      
      
      dev.off()
    } else {
      print("Skipping generation of msmsEval image file because msmsEval data file does not exist.")
    }
  }
  
  return(result)
}

helpLink<-function(topic) {
  ## TODO: Re-establish help
  ###	paste("&nbsp;<a class=\"help-link\" href=\"http://delphi.mayo.edu/wiki/trac.cgi/wiki/", topic, "\">?</a>", sep="")
}

colorSpan <- function(text, color) {
  str <- paste("<span style=\"color: ", color, ";\">", text, "</span>", sep="")
}

nbspize <- function(text) {
  gsub(" ", "&nbsp;", text)    
}

startReportFile<-function(reportFile) {
  cat(
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">
    <html>
    <head>
    <title>Lockmass and Mass Accuracy QA</title>
    <style type=\"text/css\">
    p { margin: 0.1em }
    img { image-rendering: optimizeQuality; -ms-interpolation-mode:bicubic; }
    body, table { font-family: sans-serif; font-size: 13px; }
    ul li { margin: 5px 0 }            
    a.help-link { background-color: #ccf; padding: 3px; color: #fff; text-decoration: none; }
    a:hover.help-link { background-color: #aaf; }
    table th, table td { padding: 3px; }
    .spectrum { background-color: #fafaff; text-align: center; } 
    .peptide, .protein { background-color: #fffffa; text-align: center; }
    </style>
    </head>
    <body>
    <h2>Swift QA", helpLink("swiftQa"), "</h2>
    <a href=\"summary.xls\">summary.xls</a>
    <table>
    <tr><th>Data files", helpLink("swiftQaDataFile"),
    "</th><th colspan=\"2\" class=\"protein\">Proteins",
    "</th><th colspan=\"2\" class=\"peptide\">Peptides",
    "</th><th colspan=\"1\" class=\"peptide\">FP Rate", helpLink("swiftQaPeptidesAndProteins"),    
    "</th><th class=\"spectrum\">Spectrum Counts", helpLink("swiftQaSpectrumCount"),
    "</th><th>", lockmass.title, helpLink("swiftQaLockmassGraph"),
    "</th><th>", calibration.title, helpLink("swiftQaMassCalibrationGraph"),
    "</th><th>", msmsEval.title, helpLink("swiftQaMsmsEvalDiscriminant"),
    "</th><th>", pepTol.title, helpLink("swiftQaPeptideTolerance"),
    "</th><th>", tic.title, helpLink("swiftQaTic"),    
    "</th><th>", mz.title, helpLink("swiftQaScanVsMZ"),
    "</th><th>", current.title, helpLink("swiftQaSourceCurrent"),
    "</th><th>", uv.title, helpLink("swiftUvData"),    
    "</th>"
    
    , file=reportFile, sep="")
    }

addRowToReportFile<-function(reportFile, row) {
  filePath <- basename(file.path(row$data.file))
  
  chunk <- paste("<tr><td><a href=\"", filePath, "\">", filePath, "</a></td>")
  
  # Produce HTML tag displaying thumbnail of a given image (including the <td>s surrounding it)
  getHtmlTextImageFileInfo<-function(imageFile) {
    htmlText <- NULL
    
    if (file.exists(imageFile)) {            
      filePath <- basename(file.path(imageFile))
      thumbPath <- makeThumb(imageFile, plot.dimension.thumb[1], plot.dimension.thumb[2])
      htmlText <- paste(htmlText, "<td><a href=\"", filePath, "\"><img border=\"0\" src=\"", thumbPath, "\" style=\"width : ",plot.dimension.web[1],"px ; height: ", plot.dimension.web[2],"px\"/></a></td>", sep="")
    } else {
      htmlText <- paste(htmlText, "<td align=\"center\">No Image Generated</td>", sep="")
    }
    
    return(htmlText)
  }
  
  percents <- function(x, total, ifZero="", ifNull="") {
    if (is.null(x) || is.null(total)) {
      return (ifNull)
    }
    
    if(x>0 && total>0) {
      return(paste(round(100*x/total, digits=2), "%", sep=""))
    } else {
      return (ifZero)
    }
  }
  
  chunk <- paste(chunk, 
                 "<td class=\"protein\">",row$proteins.all.count,"</td>",                        
                 "<td class=\"protein\">",colorSpan(row$proteins.rev.count, spectrum.rev.colHtml),"</td>",
                 "<td class=\"peptide\">",row$peptides.all.count,"</td>",
                 "<td class=\"peptide\">",colorSpan(row$peptides.rev.count, spectrum.rev.colHtml),"</td>",
                 "<td class=\"peptide\">",colorSpan(percents(row$peptides.rev.count * 2, row$peptides.all.count, "0%"), spectrum.rev.colHtml),"</td>",
                 "<td class=\"spectrum\"><table>",
                 "<tr><th>", nbspize(spectrum.all.title), "</th><td>", row$spectrum.all.count, "</td><td>", percents(row$spectrum.all.count, row$spectrum.msn.count), "</td></tr>",
                 "<tr><th>", nbspize(spectrum.msn.title), "</th><td>", row$spectrum.msn.count, "</td><td>", percents(row$spectrum.msn.count, row$spectrum.msn.count), "</td></tr>",
                 "<tr><th>", nbspize(spectrum.dat.title), "</th><td>", row$spectrum.dat.count, "</td><td>", percents(row$spectrum.dat.count, row$spectrum.msn.count), "</td></tr>",
                 "<tr><th>", nbspize(spectrum.id.title ), "</th><td>", colorSpan(         row$spectrum.id.count,                           spectrum.id.colHtml ), "</td><td>", 
                 colorSpan(percents(row$spectrum.id.count,  row$spectrum.msn.count), spectrum.id.colHtml ), "</td></tr>",
                 "<tr><th>", nbspize(spectrum.rev.title), "</th><td>", colorSpan(         row$spectrum.rev.count,                          spectrum.rev.colHtml), "</td><td>", 
                 colorSpan(percents(row$spectrum.rev.count, row$spectrum.msn.count), spectrum.rev.colHtml), "</td></tr>",
                 "<tr><th>", nbspize(spectrum.polymer.title), "</th><td>", colorSpan(     row$spectrum.polymer.count,                      spectrum.polymer.colHtml), "</td><td>", 
                 colorSpan(percents(row$spectrum.polymer.count, row$spectrum.msn.count), spectrum.polymer.colHtml), "</td></tr>",                                                                         
                 "<tr><th>", nbspize(spectrum.unfrag.title), "</th><td>", colorSpan(     row$spectrum.unfrag.count,                      spectrum.unfrag.colHtml), "</td><td>", 
                 colorSpan(percents(row$spectrum.unfrag.count, row$spectrum.msn.count), spectrum.unfrag.colHtml), "</td></tr>",
                 "<tr><th>", nbspize(spectrum.domfrag.title), "</th><td>", colorSpan(     row$spectrum.domfrag.count,                      spectrum.domfrag.colHtml), "</td><td>", 
                 colorSpan(percents(row$spectrum.domfrag.count, row$spectrum.msn.count), spectrum.domfrag.colHtml), "</td></tr>",
                 
                 "</table></td>",
                 getHtmlTextImageFileInfo(row$lockmass.file),
                 getHtmlTextImageFileInfo(row$calibration.file),
                 getHtmlTextImageFileInfo(row$msmsEval.file),
                 getHtmlTextImageFileInfo(row$pepTol.file),
                 getHtmlTextImageFileInfo(row$tic.file),
                 getHtmlTextImageFileInfo(row$mz.file),
                 getHtmlTextImageFileInfo(row$source.current.file),
                 getHtmlTextImageFileInfo(row$uv.file))
  
  chunk <- paste(chunk, "</tr>")
  
  cat(chunk, file=reportFile)
}

endReportFile<-function(reportFile) {
  cat("</table>
      </body>
      </html>"
      , file=reportFile)
}

# Main function that does all the work
# inputFile - a file describing all input files and where to put the images
# reportFileName - path to index.html file that will be generated
# decoyRegex - how to detect reverse hits
run <- function(inputFile, reportFileName, decoyRegex) {
  inputDataTab<-read.delim(inputFile, header=TRUE, sep="\t", colClasses="character", fileEncoding="UTF-8")
  reportFile<-file(reportFileName, "w")
  
  excelSummaryFile<-file(file.path(dirname(reportFileName), "summary.xls"), "w")
  cat("raw file name\tproteins\treverse proteins\tpeptides\treverse peptides\tall spectra\tmsn spectra\t.dat spectra\tidentified spectra\treverse hit spectra\tpolymer spectra\tunfragmented spectra\tdominant fragment spectra\tloading pressure mean\tnc pressure mean\tcolumn temperature min\tcolumn temperature max\n" , file=excelSummaryFile)
  
  startReportFile(reportFile)
  
  print("Generating image files and report file.")
  
  for(i in 1:length(inputDataTab$Data.File)) {
    line <- inputDataTab[i,]
    row <- imageGenerator(
      line$Data.File,          
      line$msmsEval.Output,
      line$Raw.Info.File,
      line$Raw.Spectra.File,
      line$Chromatogram.File,
      list(
        lockmass.file = line$Id.File,
        calibration.file = line$Mz.File,
        mz.file = line$IdVsMz.File,
        source.current.file = line$Source.Current.File,
        msmsEval.file = line$msmsEval.Discriminant.File,
        pepTol.file = line$Peptide.Tolerance.File,
        tic.file = line$TIC.File,
        uv.file = line$UV.Data.File),
      line$Generate.Files,
      decoyRegex)
    
    addRowToReportFile(reportFile, row)
    cat(line$Raw.File, 
        row$proteins.all.count, 
        row$proteins.rev.count, 
        row$peptides.all.count,
        row$peptides.rev.count,
        row$spectrum.all.count,
        row$spectrum.msn.count,
        row$spectrum.dat.count,
        row$spectrum.id.count,
        row$spectrum.rev.count,
        row$spectrum.polymer.count,
        row$spectrum.unfrag.count,
        row$spectrum.domfrag.count,
        row$uv.loading.pressure.mean,
        row$uv.nc.pressure.mean,
        row$uv.column.temperature.min,
        row$uv.column.temperature.max,
        file=excelSummaryFile, sep="\t")  
    cat("\n", file=excelSummaryFile)
  }
  
  endReportFile(reportFile)
  
  print("Closing report file.")
  
  close(reportFile)
  
  cat("END OF FILE\n", file=excelSummaryFile)
  close(excelSummaryFile)
}

args<-commandArgs(TRUE)
#args<-c("/Users/m044910/Documents/devel/swift/swift/scripts/src/test/input.txt", "/Users/m044910/Documents/devel/swift/swift/scripts/src/test/output.html", "Rev_")
inputFile<-args[1]
reportFileName<-args[2]
decoyRegex<-args[3] # Currently treated just as a plain prefix

run(inputFile, reportFileName, decoyRegex)

# vi: set filetype=R expandtab tabstop=4 shiftwidth=4 autoindent smartindent:

