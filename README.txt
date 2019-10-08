Deployment to Maven-Central:

mvn release:clean release:prepare -Darguments="-DskipTests" --settings D:\Development\.m2\settings.xml
mvn release:perform --settings D:\Development\.m2\settings.xml -Darguments="-DskipTests"




TODOs:
- refactor core-common project to client, discovery-api, discovery-multicast and common

Features:
- support LB on Registry-Services
  - round-robin with configurable start number to pick a service from the service list

- configurations
  - dynamic properties
  - in registry and sync
  
- Microservice
  - heartbeat for microservice 200OK optional method 'heartbeat()'