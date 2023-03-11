```shell
# kubectl run会创建一个controller
# --generator让k8s创建一个ReplicationController
# ReplicationController用于保持pod的指定副本个数
kubectl run kubia --image=luksa/kubia --port=8080 --generator=run/v1

kubectl get rc
kubectl scale rc kubia --replicas=3



# 列出pod
kubectl get pods -o wide

# 创建一个服务对象，将该controller管理的所有pod能够从集群外部访问
kubectl expose rc kubia --type=LoadBalancer --name kubia-http

# 列出service
kubectl get svc


# 新的pod与替换它的pod具有不同的IP地址，这就是需要svc的地方，解决不断变化的pod IP地址问题，在一个固定的IP和端口上对外暴露多个pod。
# 客户端应该通过固定IP地址连接到Service，而不是直接连接pod



kubectl describe pod pod-name
```

