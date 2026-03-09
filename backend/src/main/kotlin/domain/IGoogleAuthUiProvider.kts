expect class IGoogleAuthUiProvider {
    suspend fun signIn(): GoogleAccount?
}
