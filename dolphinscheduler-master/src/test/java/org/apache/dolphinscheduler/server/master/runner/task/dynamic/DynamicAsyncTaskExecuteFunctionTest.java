/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.server.master.runner.task.dynamic;

import static org.apache.dolphinscheduler.server.master.runner.execute.AsyncTaskExecuteFunction.AsyncTaskExecutionStatus;

import org.apache.dolphinscheduler.dao.entity.TaskInstance;
import org.apache.dolphinscheduler.dao.entity.WorkflowInstance;
import org.apache.dolphinscheduler.dao.mapper.CommandMapper;
import org.apache.dolphinscheduler.plugin.task.api.parameters.DynamicParameters;
import org.apache.dolphinscheduler.service.subworkflow.SubWorkflowService;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamicAsyncTaskExecuteFunctionTest {

    @Mock
    private WorkflowInstance workflowInstance;

    @Mock
    private TaskInstance taskInstance;

    @Mock
    private SubWorkflowService subWorkflowService;

    @Mock
    private CommandMapper commandMapper;

    @Mock
    private DynamicLogicTask dynamicLogicTask;

    private DynamicAsyncTaskExecuteFunction function;

    @BeforeEach
    void setUp() {
        workflowInstance = new WorkflowInstance();
        taskInstance = new TaskInstance();

        workflowInstance.setId(1);
        taskInstance.setTaskCode(2L);

        function = new DynamicAsyncTaskExecuteFunction(
                null,
                workflowInstance,
                taskInstance,
                dynamicLogicTask,
                commandMapper,
                subWorkflowService,
                0);
    }

    @Test
    void shouldReturnSuccessWhenAllSubProcessInstancesFinishedSuccessfully() {
        // Given
        List<WorkflowInstance> workflowInstances = Arrays.asList(Mockito.mock(WorkflowInstance.class));

        Mockito.when(workflowInstances.get(0).getCommandParam()).thenReturn("{}");
        Mockito.when(subWorkflowService.getAllDynamicSubWorkflow(1, 2L)).thenReturn(workflowInstances);
        Mockito.when(subWorkflowService.filterFinishProcessInstances(Mockito.any())).thenReturn(workflowInstances);
        Mockito.when(subWorkflowService.filterSuccessProcessInstances(Mockito.any())).thenReturn(workflowInstances);

        // When
        DynamicParameters dynamicParameters = new DynamicParameters();
        Mockito.when(dynamicLogicTask.getTaskParameters()).thenReturn(dynamicParameters);
        AsyncTaskExecutionStatus status = function.getAsyncTaskExecutionStatus();

        // Then
        Assertions.assertEquals(AsyncTaskExecutionStatus.SUCCESS, status);
    }

    @Test
    void shouldReturnFailedWhenSomeSubProcessInstancesFinishedUnsuccessfully() {
        // Given
        List<WorkflowInstance> workflowInstances =
                Arrays.asList(Mockito.mock(WorkflowInstance.class), Mockito.mock(WorkflowInstance.class));
        Mockito.when(subWorkflowService.getAllDynamicSubWorkflow(1, 2L)).thenReturn(workflowInstances);
        Mockito.when(subWorkflowService.filterFinishProcessInstances(Mockito.anyList())).thenReturn(workflowInstances);
        Mockito.when(subWorkflowService.filterSuccessProcessInstances(Mockito.anyList()))
                .thenReturn(Arrays.asList(workflowInstances.get(0)));

        // When
        AsyncTaskExecutionStatus status = function.getAsyncTaskExecutionStatus();

        // Then
        Assertions.assertEquals(AsyncTaskExecutionStatus.FAILED, status);
    }

    @Test
    void shouldReturnRunningWhenSomeSubProcessInstancesAreRunning() {
        // Given
        List<WorkflowInstance> workflowInstances = Arrays.asList(Mockito.mock(WorkflowInstance.class));
        Mockito.when(subWorkflowService.getAllDynamicSubWorkflow(1, 2L)).thenReturn(workflowInstances);
        Mockito.when(subWorkflowService.filterFinishProcessInstances(Mockito.anyList())).thenReturn(Arrays.asList());

        // When
        AsyncTaskExecutionStatus status = function.getAsyncTaskExecutionStatus();

        // Then
        Assertions.assertEquals(AsyncTaskExecutionStatus.RUNNING, status);
    }

    @Test
    void shouldReturnFailedWhenLogicTaskIsCancelled() {
        // Given
        List<WorkflowInstance> workflowInstances = Arrays.asList(Mockito.mock(WorkflowInstance.class));
        Mockito.when(subWorkflowService.getAllDynamicSubWorkflow(1, 2L)).thenReturn(workflowInstances);
        Mockito.when(dynamicLogicTask.isCancel()).thenReturn(true);

        // When
        AsyncTaskExecutionStatus status = function.getAsyncTaskExecutionStatus();

        // Then
        Assertions.assertEquals(AsyncTaskExecutionStatus.FAILED, status);
    }

}
