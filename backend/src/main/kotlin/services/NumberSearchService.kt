package com.pna.backend.services

import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.domain.auth.response.PhoneNumberLookupResult
import com.pna.backend.domain.auth.response.SavedNumberSearchResponse

class NumberSearchService(
    private val repository: NumberSearchRepository
) {

    fun getOrLookup(number: String, lookup: (String) -> PhoneNumberLookupResult): PhoneNumberLookupResult {
        val normalizedNumber = number.trim()
        val cached = repository.findLatestByNumber(normalizedNumber)
        if (cached != null) {
            return cached.result
        }

        val result = lookup(normalizedNumber)
        repository.save(normalizedNumber, result)
        return result
    }

    fun getAll(): List<SavedNumberSearchResponse> {
        return repository.findAll()
    }
}
