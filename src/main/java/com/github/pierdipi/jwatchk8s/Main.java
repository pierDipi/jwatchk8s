package com.github.pierdipi.jwatchk8s;

import io.fabric8.knative.eventing.v1alpha1.Broker;
import io.fabric8.knative.eventing.v1alpha1.BrokerList;
import io.fabric8.knative.eventing.v1alpha1.DoneableBroker;
import io.fabric8.knative.eventing.v1alpha1.DoneableTrigger;
import io.fabric8.knative.eventing.v1alpha1.Trigger;
import io.fabric8.knative.eventing.v1alpha1.TriggerList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {

  private static final Logger logger = Logger.getLogger(Main.class.getCanonicalName());

  private static final String NAMESPACE_ENV = "NAMESPACE";
  private static final String CONFIG_MAP_NAME = "CONFIG_MAP";
  private static final String DEFAULT_CONFIG_MAP_NAME = "jwatch";
  private static final String DEFAULT_CONFIG_MAP_NAMESPACE = DEFAULT_CONFIG_MAP_NAME;
  private static final String KNATIVE_BROKER_CRD_NAME = "brokers.eventing.knative.dev";
  private static final String KNATIVE_TRIGGER_CRD_NAME = "triggers.eventing.knative.dev";

  static {
    logger.setLevel(Level.INFO);
  }

  /**
   * 1. Create a k8s client 2. Watch configmaps in a namespace 3. Watch Knative Brokers in a
   * namespace 4. Watch Knative Triggers in a namespace
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    logger.info("application start");

    final var namespace = namespace();
    final var configMapName = configMapName();

    final var client = new DefaultKubernetesClient();

    logger.info(String.format("watching ConfigMaps in namespace %s ...", namespace));
    client.configMaps()
        .inNamespace(namespace)
        .withName(configMapName)
        .watch(new LogOnEventReceived<>(configMap -> logger.info(configMap.getData()
            .entrySet()
            .stream()
            .map(entry -> String
                .format("key = '%s'  value = '%s'", entry.getKey(), entry.getValue().trim()))
            .collect(Collectors.joining("\n"))
        )));

    logger.info(String.format("watching Brokers in namespace %s ...", namespace));
    final var brokerCR = client.customResourceDefinitions()
        .withName(KNATIVE_BROKER_CRD_NAME)
        .get();
    client.customResources(brokerCR, Broker.class, BrokerList.class, DoneableBroker.class)
        .inNamespace(namespace)
        .watch(new LogOnEventReceived<>());

    logger.info(String.format("watching Triggers in namespace %s ...", namespace));
    final var triggerCR = client.customResourceDefinitions()
        .withName(KNATIVE_TRIGGER_CRD_NAME)
        .get();
    client.customResources(triggerCR, Trigger.class, TriggerList.class, DoneableTrigger.class)
        .inNamespace(namespace)
        .watch(new LogOnEventReceived<>());
  }

  private static String namespace() {
    var namespace = System.getenv(NAMESPACE_ENV);
    if (namespace == null || namespace.equals("")) {
      namespace = DEFAULT_CONFIG_MAP_NAMESPACE;
    }
    return namespace;
  }

  private static String configMapName() {
    var configMapName = System.getenv(CONFIG_MAP_NAME);
    if (configMapName == null || configMapName.equals("")) {
      configMapName = DEFAULT_CONFIG_MAP_NAME;
    }
    return configMapName;
  }

  static class LogOnEventReceived<T extends HasMetadata> implements Watcher<T> {

    private final Consumer<T> postOnEventReceived;

    LogOnEventReceived() {
      postOnEventReceived = r -> {
      };
    }

    LogOnEventReceived(final Consumer<T> postOnEventReceived) {
      this.postOnEventReceived = postOnEventReceived;
    }

    @Override
    public void eventReceived(final Action action, final T resource) {
      final var format = resource.getKind() + ".%s = %s";
      final var namespace = String
          .format(format, "namespace", resource.getMetadata().getNamespace());
      final var name = String.format(format, "name", resource.getMetadata().getName());
      logger.info(String.format("%s -> %s %s", action, namespace, name));
      postOnEventReceived.accept(resource);
    }

    @Override
    public void onClose(final KubernetesClientException cause) {
      if (cause == null) {
        return;
      }
      logger.warning(cause.getMessage());
    }
  }
}
