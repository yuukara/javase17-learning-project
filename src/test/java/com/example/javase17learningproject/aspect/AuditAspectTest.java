package com.example.javase17learningproject.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.javase17learningproject.annotation.Audited;
import com.example.javase17learningproject.model.audit.AuditEvent;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.service.AuditLogService;

/**
 * AuditAspectのテストクラス。
 */
@SpringBootTest
@Import({AuditAspect.class, AuditAspectTest.TestService.class})
class AuditAspectTest {

    @Autowired
    private TestService testService;

    @MockBean
    private AuditLogService auditLogService;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<String> descriptionCaptor;

    @Test
    @WithMockUser(username = "test@example.com")
    void testAuditedMethodSuccess() {
        // When
        testService.successMethod("testParam");

        // Then
        verify(auditLogService, times(1)).logEvent(
            eventCaptor.capture(),
            any(),
            any(),
            descriptionCaptor.capture()
        );

        AuditEvent capturedEvent = eventCaptor.getValue();
        String capturedDescription = descriptionCaptor.getValue();

        assertThat(capturedEvent.getType()).isEqualTo("TEST_SUCCESS");
        assertThat(capturedEvent.getSeverity()).isEqualTo(Severity.LOW);
        assertThat(capturedDescription).contains("testParam");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testAuditedMethodFailure() {
        // When
        try {
            testService.failureMethod();
        } catch (RuntimeException e) {
            // Expected
        }

        // Then
        verify(auditLogService, times(1)).logEvent(
            eventCaptor.capture(),
            any(),
            any(),
            descriptionCaptor.capture()
        );

        AuditEvent capturedEvent = eventCaptor.getValue();
        String capturedDescription = descriptionCaptor.getValue();

        assertThat(capturedEvent.getType()).isEqualTo("TEST_FAILURE");
        assertThat(capturedEvent.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(capturedDescription).contains("テスト用エラー");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testCustomDescriptionMethod() {
        // When
        testService.customDescriptionMethod();

        // Then
        verify(auditLogService, times(1)).logEvent(
            eventCaptor.capture(),
            any(),
            any(),
            descriptionCaptor.capture()
        );

        String capturedDescription = descriptionCaptor.getValue();
        assertThat(capturedDescription).isEqualTo("カスタム説明付きの監査ログ");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testMethodWithParameters() {
        // When
        testService.methodWithParameters(1L, "testValue");

        // Then
        verify(auditLogService, times(1)).logEvent(
            eventCaptor.capture(),
            any(),
            any(),
            descriptionCaptor.capture()
        );

        String capturedDescription = descriptionCaptor.getValue();
        assertThat(capturedDescription).contains("1");
        assertThat(capturedDescription).contains("testValue");
    }

    /**
     * テスト用のサービスクラス
     */
    static class TestService {
        
        @Audited(eventType = "TEST_SUCCESS", severity = Severity.LOW)
        public void successMethod(String param) {
            // テスト用の成功メソッド
        }

        @Audited(eventType = "TEST_FAILURE", severity = Severity.HIGH)
        public void failureMethod() {
            throw new RuntimeException("テスト用エラー");
        }

        @Audited(
            eventType = "CUSTOM_DESCRIPTION",
            severity = Severity.MEDIUM,
            description = "カスタム説明付きの監査ログ"
        )
        public void customDescriptionMethod() {
            // カスタム説明のテスト
        }

        @Audited(eventType = "METHOD_WITH_PARAMS", severity = Severity.MEDIUM)
        public void methodWithParameters(Long id, String value) {
            // パラメータ付きメソッドのテスト
        }
    }
}