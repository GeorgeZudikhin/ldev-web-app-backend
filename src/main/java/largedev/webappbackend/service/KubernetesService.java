package largedev.webappbackend.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import largedev.webappbackend.exception.PodDeletionException;

import java.io.IOException;
import java.util.*;

@Service
public class KubernetesService {

    private CoreV1Api api;

    private final int numberOfContPerPod = 10;

    @Value("${namespace}")
    private String namespace;

    @Value("${tvIotImage}")
    private String tvIotImage;

    @PostConstruct
    public void init() throws IOException {
        ApiClient client = Config.defaultClient();
        api = new CoreV1Api(client);
    }

    public Map<String, Integer> getPodNamesAndDeviceCounts() throws ApiException {
        V1PodList podList = api.listNamespacedPod(namespace).execute();
        Map<String, Integer> podDeviceCountMap = new HashMap<>();

        List<V1Pod> pods = podList.getItems();
        for (V1Pod pod : pods) {
            String podName = Objects.requireNonNull(pod.getMetadata()).getName();
            int deviceCount = Objects.requireNonNull(pod.getSpec()).getContainers().size();
            podDeviceCountMap.put(podName, deviceCount);
        }

        return podDeviceCountMap;
    }

    public void spawnDevices(String assignmentString, int numberOfDev) {
        int numberOfFullPods = numberOfDev / numberOfContPerPod;
        Map<String, String> nodeSelector = new HashMap<>();
        nodeSelector.put("worker.gardener.cloud/pool", "np-dev");

        for (int currentPod = 0; currentPod < numberOfFullPods; currentPod++) {
            List<V1Container> containers = createContainers(assignmentString);
            createAndDeployPod(containers, nodeSelector);
        }
    }

    public void deletePod(String podName) {
        try {
            api.deleteNamespacedPod(podName, namespace).execute();
        } catch (ApiException e) {
            throw new PodDeletionException("Error occurred while deleting pod: " + podName);
        }
    }

    private List<V1Container> createContainers(String assignmentString) {
        List<V1Container> containers = new ArrayList<>();
        for (int i = 1; i <= numberOfContPerPod; i++) {
            V1Container container = new V1Container()
                    .name("device-container-" + i + "-" + UUID.randomUUID().toString().replace("-", ""))
                    .image(tvIotImage)
                    .env(Collections.singletonList(new V1EnvVar()
                            .name("ASSIGNMENT_STRING")
                            .value(assignmentString)));
            containers.add(container);
        }
        return containers;
    }

    private void createAndDeployPod(List<V1Container> containers, Map<String, String> nodeSelector) {
        V1Pod pod = new V1Pod()
                .metadata(new V1ObjectMeta()
                        .name("device-group-" + UUID.randomUUID().toString().replace("-", ""))
                        .namespace(namespace))
                .spec(new V1PodSpec()
                        .overhead(null)
                        .nodeSelector(nodeSelector)
                        .containers(containers));

        try {
            V1Pod result = api.createNamespacedPod(namespace, pod).execute();
            System.out.println("Pod created: " + result);
        } catch (Exception e) {
            System.out.println("Error while creating pod: " + e.getMessage());
        }
    }
}
