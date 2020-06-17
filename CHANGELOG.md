### Changelog

##### Version 0.22 (November 17, 2017)

-   Fix issue with parameterized test names
    ([JENKINS-5674](https://issues.jenkins-ci.org/browse/JENKINS-5674)
    thanks [timotei](https://github.com/timotei))
-   Show inconclusive tests as skipped (thanks
    [2improveIT](https://github.com/2improveIT))
-   Fix test names not showing up in the correct namespace
    ([JENKINS-13471](https://issues.jenkins-ci.org/browse/JENKINS-13471))
-   Added ability to set health scale factor
    ([JENKINS-23484](https://issues.jenkins-ci.org/browse/JENKINS-23484))

##### Version 0.21 (July 12, 2017)

-   Added explicit LICENSE file
    ([JENKINS-44811](https://issues.jenkins-ci.org/browse/JENKINS-44811))
-   Fixed issue with filenames becoming too long
    ([JENKINS-44527](https://issues.jenkins-ci.org/browse/JENKINS-44527))
-   Fixed issue with invalid characters in XML
    ([JENKINS-17521](https://issues.jenkins-ci.org/browse/JENKINS-17521))

##### Version 0.20 (May 3, 2017)

-   Fix issue when nunit xml file has a BOM
    ([JENKINS-33493](https://issues.jenkins-ci.org/browse/JENKINS-33493))

##### Version 0.19 (April 27, 2017)

-   Add support for NUnit3 output files
    ([JENKINS-27618](https://issues.jenkins-ci.org/browse/JENKINS-27618))
-   Added easier usage for pipeline
    ([JENKINS-27906](https://issues.jenkins-ci.org/browse/JENKINS-27906))
    -   You can now use 'nunit' as the step instead of step()
-   Fix issue with exception if there are no tests
    ([JENKINS-34452](https://issues.jenkins-ci.org/browse/JENKINS-34452))
-   Fixed issue with failIfNoResults
    ([JENKINS-42967](https://issues.jenkins-ci.org/browse/JENKINS-42967))

##### Version 0.18 (August 26,2016)

-   [JENKINS-27618](https://issues.jenkins-ci.org/browse/JENKINS-27618):
    Workflow support for NUnit plugin (via pull request
    [\#11](https://github.com/jenkinsci/nunit-plugin/pull/11))
-   Bumped up Jenkins parent to 1.580.1
-   Added junit-plugin-1.13 as dependency

##### Version 0.17 (June 06, 2015)

-   [Pull Request
    \#10](https://github.com/jenkinsci/nunit-plugin/pull/10)
     thanks to @mbektchiev

##### Version 0.16 (July 24, 2014)

-   [JENKINS-14864](https://issues.jenkins-ci.org/browse/JENKINS-14864)
    - NUnit Plugin 0.14 - won't merge multiple xmls (merged pull
    request [\#7](https://github.com/jenkinsci/nunit-plugin/pull/7),
    thanks
    to [@akoeplinger](https://github.com/akoeplinger))
-   [JENKINS-18642](https://issues.jenkins-ci.org/browse/JENKINS-18642)
    - Job build is marked as failed if NUnit test result contains only
    ignored tests (merged pull
    request [\#6](https://github.com/jenkinsci/nunit-plugin/pull/6),
    thanks to
    [@bartensud](https://github.com/bartensud))

##### Version 0.15 (August 26, 2013)

-   JENKINS-9965 - Nunit plugin does not display graph when
    fingerprinting is used on the xml report

##### Version 0.14 (May 06, 2011)

-   Fixed a problem when creating temporay JUnit files. This fixes a
    problem that the NUnit plugin could lose several parameterized NUnit
    tests
    ([JENKINS-9246](https://issues.jenkins-ci.org/browse/JENKINS-9246))

##### Version 0.13 (Mar 15, 2011)

-   Fixed so parameterized NUnit tests are now shown properly with their
    names.
    ([JENKINS-5674](https://issues.jenkins-ci.org/browse/JENKINS-5674))
-   Fixed so NUnit tests that have the same namespace and class names
    are reported separately. Before it would just use one of the
    duplicated NUnit tests.

##### Version 0.12 (Mar 11, 2011)

-   Fixed so ignored test cases are shown as skipped in the UI, before
    it was only showing the number of skipped issues.
    ([JENKINS-6353](https://issues.jenkins-ci.org/browse/JENKINS-6353))

##### Version 0.11 (Feb 14, 2011)

-   Update link in help

##### Version 0.10 (Mar 18, 2010)

-   Plugin no longer throws a File exception when test case names
    contain \<\>\|?\* characters.
    ([JENKINS-5673](https://issues.jenkins-ci.org/browse/JENKINS-5673))

##### Version 0.9 (Jan 31, 2010)

-   Update code for more recent Hudson

##### Version 0.8 (Apr 13, 2009)

-   Re-inserted the dropped exception strings. Note, that this version
    requires Hudson 1.298
    ([JENKINS-3427](https://issues.jenkins-ci.org/browse/JENKINS-3427))

##### Version 0.7

-   Removed dependency of maven plugin, this will fix the issue not
    being able to use the plugin with Hudson 1.296
    ([JENKINS-3427](https://issues.jenkins-ci.org/browse/JENKINS-3427))

##### Version 0.6

-   Plugin now merges JUnit and NUnit test results into one test report
    ([JENKINS-1091](https://issues.jenkins-ci.org/browse/JENKINS-1091))
-   NUnit file report pattern can not cope with spaces in paths
    ([JENKINS-1175](https://issues.jenkins-ci.org/browse/JENKINS-1175))

##### Version 0.5

-   Fixed so the plugin works on a remote agent
-   More error tolerant XSL transformation
    ([JENKINS-1077](https://issues.jenkins-ci.org/browse/JENKINS-1077))

##### Version 0.4

-   Removed exception throwing when no test reports could be found

##### Version 0.3

-   Fixed so the plugin calls the correct method in JUnit archiver.
    ([JENKINS-1016](https://issues.jenkins-ci.org/browse/JENKINS-1016))
-   Made sure that NUnit record archiver runs before other notification
    plugins
    ([JENKINS-947](https://issues.jenkins-ci.org/browse/JENKINS-947))
-   Fixed so the number of skipped tests in the NUnit report is copied
    to the JUnit reports.

##### Version 0.2.3

-   Fixed a problem when removing the temporary JUnit reports that
    occurred on windows.

##### Version 0.2.2

-   Removes the transformed JUnit reports after they have been recorded
    by the JUnitArchiver

##### Version 0.2.1

-   Splits the transformed JUnit file in code instead of using the
    \<redirect\> feature in Xalan. (Fixes
    [JENKINS-892](https://issues.jenkins-ci.org/browse/JENKINS-892))

##### Version 0.2

-   Splits the NUnit report file into several JUnit files to fix the
    problem that the file name is shown instead of the namespaces.

##### Version 0.1

-   First version