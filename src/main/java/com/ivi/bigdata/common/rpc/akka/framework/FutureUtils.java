package com.ivi.bigdata.common.rpc.akka.framework;

import akka.dispatch.OnComplete;
import scala.concurrent.Future;
import scala.concurrent.ExecutionContext;

import java.util.concurrent.CompletableFuture;

/**
 * 将Scala里面的Future转化为Java中的Future
 */
public class FutureUtils {
    public static <T, U extends T> CompletableFuture<T> toJava(Future<U> scalaFuture) {
        final CompletableFuture<T> result = new CompletableFuture<>();

        scalaFuture.onComplete(new OnComplete<U>() {
            @Override
            public void onComplete(Throwable failure, U success) throws Throwable, Throwable {
                if (failure != null) {
                    result.completeExceptionally(failure);
                } else {
                    result.complete(success);
                }
            }
        }, DirectExecutionContext.INSTANCE);
        return result;
    }

    private static class DirectExecutionContext implements ExecutionContext {
        static final DirectExecutionContext INSTANCE = new DirectExecutionContext();

        private DirectExecutionContext() {
        }

        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void reportFailure(Throwable cause) {
            throw new IllegalStateException("Error in direct execution contex.", cause);
        }

        @Override
        public ExecutionContext prepare() {
            return this;
        }
    }
}
