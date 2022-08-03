# The WebFX Stack

## Why a new framework?

Because the WebFX Stack has these special unique qualities:

* Mainly client-side
* Cross-platform (GWT compatible in particular)
* Designed to work with JavaFX

These 3 combined qualities are not shared by any other existing Java frameworks, and these are the qualities you would naturally expect when developing WebFX applications.

WebFX in general, and the WebFX Stack in particular, like other modern web technologies, emphasis the client-side paradigm where most of the application code resides on the client rather than the server, to provide a better user experience (faster navigation, faster rendering, offline mode, etc...).

For example, as opposed to other Java ORM frameworks which are primary designed to run on the server (some may run on the client - but definitely not in the browser - and yet require a direct database access), the ORM we provide is primary designed to run on the client (including in the browser). It builds and maintains the entities in the client memory, so they can be directly accessed when running the client application logic, or building the UI.

Our authorization framework is also designed for a client-side usage, and integrates beautifully with JavaFX (all your UI actions will be automatically displayed/hidden or enabled/disabled through JavaFX bindings).

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
* **Com**: Async communication via a websocket event bus - client & server implementation
* **DB**: Async database access API (query & submit) - works remotely (client) or directly (server)
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

* **Vert.x**: Vert.x implementation of the WebFX Stack server-side modules

An additional implementation for Spring Boot might be considered in the future. 