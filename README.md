eswebbapp
=========

## Docker OSX

    $ mvn clean install
    $ docker build -f Dockerfile.osx -t mannetroll/eswebbapp .
    $ docker run --rm -ti -p 8080:8080 -p 9090:9090 mannetroll/eswebbapp /bin/sh
    
## prometheus

    $ curl -is http://localhost:9090/actuator/prometheus