package com.hdgh0g.jooq.translator.reproduce

import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.support.TransactionOperations
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = [TestExceptionHandling.Initializer::class])
class TestExceptionHandling(
    @Autowired private val jooq: DSLContext,
    @Autowired private val transactionOperations: TransactionOperations
) {

    @Test
    fun `should throw spring DataIntegrityViolationException exception`() {
        jooq.execute("""
            create table test_table(
                id varchar not null primary key,
                value int not null 
            );
        """.trimIndent())

        val queries = (1..3).map { value -> "insert into test_table values ('key', $value)" }
        assertThrows<DataIntegrityViolationException> {
            transactionOperations.execute {
                jooq.batch(*queries.toTypedArray()).execute()
            }
        }
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
//                "spring.datasource.url=" + postgreSQLContainer.jdbcUrl,
                "spring.datasource.url=" + postgreSQLContainer.jdbcUrl + "&preferQueryMode=simple",
                "spring.datasource.username=" + postgreSQLContainer.username,
                "spring.datasource.password=" + postgreSQLContainer.password
            ).applyTo(applicationContext.environment); }

    }

    companion object {
        @Container
        protected val postgreSQLContainer = PostgreSQLContainer("postgres:13")
            .withDatabaseName("integration-tests-db")
            .withUsername("sa")
            .withPassword("sa")!!
    }

}
