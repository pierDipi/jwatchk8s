# Watch ConfigMaps and Knative Eventing CRDs in Java

1. Create a cluster with KinD

    ```bash
    kind create cluster --name jwatchk8s

    kubectl cluster-info --context kind-jwatchk8s
    ```

1. Install Knative Eventing

    ```bash
    ko apply -f ~/go/src/knative.dev/eventing/config
    ```

1. Run `jwatchk8s` 

    - inside the cluster ```kubectl apply -f k8s/```
        - Wait the Deployment `jwatch` to become ready ```kubectl get deploy -n knative-eventing```
        - Access logs ```kubectl logs --follow -n knative-eventing -l app=jwatch```
    - outside the cluster ```./gradlew run``` 
    
1. Create a broker and a trigger

    ```bash
    kubectl apply -f k8s/knative-cr/
    ```
