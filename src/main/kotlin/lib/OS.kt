package name.blackcap.socialbutterfly.lib

enum class OS {
    MAC, UNIX, WINDOWS, OTHER;
    companion object {
        private val rawType = System.getProperty("os.name")?.lowercase()
        val type = if (rawType == null) {
            OTHER
        } else if (rawType.contains("win")) {
            WINDOWS
        } else if (rawType.contains("mac")) {
            MAC
        } else if (rawType.contains("nix") || rawType.contains("nux") || rawType.contains("aix") || rawType.contains("sunos")) {
            UNIX
        } else {
            OTHER
        }
    }
}
