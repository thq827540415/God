package com.ava.bigdata.common.io.akka.code;

import akka.actor.*;
import akka.dispatch.OnSuccess;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.remote.RemoteScope;
import scala.concurrent.Future;

public class JavaActorApp {

    /**
     * 指定ActorClass创建Props实例
     */
    private final static ActorSystem system = ActorSystem.create("sys");

    // private final LoggingAdapter log = Logging.getLogger(this.getContext().system(), this);

    /**
     * 基本操作
     */
    private static class MyActor extends AbstractLoggingActor {
        private final static ActorRef ref = system.actorOf(Props.create(MyActor.class), "myActor");

        static {
            // 2. 指定一个Actor工厂，实现akka.japi.Creator接口，重写其create方法
            system.actorOf(createProps(), "propsActor");
        }

        /**
         * 用于测试forward方法
         */
        private static class TargetActor extends AbstractLoggingActor {
            @Override
            public Receive createReceive() {
                return receiveBuilder()
                        .matchEquals("target", ignored -> log().info("this is target actor"))
                        .build();
            }
        }

        // protocol
        private static final class IncreaseMessage {
        }

        // protocol
        private static final class INeedReturnMessage {
        }

        private static final class ForwardMessage {
        }

        private int cnt = 0;

        private void increase() {
            cnt++;
        }

        private static Props createProps() {
            // 实现Creator接口
            return Props.create(MyActor.class, new Creator<MyActor>() {
                @Override
                public MyActor create() throws Exception, Exception {
                    return new MyActor();
                }
            });
        }

        @Override
        public Receive createReceive() {
            // 通过builder匹配消息，同时进行消息的发送
            return receiveBuilder()
                    .match(INeedReturnMessage.class,
                            ignored -> {
                                // sender => Actor[akka://sys/temp/$a]
                                getSender().tell("我已经收到了你的消息了", getSelf());
                            })
                    .match(ForwardMessage.class,
                            ignored -> {
                                log().info("i got the forward message， then i forward to hello");
                                // 通过getContext()来创建子Actor，getContext这里指MyActor的上下文对象
                                // akka://sys/user/myActor/targetActor
                                ActorRef targetActor = getContext().actorOf(
                                        Props.create(TargetActor.class), "targetActor");
                                targetActor.forward("target", getContext());
                                targetActor.tell("target", getSelf());
                            })
                    .matchEquals("hello", ignored -> log().info("i got a fixed value -> hello"))
                    .match(Integer.class, msg -> log().info("i got a integer type: {}", msg))
                    .match(String.class, msg -> log().info("i got a string type: {}", msg))
                    .match(IncreaseMessage.class,
                            ignored -> {
                                increase();
                                log().info("i got a message and cnt = {}", cnt);
                            })
                    .matchAny(this::unhandled)
                    .build();
        }

        private static void tellMsg() {
            // msg为任意可序列化的数据或对象
            ref.tell("hello", ActorRef.noSender());
            ref.tell(4, ActorRef.noSender());
            ref.tell("haha", ActorRef.noSender());

            for (int i = 0; i < 5; i++) {
                new Thread(() -> {
                    for (int j = 0; j < 5; j++) {
                        ref.tell(new IncreaseMessage(), ActorRef.noSender());
                    }
                }).start();
            }
        }

        private static void askMsg() {
            // 是Scala中的Future
            // 必须传入sender，要不就找不到返回的地方了
            Future<Object> f = Patterns.ask(ref, new INeedReturnMessage(), 2000);

            System.out.println("ask...");

            f.onSuccess(new OnSuccess<Object>() {
                @Override
                public void onSuccess(Object result) throws Throwable, Throwable {
                    System.out.println("收到回复的消息：" + result);
                }
            }, system.dispatcher());

            System.out.println("continue...");
        }

        private static void forwardMsg() {
            // 给MyActor发送消息，然后目标Actor调用forward转发给目标
            // 谁首先给MyActor发消息，谁就是sender
            ref.tell(new ForwardMessage(), ActorRef.noSender());
        }
    }

    /**
     * Actor行为切换
     */
    private static class BecomeActor extends AbstractLoggingActor {

        private static final ActorRef ref = system.actorOf(Props.create(BecomeActor.class), "becomeActor");

        /**
         * 转换后的行为
         */
        private Receive doActive() {
            return receiveBuilder()
                    .matchAny(
                            msg -> {
                                if ("turn back".equals(msg)) {
                                    getContext().become(createReceive());
                                    // getContext().unbecome();
                                }
                                log().info("this is doActive, msg: {}", msg);
                            })
                    .build();
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchAny(
                            msg -> {
                                log().info("this is createReceive, msg: {}", msg);
                                // become会将doActive存入一个执行栈中
                                getContext().become(doActive());
                            })
                    .build();
        }


        private static void doBecome() {
            ref.tell("hi", ActorRef.noSender());
            ref.tell("hi", ActorRef.noSender());
            ref.tell("turn back", ActorRef.noSender());
            ref.tell("hi", ActorRef.noSender());
            ref.tell("hi", ActorRef.noSender());
        }
    }


    /**
     * 远程访问
     */
    private static class Remote extends AbstractLoggingActor {

        // ActorSystem启动在远程的127.0.0.1:2552节点上，并创建了一个名为rmtActor的Actor
        {

            ActorSelection selection =
                    getContext().actorSelection("akka.tcp://sys@127.0.0.1:2552/user/rmtActor");
            // 假设远程Actor停止了，直接通过ActorSelection发送消息肯定不会成功
            // 要先向对方发送一个Identify信息，让对方自动返回一个ActorIdentity信息，该信息里面就包含了Actor引用，然后判断是否可用
            // selection.tell("hello remoteActor", getSelf());
            selection.tell(new Identify(1), getSelf());


            // 动态获取远程节点信息
            Address addr = new Address("akka.tcp", "sys", "localhost", 2552);
            ActorRef ref = getContext().actorOf(
                    Props.create(Remote.class).withDeploy(new Deploy(new RemoteScope(addr))));
        }


        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(ActorIdentity.class, msg -> {
                        log().info("被调用");
                        ActorRef ref = msg.getRef();
                        log().info("{}", ref);
                    })
                    .build();
        }
    }

    public static void main(String[] args) {
        system.actorOf(Props.create(Remote.class), "rmtActor");
    }
}
