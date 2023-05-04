package com.ivi.bigdata.common.rpc.akka.framework.client;

import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.ivi.bigdata.common.rpc.akka.framework.AkkaUtils;
import com.ivi.bigdata.common.rpc.akka.framework.FutureUtils;
import scala.reflect.ClassTag$;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AkkaRpcClient {
    private ActorRef actorRef;

    public void connect(String address) throws ExecutionException, InterruptedException {
        ActorSystem localActorSystem = AkkaUtils.createRemoteActorSystem("rpcClientSystem", 9999);
        ActorSelection actorSel = localActorSystem.actorSelection(address);
        Timeout timeout = new Timeout(2, TimeUnit.SECONDS);

        final CompletableFuture<ActorIdentity> identityFuture = FutureUtils.toJava(
                Patterns.ask(actorSel, new Identify(42), timeout)
                        .mapTo(ClassTag$.MODULE$.apply(ActorIdentity.class)));

        final CompletableFuture<ActorRef> actorRefFuture = identityFuture.thenApply(
                (actorIdentity -> {
                    if (actorIdentity.getRef() == null) {
                        throw new CompletionException(
                                new RuntimeException("Couldn't connect to rpc endpoint under address " + address + "."));
                    } else {
                        return actorIdentity.getRef();
                    }
                }));

        actorRef = actorRefFuture.get();
    }

    public Object ask(Object message) throws ExecutionException, InterruptedException {
        Timeout timeout = new Timeout(2, TimeUnit.SECONDS);
        CompletableFuture<Object> resultFuture = FutureUtils.toJava(Patterns.ask(actorRef, message, timeout));
        return resultFuture.get();
    }
}
