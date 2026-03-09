expect class IGoogleAuthProvider {
    @Composable
    fun getUiProvider(): GoogleAuthUiProvider

    suspend fun signOut()
}
