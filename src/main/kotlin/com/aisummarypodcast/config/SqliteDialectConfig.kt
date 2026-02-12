package com.aisummarypodcast.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
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
            listOf(IntegerToBooleanConverter(), BooleanToIntegerConverter())
        )
    }

    class IntegerToBooleanConverter : Converter<Int, Boolean> {
        override fun convert(source: Int): Boolean = source != 0
    }

    class BooleanToIntegerConverter : Converter<Boolean, Int> {
        override fun convert(source: Boolean): Int = if (source) 1 else 0
    }
}
