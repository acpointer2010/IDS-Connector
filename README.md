This project creates a custom Apaache Karaf configuration including a custom feature "camel-ids".

Apache Karaf is an OSGi runtime which has originally been the basis for the ServiceMix application service. Karaf has become independent from ServiceMix and is now a middleware which is commonly used in IoT platforms. Example projects using Karaf are the OpenHAB home automation server or the OpenDayLight SDN controller.

Karaf is merely a set of different "features". A feature is a deployment unit which provides a certain functionality. For example, the activemq message queue comes as an "activemq-broker" feature. This project defines a custom "assembly" of Karaf, i.e. instead of using the default features, we create our own configuration, which includes especially the ActiveMQ message queue and the Apache Camel routing engine.

In addition to standard Karaf features, we also create our own custom feature which is extends Camel with a new protocol endpoint "ids://". The actual code for this feature is in project "camel-ids" and the definition of the Karaf feature is in "ids-karaf-feature".



# How to build

## Dependencies
Denpending on your OS, you might need Node.js installed:
For Ubuntu, (after installing curl if needed) run:
```
curl -sL https://deb.nodesource.com/setup_7.x | sudo -E bash -
sudo apt-get install -y nodejs
```

## Build web frontend/check Node.js installation
go to ids-webconsole/src/main/resources/www
And run:

```
npm install
sudo npm install -g --save process-nextick-args
```

Now we need ng2admin:
```
git clone https://github.com/akveo/ng2-admin.git
cd ng2-admin
npm install
```

And sometimes we need to do:
```
npm rebuild node-sass
```

TODO: We should fix this to get an automated build



## Install Docker 
Please see here: https://docs.docker.com/engine/installation/


## Run Maven

```
mvn clean install
```

You will now have a custom installation of Karaf including our own features in `karaf-assembly/target/assembly`

# How to run



## Variant 1: Run locally without docker

```
karaf-assembly/target/assembly/bin/karaf clean debug
```

(`clean` clears the workspace at startup, `debug` allows remote debugging. None of these is required)


If everything goes fine, you will see a Karaf shell. Type `help` to get started with it.





## Variant 2: Run in docker


```
./runDocker.sh
```



## URLs

When running, the following URLs will be available:


`http://localhost:8181/ids`

IDS connector dashboard. This is the main UI of the connector and used for configuring the connector by the user.


`http://localhost:8181/activemqweb/`

Management console for ActiveMQ. Use it during development to set up message queues and send test events. May be turned off for production.


`http://localhost:8181/cxf/api/v1/apps/list`

REST API endpoint for listing installed containers (dockers or trustme)

`http://localhost:8181/cxf/api/v1/apps/pull?imageId=<string>`

Pull and image and creates a container (but does not start it yet).

`http://localhost:8181/cxf/api/v1/apps/start?containerID=<string>`

Starts a container.

`http://localhost:8181/cxf/api/v1/apps/stop?containerId=<string>`

Stops a container.


`http://localhost:8181/cxf/api/v1/apps/wipe?containerId=<string>`

Removes a container (the image remains).

`http://localhost:8181/cxf/api/v1/config/list`


## Getting around in the Karaf shell

The Karaf shell is an adminstration environment for the Karaf platform. Try the following commands to check whether everything works:

`feature:list`: Shows all available features and their status (uninstalled or installed). Those marked with a star have explicitly been asked to be installed, other installed features are necessary dependencies. Make sure the `ids`feature is installed.

`bundle:list -t 0`: Lists all OSGi bundles. `-t 0` sets the start level threshold to 0 so you see all bundles. Otherwise you would only see bundles installed in addition the system bundles.

`camel:route-list`: Lists all active Camel routes

`log:tail`: Continuously displays the log. Press `Ctrl+c` to exit.

`log:display`: Display the log

`Ctrl+d`: Exits the Karaf shell




# Configuring routes

Create a file `karaf-assembly/target/assembly/deploy/my_route.xml` and paste the following lines into it:

```
<?xml version="1.0" encoding="UTF-8"?>
<blueprint
    xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="
      http://www.osgi.org/xmlns/blueprint/v1.0.0
      http://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd">

    <camelContext xmlns="http://camel.apache.org/schema/blueprint">
      <route>
        <from uri="ids://echo"/>
        <log message="Copying ${file:name} to the output directory"/>
        <to uri="file:output"/>
      </route>
    </camelContext>

</blueprint>
```

If you observe the logs in the Karaf console with `log:tail` you will see how that route is picked up. Type `camel:route-list` to confirm the route has been picked up and activated. 

This route opens an endpoint speaking the IDS protocol (by default at port `9292`) and puts all data received at that endpoint into a file `karaf-assembly/target/assembly/output/<filename>`. 

# Setting up Eclipse

 <a href="http://repo1.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.5.0.Final/os-maven-plugin-1.5.0.Final.jar">Download <code>os-maven-plugin-1.5.0.Final.jar</code></a> and put it into the <code>&lt;ECLIPSE_HOME&gt;/plugins</code> directory