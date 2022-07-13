package dev.webfx.stack.db.submit.listener;

import dev.webfx.stack.db.submit.SubmitArgument;

/**
 * @author Bruno Salmon
 */
public interface SubmitListener {

    void onSuccessfulSubmit(SubmitArgument... submitArguments);

}
