# spatialconnect-android-sdk
SpatialConnect library for Android


# Overview

SpatialConnect is a collection of libraries that makes it easier for developers to write
apps that connect to multiple spatial data stores online and offline. It leverages [Reactive Extentsions](http://reactivex.io/) for communicating with the data stores using a common API across [iOS](https://github.com/boundlessgeo/spatialconnect-ios-sdk), [Android](https://github.com/boundlessgeo/spatialconnect-android-sdk), and [Javascript](https://github.com/boundlessgeo/spatialconnect-js) runtimes.
 
This library provides native APIs for Android as well as a Javascript bridge to communicate to the native API from mobile browsers.   The SpatialConnect Android SDK is packaged in the [aar format](http://tools.android.com/tech-docs/new-build-system/aar-format) and can be imported as a dependency to your Android app.


## Core Concepts

SpatialConnect consists of a collection of services which each handle specific functionality.  For instance, the `SCDataService` handles reads and writes to data stores while the `SCSensorService` handles subscribing to and reciving GPS updates.  All services are managed by the `SCServiceManager` which is responsible for loading a configuration file that will initialize the services and data stores.  All services and data stores in SpatialConnect provide an API that returns an [Observable](http://reactivex.io/documentation/observable.html) that emits data or events for subscribers to consume.

### Services and the ServiceManager

SpatialConnect services are the building blocks that developers can use for interacting with the mobile device and data stores.  Currently there are 3 services, the `SCDataService`, the `SCNetworkService`, and the `SCSensorService`.  The `SCServiceManager` is responsible for managing the service lifecycle (registering them, starting/stopping them, etc).  When you create an instance of the `SCServiceManager`, it will enable all the services and read the configuration file to determine what to do next.  If data stores are defined, it will attempt to register each store with the `SCDataService`.

### Data Stores and the DataService
The `SCDataService` is responsible for interacting with the data stores.     


Developers need to create a new data store by creating a class that implements the `SCSpatialStore` interface and updating a configuration file to let SpatialConnect know
that the store exists.  Each data store should also extend `SCDataStore` and provide
an `SCDataAdapter` that the store uses as the implementation of
CRUD functionality on `SCSpatialFeature`s.  Without an adapter
implementation, SpatialConnect will not consider the store to be a
supported and will not attempt to connect.


### Querying

To query the stores for `SCSpatialFeature`s, you need to create a `SCQueryFilter` with `SCPredicate`s and pass it to the query function of your `SCDataAdapter`.


### Geometry Object Model

The SpatialConnect provides a custom geometry object model.  One reason this
is necessary is b/c we need to identify objects by their ids (in case
they need to be edited) and the GeoJSON spec doesn’t require ids.  The
object model is a blend of the OGC Simple Feature Specification and the
GeoJSON spec but it doesn’t strictly follow either because it’s trying to be
a useful, developer-friendly abstraction.

The primary objects of the SpatialConnect geometry object model are the
`SCSpatialFeature` (which contains an id, some audit metadata, and k/v map
properties) and the `SCGeometry`, which extends `SCSpatialFeature` with a
[JTS Geometry object](http://docs.geotools.org/stable/userguide/library/jts/geometry.html) and some other attributes.  A useful side effect of
this is that the `SCGeometry` object can always be represented as a
[GeoJSON Geometry object](http://geojson.org/geojson-spec.html#geometry-objects).

`SCSpatialFeature`s is the parent class of all geometries. This allows the library to handle spatial data types that do not contain a geometry. Supplemental data that contains no location can be stored, queried, and filtered with the functionality of the library. All data is treated as a feature is queried with a filter.





## How to create a new data store
To create a new data store you need to create a class that extends SCDataStore and implements SCSpatialStore.  Then you must update the config file `/src/main/res/raw/scconfig.json` with the store's name, type, and an optional version for the store type (eg. when WMS is the store type, 1.1.0 is an example version).

Here's an example config file:

```
{
  "stores": [
    {
      "name": "My features",
      "type": "geojson",
      "uri": "/mnt/sdcard/myfeatures.json"
    },
    {
      "name": "haiti-vectors-split.gpkg",
      "type": "geopackage",
      "uri": "http:/www.geopackage.org/data/haiti-vectors-split.gpkg"
    },
    {
      "name": "Your features",
      "type": "WFS",
      "version": "1.1.0",
      "uri": "http://your.domain.com/geoserver/wfs"
    }
  ]
}
```

The data store needs an adapter to connect to the underlying data source (a GeoJson file, a GeoPackage database, a CSV file, etc), therefore you must also create a class that extends SCDataAdapter.  The adapter will manage connecting and disconecting to the data source as well as the actual I/O to the data source.  The adapter will use the uri defined in the config to connect to the data source.  If the uri is remote, then it will download from the location and store it locally (at least for a geopackage).  

### Testing

To run the tests and generate a coverage report run `./gradlew connectedCheck`.  The
testing and coverage reports can be found in `$projectRoot/spatialconnect/build/reports/`

### Version Support
Android 4.1+ Jelly Bean API 16
