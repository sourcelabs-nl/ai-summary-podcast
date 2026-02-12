package com.aisummarypodcast.store

import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, String>
