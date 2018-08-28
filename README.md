[![Build Status](https://travis-ci.org/ExpediaDotCom/haystack-secrets-commons.svg?branch=master)](https://travis-ci.org/ExpediaDotCom/haystack-secrets-commons)
[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://github.com/ExpediaDotCom/haystack/blob/master/LICENSE)


# haystack-secrets-commons
Module with common code that is used by various haystack components that need to detect secrets in spans or blobs.

## Building

Since this repo contains haystack-idl as the submodule, so use the following to clone the repo

```git clone --recursive git@github.com:ExpediaDotCom/haystack-secrets-commons.git```

#### Prerequisite: 

* Make sure you have Java 1.8
* Make sure you have maven 3.3.9 or higher


#### Build

For a full build including unit tests, one can run -

```
mvn clean package
```

#### Updating haystack-idl

* Run:

```git submodule update --recursive --remote```

* Update maven version

* Raise a PR

#### Releasing haystack-secrets-commons

* Git tagging: 

```git tag -a v1.4 -m "my version 1.4"```

Or you can also tag using UI: https://github.com/ExpediaDotCom/haystack-secrets-commons/releases

* Update https://github.com/ExpediaDotCom/haystack-secrets-commons/blob/master/ReleaseNotes.md
