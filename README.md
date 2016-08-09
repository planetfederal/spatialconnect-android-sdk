#spatialconnect-schema

## Protocol Buffers

### Objective-C
```
protoc SCMessage.proto --objc_out="./"
```

### java
```
protoc SCMessage.proto --java_out="./"
```

## Actions
To build the actions run
```
node index.js
```
It will produce Commands.java and Commands.h. 
