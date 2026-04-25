package com.pna.backend.services

import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.domain.auth.response.PhoneNumberLookupResult
import com.pna.backend.domain.auth.response.SavedNumberSearchResponse
import domain.auth.GoogleUser

class NumberSearchService(
    private val repository: NumberSearchRepository
) {

    fun getOrLookup(user: GoogleUser, number: String, lookup: (String) -> PhoneNumberLookupResult): PhoneNumberLookupResult {
        val normalizedNumber = number.trim()
        val result = lookup(normalizedNumber)
        repository.save(user, normalizedNumber, result)
        return result
    }

    fun getAll(user: GoogleUser): List<SavedNumberSearchResponse> {
        return repository.findAllByUser(user)
    }

    fun deleteById(user: GoogleUser, searchId: String): Boolean {
        return repository.deleteById(user, searchId)
    }

    fun deleteAllByUser(user: GoogleUser): Int {
        return repository.deleteAllByUser(user)
    }
}
