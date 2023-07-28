# The WebFX Stack

The WebFX Stack is a collection of framework pieces for WebFX, designed to work together as follows:

<div align="center">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://docs.webfx.dev/webfx-readmes/webfx-stack-dark.svg">
      <img src="https://docs.webfx.dev/webfx-readmes/webfx-stack-light.svg" />
    </picture>

<p></p>

| Framework piece        | Location | Description                                                                          |
|------------------------|----------|--------------------------------------------------------------------------------------|
| **Routing**            | UI       | UI router for Single Page Application navigation                                     |
| **I18n**               | UI       | Internationalisation with JavaFX binding                                             |
| **Action**             | UI       | Simple Action API with text, graphic, disabled & visible bindings                    |
| **Validation**         | UI       | Form & dialog validation framework                                                   |
| **Authn**              | Non-UI   | Simple interface for Authentication                                                  |
| **Authz**              | Non-UI   | Advanced authorization framework with JavaFX bindings                                |
| **ORM**                | Non-UI   | Client-side ORM with powerful reactive filters (using JavaFX binding)                |
| **QueryPush**          | Non-UI   | Push notification for database queries                                               |
| **Push**               | Non-UI   | General purpose push notification API                                                |
| **DB**                 | Non-UI   | Async database access API (query & submit)                                           |
| **Com**                | Shared   | Async communication via a websocket event bus                                        |
| **QueryPush (server)** | Server   | server-side peer of QueryPush                                                        |
| **Push (server)**      | Server   | server-side peer of Push                                                             |
| **DB (server)**        | Server   | server-side peer of DB                                                               |
| **Vert.x** *           | Server   | Implementation of the WebFX Stack server-side modules for [Vert.x](https://vertx.io) |

*\* An additional implementation for Spring Boot might be considered in the future*. 

</div>

## Why a new framework?

Because the WebFX Stack possesses these three special qualities:

* Mainly client-side
* Cross-platform (GWT compatible in particular)
* Designed to work with JavaFX

These combined qualities are not possessed by any other Java framework, and are the qualities you would naturally expect when developing WebFX applications.

Why mainly client-side? The whole WebFX ecosystem, and the WebFX Stack in particular, like other modern mobile & web technologies, emphasise the client-side paradigm where most of the application code resides on the client rather than on the server. This provides a better user experience (faster navigation, faster rendering, offline mode, etc...).

For example, as opposed to other Java ORMs which are primarily designed to run on the server (some may run on the client - but definitely not in the browser - and require most of the time a direct database access), the ORM we provide is primarily designed to run on the client (including in the browser) and doesn't require a direct database access (DB queries are sent asynchronously through the event bus). It builds the domain objects, or entities, in the client memory, so they can be directly accessed when running the client application logic, or building the UI. It can even react to push notifications and automatically update these entities, and notify the UI of these changes (the UI is automatically updated when using Visual components from [WebFX Extras](https://github.com/webfx-project/webfx-extras)).

Our authorization framework is also designed for a client-side usage, and integrates beautifully with JavaFX (all your UI actions will be automatically displayed/hidden or enabled/disabled through JavaFX bindings, depending on the authorizations granted to the authenticated user).

## License

The WebFX Stack is a free, open-source software licensed under the [Apache License 2.0](LICENSE)
