package com.devarsh.audio_workflow.api.controller;

import com.devarsh.audio_workflow.api.dto.TaskResponse;
import com.devarsh.audio_workflow.api.dto.TimelineEvent;
import com.devarsh.audio_workflow.api.dto.WorkflowResponse;
import com.devarsh.audio_workflow.domain.Task;
import com.devarsh.audio_workflow.domain.TaskHistory;
import com.devarsh.audio_workflow.domain.TaskType;
import com.devarsh.audio_workflow.domain.Workflow;
import com.devarsh.audio_workflow.repository.TaskRepository;
import com.devarsh.audio_workflow.service.MinioStorageService;
import com.devarsh.audio_workflow.service.OrchestratorService;
import com.devarsh.audio_workflow.service.WorkflowStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class AudioController {

    private final MinioStorageService minioStorageService;
    private final WorkflowStateService workflowStateService;
    private final OrchestratorService orchestratorService;
    private final TaskRepository taskRepository;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkflowResponse> uploadAudio(@RequestParam("file" )MultipartFile file){
        String originalName=file.getOriginalFilename();

        String extension="";
        if(originalName!=null && originalName.contains(".")){
            extension=originalName.substring(originalName.lastIndexOf("."));
        }
        String objectKey =
                "audio/" +
                        UUID.randomUUID() +
                        extension;
        String uploadedKey =
                minioStorageService.uploadFile(
                        file,
                        objectKey
                );

        Workflow workflow =
                workflowStateService.createWorkflow(
                        uploadedKey
                );

        // Step 4.5: dispatch the VALIDATE task immediately
        Task validateTask = taskRepository.findByWorkflowId(workflow.getId()).stream()
                .filter(t -> t.getTaskType() == TaskType.VALIDATE)
                .findFirst()
                .orElseThrow();
        orchestratorService.dispatchTask(validateTask, java.util.Map.of());

        WorkflowResponse response =
                new WorkflowResponse(
                        workflow.getExternalId(),
                        workflow.getStatus().name(),
                        workflow.getAudioFileKey(),
                        workflow.getCreatedAt(),
                        workflow.getUpdatedAt()
                );

        return ResponseEntity
                .accepted()
                .body(response);
    }

    @GetMapping("/{workflowId}")
    public WorkflowResponse getWorkflow(@PathVariable UUID workflowId){
        Workflow workflow=workflowStateService.getWorkflow(workflowId);

        return new WorkflowResponse(workflow.getExternalId(),
                workflow.getStatus().name(),
                workflow.getAudioFileKey(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt());
    }

    @GetMapping("/{workflowId}/tasks")
    public List<TaskResponse> getTasks(
            @PathVariable UUID workflowId
    ) {

        return workflowStateService
                .getTasks(workflowId)
                .stream()
                .map(task -> new TaskResponse(
                        task.getId(),
                        task.getTaskType().name(),
                        task.getStatus().name(),
                        task.getRetryCount(),
                        task.getErrorMessage(),
                        task.getCreatedAt()
                ))
                .toList();
    }

    @GetMapping("/{workflowId}/timeline")
    public List<TimelineEvent> getTimeline(
            @PathVariable UUID workflowId
    ) {

        return workflowStateService
                .getTimeline(workflowId)
                .stream()
                .map(history -> new TimelineEvent(
                        history.getTask().getId(),
                        history.getTaskType(),
                        history.getOldStatus(),
                        history.getNewStatus(),
                        history.getWorkerId(),
                        history.getMessage(),
                        history.getOccurredAt()
                ))
                .toList();
    }


}
