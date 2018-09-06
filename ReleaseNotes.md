# Release Notes

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