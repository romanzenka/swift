<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mzid="xalan://edu.mayo.mprc.myrimatch.MzIdentMl"
                extension-element-prefixes="mzid"
                xmlns:m="http://psidev.info/psi/pi/mzIdentML/1.1">

    <xsl:output encoding="ISO-8859-1" indent="yes"/>

    <xsl:param name="titles"/>

    <xsl:strip-space elements="*"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="@spectrumID[parent::m:SpectrumIdentificationResult]">
        <xsl:attribute name="spectrumID">
            <xsl:value-of select="mzid:retitle(string(current()), $titles)"/>
        </xsl:attribute>
    </xsl:template>

</xsl:stylesheet>