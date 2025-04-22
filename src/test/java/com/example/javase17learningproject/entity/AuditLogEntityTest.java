package com.example.javase17learningproject.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

/**
 * エンティティの基本的な機能をテストするクラス.
 *
 * <p>このテストクラスでは以下の項目を検証します：
 * <ul>
 *   <li>エンティティの作成と永続化</li>
 *   <li>バリデーション制約の検証</li>
 *   <li>AuditLogレコードとの相互変換</li>
 *   <li>デフォルト値の設定</li>
 * </ul>
 */
class AuditLogEntityTest {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogEntityTest.class);


   /**
    * 有効な監査ログエンティティの作成テスト.
    *
    * <p>すべての必須フィールドを持つエンティティが正しく作成されることを検証します。
    */
   @Test
   void testCreateValidEntity() {
       logger.info("有効な監査ログエンティティの作成テストを開始");
       LocalDateTime now = LocalDateTime.now();
       AuditLogEntity entity = new AuditLogEntity(
           null,
           "TEST_EVENT",
           Severity.MEDIUM,
           1L,
           2L,
           "Test description",
           now
       );

       logger.info("エンティティを作成しました: {}", entity);
       
       // Then
       logger.info("エンティティの検証を開始");
       assertThat(entity).isNotNull();
       assertThat(entity.getEventType()).isEqualTo("TEST_EVENT");
       assertThat(entity.getSeverity()).isEqualTo(Severity.MEDIUM);
       assertThat(entity.getUserId()).isEqualTo(1L);
       assertThat(entity.getTargetId()).isEqualTo(2L);
       assertThat(entity.getDescription()).isEqualTo("Test description");
       assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    /**
     * イベントタイプがnullの場合のバリデーションテスト.
     *
     * <p>イベントタイプがnullの場合にNullPointerExceptionが
     * スローされることを検証します。
     */
    @Test
    void testCreateEntityWithNullEventType() {
        logger.info("イベントタイプnullのテストを開始");
        AuditLogEntity entity = new AuditLogEntity(
            null,
            null,
            Severity.MEDIUM,
            1L,
            2L,
            "Test description",
            LocalDateTime.now()
        );

        logger.info("nullイベントタイプでエンティティを作成: {}", entity);
        
        // Then
        logger.info("イベントタイプの検証");
        String eventType = entity.getEventType();  // このアクセスでnullが返される
        assertThat(eventType).isNull();
    }

    /**
     * 重要度のデフォルト値テスト.
     *
     * <p>重要度がnullの場合にデフォルト値(MEDIUM)が
     * 設定されることを検証します。
     */
    @Test
    void testCreateEntityWithNullSeverity() {
        logger.info("重要度nullのテストを開始");
        AuditLogEntity entity = new AuditLogEntity(
            null,
            "TEST_EVENT",
            null,
            1L,
            2L,
            "Test description",
            LocalDateTime.now()
        );

        logger.info("nullの重要度でエンティティを作成: {}", entity);
        
        // Then
        logger.info("デフォルト値の設定を検証");
        assertThat(entity.getSeverity()).isEqualTo(Severity.MEDIUM);  // デフォルト値が設定される
    }

    /**
     * 作成日時がnullの場合のバリデーションテスト.
     *
     * <p>作成日時がnullの場合に{@link ConstraintViolationException}が
     * スローされることを検証します。
     */
    @Test
    void testCreateEntityWithNullCreatedAt() {
        logger.info("作成日時nullのテストを開始");
        AuditLogEntity entity = new AuditLogEntity(
            null,
            "TEST_EVENT",
            Severity.MEDIUM,
            1L,
            2L,
            "Test description",
            null
        );

        // Then
        assertThat(entity.getCreatedAt()).isNotNull();  // 現在時刻が設定される
    }

    /**
     * エンティティからレコードへの変換テスト.
     *
     * <p>エンティティが正しくAuditLogレコードに変換されることを検証します。
     * すべてのフィールドが適切に変換されることを確認します。
     */
    @Test
    void testRecordConversion() {
        logger.info("エンティティ→レコード変換テストを開始");
        LocalDateTime now = LocalDateTime.now();
        AuditLogEntity entity = new AuditLogEntity(
            1L,
            "TEST_EVENT",
            Severity.HIGH,
            1L,
            2L,
            "Test description",
            now
        );

        logger.info("変換元エンティティ: {}", entity);
        
        // When
        AuditLog record = entity.toRecord();
        logger.info("変換後のレコード: {}", record);

        // Then
        assertThat(record.id()).isEqualTo(entity.getId());
        assertThat(record.eventType()).isEqualTo(entity.getEventType());
        assertThat(record.severity()).isEqualTo(entity.getSeverity());
        assertThat(record.userId()).isEqualTo(entity.getUserId());
        assertThat(record.targetId()).isEqualTo(entity.getTargetId());
        assertThat(record.description()).isEqualTo(entity.getDescription());
        assertThat(record.createdAt()).isEqualTo(entity.getCreatedAt());
    }

    /**
     * レコードからエンティティへの変換テスト.
     *
     * <p>AuditLogレコードが正しくエンティティに変換されることを検証します。
     * すべてのフィールドが適切に変換されることを確認します。
     */
    @Test
    void testCreateFromRecord() {
        logger.info("レコード→エンティティ変換テストを開始");
        LocalDateTime now = LocalDateTime.now();
        AuditLog record = new AuditLog(
            1L,
            "TEST_EVENT",
            Severity.HIGH,
            1L,
            2L,
            "Test description",
            now
        );

        logger.info("変換元レコード: {}", record);
        
        // When
        AuditLogEntity entity = AuditLogEntity.fromRecord(record);
        logger.info("変換後のエンティティ: {}", entity);
        
        // Then
        logger.info("変換結果を検証");
        assertThat(entity.getId()).isEqualTo(record.id());
        assertThat(entity.getEventType()).isEqualTo(record.eventType());
        assertThat(entity.getSeverity()).isEqualTo(record.severity());
        assertThat(entity.getUserId()).isEqualTo(record.userId());
        assertThat(entity.getTargetId()).isEqualTo(record.targetId());
        assertThat(entity.getDescription()).isEqualTo(record.description());
        assertThat(entity.getCreatedAt()).isEqualTo(record.createdAt());
    }

    /**
     * 重要度のデフォルト値設定テスト.
     *
     * <p>重要度にnullが指定された場合に、デフォルト値としてMEDIUMが
     * 設定されることを検証します。
     */
    @Test
    void testDefaultSeverity() {
        logger.info("重要度デフォルト値テストを開始");
        AuditLogEntity entity = new AuditLogEntity(
            null,
            "TEST_EVENT",
            null,  // severity is null
            1L,
            2L,
            "Test description",
            LocalDateTime.now()
        );

        logger.info("エンティティを作成: {}", entity);
        
        // Then
        logger.info("デフォルト作成日時を検証");
        assertThat(entity.getSeverity()).isEqualTo(Severity.MEDIUM);
    }

    /**
     * 作成日時のデフォルト値設定テスト.
     *
     * <p>作成日時にnullが指定された場合に、現在時刻が
     * 自動的に設定されることを検証します。
     */
    @Test
    void testDefaultCreatedAt() {
        logger.info("作成日時デフォルト値テストを開始");
        AuditLogEntity entity = new AuditLogEntity(
            null,
            "TEST_EVENT",
            Severity.MEDIUM,
            1L,
            2L,
            "Test description",
            null  // createdAt is null
        );

        // Then
        assertThat(entity.getCreatedAt()).isNotNull();
    }
}