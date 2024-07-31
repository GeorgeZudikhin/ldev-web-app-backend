package largedev.webappbackend.controller;

import io.kubernetes.client.openapi.ApiException;
import largedev.webappbackend.dto.AssignmentRequestDTO;
import largedev.webappbackend.dto.DeleteDevicesDTO;
import largedev.webappbackend.exception.PodDeletionException;
import largedev.webappbackend.service.KubernetesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class KubernetesController {

    private final KubernetesService kubernetesService;

    @GetMapping("/devices")
    public ResponseEntity<Map<String, Integer>> getAllDevices() throws ApiException {
        Map<String, Integer> podInfo = kubernetesService.getPodNamesAndDeviceCounts();
        return ResponseEntity.ok(podInfo);
    }

    @PostMapping("/devices")
    public ResponseEntity<Void> createDevices(@RequestBody AssignmentRequestDTO assignmentRequest) {
        kubernetesService.spawnDevices(assignmentRequest.getAssignmentString(), assignmentRequest.getNumberOfDev());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/devices")
    public ResponseEntity<String> deleteDevices(@RequestBody DeleteDevicesDTO deleteDevicesDTO) {
        try {
            kubernetesService.deletePod(deleteDevicesDTO.getPodName());
            return ResponseEntity.ok("Pod deleted successfully!");
        } catch (PodDeletionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
