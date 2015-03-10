VesselTrack
==========================

Live monitoring of AIS vessel targets.

The data is persisted in a MySQL database.

## Prerequisites

* Java 8
* Maven (for building)
* MySQL


## Building ##

    mvn clean install

## Launch

The build produces a executable .war-file in the /target folder. The application can be launched with:

    java -jar target/vessel-track-0.1-SNAPSHOT.war

or by using maven:

    mvn spring-boot:run

A local deployment will setup VesselTrack at the following URL:

    http://localhost:8080/index.html

## Configuration

VesselTrack can be configured by passing runtime parameters to it.
Please refer to [src/main/resources/application.properties](src/main/resources/application.properties)

Example (master instance):

    java -jar target/vessel-track-0.1-SNAPSHOT.war \
     --server.port=9000 \
     --aisbus=aisbus.xml \
     --spring.datasource.url=jdbc:mysql://localhost:3306/track \
     --aisbusFilter="s.country in (DNK)"

Example (slave instance):

    java -jar target/vessel-track-0.1-SNAPSHOT.war \
     --server.port=9090 \
     --spring.datasource.url=jdbc:mysql://localhost:3306/track \
     --slave=true

## Docker

An easy way to run a mysql instance:

    docker run -d \
      -e MYSQL_ROOT_PASSWORD=mysql -e MYSQL_USER=track -e MYSQL_PASSWORD=track -e MYSQL_DATABASE=track \
      -v /home/docker/mysql/data:/var/lib/mysql \
      -p 3306:3306 --name trackdb mysql:5.7.4

Which would allow you to run vessel-track using:

    docker build -t test/vessel-track .
    docker run -d --name vessel-track -p 8080:8080 \
      --link trackdb:trackdb -e DB_URL="jdbc:mysql://trackdb:3306/track"  \
      test/vessel-track


## REST API ##

#### Vessel target information

	http://locahost:8080/vessels/{mmsi}

#### Vessel target list

	http://locahost:8080/vessels/list

The following GET arguments can be supplied for filtering

  * `top` - the top latitude
  * `left` - the left longitude
  * `bottom` - the bottom latitude
  * `right` - the right longitude
  * `maxHits` - The maximum number of vessels

#### Vessel target count

	http://locahost:8080/vessels/count

Same arguments as list

#### Historical track

	http://locahost:8080/vessels/track/{mmsi}

Past track for the given vessel. The following GET arguments can be supplied for trimming the output

  * `minDist` (meters) - Samples the past track. The minimum distance between
	positions will be `minDist`. This argument can greatly reduce the number of track points for vessels at berth or anchor.
  * `age` - How long back to get past track for (format: https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence)

