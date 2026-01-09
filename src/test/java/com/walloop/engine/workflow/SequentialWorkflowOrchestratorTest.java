package com.walloop.engine.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.Mockito;

class SequentialWorkflowOrchestratorTest {

    @Test
    void pausesOnWaitingStepAndResumesToCompletion() {
        TestWorkflowExecutionRepository repository = new TestWorkflowExecutionRepository();
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        SequentialWorkflowOrchestrator orchestrator = new SequentialWorkflowOrchestrator(repository, eventPublisher);

        WorkflowDefinition definition = new WorkflowDefinition() {
            @Override
            public String name() {
                return "test_workflow";
            }

            @Override
            public List<WorkflowStep> steps() {
                return List.of(
                        new WorkflowStep() {
                            @Override
                            public String key() {
                                return "await_flag";
                            }

                            @Override
                            public StepResult execute(WorkflowContext context) {
                                boolean flag = context.get("flag", Boolean.class).orElse(false);
                                return flag ? StepResult.completed("ok") : StepResult.waiting("waiting");
                            }
                        },
                        new WorkflowStep() {
                            @Override
                            public String key() {
                                return "next_step";
                            }

                            @Override
                            public StepResult execute(WorkflowContext context) {
                                return StepResult.completed("done");
                            }
                        }
                );
            }
        };

        WorkflowContext context = new WorkflowContext();
        context.put("flag", false);
        WorkflowExecution execution = orchestrator.start(definition, context);

        assertThat(execution.getStatus()).isEqualTo(WorkflowStatus.WAITING);
        assertThat(execution.getNextStepIndex()).isEqualTo(0);

        context.put("flag", true);
        WorkflowExecution resumed = orchestrator.resume(execution.getId(), definition, context);

        assertThat(resumed.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(resumed.getNextStepIndex()).isEqualTo(2);
        assertThat(resumed.getHistory()).hasSizeGreaterThanOrEqualTo(2);
    }

    private static final class TestWorkflowExecutionRepository implements WorkflowExecutionRepository {
        private final Map<UUID, WorkflowExecution> storage = new ConcurrentHashMap<>();

        @Override
        public WorkflowExecution save(WorkflowExecution execution) {
            storage.put(execution.getId(), execution);
            return execution;
        }

        @Override
        public Optional<WorkflowExecution> findById(UUID executionId) {
            return Optional.ofNullable(storage.get(executionId));
        }

        @Override
        public Optional<WorkflowExecution> findByTransactionId(UUID transactionId) {
            return storage.values().stream()
                    .filter(execution -> transactionId.equals(execution.getTransactionId()))
                    .findFirst();
        }

        @Override
        public boolean existsPendingRetries() {
            return storage.values().stream()
                    .anyMatch(execution -> execution.getNextRetryAt() != null);
        }

        @Override
        public List<WorkflowExecution> findRetriesDue(java.time.Instant now) {
            return storage.values().stream()
                    .filter(execution -> execution.getNextRetryAt() != null)
                    .filter(execution -> !execution.getNextRetryAt().isAfter(now))
                    .toList();
        }
    }
}
