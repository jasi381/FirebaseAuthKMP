package dev.jasmeetsingh.firebaseauth

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform