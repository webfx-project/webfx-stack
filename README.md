# The WebFX Stack

## Why a new framework?

The WebFX Stack has some special unique qualities, in particular:

* Mainly client-side
* Cross-platform (GWT compatible in particular)
* Designed to work with JavaFX

These 3 combined qualities are not shared by any other existing Java frameworks, and these are the qualities you would naturally expect when developing WebFX applications.

While most of Java frameworks are server-side, we are following the direction of modern mobile and web app development, embracing the client-side paradigm, with fast page navigation, fast page rendering, offline mode, etc... For example, the ORM we provide builds and maintains the entities on client-side, so they can be directly accessed by your client application logic. Our authorization framework is also designed for a client-side usage, and integrates beautifully with JavaFX (all your UI actions will be automatically displayed/hidden or enabled/disabled through JavaFX bindings).

## The main components

### UI

* **Routing**: UI router for Single Page Application navigation
* **Action**: Simple Action API with text, graphic, disabled & visible bindings
* **I18n**: Internationalisation with JavaFX bindings
* **Validation**: Form & dialog validation framework

### Non-UI
* **Async**: Future & Promise API for async operations
* **Authn**: Simple interface for Authentication
* **Authz**: Authorization framework with JavaFX bindings
* **Com**: Communication via websocket event bus - client & server implementation
* **DB**: Database access API (query & submit) - client & server implementation
* **ORM**: client-side ORM with powerful reactive filters (JavaFX binding)
* **Push**: Push notification API
* **QueryPush**: Push notification implementation for database queries

### Platform
* **Json**: Json API
* **WebAssembly**: API for working with WebAssembly
* **WebWorker**: API for working with web workers
* **WindowHistory**: API for accessing the window history
* **WindowLocation**: API for accessing the window location

### Server implementation

* **Vert.x**: Vert.x implementation for the WebFX Stack server modules

An additional implementation for Spring Boot might be considered in the future. 