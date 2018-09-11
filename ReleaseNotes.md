# Release Notes

## 1.0.8 / 2018-09-11 Time calls to find()
Use underscore delimited names for finders so that they are more readable in Grafana

## 1.0.7 / 2018-09-10 Time calls to find()
New metrics (created in HaystackFinderEngine) for each call to a finder's find() method; this will give us an average
duration metric, and count metric, to help determine what finders might need to be sped up

## 1.0.6 / 2018-09-06 Add HaystackCompositePhoneNumberFinder
The is the old "fat" phone number finder which is available for use if the "slim" finders are too slow

## 1.0.5 / 2018-09-06 Stop looking for secrets once a secret is found
This is to improve performance

## 1.0.4 / 2018-09-06 Make HaystackFinderEngine non-final
So Mockito can mock it in packages that use it (e.g. haystack-pipes)

## 1.0.3 / 2018-09-05 User HaystackFinderEngine everywhere
Solves problems with using custom finders that are specified in default_finders.xml

## 1.0.2 / 2018-09-05 Make CldrRegion public
Needs to be public so that it can be used by Spring configuration files in other packages

## 1.0.1 / 2018-09-04 Separation of the phone number finder into individual finders to facilitate multi-threading
Similar work is still needed for the credit card finder if we decide to continue the multi-threading effort. 

## 1.0.0 / 2018-08-80 Initial release
Code was moved from the haystack-commons package