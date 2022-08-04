# The WebFX Stack

The WebFX Stack is a collection of pieces of framework designed to work together as follows:

<p align="center">
  <img src="https://docs.webfx.dev/webfx-stack/webfx-stack.svg" />
</p>


### UI

* **Action**: Simple Action API with text, graphic, disabled & visible bindings
* **Routing**: UI router for Single Page Application navigation
* **I18n**: Internationalisation with JavaFX binding
* **Validation**: Form & dialog validation framework

### Non-UI
* **ORM**: client-side ORM with powerful reactive filters (using JavaFX binding)
* **QueryPush**: Push notification implementation for database queries
* **Push**: Push notification API
* **DB**: Async database access API (query & submit)
* **Authn**: Simple interface for Authentication
* **Authz**: Advanced authorization framework with JavaFX bindings

### Platform
* **WindowLocation**: API for accessing the window location
* **WindowHistory**: API for accessing the window history
* **WebAssembly**: API for working with WebAssembly
* **WebWorker**: API for working with web workers

### Shared (common to client & server)
* **Async**: Future & Promise API for async operations
* **Com**: Async communication via a websocket event bus
* **Json**: Cross-platform Json API

### Server

* **QueryPush**: server-side peer of QueryPush
* **Push**: server-side peer of Push
* **DB**: server-side peer of DB
* **Vert.x**: Vert.x implementation of the WebFX Stack server-side modules

An additional implementation for Spring Boot might be considered in the future. 

## Why a new framework?

Because the WebFX Stack has these 3 special unique qualities:

* Mainly client-side
* Cross-platform (GWT compatible in particular)
* Designed to work with JavaFX

These 3 combined qualities are not shared by any other existing Java frameworks, and they are the qualities you would naturally expect when developing WebFX applications.

The whole WebFX ecosystem, and the WebFX Stack in particular, like other modern mobile & web technologies, emphasis the client-side paradigm where most of the application code resides on the client rather than on the server, to provide a better user experience (faster navigation, faster rendering, offline mode, etc...).

For example, as opposed to other Java ORMs which are primarily designed to run on the server (some may run on the client - but definitely not in the browser), and require most of the time a direct database access, the ORM we provide is primarily designed to run on the client (including in the browser) and doesn't require a direct database access (DB queries are sent asynchronously through the event bus). It builds the domain objects, or entities, in the client memory, so they can be directly accessed when running the client application logic, or building the UI. It can even react to push notifications and automatically update these entities.

Our authorization framework is also designed for a client-side usage, and integrates beautifully with JavaFX (all your UI actions will be automatically displayed/hidden or enabled/disabled through JavaFX bindings, depending on the authorizations granted to the authenticated user).