# Jabba the easy going java web app plumber
Jabba is a java library that gets its inspiration from Python Flask. It will expose all the elementary features needed for development of web apps and microservices.

# How to Build Things

* running a build via: gradle jar
* running a test via: gradle test
* running a continuous server via: gradle --watch-fs -t runServer, then work on code (every save will rebuild and restart so you just refresh browser)

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
* ~~Asynchronous request processing with CompletableFuture support~~
* ~~WebSocket support for real-time bidirectional communication~~

With above things complete we ~~will~~ have a library that can be used for new webapps.
Don't have a profile page yet (which goes into app templates) and the dbo layer is basic not like sql alchemy but mostly things are in place. 

At this point we could use jabba to spawn new apps.

# Security Best Practices

When deploying Jabba applications in production, follow these security guidelines:

## Secret Key Management

**Critical:** Always set a strong, unique secret key for encryption. Never use the default or commit secrets to version control.

* Set `SECRET_KEY` in your configuration file, or
* Set `JABBA_SECRET_KEY` environment variable, or  
* Set `jabba.secret.key` system property

The secret key should be:
- At least 32 characters long
- Randomly generated (use a secure random generator)
- Unique per application instance
- Stored securely (use a secrets manager in production)

Example:
```bash
export JABBA_SECRET_KEY=$(openssl rand -base64 32)
```

## Cookie Security

Session cookies are automatically set with `HttpOnly` flag to prevent XSS attacks. The `Secure` flag is automatically set when requests are made over HTTPS.

## Input Validation

Jabba includes basic input validation to prevent DoS attacks:
- String parameters are limited to 100,000 characters
- Array parameters are limited to 1,000 elements

For additional validation, implement custom validation in your endpoint methods.

## Authentication

* Use strong password policies
* Never log passwords or sensitive credentials
* Use HTTPS in production to protect credentials in transit
* Implement proper session timeout and expiration

## Configuration

* Validate all configuration values on startup
* Use environment variables for sensitive configuration
* Never commit `.env` files or configuration with secrets to version control
* Use `.gitignore` to exclude sensitive files

## Database Security

* Use parameterized queries (Jabba uses PreparedStatement by default)
* Never construct SQL queries by concatenating user input
* Use connection pooling with appropriate limits
* Restrict database user permissions to minimum required

## Error Handling

* In production, sanitize error messages to avoid information disclosure
* Log detailed errors server-side, but return generic messages to clients
* Use proper HTTP status codes

## Server Configuration

* Configure request size limits to prevent DoS attacks
* Set appropriate timeouts
* Use reverse proxy (nginx, Apache) in front of Jabba for additional security layers
* Keep dependencies up to date

# Code Structure

There are 4 major modules all located under com.reliancy. 

They are:

* rec - slot based object and array definition, akin to json, base of dbo and any data access object (DAO)
* dbo - database access layer on top of rec
* jabba - web application layer
* util - utility methods maximally independent

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

Use slot based value access instead of by string names. The reason is that change happens and then you potentially have multiple places to change if using string. When using slots you can rely on refactoring help. Slots are smart they know type and format and position of the field.

## dbo
Here we define a very generic DAO interface. At the center is a Terminal which represents a data store and allows us to perform CRUD with some extra facility for complex queries. For database purposes we implement DBO or database object as a special instance of Rec interface. Finally to make this a useful module we provide SQLTerminal and helper classes to deal with Read, Create/Update, Delete actions using SQL language. Of course read action deals with querying.

Please note one thing about SQL in particular. SQL and related RDBMS systems are nothing ground breaking or throughput busting. They are an old and messy protocol to access data. The most important point is do not treat SQL connections as an open file handle. Instead you connect, you CRUD, you disconnect (and if that sounds inefficient it is). If you forget this, as I did, and you build your entire app on the premise that you can reuse a connection by multiplexing commands down the pipe you will be in a world of hurt. The hurt does not manifest during development but once tens or thousands of sessions start working the same few connections.

In any case this module tries to be SQL agnostic while sharing nomenclature. If you stick with the interface your app will be too. In my decades old experience there is no way to allow just a little SQL in your code. So treat your database layer as if it was not a SQL database and maybe you will be able to later switch to something else. Otherwise it will SQL(squeel) till the judgement day.

## jabba

Finally the center of the library is the module that implements an HTTP servlet (jetty handler actually). Entire machinery is added to perform marshalling and unmarshalling of HTTP requests into and out of java methods. Along the way we also deal with sessions and security and errors and also server side templating. Ideally your app will be a set of REST endpoints that are used by ReactJS or similar front end GUIs but in case you like server-side templating it is available.

You application can be any POJO, the entry point is JettyApp from there a request processor is installed which parses your class and discovers all the endpoints. We chain request processors landing on a MethodEndPoint. Middleware is injected, you guessed it, as a request processor in the middle of the chain. Examples include session management, security policy.

### Asynchronous Support

Jabba provides first-class support for asynchronous request processing, allowing your application to handle long-running operations efficiently without blocking server threads. Async support is automatically detected and requires no special configuration.

**Automatic Detection:**
- Methods returning `CompletableFuture<T>` are automatically handled asynchronously
- Methods annotated with `@Async` are executed in a thread pool
- Regular synchronous methods continue to work as expected

**Benefits:**
- Improved throughput for I/O-bound operations
- Efficient handling of concurrent requests
- Non-blocking execution for database queries, external API calls, and file operations
- Seamless integration with Java's `CompletableFuture` API

**Example:**

```java
// Async endpoint - automatically detected by return type
@Routed(path="/users/{id}")
public CompletableFuture<User> getUser(int id) {
    return CompletableFuture.supplyAsync(() -> {
        // Long-running database query
        return database.findUserById(id);
    });
}

// Async endpoint using @Async annotation
@Routed(path="/report")
@Async
public Report generateReport(String month, int year) {
    // Heavy computation executed in thread pool
    return reportService.generate(month, year);
}

// Regular synchronous endpoint - no changes needed
@Routed(path="/ping")
public String ping() {
    return "pong";
}
```

The async implementation handles proper resource cleanup, session management, and error propagation automatically. Both synchronous and asynchronous endpoints can coexist in the same application without any special routing or configuration.

### WebSocket Support

Jabba provides built-in support for WebSocket connections, enabling real-time bidirectional communication between clients and servers. WebSocket endpoints are treated as first-class citizens alongside HTTP endpoints, with automatic lifecycle management and session integration.

**Key Features:**
- Declarative WebSocket endpoints using `@WebSocket` annotation
- Automatic protocol upgrade from HTTP to WebSocket
- Session management and authentication integration
- Callback-based message handling (text and binary)
- Built-in support for broadcasting to multiple clients
- Seamless integration with application security policies

**Architecture:**
- WebSocket endpoints work in conjunction with `@Routed` for path mapping
- Full access to `AppSession` context for authenticated connections
- Automatic session tracking with built-in registry for broadcasting
- Clean separation between Jabba abstractions and underlying Jakarta WebSocket implementation

**Example:**

```java
// Simple echo endpoint
@Routed(path="/ws/echo")
@WebSocket
public void echoEndpoint(WebSocketSession session) {
    session.onText(message -> {
        try {
            session.sendText("Echo: " + message);
        } catch (IOException e) {
            log().error("Failed to send message", e);
        }
    });
}

// Chat room with broadcasting
@Routed(path="/ws/chat")
@WebSocket
public void chatEndpoint(WebSocketSession session) {
    String route = session.getRoute();
    
    session.onText(message -> {
        // Broadcast to all clients on this route
        WebSocketSession.broadcast(route, "User says: " + message);
    });
    
    session.onClose((code, reason) -> {
        WebSocketSession.broadcast(route, "User disconnected");
    });
    
    // Welcome message
    try {
        session.sendText("Welcome to the chat room!");
    } catch (IOException e) {
        log().error("Failed to send welcome message", e);
    }
}

// Authenticated WebSocket with AppSession access
@Routed(path="/ws/notifications")
@WebSocket
@Secured
public void notificationEndpoint(WebSocketSession session) {
    Session appSession = session.getAppSession();
    String userId = appSession != null ? appSession.getId() : "anonymous";
    
    session.onText(message -> {
        log().info("Received from user {}: {}", userId, message);
    });
}
```

WebSocket connections can be tested using standard WebSocket clients. In JavaScript:

```javascript
const ws = new WebSocket('ws://localhost:8090/ws/echo');
ws.onopen = () => ws.send('Hello Server');
ws.onmessage = (event) => console.log('Received:', event.data);
```

The WebSocket implementation ensures proper cleanup of resources, handles reconnection scenarios gracefully, and maintains compatibility with standard Jakarta WebSocket clients while providing a cleaner, callback-based API.

### Where to Start

As I said any POJO will do. You can look into JettyApp for an example. You can derive from JettyApp and then code something like this:

``` java
public class App extends JettyApp{
    /** Jabba does not hijack your boot process. 
     * Alas you got to setup things yourself.
     */ 
    public static void main( String[] args ) throws Exception{
        // deal with paths in some sane manner, especially for templates
        String work_dir="./var";
        if(new File(work_dir).exists()==false){
            work_dir="../var";
        }
        Template.search_path(work_dir,App.class);
        JettyApp app=new JettyApp();
        app.addAppSession();
        SecurityPolicy secpol=new SecurityPolicy().setStore(new PlainSecurityStore());
        app.setSecurityPolicy(secpol);
        // this is where method parsing happens and app could be any POJO
        RoutedEndPoint rep=new RoutedEndPoint().importMethods(app);
        app.setRouter(rep);
        // it helps to support static file serving too
        FileServer fs=new FileServer("/static",work_dir+"/public");
        fs.exportRoutes(app.getRouter());
        // setup menu if you are going to use templates
        Menu top_menu=Menu.request(Menu.TOP);
        top_menu.add(new MenuItem("home")).addSpacer().add(new MenuItem("login"));
        top_menu.setTitle("Jabba");
        app.run(new FileConfig());
        //System.out.println("Goodbye World!");
    }
    // case 1: simplest endpoint (path from method name)
    @Routed()
    public String hello(){
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Jared");
        String ret="";
        try {
                Template t=Template.find("/templates/login.hbs");
                System.out.println("Template:"+t);
                ret = t.render(context).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
        //#return "Hello World";
    }
    // case2: we take over input/output
    @Routed(
        path="/helloPlain"
    )
    public void hello2(com.reliancy.jabba.Request req,Response resp) throws IOException{
        resp.getEncoder().writeln("Hi There");
    }
    // case3: we use path templates
    @Routed(
        path="/hello3/{idd:int}"
    )
    public String hello3(int id){
        return "Hello3:"+id;
    }
    // web landing page to link to other cases
    @Routed(
        path="/"
    )
    public String home(){
        StringBuilder buf=new StringBuilder();
        buf.append("<p>Sample pages:</p>");
        buf.append("<dd><a href='/helloPlain'>plain</a></dd>");
        buf.append("<dd><a href='/hello3/5'>parametric</a></dd>");
        buf.append("<dd><a href='/hello'>templated</a></dd>");
        buf.append("<dd><a href='/secured'>secured http</a></dd>");
        buf.append("<dd><a href='/secured_form'>secured form</a></dd>");
        return buf.toString();
    }
    // case 4: we require authentication for this route
    @Routed
    @Secured
    public String secured(){
        return "We are secured";
    }
    // case 5: we require auth but we send to an html login form
    @Routed
    @Secured(
        login_form = "/login"
    )
    public String secured_form(){
        return "We are secured by form";
    }
    // case 6: handle post requests, use templating
    @Routed
    public void login(com.reliancy.jabba.Request req,Response resp){
        //return "login form here";
        if(req.getVerb().equals("POST")){
            // here we need to process login and redirect
            try{
            String userid=(String)req.getParam("userid",null);
            String pwd=(String)req.getParam("password",null);
            AppSession ass=AppSession.getInstance();
            SecurityPolicy secpol=ass.getApp().getSecurityPolicy();
            SecurityActor user=secpol.authenticate(userid, pwd);
            if(user==null) throw new NotAuthentic("invalid credentials");
            resp.setStatus(Response.HTTP_FOUND_REDIRECT);
            //String old_url=request.getPath();
            //old_url=URLEncoder.encode(old_url,StandardCharsets.UTF_8.toString());
            resp.setHeader("Location","/home");
            }catch(Exception ex){
                log().error("error:",ex);
                Feedback.get().push(FeedbackLine.error(ex.getLocalizedMessage()));        
            }
        }
        //Map<String, Object> context = new HashMap<>();
        //context.put("app_title", "Jabba Login");
        //context.put("name", "Jared");
        //ArrayList<FeedbackLine> events=new ArrayList<>();

        //Feedback.get().push(FeedbackLine.error("Error"));
        //Feedback.get().push(FeedbackLine.info("Error"));
        //Feedback.get().push(FeedbackLine.warn("Error"));
        //context.put("feedback",events);
        try {
            resp.setContentType("text/html");
            Rendering.begin("/templates/login.hbs")
                //.with("feedback",events)
                .end(resp.getEncoder().getWriter());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
}
```