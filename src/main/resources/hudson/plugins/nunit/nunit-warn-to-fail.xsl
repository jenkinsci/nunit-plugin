<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes" cdata-section-elements="system-out system-err message stack-trace" />

	<!-- NUnit3 results format -->
	<xsl:template match="node()|@*">
 		 <xsl:copy>
		  <!-- copy original, but change property: https://stackoverflow.com/questions/615875/xslt-how-to-change-an-attribute-value-during-xslcopy -->
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
	</xsl:template>

	<!-- update the total count for test-suite to include the sum of all pass + fail + warnings -->
	<xsl:template match="test-suite/@total|test-run/@total">
        <xsl:attribute name="total">
			<xsl:value-of select="count(//assertion[@result = 'Failed'])+count(//assertion[@result = 'Warning'])+count(//assertion[@result = 'Pass'])"/>
		</xsl:attribute>
    </xsl:template>

	<!-- update the failed count for test-suite to include the sum of all fail + warnings -->
	<xsl:template match="test-suite/@failed|test-run/@failed">
        <xsl:attribute name="failed">
			<xsl:value-of select="count(//assertion[@result = 'Failed'])+count(//assertion[@result = 'Warning'])"/>
		</xsl:attribute>
    </xsl:template>

	<!-- update the warnings count for test-suite to 0 since we're converting all warnings to failures (with a comment) -->
	<xsl:template match="test-suite/@warnings|test-run/@warnings">
        <xsl:attribute name="warnings">
			<xsl:value-of select="0"/>
		</xsl:attribute>
    </xsl:template>

	<!-- probably don't really need this, but do it anyway -->
	<xsl:template match="test-suite/@passed|test-run/@passed">
        <xsl:attribute name="passed">
			<xsl:value-of select="count(//assertion[@result = 'Passed'])"/>
		</xsl:attribute>
    </xsl:template>

	<!-- convert any reason where the result is Warning to a failure. -->
	<xsl:template match="test-case/reason">
		 <xsl:choose>
    		<xsl:when test="../@result ='Warning'">
				<failure>
				 <message><xsl:value-of select="./message" />
				 
This test case was reported as a "Warning" in NUnit, but converted to "Fail" by Jenkins NUnuit Plugin
</message>
				</failure>
			</xsl:when>
			<xsl:otherwise>
				<skipped message="{./message}"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- copy any assertion nodes which were warnings, but apply formatting -->

	<xsl:template match="assertion[@result = 'Warning']">
		<xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
	</xsl:template>

	<!-- transform the result property of any assertion from warning to failure -->
	<xsl:template match="assertion[@result = 'Warning']/@result">
        <xsl:attribute name="result">Failed</xsl:attribute>
    </xsl:template>


	<!-- source: https://www.oxygenxml.com/archives/xsl-list/200102/msg00838.html -->
	<xsl:template name="lastIndexOf">
		<!-- declare that it takes two parameters - the string and the char -->
		<xsl:param name="string" />
		<xsl:param name="char" />

		<xsl:choose>
			<!-- if the string contains the character... -->
			<xsl:when test="contains($string, $char)">
				<xsl:call-template name="lastIndexOf">
					<xsl:with-param name="string"
									select="substring-after($string, $char)" />
					<xsl:with-param name="char" select="$char" />
				</xsl:call-template>
			</xsl:when>

			<xsl:otherwise>
				<xsl:value-of select="$string" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- source: https://stackoverflow.com/a/10528912 -->
	<xsl:template name="string-replace-all">
		<xsl:param name="text" />
		<xsl:param name="replace" />
		<xsl:param name="by" />
		<xsl:choose>
			<xsl:when test="contains($text, $replace)">
			<xsl:value-of select="substring-before($text,$replace)" />
			<xsl:value-of select="$by" />
			<xsl:call-template name="string-replace-all">
				<xsl:with-param name="text" select="substring-after($text,$replace)" />
				<xsl:with-param name="replace" select="$replace" />
				<xsl:with-param name="by" select="$by" />
			</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
			<xsl:value-of select="$text" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
