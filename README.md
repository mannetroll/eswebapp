eswebbapp
=========

## Docker OSX

    $ mvn clean install
    $ docker build -f Dockerfile.osx -t mannetroll/eswebbapp .
    $ docker run --rm -ti -p 8080:8080 -p 9090:9090 mannetroll/eswebbapp /bin/sh
    
## prometheus

    $ curl -is http://localhost:9090/actuator/prometheus
    
# agent

    java -javaagent:/path/to/elastic-apm-agent-1.0.jar \
    -Delastic.apm.service_name=eswebapp \
    -Delastic.apm.secret_token= \
    -Delastic.apm.server_url=http://localhost:8200 \
    -Delastic.apm.environment=alces-test \
    -Delastic.apm.application_packages=org.example \
    -jar eswebapp-1.0.0-SNAPSHOT.jar

# agent

    java -javaagent:./elastic-apm-agent-1.48.1.jar -jar ./target/eswebapp-1.0.0-SNAPSHOT.jar
