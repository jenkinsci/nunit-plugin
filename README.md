# NUnit Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/nunit-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/nunit-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/nunit-plugin.svg)](https://github.com/jenkinsci/nunit-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/nunit.svg)](https://plugins.jenkins.io/nunit)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/nunit.svg?color=blue)](https://plugins.jenkins.io/nunit)


![](docs/images/nunit.png)

## Introduction

This plugin allows you to publish [NUnit](http://www.nunit.org/) test results.

## Pipeline example

For more information refer to [NUnit Pipeline Steps](https://www.jenkins.io/doc/pipeline/steps/nunit/)

### For Scripted pipeline

```
node {

    ...

    stage("Publish NUnit Test Report") {
        nunit testResultsPattern: 'TestResult.xml'
    }

    ...

}
```

### For Declarative pipeline

```
pipeline {
    agent any

    ...

    stages {

        ...

        stage("Publish NUnit Test Report") {
            steps {
                nunit testResultsPattern: 'TestResult.xml'
            }
        }

        ...

    }
}
```

## Version History

See the [releases](https://github.com/jenkinsci/nunit-plugin/releases) and the [changelog](docs/CHANGELOG.md)
