# spatialconnect-android-sdk
SpatialConnect library for Android

# Current Version
0.6.0

# Overview

SpatialConnect is a collection of libraries that makes it easier for developers to write
apps that connect to multiple spatial data stores online and offline. It leverages [Reactive Extentsions](http://reactivex.io/) for communicating with the data stores using a common API across [iOS](https://github.com/boundlessgeo/spatialconnect-ios-sdk), [Android](https://github.com/boundlessgeo/spatialconnect-android-sdk), and [Javascript](https://github.com/boundlessgeo/spatialconnect-js) runtimes.

This library provides native APIs for Android as well as a Javascript bridge to communicate to the native API from mobile browsers.   The SpatialConnect Android SDK is packaged in the [aar format](http://tools.android.com/tech-docs/new-build-system/aar-format) and can be imported as a dependency to your Android app.


## Core Concepts
All services and data stores in SpatialConnect provide an API that returns an [Observable](http://reactivex.io/documentation/observable.html) that emits data or events for subscribers to consume.


### Services and SpatialConnect
SpatialConnect consists of a collection of services which each handle specific functionality.  For instance, the `SCDataService` handles reads and writes to data stores while the `SCSensorService` handles subscribing to and reciving GPS updates.  All services are managed by the `SpatialConnect` object, which is responsible for loading a configuration file that will initialize the services and data stores.

Currently there are 3 services, the `SCDataService`, the `SCNetworkService`, and the `SCSensorService`.  The `SpatialConnect` object is responsible for managing the service lifecycle (registering them, starting/stopping them, etc).  When you create an instance of `SpatialConnect`, it will enable all the services and read the configuration file to determine what to do next.  If data stores are defined, it will attempt to register each store with the `SCDataService`.

### Data Stores and the DataService
The `SCDataService` is responsible for interacting with the data stores.  All data stores must implement the `SCSpatialStore` interface which provides methods to interact with the data store.  Here's what it looks like:

```
	Observable query(SCQueryFilter scFilter);
	Observable queryById(SCKeyTuple keyTuple);
	Observable create(SCSpatialFeature scSpatialFeature);
	Observable update(SCSpatialFeature scSpatialFeature);
	Observable delete(SCKeyTuple keyTuple);
```

Implementations exist for GeoJSON and GeoPackage data stores but to
create a new data store developers need to create a class that implements this interface and update a configuration file to let SpatialConnect know
that the store exists.

> Don't worry about SCQueryFilter, SCKeyTuple, or SCSpatialFeature for now...keep reading and you'll learn about them soon!

#### How to create a new data store
To create a new data store you need to create a class that extends `SCDataStore`.  Then you must update the config file `/src/main/res/raw/scconfig.json` with the store's name, type, and an optional version for the store type (eg. when WMS is the store type, 1.1.0 is an example version).

Here's an example config file:

```
{
  "stores":[
    {
      "type": "geojson",
      "version": "1",
      "uri": "all.geojson",
      "isMainBundle":true,
      "id":"63602599-3ad3-439f-9c49-3c8a7579933b",
      "name":"Simple"
    },
    {
      "type":"gpkg",
      "version":"1",
      "name":"Haiti",
      "uri":"https://s3.amazonaws.com/test.spacon/haiti4mobile.gpkg",
      "id":"a5d93796-5026-46f7-a2ff-e5dec85heh6b"
    },
    {
      "type":"gpkg",
      "version":"1",
      "name":"Whitehorse",
      "uri":"https://s3.amazonaws.com/test.spacon/whitehorse.gpkg",
      "id":"ba293796-5026-46f7-a2ff-e5dec85heh6b"
    }
  ]
}
```

The data store needs an adapter to connect to the underlying data source (a GeoJson file, a GeoPackage database, a CSV file, etc), therefore you must also create a class that extends `SCDataAdapter`.  The adapter will manage connecting and disconecting to the data source as well as the actual I/O to the data source.  The adapter will use the uri defined in the config to connect to the data source.  If the uri is remote, then it will download from the location and store it locally (at least for a geopackage).



### Querying for features

There are a few different ways to query for features but the main idea is to create an `SCQueryFilter` with `SCPredicate`s and pass it to a query function.  All data stores will have query functions and the the `SCDataService` provides convenience methods for querying across all the data stores.

Let's see how this works with an example.  Let's say you want to query for all features that exist within a specific bounding box.  You would first need to build an `SCQueryFilter` with an`SCPredicate` that uses a `SCBoundingBox` like this:

```java
SCBoundingBox bbox = new SCBoundingBox(
    sw.longitude,
    sw.latitude,
    ne.longitude,
    ne.latitude
);

SCQueryFilter filter = new SCQueryFilter(
        new SCPredicate(
        	bbox,
        	SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN
        )
);
```

Now to query across all stores for features in that bounding box, we can use the data service like this:

```
SpatialConnect sc = new SpatialConnect(getActivity());
SCDataService ds = sc.getDataService();
// query all stores in the bounding box and add them to the map
ds.queryAllStores(filter)
            .subscribe(
                    new Subscriber<SCSpatialFeature>() {

                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                        }

                        @Override
                        public void onNext(SCSpatialFeature feature) {
                        	addMarkerToMap(feature);
                        }
                    }
            );
```
The `queryAllStores()` method returns an Observable stream of `SCSpatialFeature`s that are added to the map as the subscriber receives them.

In addition to using `SCPredicate`s to query, you can also use the `SCKeyTuple` that is part of every `SCSpatialFeature`.  The `SCKeyTuple` is a tuple that contains the store id, the layer id, and the feature id.  Let's say you need to get a specific feature to perform some editing on it.  You can get the specific feature by first getting the data store and then querying by the feature id:

```
SCKeyTuple keyTuple = new SCKeyTuple(storeId, layerId, featureId);
sc.getDataService()
        .getStoreById(storeId)
        .queryById(keyTuple)
        .subscribe(mySubscriber);
```


### SCSpatialFeature and Geometry Object Model

`SCSpatialFeature`s is the primary domain object of SpatialConnect.  It is a generic object that contains an id, some audit metadata, and a k/v map of properties.

Another common object in SpatialConnect is `SCGeometry`, which extends `SCSpatialFeature` with a
[JTS Geometry object](http://docs.geotools.org/stable/userguide/library/jts/geometry.html) and some other attributes.  A useful side effect of
this is that the `SCGeometry` object can always be represented as a
[GeoJSON Geometry object](http://geojson.org/geojson-spec.html#geometry-objects).


> `SCSpatialFeature` is the parent class of all `SCGeometry`s. This allows the library to handle data types that do not contain a geometry.  A useful aspect of this design is that data containing no location attribute can still be stored, queried, and filtered with the functionality of SpatialConnect.

The SpatialConnect provides a custom geometry object model using the `SCGeometry`.  One reason this
is necessary is b/c we need to identify objects by their ids (in case
they need to be edited) and the GeoJSON spec doesn’t require ids.  The
object model is a blend of the OGC Simple Feature Specification and the
GeoJSON spec but it doesn’t strictly follow either because it’s trying to be
a useful, developer-friendly abstraction.


As mentioned before, each `SCSpatialFeature` contains a `SCKeyTuple` containing the layer id, store id, and feature id.  When sending a `SCSpatialFeature` through the Javascript bridge, we Base64 encode each part of the tuple and use that for the GeoJSON Feature's id.  This will allow us to keep track of features even after they are edited by a Javascript mapping client like OpenLayers.

### Examples

See [https://github.com/boundlessgeo/spatialconnect-examples/](https://github.com/boundlessgeo/spatialconnect-examples/) for an example application using this SDK.

### Building

To build and install the apk in your local maven repo

```
./gradlew uploadArchives
```
### Testing

To run the tests and generate a coverage report run

```
./gradlew connectedCheck
```
The
testing and coverage reports can be found in `$projectRoot/spatialconnect/build/reports/`

### Version Support
Android 4.1+ Jelly Bean API 16
