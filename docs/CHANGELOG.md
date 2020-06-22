# Changelog

## 0.26

Release date:  10 Feb 2020

- [[SECURITY-1752]](https://www.jenkins.io/security/advisory/2020-02-12/#SECURITY-1752) Disabled external entity processing for NUnit XML parser.

## 0.25

Release date: 8 Jun 2019

- [[PR#20]](https://github.com/jenkinsci/nunit-plugin/pull/20) Fix parallel publishing of NUnit test results 

## 0.24

Release date: 22 Jan 2019

- Don't overwrite previously recorded tests results if another publishing was called but no files were found by the provided filter

## 0.23

Release date: 7 Mar 2018

- Fix xsl for output matching.
- Update with test from issue.

## 0.22 

Release date: 17 November 2017

- [[JENKINS-5674]](https://issues.jenkins-ci.org/browse/JENKINS-5674) Fix issue with parameterized test names.
- Show inconclusive tests as skipped.
- [[JENKINS-13471]](https://issues.jenkins-ci.org/browse/JENKINS-13471) Fix test names not showing up in the correct namespace. 
- [[JENKINS-23484]](https://issues.jenkins-ci.org/browse/JENKINS-23484) Added ability to set health scale factor. 

## 0.21 

Release date: 12 July 2017

- [[JENKINS-44811]](https://issues.jenkins-ci.org/browse/JENKINS-44811) Added explicit LICENSE file. 
- [[JENKINS-44527]](https://issues.jenkins-ci.org/browse/JENKINS-44527) Fixed issue with filenames becoming too long. 
- [[JENKINS-17521]](https://issues.jenkins-ci.org/browse/JENKINS-17521) Fixed issue with invalid characters in XML.

## 0.20 

Release date: 3 May 2017

- [[JENKINS-33493]](https://issues.jenkins-ci.org/browse/JENKINS-33493) Fix issue when nunit xml file has a BOM.

## 0.19
 
Release date: 27 April 2017

- [[JENKINS-27618]](https://issues.jenkins-ci.org/browse/JENKINS-27618) Add support for NUnit3 output files. 
- [[JENKINS-27906]](https://issues.jenkins-ci.org/browse/JENKINS-27906) Added easier usage for pipeline. 
- You can now use 'nunit' as the step instead of step().
- [[JENKINS-34452]](https://issues.jenkins-ci.org/browse/JENKINS-34452) Fix issue with exception if there are no tests. 
- [[JENKINS-42967]](https://issues.jenkins-ci.org/browse/JENKINS-42967) Fixed issue with failIfNoResults. 

## 0.18

Release date: 26 August 2016

- [[JENKINS-27618]](https://issues.jenkins-ci.org/browse/JENKINS-27618) Workflow support for NUnit plugin.
- Bumped up Jenkins parent to 1.580.1.
- Added junit-plugin-1.13 as dependency.

## 0.17 

Release date: 6 June 2015

- Do not wait for checkpoint from previous build.

## 0.16 

Release date: 24 July 2014

- [[JENKINS-14864]](https://issues.jenkins-ci.org/browse/JENKINS-14864) NUnit Plugin 0.14 - won't merge multiple xmls.
- [[JENKINS-18642]](https://issues.jenkins-ci.org/browse/JENKINS-18642) Job build is marked as failed if NUnit test result contains only ignored tests.