import java.net.URI

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://repo.osgeo.org/repository/release/")
        google()
        mavenCentral()
        maven {
            url = URI("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                // Do not change the username below.
                // This should always be `mapbox` (not your username).
                username = "mapbox"
                // Use the secret token you stored in gradle.properties as the password
                password = (extra["BACKPACKINGMAP_MAPBOX_SECRET"] as? String)!!
            }
        }
        maven("https://jitpack.io")
    }
}
rootProject.name = "Backpacking Map"
include(":app")
 