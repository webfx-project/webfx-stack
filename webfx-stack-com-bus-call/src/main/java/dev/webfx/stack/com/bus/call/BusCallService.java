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
                remoteBusCallServiceAddress, // the address of the remote BusCallService counterpart where entry calls are listened
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
            (busCallArgument, callerMessage) -> // great, a BusCallArgument has been received
                // Forwarding the target argument to the target address (kind of local call) and waiting for the result
                sendJavaObjectAndWaitJsonReply(busCallArgument.getTargetAddress(), busCallArgument.getJsonEncodedTargetArgument(), DeliveryOptions.localOnlyDeliveryOptions(callerMessage.state()), ar -> {
                    // Wrapping the result into a BusCallResult and sending it back to the initial BusCallService counterpart
                    Message<Object> result = ar.result();
                    if (LOGS)
                        log("Sending result back to client " + busCallArgument.getTargetAddress() + " with result = " + result);
                    sendJavaReply(new BusCallResult(busCallArgument.getCallNumber(), ar.succeeded() ? result.body() : ar.cause()), new DeliveryOptions(), callerMessage);
                })
        );
    }


    /*********************************************************************************
     * Private implementing methods of the "java layer" on top of the json event bus *
     ********************************************************************************/

    /**
     * Method to send a java object over the event bus. The java object is first serialized into json format assuming
     * there is a json codec registered for that java class. The reply handler will be called back on reply reception.
     *
     * @param <J> The java class expected by the java reply handler
     */
    private static <J> void sendJavaObjectAndWaitJavaReply(String address, Object javaObject, DeliveryOptions options, Handler<AsyncResult<J>> javaReplyHandler) {
        // Delegating the job to sendJavaObjectAndWaitJsonReply() with the following json reply handler:
        sendJavaObjectAndWaitJsonReply(address, javaObject, options, javaAsyncHandlerToJsonAsyncMessageHandler(javaReplyHandler));
    }

    /**
     * Method to send a java object over the event bus. The java object is first serialized into json format assuming
     * there is a json codec registered for that java class. The reply handler will be called back on reply reception.
     */
    private static <T> void sendJavaObjectAndWaitJsonReply(String address, Object javaObject, DeliveryOptions options, Handler<AsyncResult<Message<T>>> jsonReplyMessageHandler) {
        // Serializing the java object into json format (a json object most of the time but may also be a simple string or number)
        Object jsonObject = SerialCodecManager.encodeToJson(javaObject);
        if (LOGS)
            log("BusCallService sends json " + jsonObject);
        // Sending that json object over the json event bus
        BusService.bus().request(address, jsonObject, options, jsonReplyMessageHandler);
    }

    /**
     * Method to send a java reply over the event bus. Basically the same as the previous method but using the
     * Message.reply() method instead and no reply is expected.
     */
    private static <T> void sendJavaReply(Object javaReply, DeliveryOptions options, Message<T> callerMessage) {
        // Serializing the java reply into json format (a json object most of the time but may also be a simple string or number)
        Object jsonReply = SerialCodecManager.encodeToJson(javaReply);
        if (LOGS)
            log("BusCallService sends reply " + jsonReply);
        // Sending that json reply to the caller over the json event bus
        callerMessage.reply(jsonReply, options);
    }

    /**
     * Method to extract a java object from a json message (its body is supposed to be in json format).
     * The message json body is deserialized into a java object (assuming there is a json deserializer registered for that java class)
     *
     * @param <J> expected java class
     */
    private static <J> J jsonMessageToJavaObject(Message message) {
        // Getting the message body in json format
        Object jsonBody = message.body();
        // Converting it into a java object through json deserialization
        return SerialCodecManager.decodeFromJson(jsonBody);
    }

    /**
     * Method to convert a java handler Handler<AsyncResult<J>> into a json message handler Handler<AsyncResult<Message<T>>>.
     * The resulted json message handler will just call the java handler after having deserialized the json message into
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
                    // Getting the java object from the json message
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
     * Exactly the same but accepting a BiConsumer for the java handler and pass the json message to it (in addition to
     * the java object). In this way the java handler can send a reply to the caller.
     *
     * @param <J> expected java class as input for the java handler
     */
    private static <J, T> Handler<Message<T>> javaHandlerToJsonMessageHandler(BiConsumer<J, Message<T>> javaHandler) {
        return jsonMessage -> ThreadLocalStateHolder.runWithState(jsonMessage.state(), () -> {
            try {
                // Getting the java object from the json message
                J javaObject = jsonMessageToJavaObject(jsonMessage); // this implicit cast may throw a ClassCastException
                // and calling the java handler with that java object
                javaHandler.accept(javaObject, jsonMessage);
            } catch (Throwable throwable) {
                Console.log(throwable); // what else to do?
            }
        });
    }

    /**
     * Method to register a java handler (a handler expecting java objects and not a json objects). So json objects sent
     * to this address will automatically be deserialized into the java class expected by the java handler (assuming all
     * necessary json codecs are registered to make this possible).
     *
     * @param <J> expected java class as input for the java handler
     */
    private static <J, T> Registration registerJavaHandler(boolean local, String address, BiConsumer<J, Message<T>> javaReplyHandler) {
        return registerJsonMessageHandler(local, address, javaHandlerToJsonMessageHandler(javaReplyHandler));
    }

    private static <J, T> Registration registerJavaHandlerForLocalCalls(String address, BiConsumer<J, Message<T>> javaReplyHandler) {
        return registerJavaHandler(true, address, javaReplyHandler);
    }

    private static <J, T> Registration registerJavaHandlerForRemoteCalls(String address, BiConsumer<J, Message<T>> javaReplyHandler) {
        return registerJavaHandler(false, address, javaReplyHandler);
    }

    /**
     * Method to register a json message handler (just delegates this to the event bus).
     */
    private static <T> Registration registerJsonMessageHandler(boolean local, String address, Handler<Message<T>> jsonMessageHandler) {
        return BusService.bus().register(local, address, jsonMessageHandler);
    }

    /***********************************************************************************************
     * Public helper methods to register java handlers and functions working with the "java layer" *
     **********************************************************************************************/

    /**
     * Method to register a java asynchronous function (which returns a Future) as a java service so it can be called
     * through the BusCallService.
     *
     * @param <A> java class of the input argument of the asynchronous function
     * @param <R> java class of the output result of the asynchronous function
     */
    public static <A, R> Registration registerBusCallEndpoint(String address, AsyncFunction<A, R> javaAsyncFunction) {
        return BusCallService.<A, R>registerJavaHandlerForLocalCalls(address, (javaArgument, callerMessage) ->
            ThreadLocalStateHolder.runWithState(callerMessage.state(), () -> {
                    // Calling the java function each time a java object is received
                    javaAsyncFunction.apply(javaArgument).onComplete(javaAsyncResult -> // the java result of the asynchronous function is now ready
                        // Replying to the caller by sending this java async result to it
                        sendJavaReply(
                            // And making sure that it is serializable using SerializableAsyncResult (but assuming that javaAsyncResult.result() is serializable)
                            SerializableAsyncResult.getSerializableAsyncResult(javaAsyncResult),
                            new DeliveryOptions(),
                            callerMessage
                        )
                    );
                }
            ));
    }

    /**
     * Method to register a java synchronous function as a java service, so it can be called through the BusCallService.
     *
     * @param <A> java class of the input argument of the synchronous function
     * @param <R> java class of the output result of the synchronous function
     */
    public static <A, R> Registration registerBusCallEndpoint(String address, Function<A, R> javaFunction) {
        return BusCallService.<A, R>registerJavaHandlerForLocalCalls(address,
            (javaArgument, callerMessage) -> sendJavaReply(javaFunction.apply(javaArgument), callerMessage.options(), callerMessage)
        );
    }

    /**
     * Method to register a java callable (synchronous function with no input argument) as a java service, so it can be
     * called through the BusCallService.
     *
     * @param <R> java class of the output result of the callable
     */
    public static <R> Registration registerBusCallEndpoint(String address, Callable<R> callable) {
        return BusCallService.<Object, R>registerJavaHandlerForLocalCalls(address,
            (ignoredJavaArgument, callerMessage) -> sendJavaReply(callable.call(), null, callerMessage)
        );
    }

    public static Registration registerBusCallEndpoint(BusCallEndpoint<?, ?> endpoint) {
        return registerBusCallEndpoint(endpoint.getAddress(), endpoint.toAsyncFunction());
    }

    private static void log(String message) {
        Console.log("[BusCallService] " + message);
    }
}
