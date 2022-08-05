# The WebFX Stack

The WebFX Stack is a collection of framework pieces for WebFX designed to work together as follows:

<p align="center">
  <img src="https://docs.webfx.dev/webfx-stack/webfx-stack.svg" />
</p>

| Framework piece        | Location | Description                                                                                     |
|------------------------|----------|-------------------------------------------------------------------------------------------------|
| **Validation**         | UI       | Form & dialog validation framework                                                              |
| **Routing**            | UI       | UI router for Single Page Application navigation                                                |
| **I18n**               | UI       | Internationalisation with JavaFX binding                                                        |
| **Action**             | UI       | Simple Action API with text, graphic, disabled & visible bindings                               |
| **Authz**              | Non-UI   | Advanced authorization framework with JavaFX bindings                                           |
| **Authn**              | Non-UI   | Simple interface for Authentication                                                             |
| **ORM**                | Non-UI   | Client-side ORM with powerful reactive filters (using JavaFX binding)                           |
| **QueryPush**          | Non-UI   | Push notification for database queries                                                          |
| **Push**               | Non-UI   | General purpose push notification API                                                           |
| **DB**                 | Non-UI   | Async database access API (query & submit)                                                      |
| **WindowLocation**     | Platform | Cross-platform API for accessing the window location                                            |
| **WindowHistory**      | Platform | Cross-platform API for accessing the window history                                             |
| **WebAssembly**        | Platform | Cross-platform API for working with WebAssembly                                                 |
| **WebWorker**          | Platform | Cross-platform API for working with web workers                                                 |
| **Async**              | Shared   | Future & Promise API for async operations                                                       |
| **Com**                | Shared   | Async communication via a websocket event bus                                                   |
| **Json**               | Shared   | Cross-platform Json API                                                                         |
| **QueryPush (server)** | Server   | server-side peer of QueryPush                                                                   |
| **Push (server)**      | Server   | server-side peer of Push                                                                        |
| **DB (server)**        | Server   | server-side peer of DB                                                                          |
| **Vert.x** *           | Server   | server-side implementation of the WebFX Platform & Stack modules for [Vert.x](https://vertx.io) |

*\* An additional implementation for Spring Boot might be considered in the future*. 

## Why a new framework?

Because the WebFX Stack possesses these 3 special qualities:

* Mainly client-side
* Cross-platform (GWT compatible in particular)
* Designed to work with JavaFX

And those 3 combined qualities are not possessed by any other existing Java frameworks. And they are the qualities you would naturally expect when developing WebFX applications.

Why mainly client-side? The whole WebFX ecosystem, and the WebFX Stack in particular, like other modern mobile & web technologies, emphasis the client-side paradigm where most of the application code resides on the client rather than on the server, to provide a better user experience (faster navigation, faster rendering, offline mode, etc...).

For example, as opposed to other Java ORMs which are primarily designed to run on the server (some may run on the client - but definitely not in the browser - and require most of the time a direct database access), the ORM we provide is primarily designed to run on the client (including in the browser) and doesn't require a direct database access (DB queries are sent asynchronously through the event bus). It builds the domain objects, or entities, in the client memory, so they can be directly accessed when running the client application logic, or building the UI. It can even react to push notifications and automatically update these entities, and notify the UI of these changes.

Our authorization framework is also designed for a client-side usage, and integrates beautifully with JavaFX (all your UI actions will be automatically displayed/hidden or enabled/disabled through JavaFX bindings, depending on the authorizations granted to the authenticated user).

## Status

All the modules described above already exist, they have been developed during the prototyping phase of the [Modality project](https://github.com/modalityproject/modality). They are now in the process of being open-sourced (code review & cleaning, javadoc, documentation, etc...). Each time a module will be ready to be open-sourced, it will be published in this repository.

## License

The WebFX Stack is a free, open-source software licensed under the [Apache License 2.0](LICENSE)