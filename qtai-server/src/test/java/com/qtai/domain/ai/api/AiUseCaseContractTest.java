package com.qtai.domain.ai.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.qtai.domain.ai.api.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobResult;
import com.qtai.domain.ai.api.dto.GetAiQaResultCommand;
import com.qtai.domain.ai.api.dto.GetAiQaResultResult;
import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetResult;
import com.qtai.domain.ai.api.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.api.dto.RegisterAiValidationLogResult;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetResult;
import com.qtai.domain.ai.api.dto.RequestAiQaCommand;
import com.qtai.domain.ai.api.dto.RequestAiQaResult;
import com.qtai.domain.ai.api.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.api.dto.ReviewAiAssetResult;

class AiUseCaseContractTest {

    private static final List<Class<?>> USE_CASES = List.of(
            RequestAiQaUseCase.class,
            GetAiQaResultUseCase.class,
            CreateAiGenerationJobUseCase.class,
            RegisterAiGeneratedAssetUseCase.class,
            RegisterAiValidationLogUseCase.class,
            ReviewAiAssetUseCase.class,
            RegenerateAiAssetUseCase.class
    );

    private static final List<Class<?>> USE_CASE_DTOS = List.of(
            RequestAiQaCommand.class,
            RequestAiQaResult.class,
            GetAiQaResultCommand.class,
            GetAiQaResultResult.class,
            CreateAiGenerationJobCommand.class,
            CreateAiGenerationJobResult.class,
            RegisterAiGeneratedAssetCommand.class,
            RegisterAiGeneratedAssetResult.class,
            RegisterAiValidationLogCommand.class,
            RegisterAiValidationLogResult.class,
            ReviewAiAssetCommand.class,
            ReviewAiAssetResult.class,
            RegenerateAiAssetCommand.class,
            RegenerateAiAssetResult.class
    );

    @Test
    void publicAiContractsAreUseCaseInterfaces() {
        assertThat(USE_CASES)
                .allSatisfy(useCase -> {
                    assertThat(useCase).isInterface();
                    assertThat(useCase.getSimpleName()).endsWith("UseCase");
                });
    }

    @Test
    void useCaseMethodsUseCommandAndResultRecordsOnly() {
        for (Class<?> useCase : USE_CASES) {
            Method[] methods = useCase.getDeclaredMethods();

            assertThat(methods).hasSize(1);
            assertThat(methods[0].getParameterTypes())
                    .hasSize(1)
                    .allSatisfy(parameterType -> assertThat(parameterType.getSimpleName()).endsWith("Command"));
            assertThat(methods[0].getReturnType().getSimpleName()).endsWith("Result");
        }
    }

    @Test
    void useCaseDtosAreCommandOrResultRecords() {
        assertThat(USE_CASE_DTOS)
                .allSatisfy(dto -> {
                    assertThat(dto.isRecord()).isTrue();
                    assertThat(dto.getPackageName()).isEqualTo("com.qtai.domain.ai.api.dto");
                    assertThat(dto.getSimpleName()).matches(".*(Command|Result)$");
                });
    }

    @Test
    void useCaseDtosDoNotExposeRawProviderOrValidationReferenceText() {
        assertThat(USE_CASE_DTOS)
                .allSatisfy(dto -> assertThat(List.of(dto.getRecordComponents()))
                        .extracting(RecordComponent::getName)
                        .doesNotContain(
                                "providerRawResponse",
                                "rawResponse",
                                "validationReferenceText",
                                "referenceText",
                                "promptRaw",
                                "promptVersion"
                        ));
    }

    @Test
    void registerAiValidationLogCommandIncludesNullableValidationReferenceJobId() {
        assertThat(List.of(RegisterAiValidationLogCommand.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("validationReferenceJobId");
    }

    @Test
    void legacyFreeFormAiResponseContractIsRemoved() {
        assertThatThrownBy(() -> Class.forName("com.qtai.domain.ai.api.GenerateAiResponseUseCase"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.qtai.domain.ai.api.dto.AiPromptRequest"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.qtai.domain.ai.api.dto.AiResponse"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
