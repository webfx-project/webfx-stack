package dev.webfx.stack.com.bus.call;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.function.Callable;
import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Handler;
import dev.webfx.stack.com.bus.*;
import dev.webfx.stack.com.bus.call.spi.BusCallEndpoint;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class BusCallService {

    private static final boolean LOGS = false;
    private static final String DEFAULT_BUS_CALL_SERVICE_ADDRESS = "busCallService";
    // Address of the "is call pending?" endpoint (see registerIsCallPendingEndpoint() below)
    public static final String IS_CALL_PENDING_ADDRESS = "service/buscall/isCallPending";

    public static <T> Future<T> call(String address, Object javaArgument) {
        return call(address, javaArgument, new DeliveryOptions());
    }

    public static <T> Future<T> call(String address, Object javaArgument, DeliveryOptions options) {
        return call(address, javaArgument, options, null); // bus = null means using the default platform bus
    }

    public static <T> Future<T> call(String address, Object javaArgument, DeliveryOptions options, Bus bus) {
        return call(DEFAULT_BUS_CALL_SERVICE_ADDRESS, address, javaArgument, options, bus);
    }

    public static <T> Future<T> call(String remoteBusCallServiceAddress, String serviceAddress, Object javaArgument, DeliveryOptions options, Bus bus) {
        try (ThreadLocalBusContext context = ThreadLocalBusContext.open(bus)) {
            // Creating a PendingBusCall that will be immediately returned to the caller
            PendingBusCall<T> pendingBusCall = new PendingBusCall<>();
            // Making the actual call by sending the (wrapped) java argument over the event bus and providing a java reply handler
            BusCallService.sendJavaObjectAndWaitJavaReply( // helper method that does the job to send the (wrapped) java argument
                remoteBusCallServiceAddress, // the address of the remote BusCallService counterpart where entry calls are listened to
                new BusCallArgument(serviceAddress, javaArgument), // the java argument is wrapped into a BusCallArgument (as expected by the counterpart BusCallService)
                options,
                pendingBusCall::onBusCallResult // it just forwards the target result to the caller using the future
            );
            // We return the future immediately while we are waiting for the call result
            return pendingBusCall.future(); // The caller can set a handler to it that will be called back later on call result reception
        }
    }

    public static Registration listenBusEntryCalls() {
        return listenBusEntryCalls(DEFAULT_BUS_CALL_SERVICE_ADDRESS);
    }

    public static Registration listenBusEntryCalls(String busCallServiceAddress) {
        // Registering a java reply handler that expects BusCallArgument objects
        return BusCallService.<BusCallArgument, Object> // specifying BusCallArgument parameterized type for the expected as java class reply
            registerJavaHandlerForRemoteCalls( // helper method that does the job to register the java reply handler
            busCallServiceAddress, // the address that receives the BusCallArgument objects
            (busCallArgument, callerMessage) -> { // great, a BusCallArgument has been received
                // Registering the call as pending, keyed by the caller runId + call number (both communicated by the
                // client), so the client can ask - through the "is call pending?" endpoint - if that call is still
                // being processed (ex: a slow database query) before giving up on a reply exceeding its usual timeout.
                String callerRunId = ThreadLocalStateHolder.getRunId();
                int callNumber = busCallArgument.getCallNumber();
                PendingServerBusCalls.register(callerRunId, callNumber);
                // Forwarding the target argument to the target address (kind of local call) and waiting for the result
                sendJavaObjectAndWaitJsonReply(busCallArgument.getTargetAddress(), busCallArgument.getJsonEncodedTargetArgument(), DeliveryOptions.localOnlyDeliveryOptions(callerMessage.state()), ar -> {
                    // The call is not pending anymore, whatever the outcome (a reply is sent in both cases). Note that
                    // unregistering and replying happen in the same event loop iteration, so a client probing "is my
                    // call pending?" can never get "no" while the reply frame is written behind its probe reply.
                    PendingServerBusCalls.unregister(callerRunId, callNumber);
                    // Wrapping the result into a BusCallResult and sending it back to the initial BusCallService counterpart
                    Message<Object> result = ar.result();
                    if (LOGS)
                        log("Sending result back to client " + busCallArgument.getTargetAddress() + " with result = " + result);
                    // The failure branch goes through asFailedAsyncResult() rather than sending the cause
                    // itself: a raw throwable reaches a JS client under a codec it can't read and is taken
                    // for a successful result. This branch is how a target address that never answers —
                    // including one that hit the internal dispatch timeout — is reported, so it is exactly
                    // the case that must not read as success.
                    sendJavaReply(new BusCallResult<>(busCallArgument.getCallNumber(), ar.succeeded() ? result.body() : asFailedAsyncResult(ar.cause())), new DeliveryOptions(), callerMessage);
                });
            }
        );
    }

    /**
     * Method for the server to register the "is call pending?" endpoint. It allows a client to ask - passing the call
     * number of one of its own calls (the caller being identified by the runId of its state, a client can only ever
     * ask about its own calls, so no access control is required) - if that call is still being processed by the
     * server (ex: a slow database query still executing). The reply is the age of that pending call in millis, or -1
     * if that call is not (or no longer) pending. Clients can use this information to extend their reply timeout
     * instead of raising a final timeout error while the server is actually still working on the call.
     */
    public static Registration registerIsCallPendingEndpoint() {
        return registerBusCallEndpoint(IS_CALL_PENDING_ADDRESS, (Object callNumber) -> {
            Long ageMillis = PendingServerBusCalls.getPendingCallAgeMillis(ThreadLocalStateHolder.getRunId(), ((Number) callNumber).intValue());
            return ageMillis == null ? -1L : ageMillis;
        });
    }


    /*********************************************************************************
     * Private implementing methods of the "java layer" on top of the JSON event bus *
     ********************************************************************************/

    /**
     * Method to send a java object over the event bus. The java object is first serialized into JSON format assuming
     * there is a JSON codec registered for that java class. The reply handler will be called back on reply reception.
     *
     * @param <J> The java class expected by the java reply handler
     */
    private static <J> void sendJavaObjectAndWaitJavaReply(String address, Object javaObject, DeliveryOptions options, Handler<AsyncResult<J>> javaReplyHandler) {
        // Delegating the job to sendJavaObjectAndWaitJsonReply() with the following JSON reply handler:
        sendJavaObjectAndWaitJsonReply(address, javaObject, options, javaAsyncHandlerToJsonAsyncMessageHandler(javaReplyHandler));
    }

    /**
     * Method to send a java object over the event bus. The java object is first serialized into JSON format assuming
     * there is a JSON codec registered for that java class. The reply handler will be called back on reply reception.
     */
    private static <T> void sendJavaObjectAndWaitJsonReply(String address, Object javaObject, DeliveryOptions options, Handler<AsyncResult<Message<T>>> jsonReplyMessageHandler) {
        // Serializing the java object into JSON format (a JSON object most of the time but may also be a simple string or number)
        Object jsonObject = SerialCodecManager.encodeToJson(javaObject);
        if (LOGS)
            log("BusCallService sends json " + jsonObject);
        // Sending that JSON object over the JSON event bus
        BusService.bus().request(address, jsonObject, options, jsonReplyMessageHandler);
    }

    /**
     * Method to send a java reply over the event bus. Basically the same as the previous method but using the
     * Message.reply() method instead, and no reply is expected.
     */
    private static <T> void sendJavaReply(Object javaReply, DeliveryOptions options, Message<T> callerMessage) {
        // Serializing the java reply into JSON format (a JSON object most of the time but may also be a simple string or number)
        Object jsonReply = SerialCodecManager.encodeToJson(javaReply);
        if (LOGS)
            log("BusCallService sends reply " + jsonReply);
        // Sending that JSON reply to the caller over the JSON event bus
        callerMessage.reply(jsonReply, options);
    }

    /**
     * Method to extract a java object from a JSON message (its body is supposed to be in JSON format).
     * The message JSON body is deserialized into a java object (assuming there is a JSON deserializer registered for
     * that java class)
     *
     * @param <J> expected java class
     */
    private static <J> J jsonMessageToJavaObject(Message<?> message) {
        // Getting the message body in JSON format
        Object jsonBody = message.body();
        // Converting it into a java object through JSON deserialization
        return SerialCodecManager.decodeFromJson(jsonBody);
    }

    /**
     * Method to convert a java handler Handler<AsyncResult<J>> into a JSON message handler Handler<AsyncResult<Message<T>>>.
     * The resulting JSON message handler will just call the java handler after having deserialized the JSON message into
     * a java object or report any exception
     *
     * @param <J> expected java class as input for the java handler
     */
    private static <J, T> Handler<AsyncResult<Message<T>>> javaAsyncHandlerToJsonAsyncMessageHandler(Handler<AsyncResult<J>> javaHandler) {
        return ar -> {
            if (ar.failed())
                javaHandler.handle(Future.failedFuture(ar.cause()));
            else {
                Message<T> jsonMessage = ar.result();
                try {
                    // Getting the java object from the JSON message
                    J javaObject = jsonMessageToJavaObject(jsonMessage); // this implicit cast may throw a ClassCastException
                    // and calling the java handler with that java object
                    javaHandler.handle(Future.succeededFuture(javaObject));
                } catch (Throwable throwable) {
                    javaHandler.handle(Future.failedFuture(throwable));
                }
            }
        };
    }

    /**
     * Exactly the same but accepting a BiConsumer for the java handler and pass the JSON message to it (in addition to
     * the java object). In this way the java handler can send a reply to the caller.
     *
     * @param <J> expected java class as input for the java handler
     */
    private static <J, T> Handler<Message<T>> javaHandlerToJsonMessageHandler(BiConsumer<J, Message<T>> javaHandler, BiConsumer<Message<T>, Throwable> failureReplier) {
        return jsonMessage -> ThreadLocalStateHolder.runWithState(jsonMessage.state(), () -> {
            try {
                // Getting the java object from the JSON message
                J javaObject = jsonMessageToJavaObject(jsonMessage); // this implicit cast may throw a ClassCastException
                // and calling the java handler with that java object
                javaHandler.accept(javaObject, jsonMessage);
            } catch (Throwable throwable) {
                // Everything that goes wrong before a reply could be sent ends up here: the argument
                // naming a codec this peer doesn't have (decodeFromJson throws "No SerialCodec found
                // for id"), the result naming one on the way back out (encodeToJson throws "No
                // SerialCodec for type"), or the handler itself failing. This used to log and stop,
                // which left the caller with no reply at all — see replyFailure() for why that is
                // worse than any of the failures themselves.
                Console.error("[BusCallService] Call to '" + jsonMessage.address() + "' failed before a reply could be sent", throwable);
                replyFailure(jsonMessage, throwable, failureReplier);
            }
        });
    }

    /**
     * Answers a call that failed before its handler could reply, so that no caller is ever left
     * waiting on a call that is already over.
     * <p>
     * A silent call is the worst of the outcomes available here. The caller cannot tell it apart
     * from a slow one, so it waits out its full timeout before reporting something misleading, and
     * what it eventually reports describes the timeout rather than the fault. It is worse still for
     * the failures this catches, which are precisely the ones where the two peers disagree about
     * what they can encode — a client newer than the server, say — because the caller's own logs
     * then show a network-shaped problem and the reason sits only in the server's log, if anyone
     * thinks to look. Answering with the throwable puts the reason where the caller can act on it.
     * <p>
     * The envelope differs by who is waiting, which is why the replier is supplied per registration
     * (see registerJavaHandler): an entry call is answered to the remote client, which expects a
     * BusCallResult, while a locally-relayed endpoint call is answered to listenBusEntryCalls(),
     * which wraps whatever it receives. Getting this wrong is not a nuisance — a reply the caller
     * can't recognise is indistinguishable from no reply at all.
     * <p>
     * If replying itself fails, there is nowhere left to go: that is the genuinely dead case, and
     * the only one that still ends in a log line and silence.
     */
    private static <T> void replyFailure(Message<T> callerMessage, Throwable throwable, BiConsumer<Message<T>, Throwable> failureReplier) {
        try {
            failureReplier.accept(callerMessage, throwable);
        } catch (Throwable replyThrowable) {
            Console.error("[BusCallService] Could not even report the failure of the call to '" + callerMessage.address() + "'", replyThrowable);
        }
    }

    /**
     * Failure reply for an entry call, i.e. one received from a remote client: it is waiting for a
     * BusCallResult, and a failure travels inside one as the target result (the same shape
     * listenBusEntryCalls() sends when the target address fails to answer it). The call number is
     * read from the raw payload because the decoded argument may be exactly what we didn't get.
     */
    private static <T> void replyEntryCallFailure(Message<T> callerMessage, Throwable throwable) {
        int callNumber = BusCallArgument.readCallNumberFromRawPayload(callerMessage.body());
        // This call is over, so it must not keep answering "still pending" to the client's probe. The
        // usual unregistration is the reply callback in listenBusEntryCalls(), which never runs when the
        // failure lands before the forwarding does; a no-op when the call never got registered either.
        PendingServerBusCalls.unregister(ThreadLocalStateHolder.getRunId(), callNumber);
        sendJavaReply(new BusCallResult<>(callNumber, asFailedAsyncResult(throwable)), new DeliveryOptions(), callerMessage);
    }

    /**
     * Failure reply for a locally-relayed endpoint call: the same SerializableAsyncResult a failing
     * endpoint replies with, so both peers read it through paths they already have — PendingBusCall
     * unwraps an AsyncResult reply to its cause, and the JS client throws on the error key.
     */
    private static <T> void replyLocalCallFailure(Message<T> callerMessage, Throwable throwable) {
        sendJavaReply(asFailedAsyncResult(throwable), new DeliveryOptions(), callerMessage);
    }

    /**
     * The way a throwable travels back to a caller: as a failed SerializableAsyncResult, never as
     * itself.
     * <p>
     * A raw throwable is the intuitive payload and the wrong one, in two ways. It has to be
     * encodable, and only Exception is — the codec registry is searched up the superclass chain from
     * ExceptionSerialCodec, so an Error (a StackOverflowError from a recursive codec, say) has no
     * encoder and the attempt to report the failure throws its own. And where it does encode, it
     * arrives under the "exception" codec, which the JS client has no decoder for: it passes the
     * object through untouched, unwrapAsyncResult() sees no error key, and the call it just failed
     * is delivered as a success carrying a stray object — the same way a failure with no message
     * used to (36ae35fe).
     * <p>
     * SerializableAsyncResult has neither problem. Its codec writes the message as a plain string,
     * so any throwable survives it, and both peers already treat that envelope as a failure.
     */
    private static Object asFailedAsyncResult(Throwable throwable) {
        return SerializableAsyncResult.getSerializableAsyncResult(Future.failedFuture(throwable));
    }

    /**
     * Method to register a java handler (a handler expecting a java object and not JSON object). So JSON objects sent
     * to this address will automatically be deserialized into the java class expected by the java handler (assuming all
     * necessary JSON codecs are registered to make this possible).
     *
     * @param <J> expected java class as input for the java handler
     */
    private static <J, T> Registration registerJavaHandler(boolean local, String address, BiConsumer<J, Message<T>> javaReplyHandler) {
        // `local` also decides who is waiting on the other end, and therefore which envelope an
        // unanswerable call must be failed with — see replyFailure().
        return registerJsonMessageHandler(local, address, javaHandlerToJsonMessageHandler(javaReplyHandler,
            local ? BusCallService::replyLocalCallFailure : BusCallService::replyEntryCallFailure));
    }

    private static <J, T> Registration registerJavaHandlerForLocalCalls(String address, BiConsumer<J, Message<T>> javaReplyHandler) {
        return registerJavaHandler(true, address, javaReplyHandler);
    }

    private static <J, T> Registration registerJavaHandlerForRemoteCalls(String address, BiConsumer<J, Message<T>> javaReplyHandler) {
        return registerJavaHandler(false, address, javaReplyHandler);
    }

    /**
     * Method to register a JSON message handler (just delegates this to the event bus).
     */
    private static <T> Registration registerJsonMessageHandler(boolean local, String address, Handler<Message<T>> jsonMessageHandler) {
        return BusService.bus().register(local, address, jsonMessageHandler);
    }

    /***********************************************************************************************
     * Public helper methods to register Java handlers and functions working with the "java layer" *
     **********************************************************************************************/

    /**
     * Method to register a Java asynchronous function (which returns a Future) as a Java service so it can be called
     * through the BusCallService.
     *
     * @param <A> java class of the input argument of the asynchronous function
     * @param <R> java class of the output result of the asynchronous function
     */
    public static <A, R> Registration registerBusCallEndpoint(String address, AsyncFunction<A, R> javaAsyncFunction) {
        return BusCallService.<A, R>registerJavaHandlerForLocalCalls(address, (javaArgument, callerMessage) ->
            ThreadLocalStateHolder.runWithState(callerMessage.state(), () -> {
                    // Calling the java function each time a java object is received
                    applyEndpoint(address, javaAsyncFunction, javaArgument).onComplete(javaAsyncResult -> { // the java result of the asynchronous function is now ready
                        // A failed endpoint is logged HERE, where the throwable is still intact. Only its
                        // message survives serialisation (see SerializableAsyncResult's codec) — the type,
                        // the stack trace and any cause chain are dropped, and the caller receives a bare
                        // string. So without this the server keeps no record at all of its own failures:
                        // an endpoint could reject every call it received and leave nothing behind to say
                        // so, on either side of the wire. Logged unconditionally rather than behind LOGS,
                        // which is a debug switch for tracing normal traffic; a failure is not normal
                        // traffic, and the one we needed to see had already happened by the time anyone
                        // thought to look.
                        if (javaAsyncResult != null && javaAsyncResult.failed())
                            Console.error("[BusCallService] Endpoint '" + address + "' failed", javaAsyncResult.cause());
                        try {
                            // Replying to the caller by sending this java async result to it
                            sendJavaReply(
                                // And making sure that it is serializable using SerializableAsyncResult (but assuming that javaAsyncResult.result() is serializable)
                                SerializableAsyncResult.getSerializableAsyncResult(javaAsyncResult),
                                new DeliveryOptions(),
                                callerMessage
                            );
                        } catch (Throwable throwable) {
                            // That assumption about the result being serializable is the one that breaks here:
                            // encodeToJson() throws "No SerialCodec for type" when the endpoint returns something
                            // the registry doesn't know. This callback usually runs after the enclosing handler has
                            // returned, so the catch in javaHandlerToJsonMessageHandler doesn't cover it and the
                            // throwable would otherwise leave through the event loop, taking the reply with it.
                            // Reply with the failure instead: the caller loses the result either way, but learns why.
                            Console.error("[BusCallService] Endpoint '" + address + "' could not serialize its reply", throwable);
                            replyFailure(callerMessage, throwable, BusCallService::replyLocalCallFailure);
                        }
                    });
                }
            ));
    }

    /**
     * Invokes an asynchronous endpoint, turning the two ways it can misbehave — throwing instead of
     * returning, and returning null instead of a Future — into an ordinary failed future.
     * <p>
     * Both are otherwise fatal to the reply, not just to the call. The reply is sent from
     * onComplete(), so nothing reaches it, and the only catch upstream is the generic one in
     * javaHandlerToJsonMessageHandler, which logs the throwable and returns without answering. The
     * caller is then left holding a call that returns neither a result nor an error: it waits out
     * its own timeout with nothing to report, and any UI waiting on it simply never moves.
     * <p>
     * That is much harder to place than a failure. An endpoint that fails says which endpoint and
     * why; an endpoint that goes silent looks like a slow network, a lost subscription, or a client
     * bug, and it looks that way to every caller at once. A query-push subscribe was throwing NPE
     * on entry for two days and presented as pages stuck on their spinners with an empty push-query
     * list on /monitor — "nothing is subscribed" rather than "every subscribe is dying". Making the
     * throw a failed future puts it back on the wire, and past the log line below, with its stack
     * trace still intact.
     * <p>
     * A null future is treated the same way. No endpoint should return one, but the alternative is
     * an NPE here, which lands in the very same silence this method exists to remove.
     */
    private static <A, R> Future<R> applyEndpoint(String address, AsyncFunction<A, R> javaAsyncFunction, A javaArgument) {
        try {
            Future<R> resultFuture = javaAsyncFunction.apply(javaArgument);
            return resultFuture != null ? resultFuture
                : Future.failedFuture(new IllegalStateException("Endpoint '" + address + "' returned no future"));
        } catch (Throwable throwable) {
            return Future.failedFuture(throwable);
        }
    }

    /**
     * The synchronous counterpart of {@link #applyEndpoint}: the reply to send when a synchronous
     * endpoint throws, in place of the result it didn't return.
     * <p>
     * Failures travel as a SerializableAsyncResult, the same envelope the asynchronous endpoint
     * above replies with, even though these endpoints reply with a bare value when they succeed.
     * Both receivers already read it: PendingBusCall unwraps an AsyncResult reply to its cause, and
     * the JS client throws on the error key. So the two shapes coexist on this address without
     * either side needing to change.
     */
    private static Object endpointFailureReply(String address, Throwable throwable) {
        Console.error("[BusCallService] Endpoint '" + address + "' threw", throwable);
        return SerializableAsyncResult.getSerializableAsyncResult(Future.failedFuture(throwable));
    }

    /**
     * Method to register a Java synchronous function as a java service, so it can be called through the BusCallService.
     *
     * @param <A> java class of the input argument of the synchronous function
     * @param <R> java class of the output result of the synchronous function
     */
    public static <A, R> Registration registerBusCallEndpoint(String address, Function<A, R> javaFunction) {
        return BusCallService.<A, R>registerJavaHandlerForLocalCalls(address,
            (javaArgument, callerMessage) -> {
                // A throw here would otherwise leave the caller with no reply at all — see applyEndpoint().
                // It costs more than a failed call on this overload: these are also the client-side push
                // endpoints (PushClientService.registerPushFunction), and a push the server never gets an
                // answer to marks the client unresponsive, which drops every query-push stream it holds.
                Object javaReply;
                try {
                    javaReply = javaFunction.apply(javaArgument);
                } catch (Throwable throwable) {
                    javaReply = endpointFailureReply(address, throwable);
                }
                sendJavaReply(javaReply, callerMessage.options(), callerMessage);
            }
        );
    }

    /**
     * Method to register a Java Callable (synchronous function with no input argument) as a Java service, so it can be
     * called through the BusCallService.
     *
     * @param <R> java class of the output result of the callable
     */
    public static <R> Registration registerBusCallEndpoint(String address, Callable<R> callable) {
        return BusCallService.<Object, R>registerJavaHandlerForLocalCalls(address,
            (ignoredJavaArgument, callerMessage) -> {
                Object javaReply; // same reasoning as the Function overload above
                try {
                    javaReply = callable.call();
                } catch (Throwable throwable) {
                    javaReply = endpointFailureReply(address, throwable);
                }
                sendJavaReply(javaReply, null, callerMessage);
            }
        );
    }

    public static Registration registerBusCallEndpoint(BusCallEndpoint<?, ?> endpoint) {
        return registerBusCallEndpoint(endpoint.getAddress(), endpoint.toAsyncFunction());
    }

    private static void log(String message) {
        Console.log("[BusCallService] " + message);
    }
}
