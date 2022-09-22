# Jabba the easy going java web app plumber
Jabba is a java library that gets its inspiration from Python Flask. It will expose all the elementary features needed for deveopment of web apps and microservices.

# How to Run Things
* running a build via: gradle jar
* running a test via: gradle test
* running a continouse server via: gradle --watch-fs -t runServer, then work on code

## Things Left to Do
* ~~Complete support for demarshalling and marshalling of objects to java methods~~
* ~~Session middleware~~
* ~~Auth middleware supporting basic and digest, and security entities~~
* ~~Static file serving~~
* ~~Templating like jinja~~
* ~~Homepage with login templates~~
* ~~Error page~~
* ~~Menu handling~~
* ~~Database layer or serial/deserial system like SQL Alchemy~~

With above things complete we ~~will~~ have a library that can be used for new webapps.
Don't have a profile page (which goes into app templates) and the dbo layer is basic not like sql alchemy but mostly things are in place. At this point we could use jabba to spawn new apps.
Now I could prepare it for github and for maven central.