Seconds
===========

Seconds for Android Wear was created 2014-06-21 for a hackathon at the
[Google Developer Group Düsseldorf](https://plus.google.com/u/0/107373371386267684213).

A short presentation about this project is [here](http://www.4k2.de/seconds.pdf)

### Setup

In order to run the sample, you'll need access to the Preview Wear
SDK. As the Chat client utilize Google Cloud Messaging for Android for
notifications about new messages, you need your own GCM sender ID and its
Auth Code. For an introduction of GCM, the 
[Getting Started](http://developer.android.com/google/gcm/gs.html) guide is a good source.
Place your GCM send ID in Configuration.java.
For asynchronous message uploads it requires the library [android-async-http](https://github.com/loopj/android-async-http).

### Note

This is just a sample code and meant for educational purposes only and
should not be use in production.
