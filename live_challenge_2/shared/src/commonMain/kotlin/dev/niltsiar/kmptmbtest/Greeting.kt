package dev.niltsiar.kmptmbtest

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}