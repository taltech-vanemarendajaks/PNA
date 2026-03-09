actual class GoogleAuthProvider(
    private val credentialManager: CredentialManager
) {
    @Composable
    actual fun getUiProvider(): GoogleAuthUiProvider {
        val activityContext = LocalContext.current
        return GoogleAuthUiProvider(activityContext, credentialManager)
    }

    actual suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
