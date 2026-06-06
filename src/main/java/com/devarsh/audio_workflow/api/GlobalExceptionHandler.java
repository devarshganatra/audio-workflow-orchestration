package com.devarsh.audio_workflow.api;

import com.devarsh.audio_workflow.exception.TaskNotFoundException;
import com.devarsh.audio_workflow.exception.WorkflowNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import com.devarsh.audio_workflow.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({
            WorkflowNotFoundException.class,
            TaskNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
            RuntimeException ex,
            HttpServletRequest request
    ) {

        return new ErrorResponse(
                Instant.now(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }
}
