package com.api.todos.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * データベース設定クラス
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>Spring Data JPA Repository Scan設定
 *   <li>トランザクション管理の有効化
 *   <li>データベース接続設定（application.propertiesに委譲）
 * </ul>
 *
 * <p>【設計原則】
 *
 * <ul>
 *   <li>Infrastructure層でデータベース関連の設定を集約
 *   <li>JPA EntityはInfrastructure層に配置（自動スキャン）
 *   <li>Repository実装もInfrastructure層に配置
 *   <li>Spring Bootの自動設定を活用（DataSourceはapplication.propertiesで設定）
 * </ul>
 *
 * <p>【重要】
 *
 * <ul>
 *   <li>@EnableJpaRepositoriesでRepositoryパッケージをスキャン
 *   <li>JPA Entityは同パッケージ配下（infrastructure.persistence.entity）に配置することで自動スキャン
 *   <li>DataSourceはSpring Bootの自動設定に任せる（application.properties経由）
 * </ul>
 *
 * <p>【application.properties設定例】
 *
 * <pre>
 * # データベース接続設定
 * spring.datasource.url=jdbc:postgresql://localhost:5432/tododb
 * spring.datasource.username=todouser
 * spring.datasource.password=todopass
 * spring.datasource.driver-class-name=org.postgresql.Driver
 *
 * # JPA設定
 * spring.jpa.hibernate.ddl-auto=validate
 * spring.jpa.show-sql=true
 * spring.jpa.properties.hibernate.format_sql=true
 * spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
 * </pre>
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.api.todos.infrastructure.persistence.repository")
public class DatabaseConfig {
    // Spring Bootの自動設定によりDataSourceが自動的に作成されます
    // application.propertiesの設定を使用
}
