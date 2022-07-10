package dev.webfx.stack.platform.server.services.submitlistener;

import dev.webfx.stack.platform.shared.services.submit.SubmitArgument;

/**
 * @author Bruno Salmon
 */
public interface SubmitListener {

    void onSuccessfulSubmit(SubmitArgument... submitArguments);

}
