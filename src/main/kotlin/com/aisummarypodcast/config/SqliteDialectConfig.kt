package com.aisummarypodcast.config

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.core.dialect.JdbcDialect
import org.springframework.data.relational.core.dialect.AnsiDialect

@Configuration
class SqliteDialectConfig {

    @Bean
    fun jdbcDialect(): JdbcDialect = object : JdbcDialect {
        private val delegate = AnsiDialect.INSTANCE

        override fun limit() = delegate.limit()
        override fun lock() = delegate.lock()
        override fun getSelectContext() = delegate.selectContext
    }

    @Bean
    fun jdbcCustomConversions(dialect: JdbcDialect): JdbcCustomConversions {
        return JdbcCustomConversions.of(
            dialect,
            listOf(
                IntegerToBooleanConverter(),
                BooleanToIntegerConverter(),
                StringToMapConverter(),
                MapToStringConverter()
            )
        )
    }

    class IntegerToBooleanConverter : Converter<Int, Boolean> {
        override fun convert(source: Int): Boolean = source != 0
    }

    class BooleanToIntegerConverter : Converter<Boolean, Int> {
        override fun convert(source: Boolean): Int = if (source) 1 else 0
    }

    @ReadingConverter
    class StringToMapConverter : Converter<String, Map<String, String>> {
        private val objectMapper = jacksonObjectMapper()
        override fun convert(source: String): Map<String, String> = objectMapper.readValue(source)
    }

    @WritingConverter
    class MapToStringConverter : Converter<Map<String, String>, String> {
        private val objectMapper = jacksonObjectMapper()
        override fun convert(source: Map<String, String>): String = objectMapper.writeValueAsString(source)
    }
}
