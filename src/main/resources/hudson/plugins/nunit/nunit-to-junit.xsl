<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes" />


	<!-- NUnit2 results format -->
	<xsl:template match="/test-results">
	<testsuites>
		<xsl:for-each select="test-suite//results//test-case[1]">
	
			<xsl:for-each select="../..">
				<xsl:variable name="firstTestName"
					select="results//test-case[1]//@name" />
                     
                <xsl:variable name="assembly">
                    <xsl:choose>
                        <xsl:when test="substring($firstTestName, string-length($firstTestName)) = ')'">
                            <xsl:value-of select="substring-before($firstTestName, concat('.', @name))"></xsl:value-of>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="concat(substring-before($firstTestName, @name), @name)" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <!--
                <xsl:variable name="assembly"
                    select="concat(substring-before($firstTestName, @name), @name)" />
                -->

				<!--  <redirect:write file="{$outputpath}/TEST-{$assembly}.xml">-->

					<testsuite name="{$assembly}"
						tests="{count(*/test-case)}" time="{@time}"
						failures="{count(*/test-case/failure)}" errors="0"
						skipped="{count(*/test-case[@executed='False' or @result='Inconclusive'])}">
						<xsl:for-each select="*/test-case">
							<xsl:variable name="testcaseName">
								<xsl:choose>
									<xsl:when test="contains(./@name, concat($assembly,'.'))">
										<xsl:value-of select="substring-after(./@name, concat($assembly,'.'))"/><!-- We either instantiate a "15" -->
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="./@name"/><!-- ...or a "20" -->
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
						
							<testcase classname="{$assembly}"
								name="{$testcaseName}">
                                <xsl:if test="@time!=''">
                                   <xsl:attribute name="time"><xsl:value-of select="@time" /></xsl:attribute>
                                </xsl:if>

								<xsl:variable name="generalfailure"
									select="./failure" />

								<xsl:if test="./failure">
									<xsl:variable name="failstack"
				    						select="count(./failure/stack-trace/*) + count(./failure/stack-trace/text())" />
									<failure>
										<xsl:choose>
											<xsl:when test="$failstack &gt; 0 or not($generalfailure)">
MESSAGE:
<xsl:value-of select="./failure/message" />
+++++++++++++++++++
STACK TRACE:
<xsl:value-of select="./failure/stack-trace" />
											</xsl:when>
											<xsl:otherwise>
MESSAGE:
<xsl:value-of select="$generalfailure/message" />
+++++++++++++++++++
STACK TRACE:
<xsl:value-of select="$generalfailure/stack-trace" />
											</xsl:otherwise>
										</xsl:choose>
									</failure>
								</xsl:if>
                                <xsl:if test="@executed='False' or @result='Inconclusive'">
                                    <skipped>
                                    <xsl:attribute name="message"><xsl:value-of select="./reason/message"/></xsl:attribute>
                                    </skipped>
                                </xsl:if>
				 			</testcase>
						</xsl:for-each>
					</testsuite>
			</xsl:for-each>
		</xsl:for-each>
		</testsuites>
	</xsl:template>

	<!-- NUnit3 results format -->
	<xsl:template match="/test-run">
		<testsuites tests="{@testcasecount}" failures="{@failed}" disabled="{@skipped}" time="{@duration}">
			<xsl:apply-templates/>
		</testsuites>
	</xsl:template>

	<xsl:template match="test-suite">
		<xsl:if test="test-case">
			<testsuite tests="{@testcasecount}" time="{@duration}" errors="{@testcasecount - @passed - @skipped - @failed - @inconclusive}" failures="{@failed}" skipped="{@skipped + @inconclusive}" timestamp="{@start-time}">
				<xsl:attribute name="name">
					<xsl:for-each select="ancestor-or-self::test-suite/@name">
						<xsl:value-of select="concat(., '.')"/>
					</xsl:for-each>
				</xsl:attribute>
				<xsl:apply-templates select="test-case"/>
			</testsuite>
			<xsl:apply-templates select="test-suite"/>
		</xsl:if>
		<xsl:if test="not(test-case)">
			<xsl:apply-templates/>
		</xsl:if>
	</xsl:template>

	<xsl:template match="test-case">
		<testcase name="{@name}" assertions="{@asserts}" time="{@duration}" status="{@result}" classname="{@classname}">
			<xsl:if test="@runstate = 'Skipped' or @runstate = 'Ignored' or @runstate='Inconclusive'">
				<skipped/>
			</xsl:if>

			<xsl:apply-templates/>
		</testcase>
	</xsl:template>

	<xsl:template match="command-line"/>
	<xsl:template match="settings"/>

	<xsl:template match="output">
		<system-out>
			<xsl:value-of select="output"/>
		</system-out>
	</xsl:template>

	<xsl:template match="stack-trace">
	</xsl:template>

	<xsl:template match="test-case/failure">
		<failure message="{./message}">
			<xsl:value-of select="./stack-trace"/>
		</failure>
	</xsl:template>

	<xsl:template match="test-suite/failure"/>

	<xsl:template match="test-case/reason">
		<skipped message="{./message}"/>
	</xsl:template>

	<xsl:template match="test-suite/reason"/>

	<xsl:template match="properties"/>

</xsl:stylesheet>
