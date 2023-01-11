# Jabba the easy going java web app plumber
Jabba is a java library that gets its inspiration from Python Flask. It will expose all the elementary features needed for deveopment of web apps and microservices.

# How to Build Things

* running a build via: gradle jar
* running a test via: gradle test
* running a continouse server via: gradle --watch-fs -t runServer, then work on code (every save will rebuild and restart so you just refresh browser)

Jabba depends on following libraries:

* org.eclipse.jetty:jetty-server - http servlet container
* org.slf4j:slf4j-simple - logging facility
* com.github.jknack:handlebars - handlebars templating
* com.h2database:h2 - java native comprehensive sql database (so comprehensive it can emulate postgres)
* org.postgresql:postgresql - proven and logical sql server implementation (no hacks like the identity column)
* com.zaxxer:HikariCP - database connection pooling (trust me you need it)

Normally maven or gradle will auto-resolve and download these dependencies.
If you build the libraries yourself there is "fat jabba" task that with combine all dependencies in one jar/zip file. 
If you just want to use the library without the hassle of building anything please include the following into your build.

```
repositories { 
    maven{
        url "https://repo.reliancy.com/repository/maven-hub"
    }
}
```

This is a sonatype nexus repository manager we host here at reliancy.

# Things Left to Do
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
Don't have a profile page yet (which goes into app templates) and the dbo layer is basic not like sql alchemy but mostly things are in place. 

At this point we could use jabba to spawn new apps.

# Code Structure

There are 4 major modules all located under com.reliancy. 

They are:

* rec - slot based object and array definition, akin to json, base of dbo and any data access object (DAO)
* dbo - database access layer on top of rec
* jabba - web application layer
* util - ulity methods maximally independent

## util
This module is a treasure trove of useful classes and methods. Most standalone methods are implemented in Handy class. One very useful class is the Tokenizer which starts out as a static method and is then wrapped by an iterator.

You can then do something like:
``` java
for(String token:new Tokenizer(bodyOftext)){
    System.out.println("Word:"+token);
}
```

## rec

The core of rec module is the interface Rec/Arr and plain implementation of it called Obj which can be an array or a key-value object. The values are kept in Obj while a parent object of type Hdr describes structure (the header). JSON encoder and decoder are provided and others could be implemented. 

At the field level the class Slot describes a field or property. It is usually defined statically at class level. Accessing a value can be done in two ways:
``` java
rec.get(Slot s); // record centric
Product.first_name.get(rec);   // field centric
```
The slot mechanism does not just describe fields it also allows us to generate conditions over slots which is useful in query construction.

One example would be notation like:
``` java
db.query(Product.class).where(Product.first_name.equals("Bla"))
```

Use slot based value access instead of by string names. The reason is that change happens and then you potentially have multiple places to change. Slots are smart they know type and format and position of the field.

## dbo
Here we define a very generic DAO interface. At the center is a Terminal which represents a data store and allows us to perform CRUD with some extra facility for complex queries. For database purposes we implement DBO or database object as a special instance of Rec interface. Finally to make this a useful module we provide SQLTerminal and helper classes to deal with read, create/update, delete actions using SQL language. Of course read action deals with querying.

Plese note one thing about SQL in particular. SQL and related RDBMs are nothing ground breaking or throughput busting. They are an old and messy protocol to access data. The most important point is do not treat SQL connections as an open file handle. Instead you connect, you CRUD, you disconnect (and if that sounds inefficient it is). If you forget this, as I did, and you build your entire app on the premise that you can reuse a connection by multiplexing commands down the pipe you will be in a world of hurt. The hurt does not manifest during development but once tens or tousands of sessions start working the same few connections.

In any case this module tries to be SQL agnostic while sharing nomenclature. If you stick with the interface your app will be too. In my decades old experience there is no way to allow just a little SQL in your code. So treat your database layer as if it was not a SQL database and maybe you will be able to later switch to something else. Otherwise it will SQL till the judgement day.

## jabba

Finally the center of the library is the module that implements an HTTP servlet (jetty handler actually). Entire machinery is added to perorm marshalling and unmarshaling of HTTP requests into and out of java methods. Along the way we also deal with sessions and security and errors and also server side templating. Ideally your app will be a set of REST endpoints that are used by ReactJS or similar front end GUIs but in case you like server-side templating it is available.

You application can be any POJO, the entry point is JettyApp from there a request processor is installed which parses your class and discovers all the endpoints. We chain request processors landing on a MethodEndPoint. Middleware is injected, you guessed it, as a request processor in the middle of the chain. Examples include session management, security policy.

