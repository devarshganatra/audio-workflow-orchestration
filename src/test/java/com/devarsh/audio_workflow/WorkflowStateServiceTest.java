package com.devarsh.audio_workflow;

import com.devarsh.audio_workflow.domain.Task;
import com.devarsh.audio_workflow.domain.TaskType;
import com.devarsh.audio_workflow.domain.Workflow;
import com.devarsh.audio_workflow.repository.TaskRepository;
import com.devarsh.audio_workflow.repository.WorkflowRepository;
import com.devarsh.audio_workflow.service.WorkflowStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
@SpringBootTest
public class WorkflowStateServiceTest {
    @Autowired
    private WorkflowStateService workflowStateService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void createWorkflowShouldCreateInitialTask() {

        Workflow workflow =
                workflowStateService.createWorkflow(
                        "sample.mp3"
                );

        assertNotNull(workflow.getId());

        List<Task> tasks =
                taskRepository.findByWorkflowId(
                        workflow.getId()
                );

        assertEquals(1, tasks.size());

        assertEquals(
                TaskType.VALIDATE,
                tasks.getFirst().getTaskType()
        );
    }
}
