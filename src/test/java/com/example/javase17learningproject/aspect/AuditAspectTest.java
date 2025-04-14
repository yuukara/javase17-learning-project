package com.example.javase17learningproject.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.javase17learningproject.annotation.Audited;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.repository.AuditLogInMemoryStorage;

@ExtendWith(SpringExtension.class)
class AuditAspectTest {

    @MockBean
    private AuditLogInMemoryStorage auditLogStorage;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private TestService createProxiedTestService() {
        // セキュリティコンテキストのセットアップ
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        User user = new User("test", "test@example.com", "password");
        user.setId(1L);

        when(authentication.getPrincipal()).thenReturn(user);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // AOPプロキシの作成
        TestService target = new TestService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        AuditAspect aspect = new AuditAspect(auditLogStorage);
        factory.addAspect(aspect);

        return factory.getProxy();
    }

    @Test
    void testAuditedMethodExecution() {
        // Given
        TestService service = createProxiedTestService();
        Long targetId = 123L;
        when(auditLogStorage.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        // When
        service.auditedMethod(targetId);

        // Then
        verify(auditLogStorage).save(auditLogCaptor.capture());
        AuditLog capturedLog = auditLogCaptor.getValue();
        assertThat(capturedLog.eventType()).isEqualTo("TEST_EVENT");
        assertThat(capturedLog.severity()).isEqualTo(Severity.MEDIUM);
        assertThat(capturedLog.userId()).isEqualTo(1L);
        assertThat(capturedLog.targetId()).isEqualTo(targetId);
    }

    @Test
    void testAuditedMethodWithCustomDescription() {
        // Given
        TestService service = createProxiedTestService();
        Long targetId = 456L;

        // When
        service.auditedMethodWithDescription(targetId);

        // Then
        verify(auditLogStorage).save(auditLogCaptor.capture());
        AuditLog capturedLog = auditLogCaptor.getValue();
        assertThat(capturedLog.description()).isEqualTo("Custom test description");
    }

    @Test
    void testAuditedMethodWithHighSeverity() {
        // Given
        TestService service = createProxiedTestService();
        Long targetId = 789L;

        // When
        service.auditedMethodWithHighSeverity(targetId);

        // Then
        verify(auditLogStorage).save(auditLogCaptor.capture());
        AuditLog capturedLog = auditLogCaptor.getValue();
        assertThat(capturedLog.severity()).isEqualTo(Severity.HIGH);
    }

    // テスト用のサービスクラス
    static class TestService {
        @Audited(eventType = "TEST_EVENT")
        public void auditedMethod(Long id) {
            // テストメソッド
        }

        @Audited(eventType = "TEST_EVENT", description = "Custom test description")
        public void auditedMethodWithDescription(Long id) {
            // テストメソッド
        }

        @Audited(eventType = "TEST_EVENT", severity = Severity.HIGH)
        public void auditedMethodWithHighSeverity(Long id) {
            // テストメソッド
        }
    }
}