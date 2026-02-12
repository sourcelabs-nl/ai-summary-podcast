package com.aisummarypodcast.store

import org.springframework.data.repository.CrudRepository

interface SourceRepository : CrudRepository<Source, String>
